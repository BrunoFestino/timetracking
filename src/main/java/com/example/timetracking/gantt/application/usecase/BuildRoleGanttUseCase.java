package com.example.timetracking.gantt.application.usecase;

import com.example.timetracking.gantt.application.data.FakeGanttDataProvider;
import com.example.timetracking.gantt.application.dto.GanttChart;
import com.example.timetracking.gantt.application.dto.GanttGroup;
import com.example.timetracking.gantt.application.dto.GanttRow;
import com.example.timetracking.gantt.application.model.GanttTask;
import com.example.timetracking.gantt.application.model.Role;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Builds the "By role" Gantt: one non-collapsible group per {@link Role} present, each
 * holding all of that role's issues laid out at once (no expanding). Issues are ordered
 * by planned start date, and every bar is coloured by its role.
 */
@Service
public class BuildRoleGanttUseCase {

    private final FakeGanttDataProvider dataProvider;

    public BuildRoleGanttUseCase(FakeGanttDataProvider dataProvider) {
        this.dataProvider = dataProvider;
    }

    public GanttChart build() {
        List<GanttTask> tasks = dataProvider.tasks();
        List<GanttGroup> groups = new ArrayList<>();

        for (Role role : Role.values()) {
            List<GanttTask> roleTasks = tasks.stream()
                    .filter(t -> t.assignee().role() == role)
                    .sorted(Comparator.comparing(GanttTask::start))
                    .toList();
            if (roleTasks.isEmpty()) {
                continue;
            }
            List<GanttRow> rows = new ArrayList<>();
            for (GanttTask task : roleTasks) {
                rows.add(new GanttRow(
                        task.key() + " · " + task.summary(),
                        task.assignee().name(),
                        task.start(),
                        task.end(),
                        task.status(),
                        role.color(),
                        0));
            }
            groups.add(new GanttGroup(
                    role.label(),
                    role.color(),
                    GanttBounds.min(roleTasks),
                    GanttBounds.max(roleTasks),
                    false,
                    rows));
        }

        LocalDate start = GanttBounds.min(tasks);
        LocalDate end = GanttBounds.max(tasks);
        return new GanttChart(start, end, groups);
    }
}

package com.example.timetracking.gantt.application.usecase;

import com.example.timetracking.gantt.application.data.FakeGanttDataProvider;
import com.example.timetracking.gantt.application.dto.GanttChart;
import com.example.timetracking.gantt.application.dto.GanttColors;
import com.example.timetracking.gantt.application.dto.GanttGroup;
import com.example.timetracking.gantt.application.dto.GanttRow;
import com.example.timetracking.gantt.application.model.GanttTask;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the "By team" Gantt: one collapsible group per milestone, whose rows are the
 * milestone's epics (aggregate rows) each followed by their issues. Aggregate start/end
 * are the min/max of the children; issue bars are coloured by workflow status.
 */
@Service
public class BuildTeamGanttUseCase {

    private final FakeGanttDataProvider dataProvider;

    public BuildTeamGanttUseCase(FakeGanttDataProvider dataProvider) {
        this.dataProvider = dataProvider;
    }

    public GanttChart build() {
        List<GanttTask> tasks = dataProvider.tasks();

        // milestone -> (epic -> issues), preserving dataset order
        Map<String, Map<String, List<GanttTask>>> byMilestone = new LinkedHashMap<>();
        for (GanttTask task : tasks) {
            byMilestone
                    .computeIfAbsent(task.milestone(), m -> new LinkedHashMap<>())
                    .computeIfAbsent(task.epic(), e -> new ArrayList<>())
                    .add(task);
        }

        List<GanttGroup> groups = new ArrayList<>();
        byMilestone.forEach((milestone, epics) -> {
            List<GanttRow> rows = new ArrayList<>();
            List<GanttTask> milestoneTasks = new ArrayList<>();

            for (Map.Entry<String, List<GanttTask>> epic : epics.entrySet()) {
                List<GanttTask> epicTasks = epic.getValue();
                milestoneTasks.addAll(epicTasks);

                rows.add(new GanttRow(
                        epic.getKey(), null,
                        GanttBounds.min(epicTasks), GanttBounds.max(epicTasks),
                        null, GanttColors.EPIC, 1));

                for (GanttTask task : epicTasks) {
                    rows.add(new GanttRow(
                            task.key() + " · " + task.summary(),
                            task.assignee().name(),
                            task.start(), task.end(),
                            task.status(), GanttColors.forStatus(task.status()), 2));
                }
            }

            groups.add(new GanttGroup(
                    milestone, GanttColors.MILESTONE,
                    GanttBounds.min(milestoneTasks), GanttBounds.max(milestoneTasks),
                    true, rows));
        });

        LocalDate start = GanttBounds.min(tasks);
        LocalDate end = GanttBounds.max(tasks);
        return new GanttChart(start, end, groups);
    }
}

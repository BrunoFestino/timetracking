package com.example.timetracking.milestone.ui.widget;

import com.example.timetracking.milestone.application.dto.Contribution;
import com.example.timetracking.milestone.application.dto.Progress;
import com.example.timetracking.milestone.application.dto.ProgressWorklog;
import com.example.timetracking.milestone.ui.style.DashboardStyle;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ContributorWorklogTabWidget extends Div {

    public ContributorWorklogTabWidget(List<Progress> epics) {
        setWidthFull();
        if (epics == null || epics.isEmpty()) {
            add(DashboardStyle.note("No contributors to display."));
            return;
        }

        // subtask key → parent task key, built from the Progress tree
        Map<String, String> subtaskParent = buildSubtaskParentMap(epics);
        // task key → task name
        Map<String, String> taskNames = buildTaskNameMap(epics);

        List<Contribution> mergedContributions = mergeContributions(epics);
        if (mergedContributions.isEmpty()) {
            add(DashboardStyle.note("No worklogs recorded."));
            return;
        }

        long maxSpent = mergedContributions.stream().mapToLong(Contribution::totalSeconds).max().orElse(1L);

        VerticalLayout list = new VerticalLayout();
        list.setPadding(false);
        list.setSpacing(false);
        list.setWidthFull();
        list.getStyle().set("gap", "8px");
        mergedContributions.forEach(c -> list.add(contributorRow(c, maxSpent, subtaskParent, taskNames)));
        add(list);
    }

    // ── contributor row ────────────────────────────────────────────────────────

    private CollapsibleSection contributorRow(Contribution contribution, long maxSpentSeconds,
                                              Map<String, String> subtaskParent, Map<String, String> taskNames) {

        double pct = maxSpentSeconds > 0 ? contribution.totalSeconds() * 100.0 / maxSpentSeconds : 0;
        DetailsRow header = new DetailsRow(
                contribution.name(),
                pct,
                DashboardStyle.hours(contribution.totalSeconds()) + "h",
                DashboardStyle.SPENT);

        Div body = buildTaskList(contribution, subtaskParent, taskNames);
        return new CollapsibleSection(header, body);
    }

    // ── task list inside a contributor ────────────────────────────────────────

    private Div buildTaskList(Contribution contribution,
                              Map<String, String> subtaskParent, Map<String, String> taskNames) {

        // Step 1: accumulate seconds per sourceKey, preserving first-seen summary
        Map<String, IssueAggregate> byKey = new LinkedHashMap<>();
        for (ProgressWorklog w : contribution.worklogs()) {
            byKey.compute(w.sourceKey(), (k, ex) -> ex == null
                    ? new IssueAggregate(w.sourceSummary(), w.seconds())
                    : new IssueAggregate(ex.summary(), ex.seconds() + w.seconds()));
        }

        // Step 2: separate task-level keys from subtask-level keys;
        //         roll subtask seconds up to their parent task bucket
        Map<String, TaskAggregate> tasks = new LinkedHashMap<>();   // taskKey → aggregate
        Map<String, Map<String, IssueAggregate>> subtasksByTask = new LinkedHashMap<>(); // taskKey → subtask map

        for (Map.Entry<String, IssueAggregate> entry : byKey.entrySet()) {
            String key = entry.getKey();
            IssueAggregate agg = entry.getValue();
            String parentKey = subtaskParent.get(key);

            if (parentKey != null) {
                // this is a subtask — store under its parent task
                subtasksByTask.computeIfAbsent(parentKey, k -> new LinkedHashMap<>())
                        .put(key, agg);
                // roll its seconds into the parent task bucket
                String parentName = taskNames.getOrDefault(parentKey, parentKey);
                tasks.compute(parentKey, (k, ex) -> ex == null
                        ? new TaskAggregate(parentName, agg.seconds())
                        : new TaskAggregate(ex.summary(), ex.seconds() + agg.seconds()));
            } else {
                // direct task-level worklog
                tasks.compute(key, (k, ex) -> ex == null
                        ? new TaskAggregate(agg.summary(), agg.seconds())
                        : new TaskAggregate(ex.summary(), ex.seconds() + agg.seconds()));
            }
        }

        // Step 3: render, sorted by seconds desc
        VerticalLayout entries = new VerticalLayout();
        entries.setPadding(false);
        entries.setSpacing(false);

        tasks.entrySet().stream()
                .sorted(Map.Entry.<String, TaskAggregate>comparingByValue(
                        Comparator.comparingLong(TaskAggregate::seconds)).reversed())
                .forEach(e -> {
                    String taskKey = e.getKey();
                    TaskAggregate taskAgg = e.getValue();
                    Map<String, IssueAggregate> subtasks = subtasksByTask.get(taskKey);

                    if (subtasks == null || subtasks.isEmpty()) {
                        // no subtasks — plain row, padded left by chevron width so it aligns with expandable rows
                        IssueWorklogSubRow plainRow = new IssueWorklogSubRow(
                                taskKey, taskAgg.summary(), taskAgg.seconds(), contribution.totalSeconds());
                        Div padded = new Div(plainRow);
                        padded.getStyle()
                                .set("padding-left", CollapsibleSection.CHEVRON_WIDTH)
                                .set("box-sizing", "border-box")
                                .set("width", "100%");
                        entries.add(padded);
                    } else {
                        // has subtasks — same row style but with chevron + expandable body
                        entries.add(taskWithSubtasks(
                                taskKey, taskAgg, subtasks, contribution.totalSeconds()));
                    }
                });

        return new Div(entries);
    }

    // ── task row that expands into subtask rows ────────────────────────────────

    private CollapsibleSection taskWithSubtasks(String taskKey, TaskAggregate taskAgg,
                                                Map<String, IssueAggregate> subtasks, long contributorTotalSeconds) {

        // header row uses the same IssueWorklogSubRow so it looks identical to plain task rows
        IssueWorklogSubRow taskHeader = new IssueWorklogSubRow(
                taskKey, taskAgg.summary(), taskAgg.seconds(), contributorTotalSeconds);

        VerticalLayout subtaskEntries = new VerticalLayout();
        subtaskEntries.setPadding(false);
        subtaskEntries.setSpacing(false);
        subtasks.entrySet().stream()
                .sorted(Map.Entry.<String, IssueAggregate>comparingByValue(
                        Comparator.comparingLong(IssueAggregate::seconds)).reversed())
                .forEach(e -> subtaskEntries.add(new IssueWorklogSubRow(
                        e.getKey(), e.getValue().summary(), e.getValue().seconds(), taskAgg.seconds())));

        Div subtaskBody = new Div(subtaskEntries);
        subtaskBody.getStyle()
                .set("padding-left", "16px")
                .set("box-sizing", "border-box")
                .set("width", "100%");

        return new CollapsibleSection(taskHeader, subtaskBody);
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    /**
     * Maps every subtask key to its parent task key using the Progress tree.
     */
    private Map<String, String> buildSubtaskParentMap(List<Progress> epics) {
        Map<String, String> map = new LinkedHashMap<>();
        for (Progress epic : epics) {
            for (Progress task : epic.children()) {
                for (Progress subtask : task.children()) {
                    map.put(subtask.key(), task.key());
                }
            }
        }
        return map;
    }

    /**
     * Maps every task key to its display name.
     */
    private Map<String, String> buildTaskNameMap(List<Progress> epics) {
        Map<String, String> map = new LinkedHashMap<>();
        for (Progress epic : epics) {
            for (Progress task : epic.children()) {
                map.put(task.key(), task.name());
            }
        }
        return map;
    }

    private List<Contribution> mergeContributions(List<Progress> epics) {
        Map<String, ContributionAccumulator> byContributor = new LinkedHashMap<>();
        for (Progress epic : epics) {
            for (Contribution contribution : epic.contributions()) {
                ContributionAccumulator acc = byContributor.computeIfAbsent(
                        contribution.name(), k -> new ContributionAccumulator());
                acc.totalSeconds += contribution.totalSeconds();
                acc.worklogs.addAll(contribution.worklogs());
            }
        }
        return byContributor.entrySet().stream()
                .map(e -> new Contribution(e.getKey(), e.getValue().totalSeconds, e.getValue().worklogs))
                .sorted(Comparator.comparingLong(Contribution::totalSeconds).reversed())
                .toList();
    }

    // ── private value types ───────────────────────────────────────────────────

    private static final class ContributionAccumulator {
        private long totalSeconds;
        private final List<ProgressWorklog> worklogs = new ArrayList<>();
    }

    private record IssueAggregate(String summary, long seconds) {
    }

    private record TaskAggregate(String summary, long seconds) {
    }
}

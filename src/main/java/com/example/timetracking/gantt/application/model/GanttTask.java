package com.example.timetracking.gantt.application.model;

import java.time.LocalDate;

/**
 * A single planned piece of work in the Gantt: one issue with a planned start/end window,
 * an owning milestone and epic, an assignee (and therefore a role) and a workflow status.
 *
 * <p>This is the raw record the fake data provider emits; both use cases build their
 * {@code GanttChart} from a flat {@code List<GanttTask>}, differing only in how they group.
 *
 * @param key       issue key, e.g. {@code "ARG-101"}
 * @param summary   short human-readable title
 * @param milestone owning milestone name
 * @param epic      owning epic name
 * @param assignee  the team member doing the work (carries the role)
 * @param start     planned start date (inclusive)
 * @param end       planned end date (inclusive)
 * @param status    workflow status: {@code "Done"}, {@code "In Progress"} or {@code "To Do"}
 */
public record GanttTask(
        String key,
        String summary,
        String milestone,
        String epic,
        TeamMember assignee,
        LocalDate start,
        LocalDate end,
        String status) {
}

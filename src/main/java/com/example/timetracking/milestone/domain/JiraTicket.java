package com.example.timetracking.milestone.domain;

import java.util.List;

/**
 * A single node in the Jira hierarchy (Milestone, Epic, Issue or Sub-task).
 *
 * <p>The whole tree is expressed with one immutable type and a {@code children}
 * list instead of a class per level: a Milestone simply holds Epic children, an Epic holds Issue children, and so on. This keeps relations
 * explicit as plain collections rather than typed cross-references.
 *
 * @param key                     Jira key, e.g. {@code TTAR-9625}
 * @param summary                 human-readable title
 * @param description             free-text description, {@code null} when absent
 * @param type                    Jira issue type name (Milestone, Epic, ...)
 * @param metadata                descriptive metadata (people, dates, project, ...), never {@code null}
 * @param effortEstimateManDays   custom up-front estimate, {@code 0} when absent
 * @param originalEstimateSeconds this node's own original estimate
 * @param timeSpentSeconds        this node's own logged time
 * @param worklogs                this node's own worklogs
 * @param children                direct child tickets (never {@code null})
 */
public record JiraTicket(
        String key,
        String summary,
        String description,
        String type,
        JiraMetadata metadata,
        double effortEstimateManDays,
        long originalEstimateSeconds,
        long timeSpentSeconds,
        List<Worklog> worklogs,
        List<JiraTicket> children) {

    public JiraTicket {
        metadata = metadata == null ? JiraMetadata.EMPTY : metadata;
        worklogs = worklogs == null ? List.of() : List.copyOf(worklogs);
        children = children == null ? List.of() : List.copyOf(children);
    }

    /**
     * Own time plus the time spent on every descendant.
     */
    public long totalTimeSpentSeconds() {
        return timeSpentSeconds
                + children.stream().mapToLong(JiraTicket::totalTimeSpentSeconds).sum();
    }

    /**
     * Own estimate plus the estimate of every descendant.
     */
    public long totalOriginalEstimateSeconds() {
        return originalEstimateSeconds
                + children.stream().mapToLong(JiraTicket::totalOriginalEstimateSeconds).sum();
    }

    @Override
    public String toString() {
        return this.key + " - " + this.summary;
    }
}

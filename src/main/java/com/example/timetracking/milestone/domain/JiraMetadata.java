package com.example.timetracking.milestone.domain;

import java.util.List;

/**
 * Descriptive metadata for a Jira ticket: people, lifecycle dates, classification and project context. Kept separate from
 * {@link JiraTicket}'s time/effort data so the ticket record stays focused while still carrying the full Jira picture.
 *
 * <p>Populated for the milestone (loaded via the full issue endpoint); child nodes
 * loaded through field-restricted searches default to {@link #EMPTY}.
 *
 * @param assignee       display name of the current assignee, {@code null} when unset
 * @param reporter       display name of the reporter, {@code null} when unset
 * @param creator        display name of the creator, {@code null} when unset
 * @param status         workflow status name (e.g. "In Progress")
 * @param priority       priority name (e.g. "High")
 * @param resolution     resolution name, {@code null} while unresolved
 * @param created        ISO creation timestamp
 * @param updated        ISO last-updated timestamp
 * @param dueDate                ISO due date ({@code yyyy-MM-dd}), {@code null} when unset
 * @param resolutionDate         ISO resolution timestamp, {@code null} while unresolved
 * @param startDate              custom start date ({@code customfield_15030}), {@code null} when unset
 * @param baselineDeliveryDate   custom baseline delivery date ({@code customfield_13434}), {@code null} when unset
 * @param effectiveDeliveryDate  custom effective delivery date ({@code customfield_13445}), {@code null} when unset
 * @param labels         Jira labels (never {@code null})
 * @param projectKey     owning project key (e.g. "TTAR")
 * @param projectName    owning project display name
 * @param components     component names (never {@code null})
 * @param fixVersions    fix version names (never {@code null})
 */

public record JiraMetadata(
        String assignee,
        String reporter,
        String creator,
        String status,
        String priority,
        String resolution,
        String created,
        String updated,
        String dueDate,
        String resolutionDate,
        String startDate,
        String baselineDeliveryDate,
        String effectiveDeliveryDate,
        List<String> labels,
        String projectKey,
        String projectName,
        List<String> components,
        List<String> fixVersions) {

    /**
     * Shared empty metadata for synthetic nodes and tickets without details.
     */
    public static final JiraMetadata EMPTY = new JiraMetadata(
            null, null, null, null, null, null, null, null, null, null, null, null, null,
            List.of(), null, null, List.of(), List.of());

    public JiraMetadata {
        labels = labels == null ? List.of() : List.copyOf(labels);
        components = components == null ? List.of() : List.copyOf(components);
        fixVersions = fixVersions == null ? List.of() : List.copyOf(fixVersions);
    }
}

package com.example.timetracking.velocity.application.dto;

/**
 * Effort logged on a single ticket (issue or sub-task) within some period.
 *
 * @param key     Jira key of the ticket the work was logged on
 * @param summary ticket title, may be null
 * @param seconds effort logged
 */
public record IssueEffort(String key, String summary, long seconds) {
}

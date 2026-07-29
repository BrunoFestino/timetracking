package com.example.timetracking.velocity.application.dto;

/**
 * One person's contribution to one delivered milestone.
 *
 * @param milestoneKey Jira key of the milestone
 * @param seconds      effort this person logged on it
 */
public record PersonMilestoneEffort(String milestoneKey, long seconds) {
}

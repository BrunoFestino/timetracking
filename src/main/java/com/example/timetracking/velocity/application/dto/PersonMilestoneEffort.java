package com.example.timetracking.velocity.application.dto;

import java.time.LocalDate;

/**
 * One person's participation in one delivered milestone.
 *
 * <p>{@code firstDay}/{@code lastDay} are that person's own first and last worklog on the
 * milestone, so their engagement can be shorter than the milestone's delivery window — the
 * difference between "was on it the whole time" and "came in for a week".
 *
 * @param milestoneKey Jira key of the milestone
 * @param seconds      effort this person logged on it
 * @param firstDay     first day this person logged work on it, {@code null} when undated
 * @param lastDay      last day this person logged work on it, {@code null} when undated
 */
public record PersonMilestoneEffort(
        String milestoneKey,
        long seconds,
        LocalDate firstDay,
        LocalDate lastDay) {

    /** Inclusive calendar days between this person's first and last worklog; {@code 0} when unknown. */
    public int engagedDays() {
        if (firstDay == null || lastDay == null || lastDay.isBefore(firstDay)) {
            return 0;
        }
        return (int) (lastDay.toEpochDay() - firstDay.toEpochDay()) + 1;
    }
}

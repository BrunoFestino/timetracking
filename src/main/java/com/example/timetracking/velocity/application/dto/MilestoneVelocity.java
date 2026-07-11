package com.example.timetracking.velocity.application.dto;

import java.time.LocalDate;
import java.util.Map;

/**
 * Velocity figures for a single milestone.
 *
 * @param key               Jira key, e.g. {@code TTAR-9625}
 * @param name              milestone summary
 * @param totalSpentSeconds time logged on the whole milestone tree
 * @param durationWeeks     planned milestone duration in weeks (min 1), informational
 * @param startDate         resolved start (start-date field, else earliest worklog); may be null
 * @param secondsByWeek     time logged per relative week (week 1 = milestone start)
 */
public record MilestoneVelocity(
        String key,
        String name,
        long totalSpentSeconds,
        int durationWeeks,
        LocalDate startDate,
        Map<Integer, Long> secondsByWeek) {

    /** Highest relative week with logged work (min 1); weeks without logs count as zero. */
    public int observedWeeks() {
        return secondsByWeek.keySet().stream().mapToInt(Integer::intValue).max().orElse(1);
    }

    /** Weekly velocity: average of the weekly values over the observed weeks. */
    public long avgSecondsPerWeek() {
        return totalSpentSeconds / observedWeeks();
    }
}

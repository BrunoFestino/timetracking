package com.example.timetracking.velocity.application.dto;

import java.util.Map;

/**
 * Velocity figures for a single milestone.
 *
 * @param key               Jira key, e.g. {@code TTAR-9625}
 * @param name              milestone summary
 * @param totalSpentSeconds time logged on the whole milestone tree
 * @param durationWeeks     milestone duration in weeks (min 1), used for the per-week rate
 * @param secondsByWeek     time logged per relative week (week 1 = milestone start)
 */
public record MilestoneVelocity(
        String key,
        String name,
        long totalSpentSeconds,
        int durationWeeks,
        Map<Integer, Long> secondsByWeek) {

    public long secondsPerWeek() {
        return durationWeeks > 0 ? totalSpentSeconds / durationWeeks : totalSpentSeconds;
    }
}

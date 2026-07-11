package com.example.timetracking.velocity.application.dto;

import java.util.Map;

/**
 * Weekly velocity of one team member across the selected milestones.
 *
 * @param name               contributor display name
 * @param totalSeconds       time logged across all selected milestones
 * @param secondsByMilestone time logged per milestone key
 * @param secondsByWeek      time logged per relative week (week 1 = milestone start)
 */
public record PersonVelocity(
        String name,
        long totalSeconds,
        Map<String, Long> secondsByMilestone,
        Map<Integer, Long> secondsByWeek) {

    /** Highest relative week with logged work (min 1); weeks without logs count as zero. */
    public int observedWeeks() {
        return secondsByWeek.keySet().stream().mapToInt(Integer::intValue).max().orElse(1);
    }

    /** Personal weekly velocity: average of the weekly values over the observed weeks. */
    public long avgSecondsPerWeek() {
        return totalSeconds / observedWeeks();
    }
}

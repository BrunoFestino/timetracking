package com.example.timetracking.velocity.application.dto;

import java.util.Map;

/**
 * Weekly velocity of one team member, broken down by milestone. Each milestone the
 * person contributed to keeps its own relative-week map (week 1 = that milestone's
 * start), so weekly rows can be labelled with the milestone's real calendar dates.
 *
 * @param name                 contributor display name
 * @param totalSeconds         time logged across all selected milestones
 * @param secondsByMilestoneWeek milestone key → relative week → seconds, in selection order
 */
public record PersonVelocity(
        String name,
        long totalSeconds,
        Map<String, Map<Integer, Long>> secondsByMilestoneWeek) {

    /** Sum, per milestone, of the highest relative week worked (min 1); gap weeks count as zero. */
    public int totalActiveWeeks() {
        int sum = secondsByMilestoneWeek.values().stream()
                .mapToInt(w -> w.keySet().stream().mapToInt(Integer::intValue).max().orElse(0))
                .sum();
        return Math.max(1, sum);
    }

    /** Personal weekly velocity: average of the weekly values shown for this person. */
    public long avgSecondsPerWeek() {
        return totalSeconds / totalActiveWeeks();
    }

    /** Highest relative week the person worked within the given milestone (0 if none). */
    public int observedWeeksInMilestone(String key) {
        return secondsByMilestoneWeek.getOrDefault(key, Map.of())
                .keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
    }

    /** Total seconds the person logged on the given milestone. */
    public long milestoneTotal(String key) {
        return secondsByMilestoneWeek.getOrDefault(key, Map.of())
                .values().stream().mapToLong(Long::longValue).sum();
    }
}

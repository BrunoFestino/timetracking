package com.example.timetracking.velocity.application.dto;

import java.util.Map;

/**
 * Average throughput of one team member across the selected milestones.
 *
 * @param name                    contributor display name
 * @param totalSeconds            time logged across all selected milestones
 * @param avgSecondsPerMilestone  total divided by the number of selected milestones
 * @param secondsByMilestone      time logged per milestone key
 */
public record PersonVelocity(
        String name,
        long totalSeconds,
        long avgSecondsPerMilestone,
        Map<String, Long> secondsByMilestone) {
}

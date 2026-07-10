package com.example.timetracking.velocity.application.dto;

import java.util.Map;

/**
 * Team throughput in one relative milestone week (week 1 = milestone start),
 * aggregated across the selected milestones so their ramp-ups can be compared.
 *
 * @param weekNumber         relative week, starting at 1
 * @param totalSeconds       team time logged during this week across all selected milestones
 * @param secondsByMilestone breakdown per milestone key
 */
public record WeekVelocity(
        int weekNumber,
        long totalSeconds,
        Map<String, Long> secondsByMilestone) {
}

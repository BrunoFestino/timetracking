package com.example.timetracking.velocity.application.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Team effort logged in one calendar week (Monday to Sunday).
 *
 * @param weekStart       the Monday this week starts on
 * @param totalSeconds    team effort logged this week
 * @param secondsByPerson effort per contributor this week, ordered by effort desc
 * @param issues          every ticket worked on this week, ordered by effort desc
 */
public record CalendarWeekVelocity(
        LocalDate weekStart,
        long totalSeconds,
        Map<String, Long> secondsByPerson,
        List<IssueEffort> issues) {

    public CalendarWeekVelocity {
        issues = issues == null ? List.of() : List.copyOf(issues);
    }
}

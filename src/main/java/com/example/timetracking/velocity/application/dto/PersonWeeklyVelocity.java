package com.example.timetracking.velocity.application.dto;

import java.time.LocalDate;
import java.util.Map;

/**
 * One contributor's effort per calendar week within the selected range.
 *
 * @param name          contributor display name
 * @param totalSeconds  effort logged across the whole range
 * @param secondsByWeek effort per week (keyed by the week's Monday), ordered by week
 */
public record PersonWeeklyVelocity(
        String name,
        long totalSeconds,
        Map<LocalDate, Long> secondsByWeek) {
}

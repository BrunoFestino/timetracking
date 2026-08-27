package com.example.timetracking.gantt.application.dto;

import java.time.LocalDate;

/**
 * One renderable row in a Gantt: a label plus the planned window to draw as a bar.
 *
 * @param label human-readable row label (e.g. {@code "ARG-101 · Login API"})
 * @param key   short key shown alongside the label, {@code null} for aggregate rows
 * @param start bar start date (inclusive)
 * @param end   bar end date (inclusive)
 * @param status workflow status, {@code null} for aggregate rows
 * @param color bar colour (CSS hex)
 * @param depth indentation level: 0 = group root, 1 = child, 2 = grandchild
 */
public record GanttRow(
        String label,
        String key,
        LocalDate start,
        LocalDate end,
        String status,
        String color,
        int depth) {
}

package com.example.timetracking.gantt.application.dto;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds the month ticks of a Gantt's time axis over a {@code [start, end]} range.
 *
 * <p>Each {@link Tick} carries the month's first day (for positioning) and a short label
 * (e.g. {@code "Aug 2026"}), letting the axis widget place month markers with the same
 * percentage maths the bars use.
 */
public final class GanttTimeline {

    /**
     * @param monthStart first day of the month (used for percentage positioning)
     * @param label      short month label, e.g. {@code "Aug 2026"}
     */
    public record Tick(LocalDate monthStart, String label) {
    }

    private GanttTimeline() {
    }

    /** One tick per calendar month touched by {@code [start, end]}, in order. */
    public static List<Tick> monthTicks(LocalDate start, LocalDate end) {
        List<Tick> ticks = new ArrayList<>();
        if (start == null || end == null) {
            return ticks;
        }
        YearMonth cursor = YearMonth.from(start);
        YearMonth last = YearMonth.from(end);
        while (!cursor.isAfter(last)) {
            ticks.add(new Tick(cursor.atDay(1), label(cursor)));
            cursor = cursor.plusMonths(1);
        }
        return ticks;
    }

    private static final String[] MONTHS = {
            "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };

    private static String label(YearMonth month) {
        return MONTHS[month.getMonthValue() - 1] + " " + month.getYear();
    }
}

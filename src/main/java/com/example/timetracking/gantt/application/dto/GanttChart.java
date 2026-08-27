package com.example.timetracking.gantt.application.dto;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * A complete Gantt ready to render: the timeline bounds (used to scale every bar) and the
 * ordered groups. Both {@code BuildTeamGanttUseCase} and {@code BuildRoleGanttUseCase}
 * produce this same shape so the UI is mode-agnostic.
 *
 * @param timelineStart earliest date across all rows (inclusive)
 * @param timelineEnd   latest date across all rows (inclusive)
 * @param groups        ordered groups to render top to bottom
 */
public record GanttChart(LocalDate timelineStart, LocalDate timelineEnd, List<GanttGroup> groups) {

    public GanttChart {
        groups = groups == null ? List.of() : List.copyOf(groups);
    }

    public boolean isEmpty() {
        return groups.isEmpty() || timelineStart == null || timelineEnd == null;
    }

    /** Total inclusive span of the timeline in days (at least 1). */
    public long totalDays() {
        if (timelineStart == null || timelineEnd == null) {
            return 1;
        }
        return Math.max(1, ChronoUnit.DAYS.between(timelineStart, timelineEnd) + 1);
    }

    /** Left offset of {@code date} as a percentage of the timeline (clamped to [0,100]). */
    public double offsetPercent(LocalDate date) {
        long fromStart = ChronoUnit.DAYS.between(timelineStart, date);
        return clampPercent(fromStart * 100.0 / totalDays());
    }

    /** Width of the inclusive {@code [start,end]} window as a percentage of the timeline. */
    public double widthPercent(LocalDate start, LocalDate end) {
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        return clampPercent(days * 100.0 / totalDays());
    }

    private static double clampPercent(double value) {
        return Math.max(0.0, Math.min(100.0, value));
    }
}

package com.example.timetracking.gantt.application.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * A group of {@link GanttRow}s under one heading — a milestone (By team) or a role (By role).
 *
 * @param label       group heading
 * @param color       heading / accent colour (CSS hex)
 * @param start       aggregate start of the group (min of its rows), {@code null} when empty
 * @param end         aggregate end of the group (max of its rows), {@code null} when empty
 * @param collapsible whether the UI should render the group as a click-to-expand section
 * @param rows        the rows in this group (already ordered for display)
 */
public record GanttGroup(
        String label,
        String color,
        LocalDate start,
        LocalDate end,
        boolean collapsible,
        List<GanttRow> rows) {

    public GanttGroup {
        rows = rows == null ? List.of() : List.copyOf(rows);
    }
}

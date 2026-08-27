package com.example.timetracking.gantt.ui.style;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Local style constants and small formatting helpers for the Gantt feature.
 *
 * <p>Deliberately self-contained: the feature does not import {@code DashboardStyle} or any
 * widget from other features, so the whole Gantt lives under {@code gantt/}.
 */
public final class GanttStyle {

    public static final String FONT = "Inter, 'Helvetica Neue', Arial, system-ui, sans-serif";
    public static final String PRIMARY_900 = "#0F4660";
    public static final String INK = "#1F2A30";
    public static final String MUTED = "#5F6B72";
    public static final String BORDER = "#E5E8EA";
    public static final String GRIDLINE = "#EEF1F3";
    public static final String TODAY = "#D64545";
    public static final String CARD_BG = "#FFFFFF";

    /** Fixed width of the left label column, shared by axis header and rows. */
    public static final String LABEL_COL_WIDTH = "280px";
    /** Height of a single Gantt row track. */
    public static final String ROW_HEIGHT = "30px";

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US);

    private GanttStyle() {
    }

    public static String formatDate(LocalDate date) {
        return date == null ? "" : DATE.format(date);
    }

    /** Tooltip text for a bar: "Mmm d – Mmm d, yyyy · Status". */
    public static String barTooltip(LocalDate start, LocalDate end, String status) {
        String range = formatDate(start) + " – " + formatDate(end);
        return status == null ? range : range + " · " + status;
    }
}

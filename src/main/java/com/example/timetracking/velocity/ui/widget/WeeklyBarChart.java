package com.example.timetracking.velocity.ui.widget;

import com.example.timetracking.milestone.ui.style.DashboardStyle;
import com.example.timetracking.velocity.ui.style.VelocityStyles;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

import java.util.List;

/**
 * A labelled bar chart of team effort per calendar week: the value on top of each bar,
 * the week's Monday underneath, and a dashed horizontal line marking the team's average
 * weekly velocity so above/below-average weeks read at a glance. Subtle gridlines give
 * a vertical scale; the peak week's bar is emphasised in the primary navy. Weeks without
 * work keep their column (value label on the baseline, no bar).
 *
 * <p>Pure CSS divs, same approach as {@link Sparkline}; scaled so the average line
 * always fits inside the chart area.
 */
public class WeeklyBarChart extends Div {

    private static final int CHART_HEIGHT_PX = 140;
    /** Bars scale within this height so the value label on top never squeezes a full bar. */
    private static final int BAR_AREA_PX = 118;
    private static final String GRID_COLOR = "#F0F2F4";

    /** One bar of the chart. */
    public record Column(long seconds, String valueLabel, String weekLabel, String tooltip) {
    }

    public WeeklyBarChart(List<Column> columns, long avgSeconds, String avgLabel, String color) {
        setWidthFull();

        long maxSeconds = columns.stream().mapToLong(Column::seconds).max().orElse(0L);
        long max = Math.max(1L, Math.max(avgSeconds, maxSeconds));

        Div chartArea = new Div();
        chartArea.getStyle()
                .set("position", "relative")
                .set("height", CHART_HEIGHT_PX + "px")
                .set("display", "flex")
                .set("align-items", "flex-end")
                .set("gap", "8px")
                .set("width", "100%")
                .set("box-sizing", "border-box")
                .set("border-bottom", "1px solid " + DashboardStyle.REMAINING);

        for (int quarter = 1; quarter <= 4; quarter++) {
            Div gridline = new Div();
            gridline.getStyle()
                    .set("position", "absolute")
                    .set("left", "0")
                    .set("right", "0")
                    .set("bottom", Math.round(quarter * BAR_AREA_PX / 4.0) + "px")
                    .set("height", "1px")
                    .set("background", GRID_COLOR)
                    .set("z-index", "0");
            chartArea.add(gridline);
        }

        if (avgSeconds > 0) {
            long avgPx = Math.round(avgSeconds * (double) BAR_AREA_PX / max);

            Div avgLine = new Div();
            avgLine.getStyle()
                    .set("position", "absolute")
                    .set("left", "0")
                    .set("right", "0")
                    .set("bottom", avgPx + "px")
                    .set("border-top", "1px dashed " + DashboardStyle.MUTED)
                    .set("z-index", "2");

            Span avgText = new Span(avgLabel);
            avgText.getStyle()
                    .set("position", "absolute")
                    .set("right", "0")
                    .set("bottom", (avgPx + 3) + "px")
                    .set("font-size", "10px")
                    .set("color", DashboardStyle.MUTED)
                    .set("background", "rgba(255,255,255,0.85)")
                    .set("padding", "0 4px")
                    .set("border-radius", "4px")
                    .set("white-space", "nowrap")
                    .set("z-index", "2");

            chartArea.add(avgLine, avgText);
        }

        Div labelsRow = new Div();
        labelsRow.getStyle()
                .set("display", "flex")
                .set("gap", "8px")
                .set("width", "100%")
                .set("box-sizing", "border-box")
                .set("margin-top", "4px");

        for (Column column : columns) {
            long barPx = column.seconds() > 0
                    ? Math.max(2L, Math.round(column.seconds() * (double) BAR_AREA_PX / max))
                    : 0;
            boolean peak = column.seconds() > 0 && column.seconds() == maxSeconds;

            Span value = new Span(column.valueLabel());
            value.getStyle()
                    .set("font-size", "10px")
                    .set("font-weight", "600")
                    .set("color", DashboardStyle.INK)
                    .set("margin-bottom", "2px")
                    .set("white-space", "nowrap");

            Div bar = new Div();
            bar.addClassName(VelocityStyles.BAR_CLASS);
            bar.getStyle()
                    .set("width", "100%")
                    .set("max-width", "42px")
                    .set("height", barPx + "px")
                    .set("flex-shrink", "0")
                    .set("background", peak ? DashboardStyle.PRIMARY_900 : color)
                    .set("border-radius", "4px 4px 0 0");

            Div columnDiv = new Div(value, bar);
            columnDiv.getStyle()
                    .set("flex", "1")
                    .set("min-width", "0")
                    .set("height", "100%")
                    .set("display", "flex")
                    .set("flex-direction", "column")
                    .set("align-items", "center")
                    .set("justify-content", "flex-end")
                    .set("z-index", "1");
            columnDiv.getElement().setAttribute("title", column.tooltip());
            chartArea.add(columnDiv);

            Span weekLabel = new Span(column.weekLabel());
            weekLabel.getStyle()
                    .set("flex", "1")
                    .set("min-width", "0")
                    .set("text-align", "center")
                    .set("font-size", "11px")
                    .set("color", DashboardStyle.MUTED)
                    .set("white-space", "nowrap")
                    .set("overflow", "hidden")
                    .set("text-overflow", "ellipsis");
            labelsRow.add(weekLabel);
        }

        add(chartArea, labelsRow);
    }
}

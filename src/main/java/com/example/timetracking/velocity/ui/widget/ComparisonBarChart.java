package com.example.timetracking.velocity.ui.widget;

import com.example.timetracking.milestone.ui.style.DashboardStyle;
import com.example.timetracking.velocity.ui.style.VelocityStyles;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

import java.util.List;

/**
 * A labelled bar chart comparing one figure across the selected milestones: the value on top
 * of each bar, a short caption (the milestone key) underneath, and a dashed horizontal line
 * marking the average so faster/slower-than-average milestones read at a glance. Subtle
 * gridlines give a vertical scale; the tallest bar is emphasised in the primary navy. A column
 * whose value is unknown keeps its slot (label on the baseline, no bar).
 *
 * <p>Pure CSS divs, same approach as {@link Sparkline}; scaled so the average line
 * always fits inside the chart area.
 */
public class ComparisonBarChart extends Div {

    private static final int CHART_HEIGHT_PX = 140;
    /** Bars scale within this height so the value label on top never squeezes a full bar. */
    private static final int BAR_AREA_PX = 118;
    private static final String GRID_COLOR = "#F0F2F4";

    /**
     * One bar of the chart: the plotted value, its label, the caption below it, hover text and
     * the bar colour. A {@code null} colour falls back to the chart's default, so every bar can
     * carry the colour that identifies its milestone across chart and comparison table.
     */
    public record Column(long value, String valueLabel, String caption, String tooltip, String color) {
    }

    public ComparisonBarChart(List<Column> columns, long avgValue, String avgLabel, String defaultColor) {
        setWidthFull();

        long maxValue = columns.stream().mapToLong(Column::value).max().orElse(0L);
        long max = Math.max(1L, Math.max(avgValue, maxValue));

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

        if (avgValue > 0) {
            long avgPx = Math.round(avgValue * (double) BAR_AREA_PX / max);

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
            long barPx = column.value() > 0
                    ? Math.max(2L, Math.round(column.value() * (double) BAR_AREA_PX / max))
                    : 0;

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
                    .set("background", column.color() != null ? column.color() : defaultColor)
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

            Span caption = new Span(column.caption());
            caption.getStyle()
                    .set("flex", "1")
                    .set("min-width", "0")
                    .set("text-align", "center")
                    .set("font-size", "11px")
                    .set("color", DashboardStyle.MUTED)
                    .set("white-space", "nowrap")
                    .set("overflow", "hidden")
                    .set("text-overflow", "ellipsis");
            caption.getElement().setAttribute("title", column.tooltip());
            labelsRow.add(caption);
        }

        add(chartArea, labelsRow);
    }
}

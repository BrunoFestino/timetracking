package com.example.timetracking.gantt.ui.widget;

import com.example.timetracking.gantt.application.dto.GanttChart;
import com.example.timetracking.gantt.application.dto.GanttTimeline;
import com.example.timetracking.gantt.ui.style.GanttStyle;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

import java.util.List;

/**
 * The month header of the Gantt: an empty left cell aligned with the label column, and a
 * timeline cell with one label per calendar month positioned by percentage.
 */
final class GanttTimeAxis {

    private GanttTimeAxis() {
    }

    static Div create(GanttChart chart) {
        Div labelCell = new Div();
        labelCell.getStyle()
                .set("width", GanttStyle.LABEL_COL_WIDTH)
                .set("flex-shrink", "0")
                .set("box-sizing", "border-box");

        Div track = new Div();
        track.getStyle()
                .set("position", "relative")
                .set("flex", "1")
                .set("height", "22px")
                .set("border-bottom", "1px solid " + GanttStyle.BORDER);

        List<GanttTimeline.Tick> ticks = GanttTimeline.monthTicks(chart.timelineStart(), chart.timelineEnd());
        for (GanttTimeline.Tick tick : ticks) {
            double left = chart.offsetPercent(tick.monthStart());

            Span label = new Span(tick.label());
            label.getStyle()
                    .set("position", "absolute")
                    .set("left", left + "%")
                    .set("bottom", "2px")
                    .set("font-size", "11px")
                    .set("font-weight", "600")
                    .set("color", GanttStyle.MUTED)
                    .set("white-space", "nowrap")
                    .set("padding-left", "3px");
            track.add(label);
        }

        Div row = new Div(labelCell, track);
        row.getStyle().set("display", "flex").set("align-items", "flex-end").set("width", "100%");
        return row;
    }
}

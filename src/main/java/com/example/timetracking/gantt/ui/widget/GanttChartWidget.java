package com.example.timetracking.gantt.ui.widget;

import com.example.timetracking.gantt.application.dto.GanttChart;
import com.example.timetracking.gantt.application.dto.GanttGroup;
import com.example.timetracking.gantt.application.dto.GanttRow;
import com.example.timetracking.gantt.application.dto.GanttTimeline;
import com.example.timetracking.gantt.ui.style.GanttStyle;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

import java.time.LocalDate;
import java.util.List;

/**
 * Renders a {@link GanttChart} as pure-CSS rows: a fixed label column on the left and a
 * percentage-scaled timeline on the right, with month gridlines and a "today" marker.
 *
 * <p>Collapsible groups (By team) are wrapped in {@link CollapsibleSection}; non-collapsible
 * groups (By role) render their heading and every row at once.
 */
public class GanttChartWidget extends Div {

    private final transient GanttChart chart;
    private final transient List<GanttTimeline.Tick> ticks;
    private final LocalDate today = LocalDate.now();

    public GanttChartWidget(GanttChart chart) {
        this.chart = chart;
        this.ticks = GanttTimeline.monthTicks(chart.timelineStart(), chart.timelineEnd());

        setWidthFull();
        getStyle()
                .set("padding", "16px")
                .set("background", GanttStyle.CARD_BG)
                .set("border", "1px solid " + GanttStyle.BORDER)
                .set("border-radius", "12px")
                .set("box-shadow", "0 2px 8px rgba(31,42,48,0.08)")
                .set("box-sizing", "border-box")
                .set("overflow-x", "auto");

        if (chart.isEmpty()) {
            add(note("No data to display."));
            return;
        }

        Div content = new Div();
        content.getStyle().set("min-width", "960px");
        content.add(GanttTimeAxis.create(chart));

        for (GanttGroup group : chart.groups()) {
            content.add(group.collapsible() ? collapsibleGroup(group) : openGroup(group));
        }
        add(content);
    }

    // ── groups ────────────────────────────────────────────────────────────────────

    private Div collapsibleGroup(GanttGroup group) {
        Div header = row(group.label(), null, group.start(), group.end(), group.color(), null, 0, true);

        Div body = new Div();
        for (GanttRow r : group.rows()) {
            body.add(row(r.label(), r.key(), r.start(), r.end(), r.color(), r.status(), r.depth(), false));
        }
        return new CollapsibleSection(header, body, true);
    }

    private Div openGroup(GanttGroup group) {
        Div wrapper = new Div();
        wrapper.getStyle().set("margin-bottom", "6px");

        Div heading = row(group.label(), null, group.start(), group.end(), group.color(), null, 0, true);
        heading.getStyle().set("border-top", "2px solid " + GanttStyle.BORDER);
        wrapper.add(heading);

        for (GanttRow r : group.rows()) {
            wrapper.add(row(r.label(), r.key(), r.start(), r.end(), r.color(), r.status(), r.depth(), false));
        }
        return wrapper;
    }

    // ── one row: label cell + timeline track with a bar ─────────────────────────────

    private Div row(String label, String subLabel, LocalDate start, LocalDate end,
                    String color, String status, int depth, boolean bold) {
        Div labelCell = new Div(labelContent(label, subLabel, depth, bold));
        labelCell.getStyle()
                .set("width", GanttStyle.LABEL_COL_WIDTH)
                .set("flex-shrink", "0")
                .set("box-sizing", "border-box")
                .set("padding-right", "10px")
                .set("padding-left", (depth * 16) + "px")
                .set("display", "flex")
                .set("align-items", "center")
                .set("overflow", "hidden");

        Div track = track();
        if (start != null && end != null) {
            track.add(GanttBar.create(
                    chart.offsetPercent(start),
                    chart.widthPercent(start, end),
                    color,
                    GanttStyle.barTooltip(start, end, status)));
        }

        Div row = new Div(labelCell, track);
        row.getStyle()
                .set("display", "flex")
                .set("align-items", "stretch")
                .set("width", "100%")
                .set("min-height", GanttStyle.ROW_HEIGHT);
        return row;
    }

    private Div labelContent(String label, String subLabel, int depth, boolean bold) {
        Span text = new Span(label);
        text.getStyle()
                .set("font-size", depth == 0 ? "13px" : "12.5px")
                .set("font-weight", bold ? "700" : (depth == 1 ? "600" : "500"))
                .set("color", depth == 0 ? GanttStyle.PRIMARY_900 : GanttStyle.INK)
                .set("white-space", "nowrap")
                .set("overflow", "hidden")
                .set("text-overflow", "ellipsis");

        if (subLabel == null) {
            return new Div(text);
        }
        Span sub = new Span(subLabel);
        sub.getStyle().set("font-size", "11px").set("color", GanttStyle.MUTED).set("margin-left", "6px")
                .set("white-space", "nowrap");
        Div wrap = new Div(text, sub);
        wrap.getStyle().set("display", "flex").set("align-items", "baseline").set("min-width", "0")
                .set("overflow", "hidden");
        return wrap;
    }

    /** A timeline cell with month gridlines and the "today" marker. */
    private Div track() {
        Div track = new Div();
        track.getStyle()
                .set("position", "relative")
                .set("flex", "1")
                .set("box-sizing", "border-box")
                .set("border-bottom", "1px solid " + GanttStyle.GRIDLINE);

        for (GanttTimeline.Tick tick : ticks) {
            track.add(gridline(chart.offsetPercent(tick.monthStart())));
        }
        if (!today.isBefore(chart.timelineStart()) && !today.isAfter(chart.timelineEnd())) {
            track.add(todayLine(chart.offsetPercent(today)));
        }
        return track;
    }

    private Div gridline(double leftPercent) {
        Div line = new Div();
        line.getStyle()
                .set("position", "absolute")
                .set("top", "0")
                .set("bottom", "0")
                .set("left", leftPercent + "%")
                .set("width", "1px")
                .set("background", GanttStyle.GRIDLINE);
        return line;
    }

    private Div todayLine(double leftPercent) {
        Div line = new Div();
        line.getElement().setAttribute("title", "Today · " + GanttStyle.formatDate(today));
        line.getStyle()
                .set("position", "absolute")
                .set("top", "0")
                .set("bottom", "0")
                .set("left", leftPercent + "%")
                .set("width", "0")
                .set("border-left", "1px dashed " + GanttStyle.TODAY);
        return line;
    }

    private Span note(String text) {
        Span span = new Span(text);
        span.getStyle().set("color", GanttStyle.MUTED).set("font-style", "italic").set("font-size", "14px");
        return span;
    }
}

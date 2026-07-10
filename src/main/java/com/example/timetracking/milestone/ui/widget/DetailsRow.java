package com.example.timetracking.milestone.ui.widget;

import com.example.timetracking.milestone.ui.style.DashboardStyle;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

import java.util.Locale;

public class DetailsRow extends Div {

    /** 3-column template: label | bar | value. Used by Contributors tab. */
    static final String GRID_TEMPLATE = "1fr 160px 100px";

    /** 4-column template: label | status pill | bar | value. Used by Epics tab. */
    static final String GRID_TEMPLATE_STATUS = "1fr auto 160px 100px";

    /** Without status pill — used by the Contributors tab. */
    public DetailsRow(String label, double barPercent, String barText, String barColor) {
        this(label, barPercent, barText, barColor, null);
    }

    /** With status pill — used by the Epics tab. */
    public DetailsRow(String label, double barPercent, String barText, String barColor, Span statusPill) {
        boolean hasPill = statusPill != null;

        getStyle()
                .set("display", "grid")
                .set("grid-template-columns", hasPill ? GRID_TEMPLATE_STATUS : GRID_TEMPLATE)
                .set("align-items", "center")
                .set("width", "100%")
                .set("min-width", "0")
                .set("box-sizing", "border-box")
                .set("column-gap", "8px")
                .set("padding", "4px 0");

        // ── label ────────────────────────────────────────────────────────────────
        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("min-width", "0")
                .set("font-size", "14px")
                .set("font-weight", "500")
                .set("color", DashboardStyle.INK)
                .set("white-space", "normal")
                .set("overflow-wrap", "anywhere")
                .set("line-height", "1.3");

        // ── progress bar track ───────────────────────────────────────────────────
        Div track = new Div();
        track.getStyle()
                .set("height", "14px")
                .set("background", DashboardStyle.REMAINING)
                .set("border-radius", "4px")
                .set("overflow", "hidden");

        Div fill = new Div();
        double pct = Math.max(0.0, Math.min(100.0, barPercent));
        fill.getStyle()
                .set("width", String.format(Locale.US, "%.1f%%", pct))
                .set("height", "100%")
                .set("background", barColor)
                .set("border-radius", "4px");
        track.add(fill);

        // ── value text ───────────────────────────────────────────────────────────
        Span valueSpan = new Span(barText);
        valueSpan.getStyle()
                .set("font-size", "14px")
                .set("color", DashboardStyle.MUTED)
                .set("text-align", "right")
                .set("white-space", "nowrap");

        if (hasPill) {
            add(labelSpan, statusPill, track, valueSpan);
        } else {
            add(labelSpan, track, valueSpan);
        }
    }
}
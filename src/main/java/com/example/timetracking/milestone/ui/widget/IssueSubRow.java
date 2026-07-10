package com.example.timetracking.milestone.ui.widget;

import com.example.timetracking.milestone.ui.style.DashboardStyle;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

import java.util.Locale;

public class IssueSubRow extends Div {

    /** Without status pill. */
    public IssueSubRow(String label, double barPercent, String barText, String barColor) {
        this(label, barPercent, barText, barColor, null);
    }

    /** With status pill. */
    public IssueSubRow(String label, double barPercent, String barText, String barColor, Span statusPill) {
        boolean hasPill = statusPill != null;

        getStyle()
                .set("display", "grid")
                .set("grid-template-columns", hasPill ? DetailsRow.GRID_TEMPLATE_STATUS : DetailsRow.GRID_TEMPLATE)
                .set("align-items", "center")
                .set("width", "100%")
                .set("min-width", "0")
                .set("box-sizing", "border-box")
                .set("column-gap", "8px")
                .set("padding", "5px 0")
                .set("border-bottom", "1px solid #F0F2F4");

        // ── label ────────────────────────────────────────────────────────────────
        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("min-width", "0")
                .set("font-size", "13px")
                .set("color", DashboardStyle.INK)
                .set("white-space", "normal")
                .set("overflow-wrap", "anywhere")
                .set("line-height", "1.3");

        // ── progress bar track (inset left to distinguish from parent bar) ────────
        Div trackWrapper = new Div();
        trackWrapper.getStyle()
                .set("padding-left", "16px")
                .set("box-sizing", "border-box");

        Div track = new Div();
        track.getStyle()
                .set("height", "10px")
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
        trackWrapper.add(track);

        // ── value text ───────────────────────────────────────────────────────────
        Span valueSpan = new Span(barText);
        valueSpan.getStyle()
                .set("font-size", "13px")
                .set("color", DashboardStyle.MUTED)
                .set("text-align", "right")
                .set("white-space", "nowrap");

        if (hasPill) {
            add(labelSpan, statusPill, trackWrapper, valueSpan);
        } else {
            add(labelSpan, trackWrapper, valueSpan);
        }
    }
}
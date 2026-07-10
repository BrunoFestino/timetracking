package com.example.timetracking.milestone.ui.widget;

import com.example.timetracking.milestone.ui.style.DashboardStyle;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

import java.util.Locale;

public class IssueWorklogSubRow extends Div {

    public IssueWorklogSubRow(String issueKey, String issueSummary, long issueSeconds, long contributorTotalSeconds) {
        double pct = contributorTotalSeconds > 0 ? issueSeconds * 100.0 / contributorTotalSeconds : 0;
        String valueText = DashboardStyle.hours(issueSeconds) + "h";
        String label = issueSummary != null && !issueSummary.isBlank()
                ? issueKey + " - " + issueSummary
                : issueKey;

        getStyle()
                .set("display", "grid")
                .set("grid-template-columns", DetailsRow.GRID_TEMPLATE)
                .set("align-items", "center")
                .set("width", "100%")
                .set("min-width", "0")
                .set("box-sizing", "border-box")
                .set("column-gap", "8px")
                .set("padding", "5px 0")
                .set("border-bottom", "1px solid #F0F2F4");

        // ── issue key + name label ────────────────────────────────────────────────
        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("min-width", "0")
                .set("font-size", "13px")
                .set("color", DashboardStyle.INK)
                .set("white-space", "normal")
                .set("overflow-wrap", "anywhere")
                .set("line-height", "1.3");

        // ── bar (inset left, same as IssueSubRow) ────────────────────────────────
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
        double safePct = Math.max(0.0, Math.min(100.0, pct));
        fill.getStyle()
                .set("width", String.format(Locale.US, "%.1f%%", safePct))
                .set("height", "100%")
                .set("background", DashboardStyle.SPENT)
                .set("border-radius", "4px");
        track.add(fill);
        trackWrapper.add(track);

        // ── hours value ───────────────────────────────────────────────────────────
        Span valueSpan = new Span(valueText);
        valueSpan.getStyle()
                .set("font-size", "13px")
                .set("color", DashboardStyle.MUTED)
                .set("text-align", "right")
                .set("white-space", "nowrap");

        add(labelSpan, trackWrapper, valueSpan);
    }
}
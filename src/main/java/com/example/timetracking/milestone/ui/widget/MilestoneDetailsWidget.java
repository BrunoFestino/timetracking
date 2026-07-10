package com.example.timetracking.milestone.ui.widget;

import com.example.timetracking.milestone.application.dto.MilestoneSummary;
import com.example.timetracking.milestone.application.dto.Progress;
import com.example.timetracking.milestone.ui.style.DashboardStyle;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

public class MilestoneDetailsWidget extends VerticalLayout {

    private static final long ON_ROUTE_THRESHOLD_PERCENT = 70;
    private static final String DETAILS_WIDTH = "360px";

    private final H3 title = new H3();
    private final HorizontalLayout chartRow = new HorizontalLayout();
    private final Span spentVsEstimated = new Span();
    private final Span status = new Span();

    public MilestoneDetailsWidget(MilestoneSummary summary) {
        this(new Progress(summary.key(), summary.name(), summary.estimatedSeconds(), 0, List.of(), List.of()),
                parseDate(summary.startDate()),
                parseDate(summary.baselineDeliveryDate()),
                parseDate(summary.effectiveDeliveryDate()),
                parseDate(summary.dueDate()));
    }

    private MilestoneDetailsWidget(Progress progress, LocalDate startDate,
                                   LocalDate baselineDate, LocalDate effectiveDate, LocalDate dueDate) {
        setPadding(false);
        setSpacing(false);
        setWidth(DETAILS_WIDTH);
        setAlignItems(FlexComponent.Alignment.CENTER);
        getStyle()
                .set("gap", "6px")
                .set("text-align", "center")
                .set("padding", "16px")
                .set("background", "#FFFFFF")
                .set("border", "1px solid #E5E8EA")
                .set("border-radius", "12px")
                .set("box-shadow", "0 2px 8px rgba(31,42,48,0.08)")
                .set("box-sizing", "border-box");

        title.getStyle()
                .set("margin", "0")
                .set("width", "100%")
                .set("color", DashboardStyle.INK)
                .set("font-weight", "600")
                .set("white-space", "normal")
                .set("overflow-wrap", "anywhere")
                .set("line-height", "1.25");

        spentVsEstimated.getStyle()
                .set("color", DashboardStyle.MUTED)
                .set("font-size", "14px")
                .set("margin-top", "2px");

        chartRow.setPadding(false);
        chartRow.setSpacing(false);
        chartRow.setAlignItems(FlexComponent.Alignment.CENTER);

        // ── date rows (static — dates don't change after construction) ────────────
        Div datesBox = buildDatesBox(startDate, baselineDate, effectiveDate, dueDate);

        add(title, chartRow, spentVsEstimated, status, datesBox);
        update(progress);
    }

    public void showProgress(Progress progress) {
        if (progress == null) {
            return;
        }
        update(progress);
    }

    private void update(Progress progress) {
        title.setText(progress.name());
        spentVsEstimated.setText(String.format(Locale.US,
                "%s MD / %s MD",
                DashboardStyle.manDays(progress.spentSeconds()),
                DashboardStyle.manDays(progress.estimatedSeconds())));

        status.setText(statusText(progress.displayPercent()));
        String statusColor = statusColor(progress.displayPercent());
        status.getStyle()
                .set("display", "inline-block")
                .set("padding", "4px 10px")
                .set("border-radius", "999px")
                .set("font-size", "12px")
                .set("font-weight", "600")
                .set("border", "1px solid " + statusColor)
                .set("color", statusColor);

        chartRow.removeAll();
        chartRow.add(new RingChart(progress, DashboardStyle.MILESTONE));
    }

    // ── date box ────────────────────────────────────────────────────────────────

    private static Div buildDatesBox(LocalDate startDate, LocalDate baselineDate,
                                     LocalDate effectiveDate, LocalDate dueDate) {

        // effective end: prefer effectiveDate, then baselineDate, then dueDate
        LocalDate endDate = effectiveDate != null ? effectiveDate
                : baselineDate != null ? baselineDate
                  : dueDate;

        boolean isPast = endDate != null && LocalDate.now().isAfter(endDate);

        Div box = new Div();
        box.getStyle()
                .set("width", "100%")
                .set("box-sizing", "border-box")
                .set("border", "1px solid #E5E8EA")
                .set("border-radius", "8px")
                .set("padding", "8px 12px")
                .set("background", "#F8FAFB")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "4px");

        box.add(dateRow("Start", formatDate(startDate)));

        // show both deadline dates when they differ and at least one is set
        if (baselineDate != null && effectiveDate != null && !baselineDate.equals(effectiveDate)) {
            box.add(dateRow("Baseline deadline", formatDate(baselineDate)));
            box.add(dateRow("Effective deadline", formatDate(effectiveDate)));
        } else {
            // only one (or both equal) — show a single "End" row
            box.add(dateRow("End", formatDate(endDate)));
        }

        if (!isPast) {
            box.add(dateRow("Remaining", remainingText(endDate)));
        }

        return box;
    }

    private static Div dateRow(String label, String value) {
        Div row = new Div();
        row.getStyle()
                .set("display", "flex")
                .set("justify-content", "space-between")
                .set("align-items", "baseline")
                .set("width", "100%");

        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("font-size", "12px")
                .set("font-weight", "500")
                .set("color", DashboardStyle.MUTED);

        Span valueSpan = new Span(value);
        valueSpan.getStyle()
                .set("font-size", "12px")
                .set("color", DashboardStyle.INK)
                .set("text-align", "right");

        row.add(labelSpan, valueSpan);
        return row;
    }

    // ── helpers ─────────────────────────────────────────────────────────────────

    private static String statusText(long percentSpent) {
        if (percentSpent > 100) {
            return "Overspent";
        }
        return percentSpent > ON_ROUTE_THRESHOLD_PERCENT ? "At risk" : "On route";
    }

    private static String statusColor(long percentSpent) {
        if (percentSpent > 100) {
            return DashboardStyle.OVER;
        }
        return percentSpent > ON_ROUTE_THRESHOLD_PERCENT ? DashboardStyle.AT_RISK : DashboardStyle.ON_TRACK;
    }

    private static LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() >= 10) {
            trimmed = trimmed.substring(0, 10);
        }
        try {
            return LocalDate.parse(trimmed);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private static String formatDate(LocalDate date) {
        return date == null ? "-" : date.toString();
    }

    private static String remainingText(LocalDate endDate) {
        if (endDate == null) {
            return "-";
        }
        long days = ChronoUnit.DAYS.between(LocalDate.now(), endDate);
        if (days >= 0) {
            return days + " days";
        }
        return "Overdue by " + Math.abs(days) + " days";
    }
}
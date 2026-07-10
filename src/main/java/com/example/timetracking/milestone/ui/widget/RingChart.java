package com.example.timetracking.milestone.ui.widget;

import com.example.timetracking.milestone.application.dto.Progress;
import com.example.timetracking.milestone.ui.style.DashboardStyle;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

import java.util.Locale;

class RingChart extends Div {

    RingChart(Progress progress, String accent) {
        int size = 230;
        int cx = size / 2;
        int cy = size / 2;
        int radius = 82;
        int stroke = 28;
        double circumference = 2 * Math.PI * radius;
        double spentArc = progress.fractionSpent() * circumference;
        String arcColor = progress.isOverBudget() ? DashboardStyle.OVER : accent;

        String svg = String.format(Locale.US,
                "<svg xmlns='http://www.w3.org/2000/svg' width='%d' height='%d'>"
                        + "<circle cx='%d' cy='%d' r='%d' fill='none' stroke='%s' stroke-width='%d'/>"
                        + "<circle cx='%d' cy='%d' r='%d' fill='none' stroke='%s' stroke-width='%d' "
                        + "stroke-dasharray='%.2f %.2f' stroke-linecap='round' transform='rotate(-90 %d %d)'/>"
                        + "<text x='%d' y='%d' text-anchor='middle' font-family='%s' font-size='40' "
                        + "font-weight='700' fill='%s'>%d%%</text>"
                        + "<text x='%d' y='%d' text-anchor='middle' font-family='%s' font-size='13' fill='%s'>spent</text>"
                        + "</svg>",
                size, size,
                cx, cy, radius, DashboardStyle.REMAINING, stroke,
                cx, cy, radius, arcColor, stroke, spentArc, circumference - spentArc, cx, cy,
                cx, cy + 6, DashboardStyle.SVG_FONT, DashboardStyle.INK, progress.displayPercent(),
                cx, cy + 28, DashboardStyle.SVG_FONT, DashboardStyle.MUTED);

        add(DashboardStyle.svgImage(svg, "Time spent vs. remaining"));

        if (progress.isOverBudget()) {
            long overBy = progress.spentSeconds() - progress.estimatedSeconds();
            Span warn = new Span("Over budget by " + DashboardStyle.manDays(overBy) + " md");
            warn.getStyle().set("color", DashboardStyle.OVER)
                    .set("font-weight", "600").set("font-size", "14px");
            add(new Div(warn));
        }
    }
}
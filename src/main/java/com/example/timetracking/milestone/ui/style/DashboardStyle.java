package com.example.timetracking.milestone.ui.style;

import com.example.timetracking.milestone.application.dto.Progress;
import com.example.timetracking.milestone.domain.TimeConstants;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;

public final class DashboardStyle {
    public static final long SECONDS_PER_HOUR = TimeConstants.SECONDS_PER_HOUR;
    public static final long SECONDS_PER_MAN_DAY = TimeConstants.SECONDS_PER_MAN_DAY;
    public static final String FONT = "Inter, 'Helvetica Neue', Arial, system-ui, sans-serif";
    public static final String SVG_FONT = "Inter, Arial, sans-serif";
    public static final String PRIMARY_900 = "#0F4660";
    public static final String INK = "#1F2A30";
    public static final String MUTED = "#5F6B72";
    public static final String SPENT = "#2C8FB5";
    public static final String REMAINING = "#E5E8EA";
    public static final String OVER = "#D64545";
    public static final String MILESTONE = "#0F4660"; // deep navy — milestone ring & bar
    public static final String EPIC = "#6554C0";      // purple — epic bars
    // SPENT (blue) is reused for issues, tasks, subtasks and contributor bars
    public static final String AT_RISK = "#B36A00";
    public static final String ON_TRACK = "#2E7D32";

    // CSS property name constants
    public static final String PROP_COLOR = "color";
    public static final String PROP_DISPLAY = "display";
    public static final String PROP_FONT_SIZE = "font-size";
    public static final String PROP_FONT_WEIGHT = "font-weight";
    public static final String PROP_MARGIN_TOP = "margin-top";
    public static final String PROP_WIDTH = "width";

    private DashboardStyle() {
    }

    public static Image svgImage(String svg, String alt) {
        String encoded = Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));
        return new Image("data:image/svg+xml;base64," + encoded, alt);
    }

    public static Span note(String text) {
        Span span = new Span(text);
        span.getStyle().set(PROP_COLOR, MUTED).set("font-style", "italic").set(PROP_FONT_SIZE, "14px");
        return span;
    }

    public static String manDays(long seconds) {
        return String.format(Locale.US, "%.1f", seconds / (double) SECONDS_PER_MAN_DAY);
    }

    public static String hours(long seconds) {
        return String.format(Locale.US, "%.1f", seconds / (double) SECONDS_PER_HOUR);
    }

    /**
     * Returns a compact status pill {@link Span} for the given progress.
     */
    public static Span statusPill(Progress progress) {
        String text;
        String color;
        if (progress.estimatedSeconds() <= 0) {
            text = "Not Estimated";
            color = MUTED;
        } else if (progress.isOverBudget()) {
            text = "Overspent";
            color = OVER;
        } else if (progress.displayPercent() > 70) {
            text = "At risk";
            color = AT_RISK;
        } else {
            text = "On track";
            color = ON_TRACK;
        }
        return pill(text, color);
    }

    /**
     * Returns a compact pill/chip {@link Span} with the given text and accent colour.
     */
    public static Span pill(String text, String color) {
        Span pill = new Span(text);
        pill.getStyle()
                .set("font-size", "11px")
                .set("font-weight", "600")
                .set("white-space", "nowrap")
                .set("padding", "2px 7px")
                .set("border-radius", "999px")
                .set("border", "1px solid " + color)
                .set("color", color);
        return pill;
    }
}

package com.example.timetracking.velocity.ui.widget;

import com.example.timetracking.milestone.ui.style.DashboardStyle;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;

import java.util.List;
import java.util.function.LongFunction;

/**
 * The effort-over-time chart: one line per milestone showing how much effort was logged in each
 * fixed window since the milestone started, so the <em>shape</em> of a delivery — ramp-up, a
 * crunch near the end, idle stretches — reads at a glance, and two milestones can be compared on
 * cadence rather than just on totals.
 *
 * <p>Milestones are aligned by <em>days since their own start</em> (not the calendar), so runs
 * that happened at different times overlay directly. Each line carries its milestone's palette
 * colour, matching the bars and the comparison table.
 *
 * <p>Rendered as an inline SVG via {@link DashboardStyle#svgImage} — the one path to true line
 * geometry and axes in this feature, which has no charting library — with a plain-DOM legend and
 * axis captions around it.
 */
public class EffortTimelineChart extends Div {

    /** One milestone's line: its key, its dense per-window effort (seconds), and its colour. */
    public record Series(String key, List<Long> bucketSeconds, String color) {
    }

    private static final int W = 720;
    private static final int H = 260;
    private static final int ML = 54;
    private static final int MR = 18;
    private static final int MT = 14;
    private static final int MB = 30;
    private static final int PLOT_W = W - ML - MR;
    private static final int PLOT_H = H - MT - MB;
    private static final int BASE_Y = MT + PLOT_H;

    /**
     * @param series    one line per milestone (dense, gap-filled effort windows)
     * @param bucketDays width of each window in days, for the x-axis labels
     * @param format    seconds → display string (MD/hours), for the y-axis labels
     */
    public EffortTimelineChart(List<Series> series, int bucketDays, LongFunction<String> format) {
        setWidthFull();

        int maxBuckets = series.stream().mapToInt(s -> s.bucketSeconds().size()).max().orElse(0);
        long maxSeconds = series.stream()
                .flatMap(s -> s.bucketSeconds().stream())
                .mapToLong(Long::longValue)
                .max().orElse(0L);

        Image chart = DashboardStyle.svgImage(
                buildSvg(series, maxBuckets, Math.max(1L, maxSeconds), bucketDays, format),
                "Effort logged per " + bucketDays + "-day window since start");
        chart.getStyle().set("width", "100%").set("max-width", W + "px").set("height", "auto")
                .set("display", "block");

        add(chart, legend(series));
    }

    private String buildSvg(List<Series> series, int maxBuckets, long maxSeconds,
                            int bucketDays, LongFunction<String> format) {
        int denom = Math.max(1, maxBuckets - 1);
        int maxDay = Math.max(0, maxBuckets - 1) * bucketDays;

        StringBuilder svg = new StringBuilder(2048);
        svg.append("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 ").append(W).append(' ').append(H)
                .append("' font-family='").append(DashboardStyle.SVG_FONT).append("'>");

        // horizontal gridlines + y-axis labels at 0 / 50% / 100%
        for (double frac : new double[]{0.0, 0.5, 1.0}) {
            int y = (int) Math.round(MT + PLOT_H * (1 - frac));
            svg.append("<line x1='").append(ML).append("' y1='").append(y)
                    .append("' x2='").append(ML + PLOT_W).append("' y2='").append(y)
                    .append("' stroke='").append(DashboardStyle.REMAINING).append("' stroke-width='1'/>");
            svg.append("<text x='").append(ML - 8).append("' y='").append(y + 3)
                    .append("' text-anchor='end' font-size='10' fill='").append(DashboardStyle.MUTED)
                    .append("'>").append(esc(format.apply(Math.round(maxSeconds * frac)))).append("</text>");
        }

        // x-axis day labels at start / mid / end
        appendXLabel(svg, ML, "0d", "start");
        appendXLabel(svg, ML + PLOT_W / 2, (maxDay / 2) + "d", "middle");
        appendXLabel(svg, ML + PLOT_W, maxDay + "d", "end");

        for (Series s : series) {
            appendSeries(svg, s, denom, maxSeconds);
        }

        svg.append("</svg>");
        return svg.toString();
    }

    private void appendSeries(StringBuilder svg, Series s, int denom, long maxSeconds) {
        List<Long> values = s.bucketSeconds();
        if (values.isEmpty()) {
            return;
        }
        StringBuilder points = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            int x = (int) Math.round(ML + (values.size() == 1 ? 0 : (double) i / denom) * PLOT_W);
            int y = (int) Math.round(BASE_Y - (values.get(i) / (double) maxSeconds) * PLOT_H);
            points.append(x).append(',').append(y).append(' ');
        }
        String trimmed = points.toString().trim();
        if (values.size() == 1) {
            String[] xy = trimmed.split(",");
            svg.append("<circle cx='").append(xy[0]).append("' cy='").append(xy[1])
                    .append("' r='3.5' fill='").append(s.color()).append("'/>");
            return;
        }
        svg.append("<polyline points='").append(trimmed)
                .append("' fill='none' stroke='").append(s.color())
                .append("' stroke-width='2' stroke-linejoin='round' stroke-linecap='round'/>");
        // small vertices so short runs stay legible
        for (String pair : trimmed.split(" ")) {
            String[] xy = pair.split(",");
            svg.append("<circle cx='").append(xy[0]).append("' cy='").append(xy[1])
                    .append("' r='2.2' fill='").append(s.color()).append("'/>");
        }
    }

    private void appendXLabel(StringBuilder svg, int x, String text, String anchor) {
        svg.append("<text x='").append(x).append("' y='").append(H - 8)
                .append("' text-anchor='").append(anchor).append("' font-size='10' fill='")
                .append(DashboardStyle.MUTED).append("'>").append(esc(text)).append("</text>");
    }

    /** Colour-swatch + milestone-key legend, in plain DOM below the SVG. */
    private Div legend(List<Series> series) {
        Div legend = new Div();
        legend.getStyle().set("display", "flex").set("flex-wrap", "wrap").set("gap", "12px")
                .set("margin-top", "8px").set("justify-content", "center");
        for (Series s : series) {
            Span swatch = new Span();
            swatch.getStyle().set("display", "inline-block").set("width", "10px").set("height", "10px")
                    .set("border-radius", "2px").set("background", s.color()).set("margin-right", "5px");

            Span label = new Span(s.key());
            label.getStyle().set("font-size", "12px").set("color", DashboardStyle.INK);

            Div item = new Div(swatch, label);
            item.getStyle().set("display", "flex").set("align-items", "center");
            legend.add(item);
        }
        return legend;
    }

    /** Minimal XML-escaping for label text placed inside the SVG string. */
    private static String esc(String text) {
        return text == null ? "" : text
                .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}

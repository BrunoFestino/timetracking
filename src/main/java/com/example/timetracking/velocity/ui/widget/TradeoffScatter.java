package com.example.timetracking.velocity.ui.widget;

import com.example.timetracking.milestone.ui.style.DashboardStyle;
import com.example.timetracking.velocity.ui.style.VelocityStyles;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;

import java.util.List;

/**
 * The delivery trade-off at a glance: each milestone is a dot placed by <em>how long it took</em>
 * (x, left = faster) against <em>how much effort it cost</em> (y, higher = more), so the classic
 * quadrants read instantly — fast-and-cheap sits bottom-left, slow-and-expensive top-right — and
 * two milestones show their trade-off (e.g. "faster but costlier") as a diagonal rather than as two
 * separate numbers.
 *
 * <p>Pure CSS, same approach as {@link ComparisonBarChart}/{@link Sparkline} (no charting library,
 * and no SVG-as-image): a relative plot box with absolutely-positioned dots. Each dot carries its
 * milestone's palette colour, matching the other charts and the comparison table. Positions are
 * normalised to the selection's own min/max on each axis, with padding so dots never touch the
 * edges; milestones without a measurable duration can't be placed and are listed underneath.
 */
public class TradeoffScatter extends Div {

    /** One milestone as a point: its key, duration (days), effort (seconds), colour and hover text. */
    public record Point(String key, int durationDays, long effortSeconds, String color, String tooltip) {
    }

    private static final int PLOT_H_PX = 210;
    /** Dots map into this inset band on each axis (percent), leaving margin so labels don't clip. */
    private static final double LO = 12.0;
    private static final double HI = 88.0;

    public TradeoffScatter(List<Point> points) {
        setWidthFull();

        List<Point> plotted = points.stream().filter(p -> p.durationDays() > 0).toList();
        if (plotted.isEmpty()) {
            add(VelocityStyles.emptyState(VaadinIcon.CHART, "No dated milestones to plot",
                    "The trade-off view needs at least one milestone with a start and delivery date."));
            return;
        }

        int minDur = plotted.stream().mapToInt(Point::durationDays).min().orElse(0);
        int maxDur = plotted.stream().mapToInt(Point::durationDays).max().orElse(0);
        long minEff = plotted.stream().mapToLong(Point::effortSeconds).min().orElse(0L);
        long maxEff = plotted.stream().mapToLong(Point::effortSeconds).max().orElse(0L);

        Div plot = new Div();
        plot.getStyle()
                .set("position", "relative")
                .set("height", PLOT_H_PX + "px")
                .set("width", "100%")
                .set("box-sizing", "border-box")
                .set("border-left", "1px solid " + DashboardStyle.REMAINING)
                .set("border-bottom", "1px solid " + DashboardStyle.REMAINING);

        // y-axis caption (effort grows upward), pinned top-left inside the plot
        Span yCaption = new Span("↑ more effort");
        yCaption.getStyle()
                .set("position", "absolute").set("top", "2px").set("left", "6px")
                .set("font-size", "10px").set("color", DashboardStyle.MUTED);
        plot.add(yCaption);

        for (Point p : plotted) {
            double xPct = LO + (HI - LO) * norm(p.durationDays() - minDur, maxDur - minDur);
            double yPct = LO + (HI - LO) * norm(p.effortSeconds() - minEff, maxEff - minEff);

            Div marker = new Div();
            marker.getStyle().set("position", "absolute")
                    .set("left", fmt(xPct) + "%").set("bottom", fmt(yPct) + "%");
            marker.getElement().setAttribute("title", p.tooltip());

            Span dot = new Span();
            dot.getStyle()
                    .set("position", "absolute").set("left", "0").set("bottom", "0")
                    .set("width", "11px").set("height", "11px").set("border-radius", "50%")
                    .set("background", p.color())
                    .set("border", "2px solid #FFFFFF")
                    .set("box-shadow", "0 0 0 1px " + p.color())
                    .set("transform", "translate(-50%, 50%)");

            Span label = new Span(p.key());
            label.getStyle()
                    .set("position", "absolute").set("left", "10px").set("bottom", "0")
                    .set("transform", "translate(0, 50%)")
                    .set("font-size", "11px").set("font-weight", "600")
                    .set("color", DashboardStyle.INK).set("white-space", "nowrap");

            marker.add(dot, label);
            plot.add(marker);
        }

        add(plot, xCaptions());

        List<String> undated = points.stream()
                .filter(p -> p.durationDays() <= 0)
                .map(Point::key)
                .toList();
        if (!undated.isEmpty()) {
            Span note = new Span("Not dated: " + String.join(", ", undated));
            note.getStyle().set("font-size", "11px").set("color", DashboardStyle.MUTED)
                    .set("margin-top", "6px").set("display", "block");
            add(note);
        }
    }

    /** Bottom row: duration grows left → right, so the left end is the faster deliveries. */
    private Div xCaptions() {
        Span faster = new Span("← faster");
        faster.getStyle().set("font-size", "10px").set("color", DashboardStyle.MUTED);
        Span slower = new Span("slower →");
        slower.getStyle().set("font-size", "10px").set("color", DashboardStyle.MUTED);

        Div row = new Div(faster, slower);
        row.getStyle()
                .set("display", "flex").set("justify-content", "space-between")
                .set("margin-top", "4px").set("width", "100%");
        return row;
    }

    /** Position within [0,1] of {@code value} across a span; centred when the span is zero. */
    private static double norm(long value, long span) {
        return span > 0 ? (double) value / span : 0.5;
    }

    private static String fmt(double pct) {
        return String.format(java.util.Locale.US, "%.1f", pct);
    }
}

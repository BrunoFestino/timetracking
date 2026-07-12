package com.example.timetracking.velocity.ui.widget;

import com.example.timetracking.milestone.ui.style.DashboardStyle;
import com.vaadin.flow.component.html.Div;

import java.util.List;

/**
 * A compact weekly-trend chart: one vertical bar per week, height proportional to the
 * effort logged that week. Shows the shape of a milestone's velocity (ramp-up, peak,
 * tail-off) at a glance. Weeks without work render as a gap; a baseline marks the axis.
 */
public class Sparkline extends Div {

    /**
     * @param values   effort per relative week, in order 1..N (seconds)
     * @param tooltips per-bar hover text, same size and order as {@code values}
     * @param color    bar colour
     */
    public Sparkline(List<Long> values, List<String> tooltips, String color) {
        long max = values.stream().mapToLong(Long::longValue).max().orElse(1L);
        getStyle()
                .set("display", "flex")
                .set("align-items", "flex-end")
                .set("gap", "3px")
                .set("height", "40px")
                .set("width", "100%")
                .set("box-sizing", "border-box")
                .set("border-bottom", "1px solid " + DashboardStyle.REMAINING);

        for (int i = 0; i < values.size(); i++) {
            long v = values.get(i);
            double pct = max > 0 ? v * 100.0 / max : 0;
            double height = v > 0 ? Math.max(10.0, pct) : 0;

            Div bar = new Div();
            bar.getStyle()
                    .set("flex", "0 1 12px")
                    .set("min-width", "3px")
                    .set("height", height + "%")
                    .set("background", v > 0 ? color : "transparent")
                    .set("border-radius", "2px 2px 0 0");
            if (i < tooltips.size()) {
                bar.getElement().setAttribute("title", tooltips.get(i));
            }
            add(bar);
        }
    }
}

package com.example.timetracking.velocity.ui.widget;

import com.example.timetracking.milestone.ui.style.DashboardStyle;
import com.example.timetracking.velocity.ui.style.VelocityStyles;
import com.vaadin.flow.component.html.Div;

import java.util.List;

/**
 * A compact weekly-trend chart: one vertical bar per week, height proportional to the
 * effort logged that week. Shows the shape of a milestone's velocity (ramp-up, peak,
 * tail-off) at a glance. The peak week is emphasised in the primary navy; weeks without
 * work render as a low neutral stub so "zero" reads distinctly from a layout gap.
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
            boolean peak = v > 0 && v == max;

            Div bar = new Div();
            bar.addClassName(VelocityStyles.BAR_CLASS);
            bar.getStyle()
                    .set("flex", "0 1 12px")
                    .set("min-width", "3px")
                    .set("border-radius", "2px 2px 0 0");
            if (v > 0) {
                bar.getStyle()
                        .set("height", Math.max(10.0, pct) + "%")
                        .set("background", peak ? DashboardStyle.PRIMARY_900 : color);
            } else {
                bar.getStyle()
                        .set("height", "2px")
                        .set("background", DashboardStyle.REMAINING);
            }
            if (i < tooltips.size()) {
                bar.getElement().setAttribute("title", tooltips.get(i));
            }
            add(bar);
        }
    }
}

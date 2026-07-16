package com.example.timetracking.velocity.ui.style;

import com.example.timetracking.milestone.ui.style.DashboardStyle;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;

/**
 * Velocity-view styling helpers: an injected stylesheet for the states inline styles
 * can't express (hover, media queries), plus the reusable empty-state and KPI-tile
 * building blocks. Same injected-&lt;style&gt; technique as the milestone view's
 * responsive styles; all class names are prefixed {@code vel-} to stay scoped.
 */
public final class VelocityStyles {

    /** Class hooked to chart bars: brightens them on hover. */
    public static final String BAR_CLASS = "vel-bar";
    /** Class hooked to collapsible-section headers: soft background on hover. */
    public static final String COLLAPSE_HEADER_CLASS = "vel-collapse-header";
    /** Class for the KPI tile row: wrapping flex layout. */
    public static final String KPI_ROW_CLASS = "vel-kpi-row";
    /** Class for the collapsible chevron: rotation transition. */
    public static final String CHEVRON_CLASS = "vel-chevron";

    private static final String TILE_BG = "#F8FAFB";

    private VelocityStyles() {
    }

    /** Appends the velocity stylesheet to the given view. Call once per view instance. */
    public static void injectStyles(HasElement view) {
        Div styleHolder = new Div();
        styleHolder.getElement().setProperty("innerHTML",
                "<style id='velocity-styles'>"
                        + "." + BAR_CLASS + " { transition: filter 0.12s ease; }"
                        + "." + BAR_CLASS + ":hover { filter: brightness(1.12); }"
                        + "." + COLLAPSE_HEADER_CLASS + " { border-radius: 8px; transition: background 0.12s ease; }"
                        + "." + COLLAPSE_HEADER_CLASS + ":hover { background: #F4F7F8; }"
                        + "." + KPI_ROW_CLASS + " { display: flex; flex-wrap: wrap; gap: 12px; }"
                        + "@media (max-width: 700px) {"
                        + "  ." + KPI_ROW_CLASS + " > * { flex: 1 1 calc(50% - 12px) !important; }"
                        + "}"
                        + "." + CHEVRON_CLASS + " { transition: transform 0.15s ease; }"
                        + "</style>");
        view.getElement().appendChild(styleHolder.getElement());
    }

    /** A centered empty/error state: muted icon, short title and an actionable hint. */
    public static Div emptyState(VaadinIcon icon, String title, String hint) {
        Icon iconComponent = icon.create();
        iconComponent.setSize("28px");
        iconComponent.getStyle().set("color", DashboardStyle.MUTED).set("opacity", "0.55");

        Span titleSpan = new Span(title);
        titleSpan.getStyle()
                .set("font-size", "14px")
                .set("font-weight", "600")
                .set("color", DashboardStyle.INK);

        Span hintSpan = new Span(hint);
        hintSpan.getStyle().set("font-size", "13px").set("color", DashboardStyle.MUTED);

        Div state = new Div(iconComponent, titleSpan, hintSpan);
        state.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("align-items", "center")
                .set("gap", "6px")
                .set("padding", "28px 16px")
                .set("text-align", "center")
                .set("width", "100%")
                .set("box-sizing", "border-box");
        return state;
    }

    /** A summary stat tile: big value on top, small uppercase label underneath. */
    public static Div kpiTile(String label, String value, boolean primary) {
        Span valueSpan = new Span(value);
        valueSpan.getStyle()
                .set("font-size", value.length() > 12 ? "15px" : "22px")
                .set("font-weight", "700")
                .set("line-height", "1.3")
                .set("color", primary ? DashboardStyle.PRIMARY_900 : DashboardStyle.INK)
                .set("white-space", "nowrap");

        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("font-size", "11px")
                .set("font-weight", "600")
                .set("color", DashboardStyle.MUTED)
                .set("text-transform", "uppercase")
                .set("letter-spacing", "0.04em")
                .set("white-space", "nowrap");

        Div tile = new Div(valueSpan, labelSpan);
        tile.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("justify-content", "center")
                .set("gap", "2px")
                .set("flex", "1 1 120px")
                .set("min-width", "110px")
                .set("padding", "10px 14px")
                .set("border-radius", "8px")
                .set("box-sizing", "border-box");
        if (primary) {
            tile.getStyle()
                    .set("background", "#FFFFFF")
                    .set("border", "1px solid " + DashboardStyle.PRIMARY_900 + "33");
        } else {
            tile.getStyle().set("background", TILE_BG);
        }
        return tile;
    }
}

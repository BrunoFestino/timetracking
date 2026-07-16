package com.example.timetracking.velocity.ui.widget;

import com.example.timetracking.velocity.ui.style.VelocityStyles;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.dom.DomEventListener;

/**
 * Click-to-expand section (chevron + header + hidden body), used by the velocity
 * view. Supports rendering the body expanded from the start via
 * {@link #CollapsibleSection(Div, Component, boolean)} so the per-milestone weekly
 * charts show open without a click.
 */
public class CollapsibleSection extends Div {

    /** Width of the chevron column. Body left-padding must match to keep columns aligned. */
    static final String CHEVRON_WIDTH = "20px";

    private final Div body;
    private final Icon chevron;
    private boolean open = false;

    public CollapsibleSection(Div header, Component bodyContent) {
        this(header, bodyContent, false);
    }

    public CollapsibleSection(Div header, Component bodyContent, boolean initiallyOpen) {
        getStyle()
                .set("width", "100%")
                .set("box-sizing", "border-box")
                .set("margin-bottom", "8px");

        // ── chevron ───────────────────────────────────────────────────────────────
        chevron = VaadinIcon.ANGLE_RIGHT.create();
        chevron.addClassName(VelocityStyles.CHEVRON_CLASS);
        chevron.setSize("14px");
        chevron.getStyle()
                .set("color", "#5F6B72")
                .set("transform", "rotate(0deg)");

        Div chevronBox = new Div(chevron);
        chevronBox.getStyle()
                .set("flex-shrink", "0")
                .set("width", CHEVRON_WIDTH)
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center");

        // ── header wrapper (chevron + header row) ─────────────────────────────────
        Div headerWrap = new Div(chevronBox, header);
        headerWrap.addClassName(VelocityStyles.COLLAPSE_HEADER_CLASS);
        headerWrap.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("width", "100%")
                .set("box-sizing", "border-box")
                .set("padding", "3px 6px 3px 0")
                .set("cursor", "pointer")
                .set("user-select", "none");

        // ── body (hidden by default, indented to align sub-row grid columns) ──────
        body = new Div(bodyContent);
        body.getStyle()
                .set("display", "none")
                .set("padding-left", CHEVRON_WIDTH)
                .set("padding-top", "2px")
                .set("padding-bottom", "4px")
                .set("width", "100%")
                .set("box-sizing", "border-box");

        // ── toggle ────────────────────────────────────────────────────────────────
        DomEventListener toggle = e -> setOpen(!open);
        headerWrap.getElement().addEventListener("click", toggle);

        add(headerWrap, body);
        if (initiallyOpen) {
            setOpen(true);
        }
    }

    private void setOpen(boolean open) {
        this.open = open;
        body.getStyle().set("display", open ? "block" : "none");
        chevron.getStyle().set("transform", open ? "rotate(90deg)" : "rotate(0deg)");
    }
}

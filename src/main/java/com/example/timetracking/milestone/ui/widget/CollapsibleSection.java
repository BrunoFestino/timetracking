package com.example.timetracking.milestone.ui.widget;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.dom.DomEventListener;

public class CollapsibleSection extends Div {

    /** Width of the chevron column. Body left-padding must match to keep columns aligned. */
    static final String CHEVRON_WIDTH = "20px";

    private final Div body;
    private final Div chevron;
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
        chevron = new Div();
        chevron.getStyle()
                .set("flex-shrink", "0")
                .set("width", CHEVRON_WIDTH)
                .set("font-size", "12px")
                .set("line-height", "1")
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("color", "#5F6B72")
                .set("transition", "transform 0.15s ease")
                .set("transform", "rotate(0deg)");
        chevron.setText("▶️");

        // ── header wrapper (chevron + header row) ─────────────────────────────────
        Div headerWrap = new Div(chevron, header);
        headerWrap.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("width", "100%")
                .set("box-sizing", "border-box")
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
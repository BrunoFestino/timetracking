package com.example.timetracking.gantt.ui.widget;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.dom.DomEventListener;

/**
 * Click-to-expand section (chevron + header + body), used by the Gantt's "By team" mode to
 * make each milestone collapsible.
 *
 * <p>Local copy in the {@code gantt} feature (the velocity feature keeps its own copy too),
 * so the module does not depend on widgets from other features.
 */
public class CollapsibleSection extends Div {

    private final Div body;
    private final Div chevron;
    private boolean open;

    public CollapsibleSection(Div header, Component bodyContent, boolean initiallyOpen) {
        getStyle().set("width", "100%").set("box-sizing", "border-box").set("margin-bottom", "4px");

        chevron = new Div();
        chevron.getStyle()
                .set("flex-shrink", "0")
                .set("width", "20px")
                .set("font-size", "12px")
                .set("line-height", "1")
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("color", "#5F6B72")
                .set("transition", "transform 0.15s ease")
                .set("transform", "rotate(0deg)");
        chevron.setText("▶");

        Div headerWrap = new Div(chevron, header);
        headerWrap.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("width", "100%")
                .set("box-sizing", "border-box")
                .set("cursor", "pointer")
                .set("user-select", "none");
        header.getStyle().set("flex", "1").set("min-width", "0");

        body = new Div(bodyContent);
        body.getStyle()
                .set("display", "none")
                .set("width", "100%")
                .set("box-sizing", "border-box");

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

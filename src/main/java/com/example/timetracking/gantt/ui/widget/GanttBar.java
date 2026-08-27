package com.example.timetracking.gantt.ui.widget;

import com.vaadin.flow.component.html.Div;

/**
 * A single Gantt bar: an absolutely positioned rounded div placed over a row track by
 * left/width percentages of the timeline. Pure CSS, no chart library.
 */
final class GanttBar {

    private GanttBar() {
    }

    static Div create(double leftPercent, double widthPercent, String color, String tooltip) {
        Div bar = new Div();
        bar.getElement().setAttribute("title", tooltip);
        bar.getStyle()
                .set("position", "absolute")
                .set("top", "50%")
                .set("transform", "translateY(-50%)")
                .set("left", leftPercent + "%")
                .set("width", "max(6px, " + widthPercent + "%)")
                .set("height", "14px")
                .set("background", color)
                .set("border-radius", "7px")
                .set("box-shadow", "0 1px 2px rgba(31,42,48,0.25)");
        return bar;
    }
}

package com.example.timetracking.velocity.ui.widget;

import com.example.timetracking.milestone.ui.style.DashboardStyle;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.tabs.TabsVariant;

import java.util.function.Consumer;

/**
 * Two-position unit toggle (man-days / hours). All velocity figures are stored in
 * seconds; the view re-renders with {@link #value()} whenever the selection changes.
 */
public class UnitToggle extends Tabs {

    public enum Unit {
        MAN_DAYS {
            @Override
            public String format(long seconds) {
                return DashboardStyle.manDays(seconds) + " MD";
            }
        },
        HOURS {
            @Override
            public String format(long seconds) {
                return DashboardStyle.hours(seconds) + " h";
            }
        };

        public abstract String format(long seconds);
    }

    private final Tab manDaysTab = new Tab("MD");
    private final Tab hoursTab = new Tab("Hours");

    public UnitToggle(Consumer<Unit> onChange) {
        add(manDaysTab, hoursTab);
        addThemeVariants(TabsVariant.LUMO_SMALL);
        addSelectedChangeListener(e -> onChange.accept(value()));
    }

    public Unit value() {
        return getSelectedTab() == hoursTab ? Unit.HOURS : Unit.MAN_DAYS;
    }
}

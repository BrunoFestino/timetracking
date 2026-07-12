package com.example.timetracking.velocity.ui.widget;

import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.tabs.TabsVariant;

import java.util.function.Consumer;

/**
 * Two-position mode toggle for the velocity view: per-milestone velocity (relative
 * weeks per selected milestone) or per-week velocity (calendar weeks over a range).
 */
public class ModeToggle extends Tabs {

    public enum Mode {
        PER_MILESTONE,
        PER_WEEK
    }

    private final Tab perMilestoneTab = new Tab("Per milestone");
    private final Tab perWeekTab = new Tab("Per week");

    public ModeToggle(Consumer<Mode> onChange) {
        add(perMilestoneTab, perWeekTab);
        addThemeVariants(TabsVariant.LUMO_SMALL);
        addSelectedChangeListener(e -> onChange.accept(value()));
    }

    public Mode value() {
        return getSelectedTab() == perWeekTab ? Mode.PER_WEEK : Mode.PER_MILESTONE;
    }
}

package com.example.timetracking.gantt.ui.widget;

import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.tabs.TabsVariant;

import java.util.function.Consumer;

/**
 * Two-position mode toggle for the Gantt view: the whole team's schedule (milestones →
 * epics → issues) or the same work grouped by role.
 *
 * <p>Local copy in the {@code gantt} feature, mirroring the pattern used by the velocity
 * feature's own toggle so the module stays self-contained.
 */
public class ModeToggle extends Tabs {

    public enum Mode {
        BY_TEAM,
        BY_ROLE
    }

    private final Tab byTeamTab = new Tab("By team");
    private final Tab byRoleTab = new Tab("By role");

    public ModeToggle(Consumer<Mode> onChange) {
        add(byTeamTab, byRoleTab);
        addThemeVariants(TabsVariant.LUMO_SMALL);
        addSelectedChangeListener(e -> onChange.accept(value()));
    }

    public Mode value() {
        return getSelectedTab() == byRoleTab ? Mode.BY_ROLE : Mode.BY_TEAM;
    }
}

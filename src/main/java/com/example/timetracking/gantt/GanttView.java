package com.example.timetracking.gantt;

import com.example.timetracking.gantt.application.dto.GanttChart;
import com.example.timetracking.gantt.application.dto.GanttColors;
import com.example.timetracking.gantt.application.model.Role;
import com.example.timetracking.gantt.application.usecase.BuildRoleGanttUseCase;
import com.example.timetracking.gantt.application.usecase.BuildTeamGanttUseCase;
import com.example.timetracking.gantt.ui.style.GanttStyle;
import com.example.timetracking.gantt.ui.widget.GanttChartWidget;
import com.example.timetracking.gantt.ui.widget.ModeToggle;
import com.example.timetracking.views.MainLayout;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * Team Gantt for the Argentina team, fed entirely by fake data so it runs with no Jira.
 *
 * <p>Reachable only by URL at {@code /gantt} (not added to the side nav). Two modes via
 * {@link ModeToggle}: "By team" (milestones → epics → issues, collapsible) and "By role"
 * (grouped by role, everything shown at once).
 */
@Route(value = "gantt", layout = MainLayout.class)
@PageTitle("Team Gantt")
@StyleSheet("https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap")
public class GanttView extends VerticalLayout {

    private final transient BuildTeamGanttUseCase buildTeamGantt;
    private final transient BuildRoleGanttUseCase buildRoleGantt;

    private final ModeToggle modeToggle = new ModeToggle(mode -> render());
    private final Div legend = new Div();
    private final Div results = new Div();

    public GanttView(BuildTeamGanttUseCase buildTeamGantt, BuildRoleGanttUseCase buildRoleGantt) {
        this.buildTeamGantt = buildTeamGantt;
        this.buildRoleGantt = buildRoleGantt;

        setPadding(true);
        setSpacing(true);
        getStyle().set("font-family", GanttStyle.FONT).set("color", GanttStyle.INK);

        results.getStyle().set("margin-top", "12px").set("width", "100%");

        add(title(), subtitle(), modeToggle, legend, results);
        render();
    }

    private H1 title() {
        H1 title = new H1("Team Gantt");
        title.getStyle().set("color", GanttStyle.PRIMARY_900).set("font-weight", "700");
        return title;
    }

    private Span subtitle() {
        Span span = new Span("Argentina team · planned schedule (sample data)");
        span.getStyle().set("color", GanttStyle.MUTED).set("font-size", "14px");
        return span;
    }

    private void render() {
        boolean byRole = modeToggle.value() == ModeToggle.Mode.BY_ROLE;
        GanttChart chart = byRole ? buildRoleGantt.build() : buildTeamGantt.build();

        renderLegend(byRole);
        results.removeAll();
        results.add(new GanttChartWidget(chart));
    }

    // ── legend (mode-aware) ─────────────────────────────────────────────────────────

    private void renderLegend(boolean byRole) {
        legend.removeAll();
        legend.getStyle()
                .set("display", "flex")
                .set("flex-wrap", "wrap")
                .set("gap", "14px")
                .set("align-items", "center")
                .set("margin-top", "4px");

        if (byRole) {
            for (Role role : Role.values()) {
                legend.add(legendItem(role.color(), role.label()));
            }
        } else {
            legend.add(legendItem(GanttColors.MILESTONE, "Milestone"));
            legend.add(legendItem(GanttColors.EPIC, "Epic"));
            legend.add(legendItem(GanttColors.STATUS_DONE, "Done"));
            legend.add(legendItem(GanttColors.STATUS_IN_PROGRESS, "In Progress"));
            legend.add(legendItem(GanttColors.STATUS_TODO, "To Do"));
        }
        legend.add(todayLegend());
    }

    private Div legendItem(String color, String label) {
        Div swatch = new Div();
        swatch.getStyle()
                .set("width", "12px").set("height", "12px")
                .set("border-radius", "3px").set("background", color);

        Span text = new Span(label);
        text.getStyle().set("font-size", "12px").set("color", GanttStyle.MUTED);

        Div item = new Div(swatch, text);
        item.getStyle().set("display", "flex").set("align-items", "center").set("gap", "6px");
        return item;
    }

    private Div todayLegend() {
        Div marker = new Div();
        marker.getStyle()
                .set("width", "0").set("height", "14px")
                .set("border-left", "2px dashed " + GanttStyle.TODAY);

        Span text = new Span("Today");
        text.getStyle().set("font-size", "12px").set("color", GanttStyle.MUTED);

        Div item = new Div(marker, text);
        item.getStyle().set("display", "flex").set("align-items", "center").set("gap", "6px");
        return item;
    }
}

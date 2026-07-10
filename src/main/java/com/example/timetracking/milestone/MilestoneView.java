package com.example.timetracking.milestone;

import com.example.timetracking.milestone.application.dto.MilestoneBreakdown;
import com.example.timetracking.milestone.application.dto.MilestoneSummary;
import com.example.timetracking.milestone.application.usecase.LoadEpicsProgressUseCase;
import com.example.timetracking.milestone.application.usecase.LoadMilestoneProgressUseCase;
import com.example.timetracking.milestone.application.usecase.LoadMilestonesUseCase;
import com.example.timetracking.milestone.application.usecase.LoadProjectsUseCase;
import com.example.timetracking.milestone.domain.JiraProject;
import com.example.timetracking.milestone.domain.JiraTicket;
import com.example.timetracking.milestone.ui.style.DashboardStyle;
import com.example.timetracking.milestone.ui.widget.ContributorWorklogTabWidget;
import com.example.timetracking.milestone.ui.widget.EpicProgressTabWidget;
import com.example.timetracking.milestone.ui.widget.MilestoneDetailsWidget;
import com.example.timetracking.views.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Milestone Time Tracking")
@StyleSheet("https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap")
public class MilestoneView extends VerticalLayout {

    private final transient LoadProjectsUseCase loadProjectsUseCase;
    private final transient LoadMilestonesUseCase loadMilestonesUseCase;
    private final transient LoadMilestoneProgressUseCase loadMilestoneProgress;
    private final transient LoadEpicsProgressUseCase loadEpicsProgress;

    private final ComboBox<JiraProject> projectSelector = new ComboBox<>();
    private final ComboBox<JiraTicket> milestoneSelector = new ComboBox<>();
    private final Div results = new Div();

    public MilestoneView(LoadProjectsUseCase loadProjectsUseCase, LoadMilestonesUseCase loadMilestonesUseCase,
                         LoadMilestoneProgressUseCase loadMilestoneProgress,
                         LoadEpicsProgressUseCase loadEpicsProgress) {
        this.loadProjectsUseCase = loadProjectsUseCase;
        this.loadMilestonesUseCase = loadMilestonesUseCase;
        this.loadMilestoneProgress = loadMilestoneProgress;
        this.loadEpicsProgress = loadEpicsProgress;

        setPadding(true);
        setSpacing(true);
        getStyle().set("font-family", DashboardStyle.FONT).set("color", DashboardStyle.INK);

        results.getStyle()
                .set("margin-top", "16px")
                .set("width", "100%")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("align-items", "center")
                .set("gap", "12px");

        injectResponsiveStyles();
        add(title(), selectors(), results);
    }

    private H1 title() {
        H1 title = new H1("Milestone Time Tracking");
        title.getStyle().set("color", DashboardStyle.PRIMARY_900).set("font-weight", "700");
        return title;
    }

    private HorizontalLayout selectors() {
        HorizontalLayout selectors = new HorizontalLayout(projectSelector(), milestoneSelector());
        selectors.setAlignItems(FlexComponent.Alignment.BASELINE);
        selectors.setWidthFull();
        selectors.setSpacing(true);
        selectors.setPadding(false);
        selectors.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        selectors.addClassName("selector-row");
        selectors.setFlexGrow(0, projectSelector);
        selectors.setFlexGrow(1, milestoneSelector);
        selectors.getStyle().set("flex-wrap", "nowrap").set("gap", "12px");
        return selectors;
    }

    private ComboBox<JiraProject> projectSelector() {
        projectSelector.setPlaceholder("Select a project");
        projectSelector.setWidthFull();
        projectSelector.addClassName("project-selector");
        projectSelector.getStyle()
                .set("min-width", "0")
                .set("flex", "0 0 calc((100% - 12px) * 0.4)")
                .set("max-width", "calc((100% - 12px) * 0.4)");

        List<JiraProject> items = loadProjectsUseCase.loadProjects();

        projectSelector.setItems(items);
        projectSelector.setItemLabelGenerator(JiraProject::getLabel);
        projectSelector.addValueChangeListener(e -> searchMilestones(e.getValue().key()));

        return projectSelector;
    }

    private ComboBox<JiraTicket> milestoneSelector() {
        milestoneSelector.setWidthFull();
        milestoneSelector.setEnabled(false);
        milestoneSelector.addClassName("milestone-selector");
        milestoneSelector.getStyle()
                .set("min-width", "0")
                .set("flex", "1 1 calc((100% - 12px) * 0.6)")
                .set("max-width", "calc((100% - 12px) * 0.6)");
        milestoneSelector.addValueChangeListener(e -> search(e.getValue().key()));
        return milestoneSelector;
    }

    private void searchMilestones(String projectKey) {
        milestoneSelector.setEnabled(false);
        List<JiraTicket> milestones = loadMilestonesUseCase.loadMilestones(projectKey);
        if (!milestones.isEmpty()) {
            milestoneSelector.setPlaceholder("Select a milestone");
            milestoneSelector.setItems(milestones);
            milestoneSelector.setEnabled(true);
        } else {
            milestoneSelector.setPlaceholder("No milestones found");
        }
    }

    private void search(String key) {
        results.removeAll();
        if (key.isEmpty()) {
            Notification.show("Please enter a milestone key.");
            return;
        }
        try {
            Optional<MilestoneSummary> summary = loadMilestoneProgress.execute(key);
            if (summary.isEmpty()) {
                results.add(new Span("Milestone not found: " + key));
                return;
            }

            MilestoneDetailsWidget details = new MilestoneDetailsWidget(summary.get());
            details.addClassName("milestone-details-card");

            Div detailsTabsCard = buildDetailsTabsCard(key, details);
            detailsTabsCard.addClassName("epics-details-card");

            HorizontalLayout contentRow = new HorizontalLayout(details, detailsTabsCard);
            contentRow.setWidthFull();
            contentRow.addClassName("milestone-content-row");
            contentRow.setSpacing(true);
            contentRow.setPadding(false);
            contentRow.setAlignItems(FlexComponent.Alignment.START);
            contentRow.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
            contentRow.setFlexGrow(0, details);
            contentRow.setFlexGrow(1, detailsTabsCard);
            details.getStyle().set("flex-shrink", "0");
            detailsTabsCard.getStyle().set("min-width", "0");
            contentRow.getStyle().set("gap", "24px").set("flex-wrap", "nowrap");

            results.add(contentRow);
        } catch (IllegalArgumentException ex) {
            results.add(new Span("Invalid key format - expected e.g. TTAR-9625"));
        } catch (RuntimeException ex) {
            results.add(new Span("Could not load milestone: " + ex.getMessage()));
        }
    }

    private Div buildDetailsTabsCard(String milestoneKey, MilestoneDetailsWidget details) {
        Div card = new Div();
        card.setWidthFull();
        card.getStyle()
                .set("min-width", "0")
                .set("padding", "16px")
                .set("background", "#FFFFFF")
                .set("border", "1px solid #E5E8EA")
                .set("border-radius", "12px")
                .set("box-shadow", "0 2px 8px rgba(31,42,48,0.08)")
                .set("box-sizing", "border-box");

        H4 header = new H4("Details");
        header.getStyle().set("margin", "0 0 12px 0").set("color", DashboardStyle.EPIC).set("font-weight", "600");

        Tab epicsTab = new Tab("Epics");
        Tab contributorsTab = new Tab("Contributors");
        Tabs tabs = new Tabs(epicsTab, contributorsTab);
        tabs.setWidthFull();

        Div contentHolder = new Div();
        contentHolder.setWidthFull();
        contentHolder.getStyle()
                .set("min-width", "0")
                .set("max-height", "620px")
                .set("overflow-y", "auto")
                .set("padding-right", "6px")
                .set("box-sizing", "border-box")
                .set("margin-top", "12px");

        MilestoneBreakdown breakdown = loadEpicsProgress.execute(milestoneKey);
        if (breakdown.milestone() != null) {
            details.showProgress(breakdown.milestone());
        }

        Component epicsView = new EpicProgressTabWidget(breakdown.epics());
        Component contributorsView = new ContributorWorklogTabWidget(breakdown.epics());
        Map<Tab, Component> viewsByTab = Map.of(epicsTab, epicsView, contributorsTab, contributorsView);

        contentHolder.add(epicsView);
        tabs.addSelectedChangeListener(event -> {
            Component selected = viewsByTab.get(event.getSelectedTab());
            contentHolder.removeAll();
            if (selected != null) {
                contentHolder.add(selected);
            }
        });

        card.add(header, tabs, contentHolder);
        return card;
    }

    private void injectResponsiveStyles() {
        String styleId = "milestone-responsive-styles";
        com.vaadin.flow.component.html.Div styleHolder = new com.vaadin.flow.component.html.Div();
        styleHolder.getElement().setProperty("innerHTML",
                "<style id='" + styleId + "'>"
                        + ".selector-row { gap: 12px; }"
                        + ".selector-row .project-selector { min-width: 0; flex: 0 0 calc((100% - 12px) * 0.4); max-width: calc((100% - 12px) * 0.4); }"
                        + ".selector-row .milestone-selector { min-width: 0; flex: 1 1 calc((100% - 12px) * 0.6); max-width: calc((100% - 12px) * 0.6); }"
                        + "@media (min-width: 1024px) {"
                        + "  .selector-row { gap: 16px; flex-wrap: nowrap !important; }"
                        + "  .selector-row .project-selector { flex-basis: calc((100% - 16px) * 0.4); max-width: calc((100% - 16px) * 0.4); }"
                        + "  .selector-row .milestone-selector { flex-basis: calc((100% - 16px) * 0.6); max-width: calc((100% - 16px) * 0.6); }"
                        + "}"
                        + "@media (max-width: 900px) {"
                        + "  .selector-row { flex-wrap: wrap !important; }"
                        + "  .selector-row .project-selector, .selector-row .milestone-selector {"
                        + "    flex: 1 1 100% !important; max-width: 100% !important; width: 100% !important;"
                        + "  }"
                        + "}"
                        + "@media (max-width: 1100px) {"
                        + "  .milestone-content-row { flex-wrap: wrap !important; justify-content: center !important; }"
                        + "  .milestone-content-row > * { flex-grow: 0 !important; flex-shrink: 1 !important; }"
                        + "  .milestone-content-row .milestone-details-card { margin-left: auto; margin-right: auto; }"
                        + "  .milestone-content-row .epics-details-card { flex: 1 1 100% !important; width: 100% !important; max-width: 100% !important; }"
                        + "}"
                        + "</style>");
        getElement().appendChild(styleHolder.getElement());
    }
}

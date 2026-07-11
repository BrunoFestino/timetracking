package com.example.timetracking.velocity;

import com.example.timetracking.milestone.application.usecase.LoadMilestonesUseCase;
import com.example.timetracking.milestone.application.usecase.LoadProjectsUseCase;
import com.example.timetracking.milestone.domain.JiraProject;
import com.example.timetracking.milestone.domain.JiraTicket;
import com.example.timetracking.milestone.ui.style.DashboardStyle;
import com.example.timetracking.milestone.ui.widget.CollapsibleSection;
import com.example.timetracking.milestone.ui.widget.DetailsRow;
import com.example.timetracking.milestone.ui.widget.IssueSubRow;
import com.example.timetracking.velocity.application.dto.MilestoneVelocity;
import com.example.timetracking.velocity.application.dto.PersonVelocity;
import com.example.timetracking.velocity.application.dto.VelocityReport;
import com.example.timetracking.velocity.application.usecase.ComputeVelocityUseCase;
import com.example.timetracking.velocity.ui.widget.UnitToggle;
import com.example.timetracking.views.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Route(value = "velocity", layout = MainLayout.class)
@PageTitle("Team Velocity")
@StyleSheet("https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap")
public class VelocityView extends VerticalLayout {

    private final transient LoadProjectsUseCase loadProjectsUseCase;
    private final transient LoadMilestonesUseCase loadMilestonesUseCase;
    private final transient ComputeVelocityUseCase computeVelocityUseCase;

    private final ComboBox<JiraProject> projectSelector = new ComboBox<>();
    private final MultiSelectComboBox<JiraTicket> milestoneSelector = new MultiSelectComboBox<>();
    private final Button computeButton = new Button("Compute");
    private final UnitToggle unitToggle = new UnitToggle(unit -> this.render());
    private final Div results = new Div();

    private transient VelocityReport report;

    public VelocityView(LoadProjectsUseCase loadProjectsUseCase, LoadMilestonesUseCase loadMilestonesUseCase,
                        ComputeVelocityUseCase computeVelocityUseCase) {
        this.loadProjectsUseCase = loadProjectsUseCase;
        this.loadMilestonesUseCase = loadMilestonesUseCase;
        this.computeVelocityUseCase = computeVelocityUseCase;

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

        add(title(), selectors(), results);
    }

    private H1 title() {
        H1 title = new H1("Team Velocity");
        title.getStyle().set("color", DashboardStyle.PRIMARY_900).set("font-weight", "700");
        return title;
    }

    private HorizontalLayout selectors() {
        projectSelector.setPlaceholder("Select a project");
        projectSelector.setWidth("260px");
        projectSelector.setItems(loadProjectsUseCase.loadProjects());
        projectSelector.setItemLabelGenerator(JiraProject::getLabel);
        projectSelector.addValueChangeListener(e -> loadMilestones(e.getValue().key()));

        milestoneSelector.setPlaceholder("Select one or more milestones");
        milestoneSelector.setEnabled(false);
        milestoneSelector.setWidthFull();
        milestoneSelector.getStyle().set("min-width", "0");

        computeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        computeButton.addClickListener(e -> compute());

        HorizontalLayout selectors = new HorizontalLayout(projectSelector, milestoneSelector, computeButton, unitToggle);
        selectors.setAlignItems(FlexComponent.Alignment.BASELINE);
        selectors.setWidthFull();
        selectors.setSpacing(true);
        selectors.setPadding(false);
        selectors.setFlexGrow(1, milestoneSelector);
        selectors.getStyle().set("flex-wrap", "wrap").set("gap", "12px");
        return selectors;
    }

    private void loadMilestones(String projectKey) {
        milestoneSelector.setEnabled(false);
        milestoneSelector.clear();
        List<JiraTicket> milestones = loadMilestonesUseCase.loadMilestones(projectKey);
        if (!milestones.isEmpty()) {
            milestoneSelector.setItems(milestones);
            milestoneSelector.setEnabled(true);
        } else {
            milestoneSelector.setPlaceholder("No milestones found");
        }
    }

    private void compute() {
        List<String> keys = milestoneSelector.getSelectedItems().stream().map(JiraTicket::key).toList();
        if (keys.isEmpty()) {
            Notification.show("Select at least one milestone.");
            return;
        }
        try {
            report = computeVelocityUseCase.execute(keys);
            render();
        } catch (IllegalArgumentException ex) {
            Notification.show(ex.getMessage());
        } catch (RuntimeException ex) {
            results.removeAll();
            results.add(new Span("Could not compute velocity: " + ex.getMessage()));
        }
    }

    private void render() {
        results.removeAll();
        if (report == null) {
            return;
        }
        UnitToggle.Unit unit = unitToggle.value();
        results.add(summaryCard(unit), tabsCard(unit));
    }

    // ── summary card ────────────────────────────────────────────────────────────

    private Div summaryCard(UnitToggle.Unit unit) {
        Div card = card();
        card.getStyle().set("max-width", "360px").set("width", "100%");

        H4 header = new H4("Team summary");
        header.getStyle().set("margin", "0 0 12px 0").set("color", DashboardStyle.MILESTONE).set("font-weight", "600");
        card.add(header);

        card.add(summaryRow("Milestones", String.valueOf(report.milestones().size())));
        card.add(summaryRow("Total logged", unit.format(report.totalSeconds())));
        card.add(velocityRow(unit.formatPerWeek(report.teamAvgSecondsPerWeek())));
        return card;
    }

    /** Headline row: the team's weekly velocity, visually dominant over the other summary rows. */
    private Div velocityRow(String value) {
        Span labelSpan = new Span("Team velocity");
        labelSpan.getStyle().set("font-size", "13px").set("font-weight", "600").set("color", DashboardStyle.INK);

        Span valueSpan = new Span(value);
        valueSpan.getStyle()
                .set("font-size", "16px")
                .set("font-weight", "600")
                .set("color", DashboardStyle.PRIMARY_900)
                .set("text-align", "right");

        Div row = new Div(labelSpan, valueSpan);
        row.getStyle()
                .set("display", "flex")
                .set("justify-content", "space-between")
                .set("align-items", "baseline")
                .set("width", "100%")
                .set("padding", "6px 0 3px 0")
                .set("margin-top", "4px")
                .set("border-top", "1px solid #E5E8EA");
        return row;
    }

    private Div summaryRow(String label, String value) {
        Span labelSpan = new Span(label);
        labelSpan.getStyle().set("font-size", "13px").set("font-weight", "500").set("color", DashboardStyle.MUTED);

        Span valueSpan = new Span(value);
        valueSpan.getStyle().set("font-size", "13px").set("color", DashboardStyle.INK).set("text-align", "right");

        Div row = new Div(labelSpan, valueSpan);
        row.getStyle()
                .set("display", "flex")
                .set("justify-content", "space-between")
                .set("align-items", "baseline")
                .set("width", "100%")
                .set("padding", "3px 0");
        return row;
    }

    // ── tabs card ───────────────────────────────────────────────────────────────

    private Div tabsCard(UnitToggle.Unit unit) {
        Div card = card();

        H4 header = new H4("Velocity");
        header.getStyle().set("margin", "0 0 12px 0").set("color", DashboardStyle.EPIC).set("font-weight", "600");

        Tab teamTab = new Tab("Team velocity");
        Tab personsTab = new Tab("Per person");
        Tabs tabs = new Tabs(teamTab, personsTab);
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

        Map<Tab, Component> viewsByTab = Map.of(
                teamTab, teamTabContent(unit),
                personsTab, personsTabContent(unit));

        contentHolder.add(viewsByTab.get(teamTab));
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

    // ── team velocity tab: one weekly chart per milestone ──────────────────────

    private Component teamTabContent(UnitToggle.Unit unit) {
        Div wrapper = new Div();
        wrapper.setWidthFull();
        if (report.milestones().isEmpty()) {
            wrapper.add(DashboardStyle.note("No milestones selected."));
            return wrapper;
        }
        long maxTotal = report.milestones().stream().mapToLong(MilestoneVelocity::totalSpentSeconds).max().orElse(1L);

        for (MilestoneVelocity milestone : report.milestones()) {
            DetailsRow header = wide(new DetailsRow(
                    label(milestone.key(), milestone.name()),
                    percent(milestone.totalSpentSeconds(), maxTotal),
                    unit.format(milestone.totalSpentSeconds()) + " · " + unit.formatPerWeek(milestone.avgSecondsPerWeek()),
                    DashboardStyle.MILESTONE));

            VerticalLayout body = subRows();
            body.add(DashboardStyle.note(startNote(milestone)));
            long maxWeekSeconds = milestone.secondsByWeek().values().stream().mapToLong(Long::longValue).max().orElse(1L);
            for (int week = 1; week <= milestone.observedWeeks(); week++) {
                long seconds = milestone.secondsByWeek().getOrDefault(week, 0L);
                body.add(wide(new IssueSubRow(
                        weekLabel(week, milestone.startDate()),
                        percent(seconds, maxWeekSeconds),
                        unit.format(seconds),
                        DashboardStyle.SPENT)));
            }

            wrapper.add(new CollapsibleSection(header, body, true));
        }
        return wrapper;
    }

    // ── per-person tab: weekly velocity per contributor ────────────────────────

    private Component personsTabContent(UnitToggle.Unit unit) {
        Div wrapper = new Div();
        wrapper.setWidthFull();
        if (report.perPerson().isEmpty()) {
            wrapper.add(DashboardStyle.note("No contributors to display."));
            return wrapper;
        }
        long maxTotal = report.perPerson().stream().mapToLong(PersonVelocity::totalSeconds).max().orElse(1L);
        LocalDate singleStart = report.milestones().size() == 1 ? report.milestones().get(0).startDate() : null;
        Map<String, String> milestoneNames = milestoneNames();

        for (PersonVelocity person : report.perPerson()) {
            DetailsRow header = wide(new DetailsRow(
                    person.name(),
                    percent(person.totalSeconds(), maxTotal),
                    unit.formatPerWeek(person.avgSecondsPerWeek()),
                    DashboardStyle.SPENT));

            VerticalLayout body = subRows();
            body.add(DashboardStyle.note("Total: " + unit.format(person.totalSeconds())));
            long maxWeekSeconds = person.secondsByWeek().values().stream().mapToLong(Long::longValue).max().orElse(1L);
            for (int week = 1; week <= person.observedWeeks(); week++) {
                long seconds = person.secondsByWeek().getOrDefault(week, 0L);
                body.add(wide(new IssueSubRow(
                        weekLabel(week, singleStart),
                        percent(seconds, maxWeekSeconds),
                        unit.format(seconds),
                        DashboardStyle.SPENT)));
            }
            if (report.milestones().size() > 1) {
                body.add(DashboardStyle.note("By milestone"));
                person.secondsByMilestone().forEach((key, seconds) -> body.add(wide(new IssueSubRow(
                        label(key, milestoneNames.get(key)),
                        percent(seconds, person.totalSeconds()),
                        unit.format(seconds),
                        DashboardStyle.EPIC))));
            }

            wrapper.add(new CollapsibleSection(header, body));
        }
        return wrapper;
    }

    // ── helpers ─────────────────────────────────────────────────────────────────

    private static final DateTimeFormatter WEEK_DATE = DateTimeFormatter.ofPattern("MMM d", Locale.US);
    private static final DateTimeFormatter START_DATE = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US);

    /**
     * Widens the value column of a {@link DetailsRow}/{@link IssueSubRow} grid so combined
     * values like "10.0 MD · 2.5 MD/week" fit. Applied to every row in a section, headers
     * and sub-rows alike, so the bar columns stay vertically aligned.
     */
    private <T extends Div> T wide(T row) {
        row.getStyle().set("grid-template-columns", "1fr 160px 175px");
        return row;
    }

    /** Week label with its real date range when the milestone start is known, e.g. "Week 1 (Jul 6 – Jul 12)". */
    private String weekLabel(int week, LocalDate start) {
        if (start == null) {
            return "Week " + week;
        }
        LocalDate from = start.plusDays(7L * (week - 1));
        return "Week " + week + " (" + WEEK_DATE.format(from) + " – " + WEEK_DATE.format(from.plusDays(6)) + ")";
    }

    private String startNote(MilestoneVelocity milestone) {
        String planned = "planned " + milestone.durationWeeks() + " week(s)";
        return milestone.startDate() != null
                ? "Started " + START_DATE.format(milestone.startDate()) + " · " + planned
                : planned;
    }

    private Div card() {
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
        return card;
    }

    private VerticalLayout subRows() {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.setWidthFull();
        return layout;
    }

    private Map<String, String> milestoneNames() {
        Map<String, String> names = new LinkedHashMap<>();
        report.milestones().forEach(m -> names.put(m.key(), m.name()));
        return names;
    }

    private String label(String key, String name) {
        return name != null && !name.isBlank() ? key + " - " + name : key;
    }

    private double percent(long value, long max) {
        return max > 0 ? value * 100.0 / max : 0;
    }
}

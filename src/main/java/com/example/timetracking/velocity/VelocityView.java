package com.example.timetracking.velocity;

import com.example.timetracking.milestone.application.usecase.LoadMilestonesUseCase;
import com.example.timetracking.milestone.application.usecase.LoadProjectsUseCase;
import com.example.timetracking.milestone.domain.JiraProject;
import com.example.timetracking.milestone.domain.JiraTicket;
import com.example.timetracking.milestone.ui.style.DashboardStyle;
import com.example.timetracking.milestone.ui.widget.DetailsRow;
import com.example.timetracking.velocity.ui.widget.CollapsibleSection;
import com.example.timetracking.velocity.ui.widget.ModeToggle;
import com.example.timetracking.velocity.ui.widget.Sparkline;
import com.example.timetracking.velocity.ui.widget.WeeklyBarChart;
import com.example.timetracking.velocity.application.dto.CalendarWeekVelocity;
import com.example.timetracking.velocity.application.dto.IssueEffort;
import com.example.timetracking.velocity.application.dto.MilestoneVelocity;
import com.example.timetracking.velocity.application.dto.PersonVelocity;
import com.example.timetracking.velocity.application.dto.PersonWeeklyVelocity;
import com.example.timetracking.velocity.application.dto.VelocityReport;
import com.example.timetracking.velocity.application.dto.WeeklyVelocityReport;
import com.example.timetracking.velocity.application.usecase.ComputeVelocityUseCase;
import com.example.timetracking.velocity.application.usecase.ComputeWeeklyVelocityUseCase;
import com.example.timetracking.velocity.ui.style.VelocityStyles;
import com.example.timetracking.velocity.ui.widget.UnitToggle;
import com.example.timetracking.views.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Route(value = "velocity", layout = MainLayout.class)
@PageTitle("Team Velocity")
@StyleSheet("https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap")
public class VelocityView extends VerticalLayout {

    private final transient LoadProjectsUseCase loadProjectsUseCase;
    private final transient LoadMilestonesUseCase loadMilestonesUseCase;
    private final transient ComputeVelocityUseCase computeVelocityUseCase;
    private final transient ComputeWeeklyVelocityUseCase computeWeeklyVelocityUseCase;

    private final ModeToggle modeToggle = new ModeToggle(mode -> this.applyMode());
    private final ComboBox<JiraProject> projectSelector = new ComboBox<>();
    private final MultiSelectComboBox<JiraTicket> milestoneSelector = new MultiSelectComboBox<>();
    private final ComboBox<Integer> presetSelector = new ComboBox<>();
    private final DatePicker fromPicker = new DatePicker();
    private final DatePicker toPicker = new DatePicker();
    private final Button searchButton = new Button("Search");
    private final UnitToggle unitToggle = new UnitToggle(unit -> this.render());
    private final Div results = new Div();

    private transient VelocityReport report;
    private transient WeeklyVelocityReport weeklyReport;

    public VelocityView(LoadProjectsUseCase loadProjectsUseCase, LoadMilestonesUseCase loadMilestonesUseCase,
                        ComputeVelocityUseCase computeVelocityUseCase,
                        ComputeWeeklyVelocityUseCase computeWeeklyVelocityUseCase) {
        this.loadProjectsUseCase = loadProjectsUseCase;
        this.loadMilestonesUseCase = loadMilestonesUseCase;
        this.computeVelocityUseCase = computeVelocityUseCase;
        this.computeWeeklyVelocityUseCase = computeWeeklyVelocityUseCase;

        setPadding(true);
        setSpacing(true);
        getStyle().set("font-family", DashboardStyle.FONT).set("color", DashboardStyle.INK);
        VelocityStyles.injectStyles(this);

        results.getStyle()
                .set("margin-top", "16px")
                .set("width", "100%")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("align-items", "center")
                .set("gap", "12px");

        add(header(), toolbar(), results);
    }

    /** Page heading: title plus a one-line description of what the view shows. */
    private Div header() {
        H1 title = new H1("Team Velocity");
        title.getStyle()
                .set("color", DashboardStyle.PRIMARY_900)
                .set("font-weight", "700")
                .set("margin", "0 0 2px 0");

        Span subtitle = new Span("Logged effort per week, milestone and contributor");
        subtitle.getStyle().set("font-size", "14px").set("color", DashboardStyle.MUTED);

        Div header = new Div(title, subtitle);
        header.getStyle().set("display", "flex").set("flex-direction", "column");
        return header;
    }

    /** Wraps the selectors row in card chrome so the controls read as one toolbar. */
    private Div toolbar() {
        Div card = card(null);
        card.getStyle().set("padding", "12px 16px");
        card.add(selectors());
        return card;
    }

    /** A 1px vertical rule separating toolbar groups (mode | inputs | unit). */
    private Div toolbarDivider() {
        Div divider = new Div();
        divider.getStyle()
                .set("width", "1px")
                .set("height", "24px")
                .set("flex-shrink", "0")
                .set("background", "#E5E8EA");
        return divider;
    }

    /**
     * Three fixed toolbar rows — mode and project on top, the mode's filters (milestones,
     * or preset + date range) in the middle, the action row (Search + unit) underneath —
     * so both modes present the same silhouette regardless of which filters are visible.
     */
    private Div selectors() {
        projectSelector.setPlaceholder("Select a project");
        projectSelector.setWidth("260px");
        projectSelector.setItems(loadProjectsUseCase.loadProjects());
        projectSelector.setItemLabelGenerator(JiraProject::getLabel);
        projectSelector.addValueChangeListener(e -> loadMilestones(e.getValue().key()));

        milestoneSelector.setPlaceholder("Select one or more milestones");
        milestoneSelector.setEnabled(false);
        milestoneSelector.setWidthFull();
        milestoneSelector.getStyle().set("min-width", "0");

        presetSelector.setPlaceholder("Last N weeks");
        presetSelector.setItems(4, 8, 12);
        presetSelector.setItemLabelGenerator(n -> "Last " + n + " weeks");
        presetSelector.setWidth("160px");
        presetSelector.addValueChangeListener(e -> {
            if (e.getValue() != null && e.isFromClient()) {
                applyPreset(e.getValue());
            }
        });

        DatePicker.DatePickerI18n dateFormat = new DatePicker.DatePickerI18n();
        dateFormat.setDateFormat("yyyy/MM/dd");
        fromPicker.setPlaceholder("From");
        fromPicker.setWidth("170px");
        fromPicker.setI18n(dateFormat);
        toPicker.setPlaceholder("To");
        toPicker.setWidth("170px");
        toPicker.setI18n(dateFormat);
        fromPicker.addValueChangeListener(e -> {
            if (e.isFromClient()) {
                presetSelector.clear();
            }
        });
        toPicker.addValueChangeListener(e -> {
            if (e.isFromClient()) {
                presetSelector.clear();
            }
        });
        setWeeklyControlsVisible(false);

        searchButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        searchButton.setIcon(VaadinIcon.SEARCH.create());
        searchButton.setDisableOnClick(true);
        searchButton.addClickListener(e -> compute());

        HorizontalLayout inputsRow = new HorizontalLayout(modeToggle, toolbarDivider(), projectSelector);
        inputsRow.setAlignItems(FlexComponent.Alignment.CENTER);
        inputsRow.setWidthFull();
        inputsRow.setSpacing(true);
        inputsRow.setPadding(false);
        inputsRow.getStyle().set("flex-wrap", "wrap").set("gap", "12px");

        HorizontalLayout filtersRow = new HorizontalLayout(milestoneSelector, presetSelector, fromPicker, toPicker);
        filtersRow.setAlignItems(FlexComponent.Alignment.CENTER);
        filtersRow.setWidthFull();
        filtersRow.setSpacing(true);
        filtersRow.setPadding(false);
        filtersRow.setFlexGrow(1, milestoneSelector);
        filtersRow.getStyle().set("flex-wrap", "wrap").set("gap", "12px");

        HorizontalLayout actionRow = new HorizontalLayout(searchButton, toolbarDivider(), unitToggle);
        actionRow.setAlignItems(FlexComponent.Alignment.CENTER);
        actionRow.setSpacing(true);
        actionRow.setPadding(false);
        actionRow.getStyle().set("gap", "12px");

        Div rows = new Div(inputsRow, filtersRow, actionRow);
        rows.getStyle().set("display", "flex").set("flex-direction", "column").set("gap", "12px");
        return rows;
    }

    /** Switches the controls between per-milestone and per-week mode and re-renders. */
    private void applyMode() {
        boolean weekly = modeToggle.value() == ModeToggle.Mode.PER_WEEK;
        milestoneSelector.setVisible(!weekly);
        setWeeklyControlsVisible(weekly);
        render();
    }

    private void setWeeklyControlsVisible(boolean visible) {
        presetSelector.setVisible(visible);
        fromPicker.setVisible(visible);
        toPicker.setVisible(visible);
    }

    /** "Last N weeks": from the Monday of (current week − N−1) to this week's Sunday. */
    private void applyPreset(int weeks) {
        LocalDate today = LocalDate.now();
        fromPicker.setValue(today.with(DayOfWeek.MONDAY).minusWeeks(weeks - 1L));
        toPicker.setValue(today.with(DayOfWeek.SUNDAY));
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
        try {
            if (modeToggle.value() == ModeToggle.Mode.PER_WEEK) {
                computeWeekly();
                return;
            }
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
                showComputeError(ex);
            }
        } finally {
            searchButton.setEnabled(true);
        }
    }

    private void computeWeekly() {
        JiraProject project = projectSelector.getValue();
        if (project == null) {
            Notification.show("Select a project.");
            return;
        }
        try {
            weeklyReport = computeWeeklyVelocityUseCase.execute(project.key(), fromPicker.getValue(), toPicker.getValue());
            render();
        } catch (IllegalArgumentException ex) {
            Notification.show(ex.getMessage());
        } catch (RuntimeException ex) {
            showComputeError(ex);
        }
    }

    private void showComputeError(RuntimeException ex) {
        results.removeAll();
        Div card = card(DashboardStyle.OVER);
        card.add(VelocityStyles.emptyState(VaadinIcon.WARNING, "Could not compute velocity", ex.getMessage()));
        results.add(card);
    }

    private void render() {
        results.removeAll();
        UnitToggle.Unit unit = unitToggle.value();
        if (modeToggle.value() == ModeToggle.Mode.PER_WEEK) {
            if (weeklyReport != null) {
                results.add(weeklySummaryCard(unit), weeklyTabsCard(unit));
            }
            return;
        }
        if (report == null) {
            return;
        }
        results.add(summaryCard(unit), tabsCard(unit));
    }

    // ── summary card ────────────────────────────────────────────────────────────

    private Div summaryCard(UnitToggle.Unit unit) {
        return summaryCard(
                VelocityStyles.kpiTile("Team velocity", unit.formatPerWeek(report.teamAvgSecondsPerWeek()), true),
                VelocityStyles.kpiTile("Total logged", unit.format(report.totalSeconds()), false),
                VelocityStyles.kpiTile("Contributors", String.valueOf(report.contributors()), false),
                VelocityStyles.kpiTile("Milestones", String.valueOf(report.milestones().size()), false),
                VelocityStyles.kpiTile("Active weeks", String.valueOf(report.activeWeeks()), false),
                VelocityStyles.kpiTile("Per contributor",
                        unit.formatPerWeek(report.perContributorSecondsPerWeek()), false));
    }

    /** "Team summary" card: a wrapping row of KPI tiles, the team velocity leading. */
    private Div summaryCard(Div... tiles) {
        Div card = card(DashboardStyle.MILESTONE);

        H4 header = new H4("Team summary");
        header.getStyle().set("margin", "0 0 12px 0").set("color", DashboardStyle.MILESTONE).set("font-weight", "600");

        Div tileRow = new Div(tiles);
        tileRow.addClassName(VelocityStyles.KPI_ROW_CLASS);
        tileRow.setWidthFull();

        card.add(header, tileRow);
        return card;
    }

    // ── tabs card ───────────────────────────────────────────────────────────────

    private Div tabsCard(UnitToggle.Unit unit) {
        return tabsCard(unit, teamTabContent(unit), personsTabContent(unit));
    }

    /** "Velocity" card shared by both modes: header with unit chip, tabs and content. */
    private Div tabsCard(UnitToggle.Unit unit, Component teamContent, Component personsContent) {
        Div card = card(DashboardStyle.EPIC);

        H4 header = new H4("Velocity");
        header.getStyle().set("margin", "0").set("color", DashboardStyle.EPIC).set("font-weight", "600");

        Span unitChip = VelocityStyles.pill(
                unit == UnitToggle.Unit.MAN_DAYS ? "man-days" : "hours", DashboardStyle.SPENT);

        Div headerRow = new Div(header, unitChip);
        headerRow.getStyle()
                .set("display", "flex")
                .set("justify-content", "space-between")
                .set("align-items", "center")
                .set("margin-bottom", "12px");

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
                teamTab, teamContent,
                personsTab, personsContent);

        contentHolder.add(viewsByTab.get(teamTab));
        tabs.addSelectedChangeListener(event -> {
            Component selected = viewsByTab.get(event.getSelectedTab());
            contentHolder.removeAll();
            if (selected != null) {
                contentHolder.add(selected);
            }
        });

        card.add(headerRow, tabs, contentHolder);
        return card;
    }

    // ── team velocity tab: one weekly chart per milestone ──────────────────────

    private Component teamTabContent(UnitToggle.Unit unit) {
        Div wrapper = new Div();
        wrapper.setWidthFull();
        if (report.milestones().isEmpty()) {
            wrapper.add(VelocityStyles.emptyState(VaadinIcon.FLAG_O, "No milestones selected",
                    "Pick one or more milestones above and press Search."));
            return wrapper;
        }

        for (MilestoneVelocity milestone : report.milestones()) {
            Div header = milestoneHeader(milestone, unit);

            VerticalLayout body = subRows();
            body.add(DashboardStyle.note(startNote(milestone)));
            for (int week = 1; week <= milestone.observedWeeks(); week++) {
                long seconds = milestone.secondsByWeek().getOrDefault(week, 0L);
                body.add(weekRow(weekLabel(week, milestone.startDate()), unit.format(seconds)));
            }

            wrapper.add(new CollapsibleSection(header, body, true));
        }
        return wrapper;
    }

    /** Milestone header row: label | weekly-trend sparkline | total · velocity. */
    private Div milestoneHeader(MilestoneVelocity milestone, UnitToggle.Unit unit) {
        Span labelSpan = new Span(label(milestone.key(), milestone.name()));
        labelSpan.getStyle()
                .set("min-width", "0")
                .set("font-size", "14px")
                .set("font-weight", "600")
                .set("color", DashboardStyle.MILESTONE)
                .set("overflow-wrap", "anywhere")
                .set("line-height", "1.3");

        List<Long> values = new ArrayList<>();
        List<String> tooltips = new ArrayList<>();
        for (int week = 1; week <= milestone.observedWeeks(); week++) {
            long seconds = milestone.secondsByWeek().getOrDefault(week, 0L);
            values.add(seconds);
            tooltips.add(weekLabel(week, milestone.startDate()) + ": " + unit.format(seconds));
        }
        Sparkline spark = new Sparkline(values, tooltips, DashboardStyle.SPENT);

        Span valueSpan = new Span(
                unit.format(milestone.totalSpentSeconds()) + " · " + unit.formatPerWeek(milestone.avgSecondsPerWeek()));
        valueSpan.getStyle()
                .set("font-size", "14px")
                .set("color", DashboardStyle.MUTED)
                .set("text-align", "right")
                .set("white-space", "nowrap");

        Div row = new Div(labelSpan, spark, valueSpan);
        row.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "1fr 160px 175px")
                .set("align-items", "center")
                .set("column-gap", "8px")
                .set("width", "100%")
                .set("min-width", "0")
                .set("box-sizing", "border-box");
        return row;
    }

    // ── per-person tab: weekly velocity per contributor ────────────────────────

    private Component personsTabContent(UnitToggle.Unit unit) {
        Div wrapper = new Div();
        wrapper.setWidthFull();
        if (report.perPerson().isEmpty()) {
            wrapper.add(VelocityStyles.emptyState(VaadinIcon.USERS, "No contributors to display",
                    "No one logged time in this selection."));
            return wrapper;
        }
        long maxTotal = report.perPerson().stream().mapToLong(PersonVelocity::totalSeconds).max().orElse(1L);
        Map<String, MilestoneVelocity> byKey = new LinkedHashMap<>();
        report.milestones().forEach(m -> byKey.put(m.key(), m));

        for (PersonVelocity person : report.perPerson()) {
            DetailsRow header = wide(new DetailsRow(
                    person.name(),
                    percent(person.totalSeconds(), maxTotal),
                    unit.formatPerWeek(person.avgSecondsPerWeek()),
                    DashboardStyle.SPENT));

            VerticalLayout body = subRows();
            body.add(DashboardStyle.note("Total: " + unit.format(person.totalSeconds())));
            person.secondsByMilestoneWeek().forEach((key, weekMap) -> {
                MilestoneVelocity mv = byKey.get(key);
                LocalDate start = mv != null ? mv.startDate() : null;
                String name = mv != null ? mv.name() : null;
                body.add(DashboardStyle.note(label(key, name) + " · " + unit.format(person.milestoneTotal(key))));
                for (int week = 1; week <= person.observedWeeksInMilestone(key); week++) {
                    long seconds = weekMap.getOrDefault(week, 0L);
                    body.add(weekRow(weekLabel(week, start), unit.format(seconds)));
                }
            });

            wrapper.add(new CollapsibleSection(header, body));
        }
        return wrapper;
    }

    // ── weekly mode: summary card ───────────────────────────────────────────────

    private Div weeklySummaryCard(UnitToggle.Unit unit) {
        return summaryCard(
                VelocityStyles.kpiTile("Team velocity",
                        unit.formatPerWeek(weeklyReport.teamAvgSecondsPerWeek()), true),
                VelocityStyles.kpiTile("Total logged", unit.format(weeklyReport.totalSeconds()), false),
                VelocityStyles.kpiTile("Contributors", String.valueOf(weeklyReport.contributors()), false),
                VelocityStyles.kpiTile("Weeks in range", String.valueOf(weeklyReport.weeksInRange()), false),
                VelocityStyles.kpiTile("Range",
                        DATE.format(weeklyReport.from()) + " – " + DATE.format(weeklyReport.to()), false),
                VelocityStyles.kpiTile("Per contributor",
                        unit.formatPerWeek(weeklyReport.perContributorSecondsPerWeek()), false));
    }

    // ── weekly mode: tabs card ──────────────────────────────────────────────────

    private Div weeklyTabsCard(UnitToggle.Unit unit) {
        return tabsCard(unit, weeklyTeamTabContent(unit), weeklyPersonsTabContent(unit));
    }

    // ── weekly mode: team tab (range sparkline + one section per calendar week) ─

    private Component weeklyTeamTabContent(UnitToggle.Unit unit) {
        Div wrapper = new Div();
        wrapper.setWidthFull();
        if (weeklyReport.totalSeconds() == 0) {
            wrapper.add(VelocityStyles.emptyState(VaadinIcon.CALENDAR_O, "No work logged in the selected range",
                    "Try a wider date range."));
            return wrapper;
        }

        List<WeeklyBarChart.Column> columns = new ArrayList<>();
        for (CalendarWeekVelocity week : weeklyReport.weeks()) {
            columns.add(new WeeklyBarChart.Column(
                    week.totalSeconds(),
                    bareValue(unit, week.totalSeconds()),
                    DATE.format(week.weekStart()),
                    weekOfLabel(week.weekStart()) + ": " + unit.format(week.totalSeconds())));
        }
        Div chart = new Div(DashboardStyle.note("Team effort per week"),
                new WeeklyBarChart(columns, weeklyReport.teamAvgSecondsPerWeek(),
                        "avg " + unit.formatPerWeek(weeklyReport.teamAvgSecondsPerWeek()), DashboardStyle.SPENT));
        chart.getStyle()
                .set("margin-bottom", "16px")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "6px");
        wrapper.add(chart);

        for (CalendarWeekVelocity week : weeklyReport.weeks()) {
            Div header = weekHeader(weekOfLabel(week.weekStart()), unit.format(week.totalSeconds()));

            VerticalLayout body = subRows();
            if (week.secondsByPerson().isEmpty()) {
                body.add(DashboardStyle.note("No work logged this week."));
            } else {
                week.secondsByPerson().forEach((person, seconds) ->
                        body.add(weekRow(person, unit.format(seconds))));
                body.add(DashboardStyle.note("Issues"));
                for (IssueEffort issue : week.issues()) {
                    body.add(weekRow(label(issue.key(), issue.summary()), unit.format(issue.seconds())));
                }
            }
            wrapper.add(new CollapsibleSection(header, body));
        }
        return wrapper;
    }

    /** Calendar-week header row: "Week of ..." on the left, the team total on the right. */
    private Div weekHeader(String label, String value) {
        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("min-width", "0")
                .set("font-size", "14px")
                .set("font-weight", "600")
                .set("color", DashboardStyle.MILESTONE)
                .set("overflow-wrap", "anywhere")
                .set("line-height", "1.3");

        Span valueSpan = new Span(value);
        valueSpan.getStyle()
                .set("font-size", "14px")
                .set("color", DashboardStyle.MUTED)
                .set("text-align", "right")
                .set("white-space", "nowrap");

        Div row = new Div(labelSpan, valueSpan);
        row.getStyle()
                .set("display", "flex")
                .set("justify-content", "space-between")
                .set("align-items", "center")
                .set("width", "100%")
                .set("min-width", "0")
                .set("box-sizing", "border-box")
                .set("column-gap", "8px");
        return row;
    }

    // ── weekly mode: per-person tab ─────────────────────────────────────────────

    private Component weeklyPersonsTabContent(UnitToggle.Unit unit) {
        Div wrapper = new Div();
        wrapper.setWidthFull();
        if (weeklyReport.perPerson().isEmpty()) {
            wrapper.add(VelocityStyles.emptyState(VaadinIcon.USERS, "No contributors to display",
                    "No one logged time in this selection."));
            return wrapper;
        }

        for (PersonWeeklyVelocity person : weeklyReport.perPerson()) {
            long avgSecondsPerWeek = person.totalSeconds() / weeklyReport.weeksInRange();

            List<Long> values = new ArrayList<>();
            List<String> tooltips = new ArrayList<>();
            for (CalendarWeekVelocity week : weeklyReport.weeks()) {
                long seconds = person.secondsByWeek().getOrDefault(week.weekStart(), 0L);
                values.add(seconds);
                tooltips.add(weekOfLabel(week.weekStart()) + ": " + unit.format(seconds));
            }

            Span labelSpan = new Span(person.name());
            labelSpan.getStyle()
                    .set("min-width", "0")
                    .set("font-size", "14px")
                    .set("font-weight", "600")
                    .set("color", DashboardStyle.INK)
                    .set("overflow-wrap", "anywhere")
                    .set("line-height", "1.3");

            Span valueSpan = new Span(
                    unit.format(person.totalSeconds()) + " · " + unit.formatPerWeek(avgSecondsPerWeek));
            valueSpan.getStyle()
                    .set("font-size", "14px")
                    .set("color", DashboardStyle.MUTED)
                    .set("text-align", "right")
                    .set("white-space", "nowrap");

            Div header = new Div(labelSpan, new Sparkline(values, tooltips, DashboardStyle.SPENT), valueSpan);
            header.getStyle()
                    .set("display", "grid")
                    .set("grid-template-columns", "1fr 160px 175px")
                    .set("align-items", "center")
                    .set("column-gap", "8px")
                    .set("width", "100%")
                    .set("min-width", "0")
                    .set("box-sizing", "border-box");

            VerticalLayout body = subRows();
            for (CalendarWeekVelocity week : weeklyReport.weeks()) {
                long seconds = person.secondsByWeek().getOrDefault(week.weekStart(), 0L);
                body.add(weekRow(weekOfLabel(week.weekStart()), unit.format(seconds)));
            }

            wrapper.add(new CollapsibleSection(header, body));
        }
        return wrapper;
    }

    // ── helpers ─────────────────────────────────────────────────────────────────

    /** Single date format for every rendered date in the view, e.g. "2026/07/16". */
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    /**
     * Widens the value column of a {@link DetailsRow} header grid so combined values like
     * "10.0 MD · 2.5 MD/week" fit without clipping at the card edge.
     */
    private <T extends Div> T wide(T row) {
        row.getStyle().set("grid-template-columns", "1fr 160px 175px");
        return row;
    }

    /** "5.0 MD" → "5.0": number-only label for above-bar values. */
    private String bareValue(UnitToggle.Unit unit, long seconds) {
        String formatted = unit.format(seconds);
        int space = formatted.indexOf(' ');
        return space > 0 ? formatted.substring(0, space) : formatted;
    }

    /** Calendar-week label, e.g. "Week of 2026/06/01 – 2026/06/07". */
    private String weekOfLabel(LocalDate weekStart) {
        return "Week of " + DATE.format(weekStart) + " – " + DATE.format(weekStart.plusDays(6));
    }

    /** Week label with its real date range when the milestone start is known, e.g. "Week 1 (2026/07/06 – 2026/07/12)". */
    private String weekLabel(int week, LocalDate start) {
        if (start == null) {
            return "Week " + week;
        }
        LocalDate from = start.plusDays(7L * (week - 1));
        return "Week " + week + " (" + DATE.format(from) + " – " + DATE.format(from.plusDays(6)) + ")";
    }

    private String startNote(MilestoneVelocity milestone) {
        String planned = "planned " + milestone.durationWeeks() + " week(s)";
        return milestone.startDate() != null
                ? "Started " + DATE.format(milestone.startDate()) + " · " + planned
                : planned;
    }

    /** White rounded card; a non-null accent colour adds a coloured left edge. */
    private Div card(String accentColor) {
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
        if (accentColor != null) {
            card.getStyle().set("border-left", "3px solid " + accentColor);
        }
        return card;
    }

    private VerticalLayout subRows() {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.setWidthFull();
        return layout;
    }

    /**
     * A weekly row without a bar: "Week N (dates)" with the value right next to it,
     * so each figure reads against its label instead of sitting at the card's far edge.
     */
    private Div weekRow(String label, String value) {
        Span labelSpan = new Span(label);
        labelSpan.getStyle().set("font-size", "13px").set("color", DashboardStyle.INK)
                .set("min-width", "0");

        Span valueSpan = new Span(value);
        valueSpan.getStyle().set("font-size", "13px").set("color", DashboardStyle.MUTED)
                .set("white-space", "nowrap");

        Div row = new Div(labelSpan, valueSpan);
        row.getStyle()
                .set("display", "flex")
                .set("align-items", "baseline")
                .set("column-gap", "12px")
                .set("width", "100%")
                .set("padding", "5px 0 5px 16px")
                .set("box-sizing", "border-box")
                .set("border-bottom", "1px solid #F0F2F4");
        return row;
    }

    private String label(String key, String name) {
        return name != null && !name.isBlank() ? key + " - " + name : key;
    }

    private double percent(long value, long max) {
        return max > 0 ? value * 100.0 / max : 0;
    }
}

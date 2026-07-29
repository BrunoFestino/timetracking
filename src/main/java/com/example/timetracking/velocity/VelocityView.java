package com.example.timetracking.velocity;

import com.example.timetracking.milestone.application.usecase.LoadProjectsUseCase;
import com.example.timetracking.milestone.domain.JiraProject;
import com.example.timetracking.milestone.domain.JiraTicket;
import com.example.timetracking.milestone.ui.style.DashboardStyle;
import com.example.timetracking.velocity.application.dto.MilestoneVelocity;
import com.example.timetracking.velocity.application.dto.PersonMilestoneEffort;
import com.example.timetracking.velocity.application.dto.PersonVelocity;
import com.example.timetracking.velocity.application.dto.VelocityReport;
import com.example.timetracking.velocity.application.usecase.ComputeVelocityUseCase;
import com.example.timetracking.velocity.application.usecase.LoadDeliveredMilestonesUseCase;
import com.example.timetracking.velocity.ui.style.VelocityStyles;
import com.example.timetracking.velocity.ui.widget.CollapsibleSection;
import com.example.timetracking.velocity.ui.widget.ComparisonBarChart;
import com.example.timetracking.velocity.ui.widget.Sparkline;
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
import com.vaadin.flow.component.icon.VaadinIcon;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Delivery velocity: pick a project, then the delivered milestones within it — of any type —
 * and compare how long each took to finish, both for the team and per person.
 */
@Route(value = "velocity", layout = MainLayout.class)
@PageTitle("Delivery Velocity")
@StyleSheet("https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap")
public class VelocityView extends VerticalLayout {

    private final transient LoadProjectsUseCase loadProjectsUseCase;
    private final transient LoadDeliveredMilestonesUseCase loadDeliveredMilestonesUseCase;
    private final transient ComputeVelocityUseCase computeVelocityUseCase;

    private final ComboBox<JiraProject> projectSelector = new ComboBox<>();
    private final MultiSelectComboBox<JiraTicket> milestoneSelector = new MultiSelectComboBox<>();
    private final Button searchButton = new Button("Search");
    private final UnitToggle unitToggle = new UnitToggle(unit -> this.render());
    private final Div results = new Div();

    private transient VelocityReport report;

    public VelocityView(LoadProjectsUseCase loadProjectsUseCase,
                        LoadDeliveredMilestonesUseCase loadDeliveredMilestonesUseCase,
                        ComputeVelocityUseCase computeVelocityUseCase) {
        this.loadProjectsUseCase = loadProjectsUseCase;
        this.loadDeliveredMilestonesUseCase = loadDeliveredMilestonesUseCase;
        this.computeVelocityUseCase = computeVelocityUseCase;

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
        H1 title = new H1("Delivery Velocity");
        title.getStyle()
                .set("color", DashboardStyle.PRIMARY_900)
                .set("font-weight", "700")
                .set("margin", "0 0 2px 0");

        Span subtitle = new Span("How long delivered milestones take to finish — by team and by person");
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

    /** A 1px vertical rule separating toolbar groups. */
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
     * Three toolbar rows: the project to draw from, the delivered milestones to compare,
     * then the action row (Search + effort unit).
     */
    private Div selectors() {
        projectSelector.setPlaceholder("Select a project");
        projectSelector.setWidth("380px");
        projectSelector.setItems(loadProjectsUseCase.loadProjects());
        projectSelector.setItemLabelGenerator(JiraProject::getLabel);
        projectSelector.addValueChangeListener(e -> loadDeliveredMilestones(e.getValue()));

        milestoneSelector.setPlaceholder("Select a project first");
        milestoneSelector.setEnabled(false);
        milestoneSelector.setWidthFull();
        milestoneSelector.getStyle().set("min-width", "0");

        searchButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        searchButton.setIcon(VaadinIcon.SEARCH.create());
        searchButton.setDisableOnClick(true);
        searchButton.addClickListener(e -> compute());

        HorizontalLayout projectsRow = new HorizontalLayout(projectSelector);
        projectsRow.setAlignItems(FlexComponent.Alignment.CENTER);
        projectsRow.setWidthFull();
        projectsRow.setSpacing(true);
        projectsRow.setPadding(false);
        projectsRow.getStyle().set("flex-wrap", "wrap").set("gap", "12px");

        HorizontalLayout milestonesRow = new HorizontalLayout(milestoneSelector);
        milestonesRow.setAlignItems(FlexComponent.Alignment.CENTER);
        milestonesRow.setWidthFull();
        milestonesRow.setSpacing(true);
        milestonesRow.setPadding(false);
        milestonesRow.setFlexGrow(1, milestoneSelector);
        milestonesRow.getStyle().set("flex-wrap", "wrap").set("gap", "12px");

        HorizontalLayout actionRow = new HorizontalLayout(searchButton, toolbarDivider(), unitToggle);
        actionRow.setAlignItems(FlexComponent.Alignment.CENTER);
        actionRow.setSpacing(true);
        actionRow.setPadding(false);
        actionRow.getStyle().set("gap", "12px");

        Div rows = new Div(projectsRow, milestonesRow, actionRow);
        rows.getStyle().set("display", "flex").set("flex-direction", "column").set("gap", "12px");
        return rows;
    }

    /**
     * Rebuilds the milestone pool from the selected project. Only delivered milestones are
     * offered — an unfinished milestone has no time-to-finish to compare — of any type, so
     * milestones of different kinds within the project can be compared side by side.
     */
    private void loadDeliveredMilestones(JiraProject project) {
        milestoneSelector.clear();
        milestoneSelector.setEnabled(false);
        if (project == null) {
            milestoneSelector.setItems(List.of());
            milestoneSelector.setPlaceholder("Select a project first");
            return;
        }
        try {
            List<JiraTicket> delivered = loadDeliveredMilestonesUseCase.loadDeliveredMilestones(project.key());
            milestoneSelector.setItems(delivered);
            if (delivered.isEmpty()) {
                milestoneSelector.setPlaceholder("No delivered milestones in this project");
            } else {
                milestoneSelector.setPlaceholder("Select delivered milestones (any type)");
                milestoneSelector.setEnabled(true);
            }
        } catch (RuntimeException ex) {
            milestoneSelector.setItems(List.of());
            milestoneSelector.setPlaceholder("Could not load milestones");
            Notification.show("Could not load delivered milestones: " + ex.getMessage());
        }
    }

    private void compute() {
        try {
            List<String> keys = milestoneSelector.getSelectedItems().stream().map(JiraTicket::key).toList();
            if (keys.isEmpty()) {
                Notification.show("Select at least one delivered milestone.");
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

    private void showComputeError(RuntimeException ex) {
        results.removeAll();
        Div card = card(DashboardStyle.OVER);
        card.add(VelocityStyles.emptyState(VaadinIcon.WARNING, "Could not compute velocity", ex.getMessage()));
        results.add(card);
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

    /** "Team summary" card: the headline delivery time plus a few context figures. */
    private Div summaryCard(UnitToggle.Unit unit) {
        Div card = card(DashboardStyle.MILESTONE);

        H4 header = new H4("Team summary");
        header.getStyle().set("margin", "0 0 12px 0").set("color", DashboardStyle.MILESTONE).set("font-weight", "600");

        Div tileRow = new Div(
                VelocityStyles.kpiTile("Avg time to deliver", days(report.avgDurationDays()), true),
                VelocityStyles.kpiTile("Milestones", String.valueOf(report.milestones().size()), false),
                VelocityStyles.kpiTile("Avg effort", unit.format(report.avgSecondsPerMilestone()), false),
                VelocityStyles.kpiTile("Contributors", String.valueOf(report.contributors()), false));
        tileRow.addClassName(VelocityStyles.KPI_ROW_CLASS);
        tileRow.setWidthFull();

        card.add(header, tileRow);
        return card;
    }

    // ── tabs card ───────────────────────────────────────────────────────────────

    /** "Delivery velocity" card: header with unit chip, the two comparison tabs and content. */
    private Div tabsCard(UnitToggle.Unit unit) {
        Div card = card(DashboardStyle.EPIC);

        H4 header = new H4("Delivery velocity");
        header.getStyle().set("margin", "0").set("color", DashboardStyle.EPIC).set("font-weight", "600");

        Span unitChip = VelocityStyles.pill(
                unit == UnitToggle.Unit.MAN_DAYS ? "man-days" : "hours", DashboardStyle.SPENT);

        Div headerRow = new Div(header, unitChip);
        headerRow.getStyle()
                .set("display", "flex")
                .set("justify-content", "space-between")
                .set("align-items", "center")
                .set("margin-bottom", "12px");

        Tab teamTab = new Tab("Compare milestones");
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

        card.add(headerRow, tabs, contentHolder);
        return card;
    }

    // ── compare tab: the duration chart plus a side-by-side comparison table ───

    private Component teamTabContent(UnitToggle.Unit unit) {
        Div wrapper = new Div();
        wrapper.setWidthFull();
        if (report.milestones().isEmpty()) {
            wrapper.add(VelocityStyles.emptyState(VaadinIcon.FLAG_O, "No milestones selected",
                    "Pick one or more delivered milestones above and press Search."));
            return wrapper;
        }

        wrapper.add(durationChart(), comparisonTable(unit));
        return wrapper;
    }

    /**
     * Bar chart of days-to-deliver per milestone, each bar in its milestone's colour (matching
     * its comparison-table column) and the selection's average as a dashed line.
     */
    private Div durationChart() {
        List<MilestoneVelocity> milestones = report.milestones();
        List<ComparisonBarChart.Column> columns = new ArrayList<>();
        for (int i = 0; i < milestones.size(); i++) {
            MilestoneVelocity milestone = milestones.get(i);
            columns.add(new ComparisonBarChart.Column(
                    milestone.durationDays(),
                    milestone.hasDuration() ? String.valueOf(milestone.durationDays()) : "n/a",
                    milestone.key(),
                    label(milestone.key(), milestone.name()) + ": " + days(milestone.durationDays()),
                    VelocityStyles.colorFor(i)));
        }

        Div chart = new Div(
                DashboardStyle.note("Calendar days from start to delivery"),
                new ComparisonBarChart(columns, report.avgDurationDays(),
                        "avg " + days(report.avgDurationDays()), DashboardStyle.SPENT));
        chart.getStyle()
                .set("margin-bottom", "16px")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "6px");
        return chart;
    }

    // ── comparison table: metrics down the side, one column per milestone ──────

    /** Left label column plus one flexible column per compared milestone. */
    private String gridColumns() {
        return "minmax(150px, 200px) repeat(" + report.milestones().size() + ", minmax(150px, 1fr))";
    }

    /**
     * The side-by-side comparison: each milestone is a colour-coded column, each metric a row,
     * so two milestones can be read against each other line by line. Ends with an effort-per-person
     * block — the union of everyone who worked on any compared milestone, each with their effort
     * per milestone — so the detailed comparison also shows who carried which delivery.
     */
    private Div comparisonTable(UnitToggle.Unit unit) {
        List<MilestoneVelocity> milestones = report.milestones();
        int fastest = report.fastestDurationDays();

        Div table = new Div();
        table.getStyle().set("display", "grid").set("grid-template-columns", gridColumns())
                .set("min-width", "fit-content").set("box-sizing", "border-box");

        table.add(cornerCell());
        for (int i = 0; i < milestones.size(); i++) {
            table.add(milestoneHeaderCell(milestones.get(i), VelocityStyles.colorFor(i)));
        }

        // metric rows: label in the first column, one value per milestone
        addRow(table, "Duration", milestones, m -> durationCell(m, fastest), false);
        addRow(table, "Started", milestones, m -> textCell(date(m.startDate())), false);
        addRow(table, "Delivered", milestones, m -> textCell(date(m.deliveryDate())), false);
        addRow(table, "Total effort", milestones, m -> textCell(unit.format(m.totalSpentSeconds())), true);
        addRow(table, "Effort per day open", milestones, m -> textCell(unit.formatPerDay(m.secondsPerDay())), false);
        addRow(table, "Contributors", milestones, m -> textCell(String.valueOf(m.contributors())), true);

        addPersonRows(table, milestones, unit);

        Div scroller = new Div(table);
        scroller.getStyle().set("width", "100%").set("overflow-x", "auto").set("box-sizing", "border-box");
        return scroller;
    }

    /** Empty top-left corner of the grid. */
    private Div cornerCell() {
        Div cell = tableCell(false);
        cell.getStyle().set("background", "#FFFFFF");
        return cell;
    }

    /** Column header for one milestone: a colour bar, its key (bold) and its name. */
    private Div milestoneHeaderCell(MilestoneVelocity milestone, String color) {
        Span keySpan = new Span(milestone.key());
        keySpan.getStyle().set("font-size", "13px").set("font-weight", "700").set("color", DashboardStyle.INK);

        Span nameSpan = new Span(milestone.name() == null ? "" : milestone.name());
        nameSpan.getStyle().set("font-size", "12px").set("color", DashboardStyle.MUTED)
                .set("overflow-wrap", "anywhere").set("line-height", "1.25");

        Div cell = new Div(keySpan, nameSpan);
        cell.getStyle()
                .set("display", "flex").set("flex-direction", "column").set("gap", "2px")
                .set("padding", "8px 10px")
                .set("border-top", "3px solid " + color)
                .set("border-bottom", "1px solid " + DashboardStyle.REMAINING)
                .set("background", "#FBFCFD")
                .set("box-sizing", "border-box");
        return cell;
    }

    /** Adds one metric row: a label cell followed by a rendered value cell per milestone. */
    private void addRow(Div table, String label, List<MilestoneVelocity> milestones,
                        java.util.function.Function<MilestoneVelocity, Div> valueCell, boolean shaded) {
        table.add(labelCell(label, shaded));
        for (MilestoneVelocity milestone : milestones) {
            Div cell = valueCell.apply(milestone);
            if (shaded) {
                cell.getStyle().set("background", "#F8FAFB");
            }
            table.add(cell);
        }
    }

    /** Duration value; the fastest milestone(s) of the selection get a green, bold accent. */
    private Div durationCell(MilestoneVelocity milestone, int fastest) {
        Div cell = textCell(days(milestone.durationDays()));
        if (milestone.hasDuration() && milestone.durationDays() == fastest) {
            cell.getStyle().set("color", DashboardStyle.ON_TRACK).set("font-weight", "700");
        }
        return cell;
    }

    /**
     * One row per person who logged on any compared milestone, each cell their effort on that
     * milestone (with its share) or "—". People are ordered by total effort across the selection.
     */
    private void addPersonRows(Div table, List<MilestoneVelocity> milestones, UnitToggle.Unit unit) {
        if (report.perPerson().isEmpty()) {
            return;
        }
        table.add(sectionCell("Effort per person"));
        for (int i = 0; i < milestones.size(); i++) {
            table.add(sectionCell(""));
        }
        for (PersonVelocity person : report.perPerson()) {
            table.add(labelCell(person.name(), false));
            for (MilestoneVelocity milestone : milestones) {
                long seconds = milestone.secondsByPerson().getOrDefault(person.name(), 0L);
                table.add(seconds > 0
                        ? textCell(unit.format(seconds) + " · " + share(seconds, milestone.totalSpentSeconds()))
                        : textCell("—"));
            }
        }
    }

    /** Full-width band that titles a group of rows (e.g. "Effort per person"). */
    private Div sectionCell(String text) {
        Div cell = tableCell(false);
        cell.getStyle().set("background", "#F1F4F6").set("font-weight", "600")
                .set("font-size", "12px").set("color", DashboardStyle.MILESTONE);
        if (!text.isBlank()) {
            cell.setText(text);
        }
        return cell;
    }

    /** Left-column metric label. */
    private Div labelCell(String text, boolean shaded) {
        Div cell = tableCell(false);
        cell.setText(text);
        cell.getStyle().set("font-weight", "600").set("color", DashboardStyle.INK)
                .set("background", shaded ? "#F8FAFB" : "#FFFFFF");
        return cell;
    }

    /** Plain value cell holding the given text. */
    private Div textCell(String text) {
        Div cell = tableCell(true);
        cell.setText(text);
        return cell;
    }

    /** Shared cell chrome: padding, font and a hairline bottom border. */
    private Div tableCell(boolean muted) {
        Div cell = new Div();
        cell.getStyle()
                .set("padding", "7px 10px")
                .set("font-size", "13px")
                .set("color", muted ? DashboardStyle.MUTED : DashboardStyle.INK)
                .set("border-bottom", "1px solid #F0F2F4")
                .set("box-sizing", "border-box")
                .set("white-space", "nowrap")
                .set("overflow", "hidden")
                .set("text-overflow", "ellipsis");
        return cell;
    }

    // ── per-person tab: the same milestones seen through each contributor ──────

    private Component personsTabContent(UnitToggle.Unit unit) {
        Div wrapper = new Div();
        wrapper.setWidthFull();
        if (report.perPerson().isEmpty()) {
            wrapper.add(VelocityStyles.emptyState(VaadinIcon.USERS, "No contributors to display",
                    "No one logged time on the selected milestones."));
            return wrapper;
        }

        Map<String, MilestoneVelocity> byKey = new LinkedHashMap<>();
        report.milestones().forEach(milestone -> byKey.put(milestone.key(), milestone));

        for (PersonVelocity person : report.perPerson()) {
            wrapper.add(new CollapsibleSection(personHeader(person, unit), personBody(person, byKey, unit)));
        }
        return wrapper;
    }

    /** Person header row: name | effort spread over the selected milestones | total · milestones. */
    private Div personHeader(PersonVelocity person, UnitToggle.Unit unit) {
        Span labelSpan = new Span(person.name());
        labelSpan.getStyle()
                .set("min-width", "0")
                .set("font-size", "14px")
                .set("font-weight", "600")
                .set("color", DashboardStyle.INK)
                .set("overflow-wrap", "anywhere")
                .set("line-height", "1.3");

        List<Long> values = new ArrayList<>();
        List<String> tooltips = new ArrayList<>();
        for (MilestoneVelocity milestone : report.milestones()) {
            long seconds = person.effortOn(milestone.key());
            values.add(seconds);
            tooltips.add(milestone.key() + ": " + unit.format(seconds));
        }

        Span valueSpan = new Span(unit.format(person.totalSeconds())
                + " · " + person.milestonesParticipated() + " of " + report.milestones().size());
        valueSpan.getStyle()
                .set("font-size", "14px")
                .set("color", DashboardStyle.MUTED)
                .set("text-align", "right")
                .set("white-space", "nowrap");

        Div row = new Div(labelSpan, new Sparkline(values, tooltips, DashboardStyle.SPENT), valueSpan);
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

    /**
     * Per-milestone detail for one person: what they put into each milestone and what share of
     * the milestone's total effort that was — how much of each delivery they carried.
     */
    private VerticalLayout personBody(PersonVelocity person, Map<String, MilestoneVelocity> byKey,
                                      UnitToggle.Unit unit) {
        VerticalLayout body = subRows();
        body.add(DashboardStyle.note("Avg " + unit.format(person.avgSecondsPerMilestone()) + " per milestone"));

        for (PersonMilestoneEffort effort : person.milestones()) {
            MilestoneVelocity milestone = byKey.get(effort.milestoneKey());
            String name = milestone != null ? milestone.name() : null;
            long milestoneTotal = milestone != null ? milestone.totalSpentSeconds() : 0;
            body.add(detailRow(
                    label(effort.milestoneKey(), name),
                    unit.format(effort.seconds())
                            + " · " + share(effort.seconds(), milestoneTotal) + " of milestone"));
        }
        return body;
    }

    // ── helpers ─────────────────────────────────────────────────────────────────

    /** Single date format for every rendered date in the view, e.g. "2026/07/16". */
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private String date(LocalDate date) {
        return date != null ? DATE.format(date) : "unknown";
    }

    /** "37 days" / "1 day", or "n/a" when the duration could not be established. */
    private String days(int days) {
        if (days <= 0) {
            return "n/a";
        }
        return days + (days == 1 ? " day" : " days");
    }

    /** "35%" of a total, or "—" when there is no total to compare against. */
    private String share(long seconds, long total) {
        return total > 0 ? Math.round(seconds * 100.0 / total) + "%" : "—";
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
     * A detail row inside an expanded section: label on the left with its value right next to
     * it, so each figure reads against its label instead of sitting at the card's far edge.
     */
    private Div detailRow(String label, String value) {
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
}

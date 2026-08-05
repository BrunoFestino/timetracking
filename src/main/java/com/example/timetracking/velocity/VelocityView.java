package com.example.timetracking.velocity;

import com.example.timetracking.milestone.application.usecase.LoadProjectsUseCase;
import com.example.timetracking.milestone.domain.JiraProject;
import com.example.timetracking.milestone.domain.JiraTicket;
import com.example.timetracking.milestone.ui.style.DashboardStyle;
import com.example.timetracking.velocity.application.dto.EffortBucket;
import com.example.timetracking.velocity.application.dto.MilestoneVelocity;
import com.example.timetracking.velocity.application.dto.PersonVelocity;
import com.example.timetracking.velocity.application.dto.VelocityReport;
import com.example.timetracking.velocity.application.usecase.ComputeVelocityUseCase;
import com.example.timetracking.velocity.application.usecase.LoadDeliveredMilestonesUseCase;
import com.example.timetracking.velocity.ui.style.VelocityStyles;
import com.example.timetracking.velocity.ui.widget.EffortTimelineChart;
import com.example.timetracking.velocity.ui.widget.UnitToggle;
import com.example.timetracking.views.MainLayout;
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
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Delivery velocity: pick a project, then the delivered milestones within it — of any type —
 * and compare how fast each was delivered, how the effort evolved, and how it split per person.
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
                .set("align-items", "stretch")
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

        Span subtitle = new Span("Compare delivery speed, effort over time and effort per person across milestones");
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
        results.add(overviewCard(unit), deliveryVelocityCard(unit), compareCard(unit));
    }

    // ── comparison overview: one card per milestone, four velocity metrics each ────

    /**
     * "Comparison overview": a card per selected milestone (its key, name and colour) holding the
     * four headline velocity metrics, laid out in a responsive row so they fill the width and align
     * across milestones for at-a-glance comparison.
     */
    private Div overviewCard(UnitToggle.Unit unit) {
        Div card = card(DashboardStyle.MILESTONE);

        H4 header = new H4("Comparison overview");
        header.getStyle().set("margin", "0 0 12px 0").set("color", DashboardStyle.MILESTONE).set("font-weight", "600");
        card.add(header);

        List<MilestoneVelocity> milestones = report.milestones();
        if (milestones.isEmpty()) {
            card.add(DashboardStyle.note("No milestones selected."));
            return card;
        }

        Div row = new Div();
        row.getStyle().set("display", "flex").set("flex-wrap", "wrap").set("gap", "12px").set("width", "100%");
        for (int i = 0; i < milestones.size(); i++) {
            row.add(epicOverviewCard(milestones.get(i), VelocityStyles.colorFor(i), unit));
        }
        card.add(row);

        if (milestones.size() < 2) {
            Div hint = new Div(DashboardStyle.note("Select another milestone to compare."));
            hint.getStyle().set("margin-top", "10px");
            card.add(hint);
        }
        return card;
    }

    /** One milestone's overview card: colour-topped header + a 2×2 grid of the four velocity metrics. */
    private Div epicOverviewCard(MilestoneVelocity milestone, String color, UnitToggle.Unit unit) {
        Span key = new Span(milestone.key());
        key.getStyle().set("font-size", "14px").set("font-weight", "700").set("color", color);

        Span name = new Span(nameOf(milestone));
        name.getStyle().set("font-size", "12px").set("color", DashboardStyle.MUTED)
                .set("overflow-wrap", "anywhere").set("line-height", "1.25");

        Div head = new Div(key, name);
        head.getStyle().set("display", "flex").set("flex-direction", "column").set("gap", "2px");

        Div grid = new Div(
                overviewMetric("Duration", days(milestone.durationDays())),
                overviewMetric("Total effort", unit.format(milestone.totalSpentSeconds())),
                overviewMetric("Weekly effort rate",
                        milestone.hasDuration() ? unit.format(milestone.effortThroughputSecondsPerWeek()) + "/wk" : "n/a"),
                overviewMetric("Team size", milestone.contributors() + " people"));
        grid.getStyle().set("display", "grid").set("grid-template-columns", "1fr 1fr")
                .set("gap", "8px").set("margin-top", "10px");

        Div epic = new Div(head, grid);
        epic.getStyle()
                .set("flex", "1 1 240px").set("min-width", "220px")
                .set("border", "1px solid #E5E8EA").set("border-top", "3px solid " + color)
                .set("border-radius", "10px").set("padding", "12px 14px")
                .set("background", "#FFFFFF").set("box-sizing", "border-box");
        return epic;
    }

    /** A compact metric inside an overview card: big value on top, small uppercase label below. */
    private Div overviewMetric(String label, String value) {
        Span valueSpan = new Span(value);
        valueSpan.getStyle()
                .set("font-size", value.length() > 12 ? "15px" : "18px")
                .set("font-weight", "700")
                .set("color", DashboardStyle.INK)
                .set("white-space", "nowrap")
                .set("line-height", "1.2");

        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("font-size", "10px").set("font-weight", "600").set("color", DashboardStyle.MUTED)
                .set("text-transform", "uppercase").set("letter-spacing", "0.04em")
                .set("white-space", "nowrap").set("overflow", "hidden").set("text-overflow", "ellipsis");

        Div tile = new Div(valueSpan, labelSpan);
        tile.getStyle()
                .set("display", "flex").set("flex-direction", "column").set("gap", "2px")
                .set("min-width", "0").set("padding", "8px 10px")
                .set("background", "#F8FAFB").set("border-radius", "8px").set("box-sizing", "border-box");
        return tile;
    }

    // ── delivery velocity: the single effort-over-time chart ──────────────────────

    /**
     * "Delivery velocity" card: how the absolute effort of each milestone evolved over its
     * delivery, one line per milestone. The only chart in the feature — full width, legend beneath.
     */
    private Div deliveryVelocityCard(UnitToggle.Unit unit) {
        Div card = card(DashboardStyle.EPIC);

        H4 header = new H4("Delivery velocity");
        header.getStyle().set("margin", "0").set("color", DashboardStyle.EPIC).set("font-weight", "600");

        Span unitChip = VelocityStyles.pill(
                unit == UnitToggle.Unit.MAN_DAYS ? "man-days" : "hours", DashboardStyle.SPENT);

        Div headerRow = new Div(header, unitChip);
        headerRow.getStyle()
                .set("display", "flex").set("justify-content", "space-between")
                .set("align-items", "center").set("margin-bottom", "12px");
        card.add(headerRow);

        List<MilestoneVelocity> milestones = report.milestones();
        List<EffortTimelineChart.Series> series = new ArrayList<>();
        for (int i = 0; i < milestones.size(); i++) {
            MilestoneVelocity milestone = milestones.get(i);
            if (!milestone.effortOverTime().isEmpty()) {
                List<Long> seconds = milestone.effortOverTime().stream().map(EffortBucket::seconds).toList();
                series.add(new EffortTimelineChart.Series(milestone.key(), seconds, VelocityStyles.colorFor(i)));
            }
        }
        if (series.isEmpty()) {
            card.add(VelocityStyles.emptyState(VaadinIcon.CHART_LINE, "No dated effort to plot",
                    "The selected milestones have no worklogs with dates."));
            return card;
        }

        int bucketDays = bucketWidthDays(milestones);
        String window = bucketDays == 7 ? "week" : bucketDays + "-day window";
        Div chart = new Div(
                DashboardStyle.note("Effort logged per " + window + " since each milestone's start"),
                new EffortTimelineChart(series, bucketDays, unit::format));
        chart.getStyle().set("display", "flex").set("flex-direction", "column").set("gap", "6px");
        card.add(chart);
        return card;
    }

    /** Bucket width in days, read off the spacing of any milestone's effort windows (else 7). */
    private int bucketWidthDays(List<MilestoneVelocity> milestones) {
        for (MilestoneVelocity milestone : milestones) {
            List<EffortBucket> buckets = milestone.effortOverTime();
            if (buckets.size() >= 2) {
                return buckets.get(1).dayOffset() - buckets.get(0).dayOffset();
            }
        }
        return 7;
    }

    // ── compare milestones: grouped detail table + per-person absolute effort ─────

    /** Left label column plus one flexible column per compared milestone, filling the width. */
    private String gridColumns() {
        return "minmax(160px, 240px) repeat(" + report.milestones().size() + ", minmax(140px, 1fr))";
    }

    /**
     * "Compare milestones": the side-by-side detail, one colour-coded column per milestone, with
     * rows grouped into Delivery dates / Effort / Team. The Team group ends with each contributor's
     * absolute effort per milestone shown as horizontal bars, so people are easy to scan.
     */
    private Div compareCard(UnitToggle.Unit unit) {
        Div card = card(DashboardStyle.SPENT);

        H4 header = new H4("Compare milestones");
        header.getStyle().set("margin", "0 0 12px 0").set("color", DashboardStyle.SPENT).set("font-weight", "600");
        card.add(header);

        List<MilestoneVelocity> milestones = report.milestones();
        if (milestones.isEmpty()) {
            card.add(VelocityStyles.emptyState(VaadinIcon.FLAG_O, "No milestones selected",
                    "Pick one or more delivered milestones above and press Search."));
            return card;
        }

        int fastest = report.fastest().map(MilestoneVelocity::durationDays).orElse(0);

        Div table = new Div();
        table.getStyle().set("display", "grid").set("grid-template-columns", gridColumns())
                .set("width", "100%").set("box-sizing", "border-box");

        table.add(cornerCell());
        for (int i = 0; i < milestones.size(); i++) {
            table.add(milestoneHeaderCell(milestones.get(i), VelocityStyles.colorFor(i)));
        }

        groupBand(table, "Delivery dates", milestones.size());
        addRow(table, "Duration", milestones, m -> durationCell(m, fastest), false);
        addRow(table, "Started", milestones, m -> textCell(date(m.startDate())), false);
        addRow(table, "Delivered", milestones, m -> textCell(date(m.deliveryDate())), false);
        addRow(table, "Planned delivery", milestones, m -> textCell(date(m.plannedDeliveryDate())), false);

        groupBand(table, "Effort", milestones.size());
        addRow(table, "Total effort", milestones, m -> textCell(unit.format(m.totalSpentSeconds())), false);
        addRow(table, "Estimate", milestones, m -> textCell(m.hasEstimate() ? unit.format(m.estimateSeconds()) : "n/a"), false);
        addRow(table, "Consumption", milestones, this::consumptionCell, false);
        addRow(table, "Variance", milestones, m -> varianceCell(m, unit), false);
        addRow(table, "Active days", milestones, m -> textCell(m.hasActiveDays() ? m.activeDays() + " d" : "n/a"), false);
        addRow(table, "Effort per active day", milestones,
                m -> textCell(m.hasActiveDays() ? unit.formatPerDay(m.effectivePaceSeconds()) : "n/a"), false);
        addRow(table, "Effort per week", milestones,
                m -> textCell(m.hasDuration() ? unit.format(m.effortThroughputSecondsPerWeek()) + "/wk" : "n/a"), false);
        addRow(table, "Effort per open day", milestones, m -> textCell(unit.formatPerDay(m.secondsPerDay())), false);

        groupBand(table, "Team", milestones.size());
        addRow(table, "Contributors", milestones, m -> textCell(String.valueOf(m.contributors())), false);
        addPersonBars(table, milestones, unit);

        Div scroller = new Div(table);
        scroller.getStyle().set("width", "100%").set("overflow-x", "auto").set("box-sizing", "border-box");
        card.add(scroller);
        return card;
    }

    /** A full-width band that titles a group of rows, spanning every column of the grid. */
    private void groupBand(Div table, String text, int epicCount) {
        table.add(sectionCell(text));
        for (int i = 0; i < epicCount; i++) {
            table.add(sectionCell(""));
        }
    }

    /**
     * "Absolute effort per person": one row per contributor, each milestone's cell a horizontal bar
     * whose length is that person's man-days on it (shared scale across the selection) with the
     * absolute figure above it. People with no effort on a milestone read "0 MD".
     */
    private void addPersonBars(Div table, List<MilestoneVelocity> milestones, UnitToggle.Unit unit) {
        if (report.perPerson().isEmpty()) {
            return;
        }
        groupBand(table, "Absolute effort per person", milestones.size());
        long maxPerson = maxPersonSeconds(milestones);
        for (PersonVelocity person : report.perPerson()) {
            table.add(labelCell(person.name(), false));
            for (int i = 0; i < milestones.size(); i++) {
                long seconds = milestones.get(i).secondsByPerson().getOrDefault(person.name(), 0L);
                table.add(personBarCell(seconds, maxPerson, VelocityStyles.colorFor(i), unit));
            }
        }
    }

    /** Largest single person-on-milestone effort across the selection, for a shared bar scale. */
    private long maxPersonSeconds(List<MilestoneVelocity> milestones) {
        long max = 0;
        for (MilestoneVelocity milestone : milestones) {
            for (long seconds : milestone.secondsByPerson().values()) {
                max = Math.max(max, seconds);
            }
        }
        return max;
    }

    /** One person-on-milestone cell: absolute effort label above a colour bar on the shared scale. */
    private Div personBarCell(long seconds, long maxSeconds, String color, UnitToggle.Unit unit) {
        Span label = new Span(unit.format(seconds));
        label.getStyle().set("font-size", "12px").set("white-space", "nowrap")
                .set("color", seconds > 0 ? DashboardStyle.INK : DashboardStyle.MUTED);

        double pct = maxSeconds > 0 ? seconds * 100.0 / maxSeconds : 0;
        Div bar = new Div();
        bar.getStyle().set("height", "6px").set("border-radius", "3px")
                .set("width", (seconds > 0 ? Math.max(4.0, pct) : 0) + "%")
                .set("background", color);

        Div track = new Div(bar);
        track.getStyle().set("width", "100%").set("height", "6px").set("border-radius", "3px")
                .set("background", "#EEF1F3");

        Div cell = new Div(label, track);
        cell.getStyle()
                .set("display", "flex").set("flex-direction", "column").set("gap", "5px")
                .set("padding", "7px 10px").set("box-sizing", "border-box")
                .set("min-width", "0").set("border-bottom", "1px solid #F0F2F4");
        return cell;
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
        keySpan.getStyle().set("font-size", "13px").set("font-weight", "700").set("color", color);

        Span nameSpan = new Span(nameOf(milestone));
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

    /** Consumption %: red and bold when the milestone burned past its estimate. */
    private Div consumptionCell(MilestoneVelocity milestone) {
        if (!milestone.hasEstimate()) {
            return textCell("n/a");
        }
        Div cell = textCell(milestone.consumptionPct() + "%");
        if (milestone.overBudget()) {
            cell.getStyle().set("color", DashboardStyle.OVER).set("font-weight", "700");
        }
        return cell;
    }

    /** Effort variance vs estimate: red when over budget, green when under. */
    private Div varianceCell(MilestoneVelocity milestone, UnitToggle.Unit unit) {
        if (!milestone.hasEstimate()) {
            return textCell("n/a");
        }
        long variance = milestone.varianceSeconds();
        Div cell = textCell(signed(variance, unit));
        cell.getStyle().set("color",
                variance > 0 ? DashboardStyle.OVER : variance < 0 ? DashboardStyle.ON_TRACK : DashboardStyle.MUTED);
        return cell;
    }

    /** Full-width band that titles a group of rows (e.g. "Effort"). */
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

    /** Effort with an explicit sign, e.g. {@code "+3.5 MD"} / {@code "-1.0 MD"}. */
    private String signed(long seconds, UnitToggle.Unit unit) {
        return (seconds < 0 ? "-" : "+") + unit.format(Math.abs(seconds));
    }

    /** Milestone name, or an empty string when it has none. */
    private String nameOf(MilestoneVelocity milestone) {
        return milestone.name() == null ? "" : milestone.name();
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
}

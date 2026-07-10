package com.example.timetracking.milestone.ui.widget;

import com.example.timetracking.milestone.application.dto.Progress;
import com.example.timetracking.milestone.ui.style.DashboardStyle;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.List;
import java.util.Locale;

public class EpicProgressTabWidget extends Div {

    public EpicProgressTabWidget(List<Progress> epics) {
        setWidthFull();
        if (epics == null || epics.isEmpty()) {
            add(DashboardStyle.note("This milestone has no epics."));
            return;
        }
        epics.forEach(epic -> add(epicRow(epic)));
    }

    private CollapsibleSection epicRow(Progress epic) {
        DetailsRow header = new DetailsRow(
                epic.key() + " - " + epic.name(),
                epic.displayPercent(),
                String.format(Locale.US, "%s / %s MD",
                        DashboardStyle.manDays(epic.spentSeconds()),
                        DashboardStyle.manDays(epic.estimatedSeconds())),
                DashboardStyle.EPIC,
                DashboardStyle.statusPill(epic));

        return new CollapsibleSection(header, issueList(epic.children()));
    }

    private Div issueList(List<Progress> issues) {
        Div wrapper = new Div();
        wrapper.setWidthFull();
        if (issues == null || issues.isEmpty()) {
            wrapper.add(DashboardStyle.note("This epic has no issues."));
            return wrapper;
        }

        VerticalLayout list = new VerticalLayout();
        list.setPadding(false);
        list.setSpacing(false);
        list.setWidthFull();
        issues.forEach(issue -> list.add(new IssueSubRow(
                issue.key() + " - " + issue.name(),
                issue.displayPercent(),
                String.format(Locale.US, "%s / %s MD",
                        DashboardStyle.manDays(issue.spentSeconds()),
                        DashboardStyle.manDays(issue.estimatedSeconds())),
                DashboardStyle.SPENT,
                DashboardStyle.statusPill(issue))));
        wrapper.add(list);
        return wrapper;
    }
}
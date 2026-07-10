package com.example.timetracking.historical;

import com.example.timetracking.views.MainLayout;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route(value = "historical", layout = MainLayout.class)
public class HistoricalView extends VerticalLayout {
    public HistoricalView() {
        add(new H1("Historical"));
    }
}

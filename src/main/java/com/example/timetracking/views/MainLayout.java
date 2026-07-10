package com.example.timetracking.views;

import com.example.timetracking.historical.HistoricalView;
import com.example.timetracking.milestone.MilestoneView;
import com.example.timetracking.velocity.VelocityView;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;

public class MainLayout extends AppLayout {

    public MainLayout() {
        setPrimarySection(Section.DRAWER);
        addToNavbar(buildHeader());
        addToDrawer(buildSideNav());
    }

    private Header buildHeader() {
        DrawerToggle toggle = new DrawerToggle();
        H2 title = new H2("Time Tracking Dashboard");
        title.getStyle().set("margin", "0").set("font-size", "1.25rem");

        Header header = new Header(toggle, title);
        header.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("gap", "0.5rem")
                .set("padding", "0.5rem 1rem");
        return header;
    }

    private SideNav buildSideNav() {
        SideNav nav = new SideNav();
        nav.addItem(new SideNavItem("Milestone", MilestoneView.class, VaadinIcon.TASKS.create()));
        nav.addItem(new SideNavItem("Historical", HistoricalView.class, VaadinIcon.CHART_LINE.create()));
        nav.addItem(new SideNavItem("Velocity", VelocityView.class, VaadinIcon.DASHBOARD.create()));
        return nav;
    }
}

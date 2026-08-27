package com.example.timetracking.gantt.application.model;

/**
 * A role within the Argentina team. Each role carries a human-readable label and a
 * distinct bar colour so the "By role" Gantt reads at a glance.
 *
 * <p>Self-contained to the {@code gantt} feature: it does not depend on any Jira or
 * milestone type. When real data arrives, mapping an external role onto this enum is
 * the only wiring needed.
 */
public enum Role {

    BACKEND("Backend", "#0F4660"),
    FRONTEND("Frontend", "#2C8FB5"),
    FULL_STACK("Full Stack", "#6554C0"),
    DEVOPS("DevOps", "#B36A00"),
    MOBILE("Mobile Developer", "#2E7D32");

    private final String label;
    private final String color;

    Role(String label, String color) {
        this.label = label;
        this.color = color;
    }

    public String label() {
        return label;
    }

    public String color() {
        return color;
    }
}

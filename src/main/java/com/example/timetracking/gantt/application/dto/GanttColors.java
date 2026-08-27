package com.example.timetracking.gantt.application.dto;

/**
 * Bar colours for the "By team" Gantt, kept in one place so use cases and the UI agree.
 *
 * <p>Aggregate rows (milestone, epic) use level colours; leaf issue rows are coloured by
 * workflow status so progress reads at a glance.
 */
public final class GanttColors {

    public static final String MILESTONE = "#0F4660"; // deep navy
    public static final String EPIC = "#6554C0";      // purple

    public static final String STATUS_DONE = "#2E7D32";        // green
    public static final String STATUS_IN_PROGRESS = "#2C8FB5"; // blue
    public static final String STATUS_TODO = "#9AA5AB";        // grey

    private GanttColors() {
    }

    /** Colour for a leaf issue bar, by its workflow status. */
    public static String forStatus(String status) {
        if (status == null) {
            return STATUS_TODO;
        }
        return switch (status) {
            case "Done" -> STATUS_DONE;
            case "In Progress" -> STATUS_IN_PROGRESS;
            default -> STATUS_TODO;
        };
    }
}

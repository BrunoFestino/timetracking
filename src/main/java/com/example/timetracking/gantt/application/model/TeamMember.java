package com.example.timetracking.gantt.application.model;

/**
 * A member of the Argentina team, identified by display name and {@link Role}.
 *
 * @param name display name, e.g. {@code "Lucía Fernández"}
 * @param role the member's role, which drives grouping and bar colour in the "By role" Gantt
 */
public record TeamMember(String name, Role role) {
}

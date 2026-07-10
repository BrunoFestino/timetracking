package com.example.timetracking.velocity.application.dto;

import java.util.List;

/**
 * Full velocity computation for a set of milestones. All figures are stored in
 * seconds; the UI converts to hours or man-days according to the active unit.
 *
 * @param milestones              per-milestone figures, in selection order
 * @param totalSeconds            team total across all selected milestones
 * @param avgSecondsPerMilestone  team average per milestone
 * @param teamPerWeek             team totals per relative week, ordered by week
 * @param perPerson               per-person averages, ordered by total desc
 */
public record VelocityReport(
        List<MilestoneVelocity> milestones,
        long totalSeconds,
        long avgSecondsPerMilestone,
        List<WeekVelocity> teamPerWeek,
        List<PersonVelocity> perPerson) {

    public VelocityReport {
        milestones = milestones == null ? List.of() : List.copyOf(milestones);
        teamPerWeek = teamPerWeek == null ? List.of() : List.copyOf(teamPerWeek);
        perPerson = perPerson == null ? List.of() : List.copyOf(perPerson);
    }
}

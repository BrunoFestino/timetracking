package com.example.timetracking.velocity.application.dto;

import java.util.List;

/**
 * Full velocity computation for a set of milestones. All figures are stored in
 * seconds; the UI converts to hours or man-days according to the active unit.
 *
 * @param milestones            per-milestone figures, in selection order
 * @param totalSeconds          team total across all selected milestones
 * @param teamAvgSecondsPerWeek team velocity: total divided by the highest observed
 *                              relative week (0 if no worklogs)
 * @param perPerson             per-person weekly velocity, ordered by total desc
 */
public record VelocityReport(
        List<MilestoneVelocity> milestones,
        long totalSeconds,
        long teamAvgSecondsPerWeek,
        List<PersonVelocity> perPerson) {

    public VelocityReport {
        milestones = milestones == null ? List.of() : List.copyOf(milestones);
        perPerson = perPerson == null ? List.of() : List.copyOf(perPerson);
    }
}

package com.example.timetracking.velocity.application.dto;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Delivery velocity for a set of delivered milestones, possibly spanning several projects:
 * how long each one took to finish, plus the same selection seen per person. All effort is
 * stored in seconds; the UI converts to hours or man-days according to the active unit.
 * Durations are calendar days.
 *
 * @param milestones   per-milestone figures, in selection order
 * @param totalSeconds team effort across the whole selection
 * @param perPerson    per-person figures, ordered by effort desc
 */
public record VelocityReport(
        List<MilestoneVelocity> milestones,
        long totalSeconds,
        List<PersonVelocity> perPerson) {

    public VelocityReport {
        milestones = milestones == null ? List.of() : List.copyOf(milestones);
        perPerson = perPerson == null ? List.of() : List.copyOf(perPerson);
    }

    /** Number of contributors who logged work across the selection. */
    public int contributors() {
        return perPerson.size();
    }

    /**
     * Team delivery velocity: the average number of calendar days a milestone of this
     * selection took to finish. Milestones whose window could not be dated are left out,
     * so an undatable one does not drag the average toward zero.
     */
    public int avgDurationDays() {
        List<MilestoneVelocity> datable = datable();
        if (datable.isEmpty()) {
            return 0;
        }
        return (int) Math.round(datable.stream().mapToInt(MilestoneVelocity::durationDays).average().orElse(0));
    }

    /** Average effort spent on a milestone of this selection. */
    public long avgSecondsPerMilestone() {
        return milestones.isEmpty() ? 0 : totalSeconds / milestones.size();
    }

    /** The milestone that finished in the fewest days, if any is datable. */
    public Optional<MilestoneVelocity> fastest() {
        return datable().stream().min(Comparator.comparingInt(MilestoneVelocity::durationDays));
    }

    /** The milestone that took the most days, if any is datable. */
    public Optional<MilestoneVelocity> slowest() {
        return datable().stream().max(Comparator.comparingInt(MilestoneVelocity::durationDays));
    }

    /** Milestones with a known start and delivery date — the ones duration figures come from. */
    public List<MilestoneVelocity> datable() {
        return milestones.stream().filter(MilestoneVelocity::hasDuration).toList();
    }
}

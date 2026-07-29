package com.example.timetracking.velocity.application.dto;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Delivery velocity for a set of delivered milestones within a project: how long each one took
 * to finish, plus the same selection seen per person. All effort is stored in seconds; the UI
 * converts to hours or man-days according to the active unit. Durations are calendar days.
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

    /** Milestones with a known start and delivery date — the ones duration figures come from. */
    public List<MilestoneVelocity> datable() {
        return milestones.stream().filter(MilestoneVelocity::hasDuration).toList();
    }

    // ── extremes per dimension, for the comparison overview ────────────────────
    // Each delta in the overview is the gap between the selection's extremes on one dimension,
    // so a two-milestone selection reduces to a straight A-vs-B comparison. Empty when a
    // dimension has nothing to compare (e.g. no dated milestone for duration/pace).

    /** Milestone delivered in the fewest days, among the datable ones. */
    public Optional<MilestoneVelocity> fastest() {
        return datable().stream().min(Comparator.comparingInt(MilestoneVelocity::durationDays));
    }

    /** Milestone that took the most days, among the datable ones. */
    public Optional<MilestoneVelocity> slowest() {
        return datable().stream().max(Comparator.comparingInt(MilestoneVelocity::durationDays));
    }

    /** Milestone with the most logged effort. */
    public Optional<MilestoneVelocity> heaviestByEffort() {
        return milestones.stream().max(Comparator.comparingLong(MilestoneVelocity::totalSpentSeconds));
    }

    /** Milestone with the least logged effort. */
    public Optional<MilestoneVelocity> lightestByEffort() {
        return milestones.stream().min(Comparator.comparingLong(MilestoneVelocity::totalSpentSeconds));
    }

    /** Densest delivery (most effort per open day), among the datable ones. */
    public Optional<MilestoneVelocity> densest() {
        return datable().stream().max(Comparator.comparingLong(MilestoneVelocity::secondsPerDay));
    }

    /** Sparsest delivery (least effort per open day), among the datable ones. */
    public Optional<MilestoneVelocity> sparsest() {
        return datable().stream().min(Comparator.comparingLong(MilestoneVelocity::secondsPerDay));
    }

    /** Milestone worked on by the most people. */
    public Optional<MilestoneVelocity> mostContributors() {
        return milestones.stream().max(Comparator.comparingInt(MilestoneVelocity::contributors));
    }

    /** Milestone worked on by the fewest people. */
    public Optional<MilestoneVelocity> fewestContributors() {
        return milestones.stream().min(Comparator.comparingInt(MilestoneVelocity::contributors));
    }
}

package com.example.timetracking.velocity.application.dto;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * How long one delivered milestone took, what it cost, and how that cost compares with what was
 * planned.
 *
 * <p>Milestones from different projects are directly comparable: the figures below describe
 * the milestone's own delivery window, with no dependency on the calendar or on the rest of
 * the selection.
 *
 * @param key                 Jira key, e.g. {@code TTAR-9625}
 * @param name                milestone summary
 * @param projectKey          owning project key, so cross-project selections stay readable
 * @param type                Jira issue type, kept for display only — milestones of any type compare
 * @param startDate           start of the delivery window (start-date field, else first worklog)
 * @param deliveryDate        actual delivery (effective delivery date, else resolution, else last worklog)
 * @param plannedDeliveryDate baseline (planned) delivery date, {@code null} when not planned
 * @param durationDays        inclusive calendar days from start to delivery; {@code 0} when unknown
 * @param totalSpentSeconds   effort logged on the whole milestone tree
 * @param estimateSeconds     planned effort for the whole tree (up-front man-day estimate, else
 *                            Jira original estimate); {@code 0} when nothing was estimated
 * @param effortOverTime      effort bucketed into fixed windows since start, dense and gap-filled
 * @param secondsByPerson     effort per contributor, ordered by effort desc
 */
public record MilestoneVelocity(
        String key,
        String name,
        String projectKey,
        String type,
        LocalDate startDate,
        LocalDate deliveryDate,
        LocalDate plannedDeliveryDate,
        int durationDays,
        long totalSpentSeconds,
        long estimateSeconds,
        List<EffortBucket> effortOverTime,
        Map<String, Long> secondsByPerson) {

    public MilestoneVelocity {
        // LinkedHashMap rather than Map.copyOf: the effort-desc order is part of the contract.
        secondsByPerson = secondsByPerson == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(secondsByPerson));
        effortOverTime = effortOverTime == null ? List.of() : List.copyOf(effortOverTime);
    }

    /** Whether start and delivery are both known, i.e. the duration figures are meaningful. */
    public boolean hasDuration() {
        return durationDays > 0;
    }

    /** People who logged work on this milestone. */
    public int contributors() {
        return secondsByPerson.size();
    }

    /**
     * Delivery pace: effort spread over the calendar days the milestone was open.
     * Two milestones of the same duration but different pace mean different team sizes
     * or different amounts of idle time.
     */
    public long secondsPerDay() {
        return hasDuration() ? totalSpentSeconds / durationDays : 0;
    }

    // ── estimate baseline ──────────────────────────────────────────────────────

    /** Whether this milestone carries a planned-effort estimate to compare the actual against. */
    public boolean hasEstimate() {
        return estimateSeconds > 0;
    }

    /**
     * Share of the estimate consumed by logged effort, as a whole-number percent (e.g. {@code 120}
     * means 20% over budget). {@code 0} when nothing was estimated — guard with {@link #hasEstimate()}.
     */
    public long consumptionPct() {
        return hasEstimate() ? Math.round(totalSpentSeconds * 100.0 / estimateSeconds) : 0;
    }

    /**
     * Effort variance against the estimate: logged minus planned. Positive means over budget,
     * negative means under. {@code 0} when nothing was estimated — guard with {@link #hasEstimate()}.
     */
    public long varianceSeconds() {
        return hasEstimate() ? totalSpentSeconds - estimateSeconds : 0;
    }

    /** Whether logged effort exceeded the estimate. */
    public boolean overBudget() {
        return hasEstimate() && totalSpentSeconds > estimateSeconds;
    }

    // ── planned vs actual delivery ─────────────────────────────────────────────

    /** Whether a baseline (planned) delivery date is known to compare the actual against. */
    public boolean hasPlannedDelivery() {
        return plannedDeliveryDate != null && deliveryDate != null;
    }

    /**
     * Days the actual delivery slipped past the planned one: positive means late, negative early.
     * {@code 0} when there is no planned date — guard with {@link #hasPlannedDelivery()}.
     */
    public int scheduleSlipDays() {
        return hasPlannedDelivery()
                ? (int) (deliveryDate.toEpochDay() - plannedDeliveryDate.toEpochDay())
                : 0;
    }
}

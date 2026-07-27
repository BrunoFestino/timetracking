package com.example.timetracking.velocity.application.dto;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * How long one delivered milestone took, and what it cost.
 *
 * <p>Milestones from different projects are directly comparable: the figures below describe
 * the milestone's own delivery window, with no dependency on the calendar or on the rest of
 * the selection.
 *
 * @param key               Jira key, e.g. {@code TTAR-9625}
 * @param name              milestone summary
 * @param projectKey        owning project key, so cross-project selections stay readable
 * @param type              Jira issue type, kept for display only — milestones of any type compare
 * @param startDate           start of the delivery window (start-date field, else first worklog)
 * @param deliveryDate        actual delivery (effective delivery date, else resolution, else last worklog)
 * @param durationDays        inclusive calendar days from start to delivery; {@code 0} when unknown
 * @param plannedDate         date the milestone was meant to land (baseline, else due); {@code null} when unset
 * @param scheduleVarianceDays actual minus planned in days (+late/−early); {@code null} without a plan
 * @param totalSpentSeconds   effort logged on the whole milestone tree
 * @param secondsByPerson     effort per contributor, ordered by effort desc
 */
public record MilestoneVelocity(
        String key,
        String name,
        String projectKey,
        String type,
        LocalDate startDate,
        LocalDate deliveryDate,
        int durationDays,
        LocalDate plannedDate,
        Integer scheduleVarianceDays,
        long totalSpentSeconds,
        Map<String, Long> secondsByPerson) {

    public MilestoneVelocity {
        // LinkedHashMap rather than Map.copyOf: the effort-desc order is part of the contract.
        secondsByPerson = secondsByPerson == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(secondsByPerson));
    }

    /** Whether start and delivery are both known, i.e. the duration figures are meaningful. */
    public boolean hasDuration() {
        return durationDays > 0;
    }

    /** Whether the milestone carried a planned date, i.e. it can be judged on-time or late. */
    public boolean hasPlan() {
        return scheduleVarianceDays != null;
    }

    /** Whether the milestone landed on or before its planned date. False when there is no plan. */
    public boolean isOnTime() {
        return scheduleVarianceDays != null && scheduleVarianceDays <= 0;
    }

    /** People who logged work on this milestone. */
    public int contributors() {
        return secondsByPerson.size();
    }

    /**
     * Delivery intensity: effort spread over the calendar days the milestone was open.
     * Two milestones of the same duration but different intensity mean different team sizes
     * or different amounts of idle time.
     */
    public long secondsPerDay() {
        return hasDuration() ? totalSpentSeconds / durationDays : 0;
    }
}

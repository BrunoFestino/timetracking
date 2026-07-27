package com.example.timetracking.velocity.application.mapper;

import com.example.timetracking.milestone.domain.JiraMetadata;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Set;

/**
 * The single place where "is this milestone delivered, and between which dates?" is decided.
 *
 * <p>The velocity feature only looks at <b>delivered</b> milestones, so both the selector
 * (which milestones can be picked) and the aggregator (how long each one took) have to agree
 * on the same rule. Delivery is read from Jira in this order of confidence:
 *
 * <ol>
 *     <li>the <i>effective delivery date</i> custom field — set when a milestone is signed off;</li>
 *     <li>a terminal workflow status (Delivered / Done / Closed / Resolved / …) with a resolution date;</li>
 *     <li>a terminal status alone — delivered, but with the date falling back to the last logged work.</li>
 * </ol>
 *
 * <p>Planned dates ({@code dueDate}, {@code baselineDeliveryDate}) are deliberately never used as the
 * delivery date: they say when the milestone <i>should</i> have landed, not when it did.
 */
public final class MilestoneDelivery {

    /** Workflow statuses that mean the milestone is finished, lower-cased. */
    private static final Set<String> DELIVERED_STATUSES =
            Set.of("delivered", "done", "closed", "resolved", "completed", "finished", "released");

    private MilestoneDelivery() {
    }

    /** Whether the milestone has been delivered and can therefore be measured end to end. */
    public static boolean isDelivered(JiraMetadata metadata) {
        if (metadata == null) {
            return false;
        }
        return parseDate(metadata.effectiveDeliveryDate()) != null || hasDeliveredStatus(metadata);
    }

    /**
     * Actual delivery date: the effective delivery date, else the resolution date, else — for a
     * milestone closed without either — the last day work was logged on it. {@code null} when
     * none of the three is known.
     */
    public static LocalDate deliveryDate(JiraMetadata metadata, LocalDate lastWorklogDate) {
        if (metadata == null) {
            return lastWorklogDate;
        }
        LocalDate effective = parseDate(metadata.effectiveDeliveryDate());
        if (effective != null) {
            return effective;
        }
        LocalDate resolution = parseDate(metadata.resolutionDate());
        return resolution != null ? resolution : lastWorklogDate;
    }

    /**
     * Start of the delivery window: the start-date custom field, else the first day work was
     * logged on the milestone. {@code null} when neither is known.
     */
    public static LocalDate startDate(JiraMetadata metadata, LocalDate firstWorklogDate) {
        LocalDate start = metadata == null ? null : parseDate(metadata.startDate());
        return start != null ? start : firstWorklogDate;
    }

    /**
     * The date the milestone was <i>supposed</i> to land: the baseline delivery date, else the
     * due date. Unlike {@link #deliveryDate}, this is the plan, not the actual — it exists to be
     * compared against the actual to judge whether a delivery was on time. {@code null} when the
     * milestone carries no planned date.
     */
    public static LocalDate plannedDate(JiraMetadata metadata) {
        if (metadata == null) {
            return null;
        }
        LocalDate baseline = parseDate(metadata.baselineDeliveryDate());
        return baseline != null ? baseline : parseDate(metadata.dueDate());
    }

    /**
     * How far the actual delivery slipped from the plan, in calendar days: positive when late,
     * negative when early, zero when on the day. {@code null} when either date is unknown, so a
     * milestone without a plan is left out of on-time figures rather than counted as on time.
     */
    public static Integer scheduleVarianceDays(LocalDate planned, LocalDate delivery) {
        if (planned == null || delivery == null) {
            return null;
        }
        return (int) (delivery.toEpochDay() - planned.toEpochDay());
    }

    /** Inclusive calendar days between start and delivery; {@code 0} when the window is unknown. */
    public static int durationDays(LocalDate start, LocalDate delivery) {
        if (start == null || delivery == null || delivery.isBefore(start)) {
            return 0;
        }
        return (int) (delivery.toEpochDay() - start.toEpochDay()) + 1;
    }

    /** Parses the {@code yyyy-MM-dd} prefix of a Jira date or timestamp; {@code null} when unusable. */
    public static LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() >= 10) {
            trimmed = trimmed.substring(0, 10);
        }
        try {
            return LocalDate.parse(trimmed);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private static boolean hasDeliveredStatus(JiraMetadata metadata) {
        String status = metadata.status();
        return status != null && DELIVERED_STATUSES.contains(status.trim().toLowerCase(Locale.ROOT));
    }
}

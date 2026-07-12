package com.example.timetracking.velocity.application.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Team velocity over a range of calendar weeks. All figures are stored in seconds;
 * the UI converts to hours or man-days according to the active unit.
 *
 * @param from                  first day of the range (a Monday, after snapping)
 * @param to                    last day of the range (a Sunday, after snapping)
 * @param weeksInRange          number of calendar weeks in the range (the velocity denominator)
 * @param totalSeconds          team total logged within the range
 * @param teamAvgSecondsPerWeek team velocity: total divided by weeksInRange
 * @param weeks                 one entry per calendar week in the range, gaps included
 * @param perPerson             per-person weekly effort, ordered by total desc
 */
public record WeeklyVelocityReport(
        LocalDate from,
        LocalDate to,
        int weeksInRange,
        long totalSeconds,
        long teamAvgSecondsPerWeek,
        List<CalendarWeekVelocity> weeks,
        List<PersonWeeklyVelocity> perPerson) {

    public WeeklyVelocityReport {
        weeks = weeks == null ? List.of() : List.copyOf(weeks);
        perPerson = perPerson == null ? List.of() : List.copyOf(perPerson);
    }

    /** Number of contributors who logged work within the range. */
    public int contributors() {
        return perPerson.size();
    }

    /** Team velocity spread evenly over the contributors (avg weekly effort per person). */
    public long perContributorSecondsPerWeek() {
        return perPerson.isEmpty() ? 0 : teamAvgSecondsPerWeek / perPerson.size();
    }
}

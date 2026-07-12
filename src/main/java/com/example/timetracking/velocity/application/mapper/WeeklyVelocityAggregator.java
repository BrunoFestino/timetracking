package com.example.timetracking.velocity.application.mapper;

import com.example.timetracking.milestone.domain.JiraTicket;
import com.example.timetracking.milestone.domain.Worklog;
import com.example.timetracking.velocity.application.dto.CalendarWeekVelocity;
import com.example.timetracking.velocity.application.dto.IssueEffort;
import com.example.timetracking.velocity.application.dto.PersonWeeklyVelocity;
import com.example.timetracking.velocity.application.dto.WeeklyVelocityReport;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Turns a set of {@link JiraTicket} milestone trees into the {@link WeeklyVelocityReport}
 * model: team and per-person effort per calendar week (Monday to Sunday) within a range.
 *
 * <p>Unlike {@link VelocityAggregator} (weeks relative to each milestone's start), weeks
 * here are absolute calendar weeks, so effort across milestones lands on the real timeline.
 * Tickets reachable from more than one milestone tree are counted once.
 */
@Component
public class WeeklyVelocityAggregator {

    public WeeklyVelocityReport aggregate(List<JiraTicket> milestoneTrees, LocalDate from, LocalDate to) {
        Map<LocalDate, Long> teamByWeek = new TreeMap<>();
        Map<LocalDate, Map<String, Long>> weekPerson = new TreeMap<>();
        Map<LocalDate, Map<String, Long>> weekIssue = new TreeMap<>();
        Map<String, String> summaries = new LinkedHashMap<>();
        Map<String, Long> personTotals = new LinkedHashMap<>();
        Map<String, Map<LocalDate, Long>> personByWeek = new LinkedHashMap<>();
        Set<String> seenTickets = new HashSet<>();
        long totalSeconds = 0;

        for (JiraTicket tree : milestoneTrees) {
            totalSeconds += collect(tree, from, to, seenTickets,
                    teamByWeek, weekPerson, weekIssue, summaries, personTotals, personByWeek);
        }

        int weeksInRange = (int) ((to.toEpochDay() - from.toEpochDay() + 1) / 7);
        return new WeeklyVelocityReport(
                from,
                to,
                weeksInRange,
                totalSeconds,
                weeksInRange > 0 ? totalSeconds / weeksInRange : 0,
                weeks(from, weeksInRange, teamByWeek, weekPerson, weekIssue, summaries),
                persons(personTotals, personByWeek));
    }

    /** Walks the tree accumulating in-range worklogs; returns the seconds contributed. */
    private long collect(JiraTicket ticket, LocalDate from, LocalDate to, Set<String> seenTickets,
                         Map<LocalDate, Long> teamByWeek,
                         Map<LocalDate, Map<String, Long>> weekPerson,
                         Map<LocalDate, Map<String, Long>> weekIssue,
                         Map<String, String> summaries,
                         Map<String, Long> personTotals,
                         Map<String, Map<LocalDate, Long>> personByWeek) {
        long contributed = 0;
        if (seenTickets.add(ticket.key())) {
            for (Worklog worklog : ticket.worklogs()) {
                LocalDate date = parseDate(worklog.startedDate());
                if (date == null || date.isBefore(from) || date.isAfter(to)) {
                    continue;
                }
                LocalDate weekStart = date.with(DayOfWeek.MONDAY);
                String author = worklog.author() != null ? worklog.author() : "Unknown";
                long seconds = worklog.timeSpentSeconds();

                contributed += seconds;
                teamByWeek.merge(weekStart, seconds, Long::sum);
                weekPerson.computeIfAbsent(weekStart, k -> new LinkedHashMap<>())
                        .merge(author, seconds, Long::sum);
                weekIssue.computeIfAbsent(weekStart, k -> new LinkedHashMap<>())
                        .merge(ticket.key(), seconds, Long::sum);
                summaries.putIfAbsent(ticket.key(), ticket.summary());
                personTotals.merge(author, seconds, Long::sum);
                personByWeek.computeIfAbsent(author, k -> new TreeMap<>())
                        .merge(weekStart, seconds, Long::sum);
            }
        }
        for (JiraTicket child : ticket.children()) {
            contributed += collect(child, from, to, seenTickets,
                    teamByWeek, weekPerson, weekIssue, summaries, personTotals, personByWeek);
        }
        return contributed;
    }

    /** One entry per calendar week of the range, in order; weeks without work stay at zero. */
    private List<CalendarWeekVelocity> weeks(LocalDate from, int weeksInRange,
                                             Map<LocalDate, Long> teamByWeek,
                                             Map<LocalDate, Map<String, Long>> weekPerson,
                                             Map<LocalDate, Map<String, Long>> weekIssue,
                                             Map<String, String> summaries) {
        List<CalendarWeekVelocity> weeks = new ArrayList<>();
        for (int i = 0; i < weeksInRange; i++) {
            LocalDate weekStart = from.plusWeeks(i);
            weeks.add(new CalendarWeekVelocity(
                    weekStart,
                    teamByWeek.getOrDefault(weekStart, 0L),
                    sortedByValueDesc(weekPerson.getOrDefault(weekStart, Map.of())),
                    issues(weekIssue.getOrDefault(weekStart, Map.of()), summaries)));
        }
        return weeks;
    }

    private Map<String, Long> sortedByValueDesc(Map<String, Long> values) {
        Map<String, Long> sorted = new LinkedHashMap<>();
        values.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(e -> sorted.put(e.getKey(), e.getValue()));
        return sorted;
    }

    private List<IssueEffort> issues(Map<String, Long> issueSeconds, Map<String, String> summaries) {
        return issueSeconds.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> new IssueEffort(e.getKey(), summaries.get(e.getKey()), e.getValue()))
                .toList();
    }

    private List<PersonWeeklyVelocity> persons(Map<String, Long> personTotals,
                                               Map<String, Map<LocalDate, Long>> personByWeek) {
        return personTotals.entrySet().stream()
                .map(e -> new PersonWeeklyVelocity(e.getKey(), e.getValue(),
                        personByWeek.getOrDefault(e.getKey(), Map.of())))
                .sorted(Comparator.comparingLong(PersonWeeklyVelocity::totalSeconds).reversed())
                .toList();
    }

    private LocalDate parseDate(String value) {
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
}

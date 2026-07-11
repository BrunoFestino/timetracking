package com.example.timetracking.velocity.application.mapper;

import com.example.timetracking.milestone.domain.JiraMetadata;
import com.example.timetracking.milestone.domain.JiraTicket;
import com.example.timetracking.milestone.domain.Worklog;
import com.example.timetracking.velocity.application.dto.MilestoneVelocity;
import com.example.timetracking.velocity.application.dto.PersonVelocity;
import com.example.timetracking.velocity.application.dto.VelocityReport;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Turns a set of {@link JiraTicket} milestone trees into the {@link VelocityReport} model:
 * per-milestone and per-person weekly velocity (average logged effort per week), plus
 * team-level summary figures (peak week, active weeks).
 *
 * <p>Weeks are relative to each milestone's start (week 1 = start), so the ramp-up of
 * different milestones can be compared side by side.
 */
@Component
public class VelocityAggregator {

    public VelocityReport aggregate(List<JiraTicket> milestones) {
        List<MilestoneVelocity> perMilestone = new ArrayList<>();
        Map<String, Long> personTotals = new LinkedHashMap<>();
        Map<String, Map<String, Map<Integer, Long>>> personByMilestoneWeek = new LinkedHashMap<>();
        Map<Integer, Long> teamByWeek = new TreeMap<>();
        long totalSeconds = 0;
        int teamObservedWeeks = 0;

        for (JiraTicket milestone : milestones) {
            List<Worklog> worklogs = new ArrayList<>();
            collect(milestone, worklogs);
            LocalDate start = startOf(milestone, worklogs);

            Map<Integer, Long> secondsByWeek = new TreeMap<>();
            long milestoneTotal = 0;
            for (Worklog worklog : worklogs) {
                long seconds = worklog.timeSpentSeconds();
                int week = weekOf(start, worklog.startedDate());
                String author = worklog.author() != null ? worklog.author() : "Unknown";

                milestoneTotal += seconds;
                secondsByWeek.merge(week, seconds, Long::sum);
                teamByWeek.merge(week, seconds, Long::sum);
                personTotals.merge(author, seconds, Long::sum);
                personByMilestoneWeek.computeIfAbsent(author, k -> new LinkedHashMap<>())
                        .computeIfAbsent(milestone.key(), k -> new TreeMap<>())
                        .merge(week, seconds, Long::sum);
                teamObservedWeeks = Math.max(teamObservedWeeks, week);
            }
            totalSeconds += milestoneTotal;
            perMilestone.add(new MilestoneVelocity(
                    milestone.key(),
                    milestone.summary(),
                    milestoneTotal,
                    durationWeeks(milestone, start, worklogs),
                    start,
                    secondsByWeek));
        }

        Map.Entry<Integer, Long> peak = teamByWeek.entrySet().stream()
                .max(Comparator.comparingLong(Map.Entry::getValue))
                .orElse(null);

        return new VelocityReport(
                perMilestone,
                totalSeconds,
                teamObservedWeeks > 0 ? totalSeconds / teamObservedWeeks : 0,
                teamObservedWeeks,
                peak != null ? peak.getKey() : 0,
                peak != null ? peak.getValue() : 0,
                persons(personTotals, personByMilestoneWeek));
    }

    private List<PersonVelocity> persons(Map<String, Long> personTotals,
                                         Map<String, Map<String, Map<Integer, Long>>> personByMilestoneWeek) {
        return personTotals.entrySet().stream()
                .map(e -> new PersonVelocity(e.getKey(), e.getValue(),
                        personByMilestoneWeek.getOrDefault(e.getKey(), Map.of())))
                .sorted(Comparator.comparingLong(PersonVelocity::totalSeconds).reversed())
                .toList();
    }

    private void collect(JiraTicket ticket, List<Worklog> into) {
        into.addAll(ticket.worklogs());
        ticket.children().forEach(child -> collect(child, into));
    }

    /**
     * Milestone start: the {@code startDate} custom field, falling back to the earliest worklog date.
     */
    private LocalDate startOf(JiraTicket milestone, List<Worklog> worklogs) {
        LocalDate start = parseDate(milestone.metadata().startDate());
        if (start != null) {
            return start;
        }
        return worklogs.stream()
                .map(w -> parseDate(w.startedDate()))
                .filter(java.util.Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null);
    }

    /**
     * Milestone duration in weeks (min 1): start to the first available end date
     * (effective → baseline → due → resolution), falling back to the latest worklog date.
     */
    private int durationWeeks(JiraTicket milestone, LocalDate start, List<Worklog> worklogs) {
        JiraMetadata metadata = milestone.metadata();
        LocalDate end = firstNonNullDate(
                metadata.effectiveDeliveryDate(),
                metadata.baselineDeliveryDate(),
                metadata.dueDate(),
                metadata.resolutionDate());
        if (end == null) {
            end = worklogs.stream()
                    .map(w -> parseDate(w.startedDate()))
                    .filter(java.util.Objects::nonNull)
                    .max(Comparator.naturalOrder())
                    .orElse(null);
        }
        if (start == null || end == null || end.isBefore(start)) {
            return 1;
        }
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        return (int) Math.max(1, (days + 6) / 7);
    }

    /**
     * Relative week of a worklog: week 1 starts at the milestone start date.
     */
    private int weekOf(LocalDate start, String worklogDate) {
        LocalDate date = parseDate(worklogDate);
        if (start == null || date == null || date.isBefore(start)) {
            return 1;
        }
        return (int) (ChronoUnit.DAYS.between(start, date) / 7) + 1;
    }

    private LocalDate firstNonNullDate(String... values) {
        for (String value : values) {
            LocalDate date = parseDate(value);
            if (date != null) {
                return date;
            }
        }
        return null;
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

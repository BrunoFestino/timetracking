package com.example.timetracking.velocity.application.mapper;

import com.example.timetracking.milestone.domain.JiraMetadata;
import com.example.timetracking.milestone.domain.JiraTicket;
import com.example.timetracking.milestone.domain.Worklog;
import com.example.timetracking.velocity.application.dto.MilestoneVelocity;
import com.example.timetracking.velocity.application.dto.PersonVelocity;
import com.example.timetracking.velocity.application.dto.VelocityReport;
import com.example.timetracking.velocity.application.dto.WeekVelocity;
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
 * team totals, per relative-week throughput and per-person averages.
 *
 * <p>Weeks are relative to each milestone's start (week 1 = start), so the ramp-up of
 * different milestones can be compared side by side.
 */
@Component
public class VelocityAggregator {

    public VelocityReport aggregate(List<JiraTicket> milestones) {
        List<MilestoneVelocity> perMilestone = new ArrayList<>();
        Map<Integer, Long> weekTotals = new TreeMap<>();
        Map<Integer, Map<String, Long>> weekByMilestone = new TreeMap<>();
        Map<String, Long> personTotals = new LinkedHashMap<>();
        Map<String, Map<String, Long>> personByMilestone = new LinkedHashMap<>();
        long totalSeconds = 0;

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
                weekTotals.merge(week, seconds, Long::sum);
                weekByMilestone.computeIfAbsent(week, k -> new LinkedHashMap<>())
                        .merge(milestone.key(), seconds, Long::sum);
                personTotals.merge(author, seconds, Long::sum);
                personByMilestone.computeIfAbsent(author, k -> new LinkedHashMap<>())
                        .merge(milestone.key(), seconds, Long::sum);
            }
            totalSeconds += milestoneTotal;
            perMilestone.add(new MilestoneVelocity(
                    milestone.key(),
                    milestone.summary(),
                    milestoneTotal,
                    durationWeeks(milestone, start, worklogs),
                    secondsByWeek));
        }

        int milestoneCount = milestones.size();
        return new VelocityReport(
                perMilestone,
                totalSeconds,
                milestoneCount > 0 ? totalSeconds / milestoneCount : 0,
                weeks(weekTotals, weekByMilestone),
                persons(personTotals, personByMilestone, milestoneCount));
    }

    private List<WeekVelocity> weeks(Map<Integer, Long> weekTotals, Map<Integer, Map<String, Long>> weekByMilestone) {
        return weekTotals.entrySet().stream()
                .map(e -> new WeekVelocity(e.getKey(), e.getValue(),
                        weekByMilestone.getOrDefault(e.getKey(), Map.of())))
                .toList();
    }

    private List<PersonVelocity> persons(Map<String, Long> personTotals,
                                         Map<String, Map<String, Long>> personByMilestone, int milestoneCount) {
        return personTotals.entrySet().stream()
                .map(e -> new PersonVelocity(e.getKey(), e.getValue(),
                        milestoneCount > 0 ? e.getValue() / milestoneCount : e.getValue(),
                        personByMilestone.getOrDefault(e.getKey(), Map.of())))
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

package com.example.timetracking.velocity.application.mapper;

import com.example.timetracking.milestone.domain.JiraTicket;
import com.example.timetracking.milestone.domain.TimeConstants;
import com.example.timetracking.milestone.domain.Worklog;
import com.example.timetracking.velocity.application.dto.EffortBucket;
import com.example.timetracking.velocity.application.dto.MilestoneVelocity;
import com.example.timetracking.velocity.application.dto.PersonMilestoneEffort;
import com.example.timetracking.velocity.application.dto.PersonVelocity;
import com.example.timetracking.velocity.application.dto.VelocityReport;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Turns a set of delivered {@link JiraTicket} milestone trees into the {@link VelocityReport}:
 * how long each milestone took from start to delivery, what it cost, and the same selection
 * broken down per person.
 *
 * <p>Each milestone is measured against its own delivery window (see {@link MilestoneDelivery}),
 * never against the calendar, which is what makes a milestone of project X comparable with one of
 * project Y regardless of when either ran or which type they are.
 *
 * <p>Tickets reachable from more than one selected milestone are counted once, for the first
 * milestone that reaches them.
 */
@Component
public class VelocityAggregator {

    private static final String UNKNOWN_AUTHOR = "Unknown";

    /** Width of each effort-over-time window, in days since the milestone start. */
    private static final int BUCKET_DAYS = 10;

    public VelocityReport aggregate(List<JiraTicket> milestoneTrees) {
        List<MilestoneVelocity> perMilestone = new ArrayList<>();
        Map<String, PersonAccumulator> people = new LinkedHashMap<>();
        Set<String> seenTickets = new HashSet<>();
        long totalSeconds = 0;

        for (JiraTicket tree : milestoneTrees) {
            List<Worklog> worklogs = new ArrayList<>();
            collect(tree, seenTickets, worklogs);

            Map<String, Long> secondsByPerson = new LinkedHashMap<>();
            long milestoneSeconds = 0;
            for (Worklog worklog : worklogs) {
                String author = worklog.author() != null ? worklog.author() : UNKNOWN_AUTHOR;
                long seconds = worklog.timeSpentSeconds();

                milestoneSeconds += seconds;
                secondsByPerson.merge(author, seconds, Long::sum);
                people.computeIfAbsent(author, PersonAccumulator::new).add(tree.key(), seconds);
            }

            LocalDate start = MilestoneDelivery.startDate(tree.metadata(), earliest(worklogs));
            LocalDate delivery = MilestoneDelivery.deliveryDate(tree.metadata(), latest(worklogs));
            LocalDate planned = MilestoneDelivery.parseDate(tree.metadata().baselineDeliveryDate());

            totalSeconds += milestoneSeconds;
            perMilestone.add(new MilestoneVelocity(
                    tree.key(),
                    tree.summary(),
                    projectKeyOf(tree),
                    tree.type(),
                    start,
                    delivery,
                    planned,
                    MilestoneDelivery.durationDays(start, delivery),
                    milestoneSeconds,
                    estimateSeconds(tree),
                    effortBuckets(worklogs, start),
                    sortedByValueDesc(secondsByPerson)));
        }

        return new VelocityReport(perMilestone, totalSeconds, persons(people));
    }

    /** Walks the tree collecting the worklogs of every ticket not already counted. */
    private void collect(JiraTicket ticket, Set<String> seenTickets, List<Worklog> into) {
        if (seenTickets.add(ticket.key())) {
            into.addAll(ticket.worklogs());
        }
        ticket.children().forEach(child -> collect(child, seenTickets, into));
    }

    /**
     * Planned effort for the milestone's whole tree, in seconds. Prefers the up-front man-day
     * estimate ({@code effortEstimateManDays}, rolled up); falls back to Jira's original time
     * estimate when the custom field was never filled. {@code 0} when nothing was estimated.
     */
    private long estimateSeconds(JiraTicket tree) {
        long fromManDays = Math.round(totalEffortManDays(tree) * TimeConstants.SECONDS_PER_MAN_DAY);
        return fromManDays > 0 ? fromManDays : tree.totalOriginalEstimateSeconds();
    }

    /** Own up-front man-day estimate plus that of every descendant. */
    private double totalEffortManDays(JiraTicket ticket) {
        return ticket.effortEstimateManDays()
                + ticket.children().stream().mapToDouble(this::totalEffortManDays).sum();
    }

    /**
     * Buckets the milestone's worklogs into fixed {@value #BUCKET_DAYS}-day windows measured from
     * its start, so the shape of effort over time can be charted. Dense and gap-filled: every
     * window from the first to the last with effort is present (zero-effort windows included).
     * Empty when the start date is unknown or no work was logged. Work logged before the start
     * date is attributed to the first window rather than dropped.
     */
    private List<EffortBucket> effortBuckets(List<Worklog> worklogs, LocalDate start) {
        if (start == null) {
            return List.of();
        }
        Map<Integer, Long> byBucket = new HashMap<>();
        int maxBucket = -1;
        for (Worklog worklog : worklogs) {
            LocalDate date = MilestoneDelivery.parseDate(worklog.startedDate());
            if (date == null) {
                continue;
            }
            int dayOffset = (int) (date.toEpochDay() - start.toEpochDay());
            int bucket = Math.max(0, dayOffset) / BUCKET_DAYS;
            byBucket.merge(bucket, worklog.timeSpentSeconds(), Long::sum);
            maxBucket = Math.max(maxBucket, bucket);
        }
        if (maxBucket < 0) {
            return List.of();
        }
        List<EffortBucket> buckets = new ArrayList<>(maxBucket + 1);
        for (int i = 0; i <= maxBucket; i++) {
            buckets.add(new EffortBucket(i * BUCKET_DAYS, byBucket.getOrDefault(i, 0L)));
        }
        return buckets;
    }

    /** Project of the milestone: its metadata when loaded, else the prefix of its key. */
    private String projectKeyOf(JiraTicket milestone) {
        String fromMetadata = milestone.metadata().projectKey();
        if (fromMetadata != null && !fromMetadata.isBlank()) {
            return fromMetadata;
        }
        int dash = milestone.key().indexOf('-');
        return dash > 0 ? milestone.key().substring(0, dash) : milestone.key();
    }

    private LocalDate earliest(List<Worklog> worklogs) {
        return worklogDates(worklogs).min(Comparator.naturalOrder()).orElse(null);
    }

    private LocalDate latest(List<Worklog> worklogs) {
        return worklogDates(worklogs).max(Comparator.naturalOrder()).orElse(null);
    }

    private Stream<LocalDate> worklogDates(List<Worklog> worklogs) {
        return worklogs.stream()
                .map(worklog -> MilestoneDelivery.parseDate(worklog.startedDate()))
                .filter(Objects::nonNull);
    }

    private Map<String, Long> sortedByValueDesc(Map<String, Long> values) {
        Map<String, Long> sorted = new LinkedHashMap<>();
        values.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(entry -> sorted.put(entry.getKey(), entry.getValue()));
        return sorted;
    }

    private List<PersonVelocity> persons(Map<String, PersonAccumulator> people) {
        return people.values().stream()
                .map(PersonAccumulator::toPersonVelocity)
                .sorted(Comparator.comparingLong(PersonVelocity::totalSeconds).reversed())
                .toList();
    }

    /** Mutable per-person tally: effort per milestone, in first-seen order. */
    private static final class PersonAccumulator {

        private final String name;
        private final Map<String, Long> secondsByMilestone = new LinkedHashMap<>();
        private long totalSeconds;

        private PersonAccumulator(String name) {
            this.name = name;
        }

        private void add(String milestoneKey, long seconds) {
            totalSeconds += seconds;
            secondsByMilestone.merge(milestoneKey, seconds, Long::sum);
        }

        private PersonVelocity toPersonVelocity() {
            List<PersonMilestoneEffort> efforts = secondsByMilestone.entrySet().stream()
                    .map(entry -> new PersonMilestoneEffort(entry.getKey(), entry.getValue()))
                    .toList();
            return new PersonVelocity(name, totalSeconds, efforts);
        }
    }
}

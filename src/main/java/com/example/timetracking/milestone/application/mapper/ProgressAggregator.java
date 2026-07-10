package com.example.timetracking.milestone.application.mapper;

import com.example.timetracking.milestone.application.dto.Contribution;
import com.example.timetracking.milestone.application.dto.MilestoneSummary;
import com.example.timetracking.milestone.application.dto.Progress;
import com.example.timetracking.milestone.application.dto.ProgressWorklog;
import com.example.timetracking.milestone.domain.JiraMetadata;
import com.example.timetracking.milestone.domain.JiraTicket;
import com.example.timetracking.milestone.domain.TimeConstants;
import com.example.timetracking.milestone.domain.Worklog;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Flattens a {@link JiraTicket} subtree into the presentation-oriented {@link Progress} model (totals plus per-contributor worklogs).
 */
@Component
public class ProgressAggregator {

    public Progress toProgress(JiraTicket ticket) {
        Map<String, List<ProgressWorklog>> byUser = new LinkedHashMap<>();
        collect(ticket, byUser);

        long estimate = budget(ticket.effortEstimateManDays(), ticket.totalOriginalEstimateSeconds());
        List<Progress> children = ticket.children().stream()
                .map(this::toProgress)
                .toList();
        return new Progress(
                ticket.key(),
                ticket.summary(),
                estimate,
                ticket.totalTimeSpentSeconds(),
                contributions(byUser),
                children);
    }

    /**
     * Builds the milestone header (identity, description, full metadata, estimate) without traversing worklogs — used for the fast initial
     * view.
     */
    public MilestoneSummary toSummary(JiraTicket ticket) {
        long estimate = budget(ticket.effortEstimateManDays(), ticket.totalOriginalEstimateSeconds());
        JiraMetadata metadata = ticket.metadata();
        return new MilestoneSummary(
                ticket.key(),
                ticket.summary(),
                ticket.description(),
                ticket.type(),
                metadata.status(),
                metadata.priority(),
                metadata.resolution(),
                metadata.assignee(),
                metadata.reporter(),
                metadata.creator(),
                metadata.created(),
                metadata.updated(),
                metadata.dueDate(),
                metadata.resolutionDate(),
                metadata.startDate(),
                metadata.baselineDeliveryDate(),
                metadata.effectiveDeliveryDate(),
                metadata.labels(),
                metadata.projectKey(),
                metadata.projectName(),
                metadata.components(),
                metadata.fixVersions(),
                estimate);
    }

    private void collect(JiraTicket ticket, Map<String, List<ProgressWorklog>> byUser) {
        for (Worklog worklog : ticket.worklogs()) {
            String author = worklog.author() != null ? worklog.author() : "Unknown";
            byUser.computeIfAbsent(author, k -> new ArrayList<>())
                    .add(new ProgressWorklog(date(worklog.startedDate()),
                            worklog.timeSpentSeconds(), worklog.comment(), ticket.key(), ticket.summary(), ticket.type()));
        }
        ticket.children().forEach(child -> collect(child, byUser));
    }

    private List<Contribution> contributions(Map<String, List<ProgressWorklog>> byUser) {
        return byUser.entrySet().stream()
                .map(e -> new Contribution(e.getKey(),
                        e.getValue().stream().mapToLong(ProgressWorklog::seconds).sum(),
                        e.getValue()))
                .sorted(Comparator.comparingLong(Contribution::totalSeconds).reversed())
                .toList();
    }

    private long budget(double effortManDays, long fallbackSeconds) {
        return effortManDays > 0 ? Math.round(effortManDays * TimeConstants.SECONDS_PER_MAN_DAY) : Math.max(0, fallbackSeconds);
    }

    private String date(String isoDateTime) {
        return isoDateTime != null && isoDateTime.length() >= 10 ? isoDateTime.substring(0, 10) : "—";
    }
}

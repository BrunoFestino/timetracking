package com.example.timetracking.milestone.application.mapper;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.example.timetracking.milestone.application.usecase.LoadMilestoneDetailsUseCase;
import com.example.timetracking.milestone.domain.JiraMetadata;
import com.example.timetracking.milestone.domain.JiraProject;
import com.example.timetracking.milestone.domain.JiraTicket;
import com.example.timetracking.milestone.domain.Worklog;
import com.example.timetracking.shared.jira.dto.JiraIssueDto;
import com.example.timetracking.shared.jira.dto.JiraProjectDto;

/**
 * Turns raw Jira DTOs into the immutable {@link JiraTicket} tree.
 *
 * <p>Children are supplied by the caller because a {@code JiraTicket} is immutable
 * and the tree is assembled bottom-up in {@link LoadMilestoneDetailsUseCase}.
 */
@Component
public class JiraToDomainMapper {

    private static final Logger log = LoggerFactory.getLogger(JiraToDomainMapper.class);

    public JiraTicket toTicket(JiraIssueDto dto, List<JiraTicket> children) {
        JiraIssueDto.Fields fields = dto.fields();
        if (fields == null) {
            return new JiraTicket(dto.key(), null, null, null, JiraMetadata.EMPTY, 0, 0, 0, List.of(), children);
        }
        JiraIssueDto.TimeTracking time = fields.timetracking();
        return new JiraTicket(
                dto.key(),
                fields.summary(),
                fields.description(),
                fields.issuetype() != null ? fields.issuetype().name() : null,
                toMetadata(fields),
                parseEffort(fields.effortEstimateManDays(), dto.key()),
                time != null ? orZero(time.originalEstimateSeconds()) : 0,
                time != null ? orZero(time.timeSpentSeconds()) : 0,
                toWorklogs(fields.worklog()),
                children);
    }

    private JiraMetadata toMetadata(JiraIssueDto.Fields fields) {
        return new JiraMetadata(
                displayName(fields.assignee()),
                displayName(fields.reporter()),
                displayName(fields.creator()),
                fields.status() != null ? fields.status().name() : null,
                fields.priority() != null ? fields.priority().name() : null,
                fields.resolution() != null ? fields.resolution().name() : null,
                fields.created(),
                fields.updated(),
                fields.duedate(),
                fields.resolutiondate(),
                fields.startDate(),
                fields.baselineDeliveryDate(),
                fields.effectiveDeliveryDate(),
                fields.labels(),
                fields.project() != null ? fields.project().key() : null,
                fields.project() != null ? fields.project().name() : null,
                names(fields.components()),
                names(fields.fixVersions()));
    }

    private String displayName(JiraIssueDto.User user) {
        return user != null ? user.displayName() : null;
    }

    private List<String> names(List<JiraIssueDto.NamedValue> values) {
        return values == null ? List.of() : values.stream().map(JiraIssueDto.NamedValue::name).toList();
    }

    private List<Worklog> toWorklogs(JiraIssueDto.WorklogPage page) {
        if (page == null || page.worklogs() == null) {
            return List.of();
        }
        return page.worklogs().stream().map(this::toWorklog).toList();
    }

    private Worklog toWorklog(JiraIssueDto.Worklog dto) {
        String author = dto.author() != null ? dto.author().displayName() : null;
        return new Worklog(author, dto.timeSpentSeconds(), dto.started(), dto.comment());
    }

    private double parseEffort(String raw, String key) {
        if (raw == null || raw.isBlank()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            log.warn("Cannot parse effort estimate '{}' for item '{}'", raw, key);
            return 0.0;
        }
    }

    private long orZero(Integer value) {
        return value != null ? value : 0L;
    }

    public JiraProject toProject(JiraProjectDto projectDto) {
        return new JiraProject(projectDto.key(), projectDto.name());
    }
}

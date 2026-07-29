package com.example.timetracking.velocity.application.usecase;

import com.example.timetracking.milestone.application.mapper.JiraToDomainMapper;
import com.example.timetracking.milestone.domain.JiraTicket;
import com.example.timetracking.shared.jira.JiraApiClient;
import com.example.timetracking.shared.jira.dto.JiraIssueDto;
import com.example.timetracking.shared.jira.dto.JiraSearchResponseDto;
import com.example.timetracking.velocity.application.mapper.MilestoneDelivery;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Lists the milestones of a project that have already been delivered — the only ones velocity
 * measures, since a milestone still in flight has no time-to-finish yet.
 *
 * <p>Deliberately a velocity-owned loader rather than the milestone feature's
 * {@code LoadMilestonesUseCase}: that one fetches summaries only, and deciding "delivered"
 * needs the status and delivery-date fields. Milestone type is not filtered — a milestone of
 * any type counts, which is what makes cross-project selections possible.
 */
@Service
public class LoadDeliveredMilestonesUseCase {

    private final JiraApiClient jiraApiClient;
    private final JiraToDomainMapper mapper;

    public LoadDeliveredMilestonesUseCase(JiraApiClient jiraApiClient, JiraToDomainMapper mapper) {
        this.jiraApiClient = jiraApiClient;
        this.mapper = mapper;
    }

    /** Delivered milestones of the given project, in Jira's order (newest key first). */
    public List<JiraTicket> loadDeliveredMilestones(String projectKey) {
        return issuesOf(jiraApiClient.searchMilestonesWithDeliveryByProject(projectKey)).stream()
                .map(dto -> mapper.toTicket(dto, List.of()))
                .filter(milestone -> MilestoneDelivery.isDelivered(milestone.metadata()))
                .toList();
    }

    private List<JiraIssueDto> issuesOf(JiraSearchResponseDto response) {
        if (response == null || response.issues() == null) {
            return List.of();
        }
        return response.issues();
    }
}

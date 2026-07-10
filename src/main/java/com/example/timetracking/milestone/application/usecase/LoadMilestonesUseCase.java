package com.example.timetracking.milestone.application.usecase;

import com.example.timetracking.milestone.application.mapper.JiraToDomainMapper;
import com.example.timetracking.milestone.domain.JiraTicket;
import com.example.timetracking.shared.jira.JiraApiClient;
import com.example.timetracking.shared.jira.dto.JiraIssueDto;
import com.example.timetracking.shared.jira.dto.JiraSearchResponseDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class LoadMilestonesUseCase {
    private final JiraApiClient jiraApiClient;
    private final JiraToDomainMapper mapper;

    public LoadMilestonesUseCase(JiraApiClient jiraApiClient, JiraToDomainMapper mapper) {
        this.jiraApiClient = jiraApiClient;
        this.mapper = mapper;
    }

    public List<JiraTicket> loadMilestones(String projectKey) {
        List<JiraTicket> milestones = new ArrayList<>();
        for (JiraIssueDto milestoneDto : milestonesOf(jiraApiClient.searchMilestonesByProject(projectKey))) {
            milestones.add(mapper.toTicket(milestoneDto, List.of()));
        }

        return milestones;
    }

    private List<JiraIssueDto> milestonesOf(JiraSearchResponseDto response) {
        if (response == null || response.issues() == null) {
            return List.of();
        }
        return response.issues();
    }
}
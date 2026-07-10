package com.example.timetracking.milestone.application.usecase;

import com.example.timetracking.milestone.application.mapper.JiraToDomainMapper;
import com.example.timetracking.milestone.domain.JiraTicket;
import com.example.timetracking.shared.jira.JiraApiClient;
import com.example.timetracking.shared.jira.dto.JiraIssueDto;
import com.example.timetracking.shared.jira.dto.JiraSearchResponseDto;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.example.timetracking.shared.config.CacheConfig.MILESTONE_CACHE;
import static com.example.timetracking.shared.config.CacheConfig.MILESTONE_TREE_CACHE;

@Service
public class LoadMilestoneDetailsUseCase {

    private static final String NO_EPIC_KEY = "(no epic)";
    private static final String NO_EPIC_SUMMARY = "Issues not linked to an Epic";

    private final JiraApiClient jiraApiClient;
    private final JiraToDomainMapper mapper;

    public LoadMilestoneDetailsUseCase(JiraApiClient jiraApiClient, JiraToDomainMapper mapper) {
        this.jiraApiClient = jiraApiClient;
        this.mapper = mapper;
    }

    @Cacheable(cacheNames = MILESTONE_CACHE, key = "#milestoneKey")
    public JiraTicket loadMilestoneOnly(String milestoneKey) {
        JiraIssueDto milestoneDto = jiraApiClient.getIssue(milestoneKey);
        return milestoneDto == null ? null : mapper.toTicket(milestoneDto, List.of());
    }

    @Cacheable(cacheNames = MILESTONE_TREE_CACHE, key = "#milestoneKey")
    public JiraTicket loadByKey(String milestoneKey) {
        JiraIssueDto milestoneDto = jiraApiClient.getIssue(milestoneKey);
        if (milestoneDto == null) {
            return null;
        }
        String projectKey = extractProjectKey(milestoneKey);

        List<JiraIssueDto> epicDtos = issuesOf(jiraApiClient.searchEpicsByMilestone(projectKey, milestoneKey));

        List<JiraTicket> epics = buildEpics(epicDtos, milestoneKey, projectKey);
        return mapper.toTicket(milestoneDto, epics);
    }

    private List<JiraTicket> buildEpics(List<JiraIssueDto> epicDtos, String milestoneKey, String projectKey) {
        Set<String> assignedIssueKeys = new HashSet<>();

        List<JiraTicket> epics = new ArrayList<>();
        for (JiraIssueDto epicDto : epicDtos) {
            List<JiraTicket> issues = new ArrayList<>();
            for (JiraIssueDto issueDto : issuesOf(jiraApiClient.searchIssuesByEpic(projectKey, epicDto.key()))) {
                assignedIssueKeys.add(issueDto.key());
                issues.add(buildIssue(issueDto, projectKey));
            }
            epics.add(mapper.toTicket(epicDto, issues));
        }

        List<JiraTicket> orphanIssues = orphanIssues(milestoneKey, projectKey, assignedIssueKeys);
        if (!orphanIssues.isEmpty()) {
            epics.add(orphanEpic(orphanIssues));
        }
        return epics;
    }

    private List<JiraTicket> orphanIssues(String milestoneKey, String projectKey, Set<String> assignedIssueKeys) {
        return issuesOf(jiraApiClient.searchIssuesByMilestone(projectKey, milestoneKey)).stream()
                .filter(issueDto -> !assignedIssueKeys.contains(issueDto.key()))
                .map(issueDto -> buildIssue(issueDto, projectKey))
                .toList();
    }

    private JiraTicket buildIssue(JiraIssueDto issueDto, String projectKey) {
        List<JiraTicket> subtasks = issuesOf(jiraApiClient.searchSubtasksByIssue(projectKey, issueDto.key()))
                .stream()
                .map(dto -> mapper.toTicket(dto, List.of()))
                .toList();
        return mapper.toTicket(issueDto, subtasks);
    }

    private JiraTicket orphanEpic(List<JiraTicket> issues) {
        return new JiraTicket(NO_EPIC_KEY, NO_EPIC_SUMMARY, null, "Epic", null, 0, 0, 0, List.of(), issues);
    }

    private List<JiraIssueDto> issuesOf(JiraSearchResponseDto response) {
        if (response == null || response.issues() == null) {
            return List.of();
        }
        return response.issues();
    }

    /**
     * Extract the project key from an issue key (e.g. {@code TTAR-9625} → {@code TTAR}).
     */
    private String extractProjectKey(String issueKey) {
        int dashIndex = issueKey.indexOf('-');
        return dashIndex > 0 ? issueKey.substring(0, dashIndex) : issueKey;
    }
}

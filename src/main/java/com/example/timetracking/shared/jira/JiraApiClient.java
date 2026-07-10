package com.example.timetracking.shared.jira;

import com.example.timetracking.shared.jira.dto.JiraIssueDto;
import com.example.timetracking.shared.jira.dto.JiraProjectDto;
import com.example.timetracking.shared.jira.dto.JiraSearchResponseDto;

import java.util.List;

public interface JiraApiClient {

    List<JiraProjectDto> getProjects();

    JiraSearchResponseDto searchMilestonesByProject(String projectKey);

    JiraSearchResponseDto searchEpicsByMilestone(String porjectKey, String milestoneKey);

    JiraSearchResponseDto searchIssuesByEpic(String projectKey, String epicKey);

    JiraSearchResponseDto searchIssuesByMilestone(String projectKey, String milestoneKey);

    JiraSearchResponseDto searchSubtasksByIssue(String projectKey, String parentKey);

    JiraIssueDto getIssue(String issueKey);

}

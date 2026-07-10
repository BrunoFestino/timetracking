package com.example.timetracking.shared.jira;

import com.example.timetracking.shared.jira.dto.JiraIssueDto;
import com.example.timetracking.shared.jira.dto.JiraProjectDto;
import com.example.timetracking.shared.jira.dto.JiraSearchResponseDto;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class JiraRestClient implements JiraApiClient {

    private static final String WORKLOG_FIELDS =
            "summary,description,issuetype,labels,timetracking,worklog,subtasks,parent,issuelinks,customfield_14230";
    private static final String PROJECT_CLAUSE = "project = ";

    private final RestClient restClient;

    public JiraRestClient(RestClient jiraHttpClient) {
        this.restClient = jiraHttpClient;
    }

    @Override
    public List<JiraProjectDto> getProjects() {
        return performProjectSearch();
    }

    @Override
    public JiraSearchResponseDto searchMilestonesByProject(String projectKey) {
        String jql = PROJECT_CLAUSE + projectKey
                + " AND issuetype = Milestone"
                + " ORDER BY key DESC";
        return performSearch(jql, "summary");
    }

    @Override
    public JiraSearchResponseDto searchEpicsByMilestone(String projectKey, String milestoneKey) {
        String jql = PROJECT_CLAUSE + projectKey
                + " AND issuetype = Epic AND 'Parent Milestone' = " + milestoneKey
                + " ORDER BY key ASC";
        return performSearch(jql, "summary,description,labels,timetracking,worklog,customfield_14230");
    }

    @Override
    public JiraSearchResponseDto searchIssuesByEpic(String projectKey, String epicKey) {
        String jql = PROJECT_CLAUSE + projectKey
                + " AND 'Epic Link' = " + epicKey
                + " AND issuetype not in (Epic, Sub-task)"
                + " ORDER BY key ASC";
        return performSearch(jql, WORKLOG_FIELDS);
    }

    @Override
    public JiraSearchResponseDto searchIssuesByMilestone(String projectKey, String milestoneKey) {
        String jql = PROJECT_CLAUSE + projectKey
                + " AND 'Parent Milestone' = " + milestoneKey
                + " AND issuetype not in (Epic, Sub-task)"
                + " ORDER BY key ASC";
        return performSearch(jql, WORKLOG_FIELDS);
    }

    @Override
    public JiraSearchResponseDto searchSubtasksByIssue(String projectKey, String parentKey) {
        String jql = PROJECT_CLAUSE + projectKey
                + " AND parent = " + parentKey
                + " ORDER BY key ASC";
        return performSearch(jql, WORKLOG_FIELDS);
    }

    @Override
    public JiraIssueDto getIssue(String issueKey) {
        return restClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/rest/api/2/issue/{key}")
                        .queryParam("fields", "summary,description,issuetype,status,priority,resolution,"
                                + "assignee,reporter,creator,project,created,updated,duedate,resolutiondate,"
                                + "labels,components,fixVersions,timetracking,worklog,"
                                + "customfield_14230,customfield_15030,customfield_13434,customfield_13445")
                        .queryParam("expand", "worklog")
                        .build(issueKey))
                .retrieve()
                .body(JiraIssueDto.class);
    }

    private JiraSearchResponseDto performSearch(String jql, String fields) {
        return restClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/rest/api/2/search")
                        .queryParam("jql", jql)
                        .queryParam("fields", fields)
                        .build())
                .retrieve()
                .body(JiraSearchResponseDto.class);
    }

    private List<JiraProjectDto> performProjectSearch() {
        return restClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/rest/api/2/project")
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }
}

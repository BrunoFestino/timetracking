package com.example.timetracking.milestone.application.usecase;

import com.example.timetracking.milestone.application.mapper.JiraToDomainMapper;
import com.example.timetracking.milestone.domain.JiraProject;
import com.example.timetracking.shared.jira.JiraApiClient;
import com.example.timetracking.shared.jira.dto.JiraProjectDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class LoadProjectsUseCase {
    private final JiraApiClient jiraApiClient;
    private final JiraToDomainMapper mapper;

    public LoadProjectsUseCase(JiraApiClient jiraApiClient, JiraToDomainMapper mapper) {
        this.jiraApiClient = jiraApiClient;
        this.mapper = mapper;
    }

    public List<JiraProject> loadProjects() {
        List<JiraProject> projects = new ArrayList<>();
        for (JiraProjectDto projectDto : jiraApiClient.getProjects()) {
            projects.add(mapper.toProject(projectDto));
        }
        return projects;
    }
}

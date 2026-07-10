package com.example.timetracking.shared.jira.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
@JsonIgnoreProperties(ignoreUnknown = true)
public record JiraSearchResponseDto(List<JiraIssueDto> issues) {
}


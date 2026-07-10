package com.example.timetracking.shared.jira.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JiraIssueDto(
        String key,
        Fields fields
){

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Fields(
            String summary,
            String description,
            IssueType issuetype,
            TimeTracking timetracking,
            WorklogPage worklog,
            User assignee,
            User reporter,
            User creator,
            Status status,
            Priority priority,
            Resolution resolution,
            Project project,
            String created,
            String updated,
            String duedate,
            String resolutiondate,
            List<String> labels,
            List<NamedValue> components,
            List<NamedValue> fixVersions,
            @JsonProperty("customfield_14230") String effortEstimateManDays,
            @JsonProperty("customfield_15030") String startDate,
            @JsonProperty("customfield_13434") String baselineDeliveryDate,
            @JsonProperty("customfield_13445") String effectiveDeliveryDate) {
        
    }


   @JsonIgnoreProperties(ignoreUnknown = true)
    public record User(String displayName, String name, String emailAddress){

   }

   @JsonIgnoreProperties(ignoreUnknown = true)
    public record Status(String name) {

   }

   @JsonIgnoreProperties(ignoreUnknown = true)
    public record Priority(String name) {

   }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Resolution(String name) {

    }


   @JsonIgnoreProperties(ignoreUnknown = true)
    public record Project(String key, String name) {

   }

   @JsonIgnoreProperties(ignoreUnknown = true)
    public record NamedValue(String name) {

   }

   @JsonIgnoreProperties(ignoreUnknown = true)
    public record IssueType(String key, String name) {

   }

   @JsonIgnoreProperties(ignoreUnknown = true)
    public record TimeTracking(Integer originalEstimateSeconds, Integer timeSpentSeconds) {

   }

   @JsonIgnoreProperties(ignoreUnknown = true)
    public record WorklogPage(List<Worklog> worklogs) {

   }

   @JsonIgnoreProperties(ignoreUnknown = true)
    public record Worklog(String started, String comment, int timeSpentSeconds, Author author) {

   }

   @JsonIgnoreProperties(ignoreUnknown = true)
    public record Author(String displayName) {

   }
}




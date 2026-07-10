package com.example.timetracking.milestone.domain;

public record Worklog(
        String author,
        long timeSpentSeconds,
        String startedDate,
        String comment
) {
}

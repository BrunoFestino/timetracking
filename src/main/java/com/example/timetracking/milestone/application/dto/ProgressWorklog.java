package com.example.timetracking.milestone.application.dto;

public record ProgressWorklog(String date, long seconds, String comment, String sourceKey, String sourceSummary, String sourceType) {
    public boolean hasComment() {
        return comment != null && !comment.isBlank();
    }

    public boolean isSubTask() {
        return "Sub-task".equalsIgnoreCase(sourceType);
    }
}

package com.example.timetracking.milestone.application.dto;

import java.util.List;

public record MilestoneSummary(
        String key,
        String name,
        String description,
        String type,
        String status,
        String priority,
        String resolution,
        String assignee,
        String reporter,
        String creator,
        String created,
        String updated,
        String dueDate,
        String resolutionDate,
        String startDate,
        String baselineDeliveryDate,
        String effectiveDeliveryDate,
        List<String> labels,
String projectKey,
String projectName,
List<String> components,
List<String> fixVersions,
long estimatedSeconds) {

public MilestoneSummary {
    labels = labels == null ? List.of() : List.copyOf(labels);
    components = components == null ? List.of() : List.copyOf(components);
    fixVersions = fixVersions == null ? List.of() : List.copyOf(fixVersions);
}
}
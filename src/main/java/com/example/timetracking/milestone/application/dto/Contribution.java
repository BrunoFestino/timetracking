package com.example.timetracking.milestone.application.dto;

import java.util.List;

public record Contribution(String name, long totalSeconds, List<ProgressWorklog> worklogs) {
}

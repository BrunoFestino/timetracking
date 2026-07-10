package com.example.timetracking.milestone.application.usecase;

import com.example.timetracking.milestone.application.mapper.ProgressAggregator;
import com.example.timetracking.milestone.domain.JiraTicket;

import java.util.regex.Pattern;

abstract class MilestoneProgressBase {

    private static final Pattern JIRA_KEY = Pattern.compile("^[A-Z][A-Z0-9_]+-\\d+$");

    protected final LoadMilestoneDetailsUseCase loadMilestoneDetailsUseCase;
    protected final ProgressAggregator aggregator;

    protected MilestoneProgressBase(LoadMilestoneDetailsUseCase loadMilestoneDetailsUseCase, ProgressAggregator aggregator) {
        this.loadMilestoneDetailsUseCase = loadMilestoneDetailsUseCase;
        this.aggregator = aggregator;
    }

    protected JiraTicket validateAndLoad(String key) {
        validateKey(key);
        return loadMilestoneDetailsUseCase.loadByKey(key);
    }

    protected JiraTicket validateAndLoadMilestoneOnly(String key) {
        validateKey(key);
        return loadMilestoneDetailsUseCase.loadMilestoneOnly(key);
    }

    private void validateKey(String key) {
        if (key == null || !JIRA_KEY.matcher(key).matches()) {
            throw new IllegalArgumentException("Invalid milestone key: " + key);
        }
    }
}
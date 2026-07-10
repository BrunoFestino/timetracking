package com.example.timetracking.velocity.application.usecase;

import com.example.timetracking.milestone.application.usecase.LoadMilestoneDetailsUseCase;
import com.example.timetracking.milestone.domain.JiraTicket;
import com.example.timetracking.velocity.application.dto.VelocityReport;
import com.example.timetracking.velocity.application.mapper.VelocityAggregator;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Computes team velocity across one or more milestones. Reuses the cached
 * milestone-tree loading from the milestone feature, so repeated computations
 * do not hit Jira again.
 */
@Service
public class ComputeVelocityUseCase {

    private static final Pattern JIRA_KEY = Pattern.compile("^[A-Z][A-Z0-9_]+-\\d+$");

    private final LoadMilestoneDetailsUseCase loadMilestoneDetailsUseCase;
    private final VelocityAggregator aggregator;

    public ComputeVelocityUseCase(LoadMilestoneDetailsUseCase loadMilestoneDetailsUseCase,
                                  VelocityAggregator aggregator) {
        this.loadMilestoneDetailsUseCase = loadMilestoneDetailsUseCase;
        this.aggregator = aggregator;
    }

    public VelocityReport execute(List<String> milestoneKeys) {
        List<JiraTicket> milestones = new ArrayList<>();
        for (String key : milestoneKeys) {
            validateKey(key);
            JiraTicket milestone = loadMilestoneDetailsUseCase.loadByKey(key);
            if (milestone != null) {
                milestones.add(milestone);
            }
        }
        return aggregator.aggregate(milestones);
    }

    private void validateKey(String key) {
        if (key == null || !JIRA_KEY.matcher(key).matches()) {
            throw new IllegalArgumentException("Invalid milestone key: " + key);
        }
    }
}

package com.example.timetracking.milestone.application.usecase;

import com.example.timetracking.milestone.application.dto.MilestoneBreakdown;
import com.example.timetracking.milestone.application.dto.Progress;
import com.example.timetracking.milestone.application.mapper.ProgressAggregator;
import com.example.timetracking.milestone.domain.JiraTicket;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LoadEpicsProgressUseCase extends MilestoneProgressBase {

    public LoadEpicsProgressUseCase(LoadMilestoneDetailsUseCase loadMilestoneDetailsUseCase,
                                    ProgressAggregator aggregator) {
        super(loadMilestoneDetailsUseCase, aggregator);
    }

    public MilestoneBreakdown execute(String milestoneKey) {
        JiraTicket milestone = validateAndLoad(milestoneKey);
        if (milestone == null) {
            return new MilestoneBreakdown(null, List.of());
        }
        Progress milestoneProgress = aggregator.toProgress(milestone);
        List<Progress> epics = milestone.children().stream()
                .map(aggregator::toProgress)
                .toList();
        return new MilestoneBreakdown(milestoneProgress, epics);
    }
}
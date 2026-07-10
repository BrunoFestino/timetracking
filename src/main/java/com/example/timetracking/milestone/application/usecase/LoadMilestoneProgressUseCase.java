package com.example.timetracking.milestone.application.usecase;

import com.example.timetracking.milestone.application.dto.MilestoneSummary;
import com.example.timetracking.milestone.application.mapper.ProgressAggregator;
import com.example.timetracking.milestone.domain.JiraTicket;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class LoadMilestoneProgressUseCase extends MilestoneProgressBase {

    public LoadMilestoneProgressUseCase(LoadMilestoneDetailsUseCase loadMilestoneDetailsUseCase, ProgressAggregator aggregator) {
        super(loadMilestoneDetailsUseCase, aggregator);
    }

    public Optional<MilestoneSummary> execute(String milestoneKey) {
        JiraTicket milestone = validateAndLoadMilestoneOnly(milestoneKey);
        return milestone == null ? Optional.empty() : Optional.of(aggregator.toSummary(milestone));
    }
}
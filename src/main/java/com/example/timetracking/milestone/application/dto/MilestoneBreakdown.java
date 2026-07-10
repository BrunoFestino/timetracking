package com.example.timetracking.milestone.application.dto;

import java.util.List;

public record MilestoneBreakdown(Progress milestone, List<Progress> epics) {

    public MilestoneBreakdown {
        epics = epics == null ? List.of() : List.copyOf(epics);
    }
}

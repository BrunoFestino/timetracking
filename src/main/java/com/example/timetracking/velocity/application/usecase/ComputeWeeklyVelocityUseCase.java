package com.example.timetracking.velocity.application.usecase;

import com.example.timetracking.milestone.application.usecase.LoadMilestoneDetailsUseCase;
import com.example.timetracking.milestone.application.usecase.LoadMilestonesUseCase;
import com.example.timetracking.milestone.domain.JiraTicket;
import com.example.timetracking.velocity.application.dto.WeeklyVelocityReport;
import com.example.timetracking.velocity.application.mapper.WeeklyVelocityAggregator;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Computes calendar-week velocity for a whole project over a date range: loads every
 * milestone tree of the project (cached by the milestone feature) and aggregates the
 * worklogs that fall within the range into Monday-to-Sunday weeks.
 *
 * <p>The range is snapped outward to whole weeks: {@code from} to its Monday and
 * {@code to} to its Sunday, so the average never divides by a partial week.
 */
@Service
public class ComputeWeeklyVelocityUseCase {

    private final LoadMilestonesUseCase loadMilestonesUseCase;
    private final LoadMilestoneDetailsUseCase loadMilestoneDetailsUseCase;
    private final WeeklyVelocityAggregator weeklyVelocityAggregator;

    public ComputeWeeklyVelocityUseCase(LoadMilestonesUseCase loadMilestonesUseCase,
                                        LoadMilestoneDetailsUseCase loadMilestoneDetailsUseCase,
                                        WeeklyVelocityAggregator weeklyVelocityAggregator) {
        this.loadMilestonesUseCase = loadMilestonesUseCase;
        this.loadMilestoneDetailsUseCase = loadMilestoneDetailsUseCase;
        this.weeklyVelocityAggregator = weeklyVelocityAggregator;
    }

    public WeeklyVelocityReport execute(String projectKey, LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Select both a start and an end date.");
        }
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("The end date must not be before the start date.");
        }
        LocalDate snappedFrom = from.with(DayOfWeek.MONDAY);
        LocalDate snappedTo = to.with(DayOfWeek.SUNDAY);

        List<JiraTicket> trees = new ArrayList<>();
        for (JiraTicket milestone : loadMilestonesUseCase.loadMilestones(projectKey)) {
            JiraTicket tree = loadMilestoneDetailsUseCase.loadByKey(milestone.key());
            if (tree != null) {
                trees.add(tree);
            }
        }
        return weeklyVelocityAggregator.aggregate(trees, snappedFrom, snappedTo);
    }
}

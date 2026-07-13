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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Computes calendar-week velocity for a whole project over a date range: loads every
 * milestone tree of the project (cached by the milestone feature) and aggregates the
 * worklogs that fall within the range into Monday-to-Sunday weeks.
 *
 * <p>Trees are loaded concurrently with a bounded pool: each tree is several chained
 * Jira calls, and the 5-minute cache eviction makes most computes a cold load, so the
 * wall time is roughly the slowest batch instead of the sum of every milestone.
 *
 * <p>The range is snapped outward to whole weeks: {@code from} to its Monday and
 * {@code to} to its Sunday, so the average never divides by a partial week.
 */
@Service
public class ComputeWeeklyVelocityUseCase {

    /** Concurrent tree loads; kept modest to stay friendly to Jira rate limits. */
    private static final int PARALLEL_LOADS = 6;

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

        List<JiraTicket> milestones = loadMilestonesUseCase.loadMilestones(projectKey);
        return weeklyVelocityAggregator.aggregate(loadTreesInParallel(milestones), snappedFrom, snappedTo);
    }

    /**
     * Loads each milestone's tree on a bounded pool, preserving submission order.
     * A milestone that fails to load fails the whole compute, matching the previous
     * sequential behaviour; the cacheable {@code loadByKey} proxy still applies since
     * the injected bean is called.
     */
    private List<JiraTicket> loadTreesInParallel(List<JiraTicket> milestones) {
        if (milestones.isEmpty()) {
            return List.of();
        }
        ExecutorService pool = Executors.newFixedThreadPool(Math.min(PARALLEL_LOADS, milestones.size()));
        try {
            List<Future<JiraTicket>> futures = milestones.stream()
                    .map(milestone -> pool.submit(() -> loadMilestoneDetailsUseCase.loadByKey(milestone.key())))
                    .toList();
            List<JiraTicket> trees = new ArrayList<>();
            for (Future<JiraTicket> future : futures) {
                JiraTicket tree = future.get();
                if (tree != null) {
                    trees.add(tree);
                }
            }
            return trees;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while loading milestone trees", e);
        } catch (ExecutionException e) {
            throw e.getCause() instanceof RuntimeException runtime ? runtime
                    : new IllegalStateException("Failed to load milestone tree", e.getCause());
        } finally {
            pool.shutdownNow();
        }
    }
}

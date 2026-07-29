package com.example.timetracking.velocity.application.usecase;

import com.example.timetracking.milestone.application.usecase.LoadMilestoneDetailsUseCase;
import com.example.timetracking.milestone.domain.JiraTicket;
import com.example.timetracking.velocity.application.dto.VelocityReport;
import com.example.timetracking.velocity.application.mapper.VelocityAggregator;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

/**
 * Computes delivery velocity across a set of delivered milestones, which may belong to
 * different projects. Reuses the cached milestone-tree loading from the milestone feature,
 * so repeated computations do not hit Jira again.
 */
@Service
public class ComputeVelocityUseCase {

    private static final Pattern JIRA_KEY = Pattern.compile("^[A-Z][A-Z0-9_]+-\\d+$");

    /** Concurrent tree loads; kept modest to stay friendly to Jira rate limits. */
    private static final int PARALLEL_LOADS = 6;

    private final LoadMilestoneDetailsUseCase loadMilestoneDetailsUseCase;
    private final VelocityAggregator aggregator;

    public ComputeVelocityUseCase(LoadMilestoneDetailsUseCase loadMilestoneDetailsUseCase,
                                  VelocityAggregator aggregator) {
        this.loadMilestoneDetailsUseCase = loadMilestoneDetailsUseCase;
        this.aggregator = aggregator;
    }

    public VelocityReport execute(List<String> milestoneKeys) {
        milestoneKeys.forEach(this::validateKey);
        return aggregator.aggregate(loadTreesInParallel(milestoneKeys));
    }

    /**
     * Loads each milestone's tree on a bounded pool, preserving submission order. Each tree is
     * several chained Jira calls and the 5-minute cache eviction makes most computes a cold
     * load, so the wall time is roughly the slowest batch instead of the sum of every milestone.
     * A milestone that fails to load fails the whole compute; the cacheable {@code loadByKey}
     * proxy still applies since the injected bean is called.
     */
    private List<JiraTicket> loadTreesInParallel(List<String> milestoneKeys) {
        if (milestoneKeys.isEmpty()) {
            return List.of();
        }
        ExecutorService pool = Executors.newFixedThreadPool(Math.min(PARALLEL_LOADS, milestoneKeys.size()));
        try {
            List<Future<JiraTicket>> futures = milestoneKeys.stream()
                    .map(key -> pool.submit(() -> loadMilestoneDetailsUseCase.loadByKey(key)))
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

    private void validateKey(String key) {
        if (key == null || !JIRA_KEY.matcher(key).matches()) {
            throw new IllegalArgumentException("Invalid milestone key: " + key);
        }
    }
}

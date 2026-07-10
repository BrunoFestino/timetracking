package com.example.timetracking.milestone.application.cache;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import static com.example.timetracking.shared.config.CacheConfig.MILESTONE_CACHE;
import static com.example.timetracking.shared.config.CacheConfig.MILESTONE_TREE_CACHE;

/**
 * Scheduled cache maintenance for the milestone feature.
 *
 * <p>Periodically evicts all cached milestones to ensure Jira changes are
 * eventually reflected in the UI without requiring a server restart. The 5-minute interval balances freshness against Jira API load.
 */
@Component
public class MilestonesCacheEviction {

    /**
     * Evict all cached milestones every 5 minutes.
     *
     * <p>Triggers at minute 0, 5, 10, 15, etc. of each hour (e.g., 10:00, 10:05, 10:10...).
     * The next cache query after eviction will refetch from Jira.
     */
    @CacheEvict(cacheNames = {MILESTONE_CACHE, MILESTONE_TREE_CACHE}, allEntries = true)
    @Scheduled(cron = "0 */5 * * * *")
    public void evictMilestonesCache() {
        // Intentionally empty: the @CacheEvict annotation handles cache invalidation via Spring AOP.
    }
}
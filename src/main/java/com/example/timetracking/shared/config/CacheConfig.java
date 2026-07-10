package com.example.timetracking.shared.config;


import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableCaching
@EnableScheduling
public class CacheConfig {

    public static final String MILESTONE_CACHE = "milestones";

    public static final String MILESTONE_TREE_CACHE = "milestonesTrees";

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(MILESTONE_CACHE, MILESTONE_TREE_CACHE);
    }

}

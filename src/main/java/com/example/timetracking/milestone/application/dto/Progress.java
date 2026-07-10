package com.example.timetracking.milestone.application.dto;
import java.util.List;

public record Progress(
        String key,
        String name,
        long estimatedSeconds,
        long spentSeconds,
        List<Contribution> contributions,
        List<Progress> children) {

    public Progress {
        contributions = contributions == null ? List.of() : List.copyOf(contributions);
        children = children == null ? List.of() : List.copyOf(children);
    }

    public double fractionSpent() {
        if (estimatedSeconds <= 0) {
            return spentSeconds > 0 ? 1.0 : 0.0;
        }
        return Math.min(1.0, (double) spentSeconds / estimatedSeconds);
    }

    public boolean isOverBudget() {
        return estimatedSeconds > 0 && spentSeconds > estimatedSeconds;
    }

    public long displayPercent() {
        if (estimatedSeconds > 0) {
            return Math.round(spentSeconds * 100.0 / estimatedSeconds);
        }
        return spentSeconds > 0 ? 100 : 0;
    }
}
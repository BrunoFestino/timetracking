package com.example.timetracking.velocity.application.dto;

import java.util.List;

/**
 * One team member's record across the selected delivered milestones: what they put into each
 * one and how long they stayed on it. This is the "compare by person" half of the view — the
 * per-milestone entries let two people be compared on the same milestone, and the aggregates
 * let a person be compared with themselves across milestones and projects.
 *
 * @param name       contributor display name
 * @param totalSeconds effort logged across every selected milestone
 * @param milestones per-milestone participation, in selection order
 */
public record PersonVelocity(
        String name,
        long totalSeconds,
        List<PersonMilestoneEffort> milestones) {

    public PersonVelocity {
        milestones = milestones == null ? List.of() : List.copyOf(milestones);
    }

    /** Delivered milestones this person logged work on. */
    public int milestonesParticipated() {
        return milestones.size();
    }

    /** Average effort this person put into a milestone of the selection. */
    public long avgSecondsPerMilestone() {
        return milestones.isEmpty() ? 0 : totalSeconds / milestones.size();
    }

    /**
     * Average length of this person's engagement, over the milestones where it is datable.
     * Read next to the milestone's own duration: a much shorter engagement means the person
     * joined for part of the run rather than carrying it end to end.
     */
    public int avgEngagedDays() {
        List<PersonMilestoneEffort> datable = milestones.stream()
                .filter(effort -> effort.engagedDays() > 0)
                .toList();
        if (datable.isEmpty()) {
            return 0;
        }
        return (int) Math.round(datable.stream().mapToInt(PersonMilestoneEffort::engagedDays).average().orElse(0));
    }

    /** This person's effort on the given milestone (0 when they did not work on it). */
    public long effortOn(String milestoneKey) {
        return milestones.stream()
                .filter(effort -> effort.milestoneKey().equals(milestoneKey))
                .mapToLong(PersonMilestoneEffort::seconds)
                .findFirst()
                .orElse(0L);
    }
}

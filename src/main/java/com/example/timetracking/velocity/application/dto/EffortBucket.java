package com.example.timetracking.velocity.application.dto;

/**
 * Effort logged in one fixed-width time window of a milestone's delivery, measured from its
 * start. A milestone's {@code effortOverTime} is a dense, gap-filled list of these (one per
 * window, zero-effort windows included) so the shape of effort over time — ramp-up, crunch
 * near delivery, idle stretches — can be charted directly.
 *
 * @param dayOffset the window's first day, in days since the milestone start (0, 10, 20, …)
 * @param seconds   effort logged within the window
 */
public record EffortBucket(int dayOffset, long seconds) {
}

/**
 * Feature: <b>delivery velocity</b> – how long delivered milestones take to finish,
 * compared across projects, for the team and per person.
 *
 * <p>A selection may mix milestones of different projects and different types: each one is
 * measured against its own start-to-delivery window, so the comparison never depends on the
 * calendar. Milestones still in flight are not offered — only a delivered one has a
 * time-to-finish to compare.
 *
 * <p>Follows the same layered convention as the
 * {@link com.example.timetracking.milestone milestone} feature.
 * Reuses {@link com.example.timetracking.milestone.domain milestone domain types}.
 */

package com.example.timetracking.velocity;

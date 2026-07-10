/**
 * Feature: <b>team velocity</b> – per-person throughput across multiple
 * milestones, useful for identifying bottlenecks and refining future
 * estimates.
 *
 * <p>Computes how many man-days each team member tends to deliver per
 * milestone (or per calendar week, configurable), and how that compares
 * to their own estimates over time.
 *
 * <p>Follows the same layered convention as the
 * {@link com.example.timetracking.milestone milestone} feature.
 * Reuses {@link com.example.timetracking.milestone.domain milestone domain types}.
 */

package com.example.timetracking.velocity;


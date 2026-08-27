package com.example.timetracking.gantt.application.usecase;

import com.example.timetracking.gantt.application.model.GanttTask;

import java.time.LocalDate;
import java.util.Collection;

/** Min/max planned-date helpers shared by the two build use cases. */
final class GanttBounds {

    private GanttBounds() {
    }

    static LocalDate min(Collection<GanttTask> tasks) {
        return tasks.stream().map(GanttTask::start).min(LocalDate::compareTo).orElse(null);
    }

    static LocalDate max(Collection<GanttTask> tasks) {
        return tasks.stream().map(GanttTask::end).max(LocalDate::compareTo).orElse(null);
    }
}

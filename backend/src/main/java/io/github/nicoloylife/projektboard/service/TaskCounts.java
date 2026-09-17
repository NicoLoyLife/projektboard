package io.github.nicoloylife.projektboard.service;

import io.github.nicoloylife.projektboard.domain.TaskStatus;
import io.github.nicoloylife.projektboard.repository.TaskStatusCount;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/** Anzahl der Aufgaben eines Projekts je Status und der daraus berechnete Fortschritt. */
public record TaskCounts(long total, long open, long inProgress, long done) {

    public static TaskCounts empty() {
        return new TaskCounts(0, 0, 0, 0);
    }

    public int progressPercent() {
        return ProgressCalculator.percent(done, total);
    }

    /** Fasst die gruppierten Zählzeilen der Datenbank je Projekt zusammen. */
    public static Map<Long, TaskCounts> byProject(Collection<TaskStatusCount> rows) {
        Map<Long, long[]> raw = new HashMap<>();
        for (TaskStatusCount row : rows) {
            long[] counts = raw.computeIfAbsent(row.getProjectId(), id -> new long[3]);
            counts[row.getStatus().ordinal()] += row.getCount();
        }
        Map<Long, TaskCounts> result = new HashMap<>();
        raw.forEach((projectId, counts) -> {
            long open = counts[TaskStatus.OPEN.ordinal()];
            long inProgress = counts[TaskStatus.IN_PROGRESS.ordinal()];
            long done = counts[TaskStatus.DONE.ordinal()];
            result.put(projectId, new TaskCounts(open + inProgress + done, open, inProgress, done));
        });
        return result;
    }
}

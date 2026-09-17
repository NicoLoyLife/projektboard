package io.github.nicoloylife.projektboard.repository;

import io.github.nicoloylife.projektboard.domain.TaskStatus;

/** Ergebniszeile der gruppierten Zählung von Aufgaben je Projekt und Status. */
public interface TaskStatusCount {

    Long getProjectId();

    TaskStatus getStatus();

    long getCount();
}

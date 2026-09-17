package io.github.nicoloylife.projektboard.repository;

import io.github.nicoloylife.projektboard.domain.Task;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByProjectIdOrderByCreatedAtAscIdAsc(Long projectId);

    /** Zählt Aufgaben je Projekt und Status in einer Abfrage, Grundlage für den Fortschritt. */
    @Query("""
            select t.project.id as projectId, t.status as status, count(t) as count
            from Task t
            where t.project.id in :projectIds
            group by t.project.id, t.status
            """)
    List<TaskStatusCount> countByProjectIds(@Param("projectIds") Collection<Long> projectIds);
}

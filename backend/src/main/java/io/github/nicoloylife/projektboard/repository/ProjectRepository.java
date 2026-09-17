package io.github.nicoloylife.projektboard.repository;

import io.github.nicoloylife.projektboard.domain.Project;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    /** Projekte, die der Benutzer leitet oder in denen er Mitglied ist, nur im eigenen Mandanten. */
    @Query("""
            select distinct p from Project p
            left join p.members m
            where p.tenant.id = :tenantId
              and (p.manager.id = :userId or m.id = :userId)
            order by p.name
            """)
    List<Project> findVisibleFor(@Param("tenantId") Long tenantId, @Param("userId") Long userId);

    /** Alle Projekte eines Mandanten, für Administratoren. */
    List<Project> findByTenantIdOrderByNameAsc(Long tenantId);
}

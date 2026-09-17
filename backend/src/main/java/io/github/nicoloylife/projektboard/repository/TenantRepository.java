package io.github.nicoloylife.projektboard.repository;

import io.github.nicoloylife.projektboard.domain.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository extends JpaRepository<Tenant, Long> {
}

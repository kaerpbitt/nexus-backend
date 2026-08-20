package com.financial.engine.repository;

import com.financial.engine.entity.SystemAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SystemAuditLogRepository extends JpaRepository<SystemAuditLog, Long> {

    List<SystemAuditLog> findByUserIdOrderByCreatedAtDesc(String userId);
}

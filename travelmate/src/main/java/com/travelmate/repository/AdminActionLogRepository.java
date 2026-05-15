package com.travelmate.repository;

import com.travelmate.entity.AdminActionLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminActionLogRepository extends JpaRepository<AdminActionLog, Long> {
    List<AdminActionLog> findAllByOrderByCreatedAtDesc();
    List<AdminActionLog> findByTargetTypeOrderByCreatedAtDesc(String targetType);
    List<AdminActionLog> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(String targetType, Long targetId);
}

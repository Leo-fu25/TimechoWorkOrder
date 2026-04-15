package com.timecho.workorder.repository;

import com.timecho.workorder.model.FeishuPushLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeishuPushLogRepository extends JpaRepository<FeishuPushLog, Long> {
    List<FeishuPushLog> findTop100ByOrderByCreatedAtDesc();
}

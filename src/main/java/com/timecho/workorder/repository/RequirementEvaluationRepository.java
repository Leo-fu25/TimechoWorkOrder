package com.timecho.workorder.repository;

import com.timecho.workorder.model.RequirementEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RequirementEvaluationRepository extends JpaRepository<RequirementEvaluation, Long> {
    List<RequirementEvaluation> findByWorkOrderIdOrderByCreatedAtDesc(Long workOrderId);

    void deleteByWorkOrderId(Long workOrderId);
}

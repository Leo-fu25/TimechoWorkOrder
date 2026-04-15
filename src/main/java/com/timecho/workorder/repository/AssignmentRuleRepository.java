package com.timecho.workorder.repository;

import com.timecho.workorder.model.AssignmentRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssignmentRuleRepository extends JpaRepository<AssignmentRule, Long> {
    List<AssignmentRule> findByActiveTrueOrderByPriorityAscIdAsc();
}

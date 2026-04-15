package com.timecho.workorder.repository;

import com.timecho.workorder.model.SlaPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SlaPolicyRepository extends JpaRepository<SlaPolicy, Long> {
    List<SlaPolicy> findByActiveTrueOrderByIdAsc();
}

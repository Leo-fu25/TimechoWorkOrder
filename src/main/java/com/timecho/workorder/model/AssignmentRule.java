package com.timecho.workorder.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "assignment_rules")
public class AssignmentRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @ManyToOne
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne
    @JoinColumn(name = "type_id")
    private WorkOrderType type;

    @Column(length = 64)
    private String source;

    @Column
    private String customerType;

    @ManyToOne
    @JoinColumn(name = "target_assignee_id", nullable = false)
    private User targetAssignee;

    @Column(nullable = false)
    private Integer priority;

    @Column(nullable = false)
    private boolean active;
}

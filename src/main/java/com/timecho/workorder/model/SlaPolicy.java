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
@Table(name = "sla_policies")
public class SlaPolicy {
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

    @ManyToOne
    @JoinColumn(name = "priority_id")
    private Priority priority;

    @Column(nullable = false)
    private Integer responseHours;

    @Column(nullable = false)
    private Integer resolveHours;

    @Column(nullable = false)
    private boolean autoEscalate;

    @ManyToOne
    @JoinColumn(name = "escalation_priority_id")
    private Priority escalationPriority;

    @Column(nullable = false)
    private boolean active;
}

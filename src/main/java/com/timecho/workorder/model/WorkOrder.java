package com.timecho.workorder.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "work_orders")
public class WorkOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @ManyToOne
    @JoinColumn(name = "type_id")
    private WorkOrderType type;

    @Column(length = 64)
    private String source;

    @Column
    private String customerName;

    @Column
    private String customerEmail;

    @Column(length = 64)
    private String customerType;

    @Column
    private String productName;

    @Column
    private String impactScope;

    @Column(columnDefinition = "TEXT")
    private String tags;
    
    @ManyToOne
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;
    
    @ManyToOne
    @JoinColumn(name = "assignee_id")
    private User assignee;
    
    @ManyToOne
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;
    
    @ManyToOne
    @JoinColumn(name = "status_id", nullable = false)
    private Status status;
    
    @ManyToOne
    @JoinColumn(name = "priority_id", nullable = false)
    private Priority priority;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime updatedAt;
    
    @Column
    private LocalDateTime completedAt;

    @Column
    private LocalDateTime firstResponseAt;

    @Column
    private LocalDateTime responseDueAt;

    @Column
    private LocalDateTime dueAt;

    @Column
    private LocalDateTime resolvedAt;

    @Column
    private LocalDateTime closedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

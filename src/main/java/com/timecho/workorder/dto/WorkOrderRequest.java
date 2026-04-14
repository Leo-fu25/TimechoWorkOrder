package com.timecho.workorder.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
public class WorkOrderRequest {
    private Long operatorId;

    @NotBlank
    private String title;

    @NotBlank
    private String description;

    @NotNull
    private Long requesterId;

    private Long assigneeId;

    @NotNull
    private Long departmentId;

    private Long statusId;
    private Long priorityId;
    private Long typeId;
    private String source;
    private String customerName;
    private String customerEmail;
    private String customerType;
    private String productName;
    private String impactScope;
    private String tags;
    private LocalDateTime responseDueAt;
    private LocalDateTime dueAt;
}

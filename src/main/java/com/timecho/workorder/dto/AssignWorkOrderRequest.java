package com.timecho.workorder.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class AssignWorkOrderRequest {
    private Long operatorId;

    @NotNull
    private Long assigneeId;

    private String remark;
}

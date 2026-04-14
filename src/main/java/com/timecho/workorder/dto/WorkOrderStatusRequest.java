package com.timecho.workorder.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class WorkOrderStatusRequest {
    private Long operatorId;

    @NotNull
    private Long statusId;

    private String remark;
}

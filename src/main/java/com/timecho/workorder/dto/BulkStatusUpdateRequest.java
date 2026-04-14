package com.timecho.workorder.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class BulkStatusUpdateRequest {
    private Long operatorId;

    @NotEmpty
    private List<Long> workOrderIds;

    @NotNull
    private Long statusId;

    private String remark;
}

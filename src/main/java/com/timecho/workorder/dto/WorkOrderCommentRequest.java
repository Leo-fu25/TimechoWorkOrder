package com.timecho.workorder.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class WorkOrderCommentRequest {
    @NotNull
    private Long userId;

    @NotBlank
    private String content;

    private boolean internalOnly;
}

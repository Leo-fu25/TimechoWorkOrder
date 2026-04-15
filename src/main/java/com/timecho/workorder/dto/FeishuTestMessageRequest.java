package com.timecho.workorder.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class FeishuTestMessageRequest {
    @NotBlank
    private String message;
}

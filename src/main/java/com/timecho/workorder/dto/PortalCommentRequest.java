package com.timecho.workorder.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@Data
public class PortalCommentRequest {
    @NotBlank
    @Email
    private String customerEmail;

    @NotBlank
    private String customerName;

    @NotBlank
    private String content;
}

package com.timecho.workorder.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class AttachmentRequest {
    @NotNull
    private Long uploadedById;

    @NotBlank
    private String fileName;

    @NotBlank
    private String fileUrl;

    private String contentType;
    private Long sizeBytes;
}

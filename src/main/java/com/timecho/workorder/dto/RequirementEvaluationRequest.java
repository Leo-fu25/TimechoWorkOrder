package com.timecho.workorder.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
public class RequirementEvaluationRequest {
    @NotNull
    private Long evaluatorId;

    @Min(1)
    @Max(5)
    private int requirementValue;

    @Min(1)
    @Max(5)
    private int developmentEffort;

    @Min(1)
    @Max(5)
    private int customerWeight;

    @Min(1)
    @Max(5)
    private int competitorImpact;

    @Min(1)
    @Max(5)
    private int impactScopeScore;

    private String comment;
}

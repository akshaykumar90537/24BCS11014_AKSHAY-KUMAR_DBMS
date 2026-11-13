package com.resolveit.escalation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpdateSlaPolicyRequest(
        @Min(1) @Max(720) int targetHours,
        @Min(0) @Max(90) int reopenWindowDays
) {}


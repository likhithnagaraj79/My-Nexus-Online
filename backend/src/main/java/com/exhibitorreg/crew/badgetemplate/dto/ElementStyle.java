package com.exhibitorreg.crew.badgetemplate.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;

public record ElementStyle(
        @DecimalMin("0") @DecimalMax("100") double xPercent,
        @DecimalMin("0") @DecimalMax("100") double yPercent,
        @Positive double fontSizePt,
        boolean bold) {
}

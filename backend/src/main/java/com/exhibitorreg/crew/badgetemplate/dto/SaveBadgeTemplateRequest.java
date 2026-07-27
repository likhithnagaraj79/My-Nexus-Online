package com.exhibitorreg.crew.badgetemplate.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record SaveBadgeTemplateRequest(
        @NotNull @Valid ElementStyle name, @NotNull @Valid ElementStyle designation, @NotNull @Valid ElementStyle company) {
}

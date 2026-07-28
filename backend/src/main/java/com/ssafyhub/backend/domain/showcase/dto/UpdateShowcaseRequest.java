package com.ssafyhub.backend.domain.showcase.dto;

import com.ssafyhub.backend.domain.deployment.enums.Visibility;

public record UpdateShowcaseRequest(
        Visibility visibility,
        String description
) {

}

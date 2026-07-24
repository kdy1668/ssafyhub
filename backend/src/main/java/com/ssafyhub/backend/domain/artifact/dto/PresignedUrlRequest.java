package com.ssafyhub.backend.domain.artifact.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PresignedUrlRequest(
        @NotBlank(message = "백엔드 파일 이름은 필수입니다.")
        @Size(max = 255, message = "백엔드 파일 이름은 255자를 초과할 수 없습니다.")
        String backendFileName,

        @NotBlank(message = "프론트엔드 파일 이름은 필수입니다.")
        @Size(max = 255, message = "프론트엔드 파일 이름은 255자를 초과할 수 없습니다.")
        String frontendFileName
) {
}

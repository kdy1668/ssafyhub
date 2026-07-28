package com.ssafyhub.backend.domain.deployment.dto;

import com.ssafyhub.backend.domain.deployment.enums.Visibility;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record DeploymentCreateRequest(
        @NotBlank(message = "프로젝트 이름은 필수입니다.")
        @Size(max = 100, message = "프로젝트 이름은 100자를 초과할 수 없습니다.")
        String projectName,

        @NotNull(message = "백엔드 Artifact ID는 필수입니다.")
        @Positive(message = "백엔드 Artifact ID는 양수여야 합니다.")
        Long backendArtifactId,

        @NotNull(message = "프론트엔드 Artifact ID는 필수입니다.")
        @Positive(message = "프론트엔드 Artifact ID는 양수여야 합니다.")
        Long frontendArtifactId,

        @NotBlank(message = "Java 실행 버전은 필수입니다.")
        String runtimeVersion,

        @Min(value = 1, message = "내부 포트는 1 이상이어야 합니다.")
        @Max(value = 65535, message = "내부 포트는 65535 이하여야 합니다.")
        int internalPort,

        @NotBlank(message = "데이터베이스 이름은 필수입니다.")
        @Size(max = 100, message = "데이터베이스 이름은 100자를 초과할 수 없습니다.")
        @Pattern(
                regexp = "^[A-Za-z][A-Za-z0-9_]{0,99}$",
                message = "데이터베이스 이름은 영문자로 시작하고 영문, 숫자, 언더스코어만 사용할 수 있습니다."
        )
        String databaseName,

        @NotBlank(message = "데이터베이스 비밀번호는 필수입니다.")
        @Size(max = 200, message = "데이터베이스 비밀번호는 200자를 초과할 수 없습니다.")
        String databasePassword,

        @NotNull(message = "환경변수 목록은 필수입니다.")
        @Size(min = 1, max = 20, message = "환경변수는 1개 이상 20개 이하로 입력해주세요.")
        List<@Valid DeploymentEnvRequest> env,

        Visibility visibility,

        @Size(max = 2000, message = "설명은 2000자를 초과할 수 없습니다.")
        String description
) {
}

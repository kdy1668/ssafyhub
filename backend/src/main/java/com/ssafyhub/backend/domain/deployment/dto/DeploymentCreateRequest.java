package com.ssafyhub.backend.domain.deployment.dto;

import com.ssafyhub.backend.domain.deployment.enums.Visibility;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "배포 생성 요청")
public record DeploymentCreateRequest(
        @Schema(description = "프로젝트 이름", example = "ssafyhub-showcase")
        @NotBlank(message = "프로젝트 이름은 필수입니다.")
        @Size(max = 100, message = "프로젝트 이름은 100자를 초과할 수 없습니다.")
        String projectName,

        @Schema(description = "사전에 발급받은 백엔드 산출물 Artifact ID", example = "1")
        @NotNull(message = "백엔드 Artifact ID는 필수입니다.")
        @Positive(message = "백엔드 Artifact ID는 양수여야 합니다.")
        Long backendArtifactId,

        @Schema(description = "사전에 발급받은 프론트엔드 산출물 Artifact ID", example = "2")
        @NotNull(message = "프론트엔드 Artifact ID는 필수입니다.")
        @Positive(message = "프론트엔드 Artifact ID는 양수여야 합니다.")
        Long frontendArtifactId,

        @Schema(description = "런타임 표기용 메타 정보(실제 실행 환경은 업로드한 Dockerfile이 결정합니다)", example = "21")
        @NotBlank(message = "runtimeVersion은 필수입니다.")
        String runtimeVersion,

        @Schema(description = "애플리케이션 내부 포트", example = "8080")
        @Min(value = 1, message = "내부 포트는 1 이상이어야 합니다.")
        @Max(value = 65535, message = "내부 포트는 65535 이하여야 합니다.")
        int internalPort,

        @Schema(description = "데이터베이스 이름 (영문 시작, 영문/숫자/언더스코어). DB가 필요 없는 앱이면 비워두세요.",
                example = "ssafyhub_db", nullable = true)
        @Size(max = 100, message = "데이터베이스 이름은 100자를 초과할 수 없습니다.")
        @Pattern(
                regexp = "^$|^[A-Za-z][A-Za-z0-9_]{0,99}$",
                message = "데이터베이스 이름은 영문자로 시작하고 영문, 숫자, 언더스코어만 사용할 수 있습니다."
        )
        String databaseName,

        @Schema(description = "데이터베이스 비밀번호. databaseName을 입력한 경우에만 필수입니다.",
                example = "dbPassword123", nullable = true)
        @Size(max = 200, message = "데이터베이스 비밀번호는 200자를 초과할 수 없습니다.")
        String databasePassword,

        @Schema(description = "관계형 DB 엔진 (지원: MYSQL, POSTGRES). databaseName을 입력했는데 비워두면 MYSQL로 간주합니다.",
                example = "MYSQL", nullable = true)
        String databaseEngine,

        @Schema(description = "사전에 발급받은 DB 초기화 SQL Artifact ID (선택 - MyBatis 등 자동 테이블 생성이 안 되는 " +
                "경우에만). databaseName을 입력했을 때만 사용할 수 있습니다.", example = "3", nullable = true)
        @Positive(message = "스키마 SQL Artifact ID는 양수여야 합니다.")
        Long schemaArtifactId,

        @Schema(description = "MySQL/PostgreSQL 외에 추가로 필요한 백킹서비스 목록 (현재 지원: REDIS, MONGODB). " +
                "필요 없으면 비워두세요.", example = "[\"REDIS\"]", nullable = true)
        List<String> neededServices,

        @Schema(description = "환경변수 목록 (1개 이상 20개 이하)")
        @NotNull(message = "환경변수 목록은 필수입니다.")
        @Size(min = 1, max = 20, message = "환경변수는 1개 이상 20개 이하로 입력해주세요.")
        List<@Valid DeploymentEnvRequest> env,

        @Schema(description = "공개 여부. 미입력 시 서비스 기본값 적용", example = "PUBLIC")
        Visibility visibility,

        @Schema(description = "서비스 설명", example = "SSAFYHUB 쇼케이스 데모 서비스입니다.")
        @Size(max = 2000, message = "설명은 2000자를 초과할 수 없습니다.")
        String description
) {
}

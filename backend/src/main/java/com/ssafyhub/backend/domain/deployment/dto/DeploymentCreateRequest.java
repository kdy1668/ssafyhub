package com.ssafyhub.backend.domain.deployment.dto;

import com.ssafyhub.backend.domain.deployment.enums.Visibility;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
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

        @Schema(description = "사전에 발급받은 백엔드 산출물 Artifact ID. 프론트만 배포한다면 비워두세요.",
                example = "1", nullable = true)
        @Positive(message = "백엔드 Artifact ID는 양수여야 합니다.")
        Long backendArtifactId,

        @Schema(description = "사전에 발급받은 프론트엔드 산출물 Artifact ID. 백엔드가 프론트를 직접 서빙하는 " +
                "모놀리식 구조라면 비워두세요.", example = "2", nullable = true)
        @Positive(message = "프론트엔드 Artifact ID는 양수여야 합니다.")
        Long frontendArtifactId,

        @Schema(description = "백엔드 언어/프레임워크 스택 (지원: SPRING_BOOT, DJANGO, FASTAPI, FLASK, " +
                "EXPRESS). backendArtifactId를 보냈을 때만 필수입니다. 이 값에 맞춰 플랫폼이 Dockerfile을 자동 " +
                "생성합니다 - 사용자는 Dockerfile을 올릴 필요가 없습니다. (JPA/MyBatis 구분은 없음 - 둘 다 " +
                ".jar 실행이라 빌드 방식이 같고, 테이블 자동 생성 여부는 manualMigration으로 따로 받는다)",
                example = "SPRING_BOOT", nullable = true)
        String stack,

        @Schema(description = "백엔드 런타임 버전 (Java: 17/21, Python: 3.11/3.12, Node.js: 18/20 등). " +
                "backendArtifactId를 보냈을 때만 필수입니다.", example = "21", nullable = true)
        String runtimeVersion,

        @Schema(description = "백엔드 애플리케이션 내부 포트. backendArtifactId를 보냈을 때만 필수입니다.",
                example = "8080", nullable = true)
        Integer internalPort,

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

        @Schema(description = "관계형 DB 엔진 (지원: MYSQL). databaseName을 입력했는데 비워두면 MYSQL로 간주합니다.",
                example = "MYSQL", nullable = true)
        String databaseEngine,

        @Schema(description = "DB 호스트를 주입할 환경변수 이름. 비워두면 DB_HOST를 쓴다. 앱이 DB_HOST가 " +
                "아닌 다른 이름(예: MYSQL_HOST)을 기대하면 여기 지정하세요.", example = "DB_HOST", nullable = true)
        @Size(max = 100, message = "환경변수 이름은 100자를 초과할 수 없습니다.")
        String dbHostEnvKey,

        @Schema(description = "DB 포트를 주입할 환경변수 이름. 비워두면 DB_PORT를 쓴다.",
                example = "DB_PORT", nullable = true)
        @Size(max = 100, message = "환경변수 이름은 100자를 초과할 수 없습니다.")
        String dbPortEnvKey,

        @Schema(description = "DB 이름을 주입할 환경변수 이름. 비워두면 DB_NAME을 쓴다.",
                example = "DB_NAME", nullable = true)
        @Size(max = 100, message = "환경변수 이름은 100자를 초과할 수 없습니다.")
        String dbNameEnvKey,

        @Schema(description = "DB 사용자명을 주입할 환경변수 이름. 비워두면 DB_USERNAME을 쓴다(DB_USER도 " +
                "항상 같이 주입되긴 하지만, 그 외 이름을 쓰는 앱이면 여기 지정하세요).",
                example = "DB_USER", nullable = true)
        @Size(max = 100, message = "환경변수 이름은 100자를 초과할 수 없습니다.")
        String dbUsernameEnvKey,

        @Schema(description = "DB 비밀번호를 주입할 환경변수 이름. 비워두면 DB_PASSWORD를 쓴다.",
                example = "DB_PASSWORD", nullable = true)
        @Size(max = 100, message = "환경변수 이름은 100자를 초과할 수 없습니다.")
        String dbPasswordEnvKey,

        @Schema(description = "테이블이 앱 시작 시 자동으로 생성되는지 여부(JPA의 ddl-auto, Django의 migrate, " +
                "Sequelize의 sync() 등을 쓰면 true). databaseName을 입력했다면(=DB를 쓴다면) 필수입니다. " +
                "false면 schemaArtifactId가 필수이고, true면 사용할 수 없습니다.", example = "false", nullable = true)
        Boolean manualMigration,

        @Schema(description = "사전에 발급받은 DB 초기화 SQL Artifact ID (선택 - manualMigration이 true일 " +
                "때만). databaseName을 입력했을 때만 사용할 수 있습니다.", example = "3", nullable = true)
        @Positive(message = "스키마 SQL Artifact ID는 양수여야 합니다.")
        Long schemaArtifactId,

        @Schema(description = "MySQL 외에 추가로 필요한 백킹서비스 목록 (현재 지원: REDIS, MONGODB). " +
                "필요 없으면 비워두세요.", example = "[\"REDIS\"]", nullable = true)
        List<String> neededServices,

        @Schema(description = "환경변수 목록 (0개 이상 20개 이하)")
        @NotNull(message = "환경변수 목록은 필수입니다.")
        @Size(max = 20, message = "환경변수는 최대 20개까지 입력할 수 있습니다.")
        List<@Valid DeploymentEnvRequest> env,

        @Schema(description = "공개 여부. 미입력 시 서비스 기본값 적용", example = "PUBLIC")
        Visibility visibility,

        @Schema(description = "서비스 설명", example = "SSAFYHUB 쇼케이스 데모 서비스입니다.")
        @Size(max = 2000, message = "설명은 2000자를 초과할 수 없습니다.")
        String description
) {
}

package com.ssafyhub.backend.domain.deployment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 코드(zip/jar)만 새로 올려 재배포하는 요청. 이미 배포된 서비스에서 소스만 고쳐 다시 올릴 때 사용한다.
 * 스택/포트/DB 설정은 그대로 두고 아티팩트 참조만 새 것으로 교체한 뒤 재배포를 트리거한다.
 * 보낸 것만 교체하며(부분 교체), 최소 하나는 있어야 한다. 이 배포에 원래 없던 종류는 추가할 수 없다.
 */
@Schema(description = "새 아티팩트로 재배포 요청. 보낸 것만 교체(부분), 최소 하나 필수.")
public record DeploymentRedeployRequest(
        @Schema(description = "새 백엔드 아티팩트 ID(선택). 기존 스택과 같은 형식(jar/zip)이어야 함")
        Long backendArtifactId,

        @Schema(description = "새 프론트엔드 정적 zip 아티팩트 ID(선택)")
        Long frontendArtifactId,

        @Schema(description = "새 DB 초기화 SQL 아티팩트 ID(선택). 원래 수동 마이그레이션 배포에서만")
        Long schemaArtifactId
) {
}

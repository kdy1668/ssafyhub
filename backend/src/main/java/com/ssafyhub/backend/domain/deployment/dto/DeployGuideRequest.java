package com.ssafyhub.backend.domain.deployment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * 온보딩 배포 가이드 생성 요청.
 * 클라이언트(웹 업로드 폼 또는 CLI)가 프로젝트를 훑어 파일 트리와 핵심 파일 내용을 담아 보낸다.
 * 실제 zip/배포 전에 "어떻게 올릴지"를 분석받는 용도라, 배포 리소스와는 무관하다.
 */
@Schema(description = "배포 가이드 생성 요청 (프로젝트 파일 트리 + 핵심 파일)")
public record DeployGuideRequest(
        @Schema(description = "프로젝트 이름(선택, 로그용)", example = "richman")
        String projectName,

        @Schema(description = "프로젝트 파일 트리 텍스트. 디렉터리 구조를 그대로.", example = "backend/\\n  manage.py\\n  requirements.txt\\nfrontend/\\n  package.json")
        @NotBlank(message = "파일 트리는 비어 있을 수 없습니다.")
        String fileTree,

        @Schema(description = "판정에 필요한 핵심 파일들의 경로와 내용(package.json, requirements.txt, settings.py, api client 등)")
        List<KeyFile> keyFiles
) {
    @Schema(description = "핵심 파일 하나")
    public record KeyFile(
            @Schema(description = "파일 경로", example = "frontend/src/api/client.js")
            String path,
            @Schema(description = "파일 내용(비밀값은 서버에서 마스킹됨)")
            String content
    ) {
    }
}

package com.ssafyhub.backend.domain.deployment.dto;

import com.ssafyhub.backend.global.ai.AiJsonSchema;
import java.util.List;

/**
 * 온보딩 배포 가이드의 모델 출력 스키마. 구조화 출력(strict json_schema)으로 이 형태가 강제된다.
 *
 * 사용자가 올린 프로젝트(파일 트리 + 핵심 파일)를 분석해, SSAFY Hub에 어떻게 올릴지를
 * 구조화해 낸다. 이 JSON을 {@code DeployGuideRenderer}가 CLAUDE.md 형태의 가이드로 렌더링한다.
 *
 * strict 모드 요구사항: 모든 object에 additionalProperties:false, 그리고 properties의 모든
 * 키가 required에 있어야 한다. record 컴포넌트 이름과 스키마 property 이름이 정확히 일치해야 한다.
 * 필드 설명은 스키마 description에 적는다(실제로 모델에게 전달되는 경로).
 */
public record DeployGuide(
        String appType,
        String summary,
        Backend backend,
        Frontend frontend,
        List<PackageStep> packageSteps,
        ApiWiring apiWiring,
        List<EnvVar> requiredEnv,
        List<BackingService> backingServices,
        List<FrameworkFix> frameworkFixes,
        List<String> warnings
) {

    public record Backend(
            boolean present, String stack, String artifactFormat,
            String buildContext, int port, String notes) {
    }

    public record Frontend(
            boolean present, String framework, String buildCommand,
            String distPath, String notes) {
    }

    public record PackageStep(String target, String instruction) {
    }

    public record ApiWiring(
            boolean needsChange, String currentBase, String targetBase, List<Change> changes) {
    }

    public record Change(String file, String action, String why) {
    }

    public record EnvVar(String key, String reason, boolean required) {
    }

    public record BackingService(String serviceType, String reason) {
    }

    public record FrameworkFix(String title, String detail) {
    }

    public static final AiJsonSchema<DeployGuide> SCHEMA = new AiJsonSchema<>(
            DeployGuide.class,
            "deploy_guide",
            """
            {
              "type": "object",
              "properties": {
                "appType": {
                  "type": "string",
                  "description": "앱 유형. MONOLITH=백엔드가 프론트까지 서빙, SEPARATED=정적 프론트+백엔드 분리.",
                  "enum": ["MONOLITH", "SEPARATED", "FRONTEND_ONLY", "BACKEND_ONLY"]
                },
                "summary": {
                  "type": "string",
                  "description": "이 프로젝트가 무엇이고 어떤 유형인지 1~2문장. 한국어."
                },
                "backend": {
                  "type": "object",
                  "properties": {
                    "present": { "type": "boolean", "description": "백엔드 존재 여부" },
                    "stack": {
                      "type": "string",
                      "description": "백엔드 스택. 없으면 NONE.",
                      "enum": ["SPRING_JPA", "SPRING_MYBATIS", "DJANGO", "FASTAPI", "FLASK", "EXPRESS", "NONE"]
                    },
                    "artifactFormat": {
                      "type": "string",
                      "description": "업로드 형식. Java=JAR(빌드된 .jar), Python/Node=SOURCE_ZIP(소스 zip). 없으면 NONE.",
                      "enum": ["JAR", "SOURCE_ZIP", "NONE"]
                    },
                    "buildContext": { "type": "string", "description": "업로드/zip 대상 경로. 예: backend/ , 루트. 없으면 빈 문자열." },
                    "port": { "type": "integer", "description": "앱이 리슨할 내부 포트. 모르면 8080." },
                    "notes": { "type": "string", "description": "기동 관련 특이사항. 없으면 빈 문자열." }
                  },
                  "required": ["present", "stack", "artifactFormat", "buildContext", "port", "notes"],
                  "additionalProperties": false
                },
                "frontend": {
                  "type": "object",
                  "properties": {
                    "present": { "type": "boolean", "description": "프론트 존재 여부" },
                    "framework": { "type": "string", "description": "Vue/React/Angular/Svelte 등. 없으면 NONE." },
                    "buildCommand": { "type": "string", "description": "빌드 명령. 예: npm run build. 없으면 빈 문자열." },
                    "distPath": { "type": "string", "description": "빌드 산출물 경로. 예: frontend/dist. 없으면 빈 문자열." },
                    "notes": { "type": "string", "description": "특이사항. 없으면 빈 문자열." }
                  },
                  "required": ["present", "framework", "buildCommand", "distPath", "notes"],
                  "additionalProperties": false
                },
                "packageSteps": {
                  "type": "array",
                  "description": "무엇을 어떻게 zip/업로드하는지 순서대로. 프론트는 dist 내용물을 zip해 index.html이 루트에 오게.",
                  "items": {
                    "type": "object",
                    "properties": {
                      "target": { "type": "string", "enum": ["backend", "frontend", "schema"] },
                      "instruction": { "type": "string", "description": "정확히 무엇을 어떻게" }
                    },
                    "required": ["target", "instruction"],
                    "additionalProperties": false
                  }
                },
                "apiWiring": {
                  "type": "object",
                  "description": "프론트가 백엔드를 호출하는 방식. SSAFY Hub는 same-origin /api 규약.",
                  "properties": {
                    "needsChange": { "type": "boolean", "description": "현재 설정을 바꿔야 하는지" },
                    "currentBase": { "type": "string", "description": "감지된 현재 API base. 없으면 '없음'." },
                    "targetBase": { "type": "string", "description": "목표 API base. 보통 /api." },
                    "changes": {
                      "type": "array",
                      "description": "수정할 파일과 지시. 파괴적 자동수정이 아니라 제안 형태.",
                      "items": {
                        "type": "object",
                        "properties": {
                          "file": { "type": "string" },
                          "action": { "type": "string", "description": "무엇을 어떻게 바꾸는지" },
                          "why": { "type": "string", "description": "이유" }
                        },
                        "required": ["file", "action", "why"],
                        "additionalProperties": false
                      }
                    }
                  },
                  "required": ["needsChange", "currentBase", "targetBase", "changes"],
                  "additionalProperties": false
                },
                "requiredEnv": {
                  "type": "array",
                  "description": "기본값 없이 반드시 필요한 환경변수(예: os.environ['X'], env('X') default 없음). 비밀 '값'은 넣지 않고 키 이름만.",
                  "items": {
                    "type": "object",
                    "properties": {
                      "key": { "type": "string" },
                      "reason": { "type": "string" },
                      "required": { "type": "boolean" }
                    },
                    "required": ["key", "reason", "required"],
                    "additionalProperties": false
                  }
                },
                "backingServices": {
                  "type": "array",
                  "description": "필요한 백킹 서비스. 플랫폼이 프로비저닝한다.",
                  "items": {
                    "type": "object",
                    "properties": {
                      "serviceType": { "type": "string", "enum": ["MYSQL", "REDIS", "MONGODB"] },
                      "reason": { "type": "string", "description": "필요 근거" }
                    },
                    "required": ["serviceType", "reason"],
                    "additionalProperties": false
                  }
                },
                "frameworkFixes": {
                  "type": "array",
                  "description": "프레임워크별 필요한 조치. 예: Django ALLOWED_HOSTS, MyBatis schema.sql.",
                  "items": {
                    "type": "object",
                    "properties": {
                      "title": { "type": "string" },
                      "detail": { "type": "string" }
                    },
                    "required": ["title", "detail"],
                    "additionalProperties": false
                  }
                },
                "warnings": {
                  "type": "array",
                  "description": "확신하지 못하는 부분/사용자 확인이 필요한 항목.",
                  "items": { "type": "string" }
                }
              },
              "required": ["appType", "summary", "backend", "frontend", "packageSteps", "apiWiring", "requiredEnv", "backingServices", "frameworkFixes", "warnings"],
              "additionalProperties": false
            }
            """);
}

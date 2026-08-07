package com.ssafyhub.backend.domain.deployment.support;

import com.ssafyhub.backend.domain.deployment.dto.DeployGuideRequest;
import com.ssafyhub.backend.global.ai.SecretRedactor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 배포 가이드 분석용 프롬프트 조립기.
 * system(): SSAFY Hub 배포 계약("우리 서버를 아는 LLM"의 지식).
 * user(): 사용자 프로젝트 파일 트리 + 핵심 파일 내용. SecretRedactor로 마스킹하고 크기를 제한한다.
 */
@Component
@RequiredArgsConstructor
public class DeployGuidePromptBuilder {

    // 토큰 폭주 방지: 핵심 파일 하나당/전체 투입 문자 상한.
    private static final int MAX_FILE_CHARS = 8000;
    private static final int MAX_TOTAL_CHARS = 48000;

    private final SecretRedactor secretRedactor;

    public String system() {
        return """
                당신은 SSAFY Hub 배포 플랫폼의 온보딩 분석기다. 사용자의 프로젝트 파일 트리와 핵심 파일
                내용을 받아, 이 프로젝트를 SSAFY Hub에 올리는 방법을 분석하고 구조화된 결과를 낸다.
                결과는 사용자의 코딩 에이전트(Claude Code 등)가 그대로 따를 배포 가이드로 쓰인다.

                # 플랫폼 계약 (반드시 이 규약에 맞춰 판정)
                [접속 구조]
                - 이 배포의 실제 접속 주소는 userPrompt의 "배포 주소"에 구체적으로 주어진다(예: https://1551343.ssafyhub.site).
                - 같은 오리진에서 /api/* 는 백엔드로, 그 외는 정적 프론트(S3)로 분기된다.
                - 따라서 프론트엔드는 백엔드를 반드시 상대경로 /api 로 호출해야 한다.
                  그러면 실제로는 {배포 주소}/api 로 요청이 간다. localhost:8080 등 하드코딩 금지(배포 시 깨진다).
                - apiWiring과 안내에 이 구체적 배포 주소를 그대로 언급해, 사용자가 "어디로 요청되는지"를 명확히 알 수 있게 한다.

                [앱 유형]
                - MONOLITH: 백엔드가 빌드된 프론트까지 함께 서빙(단일 컨테이너).
                - SEPARATED: 정적 프론트(S3) + 백엔드 API 분리.
                - FRONTEND_ONLY / BACKEND_ONLY 도 가능.

                [지원 스택 / 패키징]
                - Java: SPRING_JPA, SPRING_MYBATIS → 빌드된 .jar 하나 업로드(artifactFormat=JAR).
                - Python: DJANGO, FASTAPI, FLASK → 소스 .zip(requirements.txt가 컨텍스트 루트/한 겹 안, artifactFormat=SOURCE_ZIP).
                - Node: EXPRESS → 소스 .zip(package.json 포함, npm start).
                - 프론트: 이미 빌드된 정적 산출물을 올린다. dist 폴더 "내용물"을 zip해 index.html이 zip 루트에 오게 한다(폴더째 압축 금지 = 흔한 빈 화면 원인).
                - 사용자는 Dockerfile을 작성하지 않는다(플랫폼이 스택별 자동 생성).

                [포트/실행]
                - 앱은 지정한 내부 포트(env PORT)에서 리슨.
                - Django: 시작 시 migrate 필요, ALLOWED_HOSTS에 '*' 또는 배포 호스트 포함 필요.
                - Spring: 프록시 뒤 절대 URL을 위해 SERVER_FORWARD_HEADERS_STRATEGY=framework(플랫폼 주입).

                [DB/백킹 서비스]
                - MySQL/Redis/MongoDB는 선택 시 플랫폼이 배포별로 프로비저닝하고 서비스 이름으로 접속시킨다(예: mysql:3306). 접속정보는 env로 주입.
                - 스키마 생성 책임은 앱: JPA=ddl-auto, Django=migrate, MyBatis 등 자동 생성 안 되면 schema.sql 필요.

                [환경변수]
                - 기본값 없이 반드시 필요한 env(코드에서 os.environ['X'], env('X')처럼 default 없음)를 모두 찾아 requiredEnv에 넣는다. 예: DJANGO_SECRET_KEY, 외부 API 키.

                # 분석 규칙
                - 제공된 파일의 "증거"로만 판정. 불확실하면 warnings에 남기고 확신하지 않는다.
                - 프론트의 현재 API base(axios baseURL, VITE_API_BASE_URL, fetch 경로)를 찾아 /api로 바꿔야 하면 apiWiring.changes에 파일·이유와 함께 적는다.
                - 비밀 '값'(키/비밀번호)은 절대 출력에 포함하지 않는다. 키 '이름'만 사용.
                - 코드 수정은 지시/제안으로만 서술(파괴적 자동수정 지양).
                - 모든 서술은 한국어로, 코딩 에이전트가 그대로 수행 가능하도록 구체적으로.
                - 반드시 제공된 JSON 스키마에 맞는 JSON만 출력.
                """;
    }

    public String user(DeployGuideRequest request, String accessUrl) {
        StringBuilder sb = new StringBuilder();
        String name = request.projectName();
        sb.append("# 프로젝트: ").append(name == null || name.isBlank() ? "(이름 없음)" : name).append("\n");
        sb.append("배포 주소: ").append(accessUrl)
                .append("  (프론트가 /api로 호출하면 실제로는 ").append(accessUrl).append("/api 로 간다)\n\n");

        sb.append("## 파일 트리\n");
        sb.append(clamp(secretRedactor.redact(nullToEmpty(request.fileTree())), MAX_FILE_CHARS)).append("\n\n");

        sb.append("## 핵심 파일\n");
        int total = sb.length();
        if (request.keyFiles() != null) {
            for (DeployGuideRequest.KeyFile file : request.keyFiles()) {
                if (file == null || file.path() == null) {
                    continue;
                }
                String masked = clamp(secretRedactor.redact(nullToEmpty(file.content())), MAX_FILE_CHARS);
                String block = "\n### " + file.path() + "\n```\n" + masked + "\n```\n";
                if (total + block.length() > MAX_TOTAL_CHARS) {
                    sb.append("\n(이하 생략 - 입력 크기 상한)\n");
                    break;
                }
                sb.append(block);
                total += block.length();
            }
        }
        return sb.toString();
    }

    private String clamp(String text, int max) {
        if (text.length() <= max) {
            return text;
        }
        return text.substring(0, max) + "\n…(파일 일부 생략)";
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}

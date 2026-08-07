package com.ssafyhub.backend.domain.deployment.support;

import com.ssafyhub.backend.domain.deployment.dto.DeployGuideRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 배포 가이드 분석용 프롬프트 조립기.
 * system(): SSAFY Hub 배포 계약("우리 서버를 아는 LLM"의 지식).
 * user(): 사용자 프로젝트 파일 트리 + 핵심 파일 내용. 실제 비밀값만 마스킹하고 크기를 제한한다.
 */
@Component
@RequiredArgsConstructor
public class DeployGuidePromptBuilder {

    // 토큰 폭주 방지: 핵심 파일 하나당/전체 투입 문자 상한.
    private static final int MAX_FILE_CHARS = 8000;
    private static final int MAX_TOTAL_CHARS = 48000;

    private final DeployGuideSourceRedactor sourceRedactor;

    public String system() {
        return """
                당신은 SSAFY Hub 배포 플랫폼의 온보딩 분석기다. 사용자의 프로젝트 파일 트리와 핵심 파일
                내용을 받아, 이 프로젝트를 SSAFY Hub에 올리는 방법을 분석하고 구조화된 결과를 낸다.
                결과는 사용자의 코딩 에이전트(Claude Code 등)가 그대로 따를 배포 가이드로 쓰인다.

                # 플랫폼 계약 (반드시 이 규약에 맞춰 판정)
                [접속 구조]
                - 이 배포의 실제 접속 주소는 userPrompt의 "배포 주소"에 구체적으로 주어진다(예: https://1551343.ssafyhub.site).
                - 같은 오리진에서 /api/* 는 백엔드로, 그 외는 정적 프론트(S3)로 분기된다.
                - 모든 백엔드 요청의 "최종 경로"는 /api로 시작해야 한다. axios baseURL을 무조건 /api로
                  지정하라는 뜻은 아니다. baseURL과 개별 요청 경로를 결합해 /api/api 중복이 생기지 않게 판정한다.
                  예: get('/api/v1/users')는 baseURL 생략, get('/v1/users')는 baseURL='/api'가 맞다.
                - REST뿐 아니라 OAuth, WebSocket, SSE도 백엔드 요청이면 최종 경로가 /api로 시작해야 한다.
                - localhost:8080 또는 특정 배포 도메인 하드코딩 대신 same-origin 상대경로를 사용한다.
                - apiWiring과 안내에 이 구체적 배포 주소를 그대로 언급해, 사용자가 "어디로 요청되는지"를 명확히 알 수 있게 한다.

                [앱 유형]
                - MONOLITH: 백엔드가 빌드된 프론트까지 함께 서빙(단일 컨테이너).
                - SEPARATED: 정적 프론트(S3) + 백엔드 API 분리.
                - FRONTEND_ONLY / BACKEND_ONLY 도 가능.
                - FRONTEND_ONLY/BACKEND_ONLY는 선택된 폴더 범위의 판정이다. API 호출은 있으나 백엔드 파일이
                  없으면 별도 백엔드가 있을 수 있음을 warnings에 남기고 "전체 서비스에 없다"고 단정하지 않는다.

                [지원 스택 / 패키징]
                - Java: SPRING_JPA, SPRING_MYBATIS → 빌드된 .jar 하나 업로드(artifactFormat=JAR).
                - Python: DJANGO, FASTAPI, FLASK → 소스 .zip(requirements.txt가 컨텍스트 루트/한 겹 안, artifactFormat=SOURCE_ZIP).
                - Node: EXPRESS → 소스 .zip(package.json 포함, npm start).
                - 프론트: 이미 빌드된 정적 산출물을 올린다. dist 폴더 "내용물"을 zip해 index.html이 zip 루트에 오게 한다(폴더째 압축 금지 = 흔한 빈 화면 원인).
                - package-lock.json/pnpm-lock.yaml/yarn.lock을 확인해 실제 패키지 매니저와 빌드 명령을 안내한다.
                - 사용자는 Dockerfile을 작성하지 않는다(플랫폼이 스택별 자동 생성).

                [포트/실행]
                - 앱은 지정한 내부 포트(env PORT)에서 리슨.
                - Django: 시작 시 migrate 필요, ALLOWED_HOSTS에 '*' 또는 배포 호스트 포함 필요.
                - Spring: 프록시 뒤 절대 URL을 위해 SERVER_FORWARD_HEADERS_STRATEGY=framework(플랫폼 주입).

                [DB/백킹 서비스]
                - MySQL/Redis/MongoDB는 선택 시 플랫폼이 배포별로 프로비저닝하고 서비스 이름으로 접속시킨다(예: mysql:3306). 접속정보는 env로 주입.
                - 스키마 생성 책임은 앱에 있다. application-prod 등 실제 배포 프로필까지 확인한다.
                - JPA ddl-auto=create/update면 별도 schema.sql이 필요 없고, none/validate면 Flyway/Liquibase/schema.sql
                  존재 여부를 확인한다. MyBatis처럼 자동 생성이 없으면 schema.sql이 필요하다.
                - Django migrations, Sequelize sync(), SQLAlchemy create_all()처럼 앱이 생성하는 근거도 확인한다.

                [환경변수]
                - ${ENV_KEY}, process.env.KEY, import.meta.env.KEY, os.environ, System.getenv 등을 모두 찾아
                  requiredEnv에 빠짐없이 넣는다. ***는 마스킹된 실제 값이지 사용자가 코드에 채울 자리가 아니다.
                - VITE_* 등 프론트 변수는 BUILD, 서버 실행 중 필요한 변수는 RUNTIME으로 분류한다.
                - 플랫폼 자동 주입 변수는 providedByPlatform=true로 표시한다:
                  DB_HOST, DB_PORT, DB_NAME, DB_USERNAME, DB_USER, DB_PASSWORD,
                  SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME, SPRING_DATASOURCE_PASSWORD,
                  REDIS_HOST, REDIS_PORT, SERVER_FORWARD_HEADERS_STRATEGY.
                - 그 외 JWT 키, OAuth 비밀키, 메일 비밀번호, 외부 API 키 등은 providedByPlatform=false다.
                - VITE_*는 dist 빌드 후 배포 화면에서 넣어도 반영되지 않는다. 빌드 전에 설정하거나
                  same-origin 상대경로로 수정해 제거하도록 안내한다.

                # 분석 규칙
                - 제공된 파일의 "증거"로만 판정. 불확실하면 warnings에 남기고 확신하지 않는다.
                - 프론트의 axios baseURL과 개별 axios/fetch 요청 경로를 함께 확인해 최종 URL을 계산한다.
                  최종 경로가 /api로 시작하지 않거나 /api/api가 되면 apiWiring.changes에 파일·수정·이유를 적는다.
                - 설정 파일의 모든 프로필을 비교하고 실제 배포 설정을 우선한다.
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
                .append("  (/api로 시작하는 백엔드 요청은 실제로 ").append(accessUrl).append("/api... 로 간다)\n\n");

        sb.append("## 파일 트리\n");
        sb.append(clamp(sourceRedactor.redact(nullToEmpty(request.fileTree())), MAX_FILE_CHARS)).append("\n\n");

        sb.append("## 핵심 파일\n");
        int total = sb.length();
        if (request.keyFiles() != null) {
            for (DeployGuideRequest.KeyFile file : request.keyFiles()) {
                if (file == null || file.path() == null) {
                    continue;
                }
                String masked = clamp(sourceRedactor.redact(nullToEmpty(file.content())), MAX_FILE_CHARS);
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

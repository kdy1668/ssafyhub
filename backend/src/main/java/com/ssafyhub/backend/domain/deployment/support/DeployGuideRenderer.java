package com.ssafyhub.backend.domain.deployment.support;

import com.ssafyhub.backend.domain.deployment.dto.DeployGuide;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * DeployGuide(구조화 JSON) → CLAUDE.md 형식 마크다운 가이드로 렌더링.
 *
 * 마크다운 포맷을 LLM에 맡기지 않고 여기서 결정론적으로 만든다(포맷 흔들림/누락 방지).
 * 결과물은 사용자가 레포 루트에 넣으면 코딩 에이전트가 읽고 배포 준비를 돕는 용도.
 */
@Component
public class DeployGuideRenderer {

    public String render(DeployGuide g, String projectName, String accessUrl) {
        StringBuilder md = new StringBuilder();
        String name = (projectName == null || projectName.isBlank()) ? "이 레포" : projectName;

        md.append("# SSAFYHUB 배포 가이드 (").append(name).append(", 자동 생성)\n\n");
        md.append("> SSAFY Hub가 프로젝트를 분석해 만든 배포 가이드입니다. ")
                .append("코딩 에이전트(Claude Code 등)가 이 지침을 따라 배포 준비를 도와줍니다.\n\n");
        md.append("- **요약**: ").append(safe(g.summary())).append("\n");
        md.append("- **앱 유형**: ").append(safe(g.appType())).append("\n");
        md.append("- **배포 주소**: `").append(safe(accessUrl)).append("` ")
                .append("(배포되면 이 주소로 접속됩니다)\n\n");

        // 1. 패키징
        md.append("## 1. 패키징 (무엇을 올리나)\n");
        if (g.packageSteps() != null) {
            for (DeployGuide.PackageStep s : g.packageSteps()) {
                md.append("- **").append(safe(s.target())).append("**: ").append(safe(s.instruction())).append("\n");
            }
        }
        if (g.frontend() != null && g.frontend().present()) {
            md.append("- ⚠️ 프론트 zip은 **dist 내용물**을 압축해 `index.html`이 루트에 오게 하세요.\n");
        }
        md.append("\n");

        // 2. API 연결
        md.append("## 2. 프론트 → 백엔드 연결\n");
        DeployGuide.ApiWiring w = g.apiWiring();
        if (w != null) {
            md.append("- 백엔드 최종 경로 prefix: **`").append(safe(w.targetBase())).append("`** (same-origin)\n");
            md.append("- `").append(safe(w.targetBase())).append("`로 시작하는 요청은 실제로 **`")
                    .append(safe(accessUrl)).append(safe(w.targetBase())).append("...`** 로 전달됩니다.\n");
            md.append("- 현재: ").append(safe(w.currentBase())).append("\n");
            if (w.changes() != null) {
                for (DeployGuide.Change c : w.changes()) {
                    md.append("- `").append(safe(c.file())).append("` → ")
                            .append(safe(c.action())).append(" (").append(safe(c.why())).append(")\n");
                }
            }
        }
        md.append("\n");

        // 3. 환경변수
        md.append("## 3. 환경변수\n");
        if (g.requiredEnv() == null || g.requiredEnv().isEmpty()) {
            md.append("- (없음)\n");
        } else {
            renderEnvGroup(md, "프론트 빌드 전에 설정", g.requiredEnv().stream()
                    .filter(e -> "BUILD".equals(e.phase())).toList());
            renderEnvGroup(md, "사용자가 배포 화면에서 입력", g.requiredEnv().stream()
                    .filter(e -> "RUNTIME".equals(e.phase()) && !e.providedByPlatform()).toList());
            renderEnvGroup(md, "SSAFYHUB가 자동 설정", g.requiredEnv().stream()
                    .filter(e -> "RUNTIME".equals(e.phase()) && e.providedByPlatform()).toList());
        }
        md.append("\n");

        // 4. 백킹 서비스
        md.append("## 4. 데이터베이스 / 백킹 서비스\n");
        if (g.backingServices() == null || g.backingServices().isEmpty()) {
            md.append("- (불필요)\n");
        } else {
            for (DeployGuide.BackingService b : g.backingServices()) {
                md.append("- **").append(safe(b.serviceType())).append("**: ").append(safe(b.reason()))
                        .append(" (플랫폼이 프로비저닝, 접속정보는 env로 주입)\n");
            }
        }
        md.append("\n");

        // 5. 프레임워크별 확인
        md.append("## 5. 프레임워크별 확인\n");
        if (g.frameworkFixes() == null || g.frameworkFixes().isEmpty()) {
            md.append("- (없음)\n");
        } else {
            for (DeployGuide.FrameworkFix f : g.frameworkFixes()) {
                md.append("- **").append(safe(f.title())).append("**: ").append(safe(f.detail())).append("\n");
            }
        }
        md.append("\n");

        // 6. 배포 전 체크리스트
        md.append("## 6. 배포 전 체크\n");
        md.append("- [ ] 최종 백엔드 요청 경로가 `/api`로 시작하는가\n");
        md.append("- [ ] baseURL과 요청 경로를 합쳤을 때 `/api/api` 중복이 없는가\n");
        md.append("- [ ] OAuth·WebSocket·SSE 경로도 `/api` 아래에 있는가\n");
        md.append("- [ ] 프론트 zip 루트에 index.html이 있는가\n");
        md.append("- [ ] 사용자 입력 환경변수를 모두 준비했는가\n\n");

        // 경고
        if (g.warnings() != null && !g.warnings().isEmpty()) {
            md.append("## ⚠️ 확인 필요\n");
            for (String warn : g.warnings()) {
                md.append("- ").append(safe(warn)).append("\n");
            }
            md.append("\n");
        }

        return md.toString();
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private void renderEnvGroup(StringBuilder md, String title, List<DeployGuide.EnvVar> envVars) {
        if (envVars.isEmpty()) {
            return;
        }
        md.append("\n### ").append(title).append("\n");
        for (DeployGuide.EnvVar env : envVars) {
            md.append("- `").append(safe(env.key())).append("` — ").append(safe(env.reason()));
            md.append(env.required() ? " (필수)" : " (선택)").append("\n");
        }
    }
}

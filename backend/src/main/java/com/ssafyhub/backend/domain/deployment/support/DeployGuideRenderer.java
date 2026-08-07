package com.ssafyhub.backend.domain.deployment.support;

import com.ssafyhub.backend.domain.deployment.dto.DeployGuide;
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
        md.append("- ⚠️ 프론트 zip은 **dist 내용물**을 압축해 `index.html`이 루트에 오게 하세요.\n\n");

        // 2. API 연결
        md.append("## 2. 프론트 → 백엔드 연결\n");
        DeployGuide.ApiWiring w = g.apiWiring();
        if (w != null) {
            md.append("- 목표 API base: **`").append(safe(w.targetBase())).append("`** (상대경로, same-origin)\n");
            md.append("- 프론트가 `").append(safe(w.targetBase())).append("` 로 호출하면 실제로는 **`")
                    .append(safe(accessUrl)).append(safe(w.targetBase())).append("`** 로 요청됩니다.\n");
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
        md.append("## 3. 필수 환경변수 (배포 시 입력)\n");
        if (g.requiredEnv() == null || g.requiredEnv().isEmpty()) {
            md.append("- (없음)\n");
        } else {
            for (DeployGuide.EnvVar e : g.requiredEnv()) {
                md.append("- `").append(safe(e.key())).append("` — ").append(safe(e.reason()));
                md.append(e.required() ? " (필수)" : " (선택)").append("\n");
            }
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
        if (g.frameworkFixes() != null && !g.frameworkFixes().isEmpty()) {
            md.append("## 5. 프레임워크별 확인\n");
            for (DeployGuide.FrameworkFix f : g.frameworkFixes()) {
                md.append("- **").append(safe(f.title())).append("**: ").append(safe(f.detail())).append("\n");
            }
            md.append("\n");
        }

        // 6. 배포 전 체크리스트
        md.append("## 6. 배포 전 체크\n");
        md.append("- [ ] 프론트 API base가 `/api`(상대경로)인가\n");
        md.append("- [ ] 프론트 zip 루트에 index.html이 있는가\n");
        md.append("- [ ] 필수 환경변수를 모두 준비했는가\n\n");

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
}

# 포팅 매뉴얼

이 문서만 보고 처음부터 배포할 수 있도록 작성했다.
**비밀값은 일절 포함하지 않는다** — 키 이름과 용도만 적고, 실제 값은 Jenkins 크리덴셜과
SSM Parameter Store에서 관리한다.

| | |
|---|---|
| 서비스명 | SSAFYHUB |
| 저장소 | `lab.ssafy.com/s15-webmobile1-sub1/S15P11E206` (모노레포) |
| 배포 기준 브랜치 | `main` (보호 브랜치, MR 병합만 허용) |
| 기준 시점 | 2026-07-30 |

## 1. 사용 도구 및 버전

### 백엔드

| 항목 | 버전 |
|---|---|
| Java | 21 (런타임 이미지 `eclipse-temurin:21-jre`) |
| Spring Boot | 3.5.16 |
| Gradle | 프로젝트 내 `gradlew` 래퍼 사용 |
| MySQL | 8 (AWS RDS) |
| springdoc-openapi | 2.8.5 |

주요 스타터: `web`, `validation`, `data-jpa`, `actuator`, `security`
드라이버: `mysql-connector-j`

### 프론트엔드

| 항목 | 버전 |
|---|---|
| Node.js | 20 (Jenkins 도구명 `NodeJS-20`) |
| Vue | 3.5 |
| Vite | 5.4 |
| Pinia | 2.2 |
| Vue Router | 4.4 |
| TanStack Vue Query | 5.59 |
| Axios | 1.7 |
| Tailwind CSS | 3.4 |

### 인프라

| 항목 | 내용 |
|---|---|
| Jenkins | LTS (Docker 컨테이너, 호스트 포트 9090) |
| Docker | 메인 EC2에 설치 |
| k3s | 사용자 앱 실행용 클러스터 (컨트롤 플레인 + E206 워커) |
| OS | Amazon Linux (EC2), 루트 볼륨 30GB |

## 2. 외부 서비스 및 AWS 리소스

### AWS 계정 `349865002344`

| 리소스 | 이름 / 식별자 | 용도 |
|---|---|---|
| ALB | `My-Test-ALB` | TLS 종료, 대상그룹 `My-Spring-TG` |
| EC2 (메인) | `jenkins.ssafyhub.site` | 메인 백엔드 컨테이너 + Jenkins |
| EC2 (컨트롤 플레인) | t3.small, 사설 `172.31.0.241` | `deploy-engine`, k3s 컨트롤 플레인 |
| RDS | MySQL 8 | 메인 백엔드 + `deploy-engine` 공유 |
| CloudFront | `ssafyhub.site` | 프론트 정적 서빙 + `/api/*` 프록시 |
| S3 | `ssafyhub-frontend` | **우리 프론트엔드** 빌드 산출물 |
| S3 | `ssafyhub-artifacts` | **사용자 업로드** 산출물 (`.jar` / `.zip`) |
| S3 | `ssafy-deploy-frontend` | **사용자 프론트엔드** 정적 호스팅 |
| Route 53 | `ssafyhub.site` | DNS |
| ACM | `*.ssafyhub.site` | TLS 인증서 |
| SSM Parameter Store | `/ssafyhub/...` | 백엔드 설정값 (수동 기동 경로) |
| IAM 역할 | `ssafyhub-backend-ec2-role` | EC2에 부착. S3 접근을 액세스 키 없이 처리 |

### 외부 인프라

| 항목 | 내용 |
|---|---|
| E206 | `i15e206.p.ssafy.io`, 공인 `13.124.149.32`, RAM 15GB. SSAFY 제공. 우리 k3s 워커 노드(`e206-worker`) |
| GitLab | `lab.ssafy.com` — 소스 저장소, 웹훅 발신 |
| Mattermost | 배포 성공·실패 알림 수신 |
| n8n | MR 이벤트 기반 AI 코드 리뷰 |

### 도메인

| 도메인 | 대상 |
|---|---|
| `ssafyhub.site` | CloudFront → S3 (`/api/*`는 ALB) |
| `api.ssafyhub.site` | ALB → 메인 백엔드 `:8080` |
| `jenkins.ssafyhub.site` | 메인 EC2 `:9090` |
| `{학번}.ssafyhub.site` | 사용자 배포 서비스 (메인 백엔드가 프록시) |

## 3. 포트

| 포트 | 위치 | 용도 | 노출 |
|---|---|---|---|
| 443 | ALB | 사용자 트래픽 (TLS) | 공개 |
| 8080 | 메인 EC2 | 메인 백엔드 | ALB 경유 |
| 8081 | 컨트롤 플레인 EC2 | `deploy-engine` | 보안그룹 참조 전용 (백엔드 SG → 컨트롤플레인 SG) |
| 9090 | 메인 EC2 | Jenkins | 제한 권장 |
| 50000 | 메인 EC2 | Jenkins 에이전트 | 미사용 |
| 3306 | RDS | MySQL | VPC 내부 |
| 6443 / 8472 / 10250 | 컨트롤 플레인 ↔ E206 | k3s API / VXLAN / kubelet | 상호 |

## 4. 환경변수

**값은 이 문서에 적지 않는다.** 키와 용도만 정리한다.

### 필수 — 없으면 애플리케이션이 기동하지 않는다

`application-prod.yml`에서 기본값 없이 참조하는 값들이다.

| 키 | 용도 |
|---|---|
| `DB_URL` | RDS JDBC URL (`sslMode=REQUIRED` 포함) |
| `DB_USERNAME` | DB 사용자 |
| `DB_PASSWORD` | DB 비밀번호 |
| `JWT_SECRET` | JWT 서명 키. **변경 시 기존 로그인 전부 무효화** |
| `SECRET_ENCRYPTION_KEY` | 환경변수·DB 비밀번호 암호화 키. **변경 시 기존 데이터 복호화 불가** |
| `AWS_S3_BUCKET` | 사용자 산출물 버킷 (`ssafyhub-artifacts`) |
| `DEVOPS_WAS_BASE_URL` | `deploy-engine` 주소. **끝에 슬래시를 붙이지 않는다** |

### 선택 — 기본값이 있다

| 키 | 기본값 | 용도 |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `local` | 프로덕션은 `prod`를 반드시 지정 |
| `SERVER_PORT` | `8080` | |
| `AWS_REGION` | `ap-northeast-2` | |
| `AWS_S3_PRESIGNED_URL_EXPIRATION_MINUTES` | `10` | 업로드 URL 유효 시간 |
| `JPA_DDL_AUTO` | `update` | |
| `JWT_ACCESS_TOKEN_EXPIRATION_MILLIS` | `1800000` (30분) | |
| `JWT_REFRESH_TOKEN_EXPIRATION_MILLIS` | `1209600000` (14일) | |
| `SUPPORTED_RUNTIME_VERSIONS` | `17,21` | 사용자 앱 Java 버전 |
| `DEPLOYMENT_PUBLIC_DOMAIN` | `ssafyhub.site` | 공개 URL 생성 기준 |
| `DEPLOYMENT_OUTBOX_ENABLED` | `true` | Outbox 스케줄러 |
| `DEPLOYMENT_OUTBOX_POLLING_DELAY_MS` | `5000` | 폴링 주기 |
| `DEPLOYMENT_OUTBOX_BATCH_SIZE` | `20` | 배치 크기 |
| `DEVOPS_WAS_CONNECT_TIMEOUT_MS` | `3000` | |
| `DEVOPS_WAS_READ_TIMEOUT_MS` | `5000` | |

**AWS 액세스 키는 주입하지 않는다.** EC2에 IAM 역할이 붙어 있어 SDK가 인스턴스 메타데이터에서
임시 자격증명을 받는다. `AWS_ACCESS_KEY_ID`를 설정하면 SDK 탐색 순서상 환경변수가 먼저 걸려
역할이 무시되므로 오히려 해가 된다.

### 프론트엔드 (빌드 시점에 정적 치환)

| 키 | 값 | 설명 |
|---|---|---|
| `VITE_API_BASE_URL` | **빈 문자열** | 상대경로(same-origin). CloudFront가 `/api/*`를 ALB로 프록시하므로 CORS 불필요 |
| `VITE_USE_MOCKS` | `false` | |

Vite는 **빌드 시점에** 값을 치환한다. 배포 후 S3 파일을 바꿔도 API 주소는 변경되지 않으므로,
반드시 빌드 전에 설정해야 한다.

### Jenkins 크리덴셜

| ID | 유형 | 용도 |
|---|---|---|
| `ssafyhub-db-url` | Secret text | `DB_URL` |
| `ssafyhub-db-credentials` | Username/Password | `DB_USERNAME` / `DB_PASSWORD` |
| `ssafyhub-jwt-secret` | Secret text | `JWT_SECRET` |
| `ssafyhub-secret-encryption-key` | Secret text | `SECRET_ENCRYPTION_KEY` |
| `ssafyhub-aws-region` | Secret text | `AWS_REGION` |
| `ssafyhub-s3-bucket` | Secret text | `AWS_S3_BUCKET` |
| `ssafyhub-devops-was-base-url` | Secret text | `DEVOPS_WAS_BASE_URL` |
| `ssafyhub-aws-deploy-key` | AWS credentials | 프론트엔드 S3 배포 + CloudFront 무효화 |
| `gitlab-token` | — | 저장소 체크아웃 |

## 5. 로컬 실행

### 백엔드

```bash
cd backend
```

MySQL을 컨테이너로 띄운다.

```bash
docker compose up -d
```

`backend/.env`를 만든다. `.env.example`을 복사해 값을 채운다.

```bash
cp .env.example .env
```

`application-local.yml`이 `optional:file:.env[.properties]`를 읽으므로 `.env`만 있으면 된다.
로컬 프로파일은 `security.cookie.secure: false`, Outbox 기본 비활성이다.

```bash
./gradlew bootRun
```

상태 확인.

```bash
curl http://localhost:8080/actuator/health
```

Swagger는 `http://localhost:8080/swagger-ui/index.html`.

### 프론트엔드

```bash
cd frontend
npm ci
npm run dev
```

`vite.config.js`의 프록시가 `/api`를 `http://localhost:8080`으로 넘긴다.

## 6. 배포

### 자동 배포 (정상 경로)

`main`에 병합하면 끝난다.

1. 작업 브랜치 → MR → `main` 병합
2. GitLab 웹훅이 Jenkins에 즉시 전달 (실측 약 20초)
3. 변경 경로에 따라 해당 잡만 배포
4. Mattermost 알림

| 잡 | 웹훅 | 폴링 백업 | 배포 조건 |
|---|---|---|---|
| `ssafyhub-backend` | `/project/ssafyhub-backend` | `pollSCM('H/3 * * * *')` | `main` + `backend/**` 변경 |
| `ssafyhub-frontend` | `/project/ssafyhub-frontend` | 없음 | `main` + `frontend/**` 변경 |

`docs/`만 바꾼 커밋은 어느 쪽도 배포하지 않는다.

### 백엔드 파이프라인 단계

1. **Checkout** — `main` 체크아웃
2. **Build** — `./gradlew clean build` (테스트 포함)
3. **Docker Build** — `ssafyhub-backend:{빌드번호}` 및 `:latest` 태그
4. **Deploy**
   - 구 컨테이너 `stop` → `-previous`로 `rename`
   - 새 컨테이너 `run` (크리덴셜을 환경변수로 주입)
   - 헬스체크 최대 30회 재시도
   - 성공 시 `-previous` 삭제, 현재·직전 이미지만 남기고 정리
   - **실패·중단 시 `trap`이 직전 컨테이너를 복구**
5. **Post** — Mattermost 알림

### 프론트엔드 파이프라인 단계

1. **Checkout**
2. **Build** — `npm ci` → `npm run build` (`VITE_*` 주입)
3. **Deploy**
   - 해시 파일명 에셋 → `aws s3 sync --delete`, 장기 캐시(`max-age=31536000,immutable`)
   - `index.html` → 별도 업로드, **캐시 금지**(`no-cache,no-store,must-revalidate`)
   - CloudFront 전체 무효화(`/*`)
4. **Post** — Mattermost 알림, 워크스페이스 정리

`index.html`의 캐시 금지를 빼면 배포해도 브라우저가 옛 화면을 계속 띄운다.

### 수동 배포 (폴백)

Jenkins를 쓸 수 없을 때만 사용한다. 메인 EC2에 다음이 준비돼 있다.

```bash
cd /home/ec2-user && nohup ./start-backend.sh >> app.log 2>&1 &
```

`start-backend.sh`는 SSM Parameter Store에서 설정을 읽어 `app.jar`을 기동한다.
AWS 자격증명은 IAM 역할에서 자동으로 가져온다.

**주의.** 이 프로세스가 8080을 점유하면 Jenkins의 `docker run`이 포트 충돌로 실패한다.
Jenkins 배포로 되돌릴 때는 먼저 이 프로세스를 종료해야 한다.

## 7. 초기 구축 절차

새 환경에 처음 올릴 때의 순서다.

1. **VPC·보안그룹** — ALB(443 공개), 메인 백엔드 SG, 컨트롤 플레인 SG.
   컨트롤 플레인 SG의 8081은 **메인 백엔드 SG를 참조해서만** 허용한다.
2. **RDS** — MySQL 8 생성. 스키마는 `JPA_DDL_AUTO=update`가 엔티티에서 생성한다.
3. **S3 버킷 3개** — `ssafyhub-frontend`(정적 웹), `ssafyhub-artifacts`(사용자 업로드),
   `ssafy-deploy-frontend`(사용자 프론트)
4. **IAM 역할** — `ssafyhub-backend-ec2-role`에 `ssafyhub-artifacts` 접근 권한.
   EC2에 부착하고 **IMDS `HttpPutResponseHopLimit`을 `2`로 설정**한다.
   `1`이면 브리지 네트워크의 컨테이너가 메타데이터에 도달하지 못한다.
5. **ACM 인증서** + **Route 53** 레코드
6. **CloudFront** — 기본 오리진 S3, `/api/*` behavior는 ALB
7. **Jenkins** — Multibranch Pipeline 2개 생성.
   Script Path는 각각 `backend/Jenkinsfile`, `frontend/Jenkinsfile`.
   Branch Sources에 **`Filter by name` → `main`** 을 넣어 배포 대상을 제한한다.
8. **Jenkins 크리덴셜** 등록 (4절 표)
9. **GitLab 웹훅 2개** — 잡별로 하나씩. Push events, Secret token 없음
   (Jenkins의 `useAuthenticatedEndpoint=false`)
10. **Mattermost** 연동 설정 (Jenkins 전역 설정)
11. **k3s** — 컨트롤 플레인 기동 후 E206을 워커로 조인
12. **`deploy-engine`** — 컨트롤 플레인 EC2에 systemd 서비스로 등록

## 8. 시연 시나리오

1. `https://ssafyhub.site` 접속
2. **회원가입** — 팀명 / 이름 / 학번(숫자 7자리) / 비밀번호(8자 이상)
3. **로그인**
4. **배포 요청**
   - 백엔드 `.jar`, 프론트엔드 `.zip` 업로드 (Presigned URL로 S3에 직접 전송)
   - 프로젝트명, Java 버전(17 또는 21), DB 이름·비밀번호, 환경변수(1~20개) 입력
   - 공개 여부 선택
5. **진행 상황 확인** — 상태가 `배포 대기 중` → `이미지 빌드 중` → `배포 환경에 적용 중` → `정상 기동`
   으로 바뀐다. 진행 로그와 CPU·메모리 사용량(현재값·시계열 그래프)을 확인
6. **배포된 서비스 접속** — `https://{학번}.ssafyhub.site`
7. **쇼케이스** — 공개로 설정한 서비스가 목록에 노출되는지 확인
8. **배포 삭제**

### 시연 전 점검

```bash
curl -fsS https://api.ssafyhub.site/actuator/health
```

```bash
docker ps --format '{{.Names}}\t{{.Status}}'
```

## 9. 알려진 제약

| 항목 | 내용 |
|---|---|
| 사용자당 배포 1개 | `uk_deployments_member_id` 제약. 새로 배포하려면 기존 배포를 삭제해야 한다 |
| Presigned URL 10분 | 발급 후 지체하면 만료된다 |
| 확장자 고정 | 백엔드는 `.jar`, 프론트엔드는 `.zip`만 허용 |
| 두 파일 동시 필수 | Presigned URL 발급 시 백엔드·프론트엔드 파일명을 모두 보내야 한다 |
| Outbox 재시도 없음 | 전송 실패 시 `FAILED`로 종결. 개선 과제 |
| 미구현 엔드포인트 | `GET /api/v1/admin/deployments` (프론트엔드가 호출하지만 백엔드에 없음) |

## 10. 관련 문서

| 문서 | 내용 |
|---|---|
| [아키텍처](./architecture.md) | 시스템 구성도, 접속 경로, 배포 시퀀스, CI/CD, 네트워크 |
| [API 명세서](./api-spec.md) | 엔드포인트 14개, 에러 코드 30개 |
| [ERD](./erd.md) | 테이블 7개, 제약·인덱스, 설계 결정 |
| [운영 런북](./runbook.md) | 장애 대응 절차 |
| [CI/CD 장애 기록](./cicd-incident-2026-07-29.md) | 2026-07-29 장애 원인과 조치 |
| `backend/README.md` | 백엔드 개발 가이드, 브랜치 전략, 협업 규칙 |

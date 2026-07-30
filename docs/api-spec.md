# API 명세서

**출처.** 이 문서는 프로덕션에 배포된 OpenAPI 스펙(`ssafyhub-backend:13`, `GET /v3/api-docs`)과
소스 코드에서 직접 추출했다. 손으로 옮겨 적은 값이 아니므로 구현과 일치한다.

| | |
|---|---|
| Base URL (외부) | `https://ssafyhub.site` — CloudFront `/api/*` behavior가 ALB로 프록시 |
| Base URL (직접) | `https://api.ssafyhub.site` |
| Swagger UI | `/swagger-ui/index.html` |
| OpenAPI JSON | `/v3/api-docs` |
| 엔드포인트 수 | 14 |
| 기준 시점 | 2026-07-30 |

CloudFront가 같은 오리진으로 묶어주므로 프론트엔드는 상대경로(`/api/v1/...`)로 호출하며
CORS 설정이 필요 없다.

## 공통 규약

### 응답 형식

성공과 실패 모두 동일한 봉투(envelope)를 쓴다.

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "요청이 정상적으로 처리되었습니다.",
  "data": { }
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `success` | boolean | 처리 성공 여부 |
| `code` | string | 성공 시 `SUCCESS`, 실패 시 도메인별 에러 코드 |
| `message` | string | 응답 메시지 |
| `data` | object | 응답 본문. 없으면 `null` |

### 인증

쿠키 기반이다. 응답 헤더로 내려주며 클라이언트가 직접 다루지 않는다.

| 스킴 | 전달 방식 | 대상 |
|---|---|---|
| `accessTokenCookieAuth` | 액세스 토큰 쿠키 | 인증이 필요한 모든 엔드포인트 |
| `refreshTokenCookieAuth` | 리프레시 토큰 쿠키 | `POST /api/v1/auth/reissue` |
| `csrfTokenAuth` | `XSRF-TOKEN` 쿠키 + `X-XSRF-TOKEN` 헤더 | 상태를 변경하는 모든 요청 |

프로덕션은 `security.cookie.secure: true`, 로컬은 `false`다. **CSRF 관련 문제는 로컬에서
재현되지 않을 수 있다.**

### CSRF 사용 절차

1. `GET /api/v1/auth/csrf` 호출 → `XSRF-TOKEN` 쿠키 수신 (`HttpOnly=false`, 자바스크립트에서 읽을 수 있음)
2. 상태 변경 요청 시 그 값을 `X-XSRF-TOKEN` 헤더에 담아 전송
3. 누락·불일치 시 `403 CSRF_001`

프론트엔드 인터셉터는 `403 CSRF_001`을 만나면 토큰을 강제 재발급하고 원래 요청을 1회 재시도한다.

## 엔드포인트 요약

`인증`은 액세스 토큰 쿠키, `CSRF`는 `X-XSRF-TOKEN` 헤더 필요 여부다.

| API | HTTP | 인증 | CSRF | 호출방향 | 설명 |
|---|---|:---:|:---:|---|---|
| `/api/v1/auth/csrf` | GET | — | — | Frontend → Backend | CSRF 토큰 발급 |
| `/api/v1/auth/login` | POST | — | ✓ | Frontend → Backend | 로그인 |
| `/api/v1/auth/reissue` | POST | 리프레시 | ✓ | Frontend → Backend | 액세스 토큰 재발급 |
| `/api/v1/members/signup` | POST | — | ✓ | Frontend → Backend | 회원가입 |
| `/api/v1/artifacts/presigned-urls` | POST | ✓ | ✓ | Frontend → Backend | 산출물 업로드 URL 발급 |
| `/api/v1/deployments` | POST | ✓ | ✓ | Frontend → Backend | 배포 생성 |
| `/api/v1/deployments` | GET | ✓ | — | Frontend → Backend | 내 배포 상태 조회 (**단건**) |
| `/api/v1/deployments/{deploymentId}` | DELETE | ✓ | ✓ | Frontend → Backend | 배포 삭제 |
| `/api/v1/deployments/{deploymentId}/logs` | GET | ✓ | — | Frontend → Backend | 배포 진행 로그 조회 |
| `/api/v1/deployments/{deploymentId}/resources` | GET | ✓ | — | Frontend → Backend | 리소스 사용량 조회 (현재값) |
| `/api/v1/deployments/{deploymentId}/resources/history` | GET | ✓ | — | Frontend → Backend | 리소스 사용량 시계열 조회 |
| `/api/v1/showcase/public` | GET | — | — | Frontend → Backend | 공개 서비스 목록 조회 |
| `/api/v1/showcase/{id}` | GET | — | — | Frontend → Backend | 공개 서비스 상세 조회 |
| `/api/v1/showcase/{id}` | PATCH | ✓ | ✓ | *미사용* | 서비스 정보 수정 |

### 백엔드 외부 호출

프론트엔드 → 백엔드 외에 두 방향이 더 있다.

| 호출방향 | 요청 | 설명 |
|---|---|---|
| Backend → deploy-engine | `POST /api/deployments` | Outbox 스케줄러가 5초 주기로 배포 요청 전달. 사설 IP `172.31.0.241:8081` |
| Browser → S3 | `PUT {presignedUrl}` | 산출물을 백엔드를 경유하지 않고 S3에 직접 업로드 |

## 인증 (Auth)

### `GET /api/v1/auth/csrf`

CSRF 토큰을 발급한다. 인증 불필요.

- 응답 `200` — `data`는 `null`. 토큰은 `XSRF-TOKEN` 쿠키로 전달된다.

### `POST /api/v1/auth/login`

**요청**

| 필드 | 타입 | 필수 | 설명 |
|---|---|:---:|---|
| `studentNumber` | string | ✓ | 학번 (예 `1234567`) |
| `password` | string | ✓ | 비밀번호 |

**응답 `200`** — `data.member`

| 필드 | 타입 | 설명 |
|---|---|---|
| `memberId` | integer | 회원 ID |
| `name` | string | 이름 |
| `studentNumber` | string | 학번 |
| `teamName` | string | 팀명 |
| `role` | string | 권한 (예 `MEMBER`) |

액세스·리프레시 토큰은 쿠키로 내려간다.

**에러** `400 COMMON_001` · `401 AUTH_001`

### `POST /api/v1/auth/reissue`

리프레시 토큰 쿠키로 액세스 토큰을 재발급한다. 요청 본문 없음.

**에러** `401 AUTH_004` (없음) · `AUTH_005` (유효하지 않음) · `AUTH_006` (만료) · `AUTH_007` (폐기)

## 회원 (Member)

### `POST /api/v1/members/signup`

| 필드 | 타입 | 필수 | 제약 |
|---|---|:---:|---|
| `teamName` | string | ✓ | 팀명 |
| `name` | string | ✓ | 이름 |
| `studentNumber` | string | ✓ | **숫자 7자리** |
| `password` | string | ✓ | 8~100자, 최대 72바이트 |
| `passwordConfirm` | string | ✓ | `password`와 일치해야 함 |

**응답 `201`** — `id`, `studentNumber`, `name`, `teamName`, `createdAt`

**에러** `400 COMMON_001` · `400 MEMBER_002` (비밀번호 확인 불일치) ·
`400 MEMBER_003` (비밀번호 72바이트 초과) · `409 MEMBER_001` (학번 중복)

## 산출물 (Artifact)

### `POST /api/v1/artifacts/presigned-urls`

S3 업로드용 Presigned URL을 발급한다. **백엔드·프론트엔드 파일명을 둘 다 보내야 한다.**

| 필드 | 타입 | 필수 | 제약 |
|---|---|:---:|---|
| `backendFileName` | string | ✓ | `.jar` 확장자, 255자 이하, `/` `\` 불가 |
| `frontendFileName` | string | ✓ | `.zip` 확장자, 255자 이하, `/` `\` 불가 |

**응답 `200`**

| 필드 | 타입 | 설명 |
|---|---|---|
| `backendArtifact.artifactId` | integer | 발급된 아티팩트 ID |
| `backendArtifact.uploadUrl` | string | 이 URL로 파일을 직접 `PUT` |
| `frontendArtifact.*` | | 위와 동일 |
| `expiresAt` | string | URL 만료 시각 (**발급 후 10분**) |

**에러** `400 COMMON_001` · `400 ARTIFACT_001` (jar 아님) · `400 ARTIFACT_002` (zip 아님) ·
`400 ARTIFACT_003` (파일명 부적합) · `404 MEMBER_004` · `500 ARTIFACT_004` (URL 발급 실패)

> **주의.** Presigned URL 발급은 S3를 호출하지 않고 로컬에서 서명만 계산한다.
> 자격증명이 잘못돼도 `200`이 나오고, 실패는 브라우저의 `PUT` 단계에서 `403`으로 드러난다.
> 업로드 기능을 검증할 때는 발급 응답만 보지 말고 실제 `PUT`까지 확인해야 한다.

## 배포 (Deployment)

### `POST /api/v1/deployments`

업로드된 산출물로 배포를 요청한다. 접수만 하고 실제 배포는 비동기로 진행된다(`202`).

| 필드 | 타입 | 필수 | 제약 |
|---|---|:---:|---|
| `projectName` | string | ✓ | 프로젝트 이름 |
| `backendArtifactId` | integer | ✓ | 사전 발급된 백엔드 아티팩트 ID |
| `frontendArtifactId` | integer | ✓ | 사전 발급된 프론트엔드 아티팩트 ID |
| `runtimeVersion` | string | ✓ | Java 버전. 지원: `17`, `21` |
| `internalPort` | integer | | 애플리케이션 내부 포트 (기본 `8080`) |
| `databaseName` | string | ✓ | 영문 시작, 영문/숫자/언더스코어 |
| `databasePassword` | string | ✓ | DB 비밀번호 |
| `env` | array | ✓ | 환경변수 **1~20개** |
| `env[].key` | string | ✓ | 영문 **대문자**로 시작, 대문자/숫자/언더스코어 |
| `env[].value` | string | ✓ | 값 |
| `visibility` | string | | `PUBLIC` / `PRIVATE`. 미입력 시 기본값 |
| `description` | string | | 서비스 설명 |

**응답 `202`** — `deploymentId`, `status`, `displayStatus`, `expectedUrl`, `requestedAt`

**에러**

| 코드 | 상태 | 의미 |
|---|---|---|
| `COMMON_001` | 400 | 요청 값 오류 |
| `DEPLOYMENT_002` | 400 | 지원하지 않는 Java 버전 |
| `DEPLOYMENT_003` | 400 | 중복된 환경변수 이름 |
| `ARTIFACT_007` | 400 | 아티팩트 유형 불일치 |
| `ARTIFACT_009` | 400 | S3에 파일이 없음 (업로드 미완료) |
| `ARTIFACT_006` | 403 | 본인이 발급받지 않은 아티팩트 |
| `MEMBER_004` | 404 | 회원 없음 |
| `ARTIFACT_005` | 404 | 아티팩트 정보 없음 |
| `DEPLOYMENT_001` | 409 | **사용자당 배포는 하나만** 생성 가능 |
| `ARTIFACT_008` | 409 | 이미 사용된 아티팩트 |
| `ARTIFACT_010` | 503 | S3 확인 실패, 재시도 필요 |

### `GET /api/v1/deployments`

**목록이 아니라 단건이다.** 사용자당 배포가 하나이므로 로그인한 회원의 배포 상태를 반환한다.

| 필드 | 타입 | 설명 |
|---|---|---|
| `deploymentId` | integer | 배포 ID |
| `projectName` | string | 프로젝트 이름 |
| `description` | string | 서비스 설명 (없으면 `null`) |
| `visibility` | string | `PUBLIC` / `PRIVATE` |
| `status` | string | 현재 배포 상태 |
| `displayStatus` | string | 화면 표시용 상태 문구 (예 `정상 기동`) |
| `endpointUrl` | string | 배포된 서비스 접속 URL |
| `statusMessage` | string | 실패 사유 또는 상태 메시지 |
| `updatedAt` | string | 상태 마지막 변경 시각 |

**에러** `404 DEPLOYMENT_004`

### `DELETE /api/v1/deployments/{deploymentId}`

**에러** `404 DEPLOYMENT_004` · `502 DEPLOYMENT_005` (배포 리소스 삭제 실패, 재시도 필요)

### `GET /api/v1/deployments/{deploymentId}/logs`

배포 진행 로그를 시간순으로 반환한다. `data`는 배열.

| 필드 | 타입 | 설명 |
|---|---|---|
| `logMessage` | string | 로그 메시지 |
| `status` | string | 그 시점의 배포 상태 |
| `createdAt` | string | 기록 시각 |

**에러** `404 DEPLOYMENT_004`

### `GET /api/v1/deployments/{deploymentId}/resources`

현재 리소스 사용량 스냅샷.

| 필드 | 타입 | 설명 |
|---|---|---|
| `id` | integer | 배포 ID |
| `cpuPercent` | integer | CPU 사용률(%) — 코어 사용량 기준 |
| `memoryUsedMb` | integer | 사용 메모리(MB) |
| `memoryTotalMb` | integer | 메모리 상한(MB) |
| `sampledAt` | string | 샘플 시각 |

**에러** `404 DEPLOYMENT_004`

### `GET /api/v1/deployments/{deploymentId}/resources/history`

**쿼리 파라미터** `minutes` (조회 구간, 분)

| 필드 | 타입 | 설명 |
|---|---|---|
| `cpu` | array&lt;SeriesPoint&gt; | CPU 사용률(%) 시계열 |
| `memory` | array&lt;SeriesPoint&gt; | 메모리 사용량(MB) 시계열 |
| `SeriesPoint.time` | integer | 유닉스 시각(초) |
| `SeriesPoint.value` | number | 값 |

**에러** `404 DEPLOYMENT_004`

## 쇼케이스 (Showcase)

### `GET /api/v1/showcase/public`

공개된 서비스 목록. 인증 불필요.

**쿼리 파라미터** `page` (0부터), `size`

| 필드 | 타입 | 설명 |
|---|---|---|
| `deployments` | array | 공개 서비스 목록 |
| `deployments[]` | | `deploymentId`, `teamName`, `projectName`, `description`, `endpointUrl`, `updatedAt` |
| `page` | integer | 현재 페이지 번호 |
| `size` | integer | 페이지 크기 |
| `totalElements` | integer | 전체 항목 수 |

### `GET /api/v1/showcase/{id}`

공개 서비스 상세. 인증 불필요.

**응답** `deploymentId`, `teamName`, `projectName`, `description`, `endpointUrl`, `updatedAt`

**에러** `404 SHOWCASE_001`

### `PATCH /api/v1/showcase/{id}`

소유자가 공개 여부와 설명을 수정한다. **현재 프론트엔드에서 호출하지 않는다.**

| 필드 | 타입 | 필수 | 설명 |
|---|---|:---:|---|
| `visibility` | string | | `null`이면 변경하지 않음 |
| `description` | string | | `null`이면 변경하지 않음 |

**에러** `404 SHOWCASE_001`

## 에러 코드 전체

| 코드 | 상태 | 메시지 |
|---|---|---|
| `COMMON_001` | 400 | 요청 값이 올바르지 않습니다. |
| `COMMON_002` | 500 | 서버 내부 오류가 발생했습니다. |
| `MEMBER_001` | 409 | 이미 사용 중인 학번입니다. |
| `MEMBER_002` | 400 | 비밀번호와 비밀번호 확인이 일치하지 않습니다. |
| `MEMBER_003` | 400 | 비밀번호는 최대 72바이트까지 입력할 수 있습니다. |
| `MEMBER_004` | 404 | 회원을 찾을 수 없습니다. |
| `AUTH_001` | 401 | 학번 또는 비밀번호가 올바르지 않습니다. |
| `AUTH_002` | 401 | 유효하지 않은 토큰입니다. |
| `AUTH_003` | 401 | 만료된 토큰입니다. |
| `AUTH_004` | 401 | Refresh Token이 존재하지 않습니다. |
| `AUTH_005` | 401 | 유효하지 않은 Refresh Token입니다. |
| `AUTH_006` | 401 | 만료된 Refresh Token입니다. |
| `AUTH_007` | 401 | 폐기된 Refresh Token입니다. |
| `CSRF_001` | 403 | CSRF 토큰이 없거나 유효하지 않습니다. |
| `ARTIFACT_001` | 400 | 백엔드 파일은 .jar 형식이어야 합니다. |
| `ARTIFACT_002` | 400 | 프론트엔드 파일은 .zip 형식이어야 합니다. |
| `ARTIFACT_003` | 400 | 올바르지 않은 파일 이름입니다. |
| `ARTIFACT_004` | 500 | 업로드 URL 발급에 실패했습니다. |
| `ARTIFACT_005` | 404 | 업로드 파일 정보를 찾을 수 없습니다. |
| `ARTIFACT_006` | 403 | 본인이 발급받은 파일만 사용할 수 있습니다. |
| `ARTIFACT_007` | 400 | 업로드 파일 유형이 올바르지 않습니다. |
| `ARTIFACT_008` | 409 | 이미 사용된 업로드 파일입니다. |
| `ARTIFACT_009` | 400 | S3에 업로드된 파일이 존재하지 않습니다. |
| `ARTIFACT_010` | 503 | 업로드 파일을 확인할 수 없습니다. 잠시 후 다시 시도해주세요. |
| `DEPLOYMENT_001` | 409 | 사용자별 배포는 하나만 생성할 수 있습니다. |
| `DEPLOYMENT_002` | 400 | 지원하지 않는 Java 실행 버전입니다. |
| `DEPLOYMENT_003` | 400 | 중복된 환경변수 이름이 존재합니다. |
| `DEPLOYMENT_004` | 404 | 조회할 배포 정보가 없습니다. |
| `DEPLOYMENT_005` | 502 | 배포 리소스 삭제에 실패했습니다. 잠시 후 다시 시도해 주세요. |
| `SHOWCASE_001` | 404 | 존재하지 않는 서비스입니다. |

## 기존 노션 명세서와의 차이

노션 표(7행)와 실제 구현(14개)을 대조한 결과다. **노션 쪽을 수정해야 한다.**

### 경로·설명이 다른 것

| 노션 | 실제 | 비고 |
|---|---|---|
| `POST /api/v1/auth/signup` | `POST /api/v1/members/signup` | **경로 다름.** `auth`가 아니라 `members` |
| `GET /api/v1/auth/student-numbers/{studentNumber}/availability` | 없음 | **구현되지 않음.** 학번 중복은 회원가입 시 `409 MEMBER_001`로 처리 |
| `GET /api/v1/members/me` | 없음 | **구현되지 않음.** 로그인 응답의 `data.member`로 회원 정보를 받는다 |
| `GET /api/v1/deployments` → "내 배포 목록 조회" | "내 배포 상태 조회" | **목록이 아니라 단건.** 사용자당 배포 1개 제약(`DEPLOYMENT_001`) |
| `/api/v1/auth/login` (HTTP 미기재) | `POST` | 메서드 누락 |

### 노션에 빠진 엔드포인트 9개

`GET /api/v1/auth/csrf` · `POST /api/v1/auth/reissue` ·
`DELETE /api/v1/deployments/{deploymentId}` · `GET /api/v1/deployments/{deploymentId}/logs` ·
`GET /api/v1/deployments/{deploymentId}/resources` ·
`GET /api/v1/deployments/{deploymentId}/resources/history` ·
`GET /api/v1/showcase/public` · `GET /api/v1/showcase/{id}` · `PATCH /api/v1/showcase/{id}`

## 정합성 점검 결과

프론트엔드 호출과 백엔드 구현을 대조했다.

| 구분 | 대상 | 조치 |
|---|---|---|
| 프론트엔드가 호출하지만 **백엔드에 없음** | `GET /api/v1/admin/deployments` (`frontend/src/api/deployments.js:16`) | 관리자 배포 목록. 백엔드 미구현. 코드 주석에도 "경로만 예약"으로 적혀 있다 |
| 백엔드에 있지만 **프론트엔드가 호출하지 않음** | `PATCH /api/v1/showcase/{id}` | 공개 여부·설명 수정. 화면 연동 필요 여부 확인 |

### 스펙 문서 자체의 결함

`GET /api/v1/auth/csrf`와 `POST /api/v1/auth/reissue`의 OpenAPI 스펙에
**`csrfToken`이 필수 쿼리 파라미터로 노출된다.** 실제로는 Spring Security가 주입하는
메서드 파라미터(`CsrfToken csrfToken`)이며 클라이언트가 보내는 값이 아니다.
springdoc이 이를 구분하지 못해 생긴 오해다.

Swagger를 보고 API를 붙이는 사람이 잘못된 요청을 만들 수 있으므로 숨기는 것이 좋다.

```java
// AuthController
public ResponseEntity<ApiResponse<Void>> csrf(
        @Parameter(hidden = true) CsrfToken csrfToken) {
```

## 문서 갱신 방법

이 문서는 배포된 OpenAPI 스펙에서 추출했다. API가 바뀌면 아래로 최신 스펙을 받아
변경분을 반영한다.

```bash
curl -s https://api.ssafyhub.site/v3/api-docs -o apidocs.json
```

Swagger UI(`/swagger-ui/index.html`)는 항상 배포된 코드를 반영하므로,
세부 스키마는 그쪽이 최종 근거다. 이 문서는 사람이 읽기 위한 요약과
프론트엔드 정합성 점검 결과를 담는 역할을 한다.

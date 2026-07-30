# SSAFYHUB Backend API 명세서

> **Version** `v1` · **Base URL** `http://api.ssafyhub.site`

인증은 로그인 시 발급되는 `accessToken` 쿠키로 이루어집니다.
`POST` / `PATCH` / `DELETE` 요청은 사전에 발급받은 CSRF 토큰을 `X-XSRF-TOKEN` 헤더에 담아 함께 보내야 합니다.
CSRF 토큰은 `GET /api/v1/auth/csrf` 로 발급받습니다.

---

## 목차

1. [공통 규격](#1-공통-규격)
2. [인증 흐름](#2-인증-흐름)
3. [Auth — 인증/인가](#3-auth--인증인가)
4. [Member — 회원 관리](#4-member--회원-관리)
5. [Artifact — 배포 산출물 업로드](#5-artifact--배포-산출물-업로드)
6. [Deployment — 배포](#6-deployment--배포)
7. [Showcase — 공개 서비스](#7-showcase--공개-서비스)
8. [Enum 정리](#8-enum-정리)
9. [에러 코드 전체 목록](#9-에러-코드-전체-목록)

---

## 1. 공통 규격

### 1.1 공통 응답 포맷

모든 응답은 아래 형태로 감싸져 내려옵니다.

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "요청이 정상적으로 처리되었습니다.",
  "data": {}
}
```

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `success` | boolean | 요청 처리 성공 여부 |
| `code` | string | 성공 시 `SUCCESS`, 실패 시 도메인별 에러 코드 |
| `message` | string | 응답 메시지 |
| `data` | object \| array \| null | 응답 데이터. 없을 경우 `null` |

실패 응답 예시:

```json
{
  "success": false,
  "code": "COMMON_001",
  "message": "요청 값이 올바르지 않습니다.",
  "data": null
}
```

### 1.2 인증 방식 (Security Scheme)

| 이름 | 위치 | 키 | 설명 |
| --- | --- | --- | --- |
| `accessTokenCookieAuth` | Cookie | `accessToken` | 로그인 시 발급. 인증이 필요한 API에 사용 |
| `refreshTokenCookieAuth` | Cookie | `refreshToken` | 토큰 재발급 / 로그아웃에 사용 |
| `csrfTokenAuth` | Header | `X-XSRF-TOKEN` | 상태를 변경하는 모든 요청에 필요 |

### 1.3 API 한눈에 보기

| # | Method | Path | 설명 | 인증 | CSRF |
| --- | --- | --- | --- | :---: | :---: |
| 1 | `GET` | `/api/v1/auth/csrf` | CSRF 토큰 발급 | – | – |
| 2 | `POST` | `/api/v1/auth/login` | 로그인 | – | ✅ |
| 3 | `POST` | `/api/v1/auth/reissue` | 토큰 재발급 | refresh | ✅ |
| 4 | `POST` | `/api/v1/auth/logout` | 로그아웃 | refresh | ✅ |
| 5 | `POST` | `/api/v1/members/signup` | 회원가입 | – | ✅ |
| 6 | `POST` | `/api/v1/artifacts/presigned-urls` | 업로드용 Presigned URL 발급 | ✅ | ✅ |
| 7 | `POST` | `/api/v1/deployments` | 배포 생성 | ✅ | ✅ |
| 8 | `GET` | `/api/v1/deployments` | 내 배포 상태 조회 | ✅ | – |
| 9 | `DELETE` | `/api/v1/deployments/{deploymentId}` | 배포 삭제 | ✅ | ✅ |
| 10 | `GET` | `/api/v1/deployments/{deploymentId}/logs` | 배포 진행 로그 조회 | ✅ | – |
| 11 | `GET` | `/api/v1/deployments/{deploymentId}/resources` | 리소스 사용량 조회 | ✅ | – |
| 12 | `GET` | `/api/v1/deployments/{deploymentId}/resources/history` | 리소스 사용량 시계열 조회 | ✅ | – |
| 13 | `GET` | `/api/v1/showcase/public` | 공개 서비스 목록 조회 | – | – |
| 14 | `GET` | `/api/v1/showcase/{id}` | 공개 서비스 상세 조회 | – | – |
| 15 | `PATCH` | `/api/v1/showcase/{id}` | 서비스 정보 수정 | ✅ | ✅ |

---

## 2. 인증 흐름

```
1) GET  /api/v1/auth/csrf              → Set-Cookie: XSRF-TOKEN=...
2) POST /api/v1/auth/login             → Set-Cookie: accessToken, refreshToken
   (헤더에 X-XSRF-TOKEN: {XSRF-TOKEN 쿠키 값})
3) 이후 요청은 accessToken 쿠키 + (변경 요청이면) X-XSRF-TOKEN 헤더 동봉
4) accessToken 만료 시 POST /api/v1/auth/reissue 로 재발급
5) POST /api/v1/auth/logout            → refreshToken 폐기 + 쿠키 삭제
```

프론트엔드에서는 모든 요청에 쿠키가 함께 전송되도록 설정해야 합니다.

```js
// axios
axios.defaults.withCredentials = true;
axios.defaults.xsrfCookieName = "XSRF-TOKEN";
axios.defaults.xsrfHeaderName = "X-XSRF-TOKEN";

// fetch
fetch(url, { credentials: "include" });
```

---

## 3. Auth — 인증/인가

### 3.1 CSRF 토큰 발급

```http
GET /api/v1/auth/csrf
```

상태를 변경하는 요청(`POST` / `PATCH` / `DELETE`) 전에 호출합니다.
응답 `Set-Cookie` 로 `XSRF-TOKEN` 쿠키가 내려가며, 이후 요청 시 그 값을 `X-XSRF-TOKEN` 헤더에 담아 보내야 합니다.

- **인증** 불필요

> 명세서상 `csrfToken` 이라는 query parameter가 표기되어 있으나, 이는 Spring Security의 `CsrfToken` 이 컨트롤러 파라미터로 주입되면서 자동 생성된 항목입니다. **클라이언트가 직접 보낼 필요는 없습니다.**

**응답** `200 OK`

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "요청이 정상적으로 처리되었습니다.",
  "data": null
}
```

---

### 3.2 로그인

```http
POST /api/v1/auth/login
```

학번 / 비밀번호로 로그인합니다. 응답 `Set-Cookie` 로 `accessToken` / `refreshToken` 쿠키가 내려갑니다.

- **인증** 불필요 · **CSRF** 필요

**Request Body** — `LoginRequest`

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | :---: | --- |
| `studentNumber` | string | ✅ | 학번 |
| `password` | string | ✅ | 비밀번호 |

```json
{
  "studentNumber": "1234567",
  "password": "password123"
}
```

**응답** `200 OK` — `LoginResponse`

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "요청이 정상적으로 처리되었습니다.",
  "data": {
    "member": {
      "memberId": 1,
      "name": "홍길동",
      "studentNumber": "1234567",
      "teamName": "SSAFYHUB",
      "role": "MEMBER"
    }
  }
}
```

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `member.memberId` | number(int64) | 회원 ID |
| `member.name` | string | 이름 |
| `member.studentNumber` | string | 학번 |
| `member.teamName` | string | 팀명 |
| `member.role` | string | 권한 (예: `MEMBER`) |

**에러**

| Status | Code | Message |
| --- | --- | --- |
| 400 | `COMMON_001` | 요청 값이 올바르지 않습니다. |
| 401 | `AUTH_001` | 학번 또는 비밀번호가 올바르지 않습니다. |

---

### 3.3 토큰 재발급

```http
POST /api/v1/auth/reissue
```

`refreshToken` 쿠키를 검증해 `accessToken` / `refreshToken` 을 재발급합니다.

- **인증** `refreshToken` 쿠키 · **CSRF** 필요
- **Request Body** 없음

**응답** `200 OK` — `data: null`

**에러**

| Status | Code | Message |
| --- | --- | --- |
| 401 | `AUTH_004` | Refresh Token이 존재하지 않습니다. |
| 401 | `AUTH_005` | 유효하지 않은 Refresh Token입니다. |
| 401 | `AUTH_006` | 만료된 Refresh Token입니다. |
| 401 | `AUTH_007` | 폐기된 Refresh Token입니다. |

---

### 3.4 로그아웃

```http
POST /api/v1/auth/logout
```

`refreshToken` 을 폐기하고 `accessToken` / `refreshToken` 쿠키를 삭제합니다.

- **인증** `refreshToken` 쿠키 · **CSRF** 필요
- **Request Body** 없음

⚠️ 쿠키가 없거나 이미 만료·폐기된 상태에서도 **항상 성공(200)** 으로 응답합니다.
`accessToken` 은 stateless JWT이므로 남은 유효기간 동안 토큰 자체는 무효화되지 않습니다.

**응답** `200 OK` — `data: null`

---

## 4. Member — 회원 관리

### 4.1 회원가입

```http
POST /api/v1/members/signup
```

학번, 이름, 팀명, 비밀번호로 신규 회원을 등록합니다.

- **인증** 불필요 · **CSRF** 필요

**Request Body** — `SignupRequest`

| 필드 | 타입 | 필수 | 제약 | 설명 |
| --- | --- | :---: | --- | --- |
| `studentNumber` | string | ✅ | `^\d{7}$` | 학번 (숫자 7자리) |
| `name` | string | ✅ | 최대 50자 | 이름 |
| `teamName` | string | ✅ | 최대 50자 | 팀명 |
| `password` | string | ✅ | 8~100자, 최대 72바이트 | 비밀번호 |
| `passwordConfirm` | string | ✅ | 8~100자 | 비밀번호 확인 |

```json
{
  "studentNumber": "1234567",
  "name": "홍길동",
  "teamName": "SSAFYHUB",
  "password": "password123",
  "passwordConfirm": "password123"
}
```

**응답** `201 Created` — `MemberResponse`

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "요청이 정상적으로 처리되었습니다.",
  "data": {
    "id": 1,
    "studentNumber": "1234567",
    "name": "홍길동",
    "teamName": "SSAFYHUB",
    "createdAt": "2026-07-28T12:00:00"
  }
}
```

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id` | number(int64) | 회원 ID |
| `studentNumber` | string | 학번 |
| `name` | string | 이름 |
| `teamName` | string | 팀명 |
| `createdAt` | string(date-time) | 가입 일시 |

**에러**

| Status | Code | Message |
| --- | --- | --- |
| 400 | `COMMON_001` | 요청 값이 올바르지 않습니다. |
| 400 | `MEMBER_002` | 비밀번호와 비밀번호 확인이 일치하지 않습니다. |
| 400 | `MEMBER_003` | 비밀번호는 최대 72바이트까지 입력할 수 있습니다. |
| 409 | `MEMBER_001` | 이미 사용 중인 학번입니다. |

---

## 5. Artifact — 배포 산출물 업로드

### 5.1 산출물 업로드용 Presigned URL 발급

```http
POST /api/v1/artifacts/presigned-urls
```

백엔드(`.jar`) / 프론트엔드(`.zip`) 산출물을 S3에 직접 업로드할 수 있는 Presigned URL을 발급합니다.

- **인증** 필요 · **CSRF** 필요

**Request Body** — `PresignedUrlRequest`

| 필드 | 타입 | 필수 | 제약 | 설명 |
| --- | --- | :---: | --- | --- |
| `backendFileName` | string | ✅ | 최대 255자, `.jar` | 백엔드 산출물 파일 이름 |
| `frontendFileName` | string | ✅ | 최대 255자, `.zip` | 프론트엔드 산출물 파일 이름 |

```json
{
  "backendFileName": "backend.jar",
  "frontendFileName": "frontend.zip"
}
```

**응답** `200 OK` — `PresignedUrlResponse`

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "요청이 정상적으로 처리되었습니다.",
  "data": {
    "backendArtifact": {
      "artifactId": 1,
      "uploadUrl": "https://s3.../backend.jar?X-Amz-Signature=..."
    },
    "frontendArtifact": {
      "artifactId": 2,
      "uploadUrl": "https://s3.../frontend.zip?X-Amz-Signature=..."
    },
    "expiresAt": "2026-07-28T12:10:00"
  }
}
```

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `backendArtifact.artifactId` | number(int64) | 발급된 백엔드 아티팩트 ID |
| `backendArtifact.uploadUrl` | string | S3 업로드용 Presigned URL |
| `frontendArtifact.artifactId` | number(int64) | 발급된 프론트엔드 아티팩트 ID |
| `frontendArtifact.uploadUrl` | string | S3 업로드용 Presigned URL |
| `expiresAt` | string(date-time) | Presigned URL 만료 시각 |

**업로드 방법** — 발급받은 `uploadUrl` 로 파일을 직접 `PUT` 합니다. (쿠키·CSRF 불필요)

```js
await fetch(uploadUrl, {
  method: "PUT",
  body: file, // File 객체 그대로
});
```

업로드가 끝나면 `artifactId` 를 [배포 생성](#61-배포-생성) 요청에 넘깁니다.

**에러**

| Status | Code | Message |
| --- | --- | --- |
| 400 | `COMMON_001` | 요청 값이 올바르지 않습니다. |
| 400 | `ARTIFACT_001` | 백엔드 파일은 .jar 형식이어야 합니다. |
| 400 | `ARTIFACT_002` | 프론트엔드 파일은 .zip 형식이어야 합니다. |
| 400 | `ARTIFACT_003` | 올바르지 않은 파일 이름입니다. |
| 404 | `MEMBER_004` | 회원을 찾을 수 없습니다. |
| 500 | `ARTIFACT_004` | 업로드 URL 발급에 실패했습니다. |

---

## 6. Deployment — 배포

### 6.1 배포 생성

```http
POST /api/v1/deployments
```

사전에 발급받은 Artifact를 사용해 배포를 요청합니다.
배포는 **비동기**로 처리되며, **사용자당 하나의 배포만** 존재할 수 있습니다.

- **인증** 필요 · **CSRF** 필요

**Request Body** — `DeploymentCreateRequest`

| 필드 | 타입 | 필수 | 제약 | 설명 |
| --- | --- | :---: | --- | --- |
| `projectName` | string | ✅ | 최대 100자 | 프로젝트 이름 |
| `backendArtifactId` | number(int64) | ✅ | | 백엔드 산출물 Artifact ID |
| `frontendArtifactId` | number(int64) | ✅ | | 프론트엔드 산출물 Artifact ID |
| `runtimeVersion` | string | ✅ | `17` \| `21` | Java 실행 버전 |
| `internalPort` | number(int32) | | 1 ~ 65535 | 애플리케이션 내부 포트 (예: 8080) |
| `databaseName` | string | ✅ | `^[A-Za-z][A-Za-z0-9_]{0,99}$` | DB 이름 (영문 시작, 영문/숫자/`_`) |
| `databasePassword` | string | ✅ | 최대 200자 | DB 비밀번호 |
| `env` | array | ✅ | 1 ~ 20개 | 환경변수 목록 |
| `env[].key` | string | ✅ | `^[A-Z_][A-Z0-9_]*$`, 최대 100자 | 환경변수 이름 |
| `env[].value` | string | ✅ | 최대 4000자 | 환경변수 값 |
| `visibility` | string | | `PUBLIC` \| `PRIVATE` | 공개 여부. 미입력 시 기본값 적용 |
| `description` | string | | 최대 2000자 | 서비스 설명 |

```json
{
  "projectName": "ssafyhub-showcase",
  "backendArtifactId": 1,
  "frontendArtifactId": 2,
  "runtimeVersion": "21",
  "internalPort": 8080,
  "databaseName": "ssafyhub_db",
  "databasePassword": "dbPassword123",
  "env": [
    { "key": "SPRING_PROFILES_ACTIVE", "value": "prod" }
  ],
  "visibility": "PUBLIC",
  "description": "SSAFYHUB 쇼케이스 데모 서비스입니다."
}
```

**응답** `202 Accepted` — `DeploymentCreateResponse`

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "요청이 정상적으로 처리되었습니다.",
  "data": {
    "deploymentId": 1,
    "status": "PENDING",
    "displayStatus": "배포 대기중",
    "expectedUrl": "https://ssafyhub-showcase.ssafyhub.site",
    "requestedAt": "2026-07-28T12:00:00"
  }
}
```

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `deploymentId` | number(int64) | 생성된 배포 ID |
| `status` | enum | 배포 상태 → [DeploymentStatus](#81-deploymentstatus) |
| `displayStatus` | string | 화면 표시용 상태 문구 |
| `expectedUrl` | string | 배포 완료 후 접속 예상 URL |
| `requestedAt` | string(date-time) | 배포 요청 시각 |

**에러**

| Status | Code | Message |
| --- | --- | --- |
| 400 | `COMMON_001` | 요청 값이 올바르지 않습니다. |
| 400 | `DEPLOYMENT_002` | 지원하지 않는 Java 실행 버전입니다. |
| 400 | `DEPLOYMENT_003` | 중복된 환경변수 이름이 존재합니다. |
| 400 | `ARTIFACT_007` | 업로드 파일 유형이 올바르지 않습니다. |
| 400 | `ARTIFACT_009` | S3에 업로드된 파일이 존재하지 않습니다. |
| 403 | `ARTIFACT_006` | 본인이 발급받은 파일만 사용할 수 있습니다. |
| 404 | `MEMBER_004` | 회원을 찾을 수 없습니다. |
| 404 | `ARTIFACT_005` | 업로드 파일 정보를 찾을 수 없습니다. |
| 409 | `DEPLOYMENT_001` | 사용자별 배포는 하나만 생성할 수 있습니다. |
| 409 | `ARTIFACT_008` | 이미 사용된 업로드 파일입니다. |
| 503 | `ARTIFACT_010` | 업로드 파일을 확인할 수 없습니다. 잠시 후 다시 시도해주세요. |

---

### 6.2 내 배포 상태 조회

```http
GET /api/v1/deployments
```

로그인한 사용자의 현재 배포 상태를 조회합니다. 사용자별 배포는 1개만 존재한다고 가정합니다.

- **인증** 필요

**응답** `200 OK` — `DeploymentStatusResponse`

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "요청이 정상적으로 처리되었습니다.",
  "data": {
    "deploymentId": 100,
    "projectName": "demo-service",
    "description": "실시간 채팅 기반 협업 툴입니다.",
    "visibility": "PRIVATE",
    "status": "RUNNING",
    "displayStatus": "정상 기동",
    "endpointUrl": "https://1551343-demo-service-100.apps.ssafyhub.com",
    "statusMessage": null,
    "updatedAt": "2026-07-16T16:25:00"
  }
}
```

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `deploymentId` | number(int64) | 배포 요청 ID |
| `projectName` | string | 프로젝트 이름 |
| `description` | string \| null | 서비스 설명 (미입력 시 `null`) |
| `visibility` | enum | `PUBLIC` \| `PRIVATE` |
| `status` | enum | 현재 배포 상태 → [DeploymentStatus](#81-deploymentstatus) |
| `displayStatus` | string | 화면 표시용 상태 문구 |
| `endpointUrl` | string | 배포된 서비스 접속 URL |
| `statusMessage` | string \| null | 실패 사유 또는 상태 메시지 |
| `updatedAt` | string(date-time) | 상태 마지막 변경 시각 |

**에러**

| Status | Code | Message |
| --- | --- | --- |
| 404 | `DEPLOYMENT_004` | 조회할 배포 정보가 없습니다. |

---

### 6.3 배포 삭제

```http
DELETE /api/v1/deployments/{deploymentId}
```

배포를 삭제합니다. 본인의 배포만 삭제할 수 있습니다.

- **인증** 필요 · **CSRF** 필요

⚠️ 실행 중인 서버(k8s Deployment / Service / Ingress)를 함께 내리고 배포 이력까지 제거하므로 **되돌릴 수 없습니다.**

**Path Parameter**

| 이름 | 타입 | 필수 | 설명 |
| --- | --- | :---: | --- |
| `deploymentId` | number(int64) | ✅ | 배포 ID |

**응답** `200 OK` — `data: null`

**에러**

| Status | Code | Message |
| --- | --- | --- |
| 404 | `DEPLOYMENT_004` | 조회할 배포 정보가 없습니다. |
| 502 | `DEPLOYMENT_005` | 배포 리소스 삭제에 실패했습니다. 잠시 후 다시 시도해 주세요. |

---

### 6.4 배포 진행 로그 조회

```http
GET /api/v1/deployments/{deploymentId}/logs
```

배포의 단계별 진행 로그(`PENDING` → `BUILDING` → `DEPLOYING` → `RUNNING`/`FAILED`)를 시간순으로 반환합니다.
본인 배포만 조회 가능합니다.

- **인증** 필요

**Path Parameter**

| 이름 | 타입 | 필수 | 설명 |
| --- | --- | :---: | --- |
| `deploymentId` | number(int64) | ✅ | 배포 ID |

**응답** `200 OK` — `DeploymentLogResponse[]`

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "요청이 정상적으로 처리되었습니다.",
  "data": [
    {
      "logMessage": "#5 transferring context: 52.38MB",
      "status": "BUILDING",
      "createdAt": "2026-07-30T02:38:10"
    }
  ]
}
```

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `logMessage` | string | 로그 메시지 |
| `status` | enum | 그 시점의 배포 상태 |
| `createdAt` | string(date-time) | 기록 시각 |

**에러**

| Status | Code | Message |
| --- | --- | --- |
| 404 | `DEPLOYMENT_004` | 조회할 배포 정보가 없습니다. |

---

### 6.5 배포 리소스 사용량 조회

```http
GET /api/v1/deployments/{deploymentId}/resources
```

배포된 서비스의 CPU / 메모리 사용량(현재 스냅샷)을 반환합니다. deploy-engine 메트릭을 중계합니다.
본인 배포만 조회 가능합니다.

- **인증** 필요

**Path Parameter**

| 이름 | 타입 | 필수 | 설명 |
| --- | --- | :---: | --- |
| `deploymentId` | number(int64) | ✅ | 배포 ID |

**응답** `200 OK` — `ResourceUsageResponse`

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "요청이 정상적으로 처리되었습니다.",
  "data": {
    "id": 11,
    "cpuPercent": 35,
    "memoryUsedMb": 420,
    "memoryTotalMb": 512,
    "sampledAt": "2026-07-30T02:40:00"
  }
}
```

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id` | number(int64) | 배포 ID |
| `cpuPercent` | number(int32) | CPU 사용률(%) — 코어 사용량 기준 |
| `memoryUsedMb` | number(int64) | 사용 메모리(MB) |
| `memoryTotalMb` | number(int64) | 메모리 상한(MB) |
| `sampledAt` | string(date-time) | 샘플 시각 |

**에러**

| Status | Code | Message |
| --- | --- | --- |
| 404 | `DEPLOYMENT_004` | 조회할 배포 정보가 없습니다. |

---

### 6.6 배포 리소스 사용량 시계열 조회

```http
GET /api/v1/deployments/{deploymentId}/resources/history?minutes=30
```

그래프용 CPU(%) / 메모리(MB) 시계열을 반환합니다 (deploy-engine `/metrics/range` 중계).
본인 배포만 조회 가능합니다.

- **인증** 필요

**Parameters**

| 이름 | 위치 | 타입 | 필수 | 기본값 | 설명 |
| --- | --- | --- | :---: | --- | --- |
| `deploymentId` | path | number(int64) | ✅ | | 배포 ID |
| `minutes` | query | number(int32) | | `30` | 조회 구간(분) |

**응답** `200 OK` — `ResourceHistoryResponse`

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "요청이 정상적으로 처리되었습니다.",
  "data": {
    "cpu": [
      { "time": 1785385362, "value": 35.2 }
    ],
    "memory": [
      { "time": 1785385362, "value": 420.0 }
    ]
  }
}
```

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `cpu[]` | array | CPU 사용률(%) 시계열 |
| `memory[]` | array | 메모리 사용량(MB) 시계열 |
| `*[].time` | number(int64) | 유닉스 시각(초) |
| `*[].value` | number(double) | 값 (CPU % 또는 메모리 MB) |

**에러**

| Status | Code | Message |
| --- | --- | --- |
| 404 | `DEPLOYMENT_004` | 조회할 배포 정보가 없습니다. |

---

## 7. Showcase — 공개 서비스

### 7.1 공개 서비스 목록 조회

```http
GET /api/v1/showcase/public?page=0&size=20
```

공개(`PUBLIC`)로 설정된 배포 목록을 페이지 단위로 조회합니다.

- **인증** 불필요

**Query Parameters**

| 이름 | 타입 | 필수 | 기본값 | 설명 |
| --- | --- | :---: | --- | --- |
| `page` | number(int32) | | `0` | 페이지 번호 (0부터 시작) |
| `size` | number(int32) | | `20` | 페이지 크기 |

**응답** `200 OK` — `PublicDeploymentListResponse`

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "요청이 정상적으로 처리되었습니다.",
  "data": {
    "deployments": [
      {
        "deploymentId": 1,
        "teamName": "SSAFYHUB",
        "projectName": "ssafyhub-showcase",
        "description": "SSAFYHUB 쇼케이스 데모 서비스입니다.",
        "endpointUrl": "https://1234567.ssafyhub.site",
        "updatedAt": "2026-07-28T12:00:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 42
  }
}
```

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `deployments[]` | array | 공개 서비스 목록 |
| `page` | number(int32) | 현재 페이지 번호 (0부터 시작) |
| `size` | number(int32) | 페이지 크기 |
| `totalElements` | number(int64) | 전체 항목 수 |

---

### 7.2 공개 서비스 상세 조회

```http
GET /api/v1/showcase/{id}
```

공개(`PUBLIC`)로 설정된 배포 1건의 상세 정보를 조회합니다.

- **인증** 불필요

**Path Parameter**

| 이름 | 타입 | 필수 | 설명 |
| --- | --- | :---: | --- |
| `id` | number(int64) | ✅ | 배포 ID |

**응답** `200 OK` — `PublicDeploymentResponse`

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "요청이 정상적으로 처리되었습니다.",
  "data": {
    "deploymentId": 1,
    "teamName": "SSAFYHUB",
    "projectName": "ssafyhub-showcase",
    "description": "SSAFYHUB 쇼케이스 데모 서비스입니다.",
    "endpointUrl": "https://1234567.ssafyhub.site",
    "updatedAt": "2026-07-28T12:00:00"
  }
}
```

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `deploymentId` | number(int64) | 배포 ID |
| `teamName` | string | 팀명 |
| `projectName` | string | 프로젝트 이름 |
| `description` | string | 서비스 설명 |
| `endpointUrl` | string | 접속 URL |
| `updatedAt` | string(date-time) | 마지막 수정 시각 |

**에러**

| Status | Code | Message |
| --- | --- | --- |
| 404 | `SHOWCASE_001` | 존재하지 않는 서비스입니다. |

---

### 7.3 서비스 정보 수정

```http
PATCH /api/v1/showcase/{id}
```

본인이 배포한 서비스의 공개 여부 / 설명을 수정합니다. 요청에 포함되지 않은(`null`) 필드는 변경되지 않습니다.

- **인증** 필요 · **CSRF** 필요

**Path Parameter**

| 이름 | 타입 | 필수 | 설명 |
| --- | --- | :---: | --- |
| `id` | number(int64) | ✅ | 배포 ID |

**Request Body** — `UpdateShowcaseRequest`

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | :---: | --- |
| `visibility` | enum | | `PUBLIC` \| `PRIVATE`. `null`이면 변경하지 않음 |
| `description` | string | | 서비스 설명. `null`이면 변경하지 않음 |

```json
{
  "visibility": "PUBLIC",
  "description": "SSAFYHUB 쇼케이스 데모 서비스입니다."
}
```

**응답** `200 OK` — `ShowcaseResponse`

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "요청이 정상적으로 처리되었습니다.",
  "data": {
    "deploymentId": 1,
    "projectName": "ssafyhub-showcase",
    "description": "SSAFYHUB 쇼케이스 데모 서비스입니다.",
    "visibility": "PUBLIC",
    "endpointUrl": "https://1234567.ssafyhub.site",
    "updatedAt": "2026-07-28T12:00:00"
  }
}
```

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `deploymentId` | number(int64) | 배포 ID |
| `projectName` | string | 프로젝트 이름 |
| `description` | string | 서비스 설명 |
| `visibility` | enum | `PUBLIC` \| `PRIVATE` |
| `endpointUrl` | string | 접속 URL |
| `updatedAt` | string(date-time) | 마지막 수정 시각 |

**에러**

| Status | Code | Message |
| --- | --- | --- |
| 404 | `SHOWCASE_001` | 존재하지 않는 서비스입니다. |

---

## 8. Enum 정리

### 8.1 DeploymentStatus

| 값 | 의미 |
| --- | --- |
| `PENDING` | 배포 대기중 |
| `BUILDING` | 빌드 중 |
| `DEPLOYING` | 배포 중 |
| `RUNNING` | 정상 기동 |
| `FAILED` | 실패 |

상태 전이: `PENDING` → `BUILDING` → `DEPLOYING` → `RUNNING` / `FAILED`

### 8.2 Visibility

| 값 | 의미 |
| --- | --- |
| `PUBLIC` | 쇼케이스에 공개 |
| `PRIVATE` | 비공개 |

### 8.3 RuntimeVersion

지원 Java 버전: `17`, `21` (문자열로 전달)

---

## 9. 에러 코드 전체 목록

### COMMON

| Code | Status | Message |
| --- | --- | --- |
| `COMMON_001` | 400 | 요청 값이 올바르지 않습니다. |

### AUTH

| Code | Status | Message |
| --- | --- | --- |
| `AUTH_001` | 401 | 학번 또는 비밀번호가 올바르지 않습니다. |
| `AUTH_004` | 401 | Refresh Token이 존재하지 않습니다. |
| `AUTH_005` | 401 | 유효하지 않은 Refresh Token입니다. |
| `AUTH_006` | 401 | 만료된 Refresh Token입니다. |
| `AUTH_007` | 401 | 폐기된 Refresh Token입니다. |

### MEMBER

| Code | Status | Message |
| --- | --- | --- |
| `MEMBER_001` | 409 | 이미 사용 중인 학번입니다. |
| `MEMBER_002` | 400 | 비밀번호와 비밀번호 확인이 일치하지 않습니다. |
| `MEMBER_003` | 400 | 비밀번호는 최대 72바이트까지 입력할 수 있습니다. |
| `MEMBER_004` | 404 | 회원을 찾을 수 없습니다. |

### ARTIFACT

| Code | Status | Message |
| --- | --- | --- |
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

### DEPLOYMENT

| Code | Status | Message |
| --- | --- | --- |
| `DEPLOYMENT_001` | 409 | 사용자별 배포는 하나만 생성할 수 있습니다. |
| `DEPLOYMENT_002` | 400 | 지원하지 않는 Java 실행 버전입니다. |
| `DEPLOYMENT_003` | 400 | 중복된 환경변수 이름이 존재합니다. |
| `DEPLOYMENT_004` | 404 | 조회할 배포 정보가 없습니다. |
| `DEPLOYMENT_005` | 502 | 배포 리소스 삭제에 실패했습니다. 잠시 후 다시 시도해 주세요. |

### SHOWCASE

| Code | Status | Message |
| --- | --- | --- |
| `SHOWCASE_001` | 404 | 존재하지 않는 서비스입니다. |

> `MEMBER_003`, `AUTH_002`, `AUTH_003` 등 명세서 예시에 등장하지 않는 코드가 서버에 추가로 정의되어 있을 수 있습니다. 위 표는 OpenAPI 문서에 명시된 코드만 정리한 것입니다.

---

## 부록. 전체 배포 플로우 예시

```
1. GET  /api/v1/auth/csrf                        # CSRF 토큰 발급
2. POST /api/v1/auth/login                       # 로그인 (쿠키 수신)
3. POST /api/v1/artifacts/presigned-urls         # artifactId 2개 + uploadUrl 2개 획득
4. PUT  {backendArtifact.uploadUrl}              # .jar 직접 업로드 (S3)
   PUT  {frontendArtifact.uploadUrl}             # .zip 직접 업로드 (S3)
5. POST /api/v1/deployments                      # artifactId로 배포 요청 (202)
6. GET  /api/v1/deployments                      # 상태 폴링 (PENDING→...→RUNNING)
   GET  /api/v1/deployments/{id}/logs            # 진행 로그 표시
7. GET  /api/v1/deployments/{id}/resources       # 리소스 모니터링
   GET  /api/v1/deployments/{id}/resources/history
8. PATCH /api/v1/showcase/{id}                   # 공개 설정 / 설명 수정
```

# SSAFYHUB Backend

SSAFYHUB 서비스의 백엔드 애플리케이션입니다.

사용자 인증, 프로젝트 관리, 빌드 파일 업로드, 배포 요청 및 상태 관리, 환경변수 관리 기능을 제공하는 것을 목표로 합니다.

> 현재는 프로젝트 초기화 단계입니다. 애플리케이션 진입점과 기본 실행 환경만 구성되어 있으며, 개별 기능은 아직 구현되지 않았습니다. Spring Security도 현재 적용되어 있지 않습니다.

---

## 1. 개발 환경

### Language & Framework

- Java 21
- Spring Boot 3.5.16
- Gradle 9.3.0 (Gradle Wrapper)
- Spring Web
- Spring Data JPA
- Validation
- MySQL 8.4
- Lombok
- Actuator

### 추가 라이브러리 및 외부 서비스

- JWT (`jjwt` 0.12.6)
- AWS SDK for Java v2
- AWS S3

JWT와 AWS S3 관련 의존성은 추가되어 있지만 실제 연동 기능은 아직 구현되지 않았습니다.

---

## 2. 로컬 실행

### 사전 준비

- JDK 21
- Docker Desktop 또는 Docker Engine
- Git

### 환경변수 설정

프로젝트 루트의 `.env.example`을 복사하여 `.env`를 생성합니다.

Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

macOS/Linux:

```bash
cp .env.example .env
```

생성한 `.env`에서 로컬 환경에 맞게 값을 수정합니다. Spring Boot는 실행 시 프로젝트 루트의 `.env`를 자동으로 불러옵니다.

| 환경변수 | 설명 |
|---|---|
| `DB_URL` | MySQL JDBC URL |
| `DB_USERNAME` | MySQL 사용자 이름 |
| `DB_PASSWORD` | MySQL 비밀번호 |
| `JWT_SECRET` | JWT 서명 키 |
| `JWT_ACCESS_TOKEN_EXPIRATION_MILLIS` | Access Token 만료 시간(ms) |
| `JWT_REFRESH_TOKEN_EXPIRATION_MILLIS` | Refresh Token 만료 시간(ms) |
| `AWS_REGION` | AWS 리전 |
| `AWS_S3_BUCKET` | S3 버킷 이름 |

`.env`는 Git에 포함되지 않습니다. 공유가 필요한 환경변수의 이름과 예시는 `.env.example`에만 작성합니다.

### MySQL 실행

```bash
docker compose up -d mysql
```

컨테이너 상태를 확인합니다.

```bash
docker compose ps
```

기본 로컬 접속 정보는 다음과 같습니다.

| 항목 | 값 |
|---|---|
| Host | `127.0.0.1` |
| Port | `3306` |
| Database | `ssafyhub` |
| Username | `ssafyhub` |
| Password | `ssafyhub` |

### 애플리케이션 실행

Windows:

```powershell
.\gradlew.bat bootRun
```

macOS/Linux:

```bash
./gradlew bootRun
```

기본 서버 포트는 `8080`입니다.

### 상태 확인

```text
GET http://localhost:8080/actuator/health
```

현재 별도의 루트 API 또는 `/api/health` 엔드포인트는 구현되어 있지 않습니다.

### MySQL 종료

```bash
docker compose down
```

데이터 볼륨까지 삭제하려면 다음 명령을 사용합니다. 저장된 로컬 데이터가 모두 삭제되므로 주의해야 합니다.

```bash
docker compose down -v
```

---

## 3. 브랜치 전략

본 프로젝트는 `main - develop - feature` 브랜치 전략을 사용합니다.

```text
main
└─ develop
   ├─ feature/auth
   ├─ feature/project
   ├─ feature/artifact
   ├─ feature/env
   └─ feature/deployment
```

### main

- 배포 기준 브랜치
- 직접 push 금지
- 검증된 변경 사항만 merge
- 추후 CI/CD 배포 기준 브랜치로 사용

### develop

- 개발 통합 브랜치
- 기능 개발 완료 후 Merge Request 대상 브랜치
- 기능 통합 및 테스트 확인용 브랜치

### 작업 브랜치

- 항상 최신 `develop` 브랜치에서 생성
- 하나의 브랜치에서는 하나의 기능 또는 작업 단위만 처리
- 작업 완료 후 `develop` 브랜치로 Merge Request 생성

브랜치 이름은 다음 형식을 사용합니다.

```text
feature/기능명
fix/수정내용
refactor/수정내용
docs/문서내용
chore/설정내용
```

예시:

```text
feature/auth
feature/project
feature/s3-upload
fix/login-token-error
refactor/global-exception
docs/api-spec
chore/gradle-config
```

---

## 4. 작업 흐름

### 1. develop 최신화

```bash
git switch develop
git pull origin develop
```

### 2. 작업 브랜치 생성

```bash
git switch -c feature/기능명
```

### 3. 변경 사항 커밋

```bash
git add .
git commit -m "feat: 작업 내용"
```

### 4. 원격 브랜치 push

```bash
git push -u origin feature/기능명
```

### 5. Merge Request 생성

```text
작업 브랜치 → develop
```

리뷰와 로컬 실행 또는 테스트 확인을 마친 뒤 merge합니다.

---

## 5. 커밋 메시지 규칙

```text
type: 작업 내용
```

| 타입 | 설명 |
|---|---|
| `feat` | 새로운 기능 추가 |
| `fix` | 버그 수정 |
| `refactor` | 코드 리팩터링 |
| `docs` | 문서 수정 |
| `test` | 테스트 추가 또는 수정 |
| `chore` | 빌드, 설정, 의존성 등 기타 작업 |
| `style` | 코드 동작과 무관한 형식 수정 |
| `remove` | 파일 또는 코드 삭제 |

예시:

```text
feat: 로그인 기능 구현
fix: 토큰 만료 응답 수정
refactor: 예외 처리 구조 개선
docs: 로컬 실행 방법 추가
chore: Gradle 의존성 변경
test: 로그인 서비스 테스트 추가
```

---

## 6. Merge Request 규칙

- 하나의 MR에는 하나의 기능 또는 작업 단위만 포함합니다.
- 공통 설정 파일을 수정했다면 MR 본문에 변경 내용을 표시합니다.
- 기능 동작, 테스트 결과, 충돌 여부를 확인한 후 리뷰를 요청합니다.

MR 제목은 커밋 메시지와 동일한 형식을 사용합니다.

```text
type: 작업 내용
```

MR 본문 양식:

```md
## 작업 내용

-

## 확인 내용

-

## 참고 사항

-
```

---

## 7. 보안 및 설정 관리

- `.env`, `application-secret.yml`, `application-prod.yml`은 Git에 커밋하지 않습니다.
- DB 비밀번호, JWT Secret, API Key 등의 민감정보는 환경변수로 관리합니다.
- 실제 비밀값 대신 `.env.example`에 변수 이름과 예시만 작성합니다.
- Access Token, Refresh Token, 비밀번호, Secret을 로그에 출력하지 않습니다.
- AWS 자격 증명은 로컬 환경 또는 배포 환경의 안전한 비밀 저장소에서 관리합니다.
- 비밀값이 실수로 커밋되었다면 파일 삭제만으로 끝내지 않고 해당 값을 즉시 폐기하고 재발급합니다.

---

## 8. 협업 규칙

- 작업 시작 전 issue 또는 작업 내용을 팀원과 공유합니다.
- 공통 설정 파일 수정 전 팀원에게 알립니다.
- 충돌 가능성이 높은 파일은 동시에 수정하지 않도록 조율합니다.
- MR 생성 후 팀원에게 리뷰를 요청합니다.
- merge 전 로컬 실행 또는 테스트 결과를 확인합니다.

현재 공통 설정 파일:

```text
build.gradle
settings.gradle
application.yml
docker-compose.yml
.env.example
```

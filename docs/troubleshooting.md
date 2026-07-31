# 트러블슈팅

**실제로 발생했던 문제만 적었다.** 각 항목에 원인, 해결, 그리고 커밋을 함께 남겼다.

증상이 급하면 [운영 런북](./runbook.md)의 증상별 진입점을 먼저 본다.
이 문서는 "왜 그랬는지"와 "다시 만나면 어떻게 알아채는지"를 다룬다.

## 목차

| 영역 | 사례 |
|---|---|
| [CI/CD](#cicd) | 트리거 부재 · 조용한 스킵 · 테스트 컴파일 · 헬스체크 오진 · 브랜치 소실 |
| [배포·인프라](#배포인프라) | 디스크 포화 · 롤백 미실행 · 포트 충돌 · IMDS hop limit · 컨테이너 localhost |
| [인증·CSRF](#인증csrf) | 로그인 후 토큰 무효 · 재발급 CSRF · 403 복구 경로 없음 |
| [설정](#설정) | 필수 환경변수 누락 · 끝 슬래시 · mock 모드 · CORS |
| [데이터](#데이터) | Outbox 정체 · 학번 검증 |

---

## CI/CD

### 푸시해도 빌드가 생기지 않는다

**증상.** 백엔드 코드를 `main`에 푸시해도 Jenkins에 빌드가 나타나지 않는다.

**원인.** 백엔드 잡에 **자동 트리거가 하나도 없었다.**

- GitLab 웹훅은 `/project/ssafyhub-frontend` 하나만 등록돼 있었다
- Multibranch 잡 설정의 `<triggers/>`가 비어 있어 주기적 스캔도 없었다

프론트엔드는 웹훅이 있어 정상 동작했기 때문에 "CI가 돌고 있다"고 오해하기 쉬웠다.

**해결.** 백엔드용 웹훅을 추가하고, Jenkinsfile에 `pollSCM('H/3 * * * *')`을 넣어 이중화했다.
웹훅이 먼저 빌드를 만들면 폴링 차례에는 새 커밋이 없어 중복 빌드는 생기지 않는다.

**알아채는 법.** 빌드 원인을 보면 어느 경로로 트리거됐는지 구분된다.

```bash
docker exec jenkins sh -c 'grep -a -m1 -oE "Started by an SCM change|Branch indexing|Started by user" /var/jenkins_home/jobs/ssafyhub-backend/branches/main/builds/<번호>/log'
```

| 값 | 의미 |
|---|---|
| `Branch indexing` | 웹훅이 유발한 스캔 |
| `Started by an SCM change` | `pollSCM`이 잡음 (최대 3분 지연) |
| `Started by user` | Build Now 수동 실행 |

---

### 빌드는 `SUCCESS`인데 배포가 안 됐다

**증상.** 빌드가 성공으로 끝났는데 컨테이너가 생성되지 않았다.

```
Warning, empty changelog. Probably because this is the first build.
Stage "Deploy" skipped due to when conditional
Finished: SUCCESS
```

**원인.** 배포 조건이 `changeset 'backend/**'`와 `triggeredBy 'UserIdCause'`뿐이었다.
브랜치 최초 인덱싱에서는

- changelog가 비어 `changeset`이 매칭되지 않고
- 트리거도 `UserIdCause`가 아니라 `BranchIndexingCause`라

두 조건이 모두 빗나간다. **`when`으로 스테이지를 건너뛰는 것은 실패가 아니므로** 빌드는
`SUCCESS`로 끝난다.

**해결.** 조건에 `expression { currentBuild.changeSets.isEmpty() }`를 추가했다.
"무엇이 바뀌었는지 모르겠으면 안 바뀐 것으로 단정하지 않는다"는 규칙이다.
프론트엔드에도 같은 결함이 있어 함께 고쳤다.

**커밋** `ba6f671`, `7357adb`

**알아채는 법.** 배포 여부는 빌드 결과가 아니라 **결과물로 확인한다.**

```bash
docker ps --format '{{.Names}} {{.Image}}' | grep backend
```

---

### 테스트 컴파일 오류로 모든 배포가 막혔다

**증상.** 빌드가 `FAILURE`. `Docker Build`와 `Deploy`가 연쇄로 스킵됐다.

```
error: constructor DeploymentService cannot be applied to given types;
  required: ..., DeploymentOutboxRepository, DevOpsDeploymentClient, S3ObjectVerifier, SecretEncryptor
  found:    ..., DeploymentOutboxRepository, S3ObjectVerifier, SecretEncryptor
Stage "Docker Build" skipped due to earlier failure(s)
```

**원인.** `DeploymentService`에 `DevOpsDeploymentClient` 의존성이 추가됐는데
`DeploymentServiceTest`가 갱신되지 않았다. 파이프라인이 `./gradlew clean build`로 테스트를
포함해 빌드하므로 테스트가 컴파일되지 않으면 배포까지 못 간다.

**해결.** `@Mock` 선언과 생성자 인자를 추가했다.

**커밋** `ba6f671`

**예방.** 프로덕션 코드의 생성자를 바꿀 때는 테스트도 함께 고친다.
로컬에 JDK가 없으면 Jenkins 컨테이너의 캐시를 빌려 미리 검증할 수 있다.

```bash
docker exec jenkins sh -c 'cp -a /var/jenkins_home/workspace/ssafyhub-backend_main /tmp/verify && rm -rf /tmp/verify/backend/build'
docker exec jenkins sh -c 'cd /tmp/verify/backend && ./gradlew build --console=plain 2>&1 | tail -20'
docker exec jenkins rm -rf /tmp/verify
```

---

### 헬스체크가 Jenkins 자신을 찔렀다

**증상.** 새 컨테이너가 정상인데 헬스체크가 403을 받고 실패했다.

**원인.** Jenkins가 컨테이너로 돌기 때문에 파이프라인 `sh` 스텝의 `localhost`는
**Jenkins 컨테이너 자신**을 가리킨다. Jenkins 공식 이미지는 내부에서 8080을 리슨하므로
`localhost:8080`이 백엔드가 아니라 Jenkins에 닿아, 인증 없는 요청에 403을 돌려줬다.

**해결.** 두 경로를 순서대로 시도하도록 바꿨다.

1. `docker inspect`로 컨테이너 IP를 얻어 직접 접근
2. 폴백으로 `--network host` 일회용 컨테이너에서 호스트의 8080 확인

**커밋** `b8d6180`

**교훈.** 컨테이너 안의 `localhost`는 호스트가 아니다. 이 착각은 [연동 주소](#deploy-engine-주소가-localhost다)에서도 반복됐다.

---

### `develop` 브랜치가 반복해서 사라졌다

**증상.** 하루에 두 번 `develop`이 원격에서 삭제됐다. 팀원들의 로컬 추적 브랜치가 끊기고
배포 대상이 없어졌다.

**원인.** `develop` → `main` 병합 시 MR의 **"Delete source branch"** 옵션이 develop을 삭제했다.
feature 브랜치에는 맞는 규칙이지만 `develop`은 병합받는 영구 브랜치다.

**해결.** 두 번 복구했다. 두 번째에는 병합커밋의 **두 번째 부모**를 확인해 삭제 당시의 정확한
끝단을 찾았다 — 로컬 브랜치가 한 커밋 뒤처져 있어서 그대로 복구했다면 CSRF 수정 커밋이 사라졌다.

```bash
git log -1 --format="parents=%p" <병합커밋>   # 두 번째 부모가 삭제된 브랜치의 끝단
git push origin <끝단SHA>:refs/heads/develop
```

근본 대응으로 **배포 대상을 `main` 하나로 고정**해, 사라질 수 있는 브랜치에 파이프라인이
의존하지 않게 했다.

**커밋** `0df214c`

**예방.** MR 병합 시 "Delete source branch" 체크 해제, 그리고 `develop`을 보호 브랜치로 등록한다.
보호 브랜치는 삭제가 차단된다.

---

## 배포·인프라

### 디스크가 차서 CI 전체가 멈췄다

**증상.** 모든 빌드가 시작조차 못 하고 `Waiting for next available executor`에서 대기했다.

**원인.** 루트 디스크 8GB가 93%(여유 624MB)까지 차자 **Jenkins의 DiskSpaceMonitor가 빌드 노드를
자동으로 오프라인 처리했다.** 기본 임계값은 1GB다.

기여 원인: 백엔드 Jenkinsfile에 `buildDiscarder`가 빠져 빌드 기록이 무한히 쌓였다.

**해결.** EBS 볼륨을 30GB로 무중단 확장하고 `buildDiscarder(logRotator(numToKeepStr: '10'))`를
추가했다.

**커밋** `b570e65`(이미지 정리), `ba6f671`(buildDiscarder)

**함정 하나.** **executor 개수를 늘려도 해결되지 않는다.** 노드 자체가 오프라인이기 때문이다.
공간을 확보하면 Jenkins가 주기적 재검사로 자동 복귀시킨다.

**함정 둘.** 회수할 때 `docker system prune -a`는 **정지된 컨테이너까지 삭제한다.**
`-previous` 롤백 대상이 사라지므로 장애 대응 중에는 쓰지 않는다. `docker builder prune -af` →
`docker image prune -af` 순으로 진행한다.

**함정 셋.** 컨테이너 JSON 로그는 `docker system df`에 집계되지 않는다. Docker 사용량이
작은데도 디스크가 차 있으면 여기를 본다.

```bash
sudo du -sh /var/lib/docker/containers/*/*-json.log 2>/dev/null | sort -h
```

---

### 배포 실패 후 백엔드가 죽은 채 방치됐다

**증상.** 배포가 실패한 뒤 컨테이너가 아예 존재하지 않고, `ssafyhub-backend-container-previous`만
`Exited (143)` 상태로 남아 있었다. 프로덕션 API가 응답하지 않았다.

**원인.** 배포는 `docker stop` → `rename` → `docker run` 순서로 진행된다.
롤백 코드가 **헬스체크 실패 분기 안에만** 있었기 때문에, 그보다 앞선 `docker run` 실패는
`set -eu`로 즉시 종료되며 롤백에 도달하지 못했다.

```
Successfully tagged ssafyhub-backend:3
새 컨테이너를 실행하고 상태를 확인합니다...
docker: Error ... bind: address already in use
→ 스크립트 즉시 종료. 구 컨테이너는 -previous 이름으로 정지된 채 남음
```

**해결.** `trap`으로 전 경로를 덮었다.

```sh
restore_previous() {
    docker rm -f "$CONTAINER_NAME" 2>/dev/null || true
    if docker container inspect "$PREVIOUS_CONTAINER_NAME" >/dev/null 2>&1; then
        docker rename "$PREVIOUS_CONTAINER_NAME" "$CONTAINER_NAME"
        docker start "$CONTAINER_NAME" || true
    fi
}
trap restore_previous EXIT INT TERM
```

헬스체크를 통과해 배포가 확정된 뒤에는 `trap - EXIT INT TERM`으로 해제한다.
그러지 않으면 이후 이미지 정리가 실패할 때 정상 컨테이너를 되돌려버린다.

**커밋** `ba6f671`

**수동 복구.** `-previous`가 남아 있으면 이름을 되돌리는 것이 가장 빠르다.

```bash
docker rename ssafyhub-backend-container-previous ssafyhub-backend-container
docker start ssafyhub-backend-container
```

> `Exited (143)`은 SIGTERM(128+15)으로 정상 종료된 흔적이다. 크래시가 아니므로 그대로 다시 띄울 수 있다.

---

### `docker run`이 포트 충돌로 실패했다

**증상.**

```
docker: Error response from daemon: driver failed programming external connectivity ...
Error starting userland proxy: listen tcp4 0.0.0.0:8080: bind: address already in use
```

**원인.** 배포 실패 대응으로 기동해 둔 **수동 `java -jar` 프로세스**가 8080을 점유하고 있었다.
컨테이너가 아니라 호스트 프로세스라 `docker ps`에는 보이지 않았다.

**해결.** 포트를 비운 **뒤에** 빌드를 시작한다. 순서가 뒤바뀌면 `docker run`이 먼저 실행돼
같은 충돌이 반복된다.

```bash
sudo ss -tlnp | grep :8080      # 점유자 확인
sudo kill <PID>
sudo ss -tlnp | grep :8080      # 비었는지 확인 후 Build Now
```

**구분법.** 명령줄로 판별한다.

| 명령줄 | 정체 |
|---|---|
| `java -jar /home/ec2-user/app.jar` (절대경로) | 수동 프로세스 |
| `java -jar app.jar` (상대경로) | 컨테이너 내부 프로세스 — 건드리지 않는다 |

```bash
sudo cat /proc/<PID>/cgroup    # docker-...scope 이면 컨테이너
```

`docker-proxy`가 아니라 `java`가 8080을 잡고 있으면 컨테이너 밖에서 도는 것이다.

systemd 서비스로 등록해 뒀다면 `kill`만으로는 되살아난다. `systemctl stop` + `disable`이 필요하다.

---

### IMDS hop limit — 호스트에서는 되고 컨테이너에서만 깨진다

**증상.** IAM 역할로 전환했는데 컨테이너에서 S3 접근이 실패할 수 있는 상황이었다.

**원인.** EC2 IMDS의 `HttpPutResponseHopLimit`이 **1**이면, 브리지 네트워크의 컨테이너는
`169.254.169.254`에 도달하지 못한다. 브리지를 한 번 더 거치며 TTL이 깎이기 때문이다.
호스트에서 `aws s3 ls`가 성공해도 컨테이너 안에서는 자격증명을 못 받는다.

**해결.** 값을 확인한 뒤에 액세스 키를 제거했다.

```bash
aws ec2 describe-instances --instance-ids <ID> --query 'Reservations[].Instances[].MetadataOptions'
```

`2`여야 한다. `1`이면 다음으로 올린다.

```bash
aws ec2 modify-instance-metadata-options --instance-id <ID> --http-put-response-hop-limit 2 --http-endpoint enabled
```

**검증.** 브리지 컨테이너에서 실제로 토큰이 발급되는지 확인한다.

```bash
docker run --rm curlimages/curl:latest -s --max-time 5 -X PUT \
  http://169.254.169.254/latest/api/token -H 'X-aws-ec2-metadata-token-ttl-seconds: 60'
```

**관련.** IAM 역할을 쓸 때 `AWS_ACCESS_KEY_ID`를 환경변수로 넣으면 **SDK 탐색 순서상 환경변수가
먼저 걸려 역할이 무시된다.** 넣지 않는 것이 맞다.

```bash
docker exec ssafyhub-backend-container printenv AWS_ACCESS_KEY_ID   # 출력이 없어야 정상
```

---

### `deploy-engine` 주소가 `localhost`다

**증상.** 배포 요청이 전달되지 않았다.

**원인.** `DEVOPS_WAS_BASE_URL`이 `http://localhost:8081`이었다. `.env.example`의 로컬 예시값을
그대로 쓴 것이다. **백엔드는 컨테이너 안에서 돌기 때문에 컨테이너의 `localhost`는 자기 자신이고,**
호스트나 다른 인스턴스의 서비스에 닿지 않는다.

호스트에서 `curl`로 확인하면 성공하지만 앱은 실패하는, 진단이 까다로운 형태다.

**해결.** 사설 IP(`172.31.0.241:8081`)로 바꿨다. 같은 VPC이고 보안그룹 참조로만 열려 있다.

**확인.** 반드시 **컨테이너의 환경변수를 읽어서** 검증한다.

```bash
URL=$(docker exec ssafyhub-backend-container printenv DEVOPS_WAS_BASE_URL)
curl -sS -o /dev/null -w 'HTTP %{http_code}\n' --max-time 5 "$URL/api/deployments"
```

`405`나 `400`이면 도달 성공이다(POST 전용 엔드포인트에 GET을 보냈으므로 정상). `000`이면 차단이다.

---

## 인증·CSRF

### 로그인 후 상태 변경 요청이 실패했다

**원인.** 인증 주체가 바뀌면 기존 CSRF 토큰이 무효가 될 수 있다. 로그인 직후 그대로 다음 요청을
보내면 403이 났다.

**해결.** 로그인 성공 후 CSRF 토큰을 다시 받도록 했다.

**커밋** `205863d`

---

### 재발급 요청 자체가 CSRF에 걸렸다

**원인.** `POST /api/v1/auth/reissue`도 상태 변경 요청이라 CSRF 검증 대상이다.
액세스 토큰이 만료된 상태에서 재발급을 시도할 때 CSRF 토큰이 어긋나 있으면 재발급도 실패한다.

**해결.** 재발급 경로의 CSRF 처리를 바로잡았다.

**커밋** `83157bf`

---

### 403이 한 번 나면 새로고침까지 아무것도 안 됐다

**증상.** 상태 변경 요청이 한 번 403으로 실패하면, 그 뒤 모든 상태 변경 요청이 계속 403이었다.
페이지를 새로고침하면 정상으로 돌아왔다.

**원인.** 앱 시작 시 CSRF 토큰 발급 요청을 `await`하지 않고 실패도 무시하므로, 백엔드 순단이나
쿠키 소실로 `XSRF-TOKEN`이 없는 상태가 그대로 유지됐다. 그런데 인터셉터는 **401만 복구하고
403/CSRF는 복구 경로가 없었다.**

**해결.** `403 CSRF_001` 응답 시 토큰을 강제 재발급하고 원래 요청을 1회 재시도하도록 했다.
`withXSRFToken`도 명시해 axios의 same-origin 판정에 의존하지 않게 했다.

**커밋** `7c5916e`

**주의.** 이 계열 문제는 **로컬에서 재현되지 않을 수 있다.** `security.cookie.secure`가
프로덕션은 `true`, 로컬은 `false`라서 쿠키 전송 조건이 다르다.

자세한 흐름은 [인증 흐름](./auth-flow.md) 참고.

---

## 설정

### 필수 환경변수가 없어 컨텍스트 로딩이 실패했다

**증상.** 컨테이너가 기동하자마자 종료됐다.

**원인.** `application-prod.yml`이 기본값 없이 참조하는 값들이 누락됐다.
Outbox 기능 추가와 함께 `SECRET_ENCRYPTION_KEY`, `DEVOPS_WAS_BASE_URL`이 필수가 됐는데
파이프라인의 주입 목록이 갱신되지 않았다.

**해결.** Jenkinsfile의 `withCredentials`와 `docker run -e` 목록에 추가했다.

**커밋** `bc50590`

**필수 7개.** 이 중 하나라도 없으면 기동하지 않는다.

`DB_URL` · `DB_USERNAME` · `DB_PASSWORD` · `JWT_SECRET` · `SECRET_ENCRYPTION_KEY` ·
`AWS_S3_BUCKET` · `DEVOPS_WAS_BASE_URL`

```bash
docker exec ssafyhub-backend-container printenv | grep -E '^(DB_URL|JWT_SECRET|SECRET_ENCRYPTION_KEY|AWS_S3_BUCKET|DEVOPS_WAS_BASE_URL)=' | sed 's/=.*/=<설정됨>/'
```

---

### 연동 주소의 끝 슬래시

**증상.** 잠재적 문제로 발견. `DEVOPS_WAS_BASE_URL`이 `.../8081/`로 끝나 있었다.

**원인.** `RestClient`가 `/api/deployments`를 덧붙이면서 경로가 `//api/deployments`가 될 수 있고,
대상 서버는 이를 거부한다.

```
/api/deployments    →  405  (정상)
//api/deployments   →  404
```

배포 요청은 실패 시 **재시도 없이 영구 `FAILED`** 가 되므로 조용히 유실될 수 있는 경로였다.

**해결.** Jenkins 크리덴셜에서 끝 슬래시를 제거하고 재배포로 반영했다.

**예방.** base URL에는 경로도, 끝 슬래시도 넣지 않는다.

---

### mock 모드로 배포될 수 있다

**증상.** 백엔드를 붙였는데 화면이 가짜 데이터를 보여준다.

**원인.** `src/api/client.js`의 판정이 이렇다.

```js
export const USE_MOCKS = import.meta.env.VITE_USE_MOCKS !== "false";
```

**정확히 문자열 `"false"`일 때만** 실제 API를 호출한다. 변수를 빼거나 비워두면 mock 모드다.

**해결.** Jenkinsfile이 빌드 시 `VITE_USE_MOCKS='false'`를 주입한다.

**주의.** 수동으로 프론트엔드를 빌드할 때 이 값을 빠뜨리면 **mock 데이터가 프로덕션에 배포된다.**
`.env.production.local`에 넣어두는 것이 안전하다.

**커밋** `aea9af4`

---

### API 주소를 절대경로로 두어 CORS가 필요했다

**원인.** 프론트엔드가 `VITE_API_BASE_URL`에 백엔드 절대주소를 넣어 호출하면 다른 오리진이 되어
CORS 설정이 필요해진다. 쿠키 인증까지 얹으면 `SameSite` 문제도 겹친다.

**해결.** `VITE_API_BASE_URL`을 **빈 문자열**로 두어 상대경로를 쓰게 했다. CloudFront의 `/api/*`
behavior가 ALB로 프록시하므로 브라우저에는 `ssafyhub.site` 한 오리진으로만 보인다.
CORS 설정이 아예 필요 없어졌다.

**커밋** `c79dab0`

**주의.** Vite는 **빌드 시점에** 값을 치환한다. 배포 후 S3 파일을 바꿔도 API 주소는 변경되지 않는다.

---

## 데이터

### Outbox가 `FAILED` / `PROCESSING`에서 멈춘다

**원인.** 설계상 재시도가 없다.

- `markFailed()`가 상태를 `FAILED`로 **종결**시킨다. 백오프도, 재시도 횟수 제한도 없다
- 스케줄러는 `PENDING`만 집어온다. 따라서 `FAILED`는 다시 처리되지 않는다
- `PENDING` → `PROCESSING`으로 바꾼 뒤 프로세스가 죽으면 **`PROCESSING`에서 영구 정체**된다.
  복구 스윕 로직이 없다

즉 데브옵스 WAS가 순간적으로 죽어도 그 사이 요청이 유실된다.

**임시 대응.** 주소를 바로잡은 뒤 수동으로 되살린다.

```sql
UPDATE deployment_outbox
SET status = 'PENDING', next_attempt_at = NOW(), last_error = NULL
WHERE status IN ('FAILED', 'PROCESSING');
```

⚠️ 실패가 "요청 도달 전"이 아니라 "응답 수신 실패"였다면 재전송으로 **중복 배포**가 생길 수 있다.
건수가 많으면 `deployment_id`를 대조한 뒤 실행한다.

**근본 대응(미완).** 백오프 재시도와 `PROCESSING` 고아 행 복구 스윕이 필요하다. 열린 과제다.

---

### 학번 검증이 느슨했다

**원인.** 학번 형식 제약이 없어 임의 문자열이 들어갈 수 있었다.

**해결.** 숫자 7자리로 제한했다.

```java
@Pattern(regexp = "^\\d{7}$", message = "학번은 숫자 7자리로 입력해주세요.")
```

**커밋** `a1cb52d`

---

## 진단할 때의 원칙

여러 사례에서 반복적으로 유효했던 기준들이다.

- **결과물로 확인한다.** 빌드 `SUCCESS`, HTTP `200`, "URL 발급 성공"은 모두 최종 증거가 아니다.
  컨테이너가 떠 있는지, S3에 객체가 올라갔는지, 행이 `PUBLISHED`인지를 본다.
- **호스트에서 되는 것이 컨테이너에서도 된다고 가정하지 않는다.** `localhost`와 IMDS 모두
  이 경계에서 갈렸다.
- **로컬에서 재현되지 않는 문제가 있다.** 쿠키 `Secure` 속성이 프로파일에 따라 다르다.
- **추측한 값을 확정처럼 쓰지 않는다.** 환경변수는 실행 중인 프로세스에서 읽고,
  시각은 로그·`docker inspect`에서 확인한다.
- **서비스 복구를 원인 규명보다 앞세운다.** 폴백으로 먼저 살리고 나서 조사한다.
- **되돌릴 방법을 먼저 확인한다.** 무언가를 정지·삭제하기 전에 롤백 수단이 남아 있는지 본다.

## 관련 문서

| 문서 | 내용 |
|---|---|
| [운영 런북](./runbook.md) | 증상별 대응 절차와 명령 |
| [CI/CD 장애 기록](./cicd-incident-2026-07-29.md) | 2026-07-29 복합 장애 상세 |
| [인증 흐름](./auth-flow.md) | 쿠키·CSRF·재발급 시퀀스 |
| [포팅 매뉴얼](./porting-manual.md) | 환경변수, 배포 절차 |

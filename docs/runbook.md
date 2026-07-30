# 운영 런북

**장애 기록이 "무슨 일이 있었나"라면, 이 문서는 "다음에 어떻게 대응하나"다.**
2026-07-29 장애 대응 과정에서 실제로 사용한 명령과 판단 순서를 정리했다.

명령은 모두 메인 EC2에서 실행한다.

```bash
ssh ec2-user@jenkins.ssafyhub.site
```

## 0. 30초 상태 점검

무엇이 문제인지 모를 때 가장 먼저 실행한다.

```bash
docker ps -a --format '{{.Names}}\t{{.Status}}\t{{.Ports}}\t{{.Image}}'
```
```bash
curl -s --max-time 5 http://localhost:8080/actuator/health
```
```bash
df -h /
```
```bash
sudo ss -tlnp | grep :8080
```

| 정상 상태 | 값 |
|---|---|
| 컨테이너 | `ssafyhub-backend-container` **Up**, `0.0.0.0:8080->8080/tcp` |
| 헬스체크 | `{"status":"UP"}` |
| 디스크 | 여유 **1GB 이상** (그 아래로 떨어지면 Jenkins가 멈춘다) |
| 8080 점유자 | `docker-proxy` |

**8080을 `java` 프로세스가 잡고 있으면** 컨테이너가 아니라 수동 JAR이 서비스하는 중이다.
[6절](#6-수동-jar과-컨테이너-사이-전환)을 본다.

## 1. 증상별 진입점

| 증상 | 절 |
|---|---|
| API가 응답하지 않는다 | [2. 백엔드 다운](#2-백엔드-다운) |
| 푸시했는데 배포가 안 된다 | [3. 배포가 트리거되지 않음](#3-배포가-트리거되지-않음) |
| 빌드가 실패한다 | [4. 빌드 실패](#4-빌드-실패) |
| 빌드가 `SUCCESS`인데 반영이 안 됐다 | [5. 성공했는데 배포되지 않음](#5-성공했는데-배포되지-않음) |
| 디스크가 찼다 | [7. 디스크 부족](#7-디스크-부족) |
| 배포 요청이 처리되지 않는다 | [8. Outbox 정체](#8-outbox-정체) |
| 파일 업로드가 안 된다 | [9. S3 업로드 실패](#9-s3-업로드-실패) |
| 로그인이 풀리거나 403이 난다 | [10. 인증-CSRF 문제](#10-인증csrf-문제) |

## 2. 백엔드 다운

### 진단

```bash
docker ps -a --format '{{.Names}}\t{{.Status}}'
```

| 관찰 | 원인 | 조치 |
|---|---|---|
| 컨테이너가 **없음** | 배포 실패 후 롤백 미완료 | 아래 복구 |
| `Restarting` 반복 | 기동 실패 (설정·DB 연결) | 로그 확인 |
| `Exited (143)` | `docker stop`으로 정상 종료된 흔적 | 아래 복구 |
| `-previous`만 남아 있음 | 배포 중단. 이름이 되돌려지지 않음 | 아래 복구 |

```bash
docker logs --tail 100 ssafyhub-backend-container
```

컨테이너가 없으면 Jenkins 빌드 로그를 본다.

```bash
docker exec jenkins sh -c 'ls -1 /var/jenkins_home/jobs/ssafyhub-backend/branches/main/builds/ | grep -E "^[0-9]+$" | sort -n | tail -3'
```
```bash
docker exec jenkins sh -c 'tail -60 /var/jenkins_home/jobs/ssafyhub-backend/branches/main/builds/<번호>/log'
```

### 복구 — 우선순위 순

**① `-previous` 컨테이너가 남아 있다면 그것을 되살린다.** 가장 빠르다.

```bash
docker ps -a --format '{{.Names}}' | grep previous
```
```bash
docker rename ssafyhub-backend-container-previous ssafyhub-backend-container
```
```bash
docker start ssafyhub-backend-container
```

> 이름을 한 글자라도 틀리면 `No such container`가 난다. 복사해 붙이고 줄바꿈에 잘리지 않게 주의한다.

**② 이미지가 남아 있으면 Jenkins에서 재배포한다.**

```bash
docker images ssafyhub-backend --format '{{.Repository}}:{{.Tag}}'
```

Jenkins → `ssafyhub-backend` → `main` → **Build Now**.
컨테이너가 없으면 8080이 비어 있으므로 곧바로 성공한다.

**③ 둘 다 없으면 수동 JAR로 서비스를 먼저 살린다.**

```bash
cd /home/ec2-user && nohup ./start-backend.sh >> app.log 2>&1 &
```
```bash
sleep 20 && curl -s http://localhost:8080/actuator/health
```

서비스를 복구한 뒤 원인을 조사한다. **`/home/ec2-user/app.jar`와 `start-backend.sh`를 삭제하면
안 된다** — 마지막 안전망이다.

## 3. 배포가 트리거되지 않음

트리거 수단이 두 개다. 어느 쪽도 동작하지 않는지 확인한다.

```bash
docker logs jenkins --since 30m 2>&1 | grep -i "WebHook called"
```

웹훅 수신 기록이 있으면 배선은 정상이다.

| 확인 | 방법 |
|---|---|
| GitLab 웹훅 상태 | GitLab → Settings → Webhooks → **Recent Deliveries** 응답 코드 |
| 폴링 동작 | 빌드 원인이 `Started by an SCM change`면 폴링이 잡은 것 |
| 브랜치가 인덱싱됐나 | 아래 명령 |

```bash
docker exec jenkins sh -c 'ls -1 /var/jenkins_home/jobs/ssafyhub-backend/branches/'
```

`main`이 없으면 브랜치 필터나 스캔 문제다. Jenkins에서 **Scan Multibranch Pipeline Now**를 실행한다.

**노드가 오프라인이면 아무 트리거도 빌드를 시작하지 못한다.** 이 경우 빌드가 큐에 쌓이고
`Waiting for next available executor`가 표시된다 → [7절](#7-디스크-부족).

## 4. 빌드 실패

```bash
docker exec jenkins sh -c 'grep -aE "error:|BUILD FAILED|FAILURE|Caused by" /var/jenkins_home/jobs/ssafyhub-backend/branches/main/builds/<번호>/log | tail -20'
```

| 로그 패턴 | 원인 | 조치 |
|---|---|---|
| `error: constructor ... cannot be applied` | 프로덕션 코드 변경 후 테스트 미갱신 | 테스트 수정. `Docker Build`·`Deploy`가 연쇄로 스킵된다 |
| `Tests FAILED` | 테스트 실패 | 코드 수정 |
| `address already in use` | 8080을 다른 프로세스가 점유 | [6절](#6-수동-jar과-컨테이너-사이-전환) |
| `no space left on device` | 디스크 부족 | [7절](#7-디스크-부족) |
| `헬스체크가 실패했습니다` | 새 컨테이너 기동 실패 | 아래 |

헬스체크 실패는 파이프라인이 컨테이너 로그를 함께 출력한다. 대개 설정 문제다.

```bash
docker exec ssafyhub-backend-container printenv | grep -E '^(SPRING_PROFILES_ACTIVE|DB_URL|AWS_S3_BUCKET|DEVOPS_WAS_BASE_URL)='
```

필수 환경변수가 비어 있으면 Spring 컨텍스트 로딩부터 실패한다.

### 로컬에서 빌드를 미리 검증하기

Jenkins에 올리기 전에 같은 명령을 돌려볼 수 있다. 로컬에 JDK가 없으면
Jenkins 컨테이너의 워밍된 Gradle 캐시를 빌려 쓴다.

```bash
docker exec jenkins sh -c 'cp -a /var/jenkins_home/workspace/ssafyhub-backend_main /tmp/verify && rm -rf /tmp/verify/backend/build'
```
```bash
docker exec jenkins sh -c 'cd /tmp/verify/backend && ./gradlew build --console=plain 2>&1 | tail -20'
```
```bash
docker exec jenkins rm -rf /tmp/verify
```

Jenkins의 실제 워크스페이스는 건드리지 않는다.

## 5. 성공했는데 배포되지 않음

**빌드 `SUCCESS`는 배포 성공을 뜻하지 않는다.** `when` 조건으로 건너뛴 스테이지는 실패가 아니다.

```bash
docker exec jenkins sh -c 'grep -a "skipped due to" /var/jenkins_home/jobs/ssafyhub-backend/branches/main/builds/<번호>/log'
```

| 메시지 | 의미 |
|---|---|
| `Stage "Deploy" skipped due to when conditional` | 배포 조건 불일치 |
| `skipped due to earlier failure(s)` | 앞 단계 실패로 연쇄 스킵 |

배포 조건은 `branch 'main'` **AND** (`backend/**` 변경 **OR** 수동 실행 **OR** changelog 비어 있음)이다.

| 상황 | 배포 여부 |
|---|---|
| `main`에 `backend/**` 변경 | 배포 |
| `main`에 `docs/`만 변경 | 스킵 (정상) |
| `main` 아닌 브랜치 | 스킵 (정상) |
| **Build Now** 수동 실행 | 변경 내역과 무관하게 배포 |

**배포 여부는 빌드 결과가 아니라 결과물로 확인한다.**

```bash
docker ps --format '{{.Names}} {{.Image}}' | grep backend
```

프론트엔드는 S3 업로드 시각으로 확인한다.

```bash
aws s3 ls s3://ssafyhub-frontend/ --recursive
```

## 6. 수동 JAR과 컨테이너 사이 전환

8080을 수동 `java -jar`가 점유하면 Jenkins의 `docker run`이 실패한다. **이 교착은 순서로만 풀린다.**

```bash
sudo ss -tlnp | grep :8080
```
```bash
ps -eo pid,lstart,cmd | grep "java -jar" | grep -v grep
```

명령줄이 `java -jar /home/ec2-user/app.jar`(절대경로)면 **수동 프로세스**다.
`java -jar app.jar`(상대경로)면 컨테이너 내부 프로세스이므로 건드리지 않는다.

```bash
sudo cat /proc/<PID>/cgroup
```

`docker-...scope`가 보이면 컨테이너 프로세스다.

### 수동 → 컨테이너

```bash
sudo kill <PID>
```
```bash
sudo ss -tlnp | grep :8080
```

**출력이 비어야 한다.** 확인한 뒤 Jenkins에서 **Build Now**.

> 빌드가 이미 돌고 있을 때 중간에 `kill`하면 타이밍이 어긋나 `docker run`이 먼저 실행돼
> 포트 충돌로 실패할 수 있다. **포트를 비운 뒤에 빌드를 시작한다.**

systemd 서비스로 등록해 뒀다면 `kill`만으로는 되살아난다.

```bash
sudo systemctl stop ssafyhub-backend && sudo systemctl disable ssafyhub-backend
```

### 컨테이너 → 수동 (긴급 폴백)

```bash
docker stop ssafyhub-backend-container
```
```bash
cd /home/ec2-user && nohup ./start-backend.sh >> app.log 2>&1 &
```

## 7. 디스크 부족

**Jenkins는 여유 공간이 1GB 아래로 떨어지면 빌드 노드를 자동으로 오프라인 처리한다.**
그러면 모든 빌드가 큐에서 멈추고 `Waiting for next available executor`가 표시된다.
**executor 개수를 늘려도 해결되지 않는다.**

```bash
df -h /
```
```bash
docker system df
```

### 회수 순서 — 안전한 것부터

```bash
docker builder prune -af
```

빌드 캐시만 지운다. 실행 중 컨테이너에 영향 없음.

```bash
docker image prune -af
```

미사용 이미지. **실행 중·정지 상태 컨테이너가 참조하는 이미지는 보호된다.**

> `docker system prune -a`는 **정지된 컨테이너까지 삭제한다.** `-previous` 롤백 대상이
> 사라지므로 장애 대응 중에는 쓰지 않는다.

여전히 부족하면 Docker 밖을 찾는다.

```bash
sudo du -xh --max-depth=1 / 2>/dev/null | sort -h | tail -12
```
```bash
sudo du -sh /var/lib/docker/containers/*/*-json.log 2>/dev/null | sort -h
```

컨테이너 로그는 `docker system df`에 집계되지 않는다. 크면 비운다(재시작 불필요).

```bash
sudo truncate -s 0 /var/lib/docker/containers/*/*-json.log
```
```bash
sudo journalctl --disk-usage
```

### 노드 복귀

여유 1GB를 넘기면 Jenkins가 주기적 재검사로 자동 복귀시킨다. 즉시 반영하려면
`<젠킨스>/manage/computer/(built-in)/` → **Bring this node back online**.

### 볼륨 확장

반복되면 EBS를 늘린다. 무중단이다.

콘솔 → EC2 → Volumes → **Modify volume** → 크기 변경. 이후 인스턴스에서:

```bash
lsblk
```
```bash
sudo growpart /dev/nvme0n1 1
```
```bash
sudo xfs_growfs /
```

`NOCHANGE: partition 1 ... cannot be grown`은 오류가 아니라 **이미 확장됐다는 뜻**이다.
같은 볼륨은 수정 후 약 6시간 동안 재수정할 수 없으니 처음에 넉넉히 잡는다.

## 8. Outbox 정체

배포 요청이 `deploy-engine`으로 전달되지 않는 경우다.

```bash
docker exec ssafyhub-backend-container printenv DEVOPS_WAS_BASE_URL
```

**끝에 슬래시가 있으면 안 된다.** 경로가 `//api/deployments`가 되어 404가 날 수 있다.

```bash
URL=$(docker exec ssafyhub-backend-container printenv DEVOPS_WAS_BASE_URL)
```
```bash
curl -sS -o /dev/null -w 'HTTP %{http_code}\n' --max-time 5 "$URL/api/deployments"
```

`405`나 `400`이면 도달 성공(POST 전용에 GET을 보냈으므로 정상). `000`이면 네트워크·보안그룹 문제다.

> `deploy-engine` 주소가 `localhost`면 컨테이너에서는 절대 닿지 않는다.
> 컨테이너의 `localhost`는 자기 자신이다. 사설 IP를 써야 한다.

### DB 상태 확인

```bash
DBH=$(docker exec ssafyhub-backend-container printenv DB_URL | sed -E 's|^jdbc:mysql://([^:/]*).*|\1|')
```
```bash
mysql -h "$DBH" -u admin -p ssafyhub -e "SELECT status, COUNT(*) cnt, MAX(updated_at) FROM deployment_outbox GROUP BY status;"
```

| 상태 | 의미 | 조치 |
|---|---|---|
| `PENDING`이 계속 쌓임 | 스케줄러가 돌지 않음 | `DEPLOYMENT_OUTBOX_ENABLED` 확인 |
| `PUBLISHED` | 정상 | — |
| `FAILED` | **영구 종결.** 재시도 없음 | 아래 |
| `PROCESSING`이 오래 머무름 | 처리 중 프로세스가 죽은 고아 행 | 아래 |

```bash
mysql -h "$DBH" -u admin -p ssafyhub -e "SELECT id, deployment_id, status, attempt_count, LEFT(last_error,120) FROM deployment_outbox WHERE status <> 'PUBLISHED' ORDER BY id DESC LIMIT 10;"
```

### 되살리기

**주소를 바로잡은 뒤에** 실행한다.

```sql
UPDATE deployment_outbox
SET status = 'PENDING', next_attempt_at = NOW(), last_error = NULL
WHERE status IN ('FAILED', 'PROCESSING');
```

⚠️ 실패가 "요청 도달 전"이 아니라 "응답 수신 실패"였다면 재전송으로 **중복 배포**가 생길 수 있다.
건수가 많으면 `deployment_id`를 `deploy-engine` 담당자와 대조한 뒤 실행한다.

## 9. S3 업로드 실패

**Presigned URL 발급이 `200`이어도 업로드가 되는 것은 아니다.** 발급은 S3를 호출하지 않고
로컬에서 서명만 계산하므로, 자격증명이 잘못돼도 성공한다. 실패는 브라우저의 `PUT`에서 `403`으로 드러난다.

```bash
docker exec ssafyhub-backend-container printenv AWS_ACCESS_KEY_ID
```

**출력이 없어야 정상이다.** IAM 역할을 쓰는 구성에서 액세스 키를 설정하면 SDK 탐색 순서상
환경변수가 먼저 걸려 역할이 무시된다.

```bash
TOKEN=$(curl -sX PUT http://169.254.169.254/latest/api/token -H 'X-aws-ec2-metadata-token-ttl-seconds: 60')
```
```bash
curl -s -H "X-aws-ec2-metadata-token: $TOKEN" http://169.254.169.254/latest/meta-data/iam/security-credentials/
```

역할 이름이 나와야 한다. 컨테이너에서도 도달하는지 확인한다.

```bash
docker run --rm curlimages/curl:latest -s --max-time 5 -X PUT http://169.254.169.254/latest/api/token -H 'X-aws-ec2-metadata-token-ttl-seconds: 60'
```

토큰이 나오지 않으면 **IMDS hop limit이 1**일 가능성이 높다. 호스트에서는 되고 컨테이너에서만
깨지는 증상이다. `2`로 올려야 한다.

```bash
aws s3 ls s3://ssafyhub-artifacts/
```

`AccessDenied`면 키 문제가 아니라 IAM 정책 문제다.

## 10. 인증·CSRF 문제

| 증상 | 원인 |
|---|---|
| 전원이 갑자기 로그아웃 | `JWT_SECRET`이 변경됨. 기존 토큰 전부 무효 |
| 상태 변경 요청이 계속 `403 CSRF_001` | CSRF 토큰 누락·불일치 |
| 로컬은 되는데 배포 환경만 실패 | 쿠키 `Secure` 속성 차이 (프로덕션 `true`, 로컬 `false`) |

**`JWT_SECRET`을 바꾸면 전체 재로그인이 필요하다.** Jenkins 크리덴셜과 수동 기동 설정의 값이
다르면 배포 방식을 바꿀 때마다 로그아웃이 발생한다. 두 곳을 일치시킨다.

```bash
curl -s -i http://localhost:8080/api/v1/auth/csrf | grep -i "set-cookie"
```

`XSRF-TOKEN`이 내려오고 `HttpOnly`가 **아니어야** 한다. 자바스크립트가 읽어 헤더에 실어야 하기 때문이다.

## 11. 예방 점검

주 1회 정도 훑는다.

```bash
df -h /
```
```bash
docker images | wc -l
```
```bash
docker exec jenkins sh -c 'du -sh /var/jenkins_home/workspace/* 2>/dev/null | sort -h | tail -5'
```
```bash
mysql -h "$DBH" -u admin -p ssafyhub -e "SELECT status, COUNT(*) FROM deployment_outbox GROUP BY status;"
```

| 점검 | 기준 |
|---|---|
| 디스크 여유 | 5GB 이상 유지 |
| 백엔드 이미지 수 | 2~3개 (파이프라인이 자동 정리) |
| Outbox `FAILED`/`PROCESSING` | 0건 |
| `app.jar` / `start-backend.sh` | 존재 확인 (롤백 수단) |
| Mattermost 알림 | 최근 배포 알림 도착 여부 |

## 12. 판단 원칙

장애 대응 중 반복해서 유용했던 기준들이다.

- **결과물로 확인한다.** 빌드 `SUCCESS`, HTTP `200`, "발급 성공" 모두 최종 증거가 아니다.
  컨테이너가 떠 있는지, 객체가 올라갔는지, 행이 `PUBLISHED`인지를 본다.
- **서비스 복구를 원인 규명보다 앞세운다.** 수동 JAR로 먼저 살리고 나서 조사한다.
- **롤백 수단을 먼저 확인한다.** 무언가를 정지·삭제하기 전에 되돌릴 방법이 남아 있는지 본다.
  `docker system prune -a`처럼 범위가 넓은 명령은 그 수단을 함께 지운다.
- **호스트에서 되는 것이 컨테이너에서도 된다고 가정하지 않는다.** `localhost`와 IMDS가 이 경계에서 갈린다.
- **추측한 값을 확정처럼 쓰지 않는다.** 환경변수는 실행 중 프로세스에서 읽고, 시각은 로그에서 확인한다.

## 관련 문서

| 문서 | 내용 |
|---|---|
| [CI/CD 장애 기록](./cicd-incident-2026-07-29.md) | 이 런북이 나온 배경 |
| [포팅 매뉴얼](./porting-manual.md) | 환경변수, 배포 절차, 초기 구축 |
| [아키텍처](./architecture.md) | 시스템 구성과 네트워크 |
| [ERD](./erd.md) | 테이블과 상태 전이 |

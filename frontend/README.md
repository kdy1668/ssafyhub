# DeployHub Frontend (Vue)

`../front`(React 버전)과 동일한 스펙을 Vue 3 + Composition API로 구현한 버전입니다.

## 시작하기

```bash
npm install
cp .env.example .env
npm run dev
```

기본 포트는 `5174`입니다 (React 버전의 `5173`과 동시에 띄울 수 있도록 분리). `.env`의 `VITE_USE_MOCKS=true`(기본값) 상태에서는 백엔드 없이 모든 화면이 mock 데이터로 동작합니다.

로그인/회원가입은 `/api/users/login`, `/api/users/signup`을 호출하는 실제 API 연동입니다. 로그인 라우트 가드(`useAuthStore`)는 `localStorage`의 `auth_token` 존재 여부로 인증 상태를 판단하므로, 로그인 없이는 `/home`, `/deploy/new`, `/deployments`에 접근할 수 없습니다.

## React 버전과 다른 선택 (Vue 관용구에 맞춘 대응)

| React 버전 | Vue 버전 | 이유 |
|---|---|---|
| React Query | `@tanstack/vue-query` | 같은 팀이 만든 Vue 포팅판. API가 거의 동일해 `hooks/` → `composables/`로 그대로 대응됨 |
| Zustand | Pinia | Vue 생태계 표준 스토어. `defineStore`로 동일한 역할(모달 열림 상태, 인증 mock, 토스트 큐) 수행 |
| react-hook-form | 없음 (plain `reactive()` + 수동 검증) | Vue의 세분화된 반응성 덕분에 제어 입력이어도 리렌더링 비용 문제가 없어, react-hook-form이 해결하던 성능 이슈 자체가 Vue에는 없음. `DeployRequestPage.vue`에서 `reactive(form)` + `v-model`로 충분 |
| `<ProtectedRoute>` 래퍼 컴포넌트 | `router.beforeEach` 네비게이션 가드 | Vue Router는 라우트 `meta.requiresAuth` + 전역 가드가 인증 체크의 관용적인 방식이라 별도 래퍼 컴포넌트보다 자연스러움 |
| MSW 대신 경량 mock 함수 | 동일 | `src/api/*.js`는 프레임워크 비의존적이라 React 버전과 파일 내용이 완전히 동일함 |

## 폴더 구조

```
src/
├── api/            API 함수 (React 버전과 동일, mock/실서버 분기 포함)
├── components/
│   ├── common/     BaseButton, BaseInput, BaseSelect, BaseModal, BaseBadge, BaseCard, BaseSkeleton, ToastContainer
│   ├── landing/    LandingPage 하위 컴포넌트
│   ├── home/       HomePage 하위 컴포넌트
│   ├── deploy/     DeployRequestPage 하위 컴포넌트 (소스 탭, 환경변수, 미리보기 모달)
│   └── manage/     ManagePage 하위 컴포넌트 (테이블, 상태 뱃지, 리소스 모달)
├── composables/    vue-query 기반 서버 상태 훅
├── stores/         Pinia 스토어 (auth, modal, toast)
├── pages/          라우트에 매핑되는 페이지 (.vue)
└── router.js       라우팅 설정 (beforeEach 가드로 인증 체크)
```

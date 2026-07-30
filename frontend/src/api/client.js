import axios from "axios";

// 쿠키 기반 JWT 인증:
// - Access/Refresh Token은 HttpOnly 쿠키로 관리 (프론트에서 저장/조회하지 않음)
// - 브라우저가 요청 시 쿠키를 자동 전송 (withCredentials)
// - 상태 변경(POST/PUT/PATCH/DELETE) 요청은 XSRF-TOKEN 쿠키를 읽어 X-XSRF-TOKEN 헤더로 전송
//   (axios가 xsrfCookieName/xsrfHeaderName 설정으로 자동 처리)
export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "",
  withCredentials: true,
  // axios는 기본적으로 same-origin일 때만 XSRF 헤더를 붙인다(isURLSameOrigin 판정).
  // 명시하면 판정에 의존하지 않는다. VITE_API_BASE_URL은 빈 문자열(=상대경로)이어야
  // 하며, 다른 오리진을 넣으면 토큰이 cross-origin으로 전송되므로 주의한다.
  withXSRFToken: true,
  xsrfCookieName: "XSRF-TOKEN",
  xsrfHeaderName: "X-XSRF-TOKEN",
  headers: {
    "Content-Type": "application/json",
  },
});

// 세션 만료(재발급 실패) 시 실행할 콜백. main.js에서 등록 (스토어/라우터 순환 참조 방지).
let onSessionExpired = null;
export function setOnSessionExpired(fn) {
  onSessionExpired = fn;
}

function hasXsrfCookie() {
  return document.cookie.split("; ").some((c) => c.startsWith("XSRF-TOKEN="));
}

// 앱 시작 시(또는 XSRF 쿠키가 없을 때) CSRF 토큰을 발급받는다.
// 이후 상태 변경 요청은 발급된 쿠키를 계속 사용하므로 매 요청마다 호출할 필요는 없다.
export async function ensureCsrfToken(force = false) {
  if (!force && hasXsrfCookie()) return;
  try {
    await apiClient.get("/api/v1/auth/csrf");
  } catch {
    // 실패해도 이후 요청 흐름에서 재시도되므로 무시한다.
  }
}

function normalizeError(error) {
  const message =
    error.response?.data?.message || error.message || "요청 처리 중 오류가 발생했습니다.";
  const err = new Error(message);
  err.code = error.response?.data?.code;
  err.status = error.response?.status;
  return err;
}

// 여러 요청이 동시에 401을 받아도 재발급은 한 번만 수행하기 위한 단일 실행 프라미스.
let reissuePromise = null;

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    const status = error.response?.status;
    const code = error.response?.data?.code;
    const url = originalRequest?.url ?? "";

    const isReissueRequest = url.includes("/api/v1/auth/reissue");
    const isLoginRequest = url.includes("/api/v1/auth/login");

    // CSRF 토큰 누락/불일치로 인한 403 → 강제 재발급 후 원래 요청 1회 재시도.
    // 앱 시작 시의 발급 요청(main.js)은 await하지 않고 실패도 무시하므로,
    // 백엔드 순단이나 쿠키 소실로 XSRF-TOKEN이 없는 상태가 그대로 유지될 수 있다.
    // 이 블록이 없으면 페이지를 새로고침할 때까지 모든 상태 변경 요청이 403으로 실패한다.
    if (status === 403 && code === "CSRF_001" && !originalRequest._csrfRetry) {
      originalRequest._csrfRetry = true;
      await ensureCsrfToken(true);
      return apiClient(originalRequest);
    }

    // Access Token 만료로 추정되는 401 → 재발급 API 1회 호출 후 원래 요청 재시도
    if (
      status === 401 &&
      code === "AUTHENTICATION_REQUIRED" &&
      !originalRequest._retry &&
      !isReissueRequest &&
      !isLoginRequest
    ) {
      originalRequest._retry = true;
      try {
        if (!reissuePromise) {
          reissuePromise = apiClient.post("/api/v1/auth/reissue");
        }
        await reissuePromise;
        return apiClient(originalRequest);
      } catch (reissueError) {
        // 재발급 실패 → 재로그인 필요. 인증 상태 초기화 + 로그인 페이지 이동.
        if (onSessionExpired) onSessionExpired();
        return Promise.reject(normalizeError(reissueError));
      } finally {
        reissuePromise = null;
      }
    }

    return Promise.reject(normalizeError(error));
  }
);

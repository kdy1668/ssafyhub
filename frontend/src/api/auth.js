import { apiClient, ensureCsrfToken } from "./client";

// 백엔드 member 응답 { memberId, name, studentNumber, teamName, role } 을
// 프론트에서 쓰는 공통 형태 { id, name, studentNumber, team, role } 로 정규화한다.
function normalizeMember(member) {
  if (!member) return null;
  return {
    id: member.memberId,
    name: member.name,
    studentNumber: member.studentNumber,
    team: member.teamName,
    role: member.role,
  };
}

export async function signupRequest({ team, name, studentId, password, passwordConfirm }) {
  // 상태 변경 요청이므로 CSRF 토큰이 발급되어 있어야 한다.
  await ensureCsrfToken();

  const { data } = await apiClient.post("/api/v1/members/signup", {
    teamName: team,
    name,
    studentNumber: studentId,
    password,
    passwordConfirm,
  });
  // ApiResponse { success, code, message, data } 구조 → data(MemberResponse) 반환
  return data?.data;
}

export async function loginRequest({ studentId, password }) {
  // 로그인 요청 전 CSRF 토큰 확보 (POST 요청은 X-XSRF-TOKEN 헤더 필요)
  await ensureCsrfToken();

  // 백엔드 스펙: 학번은 studentNumber 로 전송. Access/Refresh 토큰은 응답 Set-Cookie로 설정됨.
  const { data } = await apiClient.post("/api/v1/auth/login", {
    studentNumber: studentId,
    password,
  });
  // ApiResponse.data = { member: {...} } → 정규화한 member 반환 (토큰은 쿠키에 있으므로 꺼내지 않음)
  return normalizeMember(data?.data?.member);
}

export async function logoutRequest() {
  // 상태 변경 요청이므로 CSRF 토큰이 발급되어 있어야 한다.
  await ensureCsrfToken();

  // Refresh Token 폐기와 쿠키 삭제(Set-Cookie; Max-Age=0)는 서버가 처리한다.
  // 토큰은 HttpOnly 쿠키라서 프론트에서 직접 지울 수 없다.
  await apiClient.post("/api/v1/auth/logout");
}

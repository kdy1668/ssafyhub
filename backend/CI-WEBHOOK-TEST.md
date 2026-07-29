# 웹훅 트리거 검증용 임시 파일

backend/ 아래에만 변경을 발생시켜 다음 세 가지를 확인한다.

1. GitLab 웹훅(/project/ssafyhub-backend)이 main 푸시에 즉시 반응하는지
   -- pollSCM 3분 주기가 아니라 즉시 시작되는지로 판별한다.
2. 백엔드 Deploy 가 changeset 'backend/**' 조건으로 실행되는지
3. 프론트엔드 Deploy 가 changeset 'frontend/**' 불일치로 건너뛰어지는지

컴파일 대상이 아니므로 빌드 산출물에는 영향이 없다. 확인 후 삭제 예정.

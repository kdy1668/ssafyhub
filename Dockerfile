# 1. 빌드에 사용할 자바 이미지 (프로젝트 Java 버전에 맞게 수정, 예: 17 또는 21)
FROM openjdk:17-jdk-slim

# 2. 작업 디렉터리 설정
WORKDIR /app

# 3. 빌드된 jar 파일을 컨테이너 내부로 복사
# (Gradle 빌드 결과물 경로: build/libs/*.jar)
COPY build/libs/*.jar app.jar

# 4. 서버 포트 개방 (스프링부트 기본 포트)
EXPOSE 8080

# 5. 애플리케이션 실행 명령어
ENTRYPOINT ["java", "-jar", "app.jar"]
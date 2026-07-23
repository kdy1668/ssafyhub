# Java 21로 빌드된 Spring Boot 애플리케이션을 실행하는 런타임 이미지
FROM eclipse-temurin:21-jre

WORKDIR /app

# build.gradle에서 이름을 고정한 실행 가능한 Boot JAR만 복사
COPY build/libs/app.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

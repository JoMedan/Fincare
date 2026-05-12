# ── Stage 1: Build ───────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

# Gradle wrapper와 의존성 정의 파일만 먼저 복사 → 소스 변경 시 재다운로드 방지
COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle .
COPY settings.gradle .

RUN chmod +x gradlew

# 의존성 다운로드 (build.gradle이 바뀌지 않으면 캐시 재사용)
RUN ./gradlew dependencies --no-daemon

# 소스 복사 후 빌드
COPY src/ src/
RUN ./gradlew bootJar -x test --no-daemon

# ── Stage 2: Run ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

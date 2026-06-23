# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM maven:3.9.7-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Cache dependency layer separately from source — avoids full re-download on code changes
COPY pom.xml .
RUN mvn dependency:go-offline -q

COPY src ./src
RUN mvn clean package -DskipTests -q

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# curl is required by the Docker Compose healthcheck
RUN apk add --no-cache curl

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

# Container-optimised JVM flags:
#   -XX:+UseContainerSupport   — respect cgroup CPU/memory limits
#   -XX:MaxRAMPercentage=75    — use up to 75 % of container RAM for heap
#   -Djava.security.egd=...    — faster SecureRandom (avoids /dev/random blocking)
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]

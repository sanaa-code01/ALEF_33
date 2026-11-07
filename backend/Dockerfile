# ─────────────────────────────────────────────────────────────
# ALEF 33 backend: 2-stage build
#   1. "build" uses Maven + JDK 21 to compile and package the jar
#   2. the final image only has a Java 21 runtime + the jar (smaller, safer)
# ─────────────────────────────────────────────────────────────

# ---- Stage 1: build the jar ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Download dependencies first: this layer is cached as long as pom.xml doesn't change
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

# Then compile the code (tests run in CI, not here)
COPY src ./src
RUN mvn -q -B package -DskipTests

# ---- Stage 2: run ----
FROM eclipse-temurin:21-jre
WORKDIR /app

# Don't run as root inside the container
RUN groupadd --system app && useradd --system --gid app --no-create-home app

COPY --from=build /app/target/*.jar app.jar
USER app

EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]

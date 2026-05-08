# ============================================================================
# EzWallet Backend - Multi-stage Dockerfile
# ============================================================================

# ----- Stage 1: Build -----
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

COPY pom.xml ./
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ----- Stage 2: Runtime -----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S ezwallet && adduser -S ezwallet -G ezwallet
USER ezwallet

COPY --from=builder /build/target/ezwallet.jar app.jar

EXPOSE 8080
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

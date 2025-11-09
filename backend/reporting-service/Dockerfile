# syntax=docker/dockerfile:1.7

ARG MAVEN_IMAGE=maven:3.9.7-eclipse-temurin-17
ARG RUNTIME_IMAGE=eclipse-temurin:17-jre

# ====== Build Stage ======
FROM --platform=$BUILDPLATFORM ${MAVEN_IMAGE} AS build
ARG SERVICE_DIR=reporting-service
WORKDIR /workspace

COPY common-security/pom.xml common-security/pom.xml
RUN mvn -q -f common-security/pom.xml -DskipTests dependency:go-offline

COPY ${SERVICE_DIR}/pom.xml ${SERVICE_DIR}/pom.xml

COPY common-security/src common-security/src
RUN mvn -q -f common-security/pom.xml -DskipTests install

COPY ${SERVICE_DIR}/src ${SERVICE_DIR}/src
RUN mvn -q -f ${SERVICE_DIR}/pom.xml -DskipTests package

# ====== Runtime Stage ======
FROM ${RUNTIME_IMAGE} AS runtime
ARG SERVICE_DIR=reporting-service
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0" \
    APP_HOME=/app \
    SPRING_PROFILES_ACTIVE=prod
WORKDIR ${APP_HOME}

LABEL org.opencontainers.image.title="reporting-service" \
      org.opencontainers.image.source="https://github.com/hacisimsek/real-time-order-system" \
      org.opencontainers.image.description="Reporting Service runtime image" \
      org.opencontainers.image.licenses="MIT"

RUN groupadd --system spring && \
    useradd --system --home ${APP_HOME} --gid spring --shell /usr/sbin/nologin spring

COPY --from=build /workspace/${SERVICE_DIR}/target/*.jar app.jar
RUN chown spring:spring app.jar

USER spring
EXPOSE 8084

HEALTHCHECK --interval=15s --timeout=3s --start-period=30s \
  CMD wget -qO- http://localhost:${PORT:-8084}/actuator/health | grep '"status":"UP"'

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]

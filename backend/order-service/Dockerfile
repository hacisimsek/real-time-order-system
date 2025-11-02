# ====== Build Stage ======
FROM maven:3.9.7-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY common-security/pom.xml common-security/pom.xml
RUN mvn -q -f common-security/pom.xml -DskipTests dependency:go-offline

COPY order-service/pom.xml order-service/pom.xml

COPY common-security/src common-security/src
RUN mvn -q -f common-security/pom.xml -DskipTests install

COPY order-service/src order-service/src
RUN mvn -q -f order-service/pom.xml -DskipTests package

# ====== Runtime Stage ======
FROM eclipse-temurin:17-jre
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
WORKDIR /app

# Uygulama jar'ını kopyala
COPY --from=build /workspace/order-service/target/*.jar app.jar

# Healthcheck -> Spring actuator
HEALTHCHECK --interval=15s --timeout=3s --start-period=25s \
  CMD sh -c "wget -qO- http://localhost:${PORT:-8081}/actuator/health | grep '\"status\":\"UP\"'"

# Not: Spring portlarını application.yml belirliyor (8081/8082/8083)
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]

# ====== Build Stage ======
FROM maven:3.9.7-eclipse-temurin-17 AS build
WORKDIR /app

# Bağımlılık cache'i (pom -> deps)
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

# Kaynak kod
COPY src ./src
RUN mvn -q -DskipTests package

# ====== Runtime Stage ======
FROM eclipse-temurin:17-jre
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
WORKDIR /app

# Uygulama jar'ını kopyala
COPY --from=build /app/target/*.jar app.jar

# Healthcheck -> Spring actuator
HEALTHCHECK --interval=15s --timeout=3s --start-period=25s \
  CMD sh -c "wget -qO- http://localhost:${PORT:-8081}/actuator/health | grep '\"status\":\"UP\"'"

# Not: Spring portlarını application.yml belirliyor (8081/8082/8083)
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]

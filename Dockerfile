FROM gradle:8.14.6-jdk17 AS build
WORKDIR /workspace
COPY . .
RUN gradle :server:bootJar --no-daemon

FROM eclipse-temurin:17-jre
RUN useradd --system --create-home appuser
WORKDIR /app
COPY --from=build /workspace/server/build/libs/server-0.1.0.jar /app/app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

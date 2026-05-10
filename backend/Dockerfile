FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
# Cache dependency resolution layer separately from the source copy
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S appuser && adduser -S appuser -G appuser
WORKDIR /app
COPY --from=build --chown=appuser:appuser /app/target/*.jar app.jar
RUN mkdir -p /app/logs && chown appuser:appuser /app/logs
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

FROM node:24-alpine AS node
FROM maven:3.9.11-eclipse-temurin-21-alpine AS build
COPY --from=node /usr/local/ /usr/local/
WORKDIR /workspace
COPY backend/pom.xml backend/pom.xml
COPY frontend/package.json frontend/package-lock.json frontend/
COPY frontend/ frontend/
COPY backend/src/ backend/src/
RUN mvn -f backend/pom.xml -B -ntp package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S sectio && adduser -S sectio -G sectio
COPY --from=build /workspace/backend/target/*.jar app.jar
USER sectio
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]


FROM maven:3.9.11-eclipse-temurin-21-alpine AS build
WORKDIR /workspace
COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline
COPY src ./src
RUN mvn -B -ntp package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S sectio && adduser -S sectio -G sectio
COPY --from=build /workspace/target/*.jar app.jar
USER sectio
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]


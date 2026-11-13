FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S resolveit && adduser -S resolveit -G resolveit
COPY --from=build /app/target/*.jar app.jar
RUN mkdir -p /app/uploads && chown -R resolveit:resolveit /app
USER resolveit
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]


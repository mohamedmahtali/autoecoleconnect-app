# Étape 1 — build Maven
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# Étape 2 — image d'exécution JRE 21
FROM eclipse-temurin:21-jre
RUN groupadd --system spring && useradd --system --gid spring spring
USER spring
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

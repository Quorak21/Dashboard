# Build stage
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests

# Run stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Création d'un groupe et d'un utilisateur non-root pour la sécurité
RUN groupadd -g 1001 app && useradd -u 1001 -g app -s /bin/false app

# Copie du jar avec attribution de propriété à l'utilisateur non-root
COPY --from=build --chown=app:app /app/target/*.jar app.jar

# Changement d'utilisateur pour l'exécution
USER app

EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["java", "-jar", "app.jar"]

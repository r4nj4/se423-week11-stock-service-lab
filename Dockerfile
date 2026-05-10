# ----------- Build stage -----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Cache deps first so iterative builds are fast
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ----------- Runtime stage -----------
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/stock-service.jar /app/stock-service.jar

# Render injects PORT at runtime; Main.java reads it.
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/stock-service.jar"]

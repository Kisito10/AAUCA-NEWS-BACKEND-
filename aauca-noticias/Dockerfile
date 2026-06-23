FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests && find /app -name "*.jar" -not -name "*sources*"

FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY --from=build /app/microservicio-noticias/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
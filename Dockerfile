FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=builder /app/target/bunker-party-1.0.0.jar app.jar
COPY static ./static

EXPOSE 8000

ENV PORT=8000

CMD ["java", "-jar", "app.jar"]

ARG DOCKER_REGISTRY=
FROM ${DOCKER_REGISTRY}maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

COPY .mvn/settings.xml /root/.m2/settings.xml
COPY pom.xml .

RUN mvn dependency:go-offline -q

COPY src ./src

RUN mvn clean package -DskipTests -q

FROM ${DOCKER_REGISTRY}eclipse-temurin:17-jre-focal

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8330

CMD ["java", "-jar", "app.jar"]

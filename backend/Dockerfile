ARG DOCKER_REGISTRY_PREFIX=docker.io
FROM ${DOCKER_REGISTRY_PREFIX}/library/maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
ARG MAVEN_MIRROR_PROFILE=aliyun
COPY maven-settings /opt/maven-settings
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -s /opt/maven-settings/${MAVEN_MIRROR_PROFILE}.xml package -Dmaven.test.skip=true

ARG DOCKER_REGISTRY_PREFIX=docker.io
FROM ${DOCKER_REGISTRY_PREFIX}/library/eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /workspace/target/homestay-backend-0.1.0-SNAPSHOT.jar /app/app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]

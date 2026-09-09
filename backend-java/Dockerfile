FROM maven:3.9-eclipse-temurin-8 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src src
RUN mvn -q -DskipTests package

FROM python:3.12-slim-bookworm
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends openjdk-17-jre-headless \
    && rm -rf /var/lib/apt/lists/*
COPY deepagent-runtime/requirements.txt /app/deepagent-runtime/requirements.txt
RUN python3 -m pip install --no-cache-dir -r /app/deepagent-runtime/requirements.txt
COPY deepagent-runtime /app/deepagent-runtime
COPY --from=build /build/target/llmaif-backend-*.jar /app/app.jar
ENV DEEPAGENT_PYTHON=python3
ENV DEEPAGENT_RUNTIME_SCRIPT=/app/deepagent-runtime/runtime.py
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

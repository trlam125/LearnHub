FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src src
COPY scripts/Healthcheck.java scripts/Healthcheck.java
RUN mvn -B verify && javac -d /tmp/health scripts/Healthcheck.java
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --gid 10001 learnhub && useradd --uid 10001 --gid 10001 --create-home learnhub && mkdir -p /app/data /app/private-media && chown -R learnhub:learnhub /app
COPY --from=build --chown=learnhub:learnhub /build/target/learnhub-chinese-1.0.0.jar /app/app.jar
COPY --from=build --chown=learnhub:learnhub /tmp/health /app/health
USER learnhub
ENV BIND_ADDRESS=0.0.0.0
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=8s --start-period=60s --retries=3 CMD ["java","-cp","/app/health","Healthcheck"]
ENTRYPOINT ["java","-jar","/app/app.jar"]

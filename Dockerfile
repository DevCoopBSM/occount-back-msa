FROM eclipse-temurin:21-jdk AS builder

WORKDIR /workspace

COPY gradle gradle
COPY gradlew gradlew
COPY gradlew.bat gradlew.bat
COPY settings.gradle settings.gradle
COPY build.gradle build.gradle
COPY gradle.properties gradle.properties
COPY core core
COPY domains domains
COPY gateway gateway
COPY modules modules

RUN chmod +x gradlew

ARG APP_MODULE
ARG APP_BUILD_DIR
ARG APP_JAR_NAME

RUN ./gradlew "${APP_MODULE}:bootJar" --no-daemon --console=plain \
    && APP_JAR="${APP_BUILD_DIR}/${APP_JAR_NAME}" \
    && ls -l "${APP_BUILD_DIR}" \
    && test -n "${APP_JAR}" \
    && test -f "${APP_JAR}" \
    && cp "${APP_JAR}" /tmp/app.jar

FROM eclipse-temurin:21-jre

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

COPY --from=builder /tmp/app.jar /app/app.jar

ENV JAVA_OPTS=""

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]

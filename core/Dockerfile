# syntax=docker/dockerfile:1
ARG PROJECT=core

FROM eclipse-temurin:25-jdk AS build

ARG PROJECT

WORKDIR /workspace/app

COPY . .

RUN --mount=type=cache,target=/root/.gradle chmod +x ./gradlew && ./gradlew ${PROJECT}:build -x test

RUN mkdir -p ${PROJECT}/build/dependency && (cd ${PROJECT}/build/dependency; jar -xf ../libs/*.jar)

FROM eclipse-temurin:25-jre-alpine AS runtime

ARG PROJECT

ARG DEPENDENCY=/workspace/app/${PROJECT}/build/dependency

RUN addgroup -S nonroot && adduser -S nonroot -G nonroot

RUN apk upgrade --no-cache p11-kit p11-kit-trust

COPY --from=build ${DEPENDENCY}/BOOT-INF/lib /app/lib

COPY --from=build ${DEPENDENCY}/META-INF /app/META-INF

COPY --from=build ${DEPENDENCY}/BOOT-INF/classes /app

EXPOSE 8080

USER nonroot

ENTRYPOINT ["java","-cp","app:app/lib/*","br.com.fiap.pos.tech_challenge.core.TechChallengeApplication"]
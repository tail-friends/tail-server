# 꼬리친구 서버 이미지. 빌드 단계에서 jar를 만들고, 실행 단계에는 JRE와 jar만 둔다.

FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# 의존성 먼저 받아 두면 소스만 바뀌었을 때 이 층을 다시 쓴다
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN ./gradlew --no-daemon dependencies > /dev/null

COPY src src
RUN ./gradlew --no-daemon bootJar -x test

FROM eclipse-temurin:21-jre
RUN useradd --system --uid 1001 --no-create-home tailfriends
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar
USER tailfriends
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

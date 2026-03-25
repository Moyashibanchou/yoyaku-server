FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /workspace

COPY pom.xml .
COPY src ./src

RUN mvn -B -DskipTests package && \
    # spring-boot-maven-plugin により `original-*.jar` が生成されることがあるため、
    # 実行用（先頭が Artifact- のもの）だけを app.jar にリネームする
    cp target/Artifact-*.jar target/app.jar

FROM openjdk:17-jdk-slim
WORKDIR /app

COPY --from=build /workspace/target/app.jar app.jar

EXPOSE 8080

# Render は環境変数 PORT を設定することが多いので、それがあれば優先します
ENTRYPOINT ["sh", "-c", "java -jar /app/app.jar --server.port=${PORT:-8080}"]


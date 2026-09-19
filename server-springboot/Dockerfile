# Giai đoạn biên dịch: dùng JDK 21 tương ứng với maven.compiler.source trong pom.xml.
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /build
COPY pom.xml .
RUN mvn -B dependency:go-offline -DskipTests

COPY src ./src
RUN mvn -B clean package -DskipTests

# Giai đoạn chạy chỉ cần JRE, giúp image nhỏ hơn image Maven/JDK.
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app
COPY --from=build /build/target/*.jar /app/app.jar

RUN mkdir -p /app/storage
EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]

# ====================================================================
# 1. СТРОИТЕЛЬНЫЙ ЭТАП (Builder Stage)
# Используем официальный Maven + JDK 21
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

# Копируем pom.xml
COPY pom.xml .

# Предзагружаем зависимости (ускоряет сборку)
RUN mvn -B dependency:go-offline

# Копируем исходники
COPY src /app/src

# Сборка проекта
RUN mvn clean package -DskipTests -Dmaven.deploy.skip=true

# ====================================================================

# 2. ПРОИЗВОДСТВЕННЫЙ ЭТАП (Production Stage)
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Копируем jar
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

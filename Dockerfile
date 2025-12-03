# 1. СТРОИТЕЛЬНЫЙ ЭТАП (Build Stage)
# Используем полный JDK, чтобы собрать приложение
FROM eclipse-temurin:21-jdk-jammy AS builder

# Указываем рабочую директорию
WORKDIR /app

# Копируем pom.xml и исходники
COPY pom.xml .
COPY src /app/src

# Запускаем сборку Maven. Флаг -DskipTests пропускает тесты для быстрой сборки.
# Вы можете заменить 'mvn' на 'gradle' если используете Gradle
RUN ./mvnw clean package -DskipTests

# ----------------------------------------------------------------------

# 2. ПРОИЗВОДСТВЕННЫЙ ЭТАП (Production Stage)
# Используем минимальную JRE для запуска
FROM eclipse-temurin:21-jre-jammy

# Указываем рабочую директорию
WORKDIR /app

# Копируем готовый JAR-файл из строительного этапа
# В Spring Boot JAR обычно находится в target/<имя_проекта>-<версия>.jar
# Замените <имя-вашего-jar> на фактическое имя вашего jar-файла.
COPY --from=builder /app/target/<имя-вашего-jar>.jar app.jar

# Открываем порт, который используется вашим Spring-приложением (по умолчанию 8080)
EXPOSE 8080

# Команда для запуска приложения
ENTRYPOINT ["java", "-jar", "app.jar"]
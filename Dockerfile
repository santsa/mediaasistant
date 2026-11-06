# ==========================================================================
# Build stage — Maven + JDK 25 to compile the project
# ==========================================================================
FROM maven:3.9-eclipse-temurin-25 AS builder

WORKDIR /build

# First, copy only pom.xml and download dependencies.
# This uses the Docker cache: if pom.xml does not change between builds,
# dependencies do not need to be downloaded again.
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Now copy the source code and compile the project.
COPY src ./src
RUN mvn clean package -DskipTests -B

# ==========================================================================
# Runtime stage — JRE 25 only on Ubuntu Jammy
# ==========================================================================
FROM eclipse-temurin:25-jre-jammy

# Timezone: containers start in UTC by default.
# Set it explicitly so logs and LocalDate.now() use the correct timezone.
ENV TZ=Europe/Madrid
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Non-root user to run the app.
RUN groupadd -r spring && useradd -r -g spring spring

WORKDIR /app

# Copy ONLY the JAR built in the previous stage.
# The JDK, Maven, and source code are excluded from this image.
COPY --from=builder /build/target/*.jar app.jar

# Give the non-root user ownership of the directory.
RUN chown -R spring:spring /app

USER spring

# Production profile is hardcoded: this image IS the production image.
ENV SPRING_PROFILES_ACTIVE=aws

# Port exposed by the app (documentation only; not published automatically).
EXPOSE 8080

# ENTRYPOINT uses MaxRAMPercentage so the JVM can adapt dynamically
# to the container's memory.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
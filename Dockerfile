# One Dockerfile builds all three services:
#   docker build --target admin  -t chocomall-admin  .
#   docker build --target portal -t chocomall-portal .
#   docker build --target search -t chocomall-search .

# ---- build ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY . .
RUN mvn -B -q -DskipTests package

# ---- shared runtime ----
FROM eclipse-temurin:17-jre AS runtime
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app && useradd --system --gid app --no-create-home app
WORKDIR /app
USER app
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -Duser.timezone=UTC"

FROM runtime AS admin
COPY --from=build --chown=app:app /workspace/choco-admin/target/choco-admin-*.jar app.jar
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --start-period=60s --retries=5 CMD curl -fs http://localhost:8080/actuator/health | grep -q UP || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]

FROM runtime AS portal
COPY --from=build --chown=app:app /workspace/choco-portal/target/choco-portal-*.jar app.jar
EXPOSE 8085
HEALTHCHECK --interval=15s --timeout=3s --start-period=60s --retries=5 CMD curl -fs http://localhost:8085/actuator/health | grep -q UP || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]

FROM runtime AS search
COPY --from=build --chown=app:app /workspace/choco-search/target/choco-search-*.jar app.jar
EXPOSE 8081
HEALTHCHECK --interval=15s --timeout=3s --start-period=60s --retries=5 CMD curl -fs http://localhost:8081/actuator/health | grep -q UP || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]

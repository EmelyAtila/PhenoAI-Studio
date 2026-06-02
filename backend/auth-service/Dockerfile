FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S phenoai && adduser -S phenoai -G phenoai

WORKDIR /app

COPY build/libs/auth-service-*.jar app.jar

RUN chown phenoai:phenoai app.jar

USER phenoai

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]

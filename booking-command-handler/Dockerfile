FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/*-exec.jar app.jar
EXPOSE 8080
HEALTHCHECK --interval=20s --timeout=5s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/management/healthcheck || exit 1
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=60.0", \
  "-jar", "app.jar"]

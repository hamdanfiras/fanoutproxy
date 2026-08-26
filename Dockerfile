FROM eclipse-temurin:21-jre

WORKDIR /app
COPY target/fanoutproxy-0.0.1-SNAPSHOT.jar /app/fanoutproxy.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/fanoutproxy.jar"]

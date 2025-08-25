FROM amazoncorretto:17
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar
ENV SPRING_PROFILES_ACTIVE="dev"
ENTRYPOINT ["java","-Xms128m", "-Xmx512m","-XX:+HeapDumpOnOutOfMemoryError" , "-jar",  "/app.jar"]

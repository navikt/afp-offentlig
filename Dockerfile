FROM gcr.io/distroless/java21-debian12:nonroot
COPY build/libs/afp-offentlig-1.0.0.jar app.jar
ENV LOGGING_CONFIG=classpath:logback-spring.xml

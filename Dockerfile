FROM ghcr.io/navikt/baseimages/temurin:18
COPY build/libs/afp-offentlig-1.0.0.jar app.jar
ENV LOGGING_CONFIG=classpath:logback-spring.xml

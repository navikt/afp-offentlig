FROM gcr.io/distroless/java21-debian12:nonroot

ENV TZ="Europe/Oslo"
EXPOSE 8080

WORKDIR /app

COPY build/libs/afp-offentlig-1.0.0.jar .

ENTRYPOINT ["java", "-jar", "afp-offentlig-1.0.0.jar"]

#CMD ["app.jar"]
#ENTRYPOINT [ "java", "-jar", "/app/app.jar" ]
#CMD ["-jar", "/app/app.jar"]
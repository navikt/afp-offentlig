FROM gcr.io/distroless/java21-debian12:nonroot

ENV TZ="Europe/Oslo"

COPY build/libs/afp-offentlig-1.0.0.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
CMD ["/app.jar"]

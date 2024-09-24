  # official iso openJDK
    FROM openJDK: 17-jdk-slim


# working dirr
  WORKDIR /app
  #copy compiled jar-fail into container.
  COPY target/limitTransactsApi.jar app.jar

  # command for app start
  ENTRYPOINT ["java", "-jar", "app.jar"]
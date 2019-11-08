FROM adoptopenjdk/openjdk8:jdk8u222-b10

RUN mkdir /opt/app

COPY target/blessings-dev.jar /opt/app

CMD ["java" , "-jar", "/opt/app/blessings-dev.jar"]
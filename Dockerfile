FROM adoptopenjdk/openjdk11:alpine-jre
ADD target/javakubecontroller-0.0.1-SNAPSHOT.jar customController.jar
ENTRYPOINT ["java","-jar","customController.jar"]
FROM java:8
MAINTAINER Sebastian Göndör

# Install maven
RUN apt-get update
RUN apt-get install -y maven

WORKDIR /build

# Compile and package jar
ADD pom.xml /build/pom.xml
ADD src /build/src
RUN ["mvn", "clean"]
RUN ["mvn", "install"]

# Dependencies
ADD target/gsls-0.2.2.jar gsls-0.2.2.jar

EXPOSE 4001
EXPOSE 4002

ENTRYPOINT ["java", "-jar", "gsls-0.2.2.jar"]

FROM java:8
MAINTAINER Sebastian Göndör

# Install maven
#RUN apt-get update
#RUN apt-get install -y maven

WORKDIR /build

# Dependencies
#ADD pom.xml /build/pom.xml
ADD target/gsls-0.2.0.jar gsls-0.2.0.jar

# Compile and package jar
#ADD src /build/src
#RUN ["mvn", "--version"]
#RUN ["mvn", "clean"]
#RUN ["mvn", "install"]

EXPOSE 4001
EXPOSE 4002

ENTRYPOINT ["java", "-jar", "gsls-0.2.0.jar"]
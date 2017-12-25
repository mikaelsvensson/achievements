FROM openjdk:8-jre

ADD target/server-application-1.0-SNAPSHOT.jar /opt/achievements/server-application-1.0-SNAPSHOT.jar
COPY docker-startup.sh /opt/achievements/
COPY environments/* /opt/achievements/

WORKDIR /opt/achievements
CMD /opt/achievements/docker-startup.sh

EXPOSE 8080
EXPOSE 9067

FROM flowdocker/play:0.0.21

ADD . /opt/play

WORKDIR /opt/play

RUN sbt 'project www' clean stage

ENTRYPOINT ["java", "-jar", "/root/environment-provider.jar", "run", "play", "dependency", "www", "target/universal/stage/bin/dependency-www"]

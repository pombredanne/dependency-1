FROM flowdocker/play:0.0.30

ADD . /opt/play

WORKDIR /opt/play

RUN sbt 'project api' clean stage

ENTRYPOINT ["java", "-jar", "/root/environment-provider.jar", "run", "play", "dependency", "api", "target/universal/stage/bin/dependency-api"]

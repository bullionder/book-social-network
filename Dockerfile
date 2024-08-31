FROM ubuntu:latest
LABEL authors="CGHQ78"

RUN apt update
RUN #apt install -y openjdk-17-jre
RUN apt install -y openjdk-17-jdk

ENTRYPOINT ["top", "-b"]
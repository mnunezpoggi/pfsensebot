FROM sapmachine:17

RUN apt-get update \
  && apt-get install -y ca-certificates curl git sshpass openssh-client rsync --no-install-recommends \
  && rm -rf /var/lib/apt/lists/*

# common for all images
ENV MAVEN_HOME /usr/share/maven

COPY --from=maven:3.9.6-eclipse-temurin-11 ${MAVEN_HOME} ${MAVEN_HOME}
COPY --from=maven:3.9.6-eclipse-temurin-11 /usr/local/bin/mvn-entrypoint.sh /usr/local/bin/mvn-entrypoint.sh
COPY --from=maven:3.9.6-eclipse-temurin-11 /usr/share/maven/ref/settings-docker.xml /usr/share/maven/ref/settings-docker.xml

RUN ln -s ${MAVEN_HOME}/bin/mvn /usr/bin/mvn
ARG USERNAME=pfsensebot
ARG USER_UID=1000
ARG USER_GID=$USER_UID

## Create the user
RUN groupadd --gid $USER_GID $USERNAME \
    && useradd --uid $USER_UID --gid $USER_GID -m $USERNAME

ARG MAVEN_VERSION=3.9.6
ARG USER_HOME_DIR="/home/$USERNAME"
ENV MAVEN_CONFIG "$USER_HOME_DIR/.m2"

RUN mkdir -p /usr/src/app
COPY / /usr/src/app
COPY src /usr/src/app/src
RUN chown -R $USERNAME /usr/src/app
USER $USERNAME
WORKDIR /usr/src/app/
RUN git clone https://github.com/mnunezpoggi/chatbot.git
WORKDIR /usr/src/app/chatbot
RUN mvn -T 1C clean install && rm -rf target
WORKDIR /usr/src/app/

#RUN mvn clean package
RUN mvn compile
#ENTRYPOINT ["java"]
#CMD ["-jar", "/usr/src/app/target/rsyncbot-jar-with-dependencies.jar"]

ENTRYPOINT ["/usr/local/bin/mvn-entrypoint.sh"]
CMD ["mvn", "exec:java", "-Dexec.mainClass=xyz.kraftwork.pfsensebot.Pfsensebot"]
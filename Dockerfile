FROM discoenv/javabase

USER root
VOLUME ["/etc/iplant/de"]

COPY conf/main/logback.xml /home/iplant/
COPY target/iplant-groups-standalone.jar /home/iplant/
RUN chown -R iplant:iplant /home/iplant/

ARG git_commit=unknown
ARG buildenv_git_commit=unknown
ARG version=unknown
LABEL org.iplantc.de.iplant-groups.git-ref="$git_commit" \
      org.iplantc.de.iplant-groups.version="$version" \
      org.iplantc.de.buildenv.git-ref="$buildenv_git_commit"

USER iplant
ENTRYPOINT ["java", "-Dlogback.configurationFile=/etc/iplant/de/logging/iplant-groups-logging.xml", "-cp", ".:iplant-groups-standalone.jar:/home/iplant/", "iplant_groups.core"]
CMD ["--help"]

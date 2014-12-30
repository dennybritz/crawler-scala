FROM blikk/base-crawler

# The port Akka listens on
EXPOSE 10080

ENV JAVA_OPTS -Dfile.encoding=UTF-8
ENV SBT_OPTS -Dfile.encoding=UTF-8
ENV JAVA_TOOL_OPTIONS -Dfile.encoding=UTF8

WORKDIR /opt/crawler
RUN git pull

RUN sbt "crawler-backend/stage"
CMD ["./crawler-backend/target/universal/stage/bin/crawler-backend"]
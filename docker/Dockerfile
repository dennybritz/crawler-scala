FROM blikk/base-crawler

# The port Akka listens on
EXPOSE 10080

WORKDIR /opt/crawler
RUN git pull

RUN sbt "crawler-backend/stage"
CMD ["./crawler-backend/target/universal/stage/bin/crawler-backend"]
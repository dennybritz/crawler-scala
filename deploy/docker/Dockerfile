FROM centos:centos7
MAINTAINER Denny Britz <denny@blikk.co>

# Expose the API port
EXPOSE 8080

# Mount some volumes
# TODO: Nothing needed yet

# Install packages
RUN yum -y groupinstall "Development Tools"
RUN yum -y install wget java java-devel
RUN wget http://dl.bintray.com/sbt/rpm/sbt-0.13.5.rpm; rpm -i sbt-0.13.5.rpm;

# Copy over the keys
# Note: Keys are not included in the git repository!
ADD blikk_key.pem /root/.ssh/id_rsa
ADD blikk_key.pem.pub /root/.ssh/id_rsa.pub
RUN chmod 600 /root/.ssh/id_rsa

# Pull code from github
RUN echo -e "Host github.com\n\tStrictHostKeyChecking no\n" >> /root/.ssh/config
RUN git clone git@github.com:dennybritz/blikk-crawler.git /blikk-crawler

# Set application environment variables
ENV BLIKK_CRAWLER_HOME /blikk-crawler

# Build the application
# Need to break the cache for git pull
ADD http://www.random.org/strings/?num=10&len=8&digits=on&upperalpha=on&loweralpha=on&unique=on&format=plain&rnd=new uuid
WORKDIR /blikk-crawler
RUN git pull
RUN sbt compile
RUN sbt stage

# Set Java options 
# TODO

# Run the application
CMD crawler-backend/target/start
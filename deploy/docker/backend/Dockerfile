FROM centos:centos7
MAINTAINER Denny Britz <denny@blikk.co>

# Mount some volumes
# TODO: Nothing needed yet

# Install EPEL repo
RUN yum -y install wget
RUN wget http://dl.fedoraproject.org/pub/epel/beta/7/x86_64/epel-release-7-1.noarch.rpm
RUN rpm -ivh epel-release-7-1.noarch.rpm
RUN yum -y update

# Install packages
RUN yum -y groupinstall "Development Tools"
RUN yum -y install java java-devel
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

# Compile
WORKDIR /blikk-crawler
RUN sbt compile
RUN sbt stage

# Set Java options 
# TODO

# Add the run script
ADD ./run.sh /run.sh
ENTRYPOINT ["/run.sh"]
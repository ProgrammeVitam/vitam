#########################################################################################
# Dockerfile to build vitam and run on one server
# Based on Debian
#
# Maintained by Vitam Integration Team
# Image name: vitam/dev-deb-base
#########################################################################################

# Set the base image to Debian 8
FROM debian:9.5
MAINTAINER French Prime minister Office/SGMAP/DINSIC/Vitam Program <contact.vitam@culture.gouv.fr>

#ENV DEBIAN_FRONTEND noninteractive

RUN apt-get clean && apt-get update && apt-get install -y apt-utils

# Make sure the package repository and packages are up to date.

COPY deb/stretch-backports.list /etc/apt/sources.list.d

RUN apt-get clean && apt-get update && apt-get upgrade -y

RUN apt-get update && apt-get install -t stretch-backports -y openjdk-8-jdk-headless

RUN apt-get install -y systemd-sysv

################################  Configure systemd  ###############################

# Hint for systemd that we are running inside a container
ENV container docker

RUN cd /lib/systemd/system/sysinit.target.wants/ && \
		ls | grep -v systemd-tmpfiles-setup.service | xargs rm -f && \
		rm -f /lib/systemd/system/sockets.target.wants/*udev* && \
		systemctl mask -- \
			tmp.mount \
			etc-hostname.mount \
			etc-hosts.mount \
			etc-resolv.conf.mount \
			-.mount \
			swap.target \
			getty.target \
			getty-static.service \
			dev-mqueue.mount \
			systemd-tmpfiles-setup-dev.service \
			systemd-tmpfiles-setup.service \
			systemd-remount-fs.service \
			systemd-update-utmp-runlevel.service \
			systemd-logind.service && \
		systemctl set-default multi-user.target || true

################################  Install build tools (rpm / maven / java)  ###############################

RUN apt-get clean && apt-get update && apt-get install -y openssl

# to bypass the "trustAnchors parameter must be non-empty" problem
# See http://stackoverflow.com/questions/6784463/error-trustanchors-parameter-must-be-non-empty
RUN /var/lib/dpkg/info/ca-certificates-java.postinst configure

RUN apt-get clean && apt-get update && apt-get install -y curl && apt-get clean

# Add Java to path
ENV JAVA_HOME /usr/lib/jvm/java-8-openjdk-amd64

# Install & configure maven
RUN curl -k http://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.3.9/apache-maven-3.3.9-bin.tar.gz --output /tmp/maven.tgz \
   && tar xvzf /tmp/maven.tgz --directory /opt \
   && rm -f /tmp/maven.tgz \
   && ln -s /opt/apache-maven-3.3.9 /opt/maven \
   && mkdir -p /devhome/.m2 \
   && chmod -R 777 /devhome

# Add Maven & java to path
ENV M2_HOME /opt/maven
ENV PATH ${M2_HOME}/bin:${JAVA_HOME}/bin:${PATH}

################################  Install ansible  ###############################
# for sudo in automatic deployment ; note : ansible needs epel repo

RUN apt-get update && apt-get install -y sudo net-tools \
  && apt-get clean


# Editor & unzip
RUN apt-get install -y vim unzip wget

RUN apt-get install -y -t stretch-backports golang

RUN apt-get install -y node-gyp  nodejs-dev && \
    apt-get install -y nodejs && \
    ln -s /usr/bin/nodejs /usr/bin/node

RUN apt-get -t stretch-backports -y install ansible

RUN apt-get clean && apt-get update && apt-get install -y git vim

##################################  Declare local deb repo  #################################

# Note : declare at the end ; else other commands will fail.
RUN apt-get update && apt-get install -y  dpkg-dev \
    && apt-get clean

COPY deb/local.list /etc/apt/sources.list.d/local.list

################################## Add dev helpers #################################

COPY deb/vitam-build-repo /usr/bin
COPY vitam-deploy /usr/bin
COPY vitam-deploy-extra /usr/bin
COPY deb/vitam-maven-build-only /usr/bin
COPY vitam-redeploy /usr/bin
COPY vitam-redeploy-extra /usr/bin
COPY vitam-command /usr/bin
RUN  chmod a+x /usr/bin/vitam-*

# KWA review : Why remove this script ?
COPY deb/profile-build-repo.sh /etc/profile.d
COPY profile-prompt-usage.sh /etc/profile.d
COPY .bashrc /devhome
COPY vitam-usage.txt /etc
COPY deb/sudo-nopwd /etc/sudoers.d
RUN chmod 664 /etc/sudoers.d/sudo-nopwd

ENV TERM xterm

# If we don't install openjdk a 2nd time, sometimes it doesn't work, why ???
RUN apt-get install -t stretch-backports -y openjdk-8-jdk-headless

##################################  CONTAINER SETTINGS  #################################

# VOLUME [ "/sys/fs/cgroup" ]
VOLUME [ "/code" ]
VOLUME [ "/devhome/.m2" ]

WORKDIR /code

#ENV DEBIAN_FRONTEND teletype

# Entry Point to systemd init
CMD ["/sbin/init"]

COPY vitam-stop /usr/bin
COPY vitam-start /usr/bin
COPY vitam-restart /usr/bin
COPY vitam-bench-ingest /usr/bin
COPY vitam-mongo-cli /usr/bin
COPY vitam-mongo-cli-rs /usr/bin
COPY deb/vitam-recreate-repo /usr/bin

RUN  chmod a+x /usr/bin/vitam-*

# System tuning for elasticsearch
RUN echo "vm.max_map_count = 262144" >> /etc/sysctl.conf

COPY deb/vitam-build-griffins /usr/bin

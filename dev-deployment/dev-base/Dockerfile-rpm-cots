#########################################################################################
# Dockerfile to run vitam on one server
# Based on CentOS
#
# Maintained by Vitam Integration Team
# Image name: vitam/jenkins-slave-base
#########################################################################################

# Set the base image to Centos 7
FROM centos:7.4.1708
MAINTAINER French Prime minister Office/SGMAP/DINSIC/Vitam Program <contact.vitam@culture.gouv.fr>

# Make sure the package repository and packages are up to date.
RUN yum install -y epel-release && yum -y update && yum -y upgrade && yum clean all

################################  Configure systemd  ###############################

# Hint for systemd that we are running inside a container
ENV container docker

# Remove useless units
RUN (cd /lib/systemd/system/sysinit.target.wants/; for i in *; do [ $i == systemd-tmpfiles-setup.service ] || rm -f $i; done); \
    rm -f /lib/systemd/system/multi-user.target.wants/*;\
    rm -f /etc/systemd/system/*.wants/*;\
    rm -f /lib/systemd/system/local-fs.target.wants/*; \
    rm -f /lib/systemd/system/sockets.target.wants/*udev*; \
    rm -f /lib/systemd/system/sockets.target.wants/*initctl*; \
    rm -f /lib/systemd/system/basic.target.wants/*;\
    rm -f /lib/systemd/system/anaconda.target.wants/*;

################################  Install build tools (rpm / maven / java)  ###############################

RUN yum install -y \
    	java-1.8.0-openjdk-devel \
    	rpm-build \
        rpmdevtools \
        initscripts.x86_64 \
        golang \
        npm \
    && yum clean all

# Add Java to path
ENV JAVA_HOME /usr/lib/jvm/java

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

################################  Install deployment tools  ###############################

# for sudo in automatic deployment ; note : ansible needs epel repo
RUN yum install -y sudo ansible openssl which && yum clean all

##################################  install git and vim  #################################

RUN yum install -y git vim && yum clean all

##################################  Declare local rpm repo  #################################

# Note : declare at the end ; else other yum commandes will fail. Ideally, we would need a "createrepo" here, but it wouldn't work, as the volume /code is normally mounter externally.
RUN yum install -y createrepo \
    && yum clean all
COPY rpm/devlocal.repo /etc/yum.repos.d/devlocal.repo

################################## Add dev helpers #################################

COPY rpm/vitam-build-repo /usr/bin
COPY vitam-deploy /usr/bin
COPY vitam-deploy-extra /usr/bin
COPY rpm/vitam-maven-build-only /usr/bin
COPY vitam-redeploy /usr/bin
COPY vitam-redeploy-extra /usr/bin
COPY vitam-command /usr/bin
COPY rpm/vitam-deploy-cots /usr/bin
RUN chmod a+x /usr/bin/vitam-*

COPY rpm/profile-build-repo.sh /etc/profile.d
COPY profile-prompt-usage.sh /etc/profile.d
COPY .bashrc /devhome
COPY vitam-usage.txt /etc
COPY rpm/wheel-nopwd /etc/sudoers.d
RUN chmod 664 /etc/sudoers.d/wheel-nopwd

# Disable fastestmirror ; helps gain several seconds per yum invocation
COPY rpm/fastestmirror.conf /etc/yum/pluginconf.d

ENV TERM xterm

##################################  CONTAINER SETTINGS  #################################

VOLUME [ "/sys/fs/cgroup" ]
VOLUME [ "/code" ]
VOLUME [ "/devhome/.m2" ]

WORKDIR /code

# Entry Point to systemd init
CMD ["/usr/sbin/init"]

COPY vitam-stop /usr/bin
COPY vitam-start /usr/bin
COPY vitam-restart /usr/bin
COPY vitam-bench-ingest /usr/bin
COPY vitam-mongo-cli /usr/bin
COPY vitam-mongo-cli-rs /usr/bin
COPY rpm/vitam-recreate-repo /usr/bin

RUN chmod a+x /usr/bin/vitam-*
RUN chmod 644 /etc/sudoers.d/wheel-nopwd

COPY rpm/vitam-build-griffins /usr/bin

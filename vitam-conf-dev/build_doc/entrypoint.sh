#!/bin/bash

# Fix user home dir permissions
sudo chown -R $(id -u):$(id -g) ~/

# Init env vars
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64/
export M2_HOME=/usr/local/maven
export PATH="$PATH:$M2_HOME/bin"
cd /code

# This will exec the CMD
exec "$@"
#!/bin/bash

source /vitam/conf/${unix.name}/sysconfig/timer_java_opts

JAVA_ENTRYPOINT=/vitam/lib/${unix.name}/storage-access-log-backup-${project.version}.jar
/usr/bin/env java -jar $JAVA_OPTS $JAVA_ENTRYPOINT

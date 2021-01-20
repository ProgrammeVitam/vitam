#!/usr/bin/env bash

source /vitam/conf/${unix.name}/sysconfig/timer_java_opts

JAVA_ENTRYPOINT=/vitam/lib/${unix.name}/functional-administration-accession-register-core-${project.version}.jar
/usr/bin/env java -jar $JAVA_OPTS $JAVA_ENTRYPOINT

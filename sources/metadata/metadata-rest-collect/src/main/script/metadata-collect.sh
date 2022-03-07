#!/usr/bin/env bash

# Fork the java service
/usr/bin/env java $JAVA_OPTS $JAVA_ENTRYPOINT $JAVA_ARGS &
# Write the PID for systemd
echo $! > /vitam/run/${unix.name}/${unix.name}.pid
exit 0

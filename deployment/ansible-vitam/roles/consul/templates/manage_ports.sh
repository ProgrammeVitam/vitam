#!/usr/bin/env bash

if (( ${#} < 1 )); then
    echo "Usage: ${0} [create|remove]"
    exit 1
fi

function manage_ports {
    local ACTION=${1}
    for TCP_PORT in 9900 29900 8500 8302 8301 8300; do
        sudo semanage port ${ACTION} -t vitam_consul_port_t -p tcp ${TCP_PORT}
    done
    for UDP_PORT in 8301 8302; do
        sudo semanage port ${ACTION} -t vitam_consul_port_t -p udp ${UDP_PORT}
    done
}

# Main
if [ ${1} = "create" ]; then
    manage_ports "-a"
elif [ ${1} = "delete" ]; then
    manage_ports "-d"
else
    echo "No actions"
fi

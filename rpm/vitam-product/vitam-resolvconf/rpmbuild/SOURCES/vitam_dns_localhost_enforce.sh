#!/usr/bin/env bash


######################################################################
############################### Vars  ################################
######################################################################

RESOLVCONF="/etc/resolv.conf"
TMP_RESOLVCONF="${RESOLVCONF}.tmp"
TMP_RESOLVCONF_2="${TMP_RESOLVCONF}2"

CONSUL_ADDR="127.0.0.1"


######################################################################
############################# Functions ##############################
######################################################################

# logger
function log_stdout {
    if (( ${#} >= 2 )); then
        local ERR_LEVEL="${1}"
        local MESSAGE="${2}"
    else
        local ERR_LEVEL="INFO"
        local MESSAGE="${1}"
    fi
    echo "[${ERR_LEVEL}] [$(basename ${0}): ${FUNCNAME[ 1 ]}] ${MESSAGE}" 1>&2
}

# Delete file function
function delete_file_if_exists {
    local FILE=${1}
    if [ -f ${FILE} ]; then
        rm -f ${FILE}
    fi
}

# Check if consul dns entrie exists in resolv.conf and if it's the first or not
function check_resolvconf_file {
    # Init nameserver check vars (which are global)
    LOCALHOST_FIRST=false
    LOCALHOST_FOUND=false
    # Init local vars
    local FILE=${1}
    local FIRST_DNS=true
    for DNS in $(grep --perl-regexp "^[\s]*nameserver" ${FILE} | awk '{print $2}'); do
        if [ ${FIRST_DNS} = true ]; then
            if [ ${DNS} = "${CONSUL_ADDR}" ]; then
                # consul is the first in the dns list
                LOCALHOST_FOUND=true
                LOCALHOST_FIRST=true
            fi
            FIRST_DNS=false
        else
            if [ ${DNS} = "${CONSUL_ADDR}" ]; then
                # consul is is found but not the fist
                LOCALHOST_FOUND=true
                LOCALHOST_FIRST=false
            fi
        fi
    done
}

# Check if the rotate directive is absent, as it is not supported by Vitam
# http://man7.org/linux/man-pages/man5/resolv.conf.5.html
function check_rotate_absent {
    ROTATE_PRESENT=false
    local FILE=${1}

    grep -q --perl-regexp "^[\s]*rotate" ${FILE}
    if [ ${?} -eq 0 ]; then
        ROTATE_PRESENT=true
    fi
}


######################################################################
#############################    Main    #############################
######################################################################

# Check if resolv.conf file is a symlink, if it is, update the RESOLVCONF var
if [ -L ${RESOLVCONF} ]; then
    readlink -e ${RESOLVCONF} >/dev/null 2>&1
    if [ ${?} -gt 0 ]; then
        log_stdout "ERROR" "${RESOLVCONF} file is a symlink and is broken !"
        exit 1
    fi
    RESOLVCONF=$(readlink -e ${RESOLVCONF})
fi

# Delete old tmp files if they exists
delete_file_if_exists ${TMP_RESOLVCONF}
delete_file_if_exists ${TMP_RESOLVCONF_2}

# Check if there is no rotate directive in the file
check_rotate_absent ${RESOLVCONF}

if [ ${ROTATE_PRESENT} = true ]; then
    log_stdout "Configuration NOT supported by VITAM: 'rotate' directive in resolv.conf file"
    exit 1
fi

# Check the resolv.conf file
check_resolvconf_file ${RESOLVCONF}

# consul DNS entry is present and is the first => nothing to do
if [ ${LOCALHOST_FIRST} = true ] && [ ${LOCALHOST_FOUND} = true ]; then
    log_stdout "Consul DNS entry found and is the first"
    exit 0
fi

# Check if consul DNS entry is present ot not
if [ ${LOCALHOST_FOUND} = true ]; then
    log_stdout "Consul DNS entry found but is not the first"
    # Create a temp resolv.conf file without the consul DNS entry
    grep -v --perl-regexp "^[\s]*nameserver[\s]*${CONSUL_ADDR}" ${RESOLVCONF} > ${TMP_RESOLVCONF}
else
    log_stdout "No consul DNS entry found"
    # Create a temp resolv.conf file
    cp -f ${RESOLVCONF} ${TMP_RESOLVCONF}
fi


# Add the consul DNS at the first position of the nameserver entries
FIRST_NAMESERVER=true
while read LINE; do
    # Check if the line contains a valid nameserver entry
    echo ${LINE} | grep -q --perl-regexp "^[\s]*nameserver"
    if [ ${?} -eq 0 ] && [ ${FIRST_NAMESERVER} = true ]; then
        echo "nameserver ${CONSUL_ADDR}" >> ${TMP_RESOLVCONF_2}
        FIRST_NAMESERVER=false
    fi
    echo ${LINE} >> ${TMP_RESOLVCONF_2}
done < ${TMP_RESOLVCONF}

# Should never happen: No nameserver line found ! -> Add consul entry at the end of the file
if [ ${FIRST_NAMESERVER} = true ]; then
    log_stdout "WARN" "No nameserver entry found on resolv.conf file"
    echo "nameserver ${CONSUL_ADDR}" >> ${TMP_RESOLVCONF_2}
fi


# Check the resolv.conf tmp file
check_resolvconf_file ${TMP_RESOLVCONF_2}


# Check if the conf is OK, if OK replace the /etc/resolv.conf file by the new one
if [ ${LOCALHOST_FIRST} = true ] && [ ${LOCALHOST_FOUND} = true ]; then
    log_stdout "Consul DNS entry added and check is OK"
    # Atomic move of resolv.conf file to manage potential concurrent access
    mv -f ${TMP_RESOLVCONF_2} ${RESOLVCONF}
    delete_file_if_exists ${TMP_RESOLVCONF}
    delete_file_if_exists ${TMP_RESOLVCONF_2}
    exit 0
else
    log_stdout "ERROR" "Consul DNS entry added but check is KO"
    exit 1
fi

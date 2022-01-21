#!/bin/sh

##########################################################################
# Role:                                                                  #
# Scan a single file using clamav anti-virus                             #
##########################################################################
# Args:                                                                  #
# - file to scan                                                         #
##########################################################################
# Return:                                                                #
# - 0: scan OK - no virus                                                #
RET_NOTVIRUS=0
# - 1: virus found and corrected                                         #
RET_VIRUS_FOUND_FIXED=1
# - 2: virus found but not corrected                                     #
RET_VIRUS_FOUND_NOTFIXED=2
# - 3: Fatal scan not performed                                          #
RET_FAILURE=3
# stdout : names of virus found (1 per line) if virus found ;            #
#          failure description if failure                                #
# stderr : full ouput of clamav                                          #
##########################################################################

# Default return code : scan NOK
RET=3
OUTPUT_DIR=$(mktemp -d)
if [ $# -ne 1 ]; then # Argument number must be one
  echo "ERROR : $# parameter(s) provided, only one parameter is needed"
else # one argument, let's go
  if [ ! -f "$1" ];then # if the file wich will be scan is existing, keep going
    echo "ERROR : \"$1\" doesn't exit"
  else
    clamdscan -z --stream "$1" 1> ${OUTPUT_DIR}/stdout 2> ${OUTPUT_DIR}/stderr # scanning the file and store the output OUTPUT
    RET=$? # return code of clamscan

    # Always output clamscan outputs to our own stderr
    (>&2 cat ${OUTPUT_DIR}/stdout  ${OUTPUT_DIR}/stderr)

    if [ ${RET} -eq ${RET_VIRUS_FOUND_FIXED} ] ; then
      RET=2 # if virus found clamscan return 1; the script must return 2
      (>&1 cat ${OUTPUT_DIR}/stdout  | grep `basename ${1}` | cut -d ' ' -f 2) # sending the list of virus to our own stdout
    elif [ ${RET} -eq 2 ] ; then
      RET=3 # if scan not performed clamscan return 2; the script must return 3
      (>&1 cat ${OUTPUT_DIR}/stdout  | grep `basename ${1}` | cut -d ' ' -f 2-) # sending the failure reason to our own stdout
    fi

    if [ -f "${OUTPUT_DIR}/stdout" ]
    then
      rm ${OUTPUT_DIR}/stdout
    fi
    if [ -f "${OUTPUT_DIR}/stderr" ]
    then
      rm ${OUTPUT_DIR}/stderr
    fi
  fi
fi
rmdir ${OUTPUT_DIR}
exit ${RET}

#!/bin/sh

##########################################################################
# Role:                                                                  #
# Scan un single file using clamav anti-virus                            #
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
	if [ ! -f  "$1" ];then # if the file which will be scaned is existing, keep going
		echo "ERROR : \"$1\" doesn't exit"
	else
		RET=$(cat "$1") # return code of scan virus script
	fi
fi
exit ${RET}

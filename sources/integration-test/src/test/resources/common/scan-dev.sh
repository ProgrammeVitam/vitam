#!/bin/sh
set -u

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

if [ $# -ne 1 ]; then
  printf 'Usage: %s <file>\n' "$(basename "$0")" >&2
  exit 3
fi

file="$1"

# Check file existence/readability
if [ ! -r "$file" ]; then
  # file missing or not readable
  exit 3
fi

# The canonical EICAR test string (68 bytes)
eicar='X5O!P%@AP[4\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*'

# Use grep treating binary files as text (-a), fixed-string (-F) and quiet (-q).
# If match found, return 2; otherwise return 0.
if grep -a -F -q -- "$eicar" "$file"; then
  exit 2
fi

exit 0

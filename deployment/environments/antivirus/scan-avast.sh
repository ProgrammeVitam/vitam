#!/bin/bash

##########################################################################
# Role:                                                                  #
# Scan a single file using Avast Business Antivirus for Linux            #
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
##########################################################################
# For verbose mode, you can add -a parameter to the scan command.        #
##########################################################################

# Default return code : scan NOK
RET=3
if [ $# -ne 1 ]; then # Argument number must be one
  echo "ERROR : $# parameter(s) provided, only one parameter is needed"

else # one argument, let's go
  SIP=$1
  if [ ! -f "$SIP" ];then # if the file wich will be scan is existing, keep going
    echo "ERROR : \"$SIP\" doesn't exit"

  else
    START_TIME=$(date +%s)
    WORKING_DIR=$(mktemp -d -p /vitam/tmp/ingest-external/)
    chown -R vitam:avast $WORKING_DIR
    chmod -R 770 $WORKING_DIR

    echo "$(date +"%Y-%m-%d %T") - scanning $SIP" |& tee -a ${WORKING_DIR}/scan.log

    scan -bifup $SIP &>> ${WORKING_DIR}/scan.log
    RET=$? # return code of scan
    if [ $RET != 0 ]; then
      RET=$(($RET+1)) # Increase the return code to fit the expected Vitam's scan code
    fi

    # Specific case for scan not performed properly (probably because of big files)
    if [ $RET == 3 ]; then

      CORRUPTED=`cat ${WORKING_DIR}/scan.log | grep 'ZIP archive is corrupted'`
      if [ -z "$CORRUPTED" ]; then

        # Specific case for big files
        # Example output for zip: avast: /vitam/data/ingest-external/upload/SIP-Perf-1-1x500M.zip|>content/ID13.tmp: Compressed file is too big to be processed
        # Example output for tar.gz: avast: /vitam/data/ingest-external/upload/SIP-Perf-1-1x500M.tar.gz|>SIP-Perf-1-1x500M.tar: Compressed file is too big to be processed
        # Example output for tar.bz2: avast: /vitam/data/ingest-external/upload/SIP-Perf-1-1x500M.tar.bz2|>{bzip}: Compressed file is too big to be processed
        BIG_FILES=`cat ${WORKING_DIR}/scan.log | grep 'Compressed file is too big to be processed' | cut -d'>' -f2 | cut -d':' -f1`

        for BIG_FILE in $BIG_FILES
        do

          TYPE_SIP=$(file -b --mime-type $SIP)

          TMP_FILE=$WORKING_DIR/$BIG_FILE

          if [ "$TYPE_SIP" == 'application/zip' ]; then
            unzip $SIP $BIG_FILE -d $WORKING_DIR |& tee -a ${WORKING_DIR}/scan.log
          elif [ "$TYPE_SIP" == 'application/x-tar' ]; then
            tar xvf $SIP --directory $WORKING_DIR $BIG_FILE |& tee -a ${WORKING_DIR}/scan.log
          elif [ "$TYPE_SIP" == 'application/gzip' ] || [ "$TYPE_SIP" == 'application/x-gzip' ]; then
            mkdir -p $TMP_FILE
            tar xvzf $SIP --directory $TMP_FILE |& tee -a ${WORKING_DIR}/scan.log #uncompress the entire archive
            # gunzip -c $SIP > $TMP_FILE |& tee -a ${WORKING_DIR}/scan.log
          elif [ "$TYPE_SIP" == 'application/x-bzip2' ]; then
            mkdir -p $TMP_FILE
            tar xvjf $SIP --directory $TMP_FILE |& tee -a ${WORKING_DIR}/scan.log #uncompress the entire archive
          else
            echo "ERROR: $SIP: mime-type $TYPE_SIP is not supported" |& tee -a ${WORKING_DIR}/scan.log
            break # exit loop
          fi

          scan -bifup $TMP_FILE &>> ${WORKING_DIR}/scan.log
          RET=$? # return code of scan
          if [ $RET != 0 ]; then
            RET=$(($RET+1)) # Increase the return code to fit the expected Vitam's scan code
            break # exit loop
          fi
          rm -rf $TMP_FILE # cleaning temporary file
        done

      fi
    fi

    # Catch global output reason
    if [ $RET == 0 ]; then
      RET_MSG='[OK]'
    elif [ $RET == 1 ]; then
      RET_MSG='[OK: Virus found and fixed]'
    elif [ $RET == 2 ]; then
      REASON=$(cat ${WORKING_DIR}/scan.log | tail --lines=1 | cut -f2-)
      RET_MSG="[KO: Virus found - $REASON]"
    elif [ $RET == 3 ]; then
      REASON=$(cat ${WORKING_DIR}/scan.log | tail --lines=1 | cut -d":" -f3- | xargs)
      RET_MSG="[ERROR: Scan not performed - $REASON]"
    fi

    END_TIME=$(date +%s)
    EXECUTION_TIME=$(($END_TIME - $START_TIME))
    echo -e "$SIP\t$RET_MSG - execution time: ${EXECUTION_TIME}s" |& tee -a ${WORKING_DIR}/scan.log

    cat ${WORKING_DIR}/scan.log >> /vitam/log/avast/scan.log # Stores output in global log file for avast; helpful for debugging purpose
    rm -rf ${WORKING_DIR} # cleaning temporary working dir
  fi
fi
exit $RET

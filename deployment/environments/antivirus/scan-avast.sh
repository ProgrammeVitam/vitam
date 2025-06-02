#!/bin/bash

################################################################################
# Role:                                                                        #
# Scan a single file using Avast Business Antivirus for Linux                  #
################################################################################
# Args:                                                                        #
# - file to scan                                                               #
################################################################################
# Expected Vitam's return values:                                              #
# - 0: scan OK - no virus                                                      #
RET_OK=0
# - 1: virus found and corrected                                               #
RET_VIRUS_FOUND_FIXED=1
# - 2: virus found but not corrected                                           #
RET_VIRUS_FOUND_NOTFIXED=2
# - 3: Fatal scan not performed                                                #
RET_FAILURE=3
# Default return code : scan NOK                                               #
RET=$RET_FAILURE
################################################################################
# See `scan -h` command for all the available parameters                       #
# Keep at least the -J parameter for JSON output                               #
SCAN_PARAMS='-Jifup'
# For verbose mode, you can remove the -q parameter to the unzip command       #
UNZIP_PARAMS=''
# Set regex pattern for ignored warnings (with -J in SCAN_PARAMS)              #
IGNORED_PATTERN='The file is a decompression bomb'
################################################################################

custom_scan () {
  local FILE_TO_SCAN="$1"

  declare -A ignored_list
  declare -A rejected_list
  declare -A too_big_files
  declare -A virus_detections

  scan $SCAN_PARAMS "$FILE_TO_SCAN" &>> ${WORKING_DIR}/scan.log
  local ret_code=$?
  # The exit status is 0 if no infected files are found and 1 otherwise.
  # If an error occurred, the exit status is 2.
  # Infected status takes precedence over error status, thus a scan where some file could not be scanned and some infection was found returns 1.

  # Properly handle REASON without -J param
  if [[ $ret_code -eq 2 ]]; then
    REASON="Rejected files found !"
  elif [[ $ret_code -eq 1 ]]; then
    REASON="Virus found !"
    ret_code=$(($ret_code+1)) # Increase the return code to fit the expected Vitam's scan code
  fi

  # Analyse JSON logs from scan
  while read -r line; do
    [[ "$line" =~ ^\{.*\}$ ]] || continue
    echo "$line"

    path=$(jq -r 'if .path[1] then .path[1] else .path[0] end // empty' <<< "$line")

    # Handle virus detections
    if jq -e '.virus? // empty' <<< "$line" > /dev/null; then
      # Without -i parameter to the SCAN_PARAMS, the searched field is .virus
      virus=$(jq -r '.virus' <<< "$line")
    elif jq -e '.detections? // empty' <<< "$line" > /dev/null; then
      # With -i parameter to the SCAN_PARAMS, the searched field is .detections
      virus=$(jq -c '.detections' <<< "$line")
    else
      virus=""
    fi
    if [[ -n "$path" && -n "$virus" ]]; then
      virus_detections["$path"]="$virus"
    fi

    # Handle warnings with classification
    warning_str=$(jq -r '.warning_str // empty' <<< "$line")
    if [[ -n "$path" && -n "$warning_str" ]]; then
      if [[ "$warning_str" == "Compressed file is too big to be processed" ]]; then
        too_big_files["$path"]="$warning_str"
      elif [[ -n "$IGNORED_PATTERN" && "$warning_str" =~ $IGNORED_PATTERN ]]; then
        ignored_list["$path"]="$warning_str"
      else
        rejected_list["$path"]="$warning_str"
      fi
    fi
  done < "${WORKING_DIR}/scan.log"

  # --- Print summaries ---
  if (( ${#ignored_list[@]} > 0 )); then
    ret_code=$RET_VIRUS_FOUND_FIXED
    REASON="${#ignored_list[@]} warnings found but ignored."
    echo "INFO: ${REASON}" |& tee -a ${WORKING_DIR}/scan.log
    for path in "${!ignored_list[@]}"; do
      echo "  $path: ${ignored_list[$path]}" |& tee -a ${WORKING_DIR}/scan.log
    done
  fi

  if (( ${#rejected_list[@]} > 0 )); then
    ret_code=$RET_VIRUS_FOUND_NOTFIXED
    REASON="${#rejected_list[@]} rejected files found !"
    echo "ERROR: ${REASON}" |& tee -a ${WORKING_DIR}/scan.log
    for path in "${!rejected_list[@]}"; do
      echo "  $path: ${rejected_list[$path]}" |& tee -a ${WORKING_DIR}/scan.log
    done
  fi

  if (( ${#virus_detections[@]} > 0 )); then
    ret_code=$RET_VIRUS_FOUND_NOTFIXED
    REASON="${#virus_detections[@]} Virus found !"
    echo "ERROR: $REASON" |& tee -a ${WORKING_DIR}/scan.log
    for path in "${!virus_detections[@]}"; do
      echo "  $path: ${virus_detections[$path]}" |& tee -a ${WORKING_DIR}/scan.log
    done
  fi

  # Do not loop over big files if there are already detected errors
  if [[ $ret_code -ne $RET_VIRUS_FOUND_NOTFIXED ]]; then
    if (( ${#too_big_files[@]} > 0 )); then
      echo "INFO: ${#too_big_files[@]} files could not be processed due to size." |& tee -a ${WORKING_DIR}/scan.log
      echo "INFO: Starting individual scan..." |& tee -a ${WORKING_DIR}/scan.log
      for path in "${!too_big_files[@]}"; do
        unzip_and_scan "$FILE_TO_SCAN" "$path"
        # Exit loop if rc != 0
        if [ $? -ne 0 ]; then
          ret_code=$RET_VIRUS_FOUND_NOTFIXED
          break
        fi
      done
    fi
  fi

  return $ret_code
}

unzip_and_scan () {
  local ARCHIVE="$1"
  local FILE="$2"

  echo "INFO: Scanning $FILE" |& tee -a ${WORKING_DIR}/scan.log
  local TMP_FILE="${WORKING_DIR}/${FILE}"

  if [ "$TYPE_SIP" == 'application/zip' ]; then
    unzip $UNZIP_PARAMS "$ARCHIVE" "${FILE}" -d "$WORKING_DIR" |& tee -a ${WORKING_DIR}/scan.log
    # $? == 11 file not found; skipping to avoid infinite loop
  elif [ "$TYPE_SIP" == 'application/x-tar' ]; then
    tar xvf "$ARCHIVE" --directory "$WORKING_DIR" "${FILE}" |& tee -a ${WORKING_DIR}/scan.log
  elif [ "$TYPE_SIP" == 'application/gzip' ] || [ "$TYPE_SIP" == 'application/x-gzip' ]; then
    mkdir -p "$TMP_FILE"
    tar xvzf "$ARCHIVE" --directory "$TMP_FILE" |& tee -a ${WORKING_DIR}/scan.log #uncompress the entire archive
    # gunzip -c $ARCHIVE > $TMP_FILE |& tee -a ${WORKING_DIR}/scan.log
  elif [ "$TYPE_SIP" == 'application/x-bzip2' ]; then
    mkdir -p "$TMP_FILE"
    tar xvjf "$ARCHIVE" --directory "$TMP_FILE" |& tee -a ${WORKING_DIR}/scan.log #uncompress the entire archive
  else
    echo "ERROR: $ARCHIVE: mime-type $TYPE_SIP is not supported" |& tee -a ${WORKING_DIR}/scan.log
    return $RET_FAILURE
  fi

  # Normal scan...
  scan $SCAN_PARAMS "$TMP_FILE" &>> ${WORKING_DIR}/scan.log
  local RET=$? # return code of scan

  rm -f "$TMP_FILE"

  if [ $RET != $RET_OK ]; then
    REASON="Virus found !"
    return $RET_VIRUS_FOUND_NOTFIXED
  fi

  return $RET
}

################################################################################

if [ $# -ne 1 ]; then # Argument number must be one
  echo "ERROR: $# parameter(s) provided, only one parameter is needed"

else # one argument, let's go
  SIP=$1
  if [ ! -f "$SIP" ];then # if the file wich will be scan is existing, keep going
    echo "ERROR: \"$SIP\" doesn't exit"

  else
    START_TIME=$(date +%s)
    WORKING_DIR=$(mktemp -d -p /vitam/tmp/ingest-external/)
    chmod -R 770 $WORKING_DIR
    chown -R vitam:vitam $WORKING_DIR

    echo "$(date +"%Y-%m-%d %T") - scanning $SIP" |& tee -a ${WORKING_DIR}/scan.log

    FILE_SIZE=$(stat -c '%s' "$SIP")
    TYPE_SIP=$(file -b --mime-type "$SIP")
    FILE_SUM=$(sha256sum $SIP | cut -d' ' -f1)

    echo "DEBUG: SIP_size: $FILE_SIZE; SIP_format: $TYPE_SIP; sha256sum: $FILE_SUM" |& tee -a ${WORKING_DIR}/scan.log

    if grep -Fxq "$FILE_SUM" /etc/avast/whitelist; then
      echo "File whitelisted, escape scanning..." |& tee -a ${WORKING_DIR}/scan.log
      RET=0
    else
      custom_scan "$SIP"
      RET=$? # return code of scan
    fi

    # Catch global output reason
    if [ $RET == $RET_OK ]; then
      RET_MSG='[OK]'
    elif [ $RET == $RET_VIRUS_FOUND_FIXED ]; then
      RET_MSG="[OK: $REASON]"
      RET=$RET_OK # FORCE O UNTIL RET=1 WELL HANDLED BY VITAM
    elif [ $RET == $RET_VIRUS_FOUND_NOTFIXED ]; then
      RET_MSG="[KO: $REASON]"
    elif [ $RET == $RET_FAILURE ]; then
      RET_MSG="[ERROR: Scan not performed.]"
    fi

    END_TIME=$(date +%s)
    EXECUTION_TIME=$(($END_TIME - $START_TIME))
    echo -e "$SIP\t$RET_MSG - execution time: ${EXECUTION_TIME}s" |& tee -a ${WORKING_DIR}/scan.log

    cat ${WORKING_DIR}/scan.log >> /vitam/log/avast/scan.log # Stores output in global log file for avast; helpful for debugging purpose
    rm -rf ${WORKING_DIR} # cleaning temporary working dir
  fi
fi
exit $RET

#!/bin/sh
set -u

##########################################################################
# Role:                                                                  #
# Scan a single file (or inside a zip) using clamav eicar signature      #
##########################################################################
# Return codes:                                                          #
# - 0: scan OK - no virus                                                #
# - 1: virus found and corrected (not implemented here)                  #
# - 2: virus found but not corrected                                     #
# - 3: Fatal scan not performed                                          #
##########################################################################

RET_NOTVIRUS=0
RET_VIRUS_FOUND_FIXED=1
RET_VIRUS_FOUND_NOTFIXED=2
RET_FAILURE=3

if [ $# -ne 1 ]; then
  printf 'Usage: %s <file>\n' "$(basename "$0")" >&2
  exit $RET_FAILURE
fi

file="$1"

# Check file existence/readability
if [ ! -r "$file" ]; then
  exit $RET_FAILURE
fi

# The canonical EICAR test string (68 bytes)
eicar='X5O!P%@AP[4\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*'

check_file_for_eicar() {
  f="$1"
  if [ -f "$f" ]; then
    if grep -a -F -q -- "$eicar" "$f"; then
      printf 'EICAR test string found in: %s\n' "$f"
      return $RET_VIRUS_FOUND_NOTFIXED
    fi
  fi
  return $RET_NOTVIRUS
}

exit_code=$RET_NOTVIRUS
tmpdir=""

cleanup() {
  [ -n "$tmpdir" ] && [ -d "$tmpdir" ] && rm -rf "$tmpdir"
  exit $exit_code
}
trap cleanup EXIT

# If it's a zip file, extract and scan contents
if file "$file" | grep -q 'Zip archive data'; then
  tmpdir=$(mktemp -d)
  if ! unzip -qq -o "$file" -d "$tmpdir" >/dev/null 2>&1; then
    printf 'Failed to extract zip file: %s\n' "$file" >&2
    exit_code=$RET_FAILURE
    exit
  fi

  # Iterate over extracted files (no subshell)
  while IFS= read -r f; do
    if ! check_file_for_eicar "$f"; then
      exit_code=$RET_VIRUS_FOUND_NOTFIXED
      exit
    fi
  done <<EOF
$(find "$tmpdir" -type f)
EOF

else
  # Regular file
  if ! check_file_for_eicar "$file"; then
    exit_code=$RET_VIRUS_FOUND_NOTFIXED
    exit
  fi
fi

exit_code=$RET_NOTVIRUS
exit

#!/bin/sh
MASTER_BRANCHES=$(git branch -r | grep -E ‘/(master_|develop)’ | grep -v HEAD)

FOUND_BRANCH=“”
FOUND_FORKDATE=0

for b in $MASTER_BRANCHES; do
    tmp_commit=$(git merge-base “$b” HEAD)
    tmp_date=$(git show -s --format=“%ct” $tmp_commit)
    #echo ?>2 “$b - $tmp_commit at $tmp_date”
    if [ “$tmp_date” -gt “$FOUND_FORKDATE” ]; then
        FOUND_FORKDATE=$tmp_date
        FOUND_BRANCH=“$b”
    fi
done

echo “$FOUND_BRANCH”
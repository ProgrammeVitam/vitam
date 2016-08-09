#!/usr/bin/env bash

pushd `dirname dirname $0` > /dev/null
path=`pwd`
popd > /dev/null

codepoints_file="${path}/bower_components/material-design-icons/iconfont/codepoints"
icons_scss_file="${path}/scss/_icons.scss"

# default css prefix
prefix_var_name="md-css-prefix"

# clear the scss
> "${icons_scss_file}"

i=0
while IFS='' read -r line || [[ -n "$line" ]]; do
    IFS=' ' read -r -a icon <<< "$line"
    if [ "${#icon[@]}" == 2 ] && [ -n "${icon}" ] && [ -n "${icon[1]}" ]; then
        echo ".#{\$${prefix_var_name}}-$(echo ${icon} | sed -e 's/_/\-/g'):before { content: "'"'"\\${icon[1]}"'"'"; }" \
            >> "${icons_scss_file}"
    else
        echo "ERROR: The file ${codepoints_file} has the invalid format on line $((i+1))" 1>&2
        exit 1
    fi
    ((i++))
done < "${codepoints_file}"

echo "Successfully imported ${i} icons"
exit 0
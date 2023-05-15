#!/bin/bash
#*******************************************************************************
# Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
#
# contact.vitam@culture.gouv.fr
#
# This software is a computer program whose purpose is to implement a digital archiving back-office system managing
# high volumetry securely and efficiently.
#
# This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
# software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
# circulated by CEA, CNRS and INRIA at the following URL "https://cecill.info".
#
# As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
# users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
# successive licensors have only limited liability.
#
# In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
# developing or reproducing the software by the user in light of its specific status of free software, that may mean
# that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
# experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
# software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
# to be ensured and, more generally, to use and operate it in the same conditions as regards security.
#
# The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
# accept its terms.
#*******************************************************************************

WORKING_FOLDER=$(dirname $0)

pushd ${WORKING_FOLDER}

# Args check

if [ -z "$1" ]; then
	echo "Usage : build.sh <component> [<target_folder>]"
	popd
	exit 1
fi

COMPONENT=$1
TARGET_FOLDER=$2

COMPONENT_FOLDER=$(pwd)/${COMPONENT}

if [ ! -d "${COMPONENT_FOLDER}" ]; then
	echo "Folder ${COMPONENT_FOLDER} doesn't exist ! Aborting."
	popd
	exit 2
fi

# Default target folder definition
if [ -z "${TARGET_FOLDER}" ]; then
	TARGET_FOLDER=${COMPONENT_FOLDER}/target
	mkdir -p ${TARGET_FOLDER}
fi

if [ ! -d "${TARGET_FOLDER}" ]; then
	echo "Target folder ${TARGET_FOLDER} doesn't exist ! Aborting."
	popd
	exit 2
fi
# will create symlinks only if the file links exists in rpmbuild
if [ -f "${COMPONENT_FOLDER}/rpmbuild/links" ]; then
	## list elements in $HOME
	HOME_CONTENT=$(find ${HOME} -maxdepth 1 -mindepth 1)
	for hid_item in ${HOME_CONTENT}; do
		item_name=$(basename ${hid_item})
		# only create symlink if the folder is in the links file. therefore do nothing if not in the file.
		grep -q ${item_name} "${COMPONENT_FOLDER}/rpmbuild/links" || continue
    # calculates link's full path
		target_link="${COMPONENT_FOLDER}/${item_name}"
		if [ -L ${target_link} ]; then
		# test if link exists and is a symlink. if this link point to somewhere else, info and override
			if [ $(readlink ${target_link}) != ${hid_item} ]; then
				echo "Info: Updating Symlink ${target_link} to ${hid_item}."
		  fi
	  # test if exists and is a file or a folder. True => warn and do nothing
		elif [ -f ${target_link} ] || [ -d ${target_link} ];then
			echo "Warning: ${target_link} should be a symlink."
			continue
		fi
		# create symlink in COMPONENT_FOLDER
		ln -sf $hid_item ${target_link}
	done
fi
# override exit function to delete created links when living.
function clean_exit(){
	returncode=${1:-0}
	find ${COMPONENT_FOLDER} -maxdepth 1 -type l -exec rm -f {} \; >/dev/null 2>&1
	exit ${returncode}
}

# Build RPM

for SPECFILE in $(ls ${COMPONENT_FOLDER}/rpmbuild/SPECS/*.spec); do
	echo "Building specfile ${SPECFILE}..."

	HOME=${COMPONENT_FOLDER} spectool -v -g -R ${SPECFILE}
	if [ ! $? -eq 0 ]; then
		echo "Error preparing the build ! Aborting."
		popd
		clean_exit 2
	fi

	HOME=${COMPONENT_FOLDER} rpmbuild -bb ${SPECFILE}
	if [ ! $? -eq 0 ]; then
		echo "Error building the rpm ! Aborting."
		popd
		clean_exit 2
	fi
done

# Copy result RPM in target folder

RPMS=$(find ${COMPONENT_FOLDER} -name '*.rpm')

mkdir -p ${TARGET_FOLDER}

for RPM in ${RPMS}; do
 	mv ${RPM} ${TARGET_FOLDER}
done

popd
clean_exit

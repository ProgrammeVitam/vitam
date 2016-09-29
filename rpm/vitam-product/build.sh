if [ -z "$1" ]; then
	echo "usage : build.sh <component>"
	exit 1
fi

COMPONENT=$(pwd)/$1

if [ ! -d "$COMPONENT" ]; then
  echo "Folder $COMPONENT doesn't exist ! Aborting..."
  exit 2
fi

for SPECFILE in $(ls ${COMPONENT}/rpmbuild/SPECS/*.spec); do
  echo "Building specfile ${SPECFILE}..."
  HOME=${COMPONENT} spectool -g -R ${SPECFILE}
  HOME=${COMPONENT} rpmbuild -bb ${SPECFILE}
done


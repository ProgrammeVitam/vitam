if [ -z "${VITAMDEV_GIT_REPO}" ] ; then
  echo "Please enter the location of your vitam git repository"
  read VITAMDEV_GIT_REPO
  export VITAMDEV_GIT_REPO
  echo "Note : to persist this value, insert this environment variable : 'export VITAMDEV_GIT_REPO=${VITAMDEV_GIT_REPO}'"
fi

VITAMDEV_USER=$(whoami)
VITAMDEV_HOME=${HOME}

VITAMDEV_USER_UID=$(id -u ${VITAMDEV_USER})
VITAMDEV_USER_GID=$(id -g ${VITAMDEV_USER})

VITAMDEV_IMAGE=vitam/dev-rpm-base
VITAMDEV_CONTAINER=vitam-rpm-dev

if [ -d "${VITAMDEV_GIT_REPO}/.git" ] ; then
	echo "Building development container..."
	docker build -t ${VITAMDEV_IMAGE} --rm --pull --build-arg http_proxy=${HTTP_PROXY} --build-arg https_proxy=${HTTPS_PROXY} dev-rpm-base
	echo "Launching docker container as daemon (launching systemd init process...)"
	docker run -d --privileged -v "${VITAMDEV_GIT_REPO}:/code" -v  /sys/fs/cgroup:/sys/fs/cgroup:ro -v "${VITAMDEV_HOME}/.m2:/devhome/.m2" -p 8082:8082 -p 9200:9200 -p 9300:9300 -p 9201:9201 -p 9301:9301 -p 27017:27017 -p 10514:10514 -p 8000-8010:8000-8010 -p 8100-8110:8100-8110 -p 8200-8210:8200-8210 -p 8300-8310:8300-8310  -p 8500:8500 --cap-add=SYS_ADMIN --security-opt seccomp=unconfined --name=${VITAMDEV_CONTAINER} --net=bridge --dns=127.0.0.1 --dns=10.100.211.222 ${VITAMDEV_IMAGE}
	echo "Registering user ${VITAMDEV_USER} in container..."
	docker exec ${VITAMDEV_CONTAINER} useradd -u ${VITAMDEV_USER_UID} -g ${VITAMDEV_USER_GID} -G wheel -d /devhome -s /bin/bash -c "Welcome, mister developer !" ${VITAMDEV_USER}
	echo "Opening console..."
	docker exec -it -u ${VITAMDEV_USER} ${VITAMDEV_CONTAINER} bash
	echo "Stopping container..."
	docker stop ${VITAMDEV_CONTAINER}
	echo "Container stopped !"
	echo "Hint : your container is now configured ; to reuse it, just launch 'docker start ${VITAMDEV_CONTAINER}' then 'docker exec -it ${VITAMDEV_CONTAINER} bash'."
	echo "To restart from scratch, run 'docker rm ${VITAMDEV_CONTAINER}' to remove the existing container."
else
	echo "Given vitam git repository is not a git repository !"
fi

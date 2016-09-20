if [ -z "${VITAM_GIT_REPO}" ] ; then
  echo "Please enter the location of your vitam git repository"
  read VITAM_GIT_REPO
  export VITAM_GIT_REPO
  echo "Hint : next time, you can set the VITAM_GIT_REPO environment variable in your shell profile to automatically launch the container"
fi

if [ -d "${VITAM_GIT_REPO}/.git" ] ; then

	echo "Launching docker container as daemon (launching systemd init process...)"
	docker-compose up -d
	echo "Opening console..."
	docker-compose exec dev-rpm-base bash
	echo "Stopping container..."
	docker-compose stop dev-rpm-base
	echo "Container stopped !"
	echo "Hint : don't forget to run docker-compose rm from time to time to reap off obsolete containers, or to restart from scratch"

else
	echo "Given vitam git repository is not a git repository !"
fi
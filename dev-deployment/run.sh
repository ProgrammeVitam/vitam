if [ -z "${VITAM_GIT_REPO}" ] ; then
  echo "Please enter the location of your vitam git repository"
  read VITAM_GIT_REPO
  export VITAM_GIT_REPO
fi

if [ -d "${VITAM_GIT_REPO}/.git" ] ; then
	echo "Pulling development docker..."
	docker pull docker.programmevitam.fr/vitam/dev-rpm-base:latest
	echo "Launching docker container as daemon (launching systemd init process...)"
#docker  run  -it  --rm  --privileged  -v  /sys/fs/cgroup:/sys/fs/cgroup:ro  -v  /home/gafou/projets/vitam/work/sources/vitam-2:/code  -v  /home/gafou/.m2:/root/.m2  -p  8082:8082  -p  9200:9200  -p  9300:9300  -p  9201:9201  -p  9301:9301  -p  27017:27017  -p  10514:10514  -p  8000-8010:8000-8010  -p  8100-8110:8100-8110  -p  8200-8210:8200-8210  -p  8300-8310:8300-8310  --cap-add=SYS_ADMIN  --security-opt seccomp=unconfined  --name=vitam-rpm-dev  --net=bridge  --dns=10.100.211.222  docker.programmevitam.fr/vitam/dev-rpm-base 
	docker run -d --privileged -v "${VITAM_GIT_REPO}:/code" -v  /sys/fs/cgroup:/sys/fs/cgroup:ro -v "${HOME}/.m2:/root/.m2" -p 8082:8082 -p 9200:9200 -p 9300:9300 -p 9201:9201 -p 9301:9301 -p 27017:27017 -p 10514:10514 -p 8000-8010:8000-8010 -p 8100-8110:8100-8110 -p 8200-8210:8200-8210 -p 8300-8310:8300-8310  -p 8500:8500 --cap-add=SYS_ADMIN --security-opt seccomp=unconfined --name=vitam-rpm-dev --net=bridge --dns=127.0.0.1 --dns=10.100.211.222 docker.programmevitam.fr/vitam/dev-rpm-base
	echo "Opening console..."
	docker exec -it vitam-rpm-dev bash
	echo "Stopping container..."
	docker stop vitam-rpm-dev
	echo "Container stopped !"
	echo "Hint : your container is now configured ; to reuse it, just launch 'docker start vitam-rpm-dev' then 'docker exec -it vitam-rpm-dev bash'."
	echo "To restart from scratch, run 'docker rm vitam-rpm-dev' to remove the existing container."
else
	echo "Given vitam git repository is not a git repository !"
fi
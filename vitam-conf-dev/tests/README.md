# Execution Tests

Vitam's Unit Tests execution requires the presence of multiple COTS services:
* mongodb
* elasticsearch

## Prerequisite

Having docker-engine CE and docker-compose installed (with docker group rights on your user)

## Execute vitam's unit tests

You need to execute `docker-compose.sh`.

This script will configure and launch all mandatory containers :
* mongodb using port 27017
    * check : http://127.0.0.1:27017/
* elasticsearch using ports 9200 and 9300
    * check : http://127.0.0.1:9200/

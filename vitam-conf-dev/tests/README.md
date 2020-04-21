# Execution Tests

Vitam's Unit Tests execution requires the presence of multiple COTS services:
* mongodb
* elasticsearch
* openio s3 storage
* minio s3 storage

## Prerequisite

Having docker-engine CE and docker-compose installed (with docker group rights on your user)

## Execute vitam's unit tests

You need to execute `docker-compose.sh`.

This script will configure and launch all mandatory containers :
* mongodb using port 27017
    * check : http://127.0.0.1:27017/
* elasticsearch using ports 9200 and 9300
    * check : http://127.0.0.1:9200/
* minio without ssl using port 9999
    * check: http://127.0.0.1:9999/
* minio with ssl (using the certificates from the vitam code resources copied in /tmp/minio_ssl_certs) using port 9000
    * check : https://127.0.0.1:9000/
* openio without ssl using ports 6000,6001,6006,6009,6011,6014,6017,6110,6120,6200,6300 and 6007
    * check : https://127.0.0.1:6007/ (should return a 404 error)

# Docker Compose replacement for Dev COTS

## Introduction

The setup process for local deployment using local docker container for COTS is complex and long to set up.
This is a proposal for a simple replacement using a Docker compose file. Reminder: this is for local development only.

## Starting up

The docker-compose environment will start:

 - ElasticSearch
 - MongoDB
 - Siegfried

The storage used by ElasticSearch and MongoDB are persistent volumes and will then be kept between restarts and up/down.

As usual with docker compose:

### Start the COTS

From the `dev-deployment/docker-cots` directory:

```shell
docker compose up -d
```

### Clear the data

From the `dev-deployment/docker-cots` directory:

```shell
docker compose down
docker volume rm docker-cots_elastic_data docker-cots_mongo_data
```

### Force rebuild images

When mongo init scripts updated, we'll need a full rebuild.

```shell
docker compose down --volumes --remove-orphans
docker compose build --no-cache
docker compose up -d --force-recreate
```
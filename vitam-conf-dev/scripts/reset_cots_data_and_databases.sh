#!/bin/bash

# Delete offers
rm -fr /vitam/data/offer/*
rm -fr /vitam/data/offer2/*
rm -fr /vitam/data/offer3/*

# Delete workspace
rm -fr /vitam/data/workspace/*

# List ES indices
curl 'localhost:9200/_cat/indices?v' | tail -n +2

# Delete ES indices
for i in `curl 'localhost:9200/_cat/indices?v' | tail -n +2 | awk '{print $3}'`; do curl -XDELETE "http://127.0.0.1:9200/$i"; echo; done

# List ES indices
curl 'localhost:9200/_cat/indices?v' | tail -n +2

# Delete mongo collections
mongo admin -u vitamdb-admin -p 'azerty' <<EOF

use offer
db.getCollectionNames().forEach(function(x) {db[x].remove({})})

use offer2
db.getCollectionNames().forEach(function(x) {db[x].remove({})})

use offer3
db.getCollectionNames().forEach(function(x) {db[x].remove({})})

use masterdata
db.getCollectionNames().forEach(function(x) {db[x].remove({})})

use logbook
db.getCollectionNames().forEach(function(x) {db[x].remove({})})

use metadata
db.getCollectionNames().forEach(function(x) {db[x].remove({})})

use metadata-collect
db.getCollectionNames().forEach(function(x) {db[x].remove({})})

use collect
db.getCollectionNames().forEach(function(x) {db[x].remove({})})

use report
db.getCollectionNames().forEach(function(x) {db[x].remove({})})

use identity
db.getCollectionNames().forEach(function(x) {db[x].remove({})})

use scheduler
db.getCollectionNames().forEach(function(x) {db[x].remove({})})

EOF
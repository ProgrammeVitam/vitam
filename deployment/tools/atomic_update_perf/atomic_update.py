#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import getopt
import getpass
import json
import re
import requests
import sys


def validateJSON(jsonData):
    try:
        json.loads(jsonData)
    except ValueError as err:
        return False
    return True


# Parse / validate arguments
outputFileName = None
elasticsearchUrl = None
tenant = None
operationIdsStr = None
username = None
password = None
threshold = 100_000

try:
    opts, args = getopt.getopt(sys.argv[1:], "e:t:u:o:m:f:")
except getopt.GetoptError as err:
    print("""
./atomic_update.py [params...]
    -e : elasticsearch url             (eg. http://localhost:9200/, https://xyz.env.programmevitam.fr/elasticsearch-data)
    -u : basic auth user name (opt)    (eg. username)
    -t : tenant                        (eg. 0)
    -o : comma-separated operation ids (eg. aeeaaaaaachn7hk2aaatkalxwxodlfyaaaaq,aecaaaaaachmf443aay7ialybbjgxyyaaaba)
    -m : Max threshold (default=10000) (eg. 100000)
    -f : Output query file (opt)       (eq. ./query.json)
""", file=sys.stderr)
    sys.exit(2)

for opt, arg in opts:
    if opt == "-e":
        elasticsearchUrl = arg
    elif opt == "-t":
        tenant = arg
    elif opt == "-u":
        username = arg
    elif opt == "-o":
        operationIdsStr = arg
    elif opt == "-m":
        threshold = int(arg)
    elif opt == "-f":
        outputFileName = arg

if elasticsearchUrl is None:
    print('Missing elasticsearch url', file=sys.stderr)
    sys.exit(2)

if tenant is None:
    print('Missing tenant', file=sys.stderr)
    sys.exit(2)

if operationIdsStr is None:
    print('Missing operation ids', file=sys.stderr)
    sys.exit(2)

if not re.match('^[a-z0-9]{36}(\\s*,\\s*[a-z0-9]{36})*$', operationIdsStr):
    print('Invalid operation ids. Expected comma-separated guids', file=sys.stderr)
    sys.exit(2)

if username is not None:
    password = getpass.getpass("Basic-auth password: ")

operationIds = [x.strip() for x in operationIdsStr.split(',')]

if not elasticsearchUrl.endswith('/'):
    elasticsearchUrl = elasticsearchUrl + '/'

# List units & generate queries

headers = {
    'content-type': "application/json",
    'accept': "application/json"
}

session = requests.Session()
if username is not None:
    session.auth = (username, password)

scrollId = None
foundUnits = 0
atomicUpdateQueries = []

while True:

    print("Proceeded units: " + str(foundUnits))

    if scrollId is None:
        # First query

        searchUrl = elasticsearchUrl + "unit_" + str(tenant) + "/_search"
        payload = json.dumps({
            'query': {
                'terms': {
                    '_opi': operationIds
                }
            },
            "size": 1000,
            '_source': False
        })

        querystring = {"scroll": "1m"}

        response = session.request("POST", searchUrl, headers=headers, params=querystring, data=payload)

    else:
        # Existing query scroll

        searchUrl = elasticsearchUrl + '_search/scroll'
        payload = json.dumps({
            'scroll': '1m',
            'scroll_id': scrollId
        })

        response = session.request("POST", searchUrl, headers=headers, params=querystring, data=payload)

    # Check error
    response.raise_for_status()

    reponseJson = json.loads(response.text)
    scrollId = reponseJson['_scroll_id']

    hits = reponseJson['hits']['hits']

    if len(hits) == 0:
        break

    for hit in hits:
        unitId = hit['_id']
        foundUnits = foundUnits + 1
        atomicUpdateQueries.append({
            '$query': [{'$eq': {"#id": unitId}}],
            "$action": [
                {"$set": {"Title": "Titre mis à jour - " + str(foundUnits)}},
                {"$set": {"Sequence": "Sequence - " + str(foundUnits)}}
            ]
        })

atomicUpdateBody = {
    "threshold": threshold,
    "queries": atomicUpdateQueries
}

# Print query
print("Writing result...")

if outputFileName is not None:
    # Write output to a file
    outputFile = open(outputFileName, "w")
    outputFile.write(json.dumps(atomicUpdateBody))
    outputFile.close()
else:
    print(json.dumps(atomicUpdateBody, indent=2))

print("Done. Total selected units: " + str(foundUnits))

# elastic-kibana-interceptor

## Context
Some users need to explore Vitam data directly via Kibana Data
without going through the Vitam UI.

## Problem
Some of vitam internal fields in Elasticsearch start with `_`.
Kibana treats these fields as its own internal metadata and refuses to display them.

## Solution
This component acts as a reverse proxy between Kibana and Elasticsearch:
- replaces `_` with `#` in ES → Kibana responses (so Kibana can display them)
- replaces `#` with `_` in Kibana → ES requests (so ES can understand them)

## Known limitations
- Fragile by nature: any breaking change in ES or Kibana behavior may break this component
- GET requests with body are converted to POST (officially supported by ES, cf. [API conventions](https://www.elastic.co/guide/en/elasticsearch/reference/8.19/api-conventions.html#get-requests))
- Must be monitored on every ES/Kibana upgrade

## ⚠️ Warning
This component is a workaround and is not safe for production use. It is only provided
for testing purposes along with kibana-data and might be removed in later versions of Vitam.

This component is fragile by nature: any breaking change in ES or Kibana behavior may
break it silently, especially on version upgrades.

KIBANA automatic dashboard import
=========

This folder contains the files that will be automatically loaded into KIBANA (precisly in the .kibana index of ElasticSearch).

Requirements
------------

This folder can contains the following directories:

 - **search**
 - **index-pattern**
 - **visualization**
 - **dashboard**

These folders will be looked up for any .json document file that will be loaded in the .kibana index with a **_type** field according to its parent directory name and an **_id** field corresponding to its name.
Example:

Considering a file `/search/superSearch.json`, the result in ElasticSearch would be:

| _index | _id | _type |
| --- | --- | --- |
| .kibana | superSearch | search

How to produce .json files
--------------

1. Open Kibana and create components
2. Go to Kibana "**Settings**"
3. Switch to the "**Objects**" tab
4. Select objects you want to export
5. Click on "**Export**" to export selected objects or click on "**Export everything**". The browser should download an *export.json* file.
6. Open the *export.json* file. You'll notice an array of resources
    ```javascript
    [
     {
       "_id": "myAwesomeVisualization",
       "_type": "visualization",
       "_source": { ... }
     },
     { ... },
     { ... }
    ]
    ```
7. For each resources, `touch` an **_id**.json file, e.g. **myAwesomeVisualization.json**, in the corresponding **_type** directory, here **visualization**. The result should be: `/visualization/myAwesomeVisualization.json`. Copy the **_source** content in the new file.

> Since the index-patterns are not exportable from the Kibana web interface, you can get them with a **GET** request at the following address: ``http://{{elasticsearch_host}}/.kibana/index-pattern/{{index_pattern_name}}`` then proceed as if you were at step 6.

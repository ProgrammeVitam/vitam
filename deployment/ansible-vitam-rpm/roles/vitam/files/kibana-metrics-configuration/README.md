KIBANA automatic dashboard import
=========

This folder contains the files that will be automatically loaded into KIBANA (precisly in the .kibana index of Elasticsearch).

Requirements
------------

This folder can contains the following directories:

 - search
 - index-pattern
 - visualization
 - dashboard

These folders will be looked up for any .json document file that will be loaded in the .kibana index with a _type_ according to its parent directory. 

How to produce .json files
--------------

To create .json documents corresponding to KIBANA components, one must first create the components in the KIBANA application and export them as .json files.
To export the components go into KIBANA __Settings__ then click on the __Objects__ tab, select the components to export and click on the __Export__ button.
Otherwise, you can click on the __Export Everything__ button to export all components at once (careful, this might not be the best option since all the components will be in the same .json file).

Since the index-patterns are not exportable from the KIBANA web interface, you can get them with a __GET__ request at the following address:

http://__elasticsearchLogAddress__/.kibana/index-pattern/__index-pattern-name/_source

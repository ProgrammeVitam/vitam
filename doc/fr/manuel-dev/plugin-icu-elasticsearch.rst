########################
Plugin ICU Elasticsearch
########################

Le letter tokenizer Elasticsearch qu'on utilise aujourd'hui n'indexe pas les chiffres. 
Pour pouvoir les indexer les chiffres, nous avons besoin d'un plugin qui hérite de ce letter tokenizer. 

Nous avons choisi le plugin ICU analysis pour Elasticsearch,
https://github.com/elasticsearch/elasticsearch-analysis-icu cela.     

Ce plugin est installé lors de déploiement du système et est associé au Node Elastichsearch Vitam, qui permet aux autres services de les 
appeler.
#########################
Plugin ICU Elasticsearch
#########################

Le letter tokenizer elasticsearch qu'on utilise aujourd'jui n'indexe pas les chiffres. Pour pouvoir les indexer les chiffres, 
nous avons besoin d'un plugin qui hérite ce letter tokenizer. Nous avons choisi le plugin ICU analysis pour Elasticsearch,
https://github.com/elasticsearch/elasticsearch-analysis-icu cela.     

Ce plugin est installé lors de déploiement du système qui associe au Node Elastichsearch Vitam, qui permet aux autres services de les 
appeler.   


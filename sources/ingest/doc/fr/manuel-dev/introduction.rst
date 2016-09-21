Introduction
############
  
L'ensemble de ces documents est le manuel de développement du module ingest, 
qui représente le métier fonctionnel de l'user story #84 de projet VITAM, dont le but 
et de réaliser des opérations sur le dépôt de document SIP vers la base de données MongoDb
(upload SIP). 

Le module est divisé en deux sous modules : ingest-internal et ingest-external. Le module ingest-internal 
fournnit les fonctionalités pour des traitements internes de la plate-forme Vitam, autrement dit il n'est 
visible que pour les appels internes de Vitam. Le module ingest-external fournit des services pour les appels 
extérieur de la plate-forme cela veux dire qu'il est visible pour les appels de l'extérieur de Vitam.      

Le manuel se compose de deux parties 
- DAT présente l'architecture technique du module  
au niveau des packages, classes
- REST-RAML explique comment on utitlise des différents service proprosés par module
- détail d'utilisation du client
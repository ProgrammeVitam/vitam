Architecture Technique Ingest 
############

Présentation
------------
Cette section présente en bref l'architecture en général du module ingest. 
Le module ingest se compose de deux sous modules : ingest-external et ingest-internal.

Le premier rôle de l'ingest-internal est de réaliser un upload d'un SIP en prenant des données (le SIP le logbook)
qui viennent de ingest-external après un scan virus sur le SIP. Son deuxième rôle est de transférer les objets
sauvegardés dans le serveur de stockage comme Inputstream au ingest-external 

Le premier rôle de l'ingest-external est de réaliser un upload d'un SIP provenant d'une application externe de vitam 
en se connectant au service. Le service ingest-external réalise un scan virus sur le SIP envoyé, préparé le 
logbook sur cette opération. Si le SIP n'est pas infecté, ingest-externe va appelé le service ingest-internal 
via son client avec des données de parametres (logbook & SIP) pour continuer le service. Son deuxième rôle est de télécharger 
les objets sauvegardés dans le serveur de stockage.   
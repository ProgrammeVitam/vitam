DAT : module collect
############################

Ce document présente l'ensemble du manuel développement concernant le développment du module 
collecte qui est identifié par la user story #9004, qui contient :

- modules & packages
- classes métiers

--------------------------


Modules et packages
=======================

collect

- collect-rest    : le serveur REST de collect qui donnes des traitement sur dépôt de document SIP.
- collect-client  : client collect qui sera utilisé par les autres modules interne de VITAM pour le service de dépôt des SIPs

Classes métier
===================

Dans cette section, nous présentons quelques classes principales dans des modules/packages 
qu'on a abordé ci-dessus.

collect-rest
--------------------

- ``TransactionRessource.java`` : définir des ressources différentes pour le serveur REST Transaction
- ``CollectMain.java`` : créer & lancer le serveur d'application avec une configuration

collect-client
-----------------------

- ``CollectClientFactory.java`` : Afin de récupérer le client-collect , une factory a été mise en place.


Common format identification
############################


Présentation
************

Le fonctionnement de cette brique est la suivante.
Un outil d'identification est installé sur un environnement à déterminer.
Ce service offre une API Rest permettant d'obtenir :
 - un status  
 - l'analyse d'un format en fonction du Path vers le fichier à analyser.


|  *Package parent :* **fr.gouv.vitam.common.format.identification**

Sous packages
*************

Identification :
================

| *Package :*  **fr.gouv.vitam.common.format.identification**

Ce package contient une factory, une interface de client, ainsi qu'un client mocké. 
Il contient également une enum précisant les différents clients disponibles (pour l'instant au nombre de 2 : siegfried + mock).

Exceptions :
============

| *Package :*  **fr.gouv.vitam.common.format.identification.exception**

Exceptions retournées par la vérification de formats.
Sont au nombre de 5 :

 - FileFormatNotFoundException : exception levée en cas de non résolution d'un format de fichier.
 - FormatIdentifierBadRequestException : exception levée si la requete soumise à l'outil n'est pas correcte.
 - FormatIdentifierFactoryException : exception levée dans le cadre de la factory.
 - FormatIdentifierNotFoundException : exception levée si l'outil ne peut pas être interrogé.
 - FormatIdentifierTechnicalException : exception levée en cas d'erreur technique générique.
 
Model :
=======

| *Package :*  **fr.gouv.vitam.common.format.identification.model**

Ce package contient une classe de configuration ainsi que 2 POJO de réponses pour des appels au service. 


Siegfried :
===========

| *Package :*  **fr.gouv.vitam.common.format.identification.siegfried**

Ce package contient les différences classes pour l'utilisation d'un client Siegfried. Une factory, un mock ainsi qu'un client REST. 



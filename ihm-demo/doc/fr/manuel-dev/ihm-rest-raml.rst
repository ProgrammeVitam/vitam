ihm-demo
########

Présentation
------------
Ce document présente le schéma de rest resources défini qui sera appelé par 
le backend de l'application web. 


------------
 package:* **fr.gouv.vitam.api**
|  *Package proposition:* **fr.gouv.vitam.metadata.rest**

Module pour le module opération : api / fr.gouv.vitam.ihmdemo.appserver

Services
--------

Rest API
--------

| URL Path : / 
|
| POST /archivesearch/units -> la recherche des métadata 
|
| POST /logbook/operations -> Recherche dans logbook par un nom (critère).  
|			   Cela retourne l'ensemble de logbook opération (avec id opération)   	
| POST /logbook/operations/{idOperation} -->  Recherche de logbook de l'opération 
|   de logbook par idOperation
| POST /admin/formats -> Recherche des formats par DSL
| POST /admin/formats/{idFormat} -> Recherche d'un formats par son id
| POST /format/check -> Validation du fichier PRONOM
| POST /format/import -> Import des formats dans le fichier PRONOM
| DELETE /format/delete -> Supprimmer tous les format dans la base de données 





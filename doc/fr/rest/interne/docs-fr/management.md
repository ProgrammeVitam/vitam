API de Gestion
===============

L'API de Gestion propose les points d'entrées et les méthodes pour gérer les différentes transactions, sauf celles de versement.

----------
**Freezes**
-------------
**Freezes** est le point d'entrée pour gérer le processus de Gel. Il contient les mêmes types d'accès à *Units*, mais selon les modes de création et effacement.

Aucune opération ne peut changer le contenu mais porte uniquement sur l'ajout ou le retrait de *Units* ou *ObjectGroups* ou *Objects* au processus concerné.


**Destructions**
-------------
**Destructions** est le point d'entrée pour gérer le processus de destruction (élimination). Il contient les mêmes types d'accès à *Units*, mais selon les modes de création, mise à jour et effacement.

Aucune opération ne peut changer le contenu mais porte uniquement sur l'ajout ou le retrait de *Units* ou *ObjectGroups* ou *Objects* au processus concerné, sauf pour la mise à jour des métadonnées de gestion.


**Transformations**
-------------
**Transformations** est le point d'entrée pour gérer les tâches asynchrones de transformations.


**Logbooks**
-------------
**Logbooks** est le point d'entrée pour pour l'accès aux journaux.

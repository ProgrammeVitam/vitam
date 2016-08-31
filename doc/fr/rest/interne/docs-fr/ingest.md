API de Versement (Ingest)
============

L'API de Versement (Ingest) propose les points d'entrées et les méthodes pour démarrer, modifier et finaliser des transactions de versement (Ingest).

----------
**Ingests**
-------------
**Ingests** est le point d'entrée pour le processus de versement. Il contient les mêmes types d'accès à *Units*, mais selon les modes de création, mise à jour et effacement.

Il faut noter que les opérations sur des *Units* pré-existantes sont autorisées uniquement en mode mise à jour. Toutes les autres opérations s'appliquent uniqtement aux *Units*, *ObjectGroups* et *Objects* nouvellement créés.

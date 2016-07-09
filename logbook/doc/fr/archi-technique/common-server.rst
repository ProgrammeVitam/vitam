Common-client
*************

Présentation
------------

|  *Package parent :* **fr.gouv.vitam.logbook**
|  *Proposition de package  :* **fr.gouv.vitam.logbook.common.server**

Ce module est utilisé par les modules server operations et lifecycles et utilise :
 
   - metadata-core
   - logbook-common

Services
--------

La logique technique actuelle est la suivante :

- Chaque journal est une collection dans MongoDB
- Chaque entrée dans la collection est la somme des événements d'une opération / cycle de vie

  - Opération ingest x contient l'ensemble des étapes de cette opération
  - Cycle de vie d'une archive x contient l'ensemble des événements associés à cette archive 
  
Ceci facilite les recherches sur la base de l'entrée primaire (la première) mais n'interdit pas la recherche sur les entrées secondaires qui sont dans le tableau "**events**".

Plus tard, ces journaux seront aussi écrits dans des fichiers.

- Opérations

  - Un par jour
  - Chaque event (unitaire et non globalisé) devra être écrit au fur et à mesure, c'est à dire en respectant les dates d'events (dans l'ordre acquité par le Moteur de journalisation)

- LifeCycles
  
  - Un fichier unique, les events dans l'ordre chronologique (qui correspond à l'odre deans events)
 
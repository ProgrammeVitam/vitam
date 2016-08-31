API d'Accès
==========

*L'API d'Accès* propose les points d'entrées et les méthodes pour atteindre, requêter et récupérer les informations depuis les **Units** et les **Objects**.

Cette API est globalement reproduite dans tous les autres points d'accès lorsque l'accès à des _Units_ et _Objects_ est nécessaire. Les fonctionnalités offertent peuvent par contre varier (droit en modification, effacement, ... selon le contexte).

-------------
**Units**
-------------

**Units** est le point d'entrée pour toutes les descriptions d'archives. Celles-ci contiennent les métadonnées de description et les métadonnées archivistiques (métadonnées de gestion).

Une _Unit_ peut être soit un simple dossier (comme dans un plan de classement), soit une description d'un item. Les _Units_ portent l'arborescence d'un plan de classement. Ce plan de classement peut être multiples :
  - Plusieurs racines (_Unit_ de plus haut niveau)
  - Plusieurs parents (un dossier peut être rattaché à plusieurs dossiers)
  - Il s'agit d'une représentation dite de "_graphe dirigé sans cycle_" (DAG en Anglais pour "_Directed Acyclic Graph_").
      
Pour le model SEDA, il est équivalent à l'**ArchiveUnit**, notamment pour les parties **Content** et **Management**. Pour l'Isad(G) / EAD, il est équivalent à **Description Unit**.

A priori, un seul _groupe_ d'_objets_ d'archives est attachée à une _Unit_. Cela signifie que si une _Unit_ devait avoir plus d'un _groupe_ d'_objets_ d'archive attaché, vous devez spécifier des sous-Units à l'_Unit_ principale, chaque sous-Unit n'ayant qu'un _groupe_ d'_objets_ d'archives attaché. Un même _groupe_ d'_objets_ d'archives peut par contre être attaché à de multiples _Unit_.

*Ce point devra être décidé : l'unicité d'un _Groupe_ d'_objets_ sous un _Unit_ _OU_ la possibilité d'avoir plusieurs _Groupes_ d'_objets_ sous un même _Unit_.*

Aucun effacement n'est autorisé, ni aucune mise à jour complète (seules les mises à jours partielles sont autorisées via la commande PATCH).


-------------
**ObjectGroups**
-------------

**ObjectGroups** est le point d'entrée pour toutes les archives binaires mais également les non binaires (comme une référence à des objet d'archives physiques ou externes au système). Elles contiennent les métadonnées techniques.

Un _Groupe_ d'_Objects_ peut être constitué de plusieurs versions (sous-objets) pour différencier des usages comme version de conservation, version de diffusion...

Pour le model SEDA, il est équivalent à un **DataObjectGroup**. Pour l'EAD, il est équivalent à un **Digital Archive Object Group or Set**.

Chaque _Groupe_ d'_Objets_ doit être attaché à au moins un parent _Unit_.

Seul l'accès est autorisé (GET).


-------------
**Objects**
-------------

**Objects** est le point d'entrée pour toutes les usages et versions d'archives binaires au sein d'un _Groupe_ mais également les non binaires (comme une référence à des objet d'archives physiques ou externes au système). Elles contiennent les métadonnées techniques.

Si un _Object_ est constitué de plusieurs versions (sous-objets) pour différencier des usages comme version de conservation, version de diffusion..., elles seront rattachés sous un même _groupe_.

Pour le model SEDA, il est équivalent à un **DataObject** (binaire avec **BinaryDataObject** ou physique avec **PhysicalDataObject**). Pour l'EAD, il est équivalent à un **Digital Archive Object**.

Chaque _Objet_ doit être attaché à ununique parent _Groupe_.

Seul l'accès est autorisé (GET).

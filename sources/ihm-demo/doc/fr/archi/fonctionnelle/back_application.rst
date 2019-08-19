Architecture fonctionnelle de l'application Back
################################################

But de cette documentation
==========================
On présente dans ce document l'architecture fonctionnelle de l'application Back IHM de VITAM.

Fonctionnement général du module
================================
L'application IHM-DEMO est une application web dont la partie Front est une application Single Page développée avec le framework AngularJS 1.5.3 et côté serveur on utilise un serveur Jetty intégré qui gère les appels à ses services REST. Dans ce document, on s'intéresse à l'application côté serveur.
On détaille dans la suite le fonctionnement par service REST.

.. caution:: La solution logicielle :term:`VITAM` étant avant tout un *back office*, si vous possédez une :term:`IHM` raccordée à VITAM, il n'est pas recommandé d'installer ce composant en environnement de production.

Recherche des units : POST /ihm-demo/v1/api/archivesearch/units
---------------------------------------------------------------
L'application Front construit en amont un objet Json passé dans le corps de la requête HTTP qui décrit les critères de recherche, les colonnes à afficher et le tri par défaut. Ci-dessous, la structure de l'objet reçu:

{
    Title = *titleCriteria*

    projection_transactdate = "TransactedDate"

    projection_id = "#id"

    projection_title = "Title"

    orderby = "TransactedDate"

}

- L'entrée *Title* définit la chaîne de caractères saisie par l'utilisateur et utilisée pour la **recherche exacte** sur les titres des archive units.
- Pour faire la distinction entre les champs utilisés dans la partie *query* de la requête DSL et les colonnes sélectionnées (*projection*), le préfixe *projection_* doit être ajouté à toutes les colonnes à afficher. Le résultat affiché inclut les colonnes *TransactedDate*, *id* et *Title*.
- L'entrée *orderby* définit la colonne sur laquelle le tri par défaut sera fait côté serveur.
- Il faudrait noter ici le caractère *#* ajouté à la sélection du champ *_id*. En fait, afin de permettre la sélection des champs protégés tels que *_id* il faut remplacer le caractère *_* par le caractère *#*.

Cet objet est convertit en Map<String, String> qui sera utilisée pour construire la requête DSL de sélection.
On passe la main maintenant à la classe utilitaire *DslQueryHelper* qui construit à partir de la Map reçue la requête DSL de sélection. Une instance de la classe *fr.gouv.vitam.builder.request.construct.Select* est créée et alimentée pour obtenir à la fin la structure suivante:

{

    $query:[{"$and":[{"$eq":{"title":"titleCriteria"}}]}],

    $filter:{"$orderby":{"TransactedDate":1}},

    $projection:{"$fields":{"Title":1, "#id":1, "TransactedDate" : 1}}

}

La classe *UserInterfaceTransactionManager* appelle le client Access qui prend en charge l'appel de MetaData et la récupération du résultat de recherche. Ci-dessous la structure du résultat retourné à l'application Front:

{
    $hint: { total:x },

    $context: {},

    $result: [*tableau des archive units trouvées*]

}


Affichage du détail d'une archive unit : GET /ihm-demo/v1/api/archivesearch/unit/{id}
-------------------------------------------------------------------------------------
Le processus d'affichage des détails d'une archive unit est déclenché suite à une sélection faite sur un résultat de recherche. L'id de l'unité sélectionnée est passé en tant que paramètre dans l'URL.

Pour indiquer qu'il s'agit d'une sélection par id (c'est à dire une archive unit spécifique), la Map utilisée pour la construction de la requête DSL va contenir seulement une entrée : **(SELECT_BY_ID, id)**. De ce fait, la requête DSL de sélection introduit l'entrée root égale à l'id de l'unit sélectionnée. Donc, on aura la structure suivante:


        {"$roots":[id],"$query":[],"$filter":{},"$projection":{}}


De même, on appelle le client Access pour passer la requête au moteur MetaData qui retourne la structure de résultat de recherche mais on aura dans le bloc $result un tableau contenant un seul objet qui est l'archive unit sélectionnée.

{
    $hint: { total:1 },

    $context: {},

    $result: [{*détails de l'archive unit sélectionnée (toutes les colonnes)*}]

}



Modification et enregistrement des détails d'une archive unit : PUT /ihm-demo/v1/api/archiveupdate/units/{id}
--------------------------------------------------------------------------------------------------------------
Au niveau du formulaire d'une archive unit, l'utilisateur peut modifier toutes les données affichées mises à part l'id et les données de management. L'application Front passe seulement les champs qui ont été modifiés pour la sauvegarde sous la forme d'un tableau d'objet Json. Dans la suite la structure retournée:

[{"fieldId":"XXXXXXXX","newFieldValue":"VVVVVVVVV"},   {"fieldId":"YYYYYYYYY","newFieldValue":"VVVVVVVVV"}, ....]


On convertit cette structure en Map<String, String> et on ajoute une entrée (SELECT_BY_ID, id) pour intégrer le bloc *root* à la construction de la requête DSL de l'update.
on construit cette fois-ci une instance de la classe *fr.gouv.vitam.builder.request.construct.Update* et on ajoute des Actions de type *fr.gouv.vitam.builder.request.construct.action.SetAction*. 

Voici un exemple de la requête obtenue:

{"$roots":[*id*],"$query":[],"$filter":{},"$action":[{"$set":{"date":"09/09/2015"}},{"$set":{"title":"Archive2"}}]}

De nouveau, on passe la requête DSL à Access qui retourne à son tour le résultat de l'opération d'update avec la même structure des requêtes de sélection mais sans résultat car l'application Front relance la récupération de l'archive unit à la réception de la réponse.

Remarque importante
^^^^^^^^^^^^^^^^^^^
Pour le moment, on ne gère pas la mise à jour des champs de type tableau qui va faire appel à un autre type d'action.

Reste à faire
-------------
Dans la suite les services REST qui sont en cours de traitement:

- Recherche sur les opérations logbook
- Affichage du détail d'une opération logbook
- Téléchargement d'un SIP
- Recherche sur le référentiel des formats
- Affichage du détail d'un format
- Validation d'un référentiel à télécharger
- Téléchargement d'un référentiel de formats
- Suppression d'un format

Ecriture des TNR
################

Structure des répertoires
=========================

Le répertoire du dépot vitam-test est strucutré de la façon suivante :

::

	vitam-itests
			|------- data

**Dossier vitam-itests** : contient les fichiers de configurations des tests fonctionnels.

**Dossier data** : contient les éventuels jeux de données nécessaires à l'exécution des tests.

Fichiers de Configuration
=========================

Nommage des fichiers
--------------------

Un fichier regroupe tous les tests à effectuer sur une fonctionnalité. Il ne peut y avoir deux fonctionnalités dans un fichier de configuration.

On va par exemple réaliser :

	* un fichier pour les tests sur l'Ingest
	* un fichier pour les test sur l'accès aux unités archivistiques

Les noms des fichiers sont composés de la façon suivante :

::

	EndPoint-Fonctionnalité.feature

Par exemple :

::

	access-archive-unit.feature
	admin-logbook-traceability.feature

Informations transverses
------------------------

Les fichiers de configuration doivent contenir les informations suivantes qui s'appliqueront ensuite à l'ensemble des scénarios du fichier :

**# language** : information obligatoire. Correspond à la langue utilisée pour les descriptions. Par exemple :

::

	language : fr.

**Annotation** : information optionnelle. L'annotation permet par la suite de lancer uniquement un fichier de configuration en ligne de commande en utilisant son annotation en paramètre. Par exemple :

::

	@AccessArchiveUnit

**Fonctionnalité** : information obligatoire. La fonctionnalité est une description qui permet d'identifier le périmètre testé. Il est notamment repris dans les rapports réalisés à la fin d'une campagne de test. Par exemple :

::

	Fonctionnalité: Recherche une archive unit existante

**Contexte** : information optionnelle. Les informations contenues dans contexte sont des actions qui vont s'exécuter pour chacun des scénarios. A ce titre, elles s'écrivent comme les actions d'un scénario. Le constexte doit être indenté de 1 par rapport aux autres éléments. Par exemple :

::

	Contexte:
    	Etant donné les tests effectués sur le tenant 0


Ecriture d'un scénario
===========================

Structure d'un scénario
-----------------------

Un scénario correspond à un test. Son nom doit être défini de la façon suivante :

::

	Scénario: Description du scénario

Il doit être sur la même indentation que le contexte, soit 1 par rapport à la fonctionnalité, annotation et langage.

Un scénario est constitué d'une succession d'actions, chacune décrite sur une ligne.

Les actions sont composées des trois informations suivantes :

	* Contexte
	* Fonction
	* Paramètre (pas toujours obligatoire)

**Actions d'étapes** : permet d'introduire l'action, de l'insérer par rapport à l'action précédente. La liste des contextes disponibles se trouve en annexe.

**Fonction** : mobilise, via un langage naturel, une fonction de Vitam. La liste des fonctions disponibles se trouve en annexe.

**Paramètre** : certaines fonctions ont besoin d'être suivies d'un paramètre. Ils sont listés dans le tableau des fonctionnalités disponibles en annexe.

Les actions doivent être indentées de 1 par rapport aux scénarios.

Exemple d'un scénario constitué de trois actions :

::

  Scénario: SIP au mauvais format
    Etant donné un fichier SIP nommé data/SIP_KO/ZIP/KO_SIP_Mauvais_Format.pdf
    Quand je télécharge le SIP
    Alors le statut final du journal des opérations est KO


Insérer une requète DSL
-----------------------

Certaines fonctions nécessitent l'entrée de requêtes DSL en paramètre. Celles-ci doivent etre insérées entre guillemets (" "), après un retour à la ligne à la suite de la fonction.

Voici un exemple d'une action suivie d'une requète DSL :

::

	Et j'utilise la requête suivante
		"""
		{ "$roots": [],
  			"$query": [
    			{ "$and": [
        			{ "$gte": {
            			"StartDate": "1914-01-01T23:00:00.000Z"
          				} }, {"$lte": {
            				"EndDate": "1918-12-31T22:59:59.000Z"
          				} } ],
      				"$depth": 20}],
  				"$filter": {"$orderby": { "TransactedDate": 1 }
  				}, "$projection": {
    				"$fields": {"TransactedDate": 1, "#id": 1, "Title": 1, "#object": 1, "DescriptionLevel": 1, "EndDate": 1, "StartDate": 1}}}
		"""

Insérer un tableau
------------------

Certaines fonctions attendent un tableau en paramètre. Les lignes des tableaux doivent simplement être séparées par des "pipes" ( | ).

Voici un exemple de fonction prenant un tableau en paramètre.

::

	Alors les metadonnées sont
      | Title            | Liste des armements |
      | DescriptionLevel | Item                |
      | StartDate        | 1917-01-01          |
      | EndDate          | 1918-01-01          |



Annexes
=======

Liste des actions d'étapes disponibles
--------------------------------------

Les types d'actions sont les suivants :

* une situation initial (les acquis) : **Etant donné**
* un événement survient : **Quand** (peut être suivi de **Et** et/ou **Mais**)
* on s’assure de l’obtention de certains résultats : **Alors** (peut être suivi de **Et** et/ou **Mais**)

.. csv-table::
	:header: Action

	"Etant donné"
	"Quand"
	"Alors"
	"Mais"
	"Et"

Liste des fonctions disponibles
-------------------------------

.. csv-table::
	:header: "Fonctionnalité", "Doit être suivi par"

	"les tests effectués sur le tenant (*)","un tenant"
	"les données du jeu de test du SIP nommé (.*)","un fichier"
	"un fichier SIP nommé (.*)","un fichier"
	"je télécharge le SIP","une autre action"
	"je recherche le journal des opérations","une autre action"
	"je télécharge son fichier ATR","une autre action"
	"je recherche le JCV de l'unité archivistique dont le titre est (.*)","un titre d'unité archivistique"
	"je recherche le JCV du groupe d'objet de l'unité archivistique dont le titre est (.*)","un titre d'unité archivistique"
	"le statut final du journal des opérations est (.*)","un statut"
	"le[s]? statut[s]? (?:de l'événement|des événements) (.*) (?:est|sont) (.*)","un ou plusieur evType et un Statut"
	"l'outcome détail de l'événement (.*) est (.*)","un outcome detail et une valeur"
	"l'état final du fichier ATR est (.*)","un statut"
	"le fichier ATR contient (.*) balise[s] de type (.*)","un nombre et un type de balise"
	"le fichier ATR contient les valeurs (.*)","une ou plusieurs valeurs séparées par des virgules"
	"le fichier ATR contient la  chaîne de caractères (.*)","un texte ou une simple chaîne de caractères"
	"j'utilise la requête suivante","une requête"
	"j'utilise le fichier de requête suivant (.*)","un fichier"
	"j'utilise dans la requête le GUID de l'unité archivistique pour le titre (.*)","un titre d'unité archivistique"
	"j'utilise dans la requête le paramètre (.*) avec la valeur (.*)","un nom de paramétre et une valeur de remplacement"
	"je recherche les unités archivistiques","une autre action"
	"je recherche les groupes d'objets des unités archivistiques","une autre action"
	"je recherche les groupes d'objets de l'unité archivistique dont le titre est (.*)","un titre d'unité archivistique"
	"le nombre de résultat est","un nombre"
	"les metadonnées sont","un tableau"
	"les metadonnées pour le résultat (.*)", "un nombre et un tableau"
	"je recherche les registres de fond","une autre action"
	"le nombre de registres de fond est (.*)","un nombre"
	"les metadonnées pour le registre de fond sont","un tableau"
	"je recherche les détails des registres de fond pour le service producteur (.*)","un identifiant de service producteur"
	"le nombre de détails du registre de fond est (.*)","un nombre"
	"les metadonnées pour le détail du registre de fond sont","un tableau"

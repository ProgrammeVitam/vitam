Menu
####

Le menu est situé en haut des pages. Il est présent sur toutes les pages et est découpé en 4 grandes sections :

- Entrée
- Recherche
- Administration
- Gestion des archives

Chaque section comprend plusieurs sous-menu permettant de naviguer sur les différentes pages de la solution.

.. image:: images/menu.png

Fil d'Ariane
------------

Le fil d'Ariane est un élément qui permet de visualiser le chemin d'accès à la page affichée. Il est situé sur toutes les pages, en dessous du menu.

La racine est toujours inactive (sans lien), elle correspond au nom du menu où est située la page.

Suite à la racine, les pages parentes sont listées et sont séparées par un "/" et, contrairement à la racine, elles sont toutes cliquables.
Un clic sur une lien redirige sur la page idoine.

.. image:: images/ariane.png

Visualisation des listes des écrans de détail
---------------------------------------------

Dans les écrans de détails comportant des listes, à savoir :
- Détail d'une opération d'entrée
- Détail d'un journal de Cycle de Vie Unit
- Détail d'un journal de Cycle de Vie Object Group

Les couleurs de fonds et des polices ont été adaptées lorsqu'elle rencontrent un statut KO, FATAL et WARNING.

Pour les cas KO ou FATAL, les ligne s'affichent en rouge et le texte et affiché en noir. Au survole avec le pointeur de la souris, le texte passe en blanc. Ci-après une capture d'écran montrant l'exemple d'une tâche et d'une étape dont le statut est FATAL :

.. image:: images/op_entree_detail_FATAL.png

Pour les cas WARNING, la ligne ne change pas de couleur mais le texte est affiché en orange. Ci-après, une capture d'écran montrant l'exemple d'une tâche et d'une étape dont le statut est WARNING :

.. image:: images/op_entree_detail_WARNING.png

Affichage des dates et heures
-----------------------------

Les dates et heures sont affichées au format JJ-MM-AAAA HH:mm. 

Ci-après un exemple

.. image:: images/date_heure.png

L'heure affichée s'adapte au fuseau horaire définit sur le poste de consultation.

Titre des pages
---------------

Les titre des pages, visibles sur les onglets des navigateurs, sont composés différement sur IHM démo et IHM recette. 

Sur IHM démo, le titre des pages est celui du dernier noeud du fil d'Ariane. Par exemple :

- Transfert
- Recherche Référentiel des formats
- Recherche Référentiel des règles de gestion

.. image:: images/titre_IHM_demo.png

Sur IHM recette, le titre des pages est celui du dernier noeud du fil d'Ariane, précédé du mot "Recette -". Par exemple :

- Recette - Test SOAP-UI
- Recette - Administration des collections

.. image:: images/titre_IHM_recette.png
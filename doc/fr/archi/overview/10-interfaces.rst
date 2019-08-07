Interfaces externes du système
##############################

.. figure:: images/vitam-blackbox.*
	:align: center

	Vue de VITAM dans son environnement (vue "boîte noire")

Interfaces requises
===================

Dans cette version du système, aucune interface externe autre que les services IT standard (:term:`NTP`, :term:`DNS`, dépôts de mise à jours des :term:`OS`, ...) n'est requise par :term:`VITAM`.

.. Les interfaces externes requises par le système VITAM sont les suivantes :
.. Pas pour l'instant, mais à garder pour la suite : * Points de distribution des CRL concernant les CA des certificats trustés par VITAM 


Interfaces métier exposées
==========================

La solution logicielle :term:`VITAM` expose trois grands groupes d':term:`API` métier :

* Les API d'*ingest* : elles permettent l'entrée d'une nouvelle archive dans le système ;
* Les API d'accès : elles permettent d'accéder aux données d'archives présentes dans le système (métadonnées et données d'archives, journaux, référentiels) ;
* Les API d'administration fonctionnelles : elles permettent notamment la modification des référentiels métier.

Ces API sont exposées en tant qu'API :term:`REST` (HTTPS) au niveau des composants externes (composants ``*-external``), avec un accès protégé par une authentification par certificat.

.. seealso:: Les points relatifs à la sécurité des interfaces externes exposées sont abordés dans la section :doc:`sécurité </securite/_toc>`.


Interfaces externes du système
##############################

.. figure:: images/vitam-blackbox.*
	:align: center

	Vue de VITAM dans son environnement (vue "boîte noire")

Interfaces requises
===================

.. TODO Lister ici les interfaces requises (ou potentiellement utilisées) par VITAM. Ex: référentiel d'authentification externe, ...

Dans cette version du système, aucune interface externe n'est requise par VITAM.

.. Les interfaces externes requises par le système VITAM sont les suivantes :
.. Pas pour l'instant, mais à garder pour la suite : * Points de distribution des CRL concernant les CA des certificats trustés par VITAM 


Interfaces métier exposées
==========================

VITAM expose deux grands groupes d'API métier :

* Les API d'ingest : elles permettent l'entrée d'une nouvelle archive dans le système ; ce sont des API REST.
* Les API d'accès : elles permettent d'accéder aux données d'archive présentes dans le système (métadonnées et données d'archives, journeaux, référentiels). Ce sont également des API REST.
  
Ces API sont exposées en tant qu'API REST (HTTPS), avec un accès protégé par une authentification par certificat.

.. seealso:: Les points relatifs à la sécurité des interfaces externes exposées sont abordés dans la section :doc:`sécurité </securite/_toc>`

.. todo Lister ici les interfaces proposées par VITAM, en en dictant éventuellement les principes d'authentification et d'autorisation ; faire référence au besoin à la documentation des API externes. ; préciser notamment les points de sécurité liés, ou reporter au chapitre idoine dans la partie sécurité. En outre, préciser les éléments relatifs aux clients logiciels fournis.
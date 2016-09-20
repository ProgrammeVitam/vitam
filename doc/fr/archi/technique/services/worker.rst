Worker
######

Type :
	Composant VITAM Java

Données gérées :
	* Aucune

Typologie de consommation de resources :
	* CPU : fort
	* Mémoire : fort
	* Réseau : fort (entrant et sortant)
	* Disque : faible
	  
.. todo : à confirmer l'usage de disque faible (cache local des fichiers de travail ?)


Particularités
==============

Les workers utilisent des outils externes pouvant avoir des pré-requis importants sur les OS utilisés ; pour réduire l'impact sur les systèmes, ces outils seront packagés dans des conteneurs Docker. 

Aucun conteneur Docker n'est fourni dans cette version de la solution VITAM.


.. Conteneurs Docker
.. =================

.. .. Il n'y aura pas de containers Docker en Bêta. (à confirmer)

.. .. Attention : à vérifier selon les besoins en terme de reconnaissance de formats pour la beta (FITS, Siegfried, FIDO, ...) et leur compatibilité CentOS.

.. Logique d'utilisation de Docker
.. -------------------------------

.. Dans le cadre du moteur d'exécution, il pourra être nécessaire de mettre en oeuvre des plugins spécifiques (ex : format de transformations de formats spécifiques/propriétaires). Pour éviter les contraintes de compatibilité entre composants et pour assurer une isolations entre les workers, il sera fait usage de conteneurs Docker. 

.. .. Répondre à la question : quelle utilisation de docker : "lancement d'une ligne de commande de scan" ou "lancement de mode serveur" ?

.. Livraison des artefacts
.. -----------------------

.. La méthode de livraison des artefacts (Containers Docker et/ou DockerFile) n'est pas définie au jalon du PP.


.. Dépôts (Registry)
.. -----------------

.. Les dépôts Dockers (appelés registry) sont des dépôts dynamiques (par opposition aux dépôts RPM ou DEB qui sont des dépôts pouvant être servis comme des sites statiques). 

.. Les implémentations libres existantes sont : 

.. * Registry fourni par Docker Inc
.. * Nexus (de Sonatype, à partir de la version 3 uniquement)

.. Le choix du registry Docker mis à disposition pour Vitam n'est pas choisi à ce jour 


.. Configuration Docker
.. --------------------

.. A définir

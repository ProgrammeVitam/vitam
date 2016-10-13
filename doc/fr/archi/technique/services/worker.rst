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
	* Disque : faible (logs)
	  
.. todo : à confirmer l'usage de disque faible (cache local des fichiers de travail ?)


Particularités
==============

Les workers utilisent des outils externes pouvant avoir des pré-requis importants sur les OS utilisés ; pour réduire l'impact sur les systèmes, ces outils pourront être packagés dans des conteneurs Docker. 

Aucun conteneur Docker n'est fourni dans cette version de la solution VITAM.

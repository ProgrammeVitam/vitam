Siegfried
#########

Type :
	Composant binaire d'identification de format de fichiers

Données stockées :
	* Aucune

Typologie de consommation de resources :
	* CPU : faible
	* Mémoire : faible
	* Réseau : très faible, et sur localhost uniquement
	* Disque :  faible (logs)

.. warning:: Dans cette version bêta du système VITAM, les formats ne sont actuellement pas mis à jour, et aucune validation automatique de cohérence avec le référentiel des formats chargé dans VITAM n'est effectuée.


Mode de fonctionnement dans VITAM
=================================

Dans VITAM, Siegfried est utilisé dans son mode serveur accédant à des fichiers locaux ; dans ce cadre, le serveur Siegfried est uniquement bindé sur localhost, et donc uniquement accessible à des processus locaux à ce serveur.

L'utilisation typique de Siegfried par un composant est donc la suivante :

- Appel du serveur siegfried sur localhost ; cet appel contient uniquement une demande de traitement, et contient le chemin d'un fichier local à analyser ;
- Siegfried réalise l'analyse du fichier local ;
- Siegfried répond à la requête en indiquant le format du fichier analysé.
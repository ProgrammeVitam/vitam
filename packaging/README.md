# Documentation technique du dossier packaging

## assembly-deb.xml

Ce fichier est utilisé pour créer un assemblage de distribution pour les environnements Debian il contient le repository deb

## assembly-deployment.xml

Ce fichier est utilisé pour créer un assemblage des fichiers de l'ansiblerie permettant le déploiement du projet. Il spécifie les fichiers à inclure dans le déploiement ainsi que leur emplacement dans l'arborescence de fichiers. Il est utilisé pour créer un fichier de distribution contenant tous les fichiers nécessaires au déploiement.

## assembly-doc.xml

Ce fichier est utilisé pour packager la documentation du projet Vitam.

## assembly-full.xml

Ce fichier est utilisé pour créer un assemblage de distribution. Il spécifie les fichiers à inclure dans la distribution ainsi que leur emplacement dans l'arborescence de fichier. Il est utilisé pour créer un fichier de distribution contenant tous les fichiers nécessaires pour les environnements Deb/RPM.

## assembly-rpm.xml

Ce fichier est utilisé pour créer un assemblage de distribution signé pour les environnements RPM. Il spécifie les fichiers à inclure dans la distribution ainsi que leur emplacement dans l'arborescence de fichier. Il est utilisé pour créer un fichier de distribution contenant tous les fichiers nécessaires pour les environnements RPM.

## pom.xml

Ce fichier est utilisé par Maven pour configurer et construire le projet. Il contient des informations sur les dépendances, les plugins, les propriétés, nécessaires à la construction du projet.

## remove_griffins.sh

Ce script est utilisé pour supprimer les fichiers de configuration de Griffin du système de fichiers. Ces fichiers peuvent être inclus dans un déploiement mais ne sont pas nécessaires pour le fonctionnement du système.

## remove_user_certificates.sh

Ce script est utilisé pour supprimer les certificats utilisateur du système de fichiers. Ces certificats sont utilisés pour les tests d'intégration mais ne sont pas nécessaires pour le fonctionnement du système.

---

## packaging

* Le fichier assembly-deployment.xml est utilisé pour créer un déploiement contenant les fichiers nécessaires pour le déploiement, tandis que assembly-full.xml est utilisé pour créer une distribution pour tous les environnements.
* assembly-deb.xml, assembly-rpm.xml, assembly-full.xml, ... sont utilisés pour créer des distributions deb et rpm. ils seront 
    -  signé -avec la clef gpg propre à vitam- lors du packaging d'une release ou un bugfix.
    -  non signées lors de builds classiques.

## Autres

* Le fichier pom.xml est utilisé pour configurer et construire le projet avec Maven.
* Les scripts remove_griffins.sh et remove_user_certificates.sh sont utilisés pour supprimer les fichiers de configuration de Griffin et les certificats utilisateur.

Cycle de vie des certificats
============================

Le tableau ci-dessous indique le mode de fonctionnement actuel pour les différents certificats et :term:`CA`. Précisions :

* Les "procédures par défaut" liées au cycle de vie des certificats dans la présente version de la solution VITAM peuvent être résumées ainsi :

  * Création : génération par :term:`PKI` partenaire + copie dans répertoires de déploiement + script ``generate_stores.sh`` + déploiement ansible
  * Suppression : suppression dans répertoires de déploiement + script ``generate_stores.sh`` + déploiement ansible 
  * Renouvellement : regénération par :term:`PKI` partenaire + suppression / remplacement dans répertoires de déploiement + script ``generate_stores.sh`` + redéploiement ansible

* Il n’y a pas de contrainte au niveau des :term:`CA` utilisées (une :term:`CA` unique pour tous les usages VITAM ou plusieurs :term:`CA` séparées – cf. :term:`DAT`). On appelle ici :

  * ":term:`PKI` partenaire" : :term:`PKI` / :term:`CA` utilisées pour le déploiement et l’exploitation de la solution VITAM par le partenaire.
  
  * ":term:`PKI` distante" : :term:`PKI` / :term:`CA` utilisées pour l’usage des frontaux en communication avec le back office VITAM.

.. csv-table:: 
    :header: "Classe", "Type", "Usages", "Origine", "Création", "Suppression", "Renouvellement"
    :widths: 2, 2, 2, 2, 2, 2, 2

    "Interne",":term:`CA`","ingest & access",":term:`PKI` partenaire ","*proc. par défaut*","*proc. par défaut*","*proc. par défaut*"
    "Interne",":term:`CA`","offer",":term:`PKI` partenaire","*proc. par défaut* ","*proc. par défaut*","*proc. par défaut*"
    "Interne","Certif","Horodatage",":term:`PKI` partenaire","*proc. par défaut* ","*proc. par défaut*","*proc. par défaut*"
    "Interne","Certif","Storage (:term:`Swift`)","Offre de stockage","*proc. par défaut*","*proc. par défaut*","*proc. par défaut*"
    "Interne","Certif","Storage (s3)","Offre de stockage","*proc. par défaut* ","*proc. par défaut*","*proc. par défaut*"
    "Interne","Certif","ingest",":term:`PKI` partenaire","*proc. par défaut* ","*proc. par défaut*","*proc. par défaut*"
    "Interne","Certif","access",":term:`PKI` partenaire","*proc. par défaut* ","*proc. par défaut*","*proc. par défaut*" 
    "Interne","Certif","offer",":term:`PKI` partenaire","*proc. par défaut* ","*proc. par défaut*","*proc. par défaut*" 
    "Interne","Certif","Timestamp",":term:`PKI` partenaire","*proc. par défaut* ","*proc. par défaut*","*proc. par défaut*"
    "IHM demo",":term:`CA`","ihm-demo",":term:`PKI` partenaire","*proc. par défaut* ","*proc. par défaut*","*proc. par défaut*"   
    "IHM demo","Certif","ihm-demo",":term:`PKI` partenaire","*proc. par défaut* ","*proc. par défaut*","*proc. par défaut*"
    ":term:`SIA`",":term:`CA`","Appel :term:`API`",":term:`PKI` distante","*proc. par défaut* (:term:`PKI` distante)","*proc. par défaut*","*proc. par défaut* (:term:`PKI` distante)+recharger Certifs"
    ":term:`SIA`","Certif","Appel :term:`API`",":term:`PKI` distante","Génération + copie répertoire + deploy(par la suite appel :term:`API` d'insertion)","Suppression Mongo","Suppression Mongo + :term:`API` d'insertion"   
    "*Personae*","Certif","Appel :term:`API`",":term:`PKI` distante",":term:`API` ajout",":term:`API` suppression",":term:`API` suppression + :term:`API` ajout"          

Remarques :
 * Lors d'un renouvellement de :term:`CA` :term:`SIA`, il faut s'assurer que les certificats qui y correspondaient soient retirés de MongoDB et que les nouveaux certificats soient ajoutés par le biais de l' :term:`API` dédiée.
 * Lors de toute suppression ou remplacement de certificats :term:`SIA`, s'assurer que la suppression ou remplacement des contextes associés soit également réalisé.
 * L’expiration des certificats n’est pas automatiquement prise en charge par la solution VITAM (pas de notification en fin de vie, pas de renouvellement automatique). Pour la plupart des usages, un certificat expiré est proprement rejeté et la connexion ne se fera pas ; les seules exceptions sont les certificats *Personae*, pour lesquels la validation de l'arborescence :term:`CA` et des dates est à charge du front office en interface avec VITAM.

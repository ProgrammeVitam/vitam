Cycle de vie des certificats
============================

Le tableau ci-dessous indique le mode de fonctionnement actuel pour les différents certificats et CA. Précisions :

* Les "procédures par défaut" liées au cycle de vie des certificats dans la présente version de la solution VITAM peuvent être résumées ainsi :

  * Création : génération par PKI partenaire + copie dans répertoires de déploiement + script ``generate_stores.sh`` + déploiement ansible
  * Suppression : suppression dans répertoires de déploiement + script ``generate_stores.sh`` + déploiement ansible 
  * Renouvellement : regénération par PKI partenaire + suppression / remplacement dans répertoires de déploiement + script ``generate_stores.sh`` + redéploiement ansible

* Il n’y a pas de contrainte au niveau des CA utilisées (une CA unique pour tous les usages VITAM ou plusieurs CA séparées – cf. :term:`DAT`). On appelle ici :

  * "PKI partenaire" : PKI / CA utilisées pour le déploiement et l’exploitation de la solution VITAM par le partenaire.
  
  * "PKI distante" : PKI / CA utilisées pour l’usage des frontaux en communication avec le back office VITAM.

+---------+--------+-----------------+-------------------+----------------------------------------------+--------------------+--------------------+
|Classe   | Type   | Usages          | Origine           | Création                                     | Suppression        | Renouvellement     |
+=========+========+=================+===================+==============================================+====================+====================+
|Interne  | CA     | ingest & access | PKI partenaire    | *proc. par défaut*                           | *proc. par défaut* | *proc. par défaut* |
|         |        +-----------------+-------------------+----------------------------------------------+--------------------+--------------------+
|         |        | offer           | PKI partenaire    | *proc. par défaut*                           | *proc. par défaut* | *proc. par défaut* |
|         +--------+-----------------+-------------------+----------------------------------------------+--------------------+--------------------+
|         |        | Horodatage      | PKI partenaire    | *proc. par défaut*                           | *proc. par défaut* | *proc. par défaut* |
|         |        +-----------------+-------------------+----------------------------------------------+--------------------+--------------------+
|         |        | Storage (swift) | Offre de stockage | *proc. par défaut*                           | *proc. par défaut* | *proc. par défaut* |
|         |        +-----------------+-------------------+----------------------------------------------+--------------------+--------------------+
|         |        | Storage (s3)    | Offre de stockage | *proc. par défaut*                           | *proc. par défaut* | *proc. par défaut* |
|         +        +-----------------+-------------------+----------------------------------------------+--------------------+--------------------+
|         | Certif | ingest          | PKI partenaire    | *proc. par défaut*                           | *proc. par défaut* | *proc. par défaut* |
|         +        +-----------------+-------------------+----------------------------------------------+--------------------+--------------------+
|         |        | access          | PKI partenaire    | *proc. par défaut*                           | *proc. par défaut* | *proc. par défaut* |
|         +        +-----------------+-------------------+----------------------------------------------+--------------------+--------------------+
|         |        | offer           | PKI partenaire    | *proc. par défaut*                           | *proc. par défaut* | *proc. par défaut* |
|         +        +-----------------+-------------------+----------------------------------------------+--------------------+--------------------+
|         |        | Timestamp       | PKI partenaire    | *proc. par défaut*                           | *proc. par défaut* | *proc. par défaut* |
+---------+--------+-----------------+-------------------+----------------------------------------------+--------------------+--------------------+
|IHM demo | CA     | ihm-demo        | PKI partenaire    | *proc. par défaut*                           | *proc. par défaut* | *proc. par défaut* |
|         +--------+-----------------+-------------------+----------------------------------------------+--------------------+--------------------+
|         | Certif | ihm-demo        | PKI partenaire    | *proc. par défaut*                           | *proc. par défaut* | *proc. par défaut* |
+---------+--------+-----------------+-------------------+----------------------------------------------+--------------------+--------------------+
|SIA      | CA     | Appel API       | PKI distante      | proc. par défaut                             | *proc. par défaut* | proc. par défaut   |
|         |        |                 |                   | (PKI distante)                               |                    | (PKI distante) +   |
|         |        |                 |                   |                                              |                    | recharger Certifs  |
|         +--------+-----------------+-------------------+----------------------------------------------+--------------------+--------------------+
|         | Certif | Appel API       | PKI distante      | Génération + copie répertoire + deploy       | Suppression Mongo  | Suppression Mongo +|
|         |        |                 |                   | (par la suite, appel API d'insertion)        |                    | API d'insertion    |
+---------+--------+-----------------+-------------------+----------------------------------------------+--------------------+--------------------+
|Personae | Certif | Appel API       | PKI distante      | API ajout                                    | API suppression    | API suppression +  |
|         |        |                 |                   |                                              |                    | API ajout          |
+---------+--------+-----------------+-------------------+----------------------------------------------+--------------------+--------------------+

Remarques :
 * Lors d'un renouvellement de CA SIA, il faut s'assurer que les certificats qui y correspondaient sont retirés de MongoDB et que les nouveaux certificats sont ajoutés par le biais de l'API dédiée.
 * Lors de toute suppression ou remplacement de certificats SIA, s'assurer que la suppression / remplacement des contextes associés soit également réalisée.
 * L’expiration des certificats n’est pas automatiquement prise en charge par la solution VITAM (pas de notification en fin de vie, pas de renouvellement automatique). Pour la plupart des usages, un certificat expiré est proprement rejeté et la connexion ne se fera pas ; les seules exceptions sont les certificats Personae, pour lesquels la validation de l'arborescence CA et des dates est à charge du front office en interface avec VITAM.

.. BRE TODO : Préciser que les certificats serveur d’ihm-demo et ihm-recette sont nécessaires à leur déploiement, qui est porté par VITAM (mais c’est peut-être déjà quelque part?)
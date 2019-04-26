Notes et procédures spécifiques R10
###################################

.. caution:: Rappel : la montée de version vers la release R10 s'effectue depuis la release R9 (V2) et doit être réalisée en s'appuyant sur les dernières versions bugfixes publiées. 

Prérequis à la montée de version
================================

Sans objet. 

Montée de version
=================

La montée de version vers la release R10 est réalisée par réinstallation de la solution logicielle :term:`VITAM` grâce aux playbooks ansible fournis, et selon la procédure d'installation classique décrite dans le Document d'INstallation (DIN). 

Etapes de migration 
===================

Procédure de réindexation des ObjectGroup 
-----------------------------------------

Sous ``deployment``, exécuter la commande suivante :

``ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/reindex_es_data.yml --ask-vault-pass --tags objectgroup``

Les changement apportés touchent le mapping Elasticsearch de la collection ``ObjectGroup``. 

.. note:: Ce `playbook` ne supprime pas les anciens indexes pour laisser à l'exploitant le soin de verifier que la procedure de migration s'est correctement déroulée. A l'issue, la suppression des index devenus inutiles devra être realisée manuellement.

Migration des données de certificats
------------------------------------

La version R10 apporte une modification quant à la déclaration des certificats. En effet, un bug empêchait l'intégration dans la solution :term:`VITAM` de certificats possédant un serial number long. 

La commande suivante est à depuis le répertoire ``deployment`` sur les différents sites hébergeant la solution logicielle :term:`VITAM` :

``ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/R10_upgrade_serial_number.yml --ask-vault-pass``

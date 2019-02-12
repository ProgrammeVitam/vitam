Storage Engine
##############

Présentation
************

|  *Parent package :* **fr.gouv.vitam.storage**
|  *Package proposition :* **fr.gouv.vitam.storage.engine**

Module embarquant la partie core du storage (client et server).

Services
========

De manière générale, pour le Storage, les méthodes utilisées sont les suivantes :

- GET : pour l'équivalent du "Select".
- POST : **sans** X-Http-Method-Override: GET dans le Header, pour faire un insert.
- POST : **avec** X-Http-Method-Override: GET dans le Header, pour faire un select (avec Body).
- PUT : pour les mises à jour de Units et ObjectGroups.
- DELETE : pour effacer des métadonnées, des objects, des units, des journaux ou bien des containers.
- HEAD : pour les tests d'existence.

Rest API
--------

URI d'appel
^^^^^^^^^^^
| http://server/storage/v1

Headers
^^^^^^^
Plusieurs informations sont nécessaires dans la partie header :

- X-Strategy-Id : Stratégie pour Offres de stockage et Copies (conservation).
- X-Tenant-Id (obligatoire pour toute requête) : id du tenant. Cette information sera utilisée dans toutes les requêtes pour déterminer sur quel tenant se baser.
- X-Request-Id : l'identifiant unique de la requête.
- Accept : Permet de spécifier si un résultat doit contenir uniquement des métadonnées ('application/json'), un DIP complet (un ZIP contenant les métadonnées et les objets) ou seulement des Objects avec un contenu binaire ('application/octet-stream').
- X-ObjectGroup-Id : Id de l'ObjectGroup
- X-Units : Ids des Units parents
- X-Caller-Id : Id du service demandeur

Méthodes
^^^^^^^^

| HEAD /  -> **Permet d'accéder aux informations d'un container.**
| POST / -> avec header X-Http-Method-Override: GET **Permet d'accéder aux informations d'un container.**
| POST / -> **Permet de créer un nouveau container (nouveau tenant).**
| DELETE / -> **Permet d'effacer un Container (si vide).**
| HEAD / -> **Permet de tester l'existence du Container + retourne état et capacité occupée + restante**

| GET /objects -> **Liste du contenu binaire pour ce tenant.**
| POST /objects -> avec header X-Http-Method-Override: GET **Liste du contenu binaire pour ce tenant.**
| GET /objects/{id_object} -> **Permet de lire un Object.**
| POST /objects/{id_object} -> avec header X-Http-Method-Override: GET **Permet de lire un Object.**
| POST /objects/{id_object} -> **Permet de créer un nouveau Object.**
| DELETE /objects/{id_object} -> **Permet de détruire un Object.**
| HEAD /objects/{id_object} -> **Permet d'obtenir des informations sur un Object.**

| GET /logbooks -> **Liste du contenu d'une collection.**
| POST /logbooks -> avec header X-Http-Method-Override: GET **Liste du contenu d'une collection.**
| GET /logbooks/{id_logbook} -> **Permet de lire un Journal.**
| POST /logbooks/{id_logbook} -> avec header X-Http-Method-Override: GET **Permet de lire un Journal.**
| POST /logbooks/{id_logbook} -> **Permet de créer un nouveau Journal.**
| DELETE /logbooks/{id_logbook} -> **Permet de détruire un Journal.**
| HEAD /logbooks/{id_logbook} -> **Permet d'obtenir des informations sur un Journal.**

| GET /units -> **Liste du contenu d'une collection.**
| POST /units -> avec header X-Http-Method-Override: GET **Liste du contenu d'une collection.**
| GET /units/{id_md} -> **Permet de lire un Unit Metadata.**
| POST /units/{id_md} -> avec header X-Http-Method-Override: GET **Permet de lire un Unit Metadata.**
| POST /units/{id_md} -> **Permet de créer un nouveau Unit Metadata.**
| PUT /units/{id_md} -> **Permet de mettre à jour un Unit Metadata (404 si non pré-existant).**
| DELETE /units/{id_md} -> **Permet de détruire un Unit Metadata.**
| HEAD /units/{id_md} -> **Permet d'obtenir des informations sur un Unit Metadata.**

| GET /objectgroups -> **Liste du contenu d'une collection.**
| POST /objectgroups -> avec header X-Http-Method-Override: GET **Liste du contenu d'une collection.**
| GET /objectgroups/{id_md} -> **Permet de lire un ObjectGroup Metadata.**
| POST /objectgroups/{id_md} -> avec header X-Http-Method-Override: GET **Permet de lire un ObjectGroup Metadata.**
| POST /objectgroups/{id_md} -> **Permet de créer un nouveau ObjectGroup Metadata.**
| PUT /objectgroups/{id_md} -> **Permet de mettre à jour un ObjectGroup Metadata (404 si non pré-existant).**
| DELETE /objectgroups/{id_md} -> **Permet de détruire un ObjectGroup Metadata.**
| HEAD /objectgroups/{id_md} -> **Permet d'obtenir des informations sur un ObjectGroup Metadata.**

| GET /status -> **statut du storage**


Distribution
============

Le distributeur (module distribution) est en charge de décider selon la stratégie de stockage dans quelles offres doit être stocké un objet binaire.

Avant tout, le moteur de stockage récupère le binaire sur le workspace et le démultplie via un tee autant de fois que de copies à réaliser.
Pour chaque offre de stockage contenue dans la stratégie le distributeur demande au SPI DriverManager le driver associé.
Le distributeur instancie alors pour chaque offre un nouveau thread qui va se charger du transfert vers chacune des
offres. Dans chaque thread le driver associé à l'offre est utilisé pour le transfert.

Les thread font un retour OK ou KO. Pour chaque offre en KO, une nouvelle tentative de transfert est faite, jusqu'à trois tentatives. Si encore une offre est en KO après trois tentatives (retry), les binaires déposés sur les offres OK sont supprimés (rollback).

Le distributeur gère la mise à jour du journal des écritures du storage liée à l'opération de stockage d'un objet binaire dans une offre.
Toutes les tentatives y sont répertoriées pour chaque offre.

D'un point de vue séquentiel :

- Lors d'un appel de type POST /objects/{id_object} pour stocker un nouvel objet, le service est appelé :

 1. Il vérifie les paramètres d'entrée (nullité et cohérence simple)
 2. Il récupère la stratégie associée à l'ID fourni
 3. Regarde uniquement la partie "offres chaudes"
 4. Récupère le fichier sur le workspace
 5. Pour chaque offre chaude :

    1. Récupération du Driver associé s'il existe (sinon remontée d'une exception technique)
    2. Instancie un thread et dans ce trhead :
       1. Récupération des paramètres de l'offre : url du service, paramètres additionels
       2. Tentative de connection à l'offre et d'upload de l'objet
       3. Comparaison du digest hash renvoyé par l'offre avec le digest calculé à la volée lors de l'envoi du stream à l'offre
       4. Retour vers le distributeur du résultat (OK ou KO)
    3. Stockage du résultat de l'upload dans une map temporaire contenant le résultat de l'upload sur chaque offre

 6. Pour chaque offre KO, un nouvelle tentative est faite (jusqu'à trois)
 7. Si tout est OK, génération d'une réponse sérialisable, en mode 'succès' si **tous** les drivers ont correctement stocker l'objet.

    Si une offre au moins est KO, suppression des binaires sur les offres en succès et renvoie une exception

DriverManager : SPI
===================

| Service permettant d'ajouter ou de supprimer des drivers d'offre.
| Le driver (son interface) est défini dans :ref:`storage-driver`.

Les différents drivers sont chargés via le ServiceLoader de la JDK puis leurs instances sont stockées dans une liste.
Cela permet ensuite de configurer les offres sur les différentes instances de driver en passant par une MAP dont la clef est l'identifant de l'offre, la valeur est le driver instancié dans la liste (une référence à ce driver donc, retrouvé par son nom (getName())).

Le distributeur va alors demander au DriverManager le driver correspondant à l'offre définie dans la stratégie afin de réaliser les opérations de stockage.

Principe
--------

Le driver à ajouter doit implémenter l'interface définie. Dans son jar, il faut donc retrouver l'implémentation du driver ainsi que le fichier permettant au ServiceLoader de fonctionner. Ce fichier **DOIT** se trouver dans les resources, sous META-INF/services (principe du ServiceLoader de la JDK). Son nom est l'interface implémentée par le driver précédé de son package.

Exemple::

    samples/fr.gouv.vitam.storage.driver.Driver

Où VitamDriver est l'interface implémentée.

Son contenu est le nom de la classe qui implémente l'interface (qui est le nom du fichier) précédé de son package.

Exemple::

    mon.package.ou.se.trouve.mon.driver.VitameDriverImpl

Où VitamDriverImpl est l'implémentation du driver.

Voici le fichier : :download:`fr.gouv.vitam.storage.driver.Driver <samples/fr.gouv.vitam.storage.driver.VitamDriver>`

| Le jar sera déposé via une interface graphique dans un répertoire défini dans le fichier de configuration driver-location.conf avec la clef **driverLocation**. Actuellement il faut le déposer manuellement.
| Le paramétrage des offres se fera également via une interface graphique.

Cependant, il faut pouvoir redémarrer Vitam sans perdre l'association driver / offre ou démarrer Vitam avec des
drivers et des offres par défaut. Pour se faire, il faut persister la configuration.

Persistance
-----------

On s'appuie sur une interface offrant différentes méthodes afin de récupérer les offres à partir d'un nom de driver,
persister la configuration... Cela permet demain de changer la stratégie de persistance sans avoir à modifier le code
du SPI.

.. code-block:: java

    public interface DriverMapper {
        List<String> getOffersFor(String driverName) throws StorageException;
        void addOfferTo(String offerId, String driverName) throws StorageException;
        void addOffersTo(List<String> offersIdsToAdd, String driverName) throws StorageException;
        void removeOfferTo(String offerId, String driverName) throws StorageException;
        void removeOffersTo(List<String> offersIdsToRemove, String driverName) throws StorageException;
    }

Dans un premier temps, l'implémentation du mapper se fera en passant par un fichier. Dans son implémentation actuelle, le *DriverMapper* a besoin d'un fichier de configuration, ``driver-mapping.conf``. Ici, il permet de définir l'emplacement où seront enregistrés les fichiers permettant la persistance via la clef **driverMappingPath**. Une autre clef est nécessaire afin de définir le délimiteur dans ce fichier via la clef **delimiter**, le principe étant de mettre en place un fichier par driver comme un fichier CSV, les offres étant séparées par ce délimiteur.

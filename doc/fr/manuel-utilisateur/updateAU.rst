Mise à jour d'un SIP
####################

Cette partie décrit la mise à jour d'une Unité Archivistique dans Vitam.


Principe de fonctionnement
==========================

Update
******

Pour le moment, seul le cas de l'ajout d'une nouvelle Unité Archivistique à une AUnité Archivistique existant est en place.

1. Lors de l'extraction SEDA, si un Archive Unit déclare un identifiant Vitam dans une Unité Archivistique (*<SystemId>GUID</SystemId>*), alors les données de l'Archive Unit existant sont récupérées et un flag *<existing>true</existing>* est ajouté pour indiquer qu'il s'agit d'un mise à jour.

2. Les règles de gestion sont recalculées pour les Unité Archivistique dont les métadonnées on été modifiées

3. Lors de l'indexation des metadonnées, si l'Unité Archivistique est indiquée comme existante, alors on opère une mise à jour des données et on ajoute l'identifiant de l'opération en cours. Ajourd'hui l'implémentation de l'algorithme prends également en compte la mise à jour des métadonnées : toutes les métadonnées déclarées dans le manifest.xml s'ajoutent ou remplacent les métadonnées existantes.

4. Alimentation des registres de fond : TODO (se comporte comme pour un ajout d'Unité Archivistique pour les existants)

Evolutions futures
******************

Toute Archive Unit déclarée dans le manifest sera mise à jour en base car il est obligatoire de déclarer le *<Title>..</Title>* et le *<DescriptionLevel>..</DescriptionLevel>*, il n'est donc pas possible de déclarer que les métadonnées Archive Unit ne doit pas être modifiées.
Une évolutions de la xsd du manifest seda sera faite.

De plus, il faut définir une tâche dans le step de vérification du borderau pour apporter une information explicitant quand même la mise à jour. Elle pourrait correspondre techniquement à la vérification de l'existence de l'ArchiveUnit et pourrait s'intituler "Vérification de l'existence de l'unité archivistique en vue d'une mise à jour" ou quelque chose comme cela.


Mise à jour du schéma XSD SEDA
==============================

Cette partie a pour but de présenter l'implémentation qui a été mise en place pour enrichir la norme SEDA.

Le but étant de proposer la mise à jour d'AU via le langage XML.

Les différents types de mises à jour :
 - Premier type de mise à jour : mise à jour de contenu (de métadonnées ou encore de règles de gestion). Il pourra être question de modifications de contenu (mise à jour / enrichissement de titre, description, ou ajout de nouvelles métadonnées, etc.). On peut déterminer d'ores et déjà que l'on pourra proposer 2 modes d'updates : un mode PATCH (modification d'un ou plusieurs champs particulier) et un mode FULL (annule et remplace).
 - Deuxième type de mise à jour : la mise à jour de liens. Il s'agit ici de modification (ajout / suppression) de liens entre plusieurs AU. Pour respecter la logique SEDA, les modifications (ajouts et suppressions) de liens se feront toujours du père vers les fils (elles seront déclarées dans l'AU père).

Pour pouvoir décrire ce genre d'opérations via un xml, il convient donc d'étendre le schéma SEDA existant. Pour se faire, il a fallu implémenter un élément abstract : OtherManagementAbstract à l'intérieur de la balise Management pour un ArchiveUnit donné.

L'implémentation choisie porte le nom "UpdateOperation". Pour toute opération de mise à jour, il faudra donc préciser une balise "UpdateOperation".

.. code-block:: xml

   <ArchiveUnit>
       <!-- ADDITIONNAL INFORMATION -->
       <Management>
       <!-- ADDITIONNAL INFORMATION -->
         <UpdateOperation>
            <!-- ADDITIONNAL INFORMATION -->
         </UpdateOperation>
       </Management>
   <ArchiveUnit>


Précision du SystemId
*********************
Pour pouvoir indiquer simplement au moteur VITAM que l'ArchiveUnit que l'on tente de mettre à jour est déjà existant dans le système, un champ SystemId doit être indiqué.
Pour le moment, l'identification d'un archive unit pré-existant dans le système Vitam se fera via son GUID. Plus tard, on étudiera la possibilité de passer par un système de requête pour sélectionner un AU.

.. code-block:: xml

   <ArchiveUnit>
       <!-- ADDITIONNAL INFORMATION -->
       <Management>
       <!-- ADDITIONNAL INFORMATION -->
         <UpdateOperation>
            <SystemId>GUID_MY_ARCHIVE_UNIT</SystemId>
         </UpdateOperation>
       </Management>
   <ArchiveUnit>

On utilise toujours, au sein du SEDA, les « XML ID » afin de définir des liens entre plusieurs AU.
Le système est capable de récupérer les objets concernés grâce à la balise SystemId (id interne à VITAM) s’ils existent déjà dans le SAE.
Ainsi, pour créer un lien entre deux AU existants dans le système, il faudra les déclarer tous les deux dans le SEDA via le XML ID tout en indiquant le SystemID (GUID).

Mise à jour de contenu
**********************
En plus de l'ajout de l'information sur le SystemId pour indiquer au système que l'AU existe déjà, si l'on souhaite effectuer une mise à jour de son contenu, il conviendra, en plus de la précision des données modifiées (dans la partie Content, pas de modification, de ce côté) préciser le type de mise à jour souhaité : total ou partiel.
Une balise FullUpdate permettra de spécifier le type de mise à jour.

TODO : remplacer FullUpdate (false/true) par updateMode (NO_DATA, PATCH, FULL)

Pour une mise à jour partielle (seuls les champs précisés dans Content seront mis à jour) :

.. code-block:: xml

   <ArchiveUnit>
       <!-- ADDITIONNAL INFORMATION -->
       <Management>
         <!-- Nouvelle regle -->
         <DisseminationRule>
            <Rule>DIS-00001</Rule>
            <StartDate>2008-07-14</StartDate>
         </DisseminationRule>
         <UpdateOperation>
            <SystemId>GUID_MY_ARCHIVE_UNIT</SystemId>
            <FullUpdate>false</FullUpdate>
         </UpdateOperation>
       </Management>
       <Content>
         <!-- Nouvelle titre -->
         <Title>Mon nouveau Titre</Title>
       </Content>
   <ArchiveUnit>

Pour une mise à jour complète (annule et remplace) :

.. code-block:: xml

   <ArchiveUnit>
       <!-- ADDITIONNAL INFORMATION -->
       <Management>
         <DisseminationRule>
            <Rule>DIS-00001</Rule>
            <StartDate>2008-07-14</StartDate>
         </DisseminationRule>
         <UpdateOperation>
            <SystemId>GUID_MY_ARCHIVE_UNIT</SystemId>
            <FullUpdate>true</FullUpdate>
         </UpdateOperation>
       </Management>
       <Content>
         <DescriptionLevel>Item</DescriptionLevel>
         <Title>Histoire de la station de sa cération à 1946.pdf</Title>
         <TransactedDate>2015-12-04T09:02:25</TransactedDate>
       </Content>
   <ArchiveUnit>

Suppression de lien
-------------------


.. image:: images/ua_remove_link.jpeg


La suppression d'un lien entre un AU père et un AU fils sera obligatoirement déclaré sur l'AU père, pour respecter la logique SEDA.
Une balise ToDelete permettra de lister les liens entre l'AU père et ses AU fils référencés.

Pour la suppression d'un lien :

.. code-block:: xml

   <ArchiveUnit id="ID_PERE">
       <!-- ADDITIONNAL INFORMATION -->
       <Management>
       <!-- ADDITIONNAL INFORMATION -->
         <UpdateOperation>
            <SystemId>GUID_ARCHIVE_UNIT_PERE</SystemId>
            <ToDelete>
               <ArchiveUnitRefId>XML_ID_FILS_1</ArchiveUnitRefId>
            </ToDelete>
         </UpdateOperation>
       </Management>
   <ArchiveUnit>

   <ArchiveUnit id="ID_FILS_1">
      <Management>
       <!-- ADDITIONNAL INFORMATION -->
         <UpdateOperation>
            <SystemId>GUID_ARCHIVE_UNIT_FILS_1</SystemId>
         </UpdateOperation>
       </Management>
   <ArchiveUnit>

A l'intérieur de la balise ArchiveUnitRefId, on doit référencer un XML ID. C'est à dire un ID interne au xml. Il ne s'agit donc pas ici d'un GUID référencé dans le système Vitam, mais bien une référence à un AU déclaré dans le manifest.
Dans le manifest doit donc être précisé également le SystemId de l'AU fils référencé, comme indiqué dans l'exemple ci-dessus, sinon le xml ne sera pas valide.

Ajout de lien
-------------

L'ajout de lien entre un AU père et un AU fils sera obligatoirement déclaré sur l'AU père, pour respecter la logique SEDA.
En complément de l'utilisation de la nouvelle balise SystemId, il conviendra d'utiliser la balise existante prévue par la norme SEDA : ArchiveUnitRefId.

Quatre cas sont possibles :
 - Ajout d'un lien entre 2 AU existants déjà dans le système VITAM.


 .. image:: images/ua_add_link.jpeg


 - Ajout d'un nouvel AU fils à un AU père déjà existant dans le système VITAM.


 .. image:: images/ua_add_child_and_link.jpeg


 - Ajout d'un AU fils à un nouvel AU père non existant dans le système VITAM.


 .. image:: images/ua_add_parent_and_link.jpeg


 - Ajout d'un nouvel AU fils à un nouvel AU père.

Pour le cas 1 (Ajout d'un lien entre 2 AU existants déjà dans le système VITAM) :

.. code-block:: xml

   <ArchiveUnit id="ID_PERE">
       <!-- ADDITIONNAL INFORMATION -->
       <Management>
       <!-- ADDITIONNAL INFORMATION -->
         <UpdateOperation>
            <SystemId>GUID_ARCHIVE_UNIT_PERE</SystemId>
         </UpdateOperation>
       </Management>
       <ArchiveUnitRefId>ID_FILS_1</ArchiveUnitRefId>
   <ArchiveUnit>

   <ArchiveUnit id="ID_FILS_1">
      <Management>
       <!-- ADDITIONNAL INFORMATION -->
         <UpdateOperation>
            <SystemId>GUID_ARCHIVE_UNIT_FILS_1</SystemId>
         </UpdateOperation>
       </Management>
   <ArchiveUnit>

Pour le cas 2 (Ajout d'un nouvel AU fils à un AU père déjà existant dans le système VITAM) :

.. code-block:: xml

   <ArchiveUnit id="ID_PERE">
       <!-- ADDITIONNAL INFORMATION -->
       <Management>
       <!-- ADDITIONNAL INFORMATION -->
         <UpdateOperation>
            <SystemId>GUID_ARCHIVE_UNIT_PERE</SystemId>
         </UpdateOperation>
       </Management>
       <ArchiveUnitRefId>ID_FILS_1_NOUVEAU</ArchiveUnitRefId>
   <ArchiveUnit>

   <ArchiveUnit id="ID_FILS_1_NOUVEAU">
      <Management>
         <!-- Information sur le management -->
      </Management>
      <Content>
         <!-- Information sur le content -->
      </Content>
   <ArchiveUnit>

Pour le cas 3 (Ajout d'un AU fils à un nouvel AU père non existant dans le système VITAM) :

.. code-block:: xml

   <ArchiveUnit id="ID_PERE">
      <Management>
         <!-- Information sur le management -->
      </Management>
      <Content>
         <!-- Information sur le management -->
      </Content>
      <ArchiveUnitRefId>ID_FILS_1_EXISTANT</ArchiveUnitRefId>
   <ArchiveUnit>
   <ArchiveUnit id="ID_FILS_1_EXISTANT">
       <!-- ADDITIONNAL INFORMATION -->
       <Management>
       <!-- ADDITIONNAL INFORMATION -->
         <UpdateOperation>
            <SystemId>GUID_ARCHIVE_UNIT_FILS_1_EXISTANT</SystemId>
         </UpdateOperation>
       </Management>
   <ArchiveUnit>

Pour le cas 4 (Ajout d'un nouvel AU fils à un nouvel AU père - cas nominal) :

.. code-block:: xml

   <ArchiveUnit id="ID_PERE">
      <Management>
         <!-- Information sur le management -->
      </Management>
      <Content>
         <!-- Information sur le management -->
      </Content>
      <ArchiveUnitRefId>ID_FILS_1_NOUVEAU</ArchiveUnitRefId>
   <ArchiveUnit>
   <ArchiveUnit id="ID_FILS_1_NOUVEAU">
      <Management>
         <!-- Information sur le management -->
      </Management>
      <Content>
         <!-- Information sur le management -->
      </Content>
   <ArchiveUnit>


Exemples de mise à jour
=======================

Vous trouverez ci-dessous des exemples d'utilisation (à adapter, bien évidemment) d'utilisation des différentes opérations d'update.

Initialisation
**************
Pour pouvoir effectuer des opérations de mise à jour, il convient d'effectuer un ingest.
Pour notre exemple, nous allons réaliser l'import d'un SIP d'origine renseignant :

- 1 ArchiveUnit XML_ID1.
- 1 ArchiveUnit XML_ID2 seul.
- 1 ArchiveUnit XML_ID_FILS1 rattaché à l'ArchiveUnit XML_ID1. Cet ArchiveUnit référençant un DataObject, déclaré dans le manifest.
- 1 ArchiveUnit XML_ID_FILS2 rattaché à l'ArchiveUnit XML_ID1.

Le SIP d'initialisation se trouvant ici : :download:'<files/SIP_INIT.zip>'
Le manifest : :download:'<files/manifest_INIT.xml>'

Mise à jour de contenu simple
*****************************
Le but ici est de mettre à jour les métadonnées de l'ArchiveUnit XML_ID1 d'origine.
Le manifest permettant de faire cette mise à jour : :download:'<files/manifest_UPDATE_CONTENT.xml>'

Ci-dessous un extrait de la syntaxe :

.. code-block:: xml

   <UpdateOperation>
         <SystemId>aeaaaaaaaaaam7mxab2kkakzn5mib7aaaaaq</SystemId>
         <FullUpdate>false</FullUpdate>
   </UpdateOperation>

*Note* : il conviendra de remplacer le contenu de la balise <SystemId> par l'identifiant interne généré lors de l'ingest original.

Après l'import du SIP contenant ce manifest (pas d'objet nécéssaire dans le SIP), les informations de l'ArchiveUnit XML_ID1 seront mises à jour.

Ajout d'un lien
***************
Le but ici est l'ajout d'un lien entre l'ArchiveUnit XML_ID2 et l'ArchiveUnit XML_ID_FILS2.
Le manifest permettant de faire cette mise à jour : :download:'<files/manifest ADD_LINK.xml>'

Ci-dessous un extrait de la syntaxe :

.. code-block:: xml

   <!-- Dans la balise Management de l'ArchiveUnit XML_ID2 -->
   <UpdateOperation>
      <SystemId>aeaaaaaaaaaam7mxab2kkakzn5micdiaaaaq</SystemId>
   </UpdateOperation>
   <!-- Puis plus loin toujours dans la balise ArchiveUnit de XML_ID2 -->
   <ArchiveUnit id="XML_ID21">
        <ArchiveUnitRefId>XML_ID_FILS2</ArchiveUnitRefId>
    </ArchiveUnit>

*Note* : il conviendra de remplacer les contenus des balises <SystemId> par les identifiants internes générés lors de l'ingest original.

Après l'import du SIP contenant ce manifest (pas d'objet nécéssaire dans le SIP), l'ArchiveUnit XML_ID2 sera un père de l'ArchiveUnit XML_ID_FILS2.

Suppression d'un lien
*********************
Le but ici est la suppression d'un lien existant entre l'ArchiveUnit XML_ID1 et l'ArchiveUnit XML_ID_FILS2.
Le manifest permettant de faire cette mise à jour : :download:'<files/manifest DELETE_LINK.xml>'

Ci-dessous un extrait de la syntaxe :

.. code-block:: xml

   <UpdateOperation>
      <SystemId>aeaaaaaaaaaam7mxab2kkakzn5mib7aaaaaq</SystemId>
      <ToDelete>
         <ArchiveUnitRefId>XML_ID_FILS2</ArchiveUnitRefId>
      </ToDelete>
   </UpdateOperation>

*Note* : il conviendra de remplacer les contenus des balises <SystemId> par les identifiants internes générés lors de l'ingest original.

Après l'import du SIP contenant ce manifest (pas d'objet nécéssaire dans le SIP), l'ArchiveUnit XML_ID1 ne sera plus un père de l'ArchiveUnit XML_ID_FILS2.

Mise à jour via les IHM
=======================

En ce qui concerne l'utilisation via les IHM minimales, ce n'est pas encore possible. Cette évolution sera dévelopée lors d'une US associée.

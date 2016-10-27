Common format identification
############################

But de cette documentation
==========================

Cette documentation indique comment utiliser le code commun du format identifier pour éventuellement ajouter un client pour un nouvel outil.

Outil Format Identifier
=======================

L'interface du format identifier est *fr.gouv.vitam.common.format.identification.FormatIdentifier*.
Elle met à disposition 2 méthodes : 
 - status() qui renvoie le statut du format identifer
 - analysePath(Path) qui renvoie une liste de formats potentiellement identifiés par l'outil.
 
 Une implémentation Mock est présente : *fr.gouv.vitam.common.format.identification.FormatIdentifierMock*
 
 Chaque nouvel outil doit implémenter l'interface : 
 
.. code-block:: java

   public class FormatIdentifierSiegfried implements FormatIdentifier {
      @Override
      public FormatIdentifierInfo status() { //CALL THE TOOL AND GET THE STATUS }
      
      @Override
      public List<FormatIdentifierResponse> analysePath(Path path) { //CALL THE TOOL AND ANALYSE}
   }
 
De plus, pour pouvoir être utilisé, l'outil doit être ajouté dans l'enum FormatIdentifierType :

.. code-block:: java
   public enum FormatIdentifierType {
       MOCK,
       SIEGFRIED
   }
      
Une factory a été mise en place pour récupérer l'instance du client adaptée. En cas de nouvel outil, il faut la mettre à jour : 

.. code-block:: java
   public class FormatIdentifierFactory {
       ......
       
       private FormatIdentifier instanciate(String formatIdentifierId){
         ...
           switch (infos.getType()) {
               case MOCK:
                   return new FormatIdentifierMock();
               case SIEGFRIED:
                   return new FormatIdentifierSiegfried(infos.getConfigurationProperties());
            .....
                   
          }
   }

Configuration :
===============

Dans **/vitam/conf** du serveur applicatif où sont déployés les services d'identification de formats, il faut un fichier **format-identifiers.conf**. C'est un fichier YAML de configuration des services d'identification de format.  Il possède les configurations des services que l'on souhaite déployer sur le serveur.

Le code suivant contient un exemple de toutes les configurations possibles :

.. code-block:: yaml

   siegfried-local:
      type: SIEGFRIED
      client: http
      host: localhost
      port: 55800
      rootPath: /root/path
      versionPath: /root/path/version/folder
      createVersionPath: false
   mock:
      type: MOCK

Pour plus d'informations sur le sujet, voir la documentation sur l'exploitation.
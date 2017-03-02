Généralités
***********

Cette section présente les modules & services pour traiter le processus de notification du téléchargement 
d'un document SIP. 

1. Génération et stockage de la notification : 
 worker/worker-core 
 - Le schéma de validation de la réponse XSD : le schéma de validation du fichier XML de la réponse 
 de notification est src/main/resources/seda-2.0-main.xsd.
   
 - La génération du fichier XML de la réponse de notification est faite par  l'XML Stream pour les éléments 
 en dehors de ReplyOutcome et par des POJO JAXB pour les éléments à itérer (ArchiveUnit, BinaryDataObject). 
 Les modèles Object Element POJO de ces deux éléments se trouvent dans fr.gouv.vitam.worker.model 
 (DataObjectTypeRoot.java et ArchiveUnitReplyTypeRoot)   
 
 - Handlers: 
  le handler ExtractSedaActionHandler est modifé pour extraire des information nécessaire depuis le SIP pour 
  générer la réponse de notification, à savoir : le map des BDOs et sa version, le json contenant des informations 
  hors Archive Unit et Binary Data Object de SEDA. 
  
  le handler TransferNotifcationActionHandler est ajouté pour l'opération de création de la réponse de notification : 
  création de fichier XML à partir des données générées dans le workflow, validation du fichier, effectuer la 
  sauvergade de la réponse dans le workspace.     
      
 
 storage/storage-engine : 
 - crééer la collection (report) pour sauvegarder des réponses de notification.
 - fournir le serive de sauvegarder de la réponse comme un document de workspace. Ce service est défini sur différens 
 niveaux à savoir API/Rest/Client    
 
2. Téléchargement de la notification 
   storage/storage-engine
   ingest/ingest-internal
   ingest/ingest-external
   ihm-demo/ihm-demo-web-application
   
   Tous ces 4 services sont mis à jour pour récupérer la réponse de notification sauvegardée dans le storage.  
   
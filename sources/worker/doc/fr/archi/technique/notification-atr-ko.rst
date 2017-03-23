Généralités
***********

Cette section présente les modules & services pour traiter le processus de notification en erreur de l'upload 
d'un document SIP. 

1. Génération et stockage de la notification : 
 worker/worker-core 
 - Le schéma de validation de la réponse XSD : les schémas différents de validation du fichier XML de la réponse 
 de notification est : src/main/resources/seda-2.0-main.xsd.
   
 - La génération du fichier XML de la réponse de notification est faite par  l'XML Stream.    
 
 - Workflow :
 Le workflow a été modifié, un behaviour "Finally" a été ajouté. A l'image du Finally java, il permet d'exécuter une étape quoi qu'il se passe
 dans le process d'ingest d'un SIP. Ceci permet la génération d'une notification KO. Le workflow a été adapté pour que l'on puisse également générer 
 une notification OK dans le cadre d'un succès.
 
 - Handlers: 
  le handler TransferNotifcationActionHandler a été modifié pour pouvoir répondre au besoin de génération d'un XML KO : 
  création de fichier XML à partir des données générées dans le workflow (logbook opération, logbook lifecycle unit et object group), effectuer la 
  sauvergade de la réponse dans le workspace.     
      
 
 storage/storage-engine : 
 - utilisation de l'API REST pour pouvoir sauvegarder la réponse.
 
2. Téléchargement de la notification 
   storage/storage-engine
   ingest/ingest-internal
   ingest/ingest-external
   ihm-demo/ihm-demo-web-application
   
   Tous ces 4 services sont mis à jour pour récupérer la réponse de notification sauvegardée dans le storage.  
   
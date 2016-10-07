Généralités
***********

Cette section présente le processus pour notifier le résultat d'un téléchargement d'un document SIP. 

Lorsque le SIP passe toutes les étapes du workflow d'entrée avec succès, Vitam lui envoie  une 
notification au service versant dans ce cas. La procédure se compose deux étapes : la génération 
d'une notification et le téléchargement de la notification   

- Génération et stockage de la notification : à partir de SIP versant, nous devrons générer une réponse en 
format XML en utilisant les informations précisées dans le SEDA et l'information sur le workflow exécuté. 
La réponse doit être validé par le schéma XSD pré-défini pour le format de la réponse de notification. 
Cette notification sera sauvegardé dans l'espace de stockage.   
 
- Téléchargement de la notification 
Dans l'interface de téléchargement du SIP, lors d'un UPLOAD succès, nous trouvons le status OK de UPLOAD SIP 
et en bas nous trouvons aussi un lien pour télécharger la notification générée dans l'étape précédente. 
    	
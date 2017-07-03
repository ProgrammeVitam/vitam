Généralités
***********

Cette section présente le processus pour notifier le résultat négatif d'un téléchargement d'un document SIP. 

Lorsque le SIP provoque une erreur au niveau du workflow d'entrée, une étape finale est exécutée.
Elle a pour but la génération d'une notification d'erreur au service versant.

La procédure se compose deux étapes : la génération d'une notification et le téléchargement de la notification   

- Génération et stockage de la notification : 

A partir du SIP soumis par le service versant, nous devrons générer une réponse en format XML en utilisant les informations précisées dans le SEDA et l'information sur le workflow exécuté. 
La réponse doit être validé par le schéma XSD pré-défini pour le format de la réponse de notification. 
Cette notification sera sauvegardé dans l'espace de stockage.
 
- Téléchargement de la notification

Dans l'interface de téléchargement du SIP, lors d'un UPLOAD en erreur, l'icone KO sera affiché, et le xml sera téléchargé automatiquement.
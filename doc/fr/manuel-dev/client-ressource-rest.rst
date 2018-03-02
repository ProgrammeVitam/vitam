Ressources et clients
#####################

Ressources
==========

Le développement des classes REST (Resource) mettant a disposition les points d'API doit respecter les règles suivantes :

 - Déclarer un Path qui ne risque pas d'entrer en conflit avec un autre
 - Déclarer un "@produce" et un "@consume" en accordance avec le verbe HTTP utilisé :
   - Pas de "@produce" dans le cas du HEAD
   - Suite à la mise en place de RESTEASY, tout objet envoyé en body de requête ne peut être null
 - Si un point d'API renvoie un résultat, il doit uniquement renvoyer au choix :
   - un objet RequestResponse<T> où T doit être un POJO (autre que JsonNode) dans l'entity de l'objet Response. Attention, le status code de l'objet RequestResponse doit être cohérent avec celui de la Response
   - un stream dans l'entity de la response
 - Les erreurs sont renvoyées sous la forme d'un objet VitamError.

Client
======

Le développement des clients vitam (interne et externe) doit respecter les règles suivantes :

 - Deux types de réponses peuvent être renvoyés :
   - un objet RequestResponse<T> où T doit être un POJO (autre que JsonNode)
   - un objet Reponse uniquement de le cas où la réponse est un stream
 - Le client ne doit pas intépréter une réponse dont le format est correct (et ce même si le status n'est pas OK)
 - Les seules exceptions qui peuvent être renvoyées sont celles générées par le client lui-même, elles doivent toutes être des VitamClientException
 - Les clients ne doivent pas utiliser la dépendance common-private

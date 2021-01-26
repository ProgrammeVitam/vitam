Implémentation du secret de la plateforme
################################################

Présentation
------------
Le secret de plateforme permet de se protéger contre des erreurs de manipulation et de configuration
en séparant les environnements de manière logique (secret partagé par l'ensemble de la plateforme mais différent entre plateforme).

Implémentation
--------------

* Un Header X-Request-Timestamp contenant le timestamp de la requête sous forme epoch (secondes depuis 1970)
* Un Header X-Platform-ID qui est SHA256("<methode>;<URL>;<Valeur du header X-Request-Timestamp>;<Secret partagé de plateforme>").

	Par contre, mettre le secret de plateforme à la fin permet de limite les attaques par extension.

Si on veut assurer une sécurité additionnelle, il est possible de transmettre un hash des valeurs suivantes :

- URI + paramètres de l'URI
- Header Timestamp
- Secret de plateforme en clair non transmis (connus par les participants de la plateforme)

=> Hash (URI + paramètres (dans l'ordre alphabétique) + Header Timestamp + secret non transmis)
Ce Hash est transmis dans le Header : X-Platform-Id

Le contrôle est alors le suivant :

1) Existence de X-Platform-Id et Timestamp ; Dans le cas contraire, la requête est refusée.
2) Vérification que Timestamp est distant de l'heure actuelle sur le serveur requêté de moins de x secondes (``| Timestamp - temps local | < x s`` ). Si la différence de temps est supérieure au seuil acceptable (10s par défaut), alors des erreurs sont tracées dans les logs et des alertes sont remontées dans le dashboard Kibana "Alertes de sécurité". Au delà d'un seuil critique (60s par défaut), la requête est refusée.
3) Calcul d'un Hash2 = Hash(URI+paramètres (dans l'ordre alphabétique) + Header Timestamp + secret non transmis) et vérification avec la valeur Hash transmise ; En cas d'échec de validation, la requête est refusée.

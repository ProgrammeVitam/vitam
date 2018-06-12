Tests curl
##########

Introduction
============

cUrl, est l'abréviation de "client URL request library". 

C'est un outil en ligne de commande permettant notamment, dans le cas qui nous intéresse, de simuler des requêtes HTTP. 

Il permet entre autres d'exécuter toutes les méthodes offertes par le REST : POST, PUT, GET, DELETE, HEAD.

Cette documentation a pour but de fournir une panoplie de jeux de test curl afin de pouvoir tester au maximum les API offertes par VITAM.

Pour toutes ces requêtes, il conviendra d'adapter l'uri en fonction de l'environnement à tester (eg: remplacer {env.programmevitam.fr}).

API Externes
============

Ingest
------

*Ingest d'un fichier se trouvant dans le dossier baseUploadPath*
Le fichier doit se trouver dans le répertoire configuré dans le fichier de configuration (baseUploadPath) puis, exécuter :
``curl -v -X POST -k --key vitam-vitam_1.key --cert vitam-vitam_1.pem '{env.programmevitam.fr}/ingest-external/v1/ingests' -H 'X-Tenant-Id: 0' -H 'X-Access-Contract-Id: ContratTNR' -H 'Content-Type: application/json;charset=UTF-8' -H 'Accept: application/json' -H 'X-Context-Id: DEFAULT_WORKFLOW' -H 'X-ACTION: RESUME' --data-binary '{"path": "SIP_OK_2_0.zip"}'``

Access
------

Units
^^^^^

*Mise à jour d'une unité archivistique :*
``curl -v -X PUT -k --key vitam-vitam_1.key --cert vitam-vitam_1.pem 'https://{env.programmevitam.fr}/access-external/v1/units/aeaqaaaaaahmtusqabz5oalc4p2zu5aaaaaq' -H 'X-Tenant-Id: 0' -H 'X-Access-Contract-Id: ContratTNR' -H 'Content-Type: application/json' -H 'Accept: application/json' --data-binary '{ "$action": [ { "$set": { "Title": "Montparnasse.txt" } } ] }' --compressed``


Administration fonctionnelle
----------------------------

*Import d'un référentiel d'agents :*
``curl -v -X POST -k --key vitam-vitam_1.key --cert vitam-vitam_1.pem 'https://{env.programmevitam.fr}/admin-external/v1/agencies' -H 'X-Tenant-Id: 0' -H 'X-Access-Contract-Id: ContratTNR' -H 'Content-Type: application/octet-stream;charset=UTF-8' -H 'Accept: application/json' --data-binary "@agencies.csv" -H 'Transfer-Encoding: chunked' --compressed``

//TODO

API Internes
============

Ingest
------
//TODO

Access
------
//TODO

Administration fonctionnelle
----------------------------
//TODO

Notes
=====

Personae
--------

Pour toutes les requêtes curl de cette documentation, il est possible d'y ajouter un header qui sera pris en compte dans les journaux d'opération : X-Personal-Certificate.
Pour réaliser une telle prouesse, il s'agit simplement d'ajouter ceci à la commande curl ciblée (modifier le certificat en base 64 avec quelque chose de valable dans l'environnement, cf la collection PersonalCertificate) : 
``-H 'X-Personal-Certificate:MIIFRjCCAy6gAwIBAgIBAjANBgkqhkiG9w0BAQsFADAtMQswCQYDVQQGEwJGUjEOMAwGA1UEBxMFUGFyaXMxDjAMBgNVBAoTBVZJVEFNMCAXDTE3MDgwMTExMTcwMFoYDzk5OTkxMjMxMjM1OTU5WjAtMQswCQYDVQQGEwJGUjEOMAwGA1UEBxMFUGFyaXMxDjAMBgNVBAoTBVZJVEFNMIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAmfwb+NP44Ygv94LOOTLhQdDLwwqiwuP3fe3qFs0hCWCEOIorFcJ3cwZ2tc8udFtK8HxLrLxwi7zZweGrwXjt4zfLtfregppt0Xw5RaJtgNReu5i/2AKgtcxscYH/0yG1bDQ3vT2tv0YH4jzdfXfwTVzytqAV1M/CNZlWbcBXqDyZLeYUm5i/Dufndj16j4hw24tBsQT1o92P5qdfPaieZc4jpscGiMmyNYwEKcbqo5wiGVsiD+sU9/JXHT2q1f18JcuwJ5/fqzsADPKXudBvibCSaANf+ZNpRaWZ7y6e/kUDs8yrp4YaXzb331ioOGk4JE9ylv1hY5l8IbbvWracaxJv3xm3EnIp9M2/VMHrrGlkVjmGBUydJDiRhUAgaqXNpezwWulweQunAelBCU4PjO40J6t2wdLi5+f+b0OLJHJg0N9xdFsKrqsAVpjaYpqnDAG2Evcw0GFUuFm10JVLCAVpi6EwgxMnwExaeSrUvNE7Sdu95z2G8yBR9tYvYve+iiq/LzkR3cxK+9Pw4xDIEgQ0ZTCvY/6SBnHdAe3tqs4kODs57BZW4DD9ytpT73BKMf7EeZAE3tJd9p40uw/b41VF9bJvoW1ammZMl4H2OwdJi7+5DAMbBC1X2kMGWRo06IM99q+TKpfrUK+p4b6NfcSdrfr+n28vd8pzp16VDoMCAwEAAaNvMG0wDAYDVR0TAQH/BAIwADAdBgNVHQ4EFgQUx0RltSTqHWXEisK78KQzn2SRpIkwCwYDVR0PBAQDAgSwMBEGCWCGSAGG+EIBAQQEAwIFoDAeBglghkgBhvhCAQ0EERYPeGNhIGNlcnRpZmljYXRlMA0GCSqGSIb3DQEBCwUAA4ICAQAZAZyzj7c5KBDLp0K324jUJB+oJAuf/D0vh0FqKvlCBTOLJsLfO2hsEL8ude5rVhP4goThIz1OjpnxFP+YmHUOtiQup21VGTaeTWn769/x6gRx/1eyJyws4ien/w7gBASLEKI7nGYAkeoYeZKWYTlfBgEisLwSsjcQeBeKcnUnuWJauiALPnBntkAnM7PotASA8Hk+dle9lng1sMlzHVcTVauCuvrk8WCec9ja56+b9N4JbaCwYFmMRlMzdBQU4LXrbqxlakpa2ua0mSzCKe8WHI9m5uCHhUi3fMa7KJsN5nBHkw63nFwGQwyRNQYgZiyhmzXtez/l+8f1quMAPoTIlsG+TBFW0s9+LqY8ufE9+8u8S1FynZlsgfIoKl2bKVXWWrZVfJ+S8mh6mH4V3MuhLwljv+/6HDZCc3FoY5eN/lyWI49Maz5W87bKqNyecYtrBlvML7k5UeOLtgNuUsTBlzFTxMkaQHOSpMyrHZ/yVPNVfuP3cCKvzMPHFGHzJZK0qvz4zdFdx7YzBq+I6YLvRES9b+DkvdrTOpZI2GjKuP5m13kcUjsFeqJR6rb+o1kJuCj/QMC2OjMXMlDqNa8mL5ooGQmYOzHkfq4vdKLG/Fvbpw2DDrwv9jKmw2l6eWLYzuIpvz7sqUHwi30wScXSm/FCKF9DjzODUpSkBvDiaA=='``

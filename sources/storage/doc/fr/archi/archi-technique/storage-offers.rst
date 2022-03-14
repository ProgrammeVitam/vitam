Storage Offers
**************

Présentation
------------

|  *Parent package:* **fr.gouv.vitam.storage**
|  *Package proposition:* **fr.gouv.vitam.storage.offers**

Module embarquant les différentes offres de stockage Vitam ainsi que leur drivers associés.

Actuellement, ce module embarque :

- une seule offre de stockage, appelée vitam-offer, qui supporte plusieurs types de persistance : système de fichiers, Swift, S3 ou sur bande magnétique.
- un seul driver (utilisé par storage-offer-default) appelé vitam-driver. Il permet d'utiliser l'offre par défaut qu'elle soit en mode swift, s3, système de fichiers ou sur bande magnétique.
- Il est possible, grace à plusieurs instance de l'offre par défaut d'avoir un stockage multi offres. Il existe une
limite, au sein de la meme JVM, il n'est possible de n'avoir qu'une seul offre d'un seul type.


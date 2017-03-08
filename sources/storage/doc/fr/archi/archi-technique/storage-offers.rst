Storage Offers
**************

Présentation
------------

|  *Parent package:* **fr.gouv.vitam.storage**
|  *Package proposition:* **fr.gouv.vitam.storage.offers**

Module embarquant les différentes offres de stockage Vitam ainsi que leur drivers associés.

Actuellement, ce module embarque :
- une seule offre de stockage, appelée vitam-offer. Cependant, elle permet d'etre de deux types différents
: système de fichier ou swift
- un seul driver (utilisée par storage-offer-default) appelé vitam-driver. Il permet d'uliser l'offre par défaut
qu'elle soit en mode swift ou en mode system de fichier
- Il est possible, grace à plusieurs instance de l'offre par défaut d'avoir un stockage multi offres. Il existe une
limite, au sein de la meme JVM, il n'est possible de n'avoir qu'une seul offre d'un seul type.


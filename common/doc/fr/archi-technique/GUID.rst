Introduction
############

Le sujet porte notamment sur les **GUID**.


Identifiant Vitam
*****************

La logique est d'utiliser des GUID (Global Unique Identifier) pour chacun des éléments dans Vitam (Unit, Groupe d'objet, Objets mais aussi Journaux, Logs, Services, ...).

Le GUID s'appuie sur l'objet ServerIdentity que chaque Service (JVM) doit instancié correctement.


Forme d'un identifiant Vitam
============================

* Identifiant en base 32 (pour des raisons de lisibilité et d'éviter des erreurs de transcription)

* Longueur de 36 caractères base 32 représentant 22 octets natifs

* L'identifiant ne doit pas être trop long car il coûte en mémoire et sur disque

  * pour 10 milliards d'objets, on peut estimer qu'un octet coûte 100 Go sur disques et 1 Mo en mémoire par serveur


* La composition de l'identifiant serait a priori la suivante : **22 octets** **soit 168 bits**

  * Une version de l'algorithme d'identifiant entre 0 et 255 (**8 bits**)

  * Un identifiant de type d'objets entre 0 et 255 (Unit, Groupe d'objets, Objet, Entrée, Transfert, Journal, ...) (**8 bits**)

  * Un domaine métier = tenant entre 0 et 2^30-1 permettant une distribution par tenant, correspondant au NAAN de ARK (**30 bits**)

    * ARK impose une longueur de 5 ou 9 caractères en numérique uniquement

    * Compte tenu que la liste ARK dépasse déjà 95 000, il faudrait peut-être anticiper la taille à 9 chiffres

  * Un identifiant de plateforme entre 0 et 2^31-1 permettant une distribution par instance Vitam (**31 bits**)

    * Cet identifiant serait en 2 parties : partie fixe par plate-forme (1 par site ou 1 pour 3 sites), partie variable par instance de host (VM)

    * La partie plate-forme devrait permettre 2^20-1 items, soit 20 bits

    * La partie par instance de host devrait permettre 2^11-1 items, soit 11 bits

    * Cet identifiant est assimilable à une adresse MAC mais dont la garantie n'est pas suffisamment fiable en virtuel (assignation dynamique de MAC address)

    * Cet identifiant de 31 bits pourrait aussi être utilisé dans d'autres cas que Vitam pur, comme dans une offre de stockage pour gérer la distribution

      * Par exemple : Distribution sur les Cas Container sur 20 bits et distribution d'un Cas Storage dans un Cas Container sur 11 bits

  * Un identifiant de processus attribuant l'Id (0 à 2^22-1) (**22 bits**)

  * Le temps UTC exprimé en millisecondes entre 0 et 2^48-1 (8 925 années après 1970) (**48 bits**)

  * Un compteur discriminant de milliseconde entre 0 et 2^24-1 (**24 bits**)

    * risque de collisions autour de 2^17 ~ 100K GUID générés par millisecondes, donc avec la progression des puissances de calculs sur 20 ans (Loi de Moore approchée : *2 tous les 3 ans) = 2^7+17 = 2^24

Certains bits ne sont pas utiliser (5) pour de futures usages.


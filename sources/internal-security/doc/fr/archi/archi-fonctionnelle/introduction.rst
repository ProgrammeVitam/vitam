Introduction
############

But de cette documentation
**************************

L'objectif de cette documentation est d'expliquer l'architecture fonctionnelle de ce module.


Security-internal
*****************
Le rôle de security-internal est de gérer les certificats applicatifs ainsi que les certificats personnels :

* Les certificats applicatifs sont associés aux SIA, et sont utilisés pour valider le certificat TLS d'appel à Vitam.

* Les certificats personnels sont utilisés dans l'authentification personae pour les endpoints dits "sensibles".

Introduction
#############

Les certificats applicatifs (SIA) et personnels sont stockés en base dans la collection MongoDB Identity dans les
collections Certificate et PersonalCertificate respectivement.
Ils sont indexés en base via leur hash (SHA256 du certificat encodé au format DER).

Le contrôle du certificat applicatif permet de vérifier si le certificat d'authentification TLS du SIA utilisé pour
appeler Vitam est bien authorisé. Il permet également de récupérer l'identifiant du contexte associé.

Le contrôle d'accès sur les certificats personnels sont fait uniquement si le endpoint externe cible requière une
authentification forte. La liste des endpoints nécessitant une authentification personnelle ou non est défini dans
la configuration du module security-internal.

En cas d'échec de vérification du certificat personnel pour un endpoint nécessitant une authentification forte, la
tentative d'accès est journalisée dans le journal des opérations.

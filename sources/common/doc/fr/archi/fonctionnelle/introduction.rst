Introduction
############

But de cette documentation
**************************

L'objectif de cette documentation est d'expliquer l'architecture fonctionnelle de ce module.


GUID
****

Cf chapitre dédié

ServerIdentity et Logger
************************

Ces 2 packages sont liés car ServerIdentity fournit des informations utiles au Logger.

Le Logger enverra un certain nombre d'information vers le log centralisé, via un filtre issu de VitamLoggerHelper.

Cette centralisation permettra notamment d'avoir des informations analysées par l'outil d'administration (par défaut, :term:`ELK`).

L'ensemble des logs seront centralisés mais tous n'iront pas dans la partie "analytique" des logs.


Introduction
############

   Vitam traite des arbres des archive units et des groupes d'objet qui peuvent être présenter par des graphes précisement par Graphe orienté acyclique(D.A.G).
   


Vérification des formats :
**************************

Cette vérification de format devra intervenir à différents endroits du processing, et pour différents types de workflow.
A l'heure actuelle, pour le processus d'Ingest, nous avons : 
 - vérification du format du SIP intégré dans l'upload (zip, tar, tar.gz...)
 - vérification des objets techniques contenus dans le SIP.

Il apparait clairement, qu'une mise en commun de cet outil doit être effectué. 
C'est pourquoi le module common-format-identification a été ajouté dans la partie commune. 
De cette manière un outil de vérification des formats pourra être utilisé dans n'importe couche Vitam, si besoin.

Pour le moment, l'outil choisi pour effectuer cette vérification de format est Siegfried.
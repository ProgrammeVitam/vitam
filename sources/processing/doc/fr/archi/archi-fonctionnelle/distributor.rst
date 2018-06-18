Distributor
***********

Le but de ce module est d'attribuer des taches pour chaque ressources disponibles. 
Le workflow se compose de plusieurs actions à faire et il sera traité par un des workers  de traitement disponibles dans la liste.

Le distributor, en plus de lancer les workflow, offre désormais la possibilité aux Workers de s'abonner, se désabonner.
Lors d'un abonnement, le Worker est ajouté à une liste de workers (regroupés par famille de worker). Pour un désabonnement, il est supprimé.
Pour le moment, les workers ajoutés ne pourront être appelés, cela sera codé dans une autre itération. Un worker par défaut sera ajouté, et utilisé dans cette itération.

Désormais, l'appel du worker se fera via un appel Rest. Le code du Worker est déplacé dans un module à part : Worker.

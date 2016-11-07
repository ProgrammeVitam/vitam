Contrôle de la cohérence de SIPs
********************************

Pour un SIP versant, il y a certaine contrôle pour valider avant de le mettre dans le VITAM. 
Une parmis des contrôles est celle de la cohérence de SIP concernant les ArchiveUnit et Object Group.
Pour ce contrôle : tous les SIP versés dans Vitam devront avoir tous leurs groupes d'objets référencés dans au moins 
une archiveUnit. La même contrainte s'applique pour les objets sans groupe d'objets. Le fait d'avoir des objets ou 
groupes d'objets sans archiveUnits est l'équivalent d'un vrac archivistique, ce que l'équipe souhaite éviter pour vitam.

- Critères d'acceptance : des critères suivantes sont appliqués pour valider cette contrôle.
CA1 : mise ne place de la nouvelle action de contrôle
Etant donné le versement d'un SIP, lorsque le SIP passe par le contrôle d'entrée globale, alors le contrôle d'entrée 
procède à une nouvelle tâche qui est la "vérification concernant la cohérence entre objet/groupe d'objet et archiveUnit". 
Cette tâche vérifie que dans le manifeste, CHAQUE objets sans groupe d'objets et CHAQUE groupe d'objets sont référencés 
par AU MOINS une archiveUnit


CA2 : SIP avec références valide
Etant donné le versement d'un SIP dont chaque objets sans groupe d'objets ET chaque groupes d'objets sont référencés 
par au moins une archiveUnit. Lorsque le SIP a terminé la tâche de vérification concernant la cohérence entre objet/groupe 
d'objet et archiveUnit alors le workflow d’entrée continue; et une ligne de status OK est ajouté dans le journal des 
opérations EVT_CHECK_MANIFEST_01_OK

CA3 : SIP avec références invalides - action non bloquante
Etant donné le versement un SIP possédant au moins un objet sans groupe d'objet et/ou au moins un groupe d'objet qui 
n'est pas référencé par au moins une archiveUnit. Lorsqu'on le contrôle passe par la tâche de vérification concernant 
la cohérence entre objet/groupe d'objet et archiveUnit alors le workflow d’entrée continue

CA4 : SIP avec références invalides - bloquage du processus à la fin du contrôle d'entrée
Lorsque le SAE a rencontré au moins un warning lors de la tâche de vérification concernant la cohérence entre objet/groupe 
d'objet et archiveUnit alors je peux constater sur l’IHM de suivi des opérations d’entrée que le statut de l’opération d’entrée 
passe à « erreur ». Le workflow d’entrée s'arrête et une ligne de status KO est ajoutée dans le journal des opérations: 
EVT_CHECK_MANIFEST_01_KO
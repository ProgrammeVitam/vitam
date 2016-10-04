Introduction
############

But de cette documentation
**************************
L'objectif de cette documentation est d'expliquer l'architecture fonctionnelle de ce module.


Ce service permet de lancer le workflow avec les paramètres d'entrée. Il traite en 
Le processus réalise par plusieurs étapes, 

Pour chaque étape 

- Il exécute une opération  (unzip d'un document, indexer d'un document, sauvegarde d'un document ...) 
- Il retourne une réponse qui contient d'un status de traitement (OK, KO, FATAL) avec une message descriptif
- Si il retourne pas le OK, une exception de traitement sera déclanché 




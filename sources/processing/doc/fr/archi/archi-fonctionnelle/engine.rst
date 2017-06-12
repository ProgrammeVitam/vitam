Introduction
############

But de cette documentation
**************************
L'objectif de cette documentation est d'expliquer l'architecture fonctionnelle de ce module.


Ce service permet d'exécuter une étape d'un processus. Il est complètement piloté par une machine à état.

Il peut faire ce qui suit:
- Exécuter une étape d'un processus
    - Initialiser le logbbok,
    - Appeler le distributeur pour exécuter l'étape  (unzip d'un document, indexer d'un document, sauvegarde d'un document ...)
    - Finaliser le logbook concernant l'étape.
    - notifier la machine à état sur le résultat de l'exécution de l'étape.




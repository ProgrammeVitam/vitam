Processing Engine
##################

Présentation
^^^^^^^^^^^^

|  *Parent package:* **fr.gouv.vitam.processing**
|  *Package proposition:* **fr.gouv.vitam.processing.engine**

Ce module présente un packge api et core. Dans api on retrouve les interface et dans core leurs implémentations.

Api
----
ProcessEngine est l'interface qu'on retrouve au niveau de la machine état. Elle expose les méthodes suivantes:
- start : pour lancer l'exécution d'une étape d'un processus auquel ce ProcessEngine est rattaché.
- pause: n'est pas encore implémenté pour le moment mais sert à propager l'action pause sur les étapes.
- cancel: n'est pas encore implémenté pour le moment mais sert à propager l'action cancel sur les étapes.


Core
-----

Dans la partie Core, la classe ProcessEngineImpl est l'implémentation de l'interface  ProcessEngine:

ProcessEngineImpl ne fait que ce qui suit:

- Initialiser le logbook pour l'étape en cours.
- Appeler le distributeur pour exécuter l'étape.
- Au retour du distributeur finaliser le logbook pour l'étape en question.
- Gérer les exceptions
- Appeler la machine à état via IEventsProcessEngine avec les méthodes: onComplete, onUpdate, onError.

    - onComplete: quand une exécution d'une étape est fini
    - onError: Quand une exception est levée lors de l'exécution d'une étape.
    - onUpdate: Quand une mise à jour à la volé d'un processus est nécessaire.

Il faut noter que l'exécution au niveau ProcessEngine est complètement asynchrone en utilisant les CompletableFuture.
Dès que l'initialisation du logbook et de l'initialisation de la CompletableFuture sont faite, une réponse est retournée tout de suite au ProcessManagement et ainsi de suite au client final avant même que l'exécution de l'étape en cours est terminée.

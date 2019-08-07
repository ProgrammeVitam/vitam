ingest-rest
############

Présentation
------------

* Proposition de package : **fr.gouv.vitam.ingest.upload.rest**

Module utilisant le service REST avec Jersey pour charger SIP et faire l'appel des autres modules (workspace, processing et logbook, etc ...).

La logique technique actuelle est la suivante :

	1. lancement du serveur d'application en appelant le fichier ingest-rest.properties ( voir le document d'exploitation).
	
	2. Créer une méthode upload du fichier sip
	
		1. Appel du journal pour la création des opérations (suivi du SIP).
		2. Push SIP dans le workspace.
		3. Appel du processing (journalisation des opération).
		4. Fermeture de la page des opérations.


``IngestInternalApplication.java``
-----------------------------------

classe de démarrage du serveur d'application de l'ingest interne.

.. sourcecode:: java

    // démarrage
    public static void main(String[] args) {
        try {
            final VitamServer vitamServer = startApplication(args);
            vitamServer.run();
        } catch (final VitamApplicationServerException exc) {
            LOGGER.error(exc);
            throw new IllegalStateException("Cannot start the Ingest Internal  Application Server", exc);
        }
    }


Dans le ``startApplication``, on effectue le start de ``VitamServer``. Le `join` est effectué dans ``run``. Le startApplication permet d'être lancé par les tests unitaires. Il peut être configuré avec un port d'écoute par les tests.

Dans le fichier de configuration, le paramètre ``jettyConfig`` est à paramétrer avec le nom du fichier de configuration de jetty.

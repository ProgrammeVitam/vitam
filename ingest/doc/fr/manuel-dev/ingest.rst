




L'application rest
******************

ingest-internal : IngestInternalApplication
-------------------------------------------
La méthode startApplication avec l'argument String[] permet aux tests unitaires de démarrer
sur un port spécifique, le deuxième argument. Le premier argument contient le nom du fichier
de configuration ingest-internal.conf (il est templetiser avec ansible).

ingest-external : IngestExternalApplication
-------------------------------------------
même chose que pour IngestInternalApplication et avec ingest-external.conf à la place de
ingest-internal.conf.

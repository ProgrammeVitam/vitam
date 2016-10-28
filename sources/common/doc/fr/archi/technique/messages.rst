Messages
########

La classe **fr.gouv.vitam.common.i18n.Messages** permets de récupérer des messages internationalisé par l'utilisation d'un *ResourceBundle*.

Elle utiliser des fichiers de resources properties dans le format suivant : *messages_fr.properties* où :

- *messages* est le nom du bundle
- *fr* est la locale

Aujourd'hui, seule la locale "fr" est gérée et les fichiers doivent être créés dans le dossier src/main/resources du module common-public.

Cette classe peut être utilisée en définissant un service qui utilise la classe *Messages* avec un fichier custom.

Messages Logbook
################

Ce service permet de centraliser les messages des logbooks.

- **Nom du bundle** : vitam-logbook-messages
- **Service** : fr.gouv.vitam.common.i18n.VitamLogbookMessages.java

Ce service offre des méthodes permettant de récupérer des messages de logbook opération et cycle de vie.
Il offre également la possibilité de récupérer toutes les clés et messages du fichier. Cette méthode ne doit être que ponctuellement pour des raisons de performance (elle est destinée à l'ihm-demo).

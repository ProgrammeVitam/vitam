Introduction
*******************

Présentation
------------

|  *Parent package:* **fr.gouv.vitam**
|  *Package proposition:* **fr.gouv.vitam.logbook**

Itération 3
-----------
4 sous-modules pour le Logbook Engine. Dans logbook (parent).

| - vitam-logbook-common :  Classes et exception communes aux différents modules
| - vitam-logbook-common-client : Classes communes pour les clients
| - vitam-logbook-operations : module lié aux opérations
| - vitam-logbook-operations-client : module client pour les opérations

Itérations suivantes / à plus long terme
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
| - vitam-logbook-lifecycles : module des cycles de vie logs
| - vitam-logbook-lifecycles-client : module client pour les cycles de vie
| - vitam-logbook-administration : module pour l'administration du moteur de journalisation (sera détaillé plus en détail)
| - vitam-logbook-administration-client : module client pour l'administration du moteur de journalisation (sera détaillé plus en détail)

Modules - packages
------------------

|  logbook
|     /logbook-common
|        fr.gouv.vitam.logbook.common.exception
|        fr.gouv.vitam.logbook.common.utils
|        fr.gouv.vitam.logbook.common.dto
|     /logbook-common-client
|        fr.gouv.vitam.logbook.common.client.exception
|        fr.gouv.vitam.logbook.common.client.utils
|        fr.gouv.vitam.logbook.common.client.dto
|     /logbook-operations
|        fr.gouv.vitam.logbook.operations.api
|        fr.gouv.vitam.logbook.operations.core
|        fr.gouv.vitam.logbook.operations.service
|     /logbook-operations-client
|     /logbook-lifecycles
|        fr.gouv.vitam.logbook.lifecycle.api
|        fr.gouv.vitam.logbook.lifecycle.core
|        fr.gouv.vitam.logbook.lifecycle.service
|     /logbook-lifecycles-operation
|     /logbook-administration
|     /logbook-administration-client

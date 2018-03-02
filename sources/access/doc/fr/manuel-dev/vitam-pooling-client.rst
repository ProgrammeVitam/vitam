vitam-pooling-client
######################

Utilisation
###########

VitamPoolingClient offre la possibilité d'attendre la fin des processus asynchrone.

Paramètres
**********

VitamPoolingClient accepte un seul paramètre dans son constructeur: C'est l'interface OperationStatusClient.
Cette interface définie la méthode suivante:

.. code-block:: java

    RequestResponse<ItemStatus> getOperationProcessStatus(VitamContext vitamContext, String id) throws VitamClientException;


Le client
*********

VitamPoolingClient implémente différentes méthodes "wait" avec différents paramètres qui offre la fonctionnalité pooling sur les différents processus asynchrone. Utiliser les méthodes "wait" pour mieux gérer le pooling côté serveur et remédier à l'asynchrone des certains opérations.

Les différentes méthodes "wait" du client VitamPoolingClient sont:

.. code-block:: java

    // Possibilité de faire plusieurs (nbTry) appel espacé d'un temps (timeWait) avant de répondre au client final
    public boolean wait(int tenantId, String processId, ProcessState state, int nbTry, long timeWait, TimeUnit timeUnit) throws VitamException
    public boolean wait(int tenantId, String processId, int nbTry, long timeout, TimeUnit timeUnit) throws VitamException
    public boolean wait(int tenantId, String processId, ProcessState state) throws VitamException
    public boolean wait(int tenantId, String processId) throws VitamException

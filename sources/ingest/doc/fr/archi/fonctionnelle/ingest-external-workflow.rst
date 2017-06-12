Généralités
***********
En plus de réaliser des upload de SIP, l'ingest-external expose aussi des méthodes pour gérer un process de traitement avec un workflow.
Pour rappel, ingest-external fait appel à ingest-internal pour continuer l'exécution des méthodes demandées.

Fonctionnalités concernant le workflow:
**************************************

L'ingest external expose les méthodes suivantes pour gérer ces process:

- initVitamProcess : Initialiser un process avec un workflow.
- initWorkFlow: Initialiser un process avec un workflow. Cette méthode est dépréciée en faveur de initVitamProcess.
- updateOperationActionProcess: exécuter une action sur un process (next, resume, pause, cancel)
- updateVitamProcess: Cette méthode est dépréciée en faveur de updateOperationActionProcess
- executeOperationProcess: Elle expose les même fonctionnalités que updateOperationActionProcess en utilisant la méthode http POST.
- cancelOperationProcessExecution: Annuler l'exécution d'un process.
- getOperationProcessStatus: Retourne le statut d'un process
- getOperationProcessExecutionDetails: Retourne le détail d'un process
- listOperationsDetails: Lister tous les process qui sont en état RUNNING ou PAUSE.
- wait(int tenantId, String processId, ProcessState state, int nbTry, long timeWait, TimeUnit timeUnit): Permet de bien gérer le pooling côté serveur. En effet, cette méthode fait appel à getOperationProcessStatus nbTry fois et esapce les appels avec un temps de timeWait. La réponse au client est retournée dans les cas suivants:
    > nbTry est atteint (nombre de rappel)
    > le state du process est COMPLETED
    > Le state du process est PAUSE et le statut est supérieur à STARTED

Les actions:
************
Les actions possible pour un workflow  sont: INIT, NEXT, RESUME, PAUSE, CANCEL
Dans le cas des méthodes initVitamProcess et cancelOperationProcessExecution les actions sont par défaut INIT et CANCEL respectivement.

Pour les autres méthodes: Les actions doivent êtres (NEXT, RESUME ou PAUSE)

- INIT: Initialiser un process avec un workflow et mettre son état à PAUSE (en attente d'une action)
- NEXT: Exécuter la première étape d'un process et mettre en état PAUSE. Si c'est la dernière étape alors mettre en état COMPLETED.
- RESUME: Exécuter tous les états d'un process et mettre en état COMPLETED
- PAUSE: Mettre le process en état PAUSE dès que possible. Si c'est la dernière étape en cours d'exécution alors mettre en état COMPLETED
- CANCEL: Mettre le process en état COMPLETED dès que possible.

Asynchrone:
***********
L'exécution d'un process est complètement asynchrone. Donc pour avoir l'état final d'un process et son statut final il faut faire du pooling. La méthode wait est là pour vous aider.
Attention: Au retour de la réponse d'une action sur le process ne vaut pas dire que l'exécution est terminé, il faut donc attendre la fin de l'exécution en appelant la méthode getOperationProcessStatus ou wait. Il faut faire attention aussi pour les tests d'intergrations et les tests de non régression.



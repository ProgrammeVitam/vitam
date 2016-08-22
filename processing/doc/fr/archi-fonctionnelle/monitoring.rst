Process Monitoring
##################

Explication
***********
L'objectif de cette documentation est d'expliquer l'architecture fonctionnelle de ce module.

Le but de ce module est de pouvoir monitorer les différentes étapes des différents Worklow. 

Une interface a été déterminée et permet les opérations suivantes : 

 - initOrderedWorkflow : permet l'initialisation d'un Workflow. Le workflow est rattaché à un process, et est composé de steps. La méthode retourne une liste ordonnée de steps avec un id unique. 
 - updateStepStatus : permet de mettre à jour le statut d'un step. (STARTED, OK, KO, WARNING, FATAL, PAUSED)
 - updateStep : permet de mettre à jour les champs elementToProcess et elementProcessed.
 - getWorkflowStatus : permet de récupérer les information de workflow par rapport à un process donné.
 
L'implémentation choisie permet d'enregistrer toutes les informations de workflow dans une HashMap, tout ceci via un singleton.
La liste des workflow étant enregistrée dans une ConcurrentHashMap, permettant de gérer les nombreux appels concurrents.
Lors de l'initOrderedWorkflow, l'id unique pour chaque step est généré de cette manière : 
 - {CONTAINER_NAME}_{WORKFLOW_ID}_{QUANTIEME_DU_STEP}_{STEP_NAME}
Paramètres
##########

Mise en place d'une classe de paramètres s'appuyant sur une map.

WorkerParamerterName, les noms de paramètre
*******************************************
Les noms de paramètres se trouvent dans l'énum WorkerParameterName. Pour ajouter un nouveau paramètre, ajouter son
nom dans l'énum.

ParameterHelper, le helper
**************************
Utiliser le ParameterHelper afin de valider les éléments requis.

WorkerParametersFactory, la factory
***********************************
Utiliser WorkerParametersFactory afin d'instancier une nouvelle classe de worker.
Actuellement 5 paramètres sont obligatoires pour tous les workers :
* urlMetadata afin d'intialiser le client metadata
* urlWorkspace afin d'initialiser le client workspace
* objectName le fichier json de l'object lorsque l'on boucle sur liste
* currentStep le nom de l'étape
* containerName l'identifiant du container

AbstractWorkerParameters, les implémentations par défaut
********************************************************
La classe abstraite AbstractWorkerParameters est l'implémentation par défaut de l'interface WorkerParameters.
Si un paramètre est ajouté, il est possible de vouloir un getter et un setter particulier (aussi bien dans 
l'interface que dans l'implémentation abstraite).

DefaultWorkerParameters, l'implémentation actuelle
**************************************************
C'est l'implémentation actuelle des paramètres de worker.

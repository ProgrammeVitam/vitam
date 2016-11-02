Ingest ExternalAntivirus
************************

L'antivirus est intégré dans le processus de upload d'un SIP pour déterter un fichier infecté. 
 L'antivirus rejettera les fichiers vérolés (qu'il pourrait corriger ou pas) afin d'éviter des 
 problèmes d'authenticité au moment du contrôle.

Le critère d'acceptance 
- Étant donné : un SIP contenant un ou plusieurs fichiers infectés. Lorsque le SAE réalise : l’étape de check sanitaire

+ Si des fichiers vérolés sont détectés et que l’antivirus peut les corriger, le workflow s’arrête à cette étape

eventType : « Contrôle sanitaire SIP »
outcome avec statut « KO »,
outcomeDetailMessage : « Échec du contrôle sanitaire du SIP : présence de fichiers infectés » 
(fichier éventuellement corrigeable par l’antivirus)."
objectIdentifierIncome : <nomDuSIP.extension>

+ Si des fichiers vérolés sont détectés sans aucune correction de l’antivirus, alors le worflow s’arrête à cette étape.

eventType : « Contrôle sanitaire SIP »
outcome avec statut « KO »,
outcomeDetailMessage : « Échec du contrôle sanitaire du SIP : présence de fichiers infectés ».
objectIdentifierIncome : <nomDuSIP.extension>

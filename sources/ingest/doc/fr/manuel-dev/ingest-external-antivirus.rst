ingest-external-antivirus
#########################

Dans cette section, nous expliquons comment utiliser et configurer le script d'antivirus 
pour le service ingest-external.

1. Configuration pour ingest-external : ingest-external.conf

	Dans ce fichier de configuration, nous précisons le nom du script antivirus utilisé, et
	le timeout pour le scan. Le script utilisé actuellement est scan-clamav.sh 
	utilisant l’antivirus ClamAV.

        Le paramètre 'timeoutScanDelay' est utilisé comme timeout du traitement de l'antivirus. Il faut 
        choisir une valeur (en millisecondes) en fonction de la performance de l'antivirus et de la somme 
        des tailles de binaires que l'on doit pouvoir traiter simultanément sur le composant ingest-external.
        La valeur par défaut est de 60 secondes. 

.. code-block:: yaml

	antiVirusScriptName : scan-clamav.sh
	timeoutScanDelay : 60000

2. Script d'antivirus scan-clamav.sh

	Le script permettant de lancer d'un scan d’un fichier envoyé avec l’antivirus ClamAV et 
	retourner le résulat :

   -1: Analyse non effectuée
	0: Analyse OK - no virus                                                
	1: Virus trouvé et corrigé
	2: Virus trouvé mais non corrigé
	3: Analyse NOK

	Ce fichier est mis dans le répertoire vitam/conf avec le droit d'exécution.	

3. Lancer le script en Java et intégration

JavaExecuteScript.java (se trouve dans ingest-external-common) permettant de lancer le script de clamav 
en Java en prenant des paramètres d'entrées : le script utilisé, le chemin du fichier à scanner et 
le temps limité d'un scan
Pour l'intégration dans ingest-external, ce script est appelé dans l'implémentation des APIs de ingest-externe.
la section suivant montre comment on appelle le script depuis ingest-external en Code.

.. code-block:: java   
     
    antiVirusResult = JavaExecuteScript.executeCommand(antiVirusScriptName, filePath, timeoutScanDelay);
    .......
    switch (antiVirusResult) {
          case STATUS_ANTIVIRUS_OK:
              LOGGER.info(IngestExternalOutcomeMessage.OK_VIRUS.toString());
              // nothing to do, default already set to ok
              break;
          case STATUS_ANTIVIRUS_WARNING:
          case STATUS_ANTIVIRUS_KO:
              LOGGER.error(IngestExternalOutcomeMessage.KO_VIRUS.toString());
              antivirusParameters.setStatus(StatusCode.KO);
              antivirusParameters.putParameterValue(LogbookParameterName.outcomeDetail,
                  messageLogbookEngineHelper.getOutcomeDetail(SANITY_CHECK_SIP, StatusCode.KO));
              antivirusParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                  messageLogbookEngineHelper.getLabelOp(SANITY_CHECK_SIP, StatusCode.KO));
              isFileInfected = true;
              break;
          case STATUS_ANTIVIRUS_NOT_PERFORMED:
          case STATUS_ANTIVIRUS_NOT_PERFORMED_2:                    
              LOGGER.error(IngestExternalOutcomeMessage.FATAL_VIRUS.toString());
              antivirusParameters.setStatus(StatusCode.FATAL);
              antivirusParameters.putParameterValue(LogbookParameterName.outcomeDetail,
                  messageLogbookEngineHelper.getOutcomeDetail(SANITY_CHECK_SIP, StatusCode.FATAL));
              antivirusParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                  messageLogbookEngineHelper.getLabelOp(SANITY_CHECK_SIP, StatusCode.FATAL));
              isFileInfected = true;
              break;
      }
    }
	.....................................................................................................        


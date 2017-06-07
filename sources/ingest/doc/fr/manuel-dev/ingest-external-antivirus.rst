ingest-external-antivirus
##########################

Dans cette section, nous expliquons comment utiliser et configurer le script d'antivirus 
pour le service ingest-external.

1. Configuration pour ingest-external : ingest-external.conf
	Dans ce fichier de configuration, nous précisons le nom de la script antivirus utilisé et
	le temps limité pour le scan. pour le moment, nous utilisons le script de scan scan-clamav.sh 
	utilisant l’antivirus ClamAV. 
		antiVirusScriptName : scan-clamav.sh
		timeoutScanDelay : 60000
2. Script d'antivirus scan-clamav.sh
	Le script permettant de lancer d'un scan d’un fichier envoyé avec l’antivirus ClamAV et 
	retourner le résulat :  
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
        case 0:
            LOGGER.info(IngestExternalOutcomeMessage.OK_VIRUS.toString());
            endParameters.setStatus(LogbookOutcome.OK);
            endParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                IngestExternalOutcomeMessage.OK_VIRUS.value());
            break;
        case 1:
            LOGGER.debug(IngestExternalOutcomeMessage.OK_VIRUS.toString());
            endParameters.setStatus(LogbookOutcome.OK);
            endParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                IngestExternalOutcomeMessage.KO_VIRUS.value());
            break;
        case 2:
            LOGGER.error(IngestExternalOutcomeMessage.KO_VIRUS.toString());
            endParameters.setStatus(LogbookOutcome.KO);
            endParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                IngestExternalOutcomeMessage.KO_VIRUS.value());
            isFileInfected = true;
            break;
        default:
            LOGGER.error(IngestExternalOutcomeMessage.KO_VIRUS.toString());
            endParameters.setStatus(LogbookOutcome.FATAL);
            endParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                IngestExternalOutcomeMessage.KO_VIRUS.value());
            isFileInfected = true;
    }
	.....................................................................................................        


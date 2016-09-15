package fr.gouv.vitam.ingest.external.common.util;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.ingest.external.api.IngestExternalException;

/**
 * Class JavaExecuteScript used to execute the shell script in java
 */
public class JavaExecuteScript {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(JavaExecuteScript.class);
//    private static final long timeoutScanDelay = 60000;

    /**
     * Return status when execute the shell script scan-clamav.sh for scanning the file
     * 
     * @param cmd the command line that will be executed
     * @param arg the file to scan
     * @return The return value of the cmd or 3 if the execution failed 
     * @throws IngestExternException 
     * @throws FileNotFoundException 
     */
    public static int executeCommand(String cmd,String arg, long timeoutScanDelay) throws IngestExternalException{
        int exitStatus = 3;
        String scriptPath;
        try {
            scriptPath = PropertiesUtils.findFile(cmd).getPath();
        } catch (FileNotFoundException e) {
            LOGGER.error(cmd+" does not exit");
            throw new IngestExternalException(e);
        }
        exitStatus = execute(scriptPath, arg, timeoutScanDelay);
        return exitStatus;
    }
    
    private static int execute(String scriptPath, String arg, long timeoutScanDelay) throws IngestExternalException {
        CommandLine cmd = new CommandLine(scriptPath).addArgument(arg);
        DefaultExecutor defaultExecutor = new DefaultExecutor();
        //TODO : Le résultat de la commande (sortie du scanner) doit être prise en compte pour informer le client du problème.
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PumpStreamHandler pumpStreamHandler = new PumpStreamHandler(out);
        ExecuteWatchdog watchdog = new ExecuteWatchdog(timeoutScanDelay);
        defaultExecutor.setStreamHandler(pumpStreamHandler);
        defaultExecutor.setWatchdog(watchdog);
        
        int exitValue = -1;
        try {
            exitValue = defaultExecutor.execute(cmd);
        } catch (ExecuteException e) {
            if (e.getExitValue() == 1 || e.getExitValue() == 2 || e.getExitValue() == 3) {
                return e.getExitValue();
            }
        } catch (IOException e) {
            throw new IngestExternalException(e);
        }
        return exitValue;
    } 
}

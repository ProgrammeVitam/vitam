package fr.gouv.vitam.ingest.external.common.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.ingest.external.api.IngestExternalException;

/**
 * Class JavaExecuteScript used to execute the shell script in java
 */
public class JavaExecuteScript {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(JavaExecuteScript.class);

    /**
     * Return status when execute the shell script scan-clamav.sh for scanning the file
     * 
     * @param cmd the command line that will be executed
     * @param arg the file to scan
     * @return The return value of the cmd or 3 if the execution failed 
     * @throws IngestExternException 
     * @throws FileNotFoundException 
     */
    public static int executeCommand(String cmd,String arg) throws IngestExternalException{
        int exitStatus = 3;
        String scriptPath;
        try {
            scriptPath = PropertiesUtils.findFile(cmd).getPath();
        } catch (FileNotFoundException e) {
            LOGGER.error(cmd+" does not exit");
            throw new IngestExternalException(e);
        }
        try {
            chmodScript(scriptPath);
            Process p = executeScript(scriptPath, arg);
            p.waitFor();
            exitStatus = p.exitValue();
        } catch (IOException | InterruptedException e) {
            LOGGER.error(cmd+" can not execute");
            throw new IngestExternalException(e);
        }
        return exitStatus;
    }

    private static void chmodScript(String filePath) throws IOException, InterruptedException {
        // FIXME : faille de sécurité maximale que de rendre exécutable qqc qui ne l'était peut être pas
        File file = PropertiesUtils.findFile(filePath);
        file.setExecutable(true, true);
    }

    private static Process executeScript(String scriptPath, String arg) throws IOException {
        String[] cmd = {scriptPath, arg};
        // FIXME très mauvaise pratique car les InputStream et OuputStream ne sont pas gérés ni un timeout
        return Runtime.getRuntime().exec(cmd);
    }
}

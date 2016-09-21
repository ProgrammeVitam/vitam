package fr.gouv.vitam.ingest.external.common.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.ingest.external.common.util.JavaExecuteScript;

public class JavaExecuteScriptTest {

    private static final String FIXED_VIRUS_FILE = "fixed-virus.txt";
    private static final String UNFIXED_VIRUS_FILE = "unfixed-virus.txt";
    private static final String NO_VIRUS_FILE = "no-virus.txt";
    private static final String SCRIPT_SCAN_CLAMAV = "scan-clamav.sh";
    
    private static final long timeoutScanDelay = 60000;
    
    @Test
    public void givenExecuteScanClamAVWhenVirusFoundButNotCorrectedThenReturn2() 
        throws Exception {
        assertEquals(1, JavaExecuteScript.executeCommand(SCRIPT_SCAN_CLAMAV,PropertiesUtils.getResourcesFile(FIXED_VIRUS_FILE).getPath(), timeoutScanDelay));
        assertEquals(2, JavaExecuteScript.executeCommand(SCRIPT_SCAN_CLAMAV,PropertiesUtils.getResourcesFile(UNFIXED_VIRUS_FILE).getPath(), timeoutScanDelay));
        assertEquals(0, JavaExecuteScript.executeCommand(SCRIPT_SCAN_CLAMAV,PropertiesUtils.getResourcesFile(NO_VIRUS_FILE).getPath(), timeoutScanDelay));
    }

}

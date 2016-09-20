package fr.gouv.vitam.ingest.external.rest;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;

import org.junit.Test;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.server.BasicVitamServer;

public class IngestExternalSslApplcationTest {

    private static final String SHOULD_NOT_RAIZED_AN_EXCEPTION = "Should not raized an exception";

    private static final String INGEST_EXTERNAL_CONF = "ingest-external-ssl1-test.conf";
    private static File config;

    @Test
    public final void testFictiveLaunch() throws FileNotFoundException {
        config = PropertiesUtils.findFile(INGEST_EXTERNAL_CONF);
        try {
            ((BasicVitamServer) IngestExternalApplication.startApplication(config.getAbsolutePath())).stop();
        } catch (final VitamException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        }
    }
    
    @Test
    public final void givenCorrrectConfigFileWhenStartApplicationThenStartOnDefaultPort() throws VitamException {
        ((BasicVitamServer) IngestExternalApplication.startApplication(INGEST_EXTERNAL_CONF)).stop();
    }

}

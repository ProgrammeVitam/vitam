package fr.gouv.vitam.ingest.external.rest;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.server.BasicVitamServer;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;

import static org.junit.Assert.fail;

public class IngestExternalApplcationTest {

    private static final String SHOULD_NOT_RAIZED_AN_EXCEPTION = "Should not raized an exception";

    private static final String INGEST_EXTERNEL_CONF = "ingest-external-test.conf";
    private static int serverPort;
    private static File config;

    @Test
    public final void testFictiveLaunch() throws FileNotFoundException {
        config = PropertiesUtils.findFile(INGEST_EXTERNEL_CONF);
        try {
            ((BasicVitamServer) IngestExternalApplication.startApplication(config.getAbsolutePath())).stop();
        } catch (final VitamException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        }
    }

    @Test(expected = NullPointerException.class)
    public final void givenNullConfigWhenStartApplicaitionThenRaiseException() throws VitamException {
            ((BasicVitamServer) IngestExternalApplication.startApplication(null)).stop();
    }

    @Test(expected = VitamApplicationServerException.class)
    public final void givenBlankConfigWhenStartApplicaitionThenRaiseException() throws VitamException {
        ((BasicVitamServer) IngestExternalApplication.startApplication("")).stop();
    }

    @Test(expected = VitamException.class)
    public final void givenInCorrrectConfigFileWhenStartApplicationThenStartOnDefaultPort() throws VitamException {
        ((BasicVitamServer) IngestExternalApplication.startApplication("ingest-external-err1.conf")).stop();
    }

    @Test
    public final void givenCorrrectConfigFileWhenStartApplicationThenStartOnDefaultPort() throws VitamException {
        ((BasicVitamServer) IngestExternalApplication.startApplication("ingest-external-test.conf")).stop();
    }

    @Test
    public void givenConfigFileNameWhenGetThenReturnConfigFileName() {
        IngestExternalApplication ingest = new IngestExternalApplication();
        Assert.assertEquals("ingest-external.conf", ingest.getConfigFilename());
    }
}

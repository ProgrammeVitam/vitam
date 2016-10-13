/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 *******************************************************************************/
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

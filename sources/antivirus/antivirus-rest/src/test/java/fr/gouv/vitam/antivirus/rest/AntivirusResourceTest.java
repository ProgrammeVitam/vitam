/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "https://cecill.info".
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
 */
package fr.gouv.vitam.antivirus.rest;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import io.restassured.RestAssured;
import jakarta.ws.rs.core.Response.Status;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

import static io.restassured.RestAssured.given;

public class AntivirusResourceTest {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AntivirusResourceTest.class);

    private static final String RESOURCE_URI = "/antivirus/v1";
    private static final String SCAN_BY_PATH_URL = "/scanByPath";
    private static final String ANTIVIRUS_TEST_CONF = "antivirus-test.conf";
    private static JunitHelper junitHelper;
    private static int serverPort;

    private static AntivirusMain application;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        junitHelper = JunitHelper.getInstance();
        serverPort = junitHelper.findAvailablePort();
        File configurationFile = PropertiesUtils.getResourceFile(ANTIVIRUS_TEST_CONF);
        final AntivirusConfiguration configuration = PropertiesUtils.readYaml(
            configurationFile,
            AntivirusConfiguration.class
        );
        configuration.setPath(configurationFile.getParentFile().getCanonicalPath());
        PropertiesUtils.writeYaml(configurationFile, configuration);

        RestAssured.port = serverPort;
        RestAssured.basePath = RESOURCE_URI;
        try {
            application = new AntivirusMain(ANTIVIRUS_TEST_CONF, BusinessApplication.class, null);
            application.start();
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
            throw new IllegalStateException("Cannot start the Antivirus Application Server", e);
        }
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        LOGGER.debug("Ending tests");
        try {
            if (application != null) {
                application.stop();
            }
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.syserr("", e);
        }
        junitHelper.releasePort(serverPort);
        VitamClientFactory.resetConnections();
        fr.gouv.vitam.common.external.client.VitamClientFactory.resetConnections();
    }

    @Test
    public final void testScanOk() {
        given().when().get(SCAN_BY_PATH_URL + "?path=no-virus.txt").then().statusCode(Status.OK.getStatusCode());
    }

    @Test
    public final void testScanVirusFixed() {
        given()
            .when()
            .get(SCAN_BY_PATH_URL + "?path=warning.txt")
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public final void testScanVirus() {
        given().when().get(SCAN_BY_PATH_URL + "?path=virus.txt").then().statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public final void testScanErrorOrException() {
        given()
            .when()
            .get(SCAN_BY_PATH_URL + "?path=error.txt")
            .then()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public final void testScanBadParameters() {
        given().when().get(SCAN_BY_PATH_URL + "?path=").then().statusCode(Status.NOT_FOUND.getStatusCode());
        given().when().get(SCAN_BY_PATH_URL).then().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public final void testScanNotFound() {
        given().when().get(SCAN_BY_PATH_URL + "?path=notfound.txt").then().statusCode(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public final void testScanNotReadable() {
        given().when().get(SCAN_BY_PATH_URL + "?path=notreadable.txt").then().statusCode(Status.OK.getStatusCode());
    }
}

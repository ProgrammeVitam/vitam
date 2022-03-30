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
package fr.gouv.vitam.access.external.rest;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.SysErrLogger;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response.Status;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThatCode;

public class AccessExternalApplicationTest {
    private AccessExternalMain application;
    private final JunitHelper junitHelper = JunitHelper.getInstance();
    private int portAvailable;

    @Before
    public void setUpBeforeMethod() throws Exception {
        portAvailable = junitHelper.findAvailablePort();
        RestAssured.port = portAvailable;
        RestAssured.basePath = "access-external/v1";
    }

    @After
    public void tearDown() throws Exception {
        try {
            if (application != null && application.getVitamServer() != null &&
                application.getVitamServer() != null) {

                application.stop();
            }
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.syserr("", e);
        }

        junitHelper.releasePort(portAvailable);
        VitamClientFactory.resetConnections();
        fr.gouv.vitam.common.external.client.VitamClientFactory.resetConnections();
    }

    @Test(expected = Exception.class)
    public void shouldRaiseAnExceptionWhenConfigureApplicationWithEmptyArgs() throws Exception {
        application = new AccessExternalMain("");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldRaiseAnExceptionWhenConfigureApplicationWithFileNotFound() throws Exception {
        application = new AccessExternalMain("notFound.conf");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionWhenConfigureApplicationWithFileErr1() throws Exception {
        application = new AccessExternalMain("access-external-test-err1.conf");
        Assert.assertFalse(application.getVitamServer().isStarted());
    }

    @Test
    public void shouldStartAndStopServerWhenStopApplicationWithFileExistsAndRun() throws Exception {
        application = new AccessExternalMain("access-external-test.conf");
        application.start();
        Assert.assertTrue(application.getVitamServer().isStarted());

        application.stop();
    }

    @Test
    public void shouldHeaderStripXSSWhenFilterThenReturnReturnNotAcceptable() throws VitamException {
        application =
            new AccessExternalMain("src/test/resources/access-external-test.conf", BusinessApplicationTest.class, null);
        application.start();

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, "0")
            .header("test", "<script>(.*?)</script>")
            .body("{\"name\":\"123\"}")
            .when()
            .put("/units/1")
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .param("test", "<?php echo\" Hello \" ?>")
            .header(GlobalDataRest.X_TENANT_ID, "0")
            .body("{\"name\":\"123\"}")
            .when()
            .put("/units/1")
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        // without X-Tenant-Id --> Precondition Failed
        given()
            .contentType(ContentType.JSON)
            .param("test", "<?php echo\" Hello \" ?>")
            .body("{\"name\":\"123\"}")
            .when()
            .put("/units/1")
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        // Incorrect Tenant Id --> UNAUTHORIZED
        given()
            .contentType(ContentType.JSON)
            .param("test", "<?php echo\" Hello \" ?>")
            .header(GlobalDataRest.X_TENANT_ID, "7")
            .body("{\"name\":\"123\"}")
            .when()
            .put("/units/1")
            .then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

    }

    @Test
    public void shouldActivateShiroFilter() throws VitamException {
        application = new AccessExternalMain("src/test/resources/access-external-test-ssl.conf");
        assertThatCode(() -> application.start()).doesNotThrowAnyException();

    }
}

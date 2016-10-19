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
package fr.gouv.vitam.access.rest;

import static com.jayway.restassured.RestAssured.given;

import javax.ws.rs.core.Response.Status;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;

import fr.gouv.vitam.access.config.AccessConfiguration;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.server.VitamServer;

/**
 * AccessApplication Test class
 */
public class AccessApplicationTest {

    private AccessApplication application;
    private final JunitHelper junitHelper = JunitHelper.getInstance();
    private int portAvailable;

    @Before
    public void setUpBeforeMethod() throws Exception {
        portAvailable = junitHelper.findAvailablePort();
        // TODO verifier la compatibilité avec les tests parallèles sur jenkins
        SystemPropertyUtil.set(VitamServer.PARAMETER_JETTY_SERVER_PORT, Integer.toString(portAvailable));
    }

    @After
    public void tearDown() throws Exception {
        if (application != null) {
            application.stop();
        }
        junitHelper.releasePort(portAvailable);
    }

    @Test(expected = Exception.class)
    public void shouldRaiseAnExceptionWhenConfigureApplicationWithEmptyArgs() throws Exception {
        application = new AccessApplication((String) null);
    }

    @Test(expected = Exception.class)
    public void shouldRaiseAnExceptionWhenConfigureApplicationWithEmptyConfig() throws Exception {
        application = new AccessApplication((AccessConfiguration) null);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldRaiseAnExceptionWhenConfigureApplicationWithFileNotFound() throws Exception {
        application = new AccessApplication("notFound.conf");
    }

    @Test
    public void shouldRunServerWhenConfigureApplicationWithFileExists() throws Exception {
        application = new AccessApplication("access-test.conf");
        Assert.assertFalse(application.getVitamServer().isStarted());
        application.start();
        Assert.assertTrue(application.getVitamServer().isStarted());
        application.stop();
        Assert.assertFalse(application.getVitamServer().isStarted());
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionWhenConfigureApplicationWithFileErr1() throws Exception {
        application = new AccessApplication("access-test-err1.conf");
    }

    /*
     * @Ignore
     * @Test public void shouldExecuteStatusServiceRest() throws URISyntaxException { webTarget = client.target(new
     * URI(getBaseUri() + "accessMock/status")); final Invocation.Builder builder = webTarget.request(); final Response
     * response = builder.get(); final String status = response.readEntity(String.class); assertNotNull(response);
     * assertEquals(200, response.getStatus()); }
     * @Ignore
     * @Test public void shouldExecuteGetUnitsServiceRest() throws URISyntaxException { webTarget = client.target(new
     * URI(getBaseUri() + "accessMock/units")); final Invocation.Builder builder = webTarget.request(); final
     * UnitRequestDTO statusRequestDTO = new UnitRequestDTO("queryDsl"); final Entity<UnitRequestDTO> entity =
     * Entity.json(statusRequestDTO); final Response response = builder.post(entity); final String status =
     * response.readEntity(String.class); assertNotNull(response); assertEquals(200, response.getStatus()); }
     * @Ignore
     * @Test public void shouldExecuteGetUnitByIdServiceRest() throws URISyntaxException { webTarget = client.target(new
     * URI(getBaseUri() + "accessMock/units/xyz")); Invocation.Builder builder = webTarget.request(); UnitRequestDTO
     * statusRequestDTO = new UnitRequestDTO("queryDsl"); Entity<UnitRequestDTO> entity = Entity.json(statusRequestDTO);
     * Response response = builder.post(entity); String status = response.readEntity(String.class);
     * assertNotNull(response); assertEquals(200, response.getStatus()); }
     */

    @Test
    public void shouldHeaderStripXSSWhenFilterThenReturnReturnNotAcceptable() throws VitamException {
        application = new AccessApplication("access-test.conf");
        try {
            application.start();
            RestAssured.port = portAvailable;
            RestAssured.basePath = "access/v1";

            given()
                .contentType(ContentType.JSON)
                .header("test", "<script>(.*?)</script>")
                .body("{\"name\":\"123\"}")
                .when()
                .put("/units/1")
                .then()
                .statusCode(Status.NOT_ACCEPTABLE.getStatusCode());
        } finally {
            application.stop();
        }
    }

    @Test
    public void shouldParamStripXSSWhenFilterThenReturnReturnNotAcceptable() throws VitamException {
        application = new AccessApplication("access-test.conf");
        try {
            application.start();

            RestAssured.port = portAvailable;
            RestAssured.basePath = "access/v1";

            given()
                .contentType(ContentType.JSON)
                .param("test", "<?php echo\" Hello \" ?>")
                .body("{\"name\":\"123\"}")
                .when()
                .put("/units/1")
                .then()
                .statusCode(Status.NOT_ACCEPTABLE.getStatusCode());
        } finally {
            application.stop();
        }
    }
}

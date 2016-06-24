/*******************************************************************************
 * This file is part of Vitam Project.
 * <p>
 * Copyright Vitam (2012, 2015)
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL license as circulated
 * by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL license and that you
 * accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.access.rest;

import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.RestAssured.given;

import java.io.FileNotFoundException;

import javax.ws.rs.core.Response.Status;

import org.eclipse.persistence.jaxb.rs.MOXyJsonProvider;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.BasicVitamServer;
import fr.gouv.vitam.common.server.VitamServer;



public class AccessResourceImplTest extends JerseyTest {


    private static final String X_HTTP_METHOD_OVERRIDE = "X-Http-Method-Override";
    // URI
    private static final String ACCESS_CONF = "access.conf";
    private static final String ACCESS_RESOURCE_URI = "access/v1";
    private static final String ACCESS_STATUS_URI = "/status";
    private static final String ACCESS_UNITS_URI = "/units";

    private static final int ASSURD_SERVER_PORT = 8187;

    private static VitamServer vitamServer;


    // QUERIES AND DSL
    // TODO
    // Create a "GET" query inspired by DSL, exemple from tech design story 76
    private static final String QUERY_TEST = "{ $query : [ { $eq : { 'title' : 'test' } } ], " +
        " $filter : { $orderby : { '#id' } }," +
        " $projection : {$fields : {#id : 1, title:2, transacdate:1}}" +
        " }";


    private static final String DATA =
        "{ \"_id\": \"aeaqaaaaaeaaaaakaarp4akuuf2ldmyaaaaq\", " + "\"data\": \"data1\" }";

    private static final String DATA2 =
        "{ \"_id\": \"aeaqaaaaaeaaaaakaarp4akuuf2ldmyaaaab\"," + "\"data\": \"data2\" }";

    // LOGGER
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AccessResourceImplTest.class);


    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        RestAssured.port = ASSURD_SERVER_PORT;
        RestAssured.basePath = ACCESS_RESOURCE_URI;

        try {
            vitamServer = AccessApplication.startApplication(new String[] {
                PropertiesUtils.getResourcesFile(ACCESS_CONF).getAbsolutePath(),
                Integer.toString(ASSURD_SERVER_PORT)});
            ((BasicVitamServer) vitamServer).start();
            LOGGER.debug("Beginning tests");
        } catch (FileNotFoundException | VitamApplicationServerException e) {
            LOGGER.error(e);
            throw new IllegalStateException(
                "Cannot start the Access Application Server", e);
        }
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        LOGGER.debug("Ending tests");
        try {
            ((BasicVitamServer) vitamServer).stop();
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
        }
    }

    @Before
    public void before() throws Exception {
        // client = ClientBuilder.newClient();
        //
        // accessModuleMock = mock(AccessModule.class);
        // accessModuleImplMock = mock(AccessModuleImpl.class);
        //
        // final String json = "{\"objects\" : [\"One\", \"Two\", \"Three\"]}";
        // final JsonNode arrNode = new ObjectMapper().readTree(json).get("objects");
        // jsonNode = arrNode;
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Override
    public javax.ws.rs.core.Application configure() {
        enable(TestProperties.LOG_TRAFFIC);
        enable(TestProperties.DUMP_ENTITY);
        enable(TestProperties.CONTAINER_FACTORY);
        enable(TestProperties.CONTAINER_PORT);
        set(TestProperties.CONTAINER_PORT, "8082");
        final ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.packages("fr.gouv.vitam.access.rest");
        resourceConfig.register(JacksonFeature.class);
        resourceConfig.register(MultiPartFeature.class);
        resourceConfig.register(MOXyJsonProvider.class);
        resourceConfig.register(AccessResourceImpl.class);
        return resourceConfig;
    }



    // Status
    /**
     * Tests the state of the access service API by get
     *
     * @throws Exception
     */
    @Test
    public void givenStartedServer_WhenGetStatus_ThenReturnStatusOk() throws Exception {
        get(ACCESS_STATUS_URI).then().statusCode(Status.OK.getStatusCode());
    }


    // Error cases
    /**
     * Test if the request is inconsistent
     *
     * @throws Exception
     */
    @Ignore("To implement")
    public void givenStartedServer_WhenRequestNotCorrect_ThenReturnError() throws Exception {

    }

    /**
     * Checks if the send parameter doesn't have Json format
     *
     * @throws Exception
     */
    @Test
    public void givenStartedServer_WhenRequestNotJson_ThenReturnError_UnsupportedMediaType() throws Exception {
        given()
            .contentType(ContentType.XML).header(X_HTTP_METHOD_OVERRIDE, "GET")
            .body(buildDSLWithOptions(QUERY_TEST, DATA2))
            .when().post(ACCESS_UNITS_URI).then().statusCode(Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode());
    }

    /**
     * Checks if the send parameter is a bad request
     *
     * @throws Exception
     */
    @Test
    public void givenStartedServer_WhenBadRequest_ThenReturnError_BadRequest() throws Exception {
        given()
            .contentType(ContentType.JSON).header(X_HTTP_METHOD_OVERRIDE, "GET")
            .body(buildDSLWithOptions(QUERY_TEST, DATA2))
            .when().post(ACCESS_UNITS_URI).then().statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void givenStartedServer_When_BadRequest_ThenReturnError_BadRequest() throws Exception {
        given()
            .contentType(ContentType.JSON).header(X_HTTP_METHOD_OVERRIDE, "GET")
            .body(buildDSLWithOptions(QUERY_TEST, DATA2))
            .when().post(ACCESS_UNITS_URI).then().statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void givenStartedServer_When_Empty_Http_Get_ThenReturnError_METHOD_NOT_ALLOWED() throws Exception {
        given()
            .contentType(ContentType.JSON).header(X_HTTP_METHOD_OVERRIDE, "ABC")
            .body(buildDSLWithOptions(QUERY_TEST, DATA2))
            .when().post(ACCESS_UNITS_URI).then().statusCode(Status.METHOD_NOT_ALLOWED.getStatusCode());
    }


    private static final String buildDSLWithOptions(String query, String data) {
        return "{ $roots : [ '' ], $query : [ " + query + " ], $data : " + data + " }";
    }

}

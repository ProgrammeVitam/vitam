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
package fr.gouv.vitam.ingest.external.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.client.IngestCollection;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.format.identification.FormatIdentifierFactory;
import fr.gouv.vitam.common.format.identification.model.FormatIdentifierResponse;
import fr.gouv.vitam.common.format.identification.siegfried.FormatIdentifierSiegfried;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.LocalFile;
import fr.gouv.vitam.common.security.rest.EndpointInfo;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.ingest.external.common.config.IngestExternalConfiguration;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClient;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClientFactory;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClientMock;
import fr.gouv.vitam.logbook.common.parameters.Contexts;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

public class IngestExternalResourceTest {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IngestExternalResourceTest.class);

    private static final String RESOURCE_URI = "/ingest-external/v1";
    private static final String STATUS_URI = "/status";
    private static final String INGEST_URI = "/ingests";
    private static final String INGEST_EXTERNAL_CONF = "ingest-external-test.conf";
    private static final Integer TENANT_ID = 0;
    private static final String UNEXISTING_TENANT_ID = "25";
    private static JunitHelper junitHelper;
    private static int serverPort;

    private static FormatIdentifierFactory formatIdentifierFactory = mock(FormatIdentifierFactory.class);
    private static IngestInternalClientFactory ingestInternalClientFactory = mock(IngestInternalClientFactory.class);

    private static IngestExternalMain application;
    private static FormatIdentifierSiegfried siegfried = mock(FormatIdentifierSiegfried.class);

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        when(formatIdentifierFactory.getFormatIdentifierFor(any())).thenReturn(siegfried);
        IngestInternalClient ingestInternalClient = mock(IngestInternalClient.class);
        when(ingestInternalClientFactory.getClient()).thenReturn(ingestInternalClient);
        when(ingestInternalClient.getWorkflowDetails(anyString())).thenReturn(
            new IngestInternalClientMock().getWorkflowDetails("DEFAULT_WORKFLOW")
        );
        junitHelper = JunitHelper.getInstance();
        serverPort = junitHelper.findAvailablePort();
        // TODO: 08/02/19 remove static (no time)
        BusinessApplicationTest.formatIdentifierFactory = formatIdentifierFactory;
        BusinessApplicationTest.ingestInternalClientFactory = ingestInternalClientFactory;

        // Update configuration with full upload folder path
        File configurationFile = PropertiesUtils.getResourceFile(INGEST_EXTERNAL_CONF);
        final IngestExternalConfiguration configuration = PropertiesUtils.readYaml(
            configurationFile,
            IngestExternalConfiguration.class
        );
        configuration.setBaseUploadPath(configurationFile.getParentFile().getCanonicalPath());
        PropertiesUtils.writeYaml(configurationFile, configuration);

        RestAssured.port = serverPort;
        RestAssured.basePath = RESOURCE_URI;
        try {
            application = new IngestExternalMain(INGEST_EXTERNAL_CONF, BusinessApplicationTest.class, null);
            application.start();
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
            throw new IllegalStateException("Cannot start the Ingest External Application Server", e);
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

    @Before
    public void before() {
        reset(siegfried);
    }

    @Test
    public final void testGetStatus() {
        // test with header on
        given()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get(STATUS_URI)
            .then()
            .statusCode(Status.NO_CONTENT.getStatusCode());

        // test without header - no content must be obtained
        given().when().get(STATUS_URI).then().statusCode(Status.NO_CONTENT.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void givenRequestWithoutTenantIdThenReturnPreconditionFailed() throws Exception {
        try (InputStream stream = PropertiesUtils.getResourceAsStream("no-virus.txt")) {
            when(siegfried.analysePath(any())).thenReturn(getFormatIdentifierZipResponse());

            given()
                .contentType(ContentType.BINARY)
                .body(stream)
                .when()
                .post(INGEST_URI)
                .then()
                .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

            RestAssured.given()
                .when()
                .get(INGEST_URI + "/1/" + IngestCollection.ARCHIVETRANSFERREPLY.getCollectionName())
                .then()
                .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
        }
    }

    @Test
    public void givenRequestWithoutIncorrectTenantIdThenReturnUnauthorized() throws Exception {
        try (InputStream stream = PropertiesUtils.getResourceAsStream("no-virus.txt")) {
            when(siegfried.analysePath(any())).thenReturn(getFormatIdentifierZipResponse());

            given()
                .contentType(ContentType.BINARY)
                .body(stream)
                .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
                .when()
                .post(INGEST_URI)
                .then()
                .statusCode(Status.UNAUTHORIZED.getStatusCode());

            RestAssured.given()
                .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
                .when()
                .get(INGEST_URI + "/1/" + IngestCollection.ARCHIVETRANSFERREPLY.getCollectionName())
                .then()
                .statusCode(Status.UNAUTHORIZED.getStatusCode());
        }
    }

    @Test
    public void givenAnInputstreamWhenUploadThenReturnOK() throws Exception {
        try (InputStream stream = PropertiesUtils.getResourceAsStream("no-virus.txt")) {
            when(siegfried.analysePath(any())).thenReturn(getFormatIdentifierZipResponse());

            given()
                .contentType(ContentType.BINARY)
                .body(stream)
                .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
                .header(GlobalDataRest.X_CONTEXT_ID, Contexts.DEFAULT_WORKFLOW)
                .when()
                .post(INGEST_URI)
                .then()
                .statusCode(Status.ACCEPTED.getStatusCode());
        }
    }

    @Test
    public void givenALocalFilePathWhenUploadedThenReturnOK() throws Exception {
        LocalFile localFile = new LocalFile("no-virus.txt");
        when(siegfried.analysePath(any())).thenReturn(getFormatIdentifierZipResponse());

        given()
            .contentType(ContentType.JSON)
            .body(localFile)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_CONTEXT_ID, Contexts.DEFAULT_WORKFLOW)
            .when()
            .post(INGEST_URI)
            .then()
            .statusCode(Status.ACCEPTED.getStatusCode());
    }

    @Test
    public void givenAnIncorrectLocalFilePathWhenUploadedThenReturnBadRequest() throws Exception {
        // this is incorrect, this will be rejected
        LocalFile localFile = new LocalFile("../no-virus.txt");
        when(siegfried.analysePath(any())).thenReturn(getFormatIdentifierZipResponse());

        given()
            .contentType(ContentType.JSON)
            .body(localFile)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_CONTEXT_ID, Contexts.DEFAULT_WORKFLOW)
            .when()
            .post(INGEST_URI)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void givenANonExistingPathWhenUploadedThenReturnInternalServerError() throws Exception {
        LocalFile localFileWithNonExistingPath = new LocalFile("NonExistingPath");
        when(siegfried.analysePath(any())).thenReturn(getFormatIdentifierZipResponse());

        given()
            .contentType(ContentType.JSON)
            .body(localFileWithNonExistingPath)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_CONTEXT_ID, Contexts.DEFAULT_WORKFLOW)
            .when()
            .post(INGEST_URI)
            .then()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    private List<FormatIdentifierResponse> getFormatIdentifierZipResponse() {
        final List<FormatIdentifierResponse> list = new ArrayList<>();
        list.add(new FormatIdentifierResponse("ZIP Format", "application/zip", "x-fmt/263", "pronom"));
        return list;
    }

    @Test
    public void downloadIngestManifestsAsStream() throws Exception {
        RestAssured.given()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_CONTEXT_ID, Contexts.DEFAULT_WORKFLOW)
            .when()
            .get(INGEST_URI + "/1/" + IngestCollection.MANIFESTS.getCollectionName())
            .then()
            .statusCode(Status.OK.getStatusCode());
    }

    @Test
    public void listResourceEndpoints() throws Exception {
        RestAssured.given()
            .accept(MediaType.APPLICATION_JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .options("/")
            .then()
            .statusCode(Status.OK.getStatusCode())
            .body(
                new BaseMatcher<String>() {
                    @Override
                    public boolean matches(Object o) {
                        try {
                            // Deserialize json
                            ObjectMapper mapper = new ObjectMapper();
                            EndpointInfo[] endpoints = mapper.readValue((String) o, EndpointInfo[].class);

                            // Find ingest post endpoint
                            EndpointInfo postIngests = Arrays.stream(endpoints)
                                .filter(ep -> ep.getPermission().equals("ingests:create"))
                                .findFirst()
                                .orElseThrow(RuntimeException::new);

                            // Check...
                            Assert.assertEquals("POST", postIngests.getVerb());
                            Assert.assertEquals("/ingest-external/v1/ingests/", postIngests.getEndpoint());

                            Assert.assertEquals(1, postIngests.getConsumedMediaTypes().length);
                            Assert.assertEquals("application/octet-stream", postIngests.getConsumedMediaTypes()[0]);

                            Assert.assertEquals(0, postIngests.getProducedMediaTypes().length);

                            Assert.assertEquals(
                                "Envoyer un SIP à Vitam afin qu'il en réalise l'entrée",
                                postIngests.getDescription()
                            );

                            return true;
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public void describeTo(Description description) {}
                }
            );
    }
}

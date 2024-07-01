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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import fr.gouv.vitam.access.external.api.AccessExtAPI;
import fr.gouv.vitam.access.external.api.AdminCollections;
import fr.gouv.vitam.access.internal.client.AccessInternalClient;
import fr.gouv.vitam.access.internal.client.AccessInternalClientFactory;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.ClientMockResultHelper;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.action.SetAction;
import fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.builder.request.single.Update;
import fr.gouv.vitam.common.database.parser.request.adapter.SingleVarNameAdapter;
import fr.gouv.vitam.common.database.parser.request.single.UpdateParserSingle;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.FakeInputStream;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.ProcessQuery;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.AgenciesModel;
import fr.gouv.vitam.common.server.application.junit.ResteasyTestApplication;
import fr.gouv.vitam.common.server.application.resources.VitamStatusService;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientMock;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.functional.administration.common.exception.DatabaseConflictException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialNotFoundException;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClient;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClientFactory;
import fr.gouv.vitam.logbook.common.parameters.Contexts;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import io.restassured.http.ContentType;
import org.hamcrest.CoreMatchers;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static fr.gouv.vitam.common.GlobalDataRest.X_HTTP_METHOD_OVERRIDE;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static io.restassured.RestAssured.basePath;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.port;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

public class AdminManagementExternalResourceTest extends ResteasyTestApplication {

    private static final String CODE_VALIDATION_DSL = VitamCodeHelper.getCode(VitamCode.GLOBAL_INVALID_DSL);

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AdminManagementExternalResourceTest.class);

    private static final String RESOURCE_URI = "/admin-external/v1";

    private static final String FORMAT_URI = "/" + AdminCollections.FORMATS.getName();
    private static final String FORMAT_CHECK_URI = "/" + AdminCollections.FORMATS.getCheckURI();

    private static final String RULES_URI = "/" + AdminCollections.RULES.getName();
    private static final String RULES_CHECK_URI = "/" + AdminCollections.RULES.getCheckURI();

    private static final String AGENCIES_URI = "/" + AdminCollections.AGENCIES.getName();
    private static final String AGENCIES_CHECK_URI = "/" + AdminCollections.AGENCIES.getCheckURI();

    private static final String EVIDENCE_AUDIT = "evidenceaudit";
    private static final String RECTIFICATION_AUDIT = "rectificationaudit";

    private static final String OPERATIONS_URI = "/logbookoperations";

    private static final String DOCUMENT_ID = "/1";

    private static final String RULE_ID = "/APP-00001";

    private static final String AGENCY_ID = "/AG-000001";

    private static final String WRONG_URI = "/wrong-uri";

    private static final String TENANT_ID = "0";

    private static final String UNEXISTING_TENANT_ID = "25";
    private static final String CONTEXT_URI = "/contexts";
    private static final String PROFILE_URI = "/profiles";
    private static final String ARCHIVE_UNIT_PROFILE_URI = "/archiveunitprofiles";
    private static final String ONTOLOGIES_URI = "/ontologies";
    private static final String AGENCY_URI = "/agencies";
    private static final String CONTRACT_ID = "NAME";

    private static final String TRACEABILITY_OPERATION_ID = "op_id";
    private static final String TRACEABILITY_OPERATION_BASE_URI = AccessExtAPI.TRACEABILITY_API + "/";
    private static final String ACCESSION_REGISTER_DETAIL_URI =
        AccessExtAPI.ACCESSION_REGISTERS_API + "/FR_ORG_AGEC/" + AccessExtAPI.ACCESSION_REGISTERS_DETAIL;

    private static final String GOOD_ID = "goodId";
    private static final String SECURITY_PROFILES_URI = "/securityprofiles";
    private static final String RULE_FILE = "jeu_donnees_OK_regles_CSV_regles.csv";

    private static final BusinessApplicationTest businessApplicationTest = new BusinessApplicationTest();

    private static final AccessInternalClientFactory accessInternalClientFactory =
        businessApplicationTest.getAccessInternalClientFactory();
    private static final AccessInternalClient accessInternalClient = mock(AccessInternalClient.class);

    private static final AdminManagementClientFactory adminManagementClientFactory =
        businessApplicationTest.getAdminManagementClientFactory();
    private static final AdminManagementClient adminManagementClient = mock(AdminManagementClient.class);

    private static final IngestInternalClientFactory ingestInternalClientFactory =
        businessApplicationTest.getIngestInternalClientFactory();
    private static final IngestInternalClient ingestInternalClient = mock(IngestInternalClient.class);

    private static final VitamStatusService vitamStatusService = businessApplicationTest.getVitamStatusService();

    @Override
    public Set<Object> getResources() {
        return businessApplicationTest.getSingletons();
    }

    @Override
    public Set<Class<?>> getClasses() {
        return businessApplicationTest.getClasses();
    }

    private InputStream stream;
    private static JunitHelper junitHelper;
    private static int serverPort;
    private static AccessExternalMain application;

    @BeforeClass
    public static void setUpBeforeClass() {
        junitHelper = JunitHelper.getInstance();
        serverPort = junitHelper.findAvailablePort();

        port = serverPort;
        basePath = RESOURCE_URI;

        try {
            application = new AccessExternalMain(
                "access-external-test.conf",
                AdminManagementExternalResourceTest.class,
                null
            );
            application.start();
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
            throw new IllegalStateException("Cannot start the Access External Application Server", e);
        }
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        try {
            if (application != null && application.getVitamServer() != null) {
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
    public void setUpBefore() {
        reset(accessInternalClient);
        reset(accessInternalClientFactory);
        reset(adminManagementClient);
        reset(adminManagementClientFactory);
        reset(ingestInternalClient);
        reset(ingestInternalClientFactory);

        when(accessInternalClientFactory.getClient()).thenReturn(accessInternalClient);
        when(adminManagementClientFactory.getClient()).thenReturn(adminManagementClient);
        when(ingestInternalClientFactory.getClient()).thenReturn(ingestInternalClient);
    }

    @Test
    public void testRectificationAudit() throws Exception {
        when(adminManagementClient.rectificationAudit(anyString())).thenReturn(
            new AdminManagementClientMock().rectificationAudit("opi")
        );

        given()
            .contentType(ContentType.JSON)
            .body("id")
            .header(GlobalDataRest.X_TENANT_ID, 0)
            .when()
            .post(RECTIFICATION_AUDIT)
            .then()
            .statusCode(Status.OK.getStatusCode());
    }

    @Test
    public void evidenceAudit() throws Exception {
        when(adminManagementClient.evidenceAudit(any())).thenReturn(
            new AdminManagementClientMock().evidenceAudit(JsonHandler.createObjectNode())
        );

        given()
            .contentType(ContentType.JSON)
            .body(new SelectMultiQuery().getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, 0)
            .when()
            .post(EVIDENCE_AUDIT)
            .then()
            .statusCode(Status.OK.getStatusCode());
    }

    @Test
    public void testCheckDocument() throws Exception {
        AdminManagementClientMock adminManagementClientMock = new AdminManagementClientMock();
        when(adminManagementClient.checkFormat(any())).thenReturn(
            adminManagementClientMock.checkFormat(new FakeInputStream(1))
        );

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given()
            .contentType(ContentType.BINARY)
            .body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(FORMAT_CHECK_URI)
            .then()
            .statusCode(Status.OK.getStatusCode());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given()
            .contentType(ContentType.BINARY)
            .body(stream)
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when()
            .post(FORMAT_CHECK_URI)
            .then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given()
            .contentType(ContentType.BINARY)
            .body(stream)
            .when()
            .post(FORMAT_CHECK_URI)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        when(adminManagementClient.checkRulesFile(any())).thenReturn(
            adminManagementClientMock.checkRulesFile(new FakeInputStream(1))
        );

        stream = PropertiesUtils.getResourceAsStream(RULE_FILE);
        given()
            .contentType(ContentType.BINARY)
            .body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(RULES_CHECK_URI)
            .then()
            .statusCode(Status.OK.getStatusCode());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given()
            .contentType(ContentType.BINARY)
            .body(stream)
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when()
            .post(RULES_CHECK_URI)
            .then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given()
            .contentType(ContentType.BINARY)
            .body(stream)
            .when()
            .post(RULES_CHECK_URI)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        when(adminManagementClient.checkAgenciesFile(any())).thenReturn(
            adminManagementClientMock.checkAgenciesFile(new FakeInputStream(1))
        );

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given()
            .contentType(ContentType.BINARY)
            .body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(AGENCIES_CHECK_URI)
            .then()
            .statusCode(Status.OK.getStatusCode());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given()
            .contentType(ContentType.BINARY)
            .body(stream)
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when()
            .post(AGENCIES_CHECK_URI)
            .then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given()
            .contentType(ContentType.BINARY)
            .body(stream)
            .when()
            .post(AGENCIES_CHECK_URI)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given()
            .contentType(ContentType.BINARY)
            .body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .put(WRONG_URI)
            .then()
            .statusCode(Status.NOT_FOUND.getStatusCode());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given()
            .contentType(ContentType.BINARY)
            .body(stream)
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when()
            .put(WRONG_URI)
            .then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given()
            .contentType(ContentType.BINARY)
            .body(stream)
            .when()
            .put(WRONG_URI)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void testCheckDocumentError() throws Exception {
        doThrow(new ReferentialException("Referential Exception")).when(adminManagementClient).checkFormat(any());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given()
            .contentType(ContentType.BINARY)
            .body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(FORMAT_CHECK_URI)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given()
            .contentType(ContentType.BINARY)
            .body(stream)
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when()
            .post(FORMAT_CHECK_URI)
            .then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given()
            .contentType(ContentType.BINARY)
            .body(stream)
            .when()
            .post(FORMAT_CHECK_URI)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void insertDocument() throws Exception {
        when(adminManagementClient.importFormat(any(), any())).thenReturn(Status.CREATED);
        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given()
            .contentType(ContentType.BINARY)
            .body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_FILENAME, "vitam.conf")
            .when()
            .post(FORMAT_URI)
            .then()
            .statusCode(Status.CREATED.getStatusCode());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given()
            .contentType(ContentType.BINARY)
            .body(stream)
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .header(GlobalDataRest.X_FILENAME, "vitam.conf")
            .when()
            .post(FORMAT_URI)
            .then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given()
            .contentType(ContentType.BINARY)
            .body(stream)
            .header(GlobalDataRest.X_FILENAME, "vitam.conf")
            .when()
            .post(FORMAT_URI)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        when(adminManagementClient.importRulesFile(any(), any())).thenReturn(Status.CREATED);
        stream = PropertiesUtils.getResourceAsStream(RULE_FILE);
        given()
            .contentType(ContentType.BINARY)
            .body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_FILENAME, "vitam.conf")
            .when()
            .post(RULES_URI)
            .then()
            .statusCode(Status.CREATED.getStatusCode());

        when(adminManagementClient.importAgenciesFile(any(), any())).thenReturn(Status.CREATED);
        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given()
            .contentType(ContentType.BINARY)
            .body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_FILENAME, "vitam.conf")
            .when()
            .post(AGENCIES_URI)
            .then()
            .statusCode(Status.CREATED.getStatusCode());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given()
            .contentType(ContentType.BINARY)
            .body(stream)
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .header(GlobalDataRest.X_FILENAME, "vitam.conf")
            .when()
            .post(RULES_URI)
            .then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given()
            .contentType(ContentType.BINARY)
            .body(stream)
            .header(GlobalDataRest.X_FILENAME, "vitam.conf")
            .when()
            .post(RULES_URI)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.BINARY)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_FILENAME, "vitam.conf")
            .when()
            .post(WRONG_URI)
            .then()
            .statusCode(Status.NOT_FOUND.getStatusCode());

        given()
            .contentType(ContentType.BINARY)
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .header(GlobalDataRest.X_FILENAME, "vitam.conf")
            .when()
            .post(WRONG_URI)
            .then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.BINARY)
            .when()
            .post(WRONG_URI)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void insertDocumentError() throws Exception {
        doThrow(new ReferentialException("")).when(adminManagementClient).importFormat(any(), any());
        doReturn(Response.ok().build()).when(adminManagementClient).checkFormat(any());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given()
            .contentType(ContentType.BINARY)
            .body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(FORMAT_URI)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());

        doThrow(new DatabaseConflictException("")).when(adminManagementClient).importFormat(any(), any());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given()
            .contentType(ContentType.BINARY)
            .body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(FORMAT_URI)
            .then()
            .statusCode(Status.CONFLICT.getStatusCode());
    }

    @Test
    public void testFindRules() throws InvalidCreateOperationException {
        final Select select = new Select();
        select.setQuery(eq("Identifier", "APP-00001"));
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and()
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(RULES_URI)
            .then()
            .statusCode(Status.OK.getStatusCode());

        final Select emptyQuerySelect = new Select();
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(emptyQuerySelect.getFinalSelect())
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and()
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(RULES_URI)
            .then()
            .statusCode(Status.OK.getStatusCode());

        final SelectMultiQuery selectMultiple = new SelectMultiQuery();
        selectMultiple.setQuery(eq("Identifier", "APP-00001"));
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(selectMultiple.getFinalSelect())
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and()
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(RULES_URI)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode())
            .body(CoreMatchers.containsString(CODE_VALIDATION_DSL));

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and()
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(RULES_URI)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and()
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when()
            .post(RULES_URI)
            .then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .when()
            .post(RULES_URI)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void testFindFormats() throws Exception {
        final Select select = new Select();
        select.setQuery(eq("PUID", "x-fmt/348"));
        when(adminManagementClient.getFormats(any())).thenReturn(ClientMockResultHelper.getFormat());
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and()
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(FORMAT_URI)
            .then()
            .statusCode(Status.OK.getStatusCode());

        final Select emptyQuerySelect = new Select();
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(emptyQuerySelect.getFinalSelect())
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and()
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(FORMAT_URI)
            .then()
            .statusCode(Status.OK.getStatusCode());

        final SelectMultiQuery selectMultiple = new SelectMultiQuery();
        selectMultiple.setQuery(eq("PUID", "x-fmt/348"));
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(selectMultiple.getFinalSelect())
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and()
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(FORMAT_URI)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode())
            .body(CoreMatchers.containsString(CODE_VALIDATION_DSL));

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and()
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(FORMAT_URI)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and()
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when()
            .post(FORMAT_URI)
            .then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .when()
            .post(FORMAT_URI)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void testFindAccessionRegister() throws Exception {
        final Select select = new Select();
        select.setQuery(eq("OriginatingAgency", "RATP"));
        AdminManagementClientMock adminManagementClientMock = new AdminManagementClientMock();
        when(adminManagementClient.getAccessionRegister(any())).thenReturn(
            adminManagementClientMock.getAccessionRegister(JsonHandler.createObjectNode())
        );
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and()
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(AccessExtAPI.ACCESSION_REGISTERS_API)
            .then()
            .statusCode(Status.OK.getStatusCode());

        final Select emptyQuerySelect = new Select();
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(emptyQuerySelect.getFinalSelect())
            .and()
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get(AccessExtAPI.ACCESSION_REGISTERS_API)
            .then()
            .statusCode(Status.OK.getStatusCode());

        final SelectMultiQuery selectMultiple = new SelectMultiQuery();
        selectMultiple.setQuery(eq("OriginatingAgency", "RATP"));
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(selectMultiple.getFinalSelect())
            .and()
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get(AccessExtAPI.ACCESSION_REGISTERS_API)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode())
            .body(CoreMatchers.containsString(CODE_VALIDATION_DSL));

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .and()
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get(AccessExtAPI.ACCESSION_REGISTERS_API)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .and()
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when()
            .get(AccessExtAPI.ACCESSION_REGISTERS_API)
            .then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .when()
            .get(AccessExtAPI.ACCESSION_REGISTERS_API)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void testFindAccessionRegisterDetail() throws Exception {
        final Select select = new Select();
        select.setQuery(eq("OriginatingAgency", "RATP"));
        AdminManagementClientMock adminManagementClientMock = new AdminManagementClientMock();
        when(adminManagementClient.getAccessionRegisterDetail(any(), any())).thenReturn(
            adminManagementClientMock.getAccessionRegisterDetail("id", JsonHandler.createObjectNode())
        );

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and()
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(ACCESSION_REGISTER_DETAIL_URI)
            .then()
            .statusCode(Status.OK.getStatusCode());

        final Select emptyQuerySelect = new Select();
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(emptyQuerySelect.getFinalSelect())
            .and()
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get(ACCESSION_REGISTER_DETAIL_URI)
            .then()
            .statusCode(Status.OK.getStatusCode());

        final SelectMultiQuery selectMultiple = new SelectMultiQuery();
        selectMultiple.setQuery(eq("OriginatingAgency", "RATP"));
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(selectMultiple.getFinalSelect())
            .and()
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get(ACCESSION_REGISTER_DETAIL_URI)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode())
            .body(CoreMatchers.containsString(CODE_VALIDATION_DSL));

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .and()
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get(ACCESSION_REGISTER_DETAIL_URI)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .and()
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when()
            .get(ACCESSION_REGISTER_DETAIL_URI)
            .then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .when()
            .get(ACCESSION_REGISTER_DETAIL_URI)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void testGetDocuments() throws Exception {
        final Select select = new Select();
        select.setQuery(eq("Id", "APP-00001"));

        when(adminManagementClient.getAgencies(any())).thenReturn(
            new AdminManagementClientMock().getAgencies(JsonHandler.createObjectNode())
        );

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(RULES_URI + RULE_ID)
            .then()
            .statusCode(Status.OK.getStatusCode());

        when(adminManagementClient.getAgencyById(any())).thenReturn(
            new AdminManagementClientMock().getAgencyById("id")
        );

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(AGENCIES_URI + AGENCY_ID)
            .then()
            .statusCode(Status.OK.getStatusCode());

        given()
            .accept(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when()
            .post(RULES_URI + DOCUMENT_ID)
            .then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .accept(ContentType.JSON)
            .body(select.getFinalSelect())
            .when()
            .post(RULES_URI + DOCUMENT_ID)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .accept(ContentType.JSON)
            .body(select.getFinalSelect())
            .when()
            .post(RULES_URI + DOCUMENT_ID)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(FORMAT_URI + DOCUMENT_ID)
            .then()
            .statusCode(Status.OK.getStatusCode());

        given()
            .accept(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when()
            .get(FORMAT_URI + DOCUMENT_ID)
            .then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .accept(ContentType.JSON)
            .body(select.getFinalSelect())
            .when()
            .get(FORMAT_URI + DOCUMENT_ID)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(WRONG_URI + DOCUMENT_ID)
            .then()
            .statusCode(Status.NOT_FOUND.getStatusCode());

        given()
            .accept(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when()
            .get(WRONG_URI + DOCUMENT_ID)
            .then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .accept(ContentType.JSON)
            .body(select.getFinalSelect())
            .when()
            .get(WRONG_URI + DOCUMENT_ID)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get(WRONG_URI)
            .then()
            .statusCode(Status.NOT_FOUND.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when()
            .get(WRONG_URI)
            .then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .when()
            .get(WRONG_URI)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        when(adminManagementClient.getAccessionRegister(any())).thenReturn(
            new AdminManagementClientMock().getAccessionRegister(JsonHandler.createObjectNode())
        );

        given()
            .contentType(ContentType.JSON)
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(select.getFinalSelect())
            .when()
            .post(AccessExtAPI.ACCESSION_REGISTERS_API)
            .then()
            .statusCode(Status.OK.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .body(select.getFinalSelect())
            .when()
            .post(AccessExtAPI.ACCESSION_REGISTERS_API)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .body(select.getFinalSelect())
            .when()
            .post(AccessExtAPI.ACCESSION_REGISTERS_API + "/" + GOOD_ID)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void testGetDocumentsError() throws Exception {
        doThrow(new ReferentialException("")).when(adminManagementClient).getFormats(any());
        doThrow(new ReferentialException("")).when(adminManagementClient).getFormatByID(any());
        final Select select = new Select();
        select.setQuery(eq("Id", "APP-00001"));

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get(FORMAT_URI + DOCUMENT_ID)
            .then()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get(FORMAT_URI)
            .then()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());

        doThrow(new InvalidParseOperationException("")).when(adminManagementClient).getFormats(any());
        doThrow(new InvalidParseOperationException("")).when(adminManagementClient).getFormatByID(any());

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get(FORMAT_URI + DOCUMENT_ID)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get(FORMAT_URI)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());

        RequestResponse rsp = new RequestResponseOK<>().setHttpCode(Status.OK.getStatusCode());
        when(adminManagementClient.getAccessionRegister(any())).thenReturn(rsp);
        when(adminManagementClient.getAccessionRegisterDetail(any(), any())).thenReturn(rsp);

        given()
            .contentType(ContentType.JSON)
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(select.getFinalSelect())
            .when()
            .post(AccessExtAPI.ACCESSION_REGISTERS_API)
            .then()
            .statusCode(Status.OK.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .body(select.getFinalSelect())
            .when()
            .post(AccessExtAPI.ACCESSION_REGISTERS_API)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and()
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(select.getFinalSelect())
            .when()
            .post(ACCESSION_REGISTER_DETAIL_URI)
            .then()
            .statusCode(Status.OK.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .body(select.getFinalSelect())
            .when()
            .post(ACCESSION_REGISTER_DETAIL_URI)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        doThrow(new InvalidParseOperationException("")).when(adminManagementClient).getAccessionRegister(any());
        doThrow(new InvalidParseOperationException(""))
            .when(adminManagementClient)
            .getAccessionRegisterDetail(anyString(), any());

        given()
            .contentType(ContentType.JSON)
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(select.getFinalSelect())
            .when()
            .post(AccessExtAPI.ACCESSION_REGISTERS_API)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(select.getFinalSelect())
            .when()
            .post(ACCESSION_REGISTER_DETAIL_URI)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());

        doThrow(new IllegalArgumentException("")).when(adminManagementClient).getAccessionRegister(any());
        doThrow(new IllegalArgumentException(""))
            .when(adminManagementClient)
            .getAccessionRegisterDetail(anyString(), any());

        given()
            .contentType(ContentType.JSON)
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(select.getFinalSelect())
            .when()
            .post(AccessExtAPI.ACCESSION_REGISTERS_API)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(select.getFinalSelect())
            .when()
            .post(ACCESSION_REGISTER_DETAIL_URI)
            .then()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void testImportIngestContractsWithInvalidFileBadRequest() throws Exception {
        doReturn(Status.BAD_REQUEST).when(adminManagementClient).importIngestContracts(any());

        given()
            .contentType(ContentType.JSON)
            .body(JsonHandler.createObjectNode())
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(AccessExtAPI.INGEST_CONTRACT_API)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode())
            .contentType("application/json");
    }

    @Test
    public void testImportCSVDocumentWithHTMLContent() throws Exception {
        stream = PropertiesUtils.getResourceAsStream("CSV_HTML.csv");
        given()
            .contentType(ContentType.BINARY)
            .body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_FILENAME, "CSV_HTML.csv")
            .when()
            .post(AGENCIES_URI)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());

        stream = PropertiesUtils.getResourceAsStream("CSV_HTML.csv");
        given()
            .contentType(ContentType.BINARY)
            .body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_FILENAME, "CSV_HTML.csv")
            .when()
            .post(RULES_URI)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void testImportJSONDocumentWithHTMLContent() throws Exception {
        File file = PropertiesUtils.getResourceFile("JSON_HTML.json");
        JsonNode json = JsonHandler.getFromFile(file);

        given()
            .contentType(ContentType.JSON)
            .body(json)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(AccessExtAPI.CONTEXTS_API)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(json)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(AccessExtAPI.INGEST_CONTRACT_API)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(json)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(AccessExtAPI.ACCESS_CONTRACT_API)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void testimportValidIngestContractsFileReturnCreated() throws Exception {
        doReturn(Status.CREATED).when(adminManagementClient).importIngestContracts(any());
        File contractFile = PropertiesUtils.getResourceFile("referential_contracts_ok.json");
        JsonNode json = JsonHandler.getFromFile(contractFile);
        given()
            .contentType(ContentType.JSON)
            .body(json)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(AccessExtAPI.INGEST_CONTRACT_API)
            .then()
            .statusCode(Status.CREATED.getStatusCode());
    }

    @Test
    public void testfindIngestContractsFile() throws Exception {
        doReturn(new RequestResponseOK<>().addAllResults(getIngestContracts()))
            .when(adminManagementClient)
            .findIngestContracts(any());

        Select select = new Select();
        select.setQuery(eq("Status", "ACTIVE"));

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get(AccessExtAPI.INGEST_CONTRACT_API)
            .then()
            .statusCode(Status.OK.getStatusCode());

        Select emptySelect = new Select();
        given()
            .contentType(ContentType.JSON)
            .body(emptySelect.getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get(AccessExtAPI.INGEST_CONTRACT_API)
            .then()
            .statusCode(Status.OK.getStatusCode());

        SelectMultiQuery selectMulti = new SelectMultiQuery();
        selectMulti.setQuery(eq("Status", "ACTIVE"));
        given()
            .contentType(ContentType.JSON)
            .body(selectMulti.getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get(AccessExtAPI.INGEST_CONTRACT_API)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode())
            .body(CoreMatchers.containsString(CODE_VALIDATION_DSL));

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get(AccessExtAPI.INGEST_CONTRACT_API)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when()
            .get(AccessExtAPI.INGEST_CONTRACT_API)
            .then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());
    }

    @Test
    public void testImportAccessContractsWithInvalidFileBadRequest() throws Exception {
        doReturn(Status.BAD_REQUEST).when(adminManagementClient).importAccessContracts(any());

        given()
            .contentType(ContentType.JSON)
            .body(JsonHandler.createObjectNode())
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(AccessExtAPI.ACCESS_CONTRACT_API)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode())
            .contentType("application/json");
    }

    @Test
    public void testimportValidAccessContractsFileReturnCreated() throws Exception {
        doReturn(Status.CREATED).when(adminManagementClient).importAccessContracts(any());
        File contractFile = PropertiesUtils.getResourceFile("contracts_access_ok.json");
        JsonNode json = JsonHandler.getFromFile(contractFile);

        given()
            .contentType(ContentType.JSON)
            .body(json)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(AccessExtAPI.ACCESS_CONTRACT_API)
            .then()
            .statusCode(Status.CREATED.getStatusCode());
    }

    @Test
    public void testfindAccessContractsFile() throws Exception {
        doReturn(new RequestResponseOK<>().addAllResults(getAccessContracts()))
            .when(adminManagementClient)
            .findAccessContracts(any());

        Select select = new Select();
        select.setQuery(eq("Status", "ACTIVE"));

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get(AccessExtAPI.ACCESS_CONTRACT_API)
            .then()
            .statusCode(Status.OK.getStatusCode());

        Select emptySelect = new Select();
        given()
            .contentType(ContentType.JSON)
            .body(emptySelect.getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get(AccessExtAPI.ACCESS_CONTRACT_API)
            .then()
            .statusCode(Status.OK.getStatusCode());

        SelectMultiQuery selectMulti = new SelectMultiQuery();
        selectMulti.setQuery(eq("Status", "ACTIVE"));
        given()
            .contentType(ContentType.JSON)
            .body(selectMulti.getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get(AccessExtAPI.ACCESS_CONTRACT_API)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode())
            .body(CoreMatchers.containsString(CODE_VALIDATION_DSL));

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get(AccessExtAPI.ACCESS_CONTRACT_API)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when()
            .get(AccessExtAPI.ACCESS_CONTRACT_API)
            .then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());
    }

    @Test
    public void testCreateProfileWithInvalidFileBadRequest() throws Exception {
        doReturn(new VitamError("").setHttpCode(Status.BAD_REQUEST.getStatusCode()))
            .when(adminManagementClient)
            .createProfiles(any());

        File fileProfiles = PropertiesUtils.getResourceFile("profile_missing_identifier.json");
        JsonNode json = JsonHandler.getFromFile(fileProfiles);

        given()
            .contentType(ContentType.JSON)
            .body(json)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(PROFILE_URI)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode())
            .contentType("application/json");
    }

    @Test
    public void testcreateValidProfileReturnCreated() throws Exception {
        doReturn(new RequestResponseOK<>().setHttpCode(Status.CREATED.getStatusCode()))
            .when(adminManagementClient)
            .createProfiles(any());

        File fileProfiles = PropertiesUtils.getResourceFile("profiles_ok.json");
        JsonNode json = JsonHandler.getFromFile(fileProfiles);

        given()
            .contentType(ContentType.JSON)
            .body(json)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(PROFILE_URI)
            .then()
            .statusCode(Status.CREATED.getStatusCode())
            .contentType("application/json");
    }

    @Test
    public void testfindProfiles() throws Exception {
        doReturn(new RequestResponseOK<>().addAllResults(getAccessContracts()))
            .when(adminManagementClient)
            .findProfiles(any());

        final Select select = new Select();
        select.setQuery(eq("Status", "ACTIVE"));
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(PROFILE_URI)
            .then()
            .statusCode(Status.OK.getStatusCode());

        final Select emptyQuerySelect = new Select();
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(emptyQuerySelect.getFinalSelect())
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(PROFILE_URI)
            .then()
            .statusCode(Status.OK.getStatusCode());

        final SelectMultiQuery selectMultiple = new SelectMultiQuery();
        selectMultiple.setQuery(eq("Status", "ACTIVE"));
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(selectMultiple.getFinalSelect())
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(PROFILE_URI)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode())
            .body(CoreMatchers.containsString(CODE_VALIDATION_DSL));

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(PROFILE_URI)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and()
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when()
            .post(PROFILE_URI)
            .then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .when()
            .post(PROFILE_URI)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void testFindAgencies() throws Exception {
        doReturn(new RequestResponseOK<AgenciesModel>().addAllResults(getAgencies()).toJsonNode())
            .when(adminManagementClient)
            .getAgencies(any());

        final Select select = new Select();
        select.setQuery(eq("Status", "ACTIVE"));
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get(AGENCY_URI)
            .then()
            .statusCode(Status.OK.getStatusCode());

        final Select emptyQuerySelect = new Select();
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(emptyQuerySelect.getFinalSelect())
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get(AGENCY_URI)
            .then()
            .statusCode(Status.OK.getStatusCode());

        final SelectMultiQuery selectMultiple = new SelectMultiQuery();
        selectMultiple.setQuery(eq("Status", "ACTIVE"));
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(selectMultiple.getFinalSelect())
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get(AGENCY_URI)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode())
            .body(CoreMatchers.containsString(CODE_VALIDATION_DSL));

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get(AGENCY_URI)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .and()
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when()
            .get(AGENCY_URI)
            .then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .when()
            .get(AGENCY_URI)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void testFindAgenciesById() throws Exception {
        doReturn(new RequestResponseOK<AgenciesModel>().addResult(getAgencies().get(0)))
            .when(adminManagementClient)
            .getAgencyById(any());

        given()
            .contentType(ContentType.JSON)
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get(AGENCY_URI + "/id")
            .then()
            .statusCode(Status.OK.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .and()
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when()
            .get(AGENCY_URI + "/id")
            .then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .when()
            .get(AGENCY_URI + "/id")
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        doThrow(new ReferentialNotFoundException("Agency not found")).when(adminManagementClient).getAgencyById(any());
        given()
            .contentType(ContentType.JSON)
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get(AGENCY_URI + "/id")
            .then()
            .statusCode(Status.NOT_FOUND.getStatusCode());

        doThrow(new AdminManagementClientServerException("Exception")).when(adminManagementClient).getAgencyById(any());
        given()
            .contentType(ContentType.JSON)
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get(AGENCY_URI + "/id")
            .then()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());

        doThrow(new InvalidParseOperationException("Exception")).when(adminManagementClient).getAgencyById(any());
        given()
            .contentType(ContentType.JSON)
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get(AGENCY_URI + "/id")
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void testDownloadTraceabilityOperationFile() throws Exception {
        Response response = mock(Response.class);
        when(response.readEntity(InputStream.class)).thenReturn(StreamUtils.toInputStream("test"));
        when(response.getStatus()).thenReturn(Status.OK.getStatusCode());
        when(response.getStatusInfo()).thenReturn(Status.OK);
        when(response.getMediaType()).thenReturn(MediaType.APPLICATION_OCTET_STREAM_TYPE);

        when(accessInternalClient.downloadTraceabilityFile(anyString())).thenReturn(response);

        given()
            .accept(MediaType.APPLICATION_OCTET_STREAM)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get(TRACEABILITY_OPERATION_BASE_URI + TRACEABILITY_OPERATION_ID + "/datafiles")
            .then()
            .statusCode(Status.OK.getStatusCode());
    }

    private List<Object> getIngestContracts() throws FileNotFoundException, InvalidParseOperationException {
        InputStream fileContracts = PropertiesUtils.getResourceAsStream("referential_contracts_ok.json");
        ArrayNode array = (ArrayNode) JsonHandler.getFromInputStream(fileContracts);
        List<Object> res = new ArrayList<>();
        array.forEach(e -> res.add(e));
        return res;
    }

    private List<Object> getContexts() throws FileNotFoundException, InvalidParseOperationException {
        InputStream fileContexts = PropertiesUtils.getResourceAsStream("context.json");
        ArrayNode array = (ArrayNode) JsonHandler.getFromInputStream(fileContexts);
        List<Object> res = new ArrayList<>();
        array.forEach(e -> res.add(e));
        return res;
    }

    private List<Object> getAccessContracts() throws FileNotFoundException, InvalidParseOperationException {
        InputStream fileContracts = PropertiesUtils.getResourceAsStream("contracts_access_ok.json");
        ArrayNode array = (ArrayNode) JsonHandler.getFromInputStream(fileContracts);
        List<Object> res = new ArrayList<>();
        array.forEach(res::add);
        return res;
    }

    private List<AgenciesModel> getAgencies() {
        List<AgenciesModel> res = new ArrayList<>();
        IntStream.range(1, 5).forEach(i -> {
            AgenciesModel agenciesModel = new AgenciesModel();
            agenciesModel.setIdentifier("AG-00000" + i);
            agenciesModel.setName("aName" + i);
            agenciesModel.setDescription("aDescription" + i);
            res.add(agenciesModel);
        });

        return res;
    }

    @Test
    public void listResourceEndpoints() throws Exception {
        given()
            .accept(MediaType.APPLICATION_JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .options("/")
            .then()
            .statusCode(Status.OK.getStatusCode())
            .body(CoreMatchers.containsString("formatsfile:check"));
    }

    @Test
    public void insertSecurityProfile() throws Exception {
        File securityProfileFile = PropertiesUtils.getResourceFile("security_profile_ok.json");
        JsonNode json = JsonHandler.getFromFile(securityProfileFile);
        AdminManagementClientMock adminManagementClientMock = new AdminManagementClientMock();
        when(adminManagementClient.importSecurityProfiles(any())).thenReturn(
            adminManagementClientMock.importSecurityProfiles(Lists.newArrayList())
        );
        // Test OK
        given()
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .body(json)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(SECURITY_PROFILES_URI)
            .then()
            .statusCode(Status.CREATED.getStatusCode());

        // Test unknown tenant Id
        given()
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .body(json)
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when()
            .post(SECURITY_PROFILES_URI)
            .then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

        // Missing tenant Id
        given()
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .body(json)
            .when()
            .post(SECURITY_PROFILES_URI)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void testFindSecurityProfiles() throws Exception {
        final Select select = new Select();
        String securityProfileIdentifier = "SEC_PROFILE-00001";
        select.setQuery(eq("Identifier", securityProfileIdentifier));
        AdminManagementClientMock adminManagementClientMock = new AdminManagementClientMock();
        when(adminManagementClient.findSecurityProfiles(any())).thenReturn(
            adminManagementClientMock.findSecurityProfiles(JsonHandler.createObjectNode())
        );

        // Test OK with GET
        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get(SECURITY_PROFILES_URI)
            .then()
            .statusCode(Status.OK.getStatusCode());

        // Test OK with POST and X-HTTP-Method-Override
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(SECURITY_PROFILES_URI)
            .then()
            .statusCode(Status.OK.getStatusCode());

        // Test empty query
        final Select emptySelect = new Select();
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(emptySelect.getFinalSelect())
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get(SECURITY_PROFILES_URI)
            .then()
            .statusCode(Status.OK.getStatusCode());

        // Test multi query
        final SelectMultiQuery selectMulti = new SelectMultiQuery();
        selectMulti.setQuery(eq("Identifier", securityProfileIdentifier));
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(selectMulti.getFinalSelect())
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get(SECURITY_PROFILES_URI)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode())
            .body(CoreMatchers.containsString(CODE_VALIDATION_DSL));

        // Test no query
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get(SECURITY_PROFILES_URI)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        // Test unknown tenant Id
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .and()
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when()
            .get(SECURITY_PROFILES_URI)
            .then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

        // Missing tenant Id
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .when()
            .get(SECURITY_PROFILES_URI)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void testFindSecurityProfilesByIdentifier() throws Exception {
        String securityProfileIdentifier = "SEC_PROFILE-00001";

        AdminManagementClientMock adminManagementClientMock = new AdminManagementClientMock();
        when(adminManagementClient.findSecurityProfileByIdentifier(anyString())).thenReturn(
            adminManagementClientMock.findSecurityProfileByIdentifier(securityProfileIdentifier)
        );

        // Test OK
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get(SECURITY_PROFILES_URI + "/" + securityProfileIdentifier)
            .then()
            .statusCode(Status.OK.getStatusCode());

        // Test unknown tenant Id
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .and()
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when()
            .get(SECURITY_PROFILES_URI + "/" + securityProfileIdentifier)
            .then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

        // Missing tenant Id
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .when()
            .get(SECURITY_PROFILES_URI + "/" + securityProfileIdentifier)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void testUpdateSecurityProfiles() throws Exception {
        // Add permission
        String NewPermission = "new_permission:read";

        final UpdateParserSingle updateParser = new UpdateParserSingle(new SingleVarNameAdapter());
        final Update update = new Update();
        update.setQuery(QueryHelper.eq("Name", "SEC_PROFILE_1"));
        final SetAction setActionPermission = UpdateActionHelper.set("Permissions", NewPermission);
        update.addActions(setActionPermission);
        updateParser.parse(update.getFinalUpdate());

        String securityProfileIdentifier = "SEC_PROFILE-00001";

        when(adminManagementClient.updateSecurityProfile(anyString(), any())).thenReturn(
            new AdminManagementClientMock().updateSecurityProfile("id", JsonHandler.createObjectNode())
        );
        // valid query
        given()
            .contentType(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(update.getFinalUpdateById())
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .put(SECURITY_PROFILES_URI + "/" + GOOD_ID)
            .then()
            .statusCode(Status.OK.getStatusCode());

        // wrong query
        given()
            .contentType(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(update.getFinalUpdate())
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .put(SECURITY_PROFILES_URI + "/" + GOOD_ID)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());

        // no query
        given()
            .contentType(ContentType.JSON)
            .contentType(ContentType.JSON)
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .put(SECURITY_PROFILES_URI + "/" + GOOD_ID)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        // Test unknown tenant Id
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(update.getFinalUpdate())
            .and()
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when()
            .put(SECURITY_PROFILES_URI + "/" + securityProfileIdentifier)
            .then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

        // Missing tenant Id
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(update.getFinalUpdate())
            .when()
            .put(SECURITY_PROFILES_URI + "/" + securityProfileIdentifier)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void testUpdateProfile() throws Exception {
        String NewPermission = "new_permission:update:json";

        final Update update = new Update();
        update.setQuery(QueryHelper.eq("Name", "aName"));
        final SetAction setActionAddPermission = UpdateActionHelper.set("Permissions", NewPermission);
        update.addActions(setActionAddPermission);

        AdminManagementClientMock adminManagementClientMock = new AdminManagementClientMock();
        when(adminManagementClient.updateProfile(anyString(), any())).thenReturn(
            adminManagementClientMock.updateProfile("id", JsonHandler.createObjectNode())
        );

        // valid query
        given()
            .contentType(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(update.getFinalUpdateById())
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .put(PROFILE_URI + "/" + GOOD_ID)
            .then()
            .statusCode(Status.OK.getStatusCode());

        // wrong query
        given()
            .contentType(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(update.getFinalUpdate())
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .put(PROFILE_URI + "/" + GOOD_ID)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());

        // no query
        given()
            .contentType(ContentType.JSON)
            .contentType(ContentType.JSON)
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .put(PROFILE_URI + "/" + GOOD_ID)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        // no tenant
        given()
            .contentType(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(update.getFinalUpdateById())
            .when()
            .put(PROFILE_URI + "/" + GOOD_ID)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void testUpdateContext() throws Exception {
        final Update update = new Update();
        update.setQuery(QueryHelper.eq("Identifier", "CT-000001"));
        final SetAction setActionDescription = UpdateActionHelper.set("Name", "admin-context");
        update.addActions(setActionDescription);

        when(adminManagementClient.updateContext(anyString(), any())).thenReturn(
            new AdminManagementClientMock().updateContext("id", JsonHandler.createObjectNode())
        );

        // valid query
        given()
            .contentType(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(update.getFinalUpdateById())
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .put(CONTEXT_URI + "/" + GOOD_ID)
            .then()
            .statusCode(Status.OK.getStatusCode());

        // wrong query
        given()
            .contentType(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(update.getFinalUpdate())
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .put(CONTEXT_URI + "/" + GOOD_ID)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());

        // no query
        given()
            .contentType(ContentType.JSON)
            .contentType(ContentType.JSON)
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .put(CONTEXT_URI + "/" + GOOD_ID)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        // no tenant
        given()
            .contentType(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(update.getFinalUpdateById())
            .when()
            .put(CONTEXT_URI + "/" + GOOD_ID)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void testUpdateAccessContract() throws Exception {
        final Update update = new Update();
        update.setQuery(QueryHelper.eq("Identifier", "CT-000001"));
        final SetAction setActionDescription = UpdateActionHelper.set("Name", "admin-context");
        update.addActions(setActionDescription);

        AdminManagementClientMock adminManagementClientMock = new AdminManagementClientMock();
        when(adminManagementClient.updateAccessContract(anyString(), any())).thenReturn(
            adminManagementClientMock.updateAccessContract("id", JsonHandler.createObjectNode())
        );

        // valid query
        given()
            .contentType(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(update.getFinalUpdateById())
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .put(AccessExtAPI.ACCESS_CONTRACT_API_UPDATE + "/" + GOOD_ID)
            .then()
            .statusCode(Status.OK.getStatusCode());

        // wrong query
        given()
            .contentType(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(update.getFinalUpdate())
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .put(AccessExtAPI.ACCESS_CONTRACT_API_UPDATE + "/" + GOOD_ID)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());

        // no query
        given()
            .contentType(ContentType.JSON)
            .contentType(ContentType.JSON)
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .put(AccessExtAPI.ACCESS_CONTRACT_API_UPDATE + "/" + GOOD_ID)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        // no tenant
        given()
            .contentType(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(update.getFinalUpdateById())
            .when()
            .put(AccessExtAPI.ACCESS_CONTRACT_API_UPDATE + "/" + GOOD_ID)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void testUpdateIngestContract() throws Exception {
        final Update update = new Update();
        update.setQuery(QueryHelper.eq("Identifier", "CT-000001"));
        final SetAction setActionDescription = UpdateActionHelper.set("Name", "admin-context");
        update.addActions(setActionDescription);

        AdminManagementClientMock adminManagementClientMock = new AdminManagementClientMock();
        when(adminManagementClient.updateIngestContract(anyString(), any())).thenReturn(
            adminManagementClientMock.updateIngestContract("id", JsonHandler.createObjectNode())
        );

        // valid query
        given()
            .contentType(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(update.getFinalUpdateById())
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .put(AccessExtAPI.INGEST_CONTRACT_API_UPDATE + "/" + GOOD_ID)
            .then()
            .statusCode(Status.OK.getStatusCode());

        // wrong query
        given()
            .contentType(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(update.getFinalUpdate())
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .put(AccessExtAPI.INGEST_CONTRACT_API_UPDATE + "/" + GOOD_ID)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());

        // no query
        given()
            .contentType(ContentType.JSON)
            .contentType(ContentType.JSON)
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .put(AccessExtAPI.INGEST_CONTRACT_API_UPDATE + "/" + GOOD_ID)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        // no tenant
        given()
            .contentType(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(update.getFinalUpdateById())
            .when()
            .put(AccessExtAPI.INGEST_CONTRACT_API_UPDATE + "/" + GOOD_ID)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void listOperations() throws Exception {
        RequestResponseOK requestResponseOK = new RequestResponseOK();
        requestResponseOK.setHttpCode(Status.OK.getStatusCode());
        when(ingestInternalClient.listOperationsDetails(any())).thenReturn(requestResponseOK);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(new ProcessQuery())
            .when()
            .get("operations")
            .then()
            .statusCode(Status.OK.getStatusCode());
    }

    @Test
    public void cancelOperationTest() throws Exception {
        ItemStatus result = new ItemStatus();
        result.setGlobalState(ProcessState.COMPLETED);
        result.increment(StatusCode.FATAL);
        result.setItemId("Itzm");

        RequestResponseOK<ItemStatus> responseOK = new RequestResponseOK<ItemStatus>().addResult(result);
        responseOK.setHttpCode(Status.ACCEPTED.getStatusCode());

        when(ingestInternalClient.cancelOperationProcessExecution(any())).thenReturn(responseOK);
        given()
            .accept(MediaType.APPLICATION_JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .delete("operations/1")
            .then()
            .statusCode(Status.ACCEPTED.getStatusCode());
    }

    @Test
    public void getWorkFlowExecutionStatusTest() throws Exception {
        when(ingestInternalClient.getOperationProcessStatus(anyString())).thenReturn(
            new ItemStatus().setGlobalState(ProcessState.RUNNING)
        );

        given()
            .accept(MediaType.APPLICATION_JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .head("operations/1")
            .then()
            .statusCode(Status.ACCEPTED.getStatusCode());
    }

    @Test
    public void getWorkFlowStatusTest() throws Exception {
        when(ingestInternalClient.getOperationProcessExecutionDetails(anyString())).thenReturn(
            new RequestResponseOK<ItemStatus>().addResult(new ItemStatus()).setHttpCode(Status.OK.getStatusCode())
        );
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get("operations/1")
            .then()
            .statusCode(Status.OK.getStatusCode());
    }

    @Test
    public void getWorkFlowNotFoundTest() throws Exception {
        VitamError vitamError = new VitamError("fake")
            .setHttpCode(Status.NOT_FOUND.getStatusCode())
            .setContext("Fake")
            .setState("code_vitam")
            .setMessage("msg")
            .setDescription("description");

        when(ingestInternalClient.getOperationProcessExecutionDetails(anyString())).thenReturn(vitamError);
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(new ProcessQuery())
            .when()
            .get("operations/1")
            .then()
            .statusCode(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void getWorkflowDefinitionsTest() throws Exception {
        RequestResponseOK requestResponseOK = new RequestResponseOK();
        requestResponseOK.setHttpCode(Status.OK.getStatusCode());
        when(ingestInternalClient.getWorkflowDefinitions()).thenReturn(requestResponseOK);

        given()
            .accept(MediaType.APPLICATION_JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get("workflows/")
            .then()
            .statusCode(Status.OK.getStatusCode());
    }

    @Test
    public void updateWorkFlowStatusTest() throws Exception {
        RequestResponseOK<ItemStatus> objectRequestResponseOK = new RequestResponseOK<>();
        objectRequestResponseOK.addResult(new ItemStatus().setGlobalState(ProcessState.PAUSE));
        objectRequestResponseOK.setHttpCode(Status.OK.getStatusCode());
        when(ingestInternalClient.updateOperationActionProcess(anyString(), anyString())).thenReturn(
            objectRequestResponseOK
        );

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_ACTION, ProcessAction.PAUSE.getValue())
            .body(new ProcessQuery())
            .when()
            .put("operations/1")
            .then()
            .statusCode(Status.OK.getStatusCode());
    }

    @Test
    public void updateWorkFlowStatusWithoutHeadersTest() {
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(new ProcessQuery())
            .when()
            .put("operations/1")
            .then()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void testimportValidContextFileReturnCreated() throws Exception {
        doReturn(Status.CREATED).when(adminManagementClient).importContexts(any());
        File contextFile = PropertiesUtils.getResourceFile("context.json");
        JsonNode json = JsonHandler.getFromFile(contextFile);
        given()
            .contentType(ContentType.JSON)
            .body(json)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(AccessExtAPI.CONTEXTS_API)
            .then()
            .statusCode(Status.CREATED.getStatusCode());
    }

    @Test
    public void testfindContextFile() throws Exception {
        doReturn(new RequestResponseOK<>().addAllResults(getContexts()))
            .when(adminManagementClient)
            .findContexts(any());

        final Select select = new Select();
        select.setQuery(eq("STATUS", true));
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and()
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(AccessExtAPI.CONTEXTS_API)
            .then()
            .statusCode(Status.OK.getStatusCode());

        final Select emptyQuerySelect = new Select();
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(emptyQuerySelect.getFinalSelect())
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and()
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(AccessExtAPI.CONTEXTS_API)
            .then()
            .statusCode(Status.OK.getStatusCode());

        final SelectMultiQuery selectMultiple = new SelectMultiQuery();
        selectMultiple.setQuery(eq("STATUS", true));
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(selectMultiple.getFinalSelect())
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and()
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(AccessExtAPI.CONTEXTS_API)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode())
            .body(CoreMatchers.containsString(CODE_VALIDATION_DSL));

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and()
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(AccessExtAPI.CONTEXTS_API)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and()
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when()
            .post(AccessExtAPI.CONTEXTS_API)
            .then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .when()
            .post(AccessExtAPI.CONTEXTS_API)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void testImportContextsWithInvalidFileBadRequest() throws Exception {
        doReturn(Status.BAD_REQUEST).when(adminManagementClient).importContexts(any());

        given()
            .contentType(ContentType.JSON)
            .body(JsonHandler.createObjectNode())
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(AccessExtAPI.CONTEXTS_API)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode())
            .contentType("application/json");

        doThrow(new ReferentialException("ReferentialException")).when(adminManagementClient).importContexts(any());

        given()
            .contentType(ContentType.JSON)
            .body(JsonHandler.createObjectNode())
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(AccessExtAPI.CONTEXTS_API)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode())
            .contentType("application/json");
    }

    @Test
    public void downloadIngestReportsAsStream() throws Exception {
        Response response = mock(Response.class);
        when(response.readEntity(InputStream.class)).thenReturn(StreamUtils.toInputStream("test"));
        when(response.getStatus()).thenReturn(Status.OK.getStatusCode());
        when(response.getStatusInfo()).thenReturn(Status.OK);
        when(response.getMediaType()).thenReturn(MediaType.APPLICATION_OCTET_STREAM_TYPE);

        when(ingestInternalClient.downloadObjectAsync(anyString(), any())).thenReturn(response);
        given()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_CONTEXT_ID, Contexts.DEFAULT_WORKFLOW)
            .when()
            .get(AccessExtAPI.RULES_REPORT_API + "/id")
            .then()
            .statusCode(Status.OK.getStatusCode());
    }

    @Test
    public void testCreateArchiveUnitProfileWithInvalidFileBadRequest() throws Exception {
        doReturn(new VitamError("").setHttpCode(Status.BAD_REQUEST.getStatusCode()))
            .when(adminManagementClient)
            .createArchiveUnitProfiles(any());

        File fileArchiveUnitProfiles = PropertiesUtils.getResourceFile("AUP_missing_identifier.json");
        JsonNode json = JsonHandler.getFromFile(fileArchiveUnitProfiles);

        given()
            .contentType(ContentType.JSON)
            .body(json)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(ARCHIVE_UNIT_PROFILE_URI)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode())
            .contentType("application/json");
    }

    @Test
    public void testCreateValidArchiveUnitProfileReturnCreated() throws Exception {
        doReturn(new RequestResponseOK<>().setHttpCode(Status.CREATED.getStatusCode()))
            .when(adminManagementClient)
            .createArchiveUnitProfiles(any());

        File fileArchiveUnitProfiles = PropertiesUtils.getResourceFile("archive_unit_profiles_ok.json");
        JsonNode json = JsonHandler.getFromFile(fileArchiveUnitProfiles);

        given()
            .contentType(ContentType.JSON)
            .body(json)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(ARCHIVE_UNIT_PROFILE_URI)
            .then()
            .statusCode(Status.CREATED.getStatusCode())
            .contentType("application/json");
    }

    @Test
    public void testFindArchiveUnitProfiles() throws Exception {
        doReturn(new RequestResponseOK<>().addAllResults(getAccessContracts()))
            .when(adminManagementClient)
            .findArchiveUnitProfiles(any());

        final Select select = new Select();
        select.setQuery(eq("Status", "ACTIVE"));
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(ARCHIVE_UNIT_PROFILE_URI)
            .then()
            .statusCode(Status.OK.getStatusCode());

        final Select emptyQuerySelect = new Select();
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(emptyQuerySelect.getFinalSelect())
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(ARCHIVE_UNIT_PROFILE_URI)
            .then()
            .statusCode(Status.OK.getStatusCode());

        final SelectMultiQuery selectMultiple = new SelectMultiQuery();
        selectMultiple.setQuery(eq("Status", "ACTIVE"));
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(selectMultiple.getFinalSelect())
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(ARCHIVE_UNIT_PROFILE_URI)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode())
            .body(CoreMatchers.containsString(CODE_VALIDATION_DSL));

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(ARCHIVE_UNIT_PROFILE_URI)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and()
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when()
            .post(ARCHIVE_UNIT_PROFILE_URI)
            .then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .when()
            .post(ARCHIVE_UNIT_PROFILE_URI)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void testUpdateArchiveUnitProfile() throws Exception {
        String NewPermission = "new_permission:update:json";

        final Update update = new Update();
        update.setQuery(QueryHelper.eq("Name", "aName"));
        final SetAction setActionAddPermission = UpdateActionHelper.set("Permissions", NewPermission);
        update.addActions(setActionAddPermission);

        AdminManagementClientMock adminManagementClientMock = new AdminManagementClientMock();
        when(adminManagementClient.updateArchiveUnitProfile(anyString(), any())).thenReturn(
            adminManagementClientMock.updateArchiveUnitProfile("id", JsonHandler.createObjectNode())
        );
        // valid query
        given()
            .contentType(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(update.getFinalUpdateById())
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .put(ARCHIVE_UNIT_PROFILE_URI + "/" + GOOD_ID)
            .then()
            .statusCode(Status.OK.getStatusCode());

        // wrong query
        given()
            .contentType(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(update.getFinalUpdate())
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .put(ARCHIVE_UNIT_PROFILE_URI + "/" + GOOD_ID)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());

        // no query
        given()
            .contentType(ContentType.JSON)
            .contentType(ContentType.JSON)
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .put(ARCHIVE_UNIT_PROFILE_URI + "/" + GOOD_ID)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        // no tenant
        given()
            .contentType(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(update.getFinalUpdateById())
            .when()
            .put(ARCHIVE_UNIT_PROFILE_URI + "/" + GOOD_ID)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void should_respond_no_content_when_status() {
        when(vitamStatusService.getResourcesStatus()).thenReturn(true);
        given().accept(ContentType.JSON).when().get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());
    }

    @Test
    public void testImportValidOntologyReturnCreated() throws Exception {
        doReturn(new RequestResponseOK<>().setHttpCode(Status.CREATED.getStatusCode()))
            .when(adminManagementClient)
            .importOntologies(anyBoolean(), any());

        File fileOntologies = PropertiesUtils.getResourceFile("ontologies_ok.json");
        JsonNode json = JsonHandler.getFromFile(fileOntologies);

        given()
            .contentType(ContentType.JSON)
            .body(json)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(ONTOLOGIES_URI)
            .then()
            .statusCode(Status.CREATED.getStatusCode())
            .contentType("application/json");
    }

    @Test
    public void testImportOntologyWithInvalidFileBadRequest() throws Exception {
        doReturn(new VitamError("").setHttpCode(Status.BAD_REQUEST.getStatusCode()))
            .when(adminManagementClient)
            .importOntologies(anyBoolean(), any());

        File fileOntologies = PropertiesUtils.getResourceFile("ontologies_missing_identifier.json");
        JsonNode json = JsonHandler.getFromFile(fileOntologies);

        given()
            .contentType(ContentType.JSON)
            .body(json)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(ONTOLOGIES_URI)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode())
            .contentType("application/json");
    }

    @Test
    public void testImportOntologyWithUknownVocabularTypeThenBadRequest() throws Exception {
        String result =
            "{\"httpCode\":0,\"code\":\"020135\",\"context\":\"ADMIN_EXTERNAL\",\"state\":\"KO\",\"message\":\"Access external client error. JSON is invalid\",\"description\":\"Access external client error. JSON is invalid\"}";
        JsonNode resultNode = JsonHandler.getFromString(result);
        // Then
        doReturn(new VitamError("").setHttpCode(Status.BAD_REQUEST.getStatusCode()))
            .when(adminManagementClient)
            .importOntologies(anyBoolean(), any());

        File ontologyFile = PropertiesUtils.getResourceFile("ko_ontology_vocabular_type_unknown.json");
        JsonNode json = JsonHandler.getFromFile(ontologyFile);

        given()
            .contentType(ContentType.JSON)
            .body(json)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(ONTOLOGIES_URI)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode())
            .contentType("application/json")
            .body(CoreMatchers.containsString(resultNode.toString()));
    }

    @Test
    public void testCreateExternalOperation() throws Exception {
        when(adminManagementClient.createExternalOperation(any())).thenReturn(Status.CREATED);

        LogbookOperationParameters operation = fillLogbookParameters(
            GUIDFactory.newOperationLogbookGUID(0).getId(),
            GUIDFactory.newOperationLogbookGUID(0).getId()
        );

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(operation)
            .when()
            .post(OPERATIONS_URI)
            .then()
            .statusCode(Status.CREATED.getStatusCode());

        when(adminManagementClient.createExternalOperation(any())).thenReturn(Status.BAD_REQUEST);

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(operation)
            .when()
            .post(OPERATIONS_URI)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());

        when(adminManagementClient.createExternalOperation(any())).thenReturn(Status.INTERNAL_SERVER_ERROR);

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(operation)
            .when()
            .post(OPERATIONS_URI)
            .then()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());

        File logbookFile = PropertiesUtils.getResourceFile("external_logbook.json");
        ObjectNode operationTooBig = (ObjectNode) JsonHandler.getFromFile(logbookFile);
        // lets put maximum size for an external logbook equals to 10ko
        VitamConfiguration.setOperationMaxSizeForExternal(10240);

        LogbookOperationParameters params = JsonHandler.getFromJsonNode(
            operationTooBig,
            LogbookOperationParameters.class
        );
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .and()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(params)
            .when()
            .post(OPERATIONS_URI)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    private LogbookOperationParameters fillLogbookParameters(String guid, String evIdProc) {
        LogbookOperationParameters logbookParamaters = LogbookParameterHelper.newLogbookOperationParameters();
        logbookParamaters.putParameterValue(LogbookParameterName.eventIdentifier, guid);
        logbookParamaters.putParameterValue(LogbookParameterName.eventType, LogbookParameterName.eventType.name());
        logbookParamaters.putParameterValue(LogbookParameterName.eventDateTime, LocalDateUtil.now().toString());
        logbookParamaters.putParameterValue(
            LogbookParameterName.eventIdentifierProcess,
            evIdProc != null ? evIdProc : guid
        );
        logbookParamaters.putParameterValue(
            LogbookParameterName.eventTypeProcess,
            LogbookParameterName.eventTypeProcess.name()
        );
        logbookParamaters.putParameterValue(LogbookParameterName.outcome, LogbookParameterName.outcome.name());
        logbookParamaters.putParameterValue(
            LogbookParameterName.outcomeDetail,
            LogbookParameterName.outcomeDetail.name()
        );
        logbookParamaters.putParameterValue(
            LogbookParameterName.outcomeDetailMessage,
            LogbookParameterName.outcomeDetailMessage.name()
        );
        logbookParamaters.putParameterValue(
            LogbookParameterName.agentIdentifier,
            LogbookParameterName.agentIdentifier.name()
        );
        logbookParamaters.putParameterValue(
            LogbookParameterName.agentIdentifierApplicationSession,
            LogbookParameterName.agentIdentifierApplicationSession.name()
        );
        logbookParamaters.putParameterValue(
            LogbookParameterName.eventIdentifierRequest,
            LogbookParameterName.eventIdentifierRequest.name()
        );
        logbookParamaters.putParameterValue(LogbookParameterName.agIdExt, null);

        logbookParamaters.putParameterValue(
            LogbookParameterName.objectIdentifier,
            LogbookParameterName.objectIdentifier.name()
        );
        logbookParamaters.putParameterValue(
            LogbookParameterName.objectIdentifierRequest,
            LogbookParameterName.objectIdentifierRequest.name()
        );
        logbookParamaters.putParameterValue(
            LogbookParameterName.objectIdentifierIncome,
            LogbookParameterName.objectIdentifierIncome.name()
        );

        return logbookParamaters;
    }
}

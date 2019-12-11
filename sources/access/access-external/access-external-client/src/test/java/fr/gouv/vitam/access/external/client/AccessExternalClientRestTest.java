/*
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
 */
package fr.gouv.vitam.access.external.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import fr.gouv.vitam.access.external.api.AccessExtAPI;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.external.client.ClientMockResultHelper;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.elimination.EliminationRequestBody;
import fr.gouv.vitam.common.server.application.junit.ResteasyTestApplication;
import fr.gouv.vitam.common.serverv2.VitamServerTestRunner;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.Set;

import static fr.gouv.vitam.common.GlobalDataRest.X_HTTP_METHOD_OVERRIDE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AccessExternalClientRestTest extends ResteasyTestApplication {
    public static final String BLANK_QUERY = "selectQuery cannot be null.";
    public static final String BLANK_DSL = "dslRequest cannot be null.";
    private static final String QUERY_DSL = "{ $query : [ { $eq : { 'title' : 'test' } } ], " +
        " $filter : { $orderby : '#id' }," +
        " $projection : {$fields : {#id : 1, title:2, transacdate:1}}" +
        " }";
    private final static ExpectedResults mock = mock(ExpectedResults.class);
    private static final String NOT_FOUND = "Error with the response, get status: '404' and reason 'Not Found'.";
    private static final String INTERNAL_SERVER_ERROR =
        "Error with the response, get status: '500' and reason 'Internal Server Error'.";
    private static final String PRECONDITION_FAILED =
        "Error with the response, get status: '412' and reason 'Precondition Failed'.";
    private static final String BAD_REQUEST = "Error with the response, get status: '400' and reason 'Bad Request'.";
    private static final String UNAUTHORIZED = "Error with the response, get status: '401' and reason 'Unauthorized'.";
    private static final String UNSUPPORTED_MEDIA_TYPE =
        "Error with the response, get status: '415' and reason 'Unsupported Media Type'.";
    protected static AccessExternalClientRest client;
    static AccessExternalClientFactory factory = AccessExternalClientFactory.getInstance();
    public static VitamServerTestRunner
        vitamServerTestRunner =
        new VitamServerTestRunner(AccessExternalClientRestTest.class, factory);
    final String queryDsql =
        "{ \"$query\" : [ { \"$eq\" : { \"title\" : \"test\" } } ], \"$projection\" : {} }";
    final String ID = "identfier1";
    final int TENANT_ID = 0;
    final String CONTRACT = "contract";
    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @BeforeClass
    public static void init() throws Throwable {
        vitamServerTestRunner.start();
        client = (AccessExternalClientRest) vitamServerTestRunner.getClient();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Throwable {
        vitamServerTestRunner.runAfter();
    }

    @Override
    public Set<Object> getResources() {
        return Sets.newHashSet(new MockResource(mock));
    }

    @Test
    @RunWithCustomExecutor
    public void givenRessourceOKWhenSelectTehnReturnOK()
        throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.OK).build());
        assertThat(client.selectUnits(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
            JsonHandler.getFromString(queryDsql))).isNotNull();
    }

    @Test
    @RunWithCustomExecutor
    public void givenInternalServerError_whenSelect_ThenRaiseAnExeption() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.UNAUTHORIZED).build());
        assertThat(client
            .selectUnits(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), JsonHandler.getFromString(QUERY_DSL))
            .getHttpCode())
            .isEqualTo(Status.UNAUTHORIZED.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void givenRessourceNotFound_whenSelectUnit_ThenRaiseAnException() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        assertThat(client
            .selectUnits(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), JsonHandler.getFromString(QUERY_DSL))
            .getHttpCode())
            .isEqualTo(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void givenBadRequest_whenSelectUnit_ThenRaiseAnException() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        assertThat(client
            .selectUnits(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), JsonHandler.getFromString(queryDsql))
            .getHttpCode())
            .isEqualTo(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @RunWithCustomExecutor
    @Test
    public void givenRequestNull_whenSelectUnit_ThenErrorResponse() {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> client.selectUnits(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), null))
            .withMessage(BLANK_QUERY);
    }

    /****
     *
     * Select Unit By Id
     *
     ***/
    @Test
    @RunWithCustomExecutor
    public void givenInternalServerError_whenSelectById_ThenRaiseAnExeption() {
        when(mock.get()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        final String queryDsql =
            "{ $query : [ { $eq : { 'title' : 'test' } } ], " +
                " $filter : { $orderby : {'#id': 1} }," +
                " $projection : {$fields : {#id : 1, title:2, transacdate:1}}" +
                " }";
        assertThatExceptionOfType(VitamClientException.class)
            .isThrownBy(() -> client.selectUnitbyId(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                createDslQueryById(queryDsql), ID))
            .withMessage(INTERNAL_SERVER_ERROR);
    }

    @Test
    @RunWithCustomExecutor
    public void givenRessourceNotFound_whenSelectUnitById_ThenRaiseAnException() {
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        final String queryDsql =
            "{ \"$query\" : [ { \"$eq\" : { \"title\" : \"test\" } } ], " +
                " \"$filter\" : { \"$orderby\" : {\"#id\": 1}}," +
                " \"$projection\": {\"$fields\" : {\"#id\" : 1, \"title\":2, \"transacdate\":1}}" +
                " }";

        assertThatExceptionOfType(VitamClientException.class)
            .isThrownBy(() -> client.selectUnitbyId(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                createDslQueryById(queryDsql), ID))
            .withMessage(NOT_FOUND);
    }

    @Test
    @RunWithCustomExecutor
    public void givenBadRequest_whenSelectUnitById_ThenRaiseAnException() {
        when(mock.get()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        assertThatExceptionOfType(VitamClientException.class)
            .isThrownBy(() -> client.selectUnitbyId(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                createDslQueryById(queryDsql), ID))
            .withMessage(BAD_REQUEST);
    }

    private JsonNode createDslQueryById(String queryDsl) throws Exception {
        final SelectParserMultiple selectParserMultiple = new SelectParserMultiple();
        selectParserMultiple.parse(JsonHandler.getFromString(queryDsl));
        SelectMultiQuery selectMultiQuery = selectParserMultiple.getRequest();
        return selectMultiQuery.getFinalSelectById();
    }

    @Test
    @RunWithCustomExecutor
    public void givenRequestBlank_whenSelectUnitById_ThenRaiseAnException() {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> client.selectUnitbyId(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                null, "id"));
    }

    @Test
    @RunWithCustomExecutor
    public void givenIDBlank_whenSelectUnitById_ThenRaiseAnException() {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> client.selectUnitbyId(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                createDslQueryById(queryDsql), ""));
    }

    @Test
    @RunWithCustomExecutor
    public void givenIdAndRequestWhenSelectUnitByIdThenOK() {
        when(mock.get()).thenReturn(Response.status(Status.NO_CONTENT).build());
        assertThatCode(() -> client
            .selectUnitbyId(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), createDslQueryById(queryDsql), ID))
            .doesNotThrowAnyException();
    }

    @Test
    @RunWithCustomExecutor
    public void givenBadRequest_whenUpdateUnitById_ThenRaiseAnException() throws Exception {
        when(mock.put()).thenReturn(Response.status(Status.NOT_FOUND).build());
        assertThat(client.updateUnitbyId(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
            JsonHandler.getFromString(queryDsql), ID).getHttpCode())
            .isEqualTo(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void givenRequestBlank_whenUpdateUnitById_ThenRaiseAnException() {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> client.updateUnitbyId(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                JsonHandler.createObjectNode(), ""));
    }

    @Test
    @RunWithCustomExecutor
    public void givenIdBlank_whenUpdateUnitById_ThenRaiseAnException() {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> client.updateUnitbyId(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                JsonHandler.getFromString(queryDsql), ""));
    }

    @Test
    @RunWithCustomExecutor
    public void givenRequestBlankWhenUpdateUnitByIdThenRaiseAnException() throws Exception {
        when(mock.put()).thenReturn(Response.status(Status.UNSUPPORTED_MEDIA_TYPE).build());
        assertThat(client.updateUnitbyId(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
            JsonHandler.createObjectNode(), ID).getHttpCode())
            .isEqualTo(Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void givenBadRequest_whenUpdateUnit_ThenRaiseAnException() throws Exception {
        when(mock.put()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        assertThat(client.updateUnitbyId(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
            JsonHandler.getFromString(queryDsql), ID).getHttpCode())
            .isEqualTo(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void given500_whenUpdateUnit_ThenRaiseAnException() throws Exception {
        when(mock.put()).thenReturn(Response.status(Status.UNAUTHORIZED).build());
        assertThat(client.updateUnitbyId(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
            JsonHandler.getFromString(queryDsql), ID).getHttpCode())
            .isEqualTo(Status.UNAUTHORIZED.getStatusCode());
    }

    @RunWithCustomExecutor
    @Test
    public void givenQueryNullWhenSelectObjectByIdThenRaiseAnInvalidParseOperationException() {
        when(mock.get()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        assertThatExceptionOfType(VitamClientException.class)
            .isThrownBy(
                () -> client.selectObjectMetadatasByUnitId(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                    JsonHandler.createObjectNode(), ID))
            .withMessage(BAD_REQUEST);
    }

    @Test
    @RunWithCustomExecutor
    public void givenQueryCorrectWhenSelectObjectByIdThenRaiseInternalServerError() {
        when(mock.get()).thenReturn(Response.status(Status.UNAUTHORIZED).build());
        assertThatExceptionOfType(VitamClientException.class)
            .isThrownBy(
                () -> client.selectObjectMetadatasByUnitId(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                    JsonHandler.getFromString(queryDsql), ID))
            .withMessage(UNAUTHORIZED);
    }

    @Test
    @RunWithCustomExecutor
    public void givenQueryCorrectWhenSelectObjectByIdThenRaiseBadRequest() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        assertThatExceptionOfType(VitamClientException.class)
            .isThrownBy(
                () -> client.selectObjectMetadatasByUnitId(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                    JsonHandler.getFromString(queryDsql), ID))
            .withMessage(BAD_REQUEST);
    }

    @Test
    @RunWithCustomExecutor
    public void givenQueryCorrectWhenSelectObjectByIdThenRaisePreconditionFailed() {
        when(mock.get()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        assertThatExceptionOfType(VitamClientException.class)
            .isThrownBy(
                () -> client.selectObjectMetadatasByUnitId(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                    JsonHandler.getFromString(queryDsql), ID))
            .withMessage(PRECONDITION_FAILED);
    }

    @Test
    @RunWithCustomExecutor
    public void givenQueryCorrectWhenSelectObjectByIdThenNotFound() {
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        assertThatExceptionOfType(VitamClientException.class)
            .isThrownBy(
                () -> client.selectObjectMetadatasByUnitId(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                    JsonHandler.getFromString(queryDsql), ID))
            .withMessage(NOT_FOUND);
    }

    @Test
    @RunWithCustomExecutor
    public void givenQueryCorrectWhenSelectObjectByIdThenOK() {
        when(mock.get()).thenReturn(Response.status(Status.OK).entity(ClientMockResultHelper.getEmptyResult()).build());
        assertThatCode(
            () -> client.selectObjectMetadatasByUnitId(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                JsonHandler.getFromString(queryDsql), ID))
            .doesNotThrowAnyException();
    }

    /***
     *
     * logbook operations
     *
     ***/
    // TODO migration
    @Test
    @RunWithCustomExecutor
    public void selectLogbookOperations() throws Exception {
        when(mock.get())
            .thenReturn(Response.status(Status.OK).build());
        assertThat(client.selectOperations(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
            JsonHandler.getFromString(queryDsql))).isNotNull();
    }

    @Test
    @RunWithCustomExecutor
    public void givenSelectLogbookNotFoundThenNotFound() {
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        assertThatExceptionOfType(VitamClientException.class)
            .isThrownBy(
                () -> client.selectOperations(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                    JsonHandler.getFromString(queryDsql)))
            .withMessage(NOT_FOUND);
    }

    @Test
    @RunWithCustomExecutor
    public void givenSelectLogbookBadQueryThenPreconditionFailed() {
        when(mock.get()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        assertThatExceptionOfType(VitamClientException.class)
            .isThrownBy(
                () -> client.selectOperations(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                    JsonHandler.getFromString(queryDsql)))
            .withMessage(PRECONDITION_FAILED);
    }

    /***
     *
     * logbook operationById
     *
     ***/
    @Test
    @RunWithCustomExecutor
    public void selectLogbookOperationByID() throws Exception {
        when(mock.get())
            .thenReturn(
                Response.status(Status.OK).build());
        assertThat(client.selectOperationbyId(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), ID,
            JsonHandler.getFromString(queryDsql))).isNotNull();
    }

    @Test
    @RunWithCustomExecutor
    public void givenSelectLogbookOperationByIDNotFoundThenNotFound() {
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        assertThatExceptionOfType(VitamClientException.class)
            .isThrownBy(
                () -> client.selectOperationbyId(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), ID,
                    JsonHandler.getFromString(queryDsql)))
            .withMessage(NOT_FOUND);
    }

    @Test
    @RunWithCustomExecutor
    public void givenSelectLogbookOperationByIdNoQueryThen415() {
        when(mock.get()).thenReturn(Response.status(Status.UNSUPPORTED_MEDIA_TYPE).build());
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(
                () -> client.selectOperationbyId(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), ID, null))
            .withMessage(BLANK_QUERY);
    }

    @Test
    @RunWithCustomExecutor
    public void givenSelectLogbookOperationByIDBadQueryThenPreconditionFailed() {
        when(mock.get()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        assertThatExceptionOfType(VitamClientException.class)
            .isThrownBy(
                () -> client.selectOperationbyId(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), ID,
                    JsonHandler.getFromString(queryDsql)))
            .withMessage(PRECONDITION_FAILED);
    }

    /***
     *
     * logbook lifecycle units
     *
     ***/
    @Test
    @RunWithCustomExecutor
    public void selectLogbookLifeCyclesUnitById() throws Exception {
        when(mock.get())
            .thenReturn(
                Response.status(Status.OK).entity(ClientMockResultHelper.getLogbookOperationRequestResponse()).build());
        assertThat(client.selectUnitLifeCycleById(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), ID,
            JsonHandler.getFromString(queryDsql))).isNotNull();
    }

    @Test
    @RunWithCustomExecutor
    public void givenSelectLogbookLifeCyclesUnitByIdNotFoundThenNotFound() {
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        assertThatExceptionOfType(VitamClientException.class)
            .isThrownBy(
                () -> client.selectUnitLifeCycleById(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), ID,
                    JsonHandler.getFromString(queryDsql)))
            .withMessage(NOT_FOUND);
    }

    @Test
    @RunWithCustomExecutor
    public void givenSelectLogbookLifeCyclesUnitByIdNoQueryThen415() {
        when(mock.get()).thenReturn(Response.status(Status.UNSUPPORTED_MEDIA_TYPE).build());
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(
                () -> client.selectUnitLifeCycleById(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), ID, null))
            .withMessage(BLANK_QUERY);
    }

    @Test
    @RunWithCustomExecutor
    public void givenSelectLogbookLifeCyclesUnitByIdBadQueryThenPreconditionFailed() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        assertThatExceptionOfType(VitamClientException.class)
            .isThrownBy(
                () -> client.selectUnitLifeCycleById(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), ID,
                    JsonHandler.getFromString(queryDsql)))
            .withMessage(PRECONDITION_FAILED);
    }

    /***
     *
     * logbook lifecycle object
     *
     ***/
    @Test
    @RunWithCustomExecutor
    public void selectLogbookLifeCyclesObjectById() throws Exception {
        when(mock.get())
            .thenReturn(
                Response.status(Status.OK).entity(ClientMockResultHelper.getLogbookOperationRequestResponse()).build());
        assertThat(client.selectObjectGroupLifeCycleById(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), ID,
            JsonHandler.getFromString(queryDsql))).isNotNull();
    }

    @Test
    @RunWithCustomExecutor
    public void givenSelectLogbookLifeCyclesObjectsNotFoundThenNotFound() {
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        assertThatExceptionOfType(VitamClientException.class)
            .isThrownBy(
                () -> client.selectObjectGroupLifeCycleById(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), ID,
                    JsonHandler.getFromString(queryDsql)))
            .withMessage(NOT_FOUND);
    }

    @Test
    @RunWithCustomExecutor
    public void givenSelectLogbookLifeCyclesObjectsByIdNoQueryThen415() {
        when(mock.get()).thenReturn(Response.status(Status.UNSUPPORTED_MEDIA_TYPE).build());
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(
                () -> client
                    .selectObjectGroupLifeCycleById(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), ID, null))
            .withMessage(BLANK_QUERY);
    }

    @Test
    @RunWithCustomExecutor
    public void givenSelectLogbookLifeCyclesObjectBadQueryThenPreconditionFailed() {
        when(mock.get()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        assertThatExceptionOfType(VitamClientException.class)
            .isThrownBy(
                () -> client.selectObjectGroupLifeCycleById(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), ID,
                    JsonHandler.getFromString(queryDsql)))
            .withMessage(PRECONDITION_FAILED);
    }

    /***
     *
     * DIP export
     *
     ***/
    @Test
    @RunWithCustomExecutor
    public void exportDIP() throws Exception {
        when(mock.post())
            .thenReturn(
                Response.status(Status.OK).entity(ClientMockResultHelper.getLogbookOperationRequestResponse()).build());
        assertThat(client.exportDIP(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
            JsonHandler.getFromString(queryDsql))).isNotNull();
    }

    @Test
    @RunWithCustomExecutor
    public void givenExportDIPNotFoundThenNotFound() {
        when(mock.post()).thenReturn(Response.status(Status.NOT_FOUND).build());
        assertThatExceptionOfType(VitamClientException.class)
            .isThrownBy(
                () -> client
                    .exportDIP(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                        JsonHandler.getFromString(queryDsql)))
            .withMessage(NOT_FOUND);
    }

    @Test
    @RunWithCustomExecutor
    public void givenExportDIPNoQueryThen415() {
        when(mock.post()).thenReturn(Response.status(Status.UNSUPPORTED_MEDIA_TYPE).build());
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(
                () -> client.exportDIP(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), null))
            .withMessage(BLANK_DSL);
    }

    @Test
    @RunWithCustomExecutor
    public void givenExportDIPBadQueryThenPreconditionFailed() {
        when(mock.post()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        assertThatExceptionOfType(VitamClientException.class)
            .isThrownBy(
                () -> client
                    .exportDIP(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                        JsonHandler.getFromString(queryDsql)))
            .withMessage(PRECONDITION_FAILED);
    }

    @Test
    @RunWithCustomExecutor
    public void givenBadRequestWhenSelectObjectsThenRaiseAnException() {
        when(mock.get()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        assertThatExceptionOfType(VitamClientException.class)
            .isThrownBy(
                () -> client
                    .selectObjects(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                        JsonHandler.getFromString(queryDsql)))
            .withMessage(NOT_FOUND);
    }

    @Test
    @RunWithCustomExecutor
    public void givenRequestNullWhenSelectObjectsThenErrorResponse() {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(
                () -> client.selectObjects(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), null))
            .withMessage(BLANK_QUERY);
    }

    @Test
    @RunWithCustomExecutor
    public void givenRessourceOKWhenSelectUnitsWithInheritedRulesThenReturnOK()
        throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.OK).entity(ClientMockResultHelper.getFormat()).build());
        assertThat(client.selectUnitsWithInheritedRules(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
            JsonHandler.getFromString(queryDsql))).isNotNull();
    }

    @Test
    @RunWithCustomExecutor
    public void givenInternalServerError_whenSelectUnitsWithInheritedRules_ThenRaiseAnExeption() {
        when(mock.get()).thenReturn(Response.status(Status.UNAUTHORIZED).build());
        assertThatExceptionOfType(VitamClientException.class)
            .isThrownBy(
                () -> client
                    .selectUnitsWithInheritedRules(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                        JsonHandler.getFromString(QUERY_DSL)))
            .withMessage(UNAUTHORIZED);
    }

    /*
     * select units with inherited rules
     */

    @Test
    @RunWithCustomExecutor
    public void givenRessourceNotFound_whenSelectUnitsWithInheritedRules_ThenRaiseAnException() {
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        assertThatExceptionOfType(VitamClientException.class)
            .isThrownBy(
                () -> client
                    .selectUnitsWithInheritedRules(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                        JsonHandler.getFromString(QUERY_DSL)))
            .withMessage(NOT_FOUND);
    }

    @Test
    @RunWithCustomExecutor
    public void givenBadRequest_whenSelectUnitsWithInheritedRules_ThenRaiseAnException() {
        when(mock.get()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        assertThatExceptionOfType(VitamClientException.class)
            .isThrownBy(
                () -> client
                    .selectUnitsWithInheritedRules(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                        JsonHandler.getFromString(QUERY_DSL)))
            .withMessage(PRECONDITION_FAILED);
    }

    @Test
    @RunWithCustomExecutor
    public void startEliminationAnalysisWhenSuccessThenReturnVitamResponseOK()
        throws Exception {

        // Given
        RequestResponseOK responseOK = new RequestResponseOK();
        when(mock.post()).thenReturn(Response.status(Status.OK).entity(responseOK).build());

        EliminationRequestBody eliminationRequestBody = new EliminationRequestBody(
            "2000-01-02", JsonHandler.getFromString(queryDsql));

        // When
        RequestResponse<JsonNode> requestResponse = client.startEliminationAnalysis(
            new VitamContext(TENANT_ID).setAccessContract(CONTRACT), eliminationRequestBody);

        // Then
        assertThat(requestResponse.isOk()).isTrue();
    }

    /*
     * Elimination analysis
     */

    @Test
    @RunWithCustomExecutor
    public void startEliminationAnalysisWhenServerErrorThenReturnInternalServerErrorVitamResponse() throws Exception {

        // Given
        when(mock.post()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());

        EliminationRequestBody eliminationRequestBody = new EliminationRequestBody(
            "2000-01-02", JsonHandler.getFromString(queryDsql));

        // When Then
        assertThatExceptionOfType(VitamClientException.class)
            .isThrownBy(
                () -> client.startEliminationAnalysis(
                    new VitamContext(TENANT_ID).setAccessContract(CONTRACT), eliminationRequestBody))
            .withMessage(INTERNAL_SERVER_ERROR);
    }

    @Test
    @RunWithCustomExecutor
    public void startEliminationAnalysisWhenResourceNotFoundThenReturnNotFoundVitamResponse()
        throws Exception {

        // Given
        when(mock.post()).thenReturn(Response.status(Status.NOT_FOUND).build());

        EliminationRequestBody eliminationRequestBody = new EliminationRequestBody(
            "2000-01-02", JsonHandler.getFromString(queryDsql));

        // When Then
        assertThatExceptionOfType(VitamClientException.class)
            .isThrownBy(
                () -> client.startEliminationAnalysis(
                    new VitamContext(TENANT_ID).setAccessContract(CONTRACT), eliminationRequestBody))
            .withMessage(NOT_FOUND);
    }

    @Test
    @RunWithCustomExecutor
    public void startEliminationAnalysisWhenBadRequestThenReturnBadRequestVitamResponse()
        throws Exception {

        // Given
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).build());

        EliminationRequestBody eliminationRequestBody = new EliminationRequestBody(
            "2000-01-02", JsonHandler.getFromString(queryDsql));

        // When Then
        assertThatExceptionOfType(VitamClientException.class)
            .isThrownBy(
                () -> client.startEliminationAnalysis(
                    new VitamContext(TENANT_ID).setAccessContract(CONTRACT), eliminationRequestBody))
            .withMessage(BAD_REQUEST);
    }

    @Test
    @RunWithCustomExecutor
    public void startEliminationActionWhenSuccessThenReturnVitamResponseOK()
        throws Exception {

        // Given
        RequestResponseOK responseOK = new RequestResponseOK();
        when(mock.post()).thenReturn(Response.status(Status.OK).entity(responseOK).build());

        EliminationRequestBody eliminationRequestBody = new EliminationRequestBody(
            "2000-01-02", JsonHandler.getFromString(queryDsql));

        // When
        RequestResponse<JsonNode> requestResponse = client.startEliminationAction(
            new VitamContext(TENANT_ID).setAccessContract(CONTRACT), eliminationRequestBody);

        // Then
        assertThat(requestResponse.isOk()).isTrue();
    }

    /*
     * Elimination action
     */

    @Test
    @RunWithCustomExecutor
    public void startEliminationActionWhenServerErrorThenReturnInternalServerErrorVitamResponse() throws Exception {

        // Given
        when(mock.post()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());

        EliminationRequestBody eliminationRequestBody = new EliminationRequestBody(
            "2000-01-02", JsonHandler.getFromString(queryDsql));

        // When Then
        assertThatExceptionOfType(VitamClientException.class)
            .isThrownBy(
                () -> client.startEliminationAction(
                    new VitamContext(TENANT_ID).setAccessContract(CONTRACT), eliminationRequestBody))
            .withMessage(INTERNAL_SERVER_ERROR);
    }

    @Test
    @RunWithCustomExecutor
    public void startEliminationActionWhenResourceNotFoundThenReturnNotFoundVitamResponse()
        throws Exception {

        // Given
        when(mock.post()).thenReturn(Response.status(Status.NOT_FOUND).build());

        EliminationRequestBody eliminationRequestBody = new EliminationRequestBody(
            "2000-01-02", JsonHandler.getFromString(queryDsql));

        // When Then
        assertThatExceptionOfType(VitamClientException.class)
            .isThrownBy(
                () -> client.startEliminationAction(
                    new VitamContext(TENANT_ID).setAccessContract(CONTRACT), eliminationRequestBody))
            .withMessage(NOT_FOUND);
    }

    @Test
    @RunWithCustomExecutor
    public void startEliminationActionWhenBadRequestThenReturnBadRequestVitamResponse()
        throws Exception {

        // Given
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).build());

        EliminationRequestBody eliminationRequestBody = new EliminationRequestBody(
            "2000-01-02", JsonHandler.getFromString(queryDsql));

        // When Then
        assertThatExceptionOfType(VitamClientException.class)
            .isThrownBy(
                () -> client.startEliminationAction(
                    new VitamContext(TENANT_ID).setAccessContract(CONTRACT), eliminationRequestBody))
            .withMessage(BAD_REQUEST);
    }

    @Test
    @RunWithCustomExecutor
    public void givenRequestNullWhencomputedInheritedRules() {
        when(mock.post()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        assertThatExceptionOfType(VitamClientException.class)
            .isThrownBy(
                () -> client
                    .computedInheritedRules(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                        JsonHandler.getFromString(QUERY_DSL)))
            .withMessage(PRECONDITION_FAILED);
    }

    @Test
    @RunWithCustomExecutor
    public void givenBadRequestWhencomputedInheritedRules() {
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        // When Then
        assertThatExceptionOfType(VitamClientException.class)
            .isThrownBy(
                () -> client
                    .computedInheritedRules(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                        JsonHandler.getFromString(QUERY_DSL)))
            .withMessage(BAD_REQUEST);
    }


    @Path("/access-external/v1")
    public static class MockResource {
        private final ExpectedResults expectedResponse;

        public MockResource(ExpectedResults expectedResponse) {
            this.expectedResponse = expectedResponse;
        }

        @GET
        @Path("units")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response getUnits(String queryDsl) {
            return expectedResponse.get();
        }

        @GET
        @Path("/units/{id_unit}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response getUnitById(String queryDsl,
            @PathParam("id_unit") String id_unit) {
            return expectedResponse.get();
        }

        @PUT
        @Path("/units/{id_unit}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response updateUnitById(String queryDsl, @PathParam("id_unit") String id_unit) {
            return expectedResponse.put();
        }

        @GET
        @Path("/units/{id_object_group}/objects")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_OCTET_STREAM)
        public Response getObjectStream(@Context HttpHeaders headers,
            @PathParam("id_object_group") String idObjectGroup, String query) {
            return expectedResponse.get();
        }

        @POST
        @Path("/units/{id_object_group}/objects")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_OCTET_STREAM)
        public Response getObjectStreamPost(@Context HttpHeaders headers,
            @PathParam("id_object_group") String idObjectGroup, String query) {
            return expectedResponse.post();
        }

        @GET
        @Path("/units/{id_unit}/objects")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response getObjectGroupMetadatas(@Context HttpHeaders headers,
            @PathParam("id_unit") String idUnit, String query) {
            return expectedResponse.get();
        }

        @POST
        @Path("/units/{id_unit}/objects")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response getObjectGroupMetadatasPost(@Context HttpHeaders headers,
            @PathParam("id_unit") String idUnit, String query) {
            return expectedResponse.post();
        }

        @GET
        @Path("unitsWithInheritedRules")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response selectUnitsWithInheritedRules(String queryDsl) {
            return expectedResponse.get();
        }


        // Logbook operations
        @GET
        @Path("/logbookoperations")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response selectOperation(@PathParam("id_op") String operationId) throws InvalidParseOperationException {
            return expectedResponse.get();
        }

        @POST
        @Path("/logbookoperations")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response selectOperationWithPostOverride(@PathParam("id_op") String operationId,
            @HeaderParam(X_HTTP_METHOD_OVERRIDE) String xhttpOverride)
            throws InvalidParseOperationException {
            return expectedResponse.post();
        }

        @GET
        @Path("/logbookoperations/{id_op}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response getOperation(@PathParam("id_op") String operationId) throws InvalidParseOperationException {
            return expectedResponse.get();
        }

        @POST
        @Path("/logbookoperations/{id_op}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response selectOperationByPost(@PathParam("id_op") String operationId,
            @HeaderParam(X_HTTP_METHOD_OVERRIDE) String xhttpOverride)
            throws InvalidParseOperationException {
            return expectedResponse.post();
        }

        @GET
        @Path("/logbookunitlifecycles/{id_lc}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response getUnitLifeCycle(@PathParam("id_lc") String unitLifeCycleId, JsonNode queryDsl) {
            return expectedResponse.get();
        }

        @GET
        @Path("/logbookobjectslifecycles/{id_lc}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response getObjectGroupLifeCycle(@PathParam("id_lc") String objectGroupLifeCycleId, JsonNode queryDsl) {
            return expectedResponse.get();
        }

        @POST
        @Path(AccessExtAPI.ACCESSION_REGISTERS_API)
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response findAccessionRegister(@PathParam("id_op") String operationId,
            @HeaderParam(X_HTTP_METHOD_OVERRIDE) String xhttpOverride)
            throws InvalidParseOperationException {
            return expectedResponse.post();
        }

        @POST
        @Path(AccessExtAPI.ACCESSION_REGISTERS_API + "/{id_document}/accession-register-detail")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response findAccessionRegisterDetail(@PathParam("id_op") String operationId,
            @HeaderParam(X_HTTP_METHOD_OVERRIDE) String xhttpOverride)
            throws InvalidParseOperationException {
            return expectedResponse.post();
        }

        @POST
        @Path(AccessExtAPI.TRACEABILITY_API + "/check")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response checkTraceabilityOperation(JsonNode query)
            throws InvalidParseOperationException {
            return expectedResponse.post();
        }

        @GET
        @Path(AccessExtAPI.TRACEABILITY_API + "/{idOperation}")
        @Produces(MediaType.APPLICATION_OCTET_STREAM)
        public Response downloadTraceabilityOperationFile(@PathParam("idOperation") String operationId)
            throws InvalidParseOperationException {
            return expectedResponse.get();
        }

        @POST
        @Path("/dipexport")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response exportDIP(JsonNode queryJson) {
            return expectedResponse.post();
        }

        @POST
        @Path("/elimination/analysis")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response startEliminationAnalysis(String queryDsl) {
            return expectedResponse.post();
        }

        @POST
        @Path("/elimination/action")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response startEliminationAction(String queryDsl) {
            return expectedResponse.post();
        }

        @POST
        @Path("/units/computedInheritedRules")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response computeInheritedrules(String queryDsl) {
            return expectedResponse.post();
        }
    }
}

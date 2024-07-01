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
package fr.gouv.vitam.ingest.internal.client;

import com.google.common.collect.Sets;
import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.FileUtil;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.client.ClientMockResultHelper;
import fr.gouv.vitam.common.client.IngestCollection;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessQuery;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.WorkFlow;
import fr.gouv.vitam.common.server.application.junit.ResteasyTestApplication;
import fr.gouv.vitam.common.serverv2.VitamServerTestRunner;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.ingest.internal.common.exception.IllegalZipFileNameException;
import fr.gouv.vitam.ingest.internal.common.exception.IngestInternalClientNotFoundException;
import fr.gouv.vitam.ingest.internal.common.exception.IngestInternalClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
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
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@SuppressWarnings("rawtypes")
@RunWithCustomExecutor
public class IngestInternalClientRestTest extends ResteasyTestApplication {

    public static final String INGEST = "INGEST";
    private static final String PATH = "/ingest/v1";
    private static final String WROKFLOW_ID = "PROCESS_SIP_UNITARY";
    private static final String WROKFLOW_IDENTIFIER = "DEFAULT_WORKFLOW";
    private static final String X_ACTION = "RESUME";
    private static final String ID = "id1";
    private static final ExpectedResults mock = mock(ExpectedResults.class);
    private static final ExpectedResults mockLogbook = mock(ExpectedResults.class);

    @ClassRule
    public static RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    static IngestInternalClientFactory factory = IngestInternalClientFactory.getInstance();
    public static VitamServerTestRunner vitamServerTestRunner = new VitamServerTestRunner(
        IngestInternalClientRestTest.class,
        factory
    );
    private static IngestInternalClientRest client;

    @BeforeClass
    public static void setUpBeforeClass() throws Throwable {
        vitamServerTestRunner.start();
        client = (IngestInternalClientRest) vitamServerTestRunner.getClient();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Throwable {
        vitamServerTestRunner.runAfter();
    }

    @Before
    public void before() throws Throwable {
        reset(mock);
        reset(mockLogbook);
    }

    @Override
    public Set<Object> getResources() {
        return Sets.newHashSet(new MockResource(mock, mockLogbook));
    }

    @Test
    public void givenStartedServerWhenUploadSipThenReturnOK() throws Exception {
        final List<LogbookOperationParameters> operationList = new ArrayList<>();

        final GUID ingestGuid = GUIDFactory.newGUID();
        final GUID containerGuid = GUIDFactory.newGUID();
        final LogbookOperationParameters externalOperationParameters1 =
            LogbookParameterHelper.newLogbookOperationParameters(
                ingestGuid,
                "Ingest external",
                containerGuid,
                LogbookTypeProcess.INGEST,
                StatusCode.STARTED,
                "Start Ingest external",
                containerGuid
            );

        final LogbookOperationParameters externalOperationParameters2 =
            LogbookParameterHelper.newLogbookOperationParameters(
                ingestGuid,
                "Ingest external",
                containerGuid,
                LogbookTypeProcess.INGEST,
                StatusCode.OK,
                "End Ingest external",
                containerGuid
            );
        operationList.add(externalOperationParameters1);
        operationList.add(externalOperationParameters2);

        InputStream inputStreamATR = PropertiesUtils.getResourceAsStream("ATR_example.xml");
        when(mockLogbook.post()).thenReturn(Response.status(Status.CREATED).build());
        when(mock.post()).thenReturn(
            Response.status(Status.OK).entity(FileUtil.readInputStream(inputStreamATR)).build()
        );
        final InputStream inputStream = PropertiesUtils.getResourceAsStream("SIP_bordereau_avec_objet_OK.zip");
        client.uploadInitialLogbook(operationList);
        WorkFlow workflow = WorkFlow.of(WROKFLOW_ID, WROKFLOW_IDENTIFIER, INGEST);
        assertThatCode(
            () -> client.upload(inputStream, CommonMediaType.ZIP_TYPE, workflow, X_ACTION)
        ).doesNotThrowAnyException();
    }

    @Test
    public void givenVirusWhenUploadSipThenReturnKO() throws Exception {
        final List<LogbookOperationParameters> operationList = new ArrayList<>();

        final GUID ingestGuid = GUIDFactory.newGUID();
        final GUID conatinerGuid = GUIDFactory.newGUID();
        final LogbookOperationParameters externalOperationParameters1 =
            LogbookParameterHelper.newLogbookOperationParameters(
                ingestGuid,
                "Ingest external",
                conatinerGuid,
                LogbookTypeProcess.INGEST,
                StatusCode.STARTED,
                "Start Ingest external",
                conatinerGuid
            );

        final LogbookOperationParameters externalOperationParameters2 =
            LogbookParameterHelper.newLogbookOperationParameters(
                ingestGuid,
                "Ingest external",
                conatinerGuid,
                LogbookTypeProcess.INGEST,
                StatusCode.KO,
                "End Ingest external",
                conatinerGuid
            );
        operationList.add(externalOperationParameters1);
        operationList.add(externalOperationParameters2);

        try (InputStream inputStreamATR = PropertiesUtils.getResourceAsStream("ATR_example.xml")) {
            when(mockLogbook.post()).thenReturn(Response.status(Status.CREATED).build());
            when(mock.post()).thenReturn(
                Response.status(Status.INTERNAL_SERVER_ERROR).entity(FileUtil.readInputStream(inputStreamATR)).build()
            );
            client.uploadInitialLogbook(operationList);
            try (
                final InputStream inputStream = PropertiesUtils.getResourceAsStream("SIP_bordereau_avec_objet_OK.zip")
            ) {
                WorkFlow workflow = WorkFlow.of(WROKFLOW_ID, WROKFLOW_IDENTIFIER, INGEST);
                ThrowingCallable throwingCallable = () ->
                    client.upload(inputStream, CommonMediaType.ZIP_TYPE, workflow, X_ACTION);
                assertThatThrownBy(throwingCallable).isInstanceOf(IngestInternalClientServerException.class);
            }
        }
    }

    @Test
    public void givenServerErrorWhenPostSipThenRaiseAnException() throws Exception {
        final List<LogbookOperationParameters> operationList = new ArrayList<>();

        final GUID ingestGuid = GUIDFactory.newGUID();
        final GUID conatinerGuid = GUIDFactory.newGUID();
        final LogbookOperationParameters externalOperationParameters1 =
            LogbookParameterHelper.newLogbookOperationParameters(
                ingestGuid,
                "Ingest external",
                conatinerGuid,
                LogbookTypeProcess.INGEST,
                StatusCode.STARTED,
                "Start Ingest external",
                conatinerGuid
            );

        final LogbookOperationParameters externalOperationParameters2 =
            LogbookParameterHelper.newLogbookOperationParameters(
                ingestGuid,
                "Ingest external",
                conatinerGuid,
                LogbookTypeProcess.INGEST,
                StatusCode.OK,
                "End Ingest external",
                conatinerGuid
            );
        operationList.add(externalOperationParameters1);
        operationList.add(externalOperationParameters2);
        when(mockLogbook.post()).thenReturn(Response.status(Status.CREATED).build());
        when(mock.post()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());

        try (final InputStream inputStream = PropertiesUtils.getResourceAsStream("SIP_bordereau_avec_objet_OK.zip")) {
            client.uploadInitialLogbook(operationList);
            WorkFlow workflow = WorkFlow.of(WROKFLOW_ID, WROKFLOW_IDENTIFIER, INGEST);
            ThrowingCallable throwingCallable = () ->
                client.upload(inputStream, CommonMediaType.ZIP_TYPE, workflow, X_ACTION);
            assertThatThrownBy(throwingCallable).isInstanceOf(IngestInternalClientServerException.class);
        }
    }

    @Test
    public void givenStartedServerWhenUploadSipNonZipThenReturnKO() throws Exception {
        final List<LogbookOperationParameters> operationList = new ArrayList<>();

        final GUID ingestGuid = GUIDFactory.newGUID();
        final GUID conatinerGuid = GUIDFactory.newGUID();
        final LogbookOperationParameters externalOperationParameters1 =
            LogbookParameterHelper.newLogbookOperationParameters(
                ingestGuid,
                "Ingest external",
                conatinerGuid,
                LogbookTypeProcess.INGEST,
                StatusCode.STARTED,
                "Start Ingest external",
                conatinerGuid
            );

        final LogbookOperationParameters externalOperationParameters2 =
            LogbookParameterHelper.newLogbookOperationParameters(
                ingestGuid,
                "Ingest external",
                conatinerGuid,
                LogbookTypeProcess.INGEST,
                StatusCode.OK,
                "End Ingest external",
                conatinerGuid
            );
        operationList.add(externalOperationParameters1);
        operationList.add(externalOperationParameters2);
        when(mockLogbook.post()).thenReturn(Response.status(Status.CREATED).build());
        when(mock.post()).thenReturn(Response.status(Status.NOT_ACCEPTABLE).build());
        try (final InputStream inputStream = PropertiesUtils.getResourceAsStream("SIP_mauvais_format.pdf")) {
            client.uploadInitialLogbook(operationList);
            WorkFlow workflow = WorkFlow.of(WROKFLOW_ID, WROKFLOW_IDENTIFIER, INGEST);
            ThrowingCallable throwingCallable = () ->
                client.upload(inputStream, CommonMediaType.ZIP_TYPE, workflow, X_ACTION);
            assertThatThrownBy(throwingCallable).isInstanceOf(IllegalZipFileNameException.class);
        }
    }

    @Test
    public void givenInputstreamWhenDownloadObjectThenReturnOK() throws Exception {
        when(mock.get()).thenReturn(ClientMockResultHelper.getObjectStream());

        try (
            final InputStream fakeUploadResponseInputStream = client
                .downloadObjectAsync("1", IngestCollection.MANIFESTS)
                .readEntity(InputStream.class)
        ) {
            assertTrue(IOUtils.contentEquals(fakeUploadResponseInputStream, StreamUtils.toInputStream("test")));
        } catch (final IOException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void givenNotFoundWhenDownloadObjectThenReturnKo() {
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND.getStatusCode()).build());
        ThrowingCallable throwingCallable = () ->
            client.downloadObjectAsync("1", IngestCollection.MANIFESTS).readEntity(InputStream.class);
        assertThatThrownBy(throwingCallable).isInstanceOf(IngestInternalClientNotFoundException.class);
    }

    @Test
    public void givenInternalServerErrorWhenDownloadObjectThenThrowIngestInternalClientServerException() {
        when(mock.get()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR.getStatusCode()).build());
        ThrowingCallable throwingCallable = () ->
            client.downloadObjectAsync("1", IngestCollection.MANIFESTS).readEntity(InputStream.class);
        assertThatThrownBy(throwingCallable).isInstanceOf(IngestInternalClientServerException.class);
    }

    @Test
    public void givenBadRequestWhenDownloadObjectThenThrowInvalidParseOperationException() {
        when(mock.get()).thenReturn(Response.status(Status.BAD_REQUEST.getStatusCode()).build());
        ThrowingCallable throwingCallable = () ->
            client.downloadObjectAsync("1", IngestCollection.MANIFESTS).readEntity(InputStream.class);
        assertThatThrownBy(throwingCallable).isInstanceOf(InvalidParseOperationException.class);
    }

    @Test
    public void givenHeadOperationStatusThenReturnOK() {
        // 404
        when(mock.head()).thenReturn(Response.status(Status.NOT_FOUND).build());
        ThrowingCallable throwingCallable = () -> client.getOperationProcessStatus(ID);
        assertThatThrownBy(throwingCallable).isInstanceOf(VitamClientInternalException.class);
    }

    @Test
    public void givenHeadOperationStatusPreconditionFailedThenThrowInternalServerError() {
        // 412
        when(mock.head()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        ThrowingCallable throwingCallable = () -> client.getOperationProcessStatus(ID);
        assertThatThrownBy(throwingCallable).isInstanceOf(VitamClientInternalException.class);
    }

    @Test
    public void givenHeadOperationOKThenOK() {
        Response.ResponseBuilder builder = Response.status(Status.ACCEPTED)
            .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATE, ProcessState.COMPLETED)
            .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATUS, StatusCode.OK)
            .header(GlobalDataRest.X_CONTEXT_ID, LogbookTypeProcess.INGEST.toString());
        when(mock.head()).thenReturn(builder.build());
        assertThatCode(() -> client.getOperationProcessStatus(ID)).doesNotThrowAnyException();
    }

    @Test
    public void givenHeadOperationthInternalServerErrorThenThrowInternalServerError() {
        // 500
        when(mock.head()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        ThrowingCallable throwingCallable = () -> client.getOperationProcessStatus(ID);
        assertThatThrownBy(throwingCallable).isInstanceOf(VitamClientInternalException.class);
    }

    @Test
    public void givenHeadOperationStatusThenThrowUnauthorized() {
        // 401
        when(mock.head()).thenReturn(Response.status(Status.UNAUTHORIZED).build());
        ThrowingCallable throwingCallable = () -> client.getOperationProcessStatus(ID);
        assertThatThrownBy(throwingCallable).isInstanceOf(VitamClientInternalException.class);
    }

    @Test
    public void testPreconditionFailedWhenGetOperationProcessExecutionDetails() {
        VitamError vitamError = new VitamError("code")
            .setMessage("msg")
            .setDescription("desc")
            .setContext("ctx")
            .setState("st");
        ThrowingCallable throwingCallable;

        // 412
        when(mock.get()).thenReturn(Response.status(Status.PRECONDITION_FAILED).entity(vitamError).build());
        throwingCallable = () -> client.getOperationProcessExecutionDetails(ID);
        assertThatThrownBy(throwingCallable).isInstanceOf(VitamClientInternalException.class);

        // 500
        when(mock.get()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).entity(vitamError).build());
        throwingCallable = () -> client.getOperationProcessExecutionDetails(ID);
        assertThatThrownBy(throwingCallable).isInstanceOf(VitamClientInternalException.class);

        // 404
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).entity(vitamError).build());
        throwingCallable = () -> client.getOperationProcessExecutionDetails(ID);
        assertThatThrownBy(throwingCallable).isInstanceOf(VitamClientInternalException.class);
    }

    @Test
    public void givenDeleteOperationStatusErrors() {
        VitamError vitamError = new VitamError("code")
            .setMessage("msg")
            .setDescription("desc")
            .setContext("ctx")
            .setState("st");
        ThrowingCallable throwingCallable;

        // 500
        when(mock.delete()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).entity(vitamError).build());
        throwingCallable = () -> client.cancelOperationProcessExecution(ID);
        assertThatThrownBy(throwingCallable).isInstanceOf(VitamClientInternalException.class);

        // 409
        when(mock.delete()).thenReturn(Response.status(Status.CONFLICT).entity(vitamError).build());
        throwingCallable = () -> client.cancelOperationProcessExecution(ID);
        assertThatThrownBy(throwingCallable).isInstanceOf(VitamClientInternalException.class);

        // 412
        when(mock.delete()).thenReturn(Response.status(Status.PRECONDITION_FAILED).entity(vitamError).build());
        throwingCallable = () -> client.cancelOperationProcessExecution(ID);
        assertThatThrownBy(throwingCallable).isInstanceOf(VitamClientInternalException.class);

        // 404
        when(mock.delete()).thenReturn(Response.status(Status.NOT_FOUND).entity(vitamError).build());
        throwingCallable = () -> client.cancelOperationProcessExecution(ID);
        assertThatThrownBy(throwingCallable).isInstanceOf(VitamClientInternalException.class);
    }

    @Test
    public void givenDeleteOKThenOK() throws Exception {
        ItemStatus result = new ItemStatus();
        result.setGlobalState(ProcessState.COMPLETED);
        result.increment(StatusCode.FATAL);
        result.setItemId("Itzm");

        RequestResponseOK<ItemStatus> responseOK = new RequestResponseOK<ItemStatus>().addResult(result);
        responseOK.setHttpCode(Status.ACCEPTED.getStatusCode());

        when(mock.delete()).thenReturn(Response.status(Status.ACCEPTED).entity(responseOK).build());
        RequestResponse<ItemStatus> response = client.cancelOperationProcessExecution(ID);
        assertEquals(response.isOk(), true);
        RequestResponseOK<ItemStatus> respOK = (RequestResponseOK<ItemStatus>) response;
        assertEquals(respOK.getResults().iterator().hasNext(), true);
        assertEquals(respOK.getResults().iterator().next().getGlobalStatus(), StatusCode.FATAL);
        assertEquals(respOK.getResults().iterator().next().getGlobalState(), ProcessState.COMPLETED);
    }

    @Test
    public void givenUnauthorizedInitWorkFlowThenThrowVitamClientInternalException() {
        // 401
        when(mock.post()).thenReturn(Response.status(Status.UNAUTHORIZED).build());
        WorkFlow workflow = WorkFlow.of(WROKFLOW_ID, WROKFLOW_IDENTIFIER, INGEST);
        ThrowingCallable throwingCallable = () -> client.initWorkflow(workflow);
        assertThatThrownBy(throwingCallable).isInstanceOf(VitamClientException.class);
    }

    @Test
    public void givenInitWorkFlowThenReturnResponseAccepted() {
        // 202
        when(mock.post()).thenReturn(Response.status(Status.ACCEPTED).build());
        WorkFlow workflow = WorkFlow.of(WROKFLOW_ID, WROKFLOW_IDENTIFIER, INGEST);
        assertThatCode(() -> client.initWorkflow(workflow)).doesNotThrowAnyException();
    }

    @Test
    public void givenInitWorkFlowNotFoundThenThrowVitamClientInternalException() {
        // 404
        when(mock.post()).thenReturn(Response.status(Status.NOT_FOUND).build());
        WorkFlow workflow = WorkFlow.of(WROKFLOW_ID, WROKFLOW_IDENTIFIER, INGEST);
        ThrowingCallable throwingCallable = () -> client.initWorkflow(workflow);
        assertThatThrownBy(throwingCallable).isInstanceOf(VitamClientException.class);
    }

    @Test
    public void givenInitWorkFlowInternalServerErrorThenThrowVitamClientInternalException() {
        // 500
        when(mock.post()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        WorkFlow workflow = WorkFlow.of(WROKFLOW_ID, WROKFLOW_IDENTIFIER, INGEST);
        ThrowingCallable throwingCallable = () -> client.initWorkflow(workflow);
        assertThatThrownBy(throwingCallable).isInstanceOf(VitamClientException.class);
    }

    @Test
    public void givenOKWhenDefinitionsWorkflowThenReturnMap() throws Exception {
        List<WorkFlow> desired = new ArrayList<>();
        desired.add(new WorkFlow().setId("TEST").setComment("TEST comment"));
        RequestResponseOK<WorkFlow> expected = new RequestResponseOK<>();
        expected.addAllResults(desired);
        expected.setHits(1, 0, 1, 1);
        expected.setHttpCode(Status.OK.getStatusCode());
        when(mock.get()).thenReturn(Response.status(Status.OK).entity(expected).build());
        RequestResponse<WorkFlow> requestResponse = client.getWorkflowDefinitions();
        assertEquals(expected.getHttpCode(), requestResponse.getHttpCode());
    }

    @Test
    public void givenNotFoundWhenDefinitionsWorkflowThenReturnVitamError() {
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        ThrowingCallable throwingCallable = () -> client.getWorkflowDefinitions();
        assertThatThrownBy(throwingCallable).isInstanceOf(VitamClientInternalException.class);
    }

    @Test
    public void givenNotFoundWhenGetWorkFlowDetailThenReturnOptionalEmpty() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        Optional<WorkFlow> requestReponse = client.getWorkflowDetails("FAKE_WORKFLOW");
        assertEquals(Boolean.FALSE, requestReponse.isPresent());
    }

    @Test
    public void givenNotFoundWhenListOperationsThenReturnVitamError() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        ThrowingCallable throwingCallable = () -> client.listOperationsDetails(new ProcessQuery());
        assertThatThrownBy(throwingCallable).isInstanceOf(VitamClientInternalException.class);
    }

    @Test
    public void givenPreconditionFailedWhenDefinitionsWorkflowThenReturnVitamError() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        ThrowingCallable throwingCallable = () -> client.getWorkflowDefinitions();
        assertThatThrownBy(throwingCallable).isInstanceOf(VitamClientInternalException.class);
    }

    @Test
    public void givenUnauthaurizedWhenDefinitionsWorkflowThenReturnVitamError() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.UNAUTHORIZED).build());
        ThrowingCallable throwingCallable = () -> client.getWorkflowDefinitions();
        assertThatThrownBy(throwingCallable).isInstanceOf(VitamClientInternalException.class);
    }

    @Test
    public void givenInternalServerErrorWhenDefinitionsWorkflowThenReturnVitamError() {
        when(mock.get()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        ThrowingCallable throwingCallable = () -> client.getWorkflowDefinitions();
        assertThatThrownBy(throwingCallable).isInstanceOf(VitamClientInternalException.class);
    }

    @Path(PATH)
    public static class MockResource {

        private final ExpectedResults expectedResponse;
        private final ExpectedResults expectedResponseLogbook;

        public MockResource(ExpectedResults expectedResponse, ExpectedResults expectedResponseLogbook) {
            this.expectedResponse = expectedResponse;
            this.expectedResponseLogbook = expectedResponseLogbook;
        }

        @POST
        @Path("/logbooks")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response delegateCreateLogbookOperation(Queue<LogbookOperationParameters> queue) {
            return expectedResponseLogbook.post();
        }

        @PUT
        @Path("/logbooks")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response delegateUpdateLogbookOperation(Queue<LogbookOperationParameters> queue) {
            return expectedResponseLogbook.put();
        }

        @POST
        @Path("/ingests")
        @Consumes(
            {
                MediaType.APPLICATION_OCTET_STREAM,
                CommonMediaType.ZIP,
                CommonMediaType.XGZIP,
                CommonMediaType.GZIP,
                CommonMediaType.TAR,
                CommonMediaType.BZIP2,
            }
        )
        public Response uploadSipAsStream(
            @HeaderParam(HttpHeaders.CONTENT_TYPE) String contentType,
            @HeaderParam(GlobalDataRest.X_CONTEXT_ID) String contextId,
            @HeaderParam(GlobalDataRest.X_ACTION) String actionId,
            @HeaderParam(GlobalDataRest.X_ACTION_INIT) String xActionInit,
            @HeaderParam(GlobalDataRest.X_TYPE_PROCESS) LogbookTypeProcess logbookTypeProcess,
            InputStream uploadedInputStream
        ) {
            return expectedResponse.post();
        }

        @Path("/operations/{id}")
        @PUT
        @Produces(MediaType.APPLICATION_JSON)
        public Response updateWorkFlowStatus(@Context HttpHeaders headers, @PathParam("id") String id) {
            return expectedResponse.put();
        }

        @Path("/operations/{id}")
        @HEAD
        @Produces(MediaType.APPLICATION_JSON)
        public Response getWorkFlowExecutionStatus(@PathParam("id") String id) {
            return expectedResponse.head();
        }

        @Path("/operations/{id}")
        @GET
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response getWorkFlowStatus(@PathParam("id") String id) {
            return expectedResponse.get();
        }

        @Path("/operations/{id}")
        @DELETE
        @Produces(MediaType.APPLICATION_JSON)
        public Response interruptWorkFlowExecution(@PathParam("id") String id) {
            return expectedResponse.delete();
        }

        @GET
        @Path("/ingests/{objectId}/{type}")
        @Produces(MediaType.APPLICATION_OCTET_STREAM)
        public Response downloadObjectAsStream(@PathParam("objectId") String objectId, @PathParam("type") String type) {
            return expectedResponse.get();
        }

        @POST
        @Path("/ingests/{objectId}/report")
        @Consumes(MediaType.APPLICATION_OCTET_STREAM)
        public Response storeATR(@PathParam("objectId") String guid, InputStream atr) {
            return expectedResponse.post();
        }

        @GET
        @Path("/operations")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response listOperationsDetails(@Context HttpHeaders headers, ProcessQuery query) {
            return expectedResponse.get();
        }

        @GET
        @Path("/workflows")
        @Produces(MediaType.APPLICATION_JSON)
        public Response getWorkflowDefinitions(@Context HttpHeaders headers) {
            return expectedResponse.get();
        }

        @Path("workflows/{workfowId}")
        @GET
        @Produces(MediaType.APPLICATION_JSON)
        public Response getWorkflowDetails(@PathParam("workfowId") String workfowId) {
            return expectedResponse.get();
        }
    }
}

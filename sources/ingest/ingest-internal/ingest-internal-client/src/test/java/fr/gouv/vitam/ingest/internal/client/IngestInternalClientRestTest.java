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
package fr.gouv.vitam.ingest.internal.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

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
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.ClassRule;
import org.junit.Test;

import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.FileUtil;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.client.ClientMockResultHelper;
import fr.gouv.vitam.common.client.IngestCollection;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.exception.WorkflowNotFoundException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.WorkFlow;
import fr.gouv.vitam.common.server.application.AbstractVitamApplication;
import fr.gouv.vitam.common.server.application.configuration.DefaultVitamApplicationConfiguration;
import fr.gouv.vitam.common.server.application.junit.VitamJerseyTest;
import fr.gouv.vitam.ingest.internal.common.exception.IngestInternalClientNotFoundException;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;

@SuppressWarnings("rawtypes")
@RunWithCustomExecutor
public class IngestInternalClientRestTest extends VitamJerseyTest {

    private static final String PATH = "/ingest/v1";
    private static final String CONTEXTID = "contextId";
    private static final String CONTAINERID = "containerId";
    private static final String WORKFLOWID = "workFlowId";
    private static final String ID = "id1";

    @ClassRule
    public static RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());


    private IngestInternalClientRest client;

    public ExpectedResults mockLogbook;

    // ************************************** //
    // Start of VitamJerseyTest configuration //
    // ************************************** //
    @SuppressWarnings("unchecked")
    public IngestInternalClientRestTest() {
        super(IngestInternalClientFactory.getInstance());
    }

    @Override
    public void beforeTest() {
        client = (IngestInternalClientRest) getClient();
    }

    // Define your Application class if necessary
    public final class AbstractApplication
        extends AbstractVitamApplication<AbstractApplication, TestVitamApplicationConfiguration> {
        protected AbstractApplication(TestVitamApplicationConfiguration configuration) {
            super(TestVitamApplicationConfiguration.class, configuration);
        }

        @Override
        protected void registerInResourceConfig(ResourceConfig resourceConfig) {
            mockLogbook = mock(ExpectedResults.class);
            resourceConfig.registerInstances(new MockRessource(mock, mockLogbook));
        }

        @Override
        protected boolean registerInAdminConfig(ResourceConfig resourceConfig) {
            // do nothing as @admin is not tested here
            return false;
        }
    }

    // Define your Configuration class if necessary
    public static class TestVitamApplicationConfiguration extends DefaultVitamApplicationConfiguration {

    }

    @Override
    public StartApplicationResponse startVitamApplication(int reservedPort) throws IllegalStateException {
        final TestVitamApplicationConfiguration configuration = new TestVitamApplicationConfiguration();
        configuration.setJettyConfig(DEFAULT_XML_CONFIGURATION_FILE);
        final AbstractApplication application = new AbstractApplication(configuration);
        try {
            application.start();
        } catch (final VitamApplicationServerException e) {
            throw new IllegalStateException("Cannot start the application", e);
        }
        return new StartApplicationResponse<AbstractApplication>()
            .setServerPort(application.getVitamServer().getPort())
            .setApplication(application);
    }

    @Path(PATH)
    public static class MockRessource<ProcessingEntry> {

        private final ExpectedResults expectedResponse;
        private final ExpectedResults expectedResponseLogbook;

        public MockRessource(ExpectedResults expectedResponse, ExpectedResults expectedResponseLogbook) {
            this.expectedResponse = expectedResponse;
            this.expectedResponseLogbook = expectedResponseLogbook;
        }

        @Path("/ingests")
        @POST
        @Consumes({MediaType.APPLICATION_OCTET_STREAM, CommonMediaType.ZIP, CommonMediaType.GZIP, CommonMediaType.TAR})
        @Produces(MediaType.APPLICATION_OCTET_STREAM)
        public Response uploadSipAsStream(@HeaderParam(HttpHeaders.CONTENT_TYPE) String contentType,
            InputStream uploadedInputStream) {
            return expectedResponse.post();
        }

        @GET
        @Path("/ingests/{objectId}/{type}")
        @Produces(MediaType.APPLICATION_OCTET_STREAM)
        public Response downloadObject(@PathParam("objectId") String objectId, @PathParam("type") String type) {
            return expectedResponse.get();
        }

        @Path("/logbooks")
        @POST
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response delegateCreateLogbookOperation(Queue<LogbookOperationParameters> queue) {
            return expectedResponseLogbook.post();
        }

        @Path("/operations/{id}")
        @POST
        @Consumes({MediaType.APPLICATION_OCTET_STREAM, CommonMediaType.ZIP, CommonMediaType.GZIP, CommonMediaType.TAR,
            CommonMediaType.BZIP2})
        @Produces(MediaType.APPLICATION_OCTET_STREAM)
        public Response executeWorkFlow(@Context HttpHeaders headers, @PathParam("id") String id,
            InputStream uploadedInputStream) {
            return expectedResponse.post();
        }

        @Path("/operations/{id}")
        @GET
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response getWorkFlowStatus(@PathParam("id") String id) {
            return expectedResponse.get();
        }

        @Path("/operations/{id}")
        @PUT
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response updateWorkFlowStatus(@Context HttpHeaders headers, @PathParam("id") String id,
            ProcessingEntry process) {
            return expectedResponse.put();
        }

        @Path("/operations/{id}")
        @HEAD
        @Produces(MediaType.APPLICATION_JSON)
        public Response getWorkFlowExecutionStatus(@PathParam("id") String id) {
            return expectedResponse.head();
        }

        @Path("/operations/{id}")
        @DELETE
        @Produces(MediaType.APPLICATION_JSON)
        public Response InterruptWorkFlowExecution(@PathParam("id") String id) {
            return expectedResponse.delete();
        }

        @Path("/workflows")
        @GET
        @Produces(MediaType.APPLICATION_JSON)
        public Response getWorkflowDefinitions() {
            return expectedResponse.get();
        }
    }

    @Test
    public void givenStartedServerWhenUploadSipThenReturnOK() throws Exception {

        final List<LogbookOperationParameters> operationList = new ArrayList<>();

        final GUID ingestGuid = GUIDFactory.newGUID();
        final GUID conatinerGuid = GUIDFactory.newGUID();
        final LogbookOperationParameters externalOperationParameters1 =
            LogbookParametersFactory.newLogbookOperationParameters(
                ingestGuid,
                "Ingest external",
                conatinerGuid,
                LogbookTypeProcess.INGEST,
                StatusCode.STARTED,
                "Start Ingest external",
                conatinerGuid);

        final LogbookOperationParameters externalOperationParameters2 =
            LogbookParametersFactory.newLogbookOperationParameters(
                ingestGuid,
                "Ingest external",
                conatinerGuid,
                LogbookTypeProcess.INGEST,
                StatusCode.OK,
                "End Ingest external",
                conatinerGuid);
        operationList.add(externalOperationParameters1);
        operationList.add(externalOperationParameters2);

        InputStream inputStreamATR = PropertiesUtils.getResourceAsStream("ATR_example.xml");
        when(mockLogbook.post()).thenReturn(Response.status(Status.CREATED).build());
        when(mock.post())
            .thenReturn(Response.status(Status.OK).entity(FileUtil.readInputStream(inputStreamATR)).build());
        final InputStream inputStream =
            PropertiesUtils.getResourceAsStream("SIP_bordereau_avec_objet_OK.zip");
        client.uploadInitialLogbook(operationList);
        client.upload(inputStream, CommonMediaType.ZIP_TYPE, CONTEXTID);
    }

    @Test
    public void givenVirusWhenUploadSipThenReturnKO() throws Exception {

        final List<LogbookOperationParameters> operationList = new ArrayList<>();

        final GUID ingestGuid = GUIDFactory.newGUID();
        final GUID conatinerGuid = GUIDFactory.newGUID();
        final LogbookOperationParameters externalOperationParameters1 =
            LogbookParametersFactory.newLogbookOperationParameters(
                ingestGuid,
                "Ingest external",
                conatinerGuid,
                LogbookTypeProcess.INGEST,
                StatusCode.STARTED,
                "Start Ingest external",
                conatinerGuid);

        final LogbookOperationParameters externalOperationParameters2 =
            LogbookParametersFactory.newLogbookOperationParameters(
                ingestGuid,
                "Ingest external",
                conatinerGuid,
                LogbookTypeProcess.INGEST,
                StatusCode.KO,
                "End Ingest external",
                conatinerGuid);
        operationList.add(externalOperationParameters1);
        operationList.add(externalOperationParameters2);

        InputStream inputStreamATR = PropertiesUtils.getResourceAsStream("ATR_example.xml");
        when(mockLogbook.post()).thenReturn(Response.status(Status.CREATED).build());
        when(mock.post()).thenReturn(
            Response.status(Status.INTERNAL_SERVER_ERROR).entity(FileUtil.readInputStream(inputStreamATR)).build());
        client.uploadInitialLogbook(operationList);
        final InputStream inputStream =
            PropertiesUtils.getResourceAsStream("SIP_bordereau_avec_objet_OK.zip");
        client.upload(inputStream, CommonMediaType.ZIP_TYPE, CONTEXTID);
    }

    @Test
    public void givenServerErrorWhenPostSipThenRaiseAnException() throws Exception {

        final List<LogbookOperationParameters> operationList = new ArrayList<>();

        final GUID ingestGuid = GUIDFactory.newGUID();
        final GUID conatinerGuid = GUIDFactory.newGUID();
        final LogbookOperationParameters externalOperationParameters1 =
            LogbookParametersFactory.newLogbookOperationParameters(
                ingestGuid,
                "Ingest external",
                conatinerGuid,
                LogbookTypeProcess.INGEST,
                StatusCode.STARTED,
                "Start Ingest external",
                conatinerGuid);

        final LogbookOperationParameters externalOperationParameters2 =
            LogbookParametersFactory.newLogbookOperationParameters(
                ingestGuid,
                "Ingest external",
                conatinerGuid,
                LogbookTypeProcess.INGEST,
                StatusCode.OK,
                "End Ingest external",
                conatinerGuid);
        operationList.add(externalOperationParameters1);
        operationList.add(externalOperationParameters2);
        when(mockLogbook.post()).thenReturn(Response.status(Status.CREATED).build());
        when(mock.post()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());

        final InputStream inputStream =
            PropertiesUtils.getResourceAsStream("SIP_bordereau_avec_objet_OK.zip");

        client.uploadInitialLogbook(operationList);
        client.upload(inputStream, CommonMediaType.ZIP_TYPE, CONTEXTID);

    }

    @Test
    public void givenStartedServerWhenUploadSipNonZipThenReturnKO() throws Exception {

        final List<LogbookOperationParameters> operationList = new ArrayList<>();

        final GUID ingestGuid = GUIDFactory.newGUID();
        final GUID conatinerGuid = GUIDFactory.newGUID();
        final LogbookOperationParameters externalOperationParameters1 =
            LogbookParametersFactory.newLogbookOperationParameters(
                ingestGuid,
                "Ingest external",
                conatinerGuid,
                LogbookTypeProcess.INGEST,
                StatusCode.STARTED,
                "Start Ingest external",
                conatinerGuid);

        final LogbookOperationParameters externalOperationParameters2 =
            LogbookParametersFactory.newLogbookOperationParameters(
                ingestGuid,
                "Ingest external",
                conatinerGuid,
                LogbookTypeProcess.INGEST,
                StatusCode.OK,
                "End Ingest external",
                conatinerGuid);
        operationList.add(externalOperationParameters1);
        operationList.add(externalOperationParameters2);
        when(mockLogbook.post()).thenReturn(Response.status(Status.CREATED).build());
        when(mock.post()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        final InputStream inputStream =
            PropertiesUtils.getResourceAsStream("SIP_mauvais_format.pdf");
        client.uploadInitialLogbook(operationList);
        client.upload(inputStream, CommonMediaType.ZIP_TYPE, CONTEXTID);

    }

    @Test
    public void givenInputstreamWhenDownloadObjectThenReturnOK()
        throws Exception {

        when(mock.get()).thenReturn(ClientMockResultHelper.getObjectStream());

        final InputStream fakeUploadResponseInputStream =
            client.downloadObjectAsync("1", IngestCollection.MANIFESTS).readEntity(InputStream.class);
        assertNotNull(fakeUploadResponseInputStream);

        try {
            assertTrue(IOUtils.contentEquals(fakeUploadResponseInputStream,
                StreamUtils.toInputStream("test")));
        } catch (final IOException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void givenInputstreamWhenDownloadObjectThenStoreATR()
        throws Exception {
        when(mock.get()).thenReturn(ClientMockResultHelper.getObjectStream());
        final InputStream fakeUploadResponseInputStream =
            client.downloadObjectAsync("1", IngestCollection.MANIFESTS).readEntity(InputStream.class);
        assertNotNull(fakeUploadResponseInputStream);
        client.storeATR(GUIDFactory.newGUID(), fakeUploadResponseInputStream);
    }

    @Test(expected = IngestInternalClientNotFoundException.class)
    public void givenNotFoundWhenDownloadObjectThenReturnKo()
        throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND.getStatusCode()).build());
        client.downloadObjectAsync("1", IngestCollection.MANIFESTS).readEntity(InputStream.class);
    }

    @Test(expected = VitamClientInternalException.class)
    public void givenHeadOperationStatusThenReturnOK()
        throws Exception {

        when(mock.head()).thenReturn(Response.status(Status.NOT_FOUND).build());
        client.getOperationProcessStatus(ID);

    }

    @Test(expected = VitamClientInternalException.class)
    public void givenHeadOperationStatusPreconditionFailedThenThrowInternalServerError()
        throws Exception {

        when(mock.head()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        client.getOperationProcessStatus(ID);

    }

    @Test(expected = WorkflowNotFoundException.class)
    public void givenGetOperationStatusThenThrowVitamClientInternalException()
        throws Exception {

        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        client.getOperationProcessExecutionDetails(ID);

    }

    @Test(expected = VitamClientInternalException.class)
    public void givenHeadOperationStatusThenThrowInternalServerError()
        throws Exception {

        when(mock.head()).thenReturn(Response.status(Status.NOT_FOUND).build());
        client.getOperationProcessStatus(ID);

    }

    @Test
    public void givenHeadOperationOKThenOK()
        throws Exception {

        Response.ResponseBuilder builder = Response.status(Status.ACCEPTED)
            .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATE, ProcessState.COMPLETED)
            .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATUS, StatusCode.OK)
            .header(GlobalDataRest.X_CONTEXT_ID, LogbookTypeProcess.INGEST.toString());
        when(mock.head())
            .thenReturn(builder.build());
        ItemStatus status = client.getOperationProcessStatus(ID);
        assertEquals(status.getGlobalStatus(), StatusCode.OK);

    }

    @Test(expected = VitamClientInternalException.class)
    public void givenGetOperationStatusThenThrowInternalServerError()
        throws Exception {

        when(mock.get()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        client.getOperationProcessExecutionDetails(ID);

    }

    @Test(expected = VitamClientInternalException.class)
    public void givenHeadOperationthInternalServerErrorThenThrowInternalServerError()
        throws Exception {

        when(mock.head()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        client.getOperationProcessStatus(ID);

    }

    @Test(expected = VitamClientInternalException.class)
    public void givenHeadOperationStatusThenThrowUnauthorized()
        throws Exception {

        when(mock.head()).thenReturn(Response.status(Status.UNAUTHORIZED).build());
        client.getOperationProcessStatus(ID);

    }

    @Test(expected = VitamClientInternalException.class)
    public void givenGetOperationStatusThenThrowUnauthorized()
        throws Exception {

        when(mock.get()).thenReturn(Response.status(Status.UNAUTHORIZED).build());
        client.getOperationProcessExecutionDetails(ID);

    }

    @Test(expected = VitamClientException.class)
    public void givenGetPreconditionFailedStatusThenThrowUnauthorized()
        throws Exception {

        when(mock.get()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        client.getOperationProcessExecutionDetails(ID);

    }

    @Test(expected = BadRequestException.class)
    public void givenDeleteOperationStatusThenThrowUnauthorized()
        throws Exception {

        when(mock.delete()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        client.cancelOperationProcessExecution(ID);

    }

    @Test(expected = VitamClientException.class)
    public void givenDeleteOperationStatusThenThrowInternalServerError()
        throws Exception {

        when(mock.delete()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        client.cancelOperationProcessExecution(ID);

    }

    @Test(expected = WorkflowNotFoundException.class)
    public void givenDeleteOperationNotFoundStatusThenThrowWorkflowNotFoundException()
        throws Exception {
        when(mock.delete()).thenReturn(Response.status(Status.NOT_FOUND).build());
        client.cancelOperationProcessExecution(ID);

    }

    @Test(expected = VitamClientException.class)
    public void givenDeletePreconditionFailedThenThrowInternalServerError()
        throws Exception {

        when(mock.delete()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        client.cancelOperationProcessExecution(ID);

    }

    @Test
    public void givenDeleteOKThenOK()
        throws Exception {
        when(mock.delete())
            .thenReturn(Response.status(Status.OK).entity(new ItemStatus().increment(StatusCode.OK)).build());
        ItemStatus status = client.cancelOperationProcessExecution(ID);
        assertEquals(status.getGlobalStatus(), StatusCode.OK);

    }

    @Test(expected = VitamClientException.class)
    public void givenUnauthorizedInitOperationStatusThenThrowVitamClientInternalException()
        throws Exception {

        when(mock.post()).thenReturn(Response.status(Status.UNAUTHORIZED).build());
        client.initVitamProcess(CONTEXTID, CONTAINERID, WORKFLOWID);

    }

    @Test(expected = VitamClientException.class)
    public void givenInitOperationInternalServerErrorThenThrowVitamClientInternalException()
        throws Exception {

        when(mock.post()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        client.initVitamProcess(CONTEXTID, CONTAINERID, WORKFLOWID);

    }

    @Test(expected = VitamClientException.class)
    public void givenInitOperationNotFoundThenThrowVitamClientInternalException()
        throws Exception {

        when(mock.post()).thenReturn(Response.status(Status.NOT_FOUND).build());
        client.initVitamProcess(CONTEXTID, CONTAINERID, WORKFLOWID);

    }

    @Test(expected = VitamClientException.class)
    public void givenInitOperationPreConditionFailedThenThrowVitamClientInternalException()
        throws Exception {

        when(mock.post()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        client.initVitamProcess(CONTEXTID, CONTAINERID, WORKFLOWID);

    }

    @Test(expected = VitamClientException.class)
    public void givenUnauthorizedInitWorkFlowThenThrowVitamClientInternalException()
        throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.UNAUTHORIZED).build());
        client.initWorkFlow(CONTEXTID);

    }

    @Test
    public void givenInitWorkFlowThenReturnResponseAccepted()
        throws Exception {

        when(mock.post()).thenReturn(Response.status(Status.ACCEPTED).build());
        client.initWorkFlow(CONTEXTID);

    }

    @Test(expected = VitamClientException.class)
    public void givenInitWorkFlowNotFoundThenThrowVitamClientInternalException()
        throws Exception {

        when(mock.post()).thenReturn(Response.status(Status.NOT_FOUND).build());
        client.initWorkFlow(CONTEXTID);

    }

    @Test(expected = VitamClientException.class)
    public void givenInitWorkFlowInternalServerErrorThenThrowVitamClientInternalException()
        throws Exception {

        when(mock.post()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        client.initWorkFlow(CONTEXTID);

    }


    @Test(expected = VitamClientException.class)
    public void givenPostOperationStatusThenThrowVitamClientInternalException() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.NOT_FOUND).build());
        client.executeOperationProcess(ID, null, CONTEXTID, ProcessAction.START.getValue());
    }

    @Test(expected = VitamClientException.class)
    public void givenUnauthorizedExecuteOperationThenThrowVitamClientInternalException()
        throws Exception {

        when(mock.post()).thenReturn(Response.status(Status.UNAUTHORIZED).build());
        client.executeOperationProcess(ID, null, CONTEXTID, ProcessAction.START.getValue());

    }

    @Test
    public void givenAcceptedExecuteOperationThenReturnResponseAccepted()
        throws Exception {

        when(mock.post()).thenReturn(Response.status(Status.ACCEPTED).build());
        client.executeOperationProcess(ID, null, CONTEXTID, ProcessAction.START.getValue());

    }

    @Test(expected = VitamClientException.class)
    public void givenInternalServerErrorExecuteOperationThenThrowVitamClientInternalException()
        throws Exception {

        when(mock.post()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        client.executeOperationProcess(ID, null, CONTEXTID, ProcessAction.START.getValue());

    }

    @Test(expected = VitamClientException.class)
    public void givenPreconditionFailedExecuteOperationThenReturnResponseAccepted()
        throws Exception {

        when(mock.post()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        client.executeOperationProcess(ID, null, CONTEXTID, ProcessAction.START.getValue());

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
    public void givenNotFoundWhenDefinitionsWorkflowThenReturnVitamError() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        RequestResponse<WorkFlow> requestReponse = client.getWorkflowDefinitions();
        assertEquals(Status.NOT_FOUND.getStatusCode(), requestReponse.getHttpCode());
    }

    @Test
    public void givenPreconditionFailedWhenDefinitionsWorkflowThenReturnVitamError() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        RequestResponse<WorkFlow> requestReponse = client.getWorkflowDefinitions();
        assertEquals(Status.PRECONDITION_FAILED.getStatusCode(), requestReponse.getHttpCode());
    }

    @Test
    public void givenUnauthaurizedWhenDefinitionsWorkflowThenReturnVitamError() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.UNAUTHORIZED).build());
        RequestResponse<WorkFlow> requestReponse = client.getWorkflowDefinitions();
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), requestReponse.getHttpCode());
    }

    @Test
    public void givenInternalServerErrorWhenDefinitionsWorkflowThenReturnVitamError() throws Exception {
        when(mock.get()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        RequestResponse<WorkFlow> requestReponse = client.getWorkflowDefinitions();
        assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), requestReponse.getHttpCode());
    }

}

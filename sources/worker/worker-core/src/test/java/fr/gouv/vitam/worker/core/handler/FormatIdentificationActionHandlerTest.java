package fr.gouv.vitam.worker.core.handler;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.AbstractMockClient.FakeInboundResponse;
import fr.gouv.vitam.common.format.identification.FormatIdentifierFactory;
import fr.gouv.vitam.common.format.identification.exception.FileFormatNotFoundException;
import fr.gouv.vitam.common.format.identification.exception.FormatIdentifierFactoryException;
import fr.gouv.vitam.common.format.identification.exception.FormatIdentifierNotFoundException;
import fr.gouv.vitam.common.format.identification.exception.FormatIdentifierTechnicalException;
import fr.gouv.vitam.common.format.identification.model.FormatIdentifierResponse;
import fr.gouv.vitam.common.format.identification.siegfried.FormatIdentifierSiegfried;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.processing.common.parameter.DefaultWorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.core.impl.HandlerIOImpl;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.net.ssl.*")
@PrepareForTest({FormatIdentifierFactory.class, WorkspaceClientFactory.class, AdminManagementClientFactory.class})
public class FormatIdentificationActionHandlerTest {

    FormatIdentificationActionHandler handler;
    private static final String HANDLER_ID = "OG_OBJECTS_FORMAT_CHECK";

    private static final String OBJECT_GROUP = "storeObjectGroupHandler/aeaaaaaaaaaam7myaaaamakxfgivuryaaaaq.json";
    private static final String OBJECT_GROUP_2 = "storeObjectGroupHandler/afaaaaaaaaaam7myaaaamakxfgivuryaaaaq.json";
    private final InputStream objectGroup;
    private final InputStream objectGroup2;
    private WorkspaceClientFactory workspaceClientFactory;
    private WorkspaceClient workspaceClient;
    private HandlerIOImpl handlerIO;
    private GUID guid;

    public FormatIdentificationActionHandlerTest() throws FileNotFoundException {
        objectGroup = PropertiesUtils.getResourceAsStream(OBJECT_GROUP);
        objectGroup2 = PropertiesUtils.getResourceAsStream(OBJECT_GROUP_2);
    }

    @Before
    public void setUp() {
        PowerMockito.mockStatic(FormatIdentifierFactory.class);
        PowerMockito.mockStatic(WorkspaceClientFactory.class);
        workspaceClientFactory = mock(WorkspaceClientFactory.class);
        PowerMockito.when(WorkspaceClientFactory.getInstance()).thenReturn(workspaceClientFactory);
        workspaceClient = mock(WorkspaceClient.class);
        PowerMockito.when(WorkspaceClientFactory.getInstance().getClient()).thenReturn(workspaceClient);
        PowerMockito.mockStatic(AdminManagementClientFactory.class);
        guid = GUIDFactory.newGUID();
        handlerIO = new HandlerIOImpl(guid.getId(), "workerId");
        deleteFiles();
    }

    @After
    public void setDown() {
        deleteFiles();
    }

    @Test
    public void getFormatIdentifierNotFound() throws Exception {
        final FormatIdentifierFactory identifierFactory = PowerMockito.mock(FormatIdentifierFactory.class);
        when(FormatIdentifierFactory.getInstance()).thenReturn(identifierFactory);
        when(identifierFactory.getFormatIdentifierFor(anyObject()))
            .thenThrow(new FormatIdentifierNotFoundException(""));
        handler = new FormatIdentificationActionHandler();
        final WorkerParameters params = getDefaultWorkerParameters();

        final ItemStatus response = handler.execute(params, handlerIO);
        assertEquals(StatusCode.FATAL, response.getGlobalStatus());
    }

    @Test
    public void getFormatIdentifierFactoryError() throws Exception {
        final FormatIdentifierFactory identifierFactory = PowerMockito.mock(FormatIdentifierFactory.class);
        when(FormatIdentifierFactory.getInstance()).thenReturn(identifierFactory);
        handler = new FormatIdentificationActionHandler();
        final WorkerParameters params = getDefaultWorkerParameters();

        when(identifierFactory.getFormatIdentifierFor(anyObject())).thenThrow(new FormatIdentifierFactoryException(""));
        final ItemStatus response = handler.execute(params, handlerIO);
        assertEquals(StatusCode.FATAL, response.getGlobalStatus());
    }

    @Test
    public void getFormatIdentifierTechnicalError() throws Exception {
        final FormatIdentifierFactory identifierFactory = PowerMockito.mock(FormatIdentifierFactory.class);
        when(FormatIdentifierFactory.getInstance()).thenReturn(identifierFactory);
        handler = new FormatIdentificationActionHandler();
        final WorkerParameters params = getDefaultWorkerParameters();

        when(identifierFactory.getFormatIdentifierFor(anyObject()))
            .thenThrow(new FormatIdentifierTechnicalException(""));
        final ItemStatus response = handler.execute(params, handlerIO);
        assertEquals(StatusCode.FATAL, response.getGlobalStatus());
    }

    @Test
    public void gettingJsonFromWorkspaceError() throws Exception {
        final FormatIdentifierSiegfried siegfried = getMockedFormatIdentifierSiegfried();

        when(workspaceClient.getObject(anyObject(), anyObject()))
            .thenThrow(new ContentAddressableStorageNotFoundException(""));
        handler = new FormatIdentificationActionHandler();
        final WorkerParameters params = getDefaultWorkerParameters();

        final ItemStatus response = handler.execute(params, handlerIO);
        assertEquals(StatusCode.FATAL, response.getGlobalStatus());
        deleteFiles();
    }

    @Test
    public void formatNotFoundInInternalReferential() throws Exception {
        final FormatIdentifierSiegfried siegfried =
            getMockedFormatIdentifierSiegfried();

        when(siegfried.analysePath(anyObject())).thenReturn(getFormatIdentifierResponseList());
        when(workspaceClient.getObject(anyObject(), anyObject()))
            .thenReturn(new FakeInboundResponse(Status.OK, objectGroup, null, null))
            .thenReturn(new FakeInboundResponse(Status.OK, IOUtils.toInputStream("VitamTest"), null, null))
            .thenReturn(new FakeInboundResponse(Status.OK, IOUtils.toInputStream("VitamTest"), null, null))
            .thenReturn(new FakeInboundResponse(Status.OK, IOUtils.toInputStream("VitamTest"), null, null))
            .thenReturn(new FakeInboundResponse(Status.OK, IOUtils.toInputStream("VitamTest"), null, null));

        final AdminManagementClient adminManagementClient = getMockedAdminManagementClient();
        when(adminManagementClient.getFormats(anyObject())).thenReturn(getAdminManagementJson2Result());

        handler = new FormatIdentificationActionHandler();
        final WorkerParameters params = getDefaultWorkerParameters();

        final ItemStatus response = handler.execute(params, handlerIO);
        assertEquals(StatusCode.KO, response.getGlobalStatus());
    }

    private AdminManagementClient getMockedAdminManagementClient() {
        final AdminManagementClient adminManagementClient = mock(AdminManagementClient.class);
        final AdminManagementClientFactory adminManagementClientFactory =
            PowerMockito.mock(AdminManagementClientFactory.class);
        when(AdminManagementClientFactory.getInstance()).thenReturn(adminManagementClientFactory);
        when(adminManagementClientFactory.getClient()).thenReturn(adminManagementClient);
        return adminManagementClient;
    }

    @Test
    public void formatIdentificationWarning() throws Exception {
        final FormatIdentifierSiegfried siegfried = getMockedFormatIdentifierSiegfried();

        when(siegfried.analysePath(anyObject())).thenReturn(getFormatIdentifierResponseList());

        assertEquals(FormatIdentificationActionHandler.getId(), HANDLER_ID);
        when(workspaceClient.getObject(anyObject(), anyObject()))
            .thenReturn(new FakeInboundResponse(Status.OK, objectGroup, null, null))
            .thenReturn(new FakeInboundResponse(Status.OK, IOUtils.toInputStream("VitamTest"), null, null))
            .thenReturn(new FakeInboundResponse(Status.OK, IOUtils.toInputStream("VitamTest"), null, null))
            .thenReturn(new FakeInboundResponse(Status.OK, IOUtils.toInputStream("VitamTest"), null, null))
            .thenReturn(new FakeInboundResponse(Status.OK, IOUtils.toInputStream("VitamTest"), null, null));
        doNothing().when(workspaceClient).putObject(anyObject(), anyObject(), anyObject());

        final AdminManagementClient adminManagementClient =
            getMockedAdminManagementClient();

        when(adminManagementClient.getFormats(anyObject())).thenReturn(getAdminManagementJson());

        handler = new FormatIdentificationActionHandler();
        final WorkerParameters params = getDefaultWorkerParameters();

        final ItemStatus response = handler.execute(params, handlerIO);
        assertEquals(StatusCode.WARNING, response.getGlobalStatus());
    }

    @Test
    public void formatIdentificationWithoutFormat() throws Exception {
        final FormatIdentifierSiegfried siegfried = getMockedFormatIdentifierSiegfried();

        when(siegfried.analysePath(anyObject())).thenReturn(getFormatIdentifierResponseList());

        when(workspaceClient.getObject(anyObject(), anyObject()))
            .thenReturn(new FakeInboundResponse(Status.OK, objectGroup, null, null))
            .thenReturn(new FakeInboundResponse(Status.OK, IOUtils.toInputStream("VitamTest"), null, null))
            .thenReturn(new FakeInboundResponse(Status.OK, IOUtils.toInputStream("VitamTest"), null, null))
            .thenReturn(new FakeInboundResponse(Status.OK, IOUtils.toInputStream("VitamTest"), null, null))
            .thenReturn(new FakeInboundResponse(Status.OK, IOUtils.toInputStream("VitamTest"), null, null));
        doNothing().when(workspaceClient).putObject(anyObject(), anyObject(), anyObject());

        final AdminManagementClient adminManagementClient =
            getMockedAdminManagementClient();

        when(adminManagementClient.getFormats(anyObject())).thenReturn(getAdminManagementJson());

        handler = new FormatIdentificationActionHandler();
        final WorkerParameters params = getDefaultWorkerParameters();

        final ItemStatus response = handler.execute(params, handlerIO);
        assertEquals(StatusCode.WARNING, response.getGlobalStatus());
    }

    @Test
    public void formatIdentificationNotFound() throws Exception {
        final FormatIdentifierSiegfried siegfried =
            getMockedFormatIdentifierSiegfried();

        when(siegfried.analysePath(anyObject())).thenThrow(new FileFormatNotFoundException(""));

        when(workspaceClient.getObject(anyObject(), anyObject()))
            .thenReturn(new FakeInboundResponse(Status.OK, objectGroup, null, null))
            .thenReturn(new FakeInboundResponse(Status.OK, IOUtils.toInputStream("VitamTest"), null, null))
            .thenReturn(new FakeInboundResponse(Status.OK, IOUtils.toInputStream("VitamTest"), null, null))
            .thenReturn(new FakeInboundResponse(Status.OK, IOUtils.toInputStream("VitamTest"), null, null))
            .thenReturn(new FakeInboundResponse(Status.OK, IOUtils.toInputStream("VitamTest"), null, null));

        handler = new FormatIdentificationActionHandler();
        final WorkerParameters params = getDefaultWorkerParameters();

        final ItemStatus response = handler.execute(params, handlerIO);
        assertEquals(StatusCode.KO, response.getGlobalStatus());
    }

    // TODO : CORRECT THIS TEST
    @Ignore
    @Test
    public void formatIdentificationReferentialException() throws Exception {
        final FormatIdentifierSiegfried siegfried =
            getMockedFormatIdentifierSiegfried();

        when(siegfried.analysePath(anyObject())).thenReturn(getFormatIdentifierResponseList());

        final AdminManagementClient adminManagementClient =
            getMockedAdminManagementClient();

        when(adminManagementClient.getFormats(anyObject())).thenThrow(new ReferentialException(""));

        when(workspaceClient.getObject(anyObject(), anyObject()))
            .thenReturn(new FakeInboundResponse(Status.OK, objectGroup, null, null))
            .thenReturn(new FakeInboundResponse(Status.OK, IOUtils.toInputStream("VitamTest"), null, null));

        handler = new FormatIdentificationActionHandler();
        final WorkerParameters params = getDefaultWorkerParameters();

        final ItemStatus response = handler.execute(params, handlerIO);
        assertEquals(StatusCode.FATAL, response.getGlobalStatus());
    }

    @Test
    public void formatIdentificationTechnicalException() throws Exception {
        final FormatIdentifierSiegfried siegfried =
            getMockedFormatIdentifierSiegfried();

        when(siegfried.analysePath(anyObject())).thenThrow(new FormatIdentifierTechnicalException(""));

        when(workspaceClient.getObject(anyObject(), anyObject()))
            .thenReturn(new FakeInboundResponse(Status.OK, objectGroup, null, null))
            .thenReturn(new FakeInboundResponse(Status.OK, IOUtils.toInputStream("VitamTest"), null, null))
            .thenReturn(new FakeInboundResponse(Status.OK, IOUtils.toInputStream("VitamTest"), null, null))
            .thenReturn(new FakeInboundResponse(Status.OK, IOUtils.toInputStream("VitamTest"), null, null));
        handler = new FormatIdentificationActionHandler();
        final WorkerParameters params = getDefaultWorkerParameters();

        final ItemStatus response = handler.execute(params, handlerIO);
        assertEquals(StatusCode.FATAL, response.getGlobalStatus());
    }

    @Test
    public void formatIdentificationFileIdentifierDoesNotRespond() throws Exception {
        final FormatIdentifierSiegfried siegfried = getMockedFormatIdentifierSiegfried();

        when(siegfried.analysePath(anyObject())).thenThrow(new FormatIdentifierNotFoundException(""));

        when(workspaceClient.getObject(anyObject(), anyObject()))
            .thenReturn(new FakeInboundResponse(Status.OK, objectGroup, null, null))
            .thenReturn(new FakeInboundResponse(Status.OK, IOUtils.toInputStream("VitamTest"), null, null))
            .thenReturn(new FakeInboundResponse(Status.OK, IOUtils.toInputStream("VitamTest"), null, null))
            .thenReturn(new FakeInboundResponse(Status.OK, IOUtils.toInputStream("VitamTest"), null, null))
            .thenReturn(new FakeInboundResponse(Status.OK, IOUtils.toInputStream("VitamTest"), null, null));

        handler = new FormatIdentificationActionHandler();
        final WorkerParameters params = getDefaultWorkerParameters();

        final ItemStatus response = handler.execute(params, handlerIO);
        assertEquals(StatusCode.FATAL, response.getGlobalStatus());
    }

    private FormatIdentifierSiegfried getMockedFormatIdentifierSiegfried()
        throws FormatIdentifierNotFoundException, FormatIdentifierFactoryException, FormatIdentifierTechnicalException {
        final FormatIdentifierSiegfried siegfried = mock(FormatIdentifierSiegfried.class);
        final FormatIdentifierFactory identifierFactory = PowerMockito.mock(FormatIdentifierFactory.class);
        when(FormatIdentifierFactory.getInstance()).thenReturn(identifierFactory);
        when(identifierFactory.getFormatIdentifierFor(anyObject())).thenReturn(siegfried);
        return siegfried;
    }

    private DefaultWorkerParameters getDefaultWorkerParameters() {
        return WorkerParametersFactory.newWorkerParameters("pId", "stepId", guid.getId(),
            "currentStep", "objName", "metadataURL", "workspaceURL");
    }

    private List<FormatIdentifierResponse> getFormatIdentifierResponseList() {
        final List<FormatIdentifierResponse> list = new ArrayList<>();
        list.add(new FormatIdentifierResponse("OpenDocument Presentation", "application/vnd.oasis.opendocument" +
            ".presentation",
            "fmt/293", "pronom"));
        return list;
    }

    private JsonNode getAdminManagementJson() {
        final ObjectNode node = JsonHandler.createObjectNode();
        node.put("PUID", "fmt/293");
        node.put("Name", "OpenDocument Presentation");
        node.put("MIMEType", "application/vnd.oasis.opendocument");
        final ArrayNode ret = JsonHandler.createArrayNode();
        ret.add(node);
        final ObjectNode result = JsonHandler.createObjectNode();
        result.set("$results", ret);
        return result;
    }

    private JsonNode getAdminManagementJson2Result() {
        final ObjectNode node2 = JsonHandler.createObjectNode();
        return node2;
    }

    private void deleteFiles() {
        final String fileName1 = "containerNameobjNameaeaaaaaaaaaam7myaaaamakxfgivurqaaaaq";
        final String fileName2 = "containerNameobjNameaeaaaaaaaaaam7myaaaamakxfgivuuiaaaaq";
        final String fileName3 = "containerNameobjNameaeaaaaaaaaaam7myaaaamakxfgivuuyaaaaq";
        final String fileName4 = "containerNameobjNameaeaaaaaaaaaam7myaaaamakxfgivuvaaaaaq";
        File file = new File(VitamConfiguration.getVitamTmpFolder() + "/" + fileName1);
        if (file.exists()) {
            file.delete();
        }
        file = new File(VitamConfiguration.getVitamTmpFolder() + "/" + fileName2);
        if (file.exists()) {
            file.delete();
        }
        file = new File(VitamConfiguration.getVitamTmpFolder() + "/" + fileName3);
        if (file.exists()) {
            file.delete();
        }
        file = new File(VitamConfiguration.getVitamTmpFolder() + "/" + fileName4);
        if (file.exists()) {
            file.delete();
        }
        handlerIO.partialClose();
    }
}

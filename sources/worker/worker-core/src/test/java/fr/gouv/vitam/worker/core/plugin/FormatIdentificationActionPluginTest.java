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

package fr.gouv.vitam.worker.core.plugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
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

import com.google.common.collect.Lists;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.CharsetUtils;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.AbstractMockClient.FakeInboundResponse;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
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
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.FileFormatModel;
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
public class FormatIdentificationActionPluginTest {
    FormatIdentificationActionPlugin plugin;
    private static final String HANDLER_ID = "OG_OBJECTS_FORMAT_CHECK";

    private static final String OBJECT_GROUP = "storeObjectGroupHandler/aeaaaaaaaaaam7myaaaamakxfgivuryaaaaq.json";
    private static final String OBJECT_GROUP_2 = "storeObjectGroupHandler/afaaaaaaaaaam7myaaaamakxfgivuryaaaaq.json";
    private static final String OBJECT_GROUP_3 =
        "formatIdentificationActionPlugin/aebaaaaaaaakwtamaai7cak32lvlyoyaaaba.json";
    private final InputStream objectGroup;
    private final InputStream objectGroup2;
    private final InputStream objectGroup3;
    private final JsonNode og;
    private final JsonNode og3;
    private WorkspaceClientFactory workspaceClientFactory;
    private WorkspaceClient workspaceClient;
    private HandlerIOImpl handlerIO;
    private GUID guid;

    public FormatIdentificationActionPluginTest() throws FileNotFoundException, InvalidParseOperationException {
        objectGroup = PropertiesUtils.getResourceAsStream(OBJECT_GROUP);
        objectGroup2 = PropertiesUtils.getResourceAsStream(OBJECT_GROUP_2);
        objectGroup3 = PropertiesUtils.getResourceAsStream(OBJECT_GROUP_3);
        
        og = JsonHandler.getFromInputStream(objectGroup);
        og3 = JsonHandler.getFromInputStream(objectGroup3);
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

        plugin = new FormatIdentificationActionPlugin();
        final WorkerParameters params = getDefaultWorkerParameters();

        final ItemStatus response = plugin.execute(params, handlerIO);
        assertEquals(StatusCode.FATAL, response.getGlobalStatus());
    }

    @Test
    public void getFormatIdentifierFactoryError() throws Exception {
        final FormatIdentifierFactory identifierFactory = PowerMockito.mock(FormatIdentifierFactory.class);
        when(FormatIdentifierFactory.getInstance()).thenReturn(identifierFactory);
        plugin = new FormatIdentificationActionPlugin();
        final WorkerParameters params = getDefaultWorkerParameters();

        when(identifierFactory.getFormatIdentifierFor(anyObject())).thenThrow(new FormatIdentifierFactoryException(""));
        final ItemStatus response = plugin.execute(params, handlerIO);
        assertEquals(StatusCode.FATAL, response.getGlobalStatus());
    }

    @Test
    public void getFormatIdentifierTechnicalError() throws Exception {
        final FormatIdentifierFactory identifierFactory = PowerMockito.mock(FormatIdentifierFactory.class);
        when(FormatIdentifierFactory.getInstance()).thenReturn(identifierFactory);
        plugin = new FormatIdentificationActionPlugin();
        final WorkerParameters params = getDefaultWorkerParameters();

        when(identifierFactory.getFormatIdentifierFor(anyObject()))
            .thenThrow(new FormatIdentifierTechnicalException(""));
        final ItemStatus response = plugin.execute(params, handlerIO);
        assertEquals(StatusCode.FATAL, response.getGlobalStatus());
    }

    @Test
    public void formatNotFoundInInternalReferential() throws Exception {
        final FormatIdentifierSiegfried siegfried =
            getMockedFormatIdentifierSiegfried();

        when(siegfried.analysePath(anyObject())).thenReturn(getFormatIdentifierResponseList());
        handlerIO.getInput().clear();
        handlerIO.getInput().add(og);

        final AdminManagementClient adminManagementClient = getMockedAdminManagementClient();
        when(adminManagementClient.getFormats(anyObject())).thenReturn(getAdminManagementJson2Result());

        plugin = new FormatIdentificationActionPlugin();
        final WorkerParameters params = getDefaultWorkerParameters();

        final ItemStatus response = plugin.execute(params, handlerIO);
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

        handlerIO.getInput().clear();
        handlerIO.getInput().add(og);

        final AdminManagementClient adminManagementClient =
            getMockedAdminManagementClient();

        when(adminManagementClient.getFormats(anyObject())).thenReturn(getAdminManagementJson());

        plugin = new FormatIdentificationActionPlugin();
        final WorkerParameters params = getDefaultWorkerParameters();

        final ItemStatus response = plugin.execute(params, handlerIO);
        assertEquals(StatusCode.WARNING, response.getGlobalStatus());
        assertFalse(response.getItemsStatus().get(FormatIdentificationActionPlugin.FILE_FORMAT)
            .getEvDetailData().isEmpty());
        assertTrue(response.getItemsStatus().get(FormatIdentificationActionPlugin.FILE_FORMAT)
            .getEvDetailData().contains("Plain Text File"));
    }

    @Test
    public void formatIdentificationWithPhysicalDataObject() throws Exception {
        final FormatIdentifierSiegfried siegfried = getMockedFormatIdentifierSiegfried();

        when(siegfried.analysePath(anyObject())).thenReturn(getFormatIdentifierResponseList());

        handlerIO.getInput().clear();
        handlerIO.getInput().add(og3);

        final AdminManagementClient adminManagementClient =
            getMockedAdminManagementClient();

        when(adminManagementClient.getFormats(anyObject())).thenReturn(getAdminManagementJson());

        plugin = new FormatIdentificationActionPlugin();
        final WorkerParameters params = getDefaultWorkerParameters();

        final ItemStatus response = plugin.execute(params, handlerIO);
        assertEquals(StatusCode.WARNING, response.getGlobalStatus());
    }

    @Test
    public void formatIdentificationWithoutFormat() throws Exception {
        final FormatIdentifierSiegfried siegfried = getMockedFormatIdentifierSiegfried();

        when(siegfried.analysePath(anyObject())).thenReturn(getFormatIdentifierResponseList());

        handlerIO.getInput().clear();
        handlerIO.getInput().add(og);

        final AdminManagementClient adminManagementClient =
            getMockedAdminManagementClient();

        when(adminManagementClient.getFormats(anyObject())).thenReturn(getAdminManagementJson());

        plugin = new FormatIdentificationActionPlugin();
        final WorkerParameters params = getDefaultWorkerParameters();

        final ItemStatus response = plugin.execute(params, handlerIO);
        assertEquals(StatusCode.WARNING, response.getGlobalStatus());
    }

    @Test
    public void formatIdentificationNotFound() throws Exception {
        final FormatIdentifierSiegfried siegfried =
            getMockedFormatIdentifierSiegfried();

        when(siegfried.analysePath(anyObject())).thenThrow(new FileFormatNotFoundException(""));

        handlerIO.getInput().clear();
        handlerIO.getInput().add(og);

        plugin = new FormatIdentificationActionPlugin();
        final WorkerParameters params = getDefaultWorkerParameters();

        final ItemStatus response = plugin.execute(params, handlerIO);
        assertEquals(StatusCode.KO, response.getGlobalStatus());
        assertTrue(response.getItemsStatus().get(FormatIdentificationActionPlugin.FILE_FORMAT)
            .getEvDetailData().equals( "{}" ));
    }

    @Test
    public void formatIdentificationReferentialException() throws Exception {
        final FormatIdentifierSiegfried siegfried =
            getMockedFormatIdentifierSiegfried();

        when(siegfried.analysePath(anyObject())).thenReturn(getFormatIdentifierResponseList());

        handlerIO.getInput().clear();
        handlerIO.getInput().add(og);

        final AdminManagementClient adminManagementClient =
            getMockedAdminManagementClient();

        when(adminManagementClient.getFormats(anyObject()))
            .thenThrow(new ReferentialException("Test Referential Exception"));

        plugin = new FormatIdentificationActionPlugin();
        final WorkerParameters params = getDefaultWorkerParameters();

        final ItemStatus response = plugin.execute(params, handlerIO);
        assertEquals(StatusCode.KO, response.getGlobalStatus());
    }

    @Test
    public void formatIdentificationTechnicalException() throws Exception {
        final FormatIdentifierSiegfried siegfried =
            getMockedFormatIdentifierSiegfried();

        when(siegfried.analysePath(anyObject())).thenThrow(new FormatIdentifierTechnicalException(""));

        handlerIO.getInput().clear();
        handlerIO.getInput().add(og);
        plugin = new FormatIdentificationActionPlugin();
        final WorkerParameters params = getDefaultWorkerParameters();

        final ItemStatus response = plugin.execute(params, handlerIO);
        assertEquals(StatusCode.FATAL, response.getGlobalStatus());
    }

    @Test
    public void formatIdentificationFileIdentifierDoesNotRespond() throws Exception {
        final FormatIdentifierSiegfried siegfried = getMockedFormatIdentifierSiegfried();

        when(siegfried.analysePath(anyObject())).thenThrow(new FormatIdentifierNotFoundException(""));

        handlerIO.getInput().clear();
        handlerIO.getInput().add(og);

        plugin = new FormatIdentificationActionPlugin();
        final WorkerParameters params = getDefaultWorkerParameters();

        final ItemStatus response = plugin.execute(params, handlerIO);
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
            "currentStep", Lists.newArrayList("objName"), "metadataURL", "workspaceURL");
    }

    private List<FormatIdentifierResponse> getFormatIdentifierResponseList() {
        final List<FormatIdentifierResponse> list = new ArrayList<>();
        list.add(new FormatIdentifierResponse("OpenDocument Presentation", "application/vnd.oasis.opendocument" +
            ".presentation",
            "fmt/293", "pronom"));
        return list;
    }

    private RequestResponse<FileFormatModel> getAdminManagementJson() {

        RequestResponseOK<FileFormatModel> responseOK = new RequestResponseOK<>();
        FileFormatModel fileFormatModel = new FileFormatModel();
        fileFormatModel.setPuid("fmt/293");
        fileFormatModel.setName("OpenDocument Presentation");
        fileFormatModel.setMimeType("application/vnd.oasis.opendocument");
        responseOK.getResults().add(fileFormatModel);

        return responseOK;
    }

    private RequestResponse<FileFormatModel> getAdminManagementJson2Result() {
        return new RequestResponseOK<>();
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

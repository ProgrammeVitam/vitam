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
package fr.gouv.vitam.worker.core.plugin;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.format.identification.FormatIdentifier;
import fr.gouv.vitam.common.format.identification.FormatIdentifierFactory;
import fr.gouv.vitam.common.format.identification.exception.FileFormatNotFoundException;
import fr.gouv.vitam.common.format.identification.exception.FormatIdentifierFactoryException;
import fr.gouv.vitam.common.format.identification.exception.FormatIdentifierNotFoundException;
import fr.gouv.vitam.common.format.identification.exception.FormatIdentifierTechnicalException;
import fr.gouv.vitam.common.format.identification.model.FormatIdentifierResponse;
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
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.processing.common.parameter.DefaultWorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.core.impl.HandlerIOImpl;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FormatIdentificationActionPluginTest {

    private FormatIdentificationActionPlugin plugin;
    private static final String FILE_FORMAT = "FILE_FORMAT";

    private static final String OBJECT_GROUP = "storeObjectGroupHandler/aeaaaaaaaaaam7myaaaamakxfgivuryaaaaq.json";
    private static final String OBJECT_GROUP_2 = "storeObjectGroupHandler/afaaaaaaaaaam7myaaaamakxfgivuryaaaaq.json";
    private static final String OBJECT_GROUP_3 =
        "formatIdentificationActionPlugin/aebaaaaaaaakwtamaai7cak32lvlyoyaaaba.json";

    private static final String REFERENTIAL_INGEST_CONTRACT_DEFAULT_CONFIG =
        "formatIdentificationActionPlugin/referentialIngestContractDefaultConfig.json";
    private static final String REFERENTIAL_INGEST_CONTRACT_FORMAT_UNIDENTIFIED_AUTHORIZED =
        "formatIdentificationActionPlugin/referentialIngestContractFormatUnidentifiedAuthorized.json";
    private static final String REFERENTIAL_INGEST_CONTRACT_RESTRICTED_FORMAT_LIST_KO =
        "formatIdentificationActionPlugin/referentialIngestContractRestrictedFormatListKO.json";
    private static final String REFERENTIAL_INGEST_CONTRACT_RESTRICTED_FORMAT_LIST_OK =
        "formatIdentificationActionPlugin/referentialIngestContractRestrictedFormatListOK.json";

    private final InputStream objectGroup;
    private final InputStream objectGroup3;
    private final JsonNode og;
    private final JsonNode og3;
    private WorkspaceClientFactory workspaceClientFactory;
    private WorkspaceClient workspaceClient;

    private FormatIdentifierFactory formatIdentifierFactory;
    private FormatIdentifier formatIdentifier;

    private AdminManagementClientFactory adminManagementClientFactory;
    private AdminManagementClient adminManagementClient;

    private HandlerIOImpl handlerIO;
    private GUID guid;

    public FormatIdentificationActionPluginTest() throws FileNotFoundException, InvalidParseOperationException {
        objectGroup = PropertiesUtils.getResourceAsStream(OBJECT_GROUP);
        objectGroup3 = PropertiesUtils.getResourceAsStream(OBJECT_GROUP_3);

        og = JsonHandler.getFromInputStream(objectGroup);
        og3 = JsonHandler.getFromInputStream(objectGroup3);
    }

    @Before
    public void setUp() throws Exception {
        workspaceClientFactory = mock(WorkspaceClientFactory.class);
        workspaceClient = mock(WorkspaceClient.class);
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);

        adminManagementClientFactory = mock(AdminManagementClientFactory.class);
        adminManagementClient = mock(AdminManagementClient.class);
        when(adminManagementClientFactory.getClient()).thenReturn(adminManagementClient);

        formatIdentifierFactory = mock(FormatIdentifierFactory.class);
        formatIdentifier = mock(FormatIdentifier.class);
        when(formatIdentifierFactory.getFormatIdentifierFor(any())).thenReturn(formatIdentifier);

        guid = GUIDFactory.newGUID();
        handlerIO = new HandlerIOImpl(
            workspaceClientFactory,
            mock(LogbookLifeCyclesClientFactory.class),
            guid.getId(),
            "workerId",
            Lists.newArrayList()
        );
        deleteFiles();
    }

    @After
    public void setDown() {
        deleteFiles();
    }

    @Test
    public void getFormatIdentifierNotFound() throws Exception {
        when(formatIdentifierFactory.getFormatIdentifierFor(any())).thenThrow(
            new FormatIdentifierNotFoundException("")
        );

        plugin = new FormatIdentificationActionPlugin(adminManagementClientFactory, formatIdentifierFactory);
        final WorkerParameters params = getDefaultWorkerParameters();

        handlerIO.getInput().add(null);
        handlerIO.getInput().add(PropertiesUtils.getResourceFile(REFERENTIAL_INGEST_CONTRACT_DEFAULT_CONFIG));

        final ItemStatus response = plugin.execute(params, handlerIO);
        assertEquals(StatusCode.FATAL, response.getGlobalStatus());
    }

    @Test
    public void getFormatIdentifierFactoryError() throws Exception {
        plugin = new FormatIdentificationActionPlugin(adminManagementClientFactory, formatIdentifierFactory);
        final WorkerParameters params = getDefaultWorkerParameters();
        when(formatIdentifierFactory.getFormatIdentifierFor(any())).thenThrow(new FormatIdentifierFactoryException(""));

        handlerIO.getInput().add(null);
        handlerIO.getInput().add(PropertiesUtils.getResourceFile(REFERENTIAL_INGEST_CONTRACT_DEFAULT_CONFIG));

        final ItemStatus response = plugin.execute(params, handlerIO);
        assertEquals(StatusCode.FATAL, response.getGlobalStatus());
    }

    @Test
    public void getFormatIdentifierTechnicalError() throws Exception {
        plugin = new FormatIdentificationActionPlugin(adminManagementClientFactory, formatIdentifierFactory);
        final WorkerParameters params = getDefaultWorkerParameters();

        when(formatIdentifierFactory.getFormatIdentifierFor(any())).thenThrow(
            new FormatIdentifierTechnicalException("")
        );

        handlerIO.getInput().add(null);
        handlerIO.getInput().add(PropertiesUtils.getResourceFile(REFERENTIAL_INGEST_CONTRACT_DEFAULT_CONFIG));

        final ItemStatus response = plugin.execute(params, handlerIO);
        assertEquals(StatusCode.FATAL, response.getGlobalStatus());
    }

    @Test
    public void formatNotFoundInInternalReferential() throws Exception {
        when(formatIdentifier.analysePath(any())).thenReturn(getFormatIdentifierResponseList());
        handlerIO.getInput().clear();
        handlerIO.getInput().add(og);
        handlerIO.getInput().add(PropertiesUtils.getResourceFile(REFERENTIAL_INGEST_CONTRACT_DEFAULT_CONFIG));

        when(adminManagementClient.getFormats(any())).thenReturn(getAdminManagementJson2Result());

        plugin = new FormatIdentificationActionPlugin(adminManagementClientFactory, formatIdentifierFactory);
        final WorkerParameters params = getDefaultWorkerParameters();

        final ItemStatus response = plugin.execute(params, handlerIO);
        assertEquals(StatusCode.KO, response.getGlobalStatus());
        assertTrue(response.getItemsStatus().containsKey(FILE_FORMAT));
        ItemStatus taskItemStatus = response.getItemsStatus().get(FILE_FORMAT);
        assertEquals(StatusCode.KO, taskItemStatus.getGlobalStatus());
        taskItemStatus
            .getSubTaskStatus()
            .values()
            .forEach(subTaskItemStatus -> {
                assertEquals(StatusCode.KO, subTaskItemStatus.getGlobalStatus());
                assertTrue(subTaskItemStatus.getGlobalOutcomeDetailSubcode().contains("UNCHARTED"));
            });
    }

    @Test
    public void formatIdentificationWarning() throws Exception {
        when(formatIdentifier.analysePath(any())).thenReturn(getFormatIdentifierResponseList());

        handlerIO.getInput().clear();
        handlerIO.getInput().add(og);
        handlerIO.getInput().add(PropertiesUtils.getResourceFile(REFERENTIAL_INGEST_CONTRACT_DEFAULT_CONFIG));

        when(adminManagementClient.getFormats(any())).thenReturn(getAdminManagementJson());

        plugin = new FormatIdentificationActionPlugin(adminManagementClientFactory, formatIdentifierFactory);
        final WorkerParameters params = getDefaultWorkerParameters();

        final ItemStatus response = plugin.execute(params, handlerIO);
        assertEquals(StatusCode.WARNING, response.getGlobalStatus());
        assertFalse(
            response.getItemsStatus().get(FormatIdentificationActionPlugin.FILE_FORMAT).getEvDetailData().isEmpty()
        );

        //check all different warning (only in case of PUID diff)
        LinkedHashMap<String, ItemStatus> subTaskStatusMap = response
            .getItemsStatus()
            .get(FormatIdentificationActionPlugin.FILE_FORMAT)
            .getSubTaskStatus();

        assertFalse(subTaskStatusMap.isEmpty());

        List<ItemStatus> subTaskWithWarningStatusList = subTaskStatusMap
            .values()
            .stream()
            .filter(item -> item.getGlobalStatus().equals(StatusCode.WARNING))
            .collect(Collectors.toList());
        assertThat(subTaskWithWarningStatusList).hasSize(3);
        subTaskWithWarningStatusList.forEach(objectItem -> {
            objectItem.getEvDetailData().contains("+ PUID : fmt/293");
        });
    }

    @Test
    public void formatIdentificationWithPhysicalDataObject() throws Exception {
        when(formatIdentifier.analysePath(any())).thenReturn(getFormatIdentifierResponseList());

        handlerIO.getInput().clear();
        handlerIO.getInput().add(og3);
        handlerIO.getInput().add(PropertiesUtils.getResourceFile(REFERENTIAL_INGEST_CONTRACT_DEFAULT_CONFIG));

        when(adminManagementClient.getFormats(any())).thenReturn(getAdminManagementJson());

        plugin = new FormatIdentificationActionPlugin(adminManagementClientFactory, formatIdentifierFactory);
        final WorkerParameters params = getDefaultWorkerParameters();

        final ItemStatus response = plugin.execute(params, handlerIO);
        assertEquals(StatusCode.WARNING, response.getGlobalStatus());
    }

    @Test
    public void formatIdentificationWithoutFormat() throws Exception {
        when(formatIdentifier.analysePath(any())).thenReturn(getFormatIdentifierResponseList());

        handlerIO.getInput().clear();
        handlerIO.getInput().add(og);
        handlerIO.getInput().add(PropertiesUtils.getResourceFile(REFERENTIAL_INGEST_CONTRACT_DEFAULT_CONFIG));

        when(adminManagementClient.getFormats(any())).thenReturn(getAdminManagementJson());

        plugin = new FormatIdentificationActionPlugin(adminManagementClientFactory, formatIdentifierFactory);
        final WorkerParameters params = getDefaultWorkerParameters();

        final ItemStatus response = plugin.execute(params, handlerIO);
        assertEquals(StatusCode.WARNING, response.getGlobalStatus());
    }

    @Test
    public void formatIdentificationNotFound() throws Exception {
        when(formatIdentifier.analysePath(any())).thenThrow(new FileFormatNotFoundException(""));

        handlerIO.getInput().clear();
        handlerIO.getInput().add(og);
        handlerIO.getInput().add(PropertiesUtils.getResourceFile(REFERENTIAL_INGEST_CONTRACT_DEFAULT_CONFIG));

        plugin = new FormatIdentificationActionPlugin(adminManagementClientFactory, formatIdentifierFactory);
        final WorkerParameters params = getDefaultWorkerParameters();

        final ItemStatus response = plugin.execute(params, handlerIO);
        assertEquals(StatusCode.KO, response.getGlobalStatus());
        assertTrue(response.getItemsStatus().containsKey(FILE_FORMAT));
        ItemStatus taskItemStatus = response.getItemsStatus().get(FILE_FORMAT);
        assertEquals(StatusCode.KO, taskItemStatus.getGlobalStatus());
        taskItemStatus
            .getSubTaskStatus()
            .values()
            .forEach(subTaskItemStatus -> {
                assertEquals(StatusCode.KO, subTaskItemStatus.getGlobalStatus());
                assertTrue(subTaskItemStatus.getGlobalOutcomeDetailSubcode().contains("UNKNOWN"));
            });
    }

    @Test
    public void formatUnidentifiedAuthorized() throws Exception {
        when(formatIdentifier.analysePath(any())).thenThrow(new FileFormatNotFoundException(""));

        handlerIO.getInput().clear();
        handlerIO.getInput().add(og);
        handlerIO
            .getInput()
            .add(PropertiesUtils.getResourceFile(REFERENTIAL_INGEST_CONTRACT_FORMAT_UNIDENTIFIED_AUTHORIZED));

        plugin = new FormatIdentificationActionPlugin(adminManagementClientFactory, formatIdentifierFactory);
        final WorkerParameters params = getDefaultWorkerParameters();

        final ItemStatus response = plugin.execute(params, handlerIO);
        assertEquals(StatusCode.WARNING, response.getGlobalStatus());
        assertTrue(response.getItemsStatus().containsKey(FILE_FORMAT));
        ItemStatus taskItemStatus = response.getItemsStatus().get(FILE_FORMAT);
        assertEquals(StatusCode.WARNING, taskItemStatus.getGlobalStatus());
        taskItemStatus
            .getSubTaskStatus()
            .values()
            .forEach(subTaskItemStatus -> {
                assertEquals(StatusCode.WARNING, subTaskItemStatus.getGlobalStatus());
            });
    }

    @Test
    public void formatUnidentifiedAuthorizedRestrictedFormatListKO() throws Exception {
        when(formatIdentifier.analysePath(any())).thenThrow(new FileFormatNotFoundException(""));

        handlerIO.getInput().clear();
        handlerIO.getInput().add(og);
        handlerIO
            .getInput()
            .add(PropertiesUtils.getResourceFile(REFERENTIAL_INGEST_CONTRACT_RESTRICTED_FORMAT_LIST_KO));

        plugin = new FormatIdentificationActionPlugin(adminManagementClientFactory, formatIdentifierFactory);
        final WorkerParameters params = getDefaultWorkerParameters();

        final ItemStatus response = plugin.execute(params, handlerIO);
        assertEquals(StatusCode.KO, response.getGlobalStatus());
        assertTrue(response.getItemsStatus().containsKey(FILE_FORMAT));
        ItemStatus taskItemStatus = response.getItemsStatus().get(FILE_FORMAT);
        assertEquals(StatusCode.KO, taskItemStatus.getGlobalStatus());
        assertEquals("REJECTED_FORMAT", taskItemStatus.getGlobalOutcomeDetailSubcode());
        taskItemStatus
            .getSubTaskStatus()
            .values()
            .forEach(subTaskItemStatus -> {
                assertEquals("REJECTED", subTaskItemStatus.getGlobalOutcomeDetailSubcode());
                assertEquals(StatusCode.KO, subTaskItemStatus.getGlobalStatus());
            });
    }

    @Test
    public void formatUnidentifiedAuthorizedRestrictedFormatListOK() throws Exception {
        final String puid[] = { "fmt/293", "fmt/18", "fmt/11", "x-fmt/111" };
        final String mimetype[] = {
            "application/vnd.oasis.opendocument.presentation",
            "application/pdf",
            "image/png",
            "text/plain",
        };
        final String literal[] = {
            "OpenDocument Presentation",
            "Acrobat PDF 1.4 - Portable Document Format",
            "Portable Network Graphics",
            "Plain Text File",
        };

        when(formatIdentifier.analysePath(any())).thenAnswer(
            new Answer() {
                private int count = 0;

                public List<FormatIdentifierResponse> answer(InvocationOnMock invocation) {
                    FormatIdentifierResponse response = mock(FormatIdentifierResponse.class);
                    when(response.getPuid()).thenReturn(puid[count]);
                    when(response.getMimetype()).thenReturn(mimetype[count]);
                    when(response.getFormatLiteral()).thenReturn(literal[count]);
                    when(response.getMatchedNamespace()).thenReturn("pronom");
                    ++count;
                    return new ArrayList<>(Arrays.asList(response));
                }
            }
        );

        when(adminManagementClient.getFormats(any())).thenAnswer(
            new Answer() {
                private int count = 0;

                public RequestResponse<FileFormatModel> answer(InvocationOnMock invocation) {
                    RequestResponseOK<FileFormatModel> response = new RequestResponseOK<>();
                    FileFormatModel fileFormat = mock(FileFormatModel.class);
                    when(fileFormat.getPuid()).thenReturn(puid[count]);
                    when(fileFormat.getName()).thenReturn(literal[count]);
                    when(fileFormat.getMimeType()).thenReturn(mimetype[count]);
                    ++count;
                    response.getResults().add(fileFormat);
                    return response;
                }
            }
        );

        handlerIO.getInput().clear();
        handlerIO.getInput().add(og);
        handlerIO
            .getInput()
            .add(PropertiesUtils.getResourceFile(REFERENTIAL_INGEST_CONTRACT_RESTRICTED_FORMAT_LIST_OK));

        plugin = new FormatIdentificationActionPlugin(adminManagementClientFactory, formatIdentifierFactory);
        final WorkerParameters params = getDefaultWorkerParameters();

        final ItemStatus response = plugin.execute(params, handlerIO);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
        assertTrue(response.getItemsStatus().containsKey(FILE_FORMAT));
        ItemStatus taskItemStatus = response.getItemsStatus().get(FILE_FORMAT);
        assertEquals(StatusCode.OK, taskItemStatus.getGlobalStatus());
        taskItemStatus
            .getSubTaskStatus()
            .values()
            .forEach(subTaskItemStatus -> {
                assertEquals(StatusCode.OK, subTaskItemStatus.getGlobalStatus());
            });
    }

    @Test
    public void formatIdentificationReferentialException() throws Exception {
        when(formatIdentifier.analysePath(any())).thenReturn(getFormatIdentifierResponseList());

        handlerIO.getInput().clear();
        handlerIO.getInput().add(og);
        handlerIO.getInput().add(PropertiesUtils.getResourceFile(REFERENTIAL_INGEST_CONTRACT_DEFAULT_CONFIG));
        when(adminManagementClient.getFormats(any())).thenThrow(new ReferentialException("Test Referential Exception"));

        plugin = new FormatIdentificationActionPlugin(adminManagementClientFactory, formatIdentifierFactory);
        final WorkerParameters params = getDefaultWorkerParameters();

        final ItemStatus response = plugin.execute(params, handlerIO);
        assertEquals(StatusCode.KO, response.getGlobalStatus());
        assertTrue(response.getItemsStatus().containsKey(FILE_FORMAT));
        ItemStatus taskItemStatus = response.getItemsStatus().get(FILE_FORMAT);
        assertEquals(StatusCode.KO, taskItemStatus.getGlobalStatus());
        taskItemStatus
            .getSubTaskStatus()
            .values()
            .forEach(subTaskItemStatus -> {
                assertEquals(StatusCode.KO, subTaskItemStatus.getGlobalStatus());
                assertTrue(subTaskItemStatus.getGlobalOutcomeDetailSubcode().contains("UNCHARTED"));
            });
    }

    @Test
    public void formatIdentificationTechnicalException() throws Exception {
        when(formatIdentifier.analysePath(any())).thenThrow(new FormatIdentifierTechnicalException(""));

        handlerIO.getInput().clear();
        handlerIO.getInput().add(og);
        handlerIO.getInput().add(PropertiesUtils.getResourceFile(REFERENTIAL_INGEST_CONTRACT_DEFAULT_CONFIG));

        plugin = new FormatIdentificationActionPlugin(adminManagementClientFactory, formatIdentifierFactory);
        final WorkerParameters params = getDefaultWorkerParameters();

        final ItemStatus response = plugin.execute(params, handlerIO);
        assertEquals(StatusCode.FATAL, response.getGlobalStatus());
    }

    @Test
    public void formatIdentificationFileIdentifierDoesNotRespond() throws Exception {
        when(formatIdentifier.analysePath(any())).thenThrow(new FormatIdentifierNotFoundException(""));

        handlerIO.getInput().clear();
        handlerIO.getInput().add(og);
        handlerIO.getInput().add(PropertiesUtils.getResourceFile(REFERENTIAL_INGEST_CONTRACT_DEFAULT_CONFIG));

        plugin = new FormatIdentificationActionPlugin(adminManagementClientFactory, formatIdentifierFactory);
        final WorkerParameters params = getDefaultWorkerParameters();

        final ItemStatus response = plugin.execute(params, handlerIO);
        assertEquals(StatusCode.FATAL, response.getGlobalStatus());
    }

    private DefaultWorkerParameters getDefaultWorkerParameters() {
        return WorkerParametersFactory.newWorkerParameters(
            "pId",
            "stepId",
            guid.getId(),
            "currentStep",
            Lists.newArrayList("objName"),
            "metadataURL",
            "workspaceURL"
        );
    }

    private List<FormatIdentifierResponse> getFormatIdentifierResponseList() {
        final List<FormatIdentifierResponse> list = new ArrayList<>();
        list.add(
            new FormatIdentifierResponse(
                "OpenDocument Presentation",
                "application/vnd.oasis.opendocument" + ".presentation",
                "fmt/293",
                "pronom"
            )
        );
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

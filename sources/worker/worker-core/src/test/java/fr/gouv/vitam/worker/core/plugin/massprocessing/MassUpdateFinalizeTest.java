/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.worker.core.plugin.massprocessing;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageAlreadyExistsClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MassUpdateFinalize tests
 */
public class MassUpdateFinalizeTest {

    private static final String MASS_UPDATE_FINALIZE = "MASS_UPDATE_FINALIZE";
    private static final int DISTRIBUTION_LOCAL_REPORTS_RANK = 0;
    private static final String REPORTS = "reports";
    private static final int TENANT_ID = 0;
    private static final String MASS_UPDATE_FINALIZE_OP_00_REPORT_SUCCESS_JSON =
        "massUpdateFinalize/op_00_report_success.json";
    private static final String MASS_UPDATE_FINALIZE_OP_01_REPORT_SUCCESS_JSON =
        "massUpdateFinalize/op_01_report_success.json";
    private static final String MASS_UPDATE_FINALIZE_OP_02_REPORT_SUCCESS_JSON =
        "massUpdateFinalize/op_02_report_success.json";
    private static final String OP_00_SUCCESS_JSON = "op_00_report_success.json";
    private static final String OP_01_SUCCESS_JSON = "op_01_report_success.json";
    private static final String OP_02_SUCCESS_JSON = "op_02_report_success.json";
    private static final String OP_00_REPORT_ERROR_JSON = "op_00_report_error.json";
    private static final String OP_01_REPORT_ERROR_JSON = "op_01_report_error.json";
    private static final String SEPARATOR = "/";
    public static final String MASS_UPDATE_FINALIZE_OP_00_REPORT_ERROR_JSON =
        "massUpdateFinalize/op_00_report_error.json";

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Mock
    private StorageClientFactory storageClientFactory;

    @Mock
    private WorkspaceClientFactory workspaceClientFactory;

    @Mock
    private StorageClient storageClient;

    @Mock
    private WorkspaceClient workspaceClient;

    @Mock
    private HandlerIO handler;

    @InjectMocks
    private MassUpdateFinalize massUpdateFinalize;

    private WorkerParameters parameters;
    private String operationId;

    @Before
    public void setup() {
        when(storageClientFactory.getClient()).thenReturn(storageClient);
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);
        when(handler.getInput(DISTRIBUTION_LOCAL_REPORTS_RANK)).thenReturn(REPORTS);
        operationId = GUIDFactory.newRequestIdGUID(TENANT_ID).toString();
        parameters =
            WorkerParametersFactory.newWorkerParameters().setUrlWorkspace("http://localhost:8080")
                .setUrlMetadata("http://localhost:8080").setObjectNameList(Lists.newArrayList("objectName"))
                .setObjectName("objectName.json").setCurrentStep("currentStep")
                .setContainerName(operationId)
                .setProcessId(operationId);
    }

    @Test
    @RunWithCustomExecutor
    public void shouldReturnOKWhenUpdateFinalizeTest()
        throws ContentAddressableStorageServerException, URISyntaxException, ProcessingException, IOException,
        ContentAddressableStorageNotFoundException, StorageNotFoundClientException, StorageServerClientException,
        StorageAlreadyExistsClientException, StorageNotFoundException {

        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        VitamThreadUtils.getVitamSession().setRequestId(operationId);
        List<URI> uris = Arrays.asList(
            new URI(OP_00_SUCCESS_JSON),
            new URI(OP_01_SUCCESS_JSON),
            new URI(OP_02_SUCCESS_JSON),
            new URI(OP_00_REPORT_ERROR_JSON),
            new URI(OP_01_REPORT_ERROR_JSON)
        );
        when(workspaceClient.getListUriDigitalObjectFromFolder(parameters.getContainerName(), REPORTS))
            .thenReturn(new RequestResponseOK().addResult(uris));
        File pathFile = tempFolder.newFile();
        given(handler.getNewLocalFile(any())).willReturn(pathFile);
        when(handler.getWorkerId()).thenReturn(operationId);

        File report = PropertiesUtils.getResourceFile(
            MASS_UPDATE_FINALIZE_OP_00_REPORT_SUCCESS_JSON);
        Response response =
            Response.ok(Files.newInputStream(report.toPath())).status(Response.Status.OK).build();
        when(workspaceClient.getObject(operationId, REPORTS + SEPARATOR + OP_00_SUCCESS_JSON)).thenReturn(response);
        File report_01 = PropertiesUtils.getResourceFile(
            MASS_UPDATE_FINALIZE_OP_01_REPORT_SUCCESS_JSON);
        Response response1 =
            Response.ok(Files.newInputStream(report_01.toPath())).status(Response.Status.OK).build();
        when(workspaceClient.getObject(operationId, REPORTS + SEPARATOR + OP_01_SUCCESS_JSON))
            .thenReturn(response1);
        File report_02 = PropertiesUtils.getResourceFile(
            MASS_UPDATE_FINALIZE_OP_02_REPORT_SUCCESS_JSON);
        Response response2 =
            Response.ok(Files.newInputStream(report_02.toPath())).status(Response.Status.OK).build();
        when(workspaceClient.getObject(operationId, REPORTS + SEPARATOR + OP_02_SUCCESS_JSON))
            .thenReturn(response2);

        File reportKo = PropertiesUtils.getResourceFile(
            MASS_UPDATE_FINALIZE_OP_00_REPORT_ERROR_JSON);
        Response responseKo =
            Response.ok(Files.newInputStream(reportKo.toPath())).status(Response.Status.OK).build();
        when(workspaceClient.getObject(operationId, REPORTS + SEPARATOR + OP_00_REPORT_ERROR_JSON))
            .thenReturn(responseKo);
        File reportKo1 = PropertiesUtils.getResourceFile(
            MASS_UPDATE_FINALIZE_OP_00_REPORT_ERROR_JSON);
        Response responseKo1 =
            Response.ok(Files.newInputStream(reportKo1.toPath())).status(Response.Status.OK).build();
        when(workspaceClient.getObject(operationId, REPORTS + SEPARATOR + OP_01_REPORT_ERROR_JSON))
            .thenReturn(responseKo1);

        // When
        ItemStatus itemStatus = massUpdateFinalize.execute(parameters, handler);

        // Then
        // reportOK & reportKO
        verify(workspaceClient, times(2)).putObject(any(), any(), any());
        verify(storageClient, times(2)).storeFileFromWorkspace(any(), any(), any(), any());

        assertThat(itemStatus).isNotNull();
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
        assertThat(itemStatus.getItemsStatus().size()).isEqualTo(1);
        assertThat(itemStatus.getItemsStatus().get(MASS_UPDATE_FINALIZE)).isNotNull();
        assertThat(itemStatus.getItemsStatus().get(MASS_UPDATE_FINALIZE).getGlobalStatus())
            .isEqualTo(StatusCode.OK);
    }

    @Test
    @RunWithCustomExecutor
    public void shouldReturnKOWhenEmptyUriReportsUpdateFinalizeTest()
        throws ContentAddressableStorageServerException, URISyntaxException, ProcessingException, IOException,
        ContentAddressableStorageNotFoundException {

        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        VitamThreadUtils.getVitamSession().setRequestId(operationId);
        when(workspaceClient.getListUriDigitalObjectFromFolder(parameters.getContainerName(), REPORTS))
            .thenReturn(new RequestResponseOK().addResult(Collections.<URI>emptyList()));
        when(handler.getWorkerId()).thenReturn(operationId);

        // When
        ItemStatus itemStatus = massUpdateFinalize.execute(parameters, handler);

        // Then
        assertThat(itemStatus).isNotNull();
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
        assertThat(itemStatus.getItemsStatus().size()).isEqualTo(1);
        assertThat(itemStatus.getItemsStatus().get(MASS_UPDATE_FINALIZE)).isNotNull();
        assertThat(itemStatus.getItemsStatus().get(MASS_UPDATE_FINALIZE).getGlobalStatus())
            .isEqualTo(StatusCode.KO);
    }

    @Test
    @RunWithCustomExecutor
    public void shouldReturnFatalWhenWorkspaceExceptionUpdateFinalizeTest()
        throws ContentAddressableStorageServerException, URISyntaxException, ProcessingException, IOException,
        ContentAddressableStorageNotFoundException, StorageNotFoundClientException, StorageServerClientException,
        StorageAlreadyExistsClientException {

        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        VitamThreadUtils.getVitamSession().setRequestId(operationId);
        when(workspaceClient.getListUriDigitalObjectFromFolder(parameters.getContainerName(), REPORTS))
            .thenReturn(new RequestResponseOK().addResult(Arrays.asList(new URI(OP_00_SUCCESS_JSON))));
        when(handler.getWorkerId()).thenReturn(operationId);
        File pathFile = tempFolder.newFile();
        given(handler.getNewLocalFile(any())).willReturn(pathFile);
        when(workspaceClient.getObject(any(), any()))
            .thenThrow(new ContentAddressableStorageNotFoundException("Not found exception"));

        // When
        ItemStatus itemStatus = massUpdateFinalize.execute(parameters, handler);

        // Then
        assertThat(itemStatus).isNotNull();
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.FATAL);
        assertThat(itemStatus.getItemsStatus().size()).isEqualTo(1);
        assertThat(itemStatus.getItemsStatus().get(MASS_UPDATE_FINALIZE)).isNotNull();
        assertThat(itemStatus.getItemsStatus().get(MASS_UPDATE_FINALIZE).getGlobalStatus())
            .isEqualTo(StatusCode.FATAL);
    }
}

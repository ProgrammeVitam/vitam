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
package fr.gouv.vitam.worker.core.plugin.massprocessing;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.UpdatePermissionException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialNotFoundException;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.io.FileNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;

/**
 * UnitMetadataUpdateCheckPermission tests
 */
@RunWith(MockitoJUnitRunner.class)
public class UnitMetadataUpdateCheckPermissionTest {

    private static final String UNIT_UPDATE_CHECK_PERMISSIONS_CONTRACT_WITHOUT_WRITING_PERMISSION_JSON =
        "unitUpdateCheckPermissions/contract_without_writing_permission.json";
    private static final String UNIT_UPDATE_CHECK_PERMISSIONS_CONTRACT_WITH_WRITING_PERMISSION_RESTRICTED_DESC_JSON =
        "unitUpdateCheckPermissions/contract_with_writing_permission_restricted_desc.json";
    private static final String UNIT_UPDATE_CHECK_PERMISSIONS_UPDATE_UNITS_QUERY_JSON =
        "unitUpdateCheckPermissions/update_units_query.json";
    private static final String UNIT_UPDATE_CHECK_PERMISSIONS_CONTRACT_WITH_ALL_UPDATE_PERMISSION_JSON =
        "unitUpdateCheckPermissions/contract_with_all_update_permission.json";
    private static final String UNIT_UPDATE_CHECK_PERMISSIONS_UPDATE_UNITS_GRAPH_QUERY_JSON =
        "unitUpdateCheckPermissions/update_units_graph_query.json";
    private static final String UNIT_UPDATE_CHECK_PERMISSIONS_UPDATE_UNITS_MDD_QUERY_JSON =
        "unitUpdateCheckPermissions/update_units_mdd_query.json";
    private static final String UNIT_UPDATE_CHECK_PERMISSIONS_UPDATE_UNITS_QUERY_DESC_JSON =
        "unitUpdateCheckPermissions/update_units_query_desc.json";

    final String CONTRACT_ID = "aefqaaaaa4hjigi7abyekaldwznbhbyaaaaq";
    final String CONTRACT_ID_2 = "aefqaaaaa4hjigi7abyekaldwznbhbyabbbq";
    final String CONTRACT_ID_3 = "aefqaaaaa4hjigi7abyekaldwznbhbyacccq";

    private static final int TENANT_ID = 0;

    private static final int CHECK_CONTRACT_RANK = 0;
    private static final int CHECK_MANAGEMENT_RANK = 1;
    private static final int CHECK_GRAPH_RANK = 2;
    private static final int CHECK_MDD_RANK = 3;

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    private WorkerParameters parameters;
    private String operationId;

    @Mock
    private HandlerIO handlerIO;

    @Mock
    private AdminManagementClientFactory adminManagementClientFactory;

    @Mock
    private AdminManagementClient adminManagementClient;

    @InjectMocks
    private UnitMetadataUpdateCheckPermission unitMetadataUpdateCheckPermission;

    @Before
    public void setUp() throws Exception {
        when(adminManagementClientFactory.getClient()).thenReturn(adminManagementClient);
        operationId = GUIDFactory.newRequestIdGUID(TENANT_ID).toString();
        parameters =
            WorkerParametersFactory.newWorkerParameters().setUrlWorkspace("http://localhost:8080")
                .setUrlMetadata("http://localhost:8080").setObjectNameList(Lists.newArrayList("objectName"))
                .setObjectName("objectName.json").setCurrentStep("currentStep")
                .setContainerName(operationId);
    }

    @Test
    @RunWithCustomExecutor
    public void givingUpdateQueryWithWrittingPermissionOK()
        throws InvalidParseOperationException, ContentAddressableStorageServerException, ProcessingException,
        FileNotFoundException, ReferentialNotFoundException, AdminManagementClientServerException {

        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        VitamThreadUtils.getVitamSession().setRequestId(operationId);
        VitamThreadUtils.getVitamSession().setContractId(CONTRACT_ID_3);

        File fileContract = PropertiesUtils.getResourceFile(
            UNIT_UPDATE_CHECK_PERMISSIONS_CONTRACT_WITH_ALL_UPDATE_PERMISSION_JSON);
        AccessContractModel accessContract = JsonHandler.getFromFileAsTypeRefence(fileContract,
            new TypeReference<AccessContractModel>() {
            });
        VitamThreadUtils.getVitamSession().setContract(accessContract);

        when(adminManagementClient.findAccessContractsByID(CONTRACT_ID_3))
            .thenReturn(new RequestResponseOK<AccessContractModel>().addResult(accessContract));

        JsonNode updateQuery =
            JsonHandler.getFromFile(PropertiesUtils.findFile(
                UNIT_UPDATE_CHECK_PERMISSIONS_UPDATE_UNITS_QUERY_DESC_JSON));

        given(handlerIO.getJsonFromWorkspace("query.json")).willReturn(updateQuery);
        given(handlerIO.getInput(CHECK_CONTRACT_RANK)).willReturn(String.valueOf(true));

        // When
        ItemStatus itemStatus = unitMetadataUpdateCheckPermission.execute(parameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
        assertThat(itemStatus.getItemsStatus().size()).isEqualTo(1);
        assertThat(itemStatus.getItemsStatus().get("UNIT_METADATA_UPDATE_CHECK_PERMISSION")).isNotNull();
        assertThat(itemStatus.getItemsStatus().get("UNIT_METADATA_UPDATE_CHECK_PERMISSION").getGlobalStatus())
            .isEqualTo(StatusCode.OK);
    }

    @Test
    @RunWithCustomExecutor
    public void givingUpdateQueryWithoutWrittingPermissionKO()
        throws InvalidParseOperationException, ProcessingException,
        FileNotFoundException, ReferentialNotFoundException, AdminManagementClientServerException {

        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        VitamThreadUtils.getVitamSession().setRequestId(operationId);
        VitamThreadUtils.getVitamSession().setContractId(CONTRACT_ID);

        File fileContract = PropertiesUtils.getResourceFile(
            UNIT_UPDATE_CHECK_PERMISSIONS_CONTRACT_WITHOUT_WRITING_PERMISSION_JSON);
        AccessContractModel accessContract = JsonHandler.getFromFileAsTypeRefence(fileContract,
            new TypeReference<AccessContractModel>() {
            });
        VitamThreadUtils.getVitamSession().setContract(accessContract);

        when(adminManagementClient.findAccessContractsByID(CONTRACT_ID))
            .thenReturn(new RequestResponseOK<AccessContractModel>().addResult(accessContract));

        given(handlerIO.getInput(CHECK_CONTRACT_RANK)).willReturn(String.valueOf(true));

        // When /Then
        ItemStatus status = unitMetadataUpdateCheckPermission.execute(parameters, handlerIO);
        assertThat(status.getGlobalStatus()).isEqualTo(StatusCode.KO);
    }

    @Test
    @RunWithCustomExecutor
    public void givingUpdateDataMgtQueryWithOnlyWrittingPermissionDescKO()
        throws InvalidParseOperationException, ProcessingException,
        FileNotFoundException, ReferentialNotFoundException, AdminManagementClientServerException {

        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        VitamThreadUtils.getVitamSession().setRequestId(operationId);
        VitamThreadUtils.getVitamSession().setContractId(CONTRACT_ID_2);

        File fileContract = PropertiesUtils.getResourceFile(
            UNIT_UPDATE_CHECK_PERMISSIONS_CONTRACT_WITH_WRITING_PERMISSION_RESTRICTED_DESC_JSON);
        AccessContractModel accessContract = JsonHandler.getFromFileAsTypeRefence(fileContract,
            new TypeReference<AccessContractModel>() {
            });
        VitamThreadUtils.getVitamSession().setContract(accessContract);

        when(adminManagementClient.findAccessContractsByID(CONTRACT_ID_2))
            .thenReturn(new RequestResponseOK<AccessContractModel>().addResult(accessContract));

        JsonNode updateQuery =
            JsonHandler.getFromFile(PropertiesUtils.findFile(UNIT_UPDATE_CHECK_PERMISSIONS_UPDATE_UNITS_QUERY_JSON));

        given(handlerIO.getJsonFromWorkspace("query.json")).willReturn(updateQuery);
        given(handlerIO.getInput(CHECK_CONTRACT_RANK)).willReturn(String.valueOf(true));
        given(handlerIO.getInput(CHECK_MANAGEMENT_RANK)).willReturn(String.valueOf(true));

        // When /Then
        ItemStatus status = unitMetadataUpdateCheckPermission.execute(parameters, handlerIO);
        assertThat(status.getGlobalStatus()).isEqualTo(StatusCode.KO);
    }

    @Test
    @RunWithCustomExecutor
    public void givingUpdateGraphQueryWithWrittingPermissionKO()
        throws InvalidParseOperationException, ContentAddressableStorageServerException, ProcessingException,
        FileNotFoundException, ReferentialNotFoundException, AdminManagementClientServerException {

        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        VitamThreadUtils.getVitamSession().setRequestId(operationId);
        VitamThreadUtils.getVitamSession().setContractId(CONTRACT_ID_3);

        File fileContract = PropertiesUtils.getResourceFile(
            UNIT_UPDATE_CHECK_PERMISSIONS_CONTRACT_WITH_ALL_UPDATE_PERMISSION_JSON);
        AccessContractModel accessContract = JsonHandler.getFromFileAsTypeRefence(fileContract,
            new TypeReference<AccessContractModel>() {
            });
        VitamThreadUtils.getVitamSession().setContract(accessContract);

        when(adminManagementClient.findAccessContractsByID(CONTRACT_ID_3))
            .thenReturn(new RequestResponseOK<AccessContractModel>().addResult(accessContract));

        JsonNode updateQuery =
            JsonHandler.getFromFile(PropertiesUtils.findFile(
                UNIT_UPDATE_CHECK_PERMISSIONS_UPDATE_UNITS_GRAPH_QUERY_JSON));

        given(handlerIO.getJsonFromWorkspace("query.json")).willReturn(updateQuery);
        given(handlerIO.getInput(CHECK_CONTRACT_RANK)).willReturn(String.valueOf(true));
        given(handlerIO.getInput(CHECK_GRAPH_RANK)).willReturn(String.valueOf(true));

        // When /Then
        assertThatThrownBy(() -> unitMetadataUpdateCheckPermission.execute(parameters, handlerIO))
            .isInstanceOf(ProcessingException.class);
    }

    @Test
    @RunWithCustomExecutor
    public void givingUpdateMDDQueryWithWrittingPermissionKO()
        throws InvalidParseOperationException, ContentAddressableStorageServerException, ProcessingException,
        FileNotFoundException, ReferentialNotFoundException, AdminManagementClientServerException {

        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        VitamThreadUtils.getVitamSession().setRequestId(operationId);
        VitamThreadUtils.getVitamSession().setContractId(CONTRACT_ID_3);

        File fileContract = PropertiesUtils.getResourceFile(
            UNIT_UPDATE_CHECK_PERMISSIONS_CONTRACT_WITH_ALL_UPDATE_PERMISSION_JSON);
        AccessContractModel accessContract = JsonHandler.getFromFileAsTypeRefence(fileContract,
            new TypeReference<AccessContractModel>() {
            });
        VitamThreadUtils.getVitamSession().setContract(accessContract);

        when(adminManagementClient.findAccessContractsByID(CONTRACT_ID_3))
            .thenReturn(new RequestResponseOK<AccessContractModel>().addResult(accessContract));

        JsonNode updateQuery =
            JsonHandler.getFromFile(PropertiesUtils.findFile(
                UNIT_UPDATE_CHECK_PERMISSIONS_UPDATE_UNITS_MDD_QUERY_JSON));

        given(handlerIO.getJsonFromWorkspace("query.json")).willReturn(updateQuery);
        given(handlerIO.getInput(CHECK_CONTRACT_RANK)).willReturn(String.valueOf(true));
        given(handlerIO.getInput(CHECK_MDD_RANK)).willReturn(String.valueOf(true));

        // When /Then
        assertThatThrownBy(() -> unitMetadataUpdateCheckPermission.execute(parameters, handlerIO))
            .isInstanceOf(ProcessingException.class);
    }
}


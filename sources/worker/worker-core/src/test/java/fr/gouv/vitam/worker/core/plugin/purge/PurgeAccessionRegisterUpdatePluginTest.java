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
package fr.gouv.vitam.worker.core.plugin.purge;

import com.fasterxml.jackson.core.type.TypeReference;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.error.ServiceName;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.AccessionRegisterDetailModel;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.ws.rs.core.Response;
import java.io.File;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

public class PurgeAccessionRegisterUpdatePluginTest {

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private AdminManagementClientFactory adminManagementClientFactory;

    @Mock
    private AdminManagementClient adminManagementClient;

    private PurgeAccessionRegisterUpdatePlugin instance;

    @Mock
    private HandlerIO handler;

    private WorkerParameters params;
    private static final String ACCESSION_REGISTER_DETAIL =
        "EliminationAction/PurgeAccessionRegisterUpdatePlugin/accession-register.json";

    @Before
    public void setUp() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        VitamThreadUtils.getVitamSession().setRequestId("opId");

        doReturn(adminManagementClient).when(adminManagementClientFactory).getClient();

        params = WorkerParametersFactory.newWorkerParameters()
            .setWorkerGUID(GUIDFactory.newGUID().getId())
            .setContainerName(VitamThreadUtils.getVitamSession().getRequestId())
            .setRequestId(VitamThreadUtils.getVitamSession().getRequestId())
            .setProcessId(VitamThreadUtils.getVitamSession().getRequestId())
            .setObjectName("REF")
            .setCurrentStep("StepName")
            .setObjectMetadata(JsonHandler.createObjectNode());

        instance = new PurgeAccessionRegisterUpdatePlugin(
            "PLUGIN_ACTION",
            LogbookTypeProcess.ELIMINATION,
            adminManagementClientFactory
        );
    }

    @Test
    @RunWithCustomExecutor
    public void test_when_update_accession_register_then_FATAL() throws Exception {
        when(adminManagementClient.createOrUpdateAccessionRegister(any())).thenThrow(
            new AdminManagementClientServerException("Simulate FATAL")
        );
        RequestResponse response = new RequestResponseOK<>().setHttpCode(Response.Status.OK.getStatusCode());
        when(adminManagementClient.getAccessionRegister(any())).thenReturn(response);

        // Given / When
        ItemStatus itemStatus = instance.execute(params, handler);

        Assertions.assertThat(itemStatus).isNotNull();
        Assertions.assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.FATAL);
    }

    @Test
    @RunWithCustomExecutor
    public void test_when_update_already_exists_accession_register_then_OK() throws Exception {
        VitamError ve = new VitamError(Response.Status.CONFLICT.name())
            .setHttpCode(Response.Status.CONFLICT.getStatusCode())
            .setContext(ServiceName.EXTERNAL_ACCESS.getName())
            .setState("code_vitam")
            .setMessage(Response.Status.CONFLICT.getReasonPhrase())
            .setDescription("Document already exists in database");

        RequestResponse response = new RequestResponseOK<>().setHttpCode(Response.Status.OK.getStatusCode());
        when(adminManagementClient.getAccessionRegister(any())).thenReturn(response);

        when(adminManagementClient.createOrUpdateAccessionRegister(any())).thenReturn(ve);
        File file = PropertiesUtils.getResourceFile(ACCESSION_REGISTER_DETAIL);
        RequestResponseOK<AccessionRegisterDetailModel> accessionRegisterDetailExisting =
            JsonHandler.getFromFileAsTypeReference(
                file,
                new TypeReference<RequestResponseOK<AccessionRegisterDetailModel>>() {}
            );
        // When
        when(adminManagementClient.getAccessionRegisterDetail(any(), any())).thenReturn(
            accessionRegisterDetailExisting
        );

        // Given / When
        ItemStatus itemStatus = instance.execute(params, handler);

        Assertions.assertThat(itemStatus).isNotNull();
        Assertions.assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
    }

    @Test
    @RunWithCustomExecutor
    public void test_when_update_accession_register_then_OK() throws Exception {
        RequestResponse<AccessionRegisterDetailModel> resp = new RequestResponseOK<>();
        resp.setHttpCode(Response.Status.OK.getStatusCode());
        RequestResponse response = new RequestResponseOK<>().setHttpCode(Response.Status.OK.getStatusCode());
        when(adminManagementClient.getAccessionRegister(any())).thenReturn(response);

        when(adminManagementClient.createOrUpdateAccessionRegister(any())).thenReturn(resp);
        File file = PropertiesUtils.getResourceFile(ACCESSION_REGISTER_DETAIL);
        RequestResponseOK<AccessionRegisterDetailModel> accessionRegisterDetailExisting =
            JsonHandler.getFromFileAsTypeReference(
                file,
                new TypeReference<RequestResponseOK<AccessionRegisterDetailModel>>() {}
            );
        // When
        when(adminManagementClient.getAccessionRegisterDetail(any(), any())).thenReturn(
            accessionRegisterDetailExisting
        );

        // Given / When
        ItemStatus itemStatus = instance.execute(params, handler);

        Assertions.assertThat(itemStatus).isNotNull();
        Assertions.assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
    }
}

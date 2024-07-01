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
package fr.gouv.vitam.worker.core.handler;

import fr.gouv.vitam.common.client.ClientMockResultHelper;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.ActivationStatus;
import fr.gouv.vitam.common.model.administration.IngestContractModel;
import fr.gouv.vitam.common.model.administration.ProfileModel;
import fr.gouv.vitam.common.model.administration.ProfileStatus;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CheckArchiveProfileRelationActionHandlerTest {

    CheckArchiveProfileRelationActionHandler handler;
    private static final String HANDLER_ID = "CHECK_IC_AP_RELATION";
    private AdminManagementClient adminClient;
    private AdminManagementClientFactory adminManagementClientFactory;
    private GUID guid;
    private static final Integer TENANT_ID = 0;
    private static final String FAKE_URL = "http://localhost:8083";
    private static final String CONTRACT_NAME = "ArchivalAgreement0";
    private static final String PROFILE_IDENTIFIER = "PROFIL-00001";

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    private HandlerIO handlerIO = mock(HandlerIO.class);

    @Before
    public void setUp() throws ProcessingException, FileNotFoundException {
        adminClient = mock(AdminManagementClient.class);
        guid = GUIDFactory.newGUID();
        adminManagementClientFactory = mock(AdminManagementClientFactory.class);
        when(adminManagementClientFactory.getClient()).thenReturn(adminClient);
    }

    @Test
    @RunWithCustomExecutor
    public void testhandlerWorking() throws InvalidParseOperationException, AdminManagementClientServerException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        when(handlerIO.getInput(0)).thenReturn(PROFILE_IDENTIFIER);
        when(handlerIO.getInput(1)).thenReturn(CONTRACT_NAME);

        when(adminClient.findIngestContracts(any())).thenReturn(createIngestContract(ActivationStatus.ACTIVE));
        when(adminClient.findProfiles(any())).thenReturn(createProfile(ProfileStatus.ACTIVE));

        final WorkerParameters params = WorkerParametersFactory.newWorkerParameters()
            .setUrlWorkspace(FAKE_URL)
            .setUrlMetadata(FAKE_URL)
            .setObjectNameList(Lists.newArrayList("objectName.json"))
            .setObjectName("objectName.json")
            .setCurrentStep("currentStep")
            .setContainerName(guid.getId());
        handler = new CheckArchiveProfileRelationActionHandler(adminManagementClientFactory);
        assertEquals(CheckArchiveProfileRelationActionHandler.getId(), HANDLER_ID);

        ItemStatus response = handler.execute(params, handlerIO);
        assertEquals(response.getGlobalStatus(), StatusCode.OK);

        when(handlerIO.getInput(0)).thenReturn("wrong_profile");

        response = handler.execute(params, handlerIO);
        assertEquals(response.getGlobalOutcomeDetailSubcode(), "DIFF");
        assertEquals(response.getGlobalStatus(), StatusCode.KO);
    }

    @Test
    @RunWithCustomExecutor
    public void givenProfilInactiveWhenTesthandlerWorkingThenReturnInvalideKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        when(handlerIO.getInput(0)).thenReturn(PROFILE_IDENTIFIER);
        when(handlerIO.getInput(1)).thenReturn(CONTRACT_NAME);

        when(adminClient.findIngestContracts(any())).thenReturn(createIngestContract(ActivationStatus.ACTIVE));
        when(adminClient.findProfiles(any())).thenReturn(createProfile(ProfileStatus.INACTIVE));
        final WorkerParameters params = WorkerParametersFactory.newWorkerParameters()
            .setUrlWorkspace(FAKE_URL)
            .setUrlMetadata(FAKE_URL)
            .setObjectNameList(Lists.newArrayList("objectName.json"))
            .setObjectName("objectName.json")
            .setCurrentStep("currentStep")
            .setContainerName(guid.getId());
        handler = new CheckArchiveProfileRelationActionHandler(adminManagementClientFactory);
        assertEquals(CheckArchiveProfileRelationActionHandler.getId(), HANDLER_ID);

        ItemStatus response = handler.execute(params, handlerIO);
        assertEquals(response.getGlobalOutcomeDetailSubcode(), "INACTIVE");
        assertEquals(response.getGlobalStatus(), StatusCode.KO);
    }

    @Test
    @RunWithCustomExecutor
    public void givenProfilNotFoundWhenTesthandlerWorkingThenReturnInvalideKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        when(handlerIO.getInput(0)).thenReturn(PROFILE_IDENTIFIER);
        when(handlerIO.getInput(1)).thenReturn(CONTRACT_NAME);

        when(adminClient.findProfiles(any())).thenReturn(ClientMockResultHelper.createEmptyReponse());
        final WorkerParameters params = WorkerParametersFactory.newWorkerParameters()
            .setUrlWorkspace(FAKE_URL)
            .setUrlMetadata(FAKE_URL)
            .setObjectNameList(Lists.newArrayList("objectName.json"))
            .setObjectName("objectName.json")
            .setCurrentStep("currentStep")
            .setContainerName(guid.getId());
        handler = new CheckArchiveProfileRelationActionHandler(adminManagementClientFactory);
        assertEquals(CheckArchiveProfileRelationActionHandler.getId(), HANDLER_ID);

        ItemStatus response = handler.execute(params, handlerIO);
        assertEquals(response.getGlobalOutcomeDetailSubcode(), "UNKNOWN");
        assertEquals(response.getGlobalStatus(), StatusCode.KO);
    }

    @Test
    @RunWithCustomExecutor
    public void givenProfilGetVitamErrorWhenTesthandlerWorkingThenReturnInvalideKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        when(handlerIO.getInput(0)).thenReturn(PROFILE_IDENTIFIER);
        when(handlerIO.getInput(1)).thenReturn(CONTRACT_NAME);

        when(adminClient.findProfiles(any())).thenReturn(ClientMockResultHelper.createVitamError());
        final WorkerParameters params = WorkerParametersFactory.newWorkerParameters()
            .setUrlWorkspace(FAKE_URL)
            .setUrlMetadata(FAKE_URL)
            .setObjectNameList(Lists.newArrayList("objectName.json"))
            .setObjectName("objectName.json")
            .setCurrentStep("currentStep")
            .setContainerName(guid.getId());
        handler = new CheckArchiveProfileRelationActionHandler(adminManagementClientFactory);
        assertEquals(CheckArchiveProfileRelationActionHandler.getId(), HANDLER_ID);

        ItemStatus response = handler.execute(params, handlerIO);
        assertEquals(response.getGlobalOutcomeDetailSubcode(), "UNKNOWN");
        assertEquals(response.getGlobalStatus(), StatusCode.KO);
    }

    @Test
    @RunWithCustomExecutor
    public void givenProfilOKAndNotFoundIngestContractWhenTesthandlerWorkingThenReturnInvalideKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        when(handlerIO.getInput(0)).thenReturn(PROFILE_IDENTIFIER);
        when(handlerIO.getInput(1)).thenReturn(CONTRACT_NAME);

        when(adminClient.findIngestContracts(any())).thenReturn(ClientMockResultHelper.createEmptyReponse());
        when(adminClient.findProfiles(any())).thenReturn(createProfile(ProfileStatus.ACTIVE));
        final WorkerParameters params = WorkerParametersFactory.newWorkerParameters()
            .setUrlWorkspace(FAKE_URL)
            .setUrlMetadata(FAKE_URL)
            .setObjectNameList(Lists.newArrayList("objectName.json"))
            .setObjectName("objectName.json")
            .setCurrentStep("currentStep")
            .setContainerName(guid.getId());
        handler = new CheckArchiveProfileRelationActionHandler(adminManagementClientFactory);
        assertEquals(CheckArchiveProfileRelationActionHandler.getId(), HANDLER_ID);

        ItemStatus response = handler.execute(params, handlerIO);
        assertEquals(response.getGlobalOutcomeDetailSubcode(), "UNKNOWN");
        assertEquals(response.getGlobalStatus(), StatusCode.KO);
    }

    @Test
    @RunWithCustomExecutor
    public void givenProfilOKAndIngestContractGetVitamErrorWhenTesthandlerWorkingThenReturnInvalideKO()
        throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        when(handlerIO.getInput(0)).thenReturn(PROFILE_IDENTIFIER);
        when(handlerIO.getInput(1)).thenReturn(CONTRACT_NAME);

        when(adminClient.findIngestContracts(any())).thenReturn(ClientMockResultHelper.createVitamError());
        when(adminClient.findProfiles(any())).thenReturn(createProfile(ProfileStatus.ACTIVE));
        final WorkerParameters params = WorkerParametersFactory.newWorkerParameters()
            .setUrlWorkspace(FAKE_URL)
            .setUrlMetadata(FAKE_URL)
            .setObjectNameList(Lists.newArrayList("objectName.json"))
            .setObjectName("objectName.json")
            .setCurrentStep("currentStep")
            .setContainerName(guid.getId());
        handler = new CheckArchiveProfileRelationActionHandler(adminManagementClientFactory);
        assertEquals(CheckArchiveProfileRelationActionHandler.getId(), HANDLER_ID);

        ItemStatus response = handler.execute(params, handlerIO);
        assertEquals(response.getGlobalOutcomeDetailSubcode(), "UNKNOWN");
        assertEquals(response.getGlobalStatus(), StatusCode.KO);
    }

    @Test
    @RunWithCustomExecutor
    public void givenProfilOKNotInIngestContractWhenTesthandlerWorkingThenReturnInvalideKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        when(handlerIO.getInput(0)).thenReturn(PROFILE_IDENTIFIER);
        when(handlerIO.getInput(1)).thenReturn(CONTRACT_NAME);

        when(adminClient.findIngestContracts(any())).thenReturn(createIngestContract(ActivationStatus.ACTIVE, null));
        when(adminClient.findProfiles(any())).thenReturn(createProfile(ProfileStatus.ACTIVE));
        final WorkerParameters params = WorkerParametersFactory.newWorkerParameters()
            .setUrlWorkspace(FAKE_URL)
            .setUrlMetadata(FAKE_URL)
            .setObjectNameList(Lists.newArrayList("objectName.json"))
            .setObjectName("objectName.json")
            .setCurrentStep("currentStep")
            .setContainerName(guid.getId());
        handler = new CheckArchiveProfileRelationActionHandler(adminManagementClientFactory);
        assertEquals(CheckArchiveProfileRelationActionHandler.getId(), HANDLER_ID);

        ItemStatus response = handler.execute(params, handlerIO);
        assertEquals(response.getGlobalOutcomeDetailSubcode(), "DIFF");
        assertEquals(response.getGlobalStatus(), StatusCode.KO);
    }

    private static RequestResponse createIngestContract(ActivationStatus status) throws InvalidParseOperationException {
        IngestContractModel contract = new IngestContractModel();
        contract.setName("ArchivalAgreement0");
        contract.setStatus(status);
        Set<String> profiles = new HashSet<>();
        profiles.add(PROFILE_IDENTIFIER);
        contract.setArchiveProfiles(profiles);
        return ClientMockResultHelper.createResponse(contract);
    }

    private static RequestResponse createIngestContract(ActivationStatus status, String profileIdentifier)
        throws InvalidParseOperationException {
        IngestContractModel contract = new IngestContractModel();
        contract.setName("ArchivalAgreement0");
        contract.setStatus(status);
        if (null != profileIdentifier) {
            Set<String> profiles = new HashSet<>();
            profiles.add(profileIdentifier);
            contract.setArchiveProfiles(profiles);
        }
        return ClientMockResultHelper.createResponse(contract);
    }

    private static RequestResponse createProfile(ProfileStatus status) throws InvalidParseOperationException {
        ProfileModel profil = new ProfileModel();
        profil.setIdentifier("PROFIL-00001");
        profil.setStatus(status);
        return ClientMockResultHelper.createResponse(profil);
    }
}

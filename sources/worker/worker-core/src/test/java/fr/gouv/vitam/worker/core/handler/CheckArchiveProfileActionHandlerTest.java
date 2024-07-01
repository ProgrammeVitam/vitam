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

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.client.AbstractMockClient;
import fr.gouv.vitam.common.client.ClientMockResultHelper;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.IngestWorkflowConstants;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.ProfileFormat;
import fr.gouv.vitam.common.model.administration.ProfileModel;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.common.tmp.TempFolderRule;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.FileNotFoundException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

public class CheckArchiveProfileActionHandlerTest {

    private static final AdminManagementClient adminClient = mock(AdminManagementClient.class);

    private static final AdminManagementClientFactory adminManagementClientFactory = mock(
        AdminManagementClientFactory.class
    );
    private GUID guid;
    private static final Integer TENANT_ID = 0;
    private static final String FAKE_URL = "http://localhost:8083";
    private static final String CONTRACT_NAME = "ArchivalAgreement0";
    private static final String HANDLER_ID = "CHECK_ARCHIVEPROFILE";
    private static final String PROFIL = "checkProfil/Profil20.rng";
    private static final String MANIFEST_OK = "checkProfil/manifest_ok.xml";
    private static final String MANIFEST_KO = "checkProfil/manifest_ko.xml";

    private static final CheckArchiveProfileActionHandler handler = new CheckArchiveProfileActionHandler(
        adminManagementClientFactory
    );

    @Rule
    public TempFolderRule tempFolderRule = new TempFolderRule();

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    private static final HandlerIO handlerIO = mock(HandlerIO.class);

    @Before
    public void setUp() throws ProcessingException, FileNotFoundException {
        guid = GUIDFactory.newGUID();
        reset(adminClient);
        reset(handlerIO);
        when(adminManagementClientFactory.getClient()).thenReturn(adminClient);
    }

    @Test
    @RunWithCustomExecutor
    public void givenProdileOKThenReturnResponseOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        final WorkerParameters params = WorkerParametersFactory.newWorkerParameters()
            .setUrlWorkspace(FAKE_URL)
            .setUrlMetadata(FAKE_URL)
            .setObjectNameList(Lists.newArrayList("objectName.json"))
            .setObjectName("objectName.json")
            .setCurrentStep("currentStep")
            .setContainerName(guid.getId());
        assertEquals(CheckArchiveProfileActionHandler.getId(), HANDLER_ID);

        when(handlerIO.getInput(0)).thenReturn(CONTRACT_NAME);
        when(
            handlerIO.getInputStreamFromWorkspace(
                IngestWorkflowConstants.SEDA_FOLDER + "/" + IngestWorkflowConstants.SEDA_FILE
            )
        ).thenReturn(PropertiesUtils.getResourceAsStream(MANIFEST_OK));

        when(adminClient.findProfiles(any())).thenReturn(createProfileRNG("Profil20.rng"));

        Response mockResponse = new AbstractMockClient.FakeInboundResponse(
            Status.OK,
            PropertiesUtils.getResourceAsStream(PROFIL),
            MediaType.APPLICATION_OCTET_STREAM_TYPE,
            null
        );

        when(adminClient.downloadProfileFile(any())).thenReturn(mockResponse);

        ItemStatus response = handler.execute(params, handlerIO);
        assertEquals(response.getGlobalStatus(), StatusCode.OK);
    }

    @Test
    @RunWithCustomExecutor
    public void givenProdileKOThenReturnResponseKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        final WorkerParameters params = WorkerParametersFactory.newWorkerParameters()
            .setUrlWorkspace(FAKE_URL)
            .setUrlMetadata(FAKE_URL)
            .setObjectNameList(Lists.newArrayList("objectName.json"))
            .setObjectName("objectName.json")
            .setCurrentStep("currentStep")
            .setContainerName(guid.getId());
        assertEquals(CheckArchiveProfileActionHandler.getId(), HANDLER_ID);

        when(handlerIO.getInput(0)).thenReturn(CONTRACT_NAME);
        when(
            handlerIO.getInputStreamFromWorkspace(
                IngestWorkflowConstants.SEDA_FOLDER + "/" + IngestWorkflowConstants.SEDA_FILE
            )
        ).thenReturn(PropertiesUtils.getResourceAsStream(MANIFEST_KO));

        when(adminClient.findProfiles(any())).thenReturn(createProfileRNG("Profil20.rng"));

        Response mockResponse = new AbstractMockClient.FakeInboundResponse(
            Status.OK,
            PropertiesUtils.getResourceAsStream(PROFIL),
            MediaType.APPLICATION_OCTET_STREAM_TYPE,
            null
        );

        when(adminClient.downloadProfileFile(any())).thenReturn(mockResponse);

        ItemStatus response = handler.execute(params, handlerIO);
        assertEquals(response.getGlobalStatus(), StatusCode.KO);
        assertNotNull(response.getEvDetailData());
    }

    @Test
    @RunWithCustomExecutor
    public void givenProdileWithoutPathThenReturnResponseKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        final WorkerParameters params = WorkerParametersFactory.newWorkerParameters()
            .setUrlWorkspace(FAKE_URL)
            .setUrlMetadata(FAKE_URL)
            .setObjectNameList(Lists.newArrayList("objectName.json"))
            .setObjectName("objectName.json")
            .setCurrentStep("currentStep")
            .setContainerName(guid.getId());
        assertEquals(CheckArchiveProfileActionHandler.getId(), HANDLER_ID);

        when(handlerIO.getInput(0)).thenReturn(CONTRACT_NAME);
        when(
            handlerIO.getInputStreamFromWorkspace(
                IngestWorkflowConstants.SEDA_FOLDER + "/" + IngestWorkflowConstants.SEDA_FILE
            )
        ).thenReturn(PropertiesUtils.getResourceAsStream(MANIFEST_OK));

        when(adminClient.findProfiles(any())).thenReturn(createProfileRNG(""));

        Response mockResponse = new AbstractMockClient.FakeInboundResponse(
            Status.OK,
            PropertiesUtils.getResourceAsStream(PROFIL),
            MediaType.APPLICATION_OCTET_STREAM_TYPE,
            null
        );

        when(adminClient.downloadProfileFile(any())).thenReturn(mockResponse);

        ItemStatus response = handler.execute(params, handlerIO);
        assertEquals(response.getGlobalStatus(), StatusCode.KO);
    }

    private static RequestResponse createProfileRNG(String path) {
        ProfileModel profile = new ProfileModel();
        profile.setIdentifier("PROFIL_0001");
        profile.setId(GUIDFactory.newProfileGUID(0).toString());
        profile.setPath(path);
        profile.setFormat(ProfileFormat.RNG);
        return ClientMockResultHelper.createResponse(profile);
    }
}

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
package fr.gouv.vitam.worker.core.plugin.preservation;

import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
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
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.model.ObjectGroupPerOriginatingAgency;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.core.impl.HandlerIOImpl;
import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

public class PreservationAccessionRegisterActionHandlerTest {
    private static final String FAKE_URL = "http://localhost:8080";
    PreservationAccessionRegisterActionHandler accessionRegisterHandler;
    private static final String HANDLER_ID = "PRESERVATION_ACCESSION_REGISTRATION";
    private HandlerIOImpl handlerIO;
    private GUID guid;
    private WorkerParameters params;
    private static final Integer TENANT_ID = 0;

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());


    MetaDataClient metaDataClient = mock(MetaDataClient.class);
    MetaDataClientFactory metaDataClientFactory = mock(MetaDataClientFactory.class);

    AdminManagementClient adminManagementClient = mock(AdminManagementClient.class);
    AdminManagementClientFactory adminManagementClientFactory = mock(AdminManagementClientFactory.class);

    @Before
    public void setUp() throws Exception {
        AdminManagementClientFactory.changeMode(null);
        guid = GUIDFactory.newGUID();
        params =
            WorkerParametersFactory.newWorkerParameters().setUrlWorkspace(FAKE_URL).setUrlMetadata(FAKE_URL)
                .setObjectNameList(Lists.newArrayList("objectName.json")).setObjectName("objectName.json")
                .setCurrentStep("currentStep").setContainerName(guid.getId());
        String objectId = "manifest";
        handlerIO = new HandlerIOImpl(guid.getId(), "workerId", newArrayList(objectId));
        handlerIO.setCurrentObjectId(objectId);

        when(metaDataClientFactory.getClient()).thenReturn(metaDataClient);
        when(adminManagementClientFactory.getClient()).thenReturn(adminManagementClient);
    }

    @After
    public void end() {
        handlerIO.partialClose();
    }

    @Test
    @RunWithCustomExecutor
    public void testResponseOK() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        GUID operationId = GUIDFactory.newGUID();
        VitamThreadUtils.getVitamSession().setRequestId(operationId);

        params.setContainerName(operationId.getId());

        List<ObjectGroupPerOriginatingAgency> originatingAgencies = new ArrayList<>();
        originatingAgencies.add(new ObjectGroupPerOriginatingAgency(operationId.getId(), "FRAN_NP_005568", 1l, 1l, 1l));


        reset(metaDataClient);
        reset(adminManagementClient);
        when(metaDataClient.selectAccessionRegisterOnObjectByOperationId(operationId.toString()))
            .thenReturn(originatingAgencies);


        AdminManagementClientFactory.changeMode(null);
        handlerIO.reset();
        accessionRegisterHandler =
            new PreservationAccessionRegisterActionHandler(metaDataClientFactory, adminManagementClientFactory);
        assertEquals(PreservationAccessionRegisterActionHandler.getId(), HANDLER_ID);

        RequestResponse<AccessionRegisterDetailModel> res =
            new RequestResponseOK<AccessionRegisterDetailModel>().setHttpCode(201);
        when(adminManagementClient.createOrUpdateAccessionRegister(any()))
            .thenReturn(res);

        // When
        final ItemStatus response = accessionRegisterHandler.execute(params, handlerIO);

        // Then
        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void testResponseFATALCouldNotCreateRegister() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        GUID operationId = GUIDFactory.newGUID();
        VitamThreadUtils.getVitamSession().setRequestId(operationId);

        params.setContainerName(operationId.getId());

        List<ObjectGroupPerOriginatingAgency> originatingAgencies = new ArrayList<>();
        originatingAgencies.add(new ObjectGroupPerOriginatingAgency(operationId.getId(), "FRAN_NP_005568", 1l, 1l, 1l));

        reset(metaDataClient);
        reset(adminManagementClient);
        when(metaDataClient.selectAccessionRegisterOnObjectByOperationId(operationId.toString()))
            .thenReturn(originatingAgencies);


        when(adminManagementClient.createOrUpdateAccessionRegister(any()))
            .thenThrow(new AdminManagementClientServerException("AdminManagementClientServerException"));

        AdminManagementClientFactory.changeMode(null);
        handlerIO.reset();
        accessionRegisterHandler =
            new PreservationAccessionRegisterActionHandler(metaDataClientFactory, adminManagementClientFactory);
        assertEquals(PreservationAccessionRegisterActionHandler.getId(), HANDLER_ID);

        // When
        final ItemStatus response = accessionRegisterHandler.execute(params, handlerIO);

        // Then
        assertEquals(StatusCode.FATAL, response.getGlobalStatus());

    }

    @Test
    @RunWithCustomExecutor
    public void testResponseOKAlreadyExistsAccessionRegister() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        GUID operationId = GUIDFactory.newGUID();
        VitamThreadUtils.getVitamSession().setRequestId(operationId);

        params.setContainerName(operationId.getId());

        List<ObjectGroupPerOriginatingAgency> originatingAgencies = new ArrayList<>();
        originatingAgencies.add(new ObjectGroupPerOriginatingAgency(operationId.getId(), "FRAN_NP_005568", 1l, 1l, 1l));

        reset(metaDataClient);
        reset(adminManagementClient);
        when(metaDataClient.selectAccessionRegisterOnObjectByOperationId(operationId.toString()))
            .thenReturn(originatingAgencies);

        RequestResponse<AccessionRegisterDetailModel> res =
            new RequestResponseOK<AccessionRegisterDetailModel>().setHttpCode(201);

        when(adminManagementClient.createOrUpdateAccessionRegister(any()))
            .thenReturn(res);

        AdminManagementClientFactory.changeMode(null);
        handlerIO.reset();
        accessionRegisterHandler =
            new PreservationAccessionRegisterActionHandler(metaDataClientFactory, adminManagementClientFactory);
        assertEquals(PreservationAccessionRegisterActionHandler.getId(), HANDLER_ID);

        // When
        final ItemStatus response = accessionRegisterHandler.execute(params, handlerIO);

        // Then
        assertEquals(StatusCode.OK, response.getGlobalStatus());

    }


    @Test
    @RunWithCustomExecutor
    public void testResponseFatal() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        GUID operationId = GUIDFactory.newGUID();
        VitamThreadUtils.getVitamSession().setRequestId(operationId);

        params.setContainerName(operationId.getId());

        List<ObjectGroupPerOriginatingAgency> originatingAgencies = new ArrayList<>();
        originatingAgencies.add(new ObjectGroupPerOriginatingAgency(operationId.getId(), "FRAN_NP_005568", 1l, 1l, 1l));


        reset(metaDataClient);
        reset(adminManagementClient);
        when(metaDataClient.selectAccessionRegisterOnObjectByOperationId(operationId.toString()))
            .thenThrow(new MetaDataClientServerException("MetaDataClientServerException"));

        AdminManagementClientFactory.changeMode(null);
        handlerIO.reset();
        accessionRegisterHandler =
            new PreservationAccessionRegisterActionHandler(metaDataClientFactory, adminManagementClientFactory);
        assertEquals(PreservationAccessionRegisterActionHandler.getId(), HANDLER_ID);
        // When
        final ItemStatus response = accessionRegisterHandler.execute(params, handlerIO);
        // Then
        assertEquals(StatusCode.FATAL, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void testResponseOKWithNoAgency() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        GUID operationId = GUIDFactory.newGUID();
        VitamThreadUtils.getVitamSession().setRequestId(operationId);
        reset(metaDataClient);
        reset(adminManagementClient);
        params.setContainerName(operationId.getId());
        AdminManagementClientFactory.changeMode(null);
        handlerIO.reset();
        accessionRegisterHandler =
            new PreservationAccessionRegisterActionHandler(metaDataClientFactory, adminManagementClientFactory);
        assertEquals(PreservationAccessionRegisterActionHandler.getId(), HANDLER_ID);

        // When
        final ItemStatus response = accessionRegisterHandler.execute(params, handlerIO);

        // Then
        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

}

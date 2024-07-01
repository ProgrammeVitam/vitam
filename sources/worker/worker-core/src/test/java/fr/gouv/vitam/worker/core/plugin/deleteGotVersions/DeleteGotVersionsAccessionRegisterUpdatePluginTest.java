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

package fr.gouv.vitam.worker.core.plugin.deleteGotVersions;

import com.fasterxml.jackson.core.type.TypeReference;
import fr.gouv.vitam.batch.report.client.BatchReportClient;
import fr.gouv.vitam.batch.report.client.BatchReportClientFactory;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.AccessionRegisterDetailModel;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.plugin.deleteGotVersions.handlers.DeleteGotVersionsAccessionRegisterUpdatePlugin;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;

import static fr.gouv.vitam.common.json.JsonHandler.createObjectNode;
import static fr.gouv.vitam.common.json.JsonHandler.getFromFile;
import static fr.gouv.vitam.common.model.StatusCode.FATAL;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.common.model.StatusCode.WARNING;
import static fr.gouv.vitam.common.server.application.junit.ResponseHelper.getOutboundResponse;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DeleteGotVersionsAccessionRegisterUpdatePluginTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @InjectMocks
    private DeleteGotVersionsAccessionRegisterUpdatePlugin deleteGotVersionsAccessionRegisterUpdatePlugin;

    @Mock
    private AdminManagementClientFactory adminManagementClientFactory;

    @Mock
    private AdminManagementClient adminManagementClient;

    @Mock
    private BatchReportClientFactory batchReportClientFactory;

    @Mock
    private BatchReportClient batchReportClient;

    @Mock
    private HandlerIO handlerIO;

    @Mock
    private WorkerParameters params;

    private static final String DELETE_GOT_VERSIONS_REPORTS_OK_FILE =
        "deleteGotVersions/deleteGotVersionsReportOk.json";
    private static final String ACCESSION_REGISTER_RESPONSE = "deleteGotVersions/accessionRegisterModel.json";

    @Before
    public void setUp() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        when(adminManagementClientFactory.getClient()).thenReturn(adminManagementClient);
        when(batchReportClientFactory.getClient()).thenReturn(batchReportClient);
        deleteGotVersionsAccessionRegisterUpdatePlugin = new DeleteGotVersionsAccessionRegisterUpdatePlugin(
            adminManagementClientFactory,
            batchReportClientFactory
        );
    }

    @Test
    @RunWithCustomExecutor
    public void givenOkResultsThenDeleteGotVersionsStorageOk() throws Exception {
        File deleteGotVersionsFile = PropertiesUtils.getResourceFile(DELETE_GOT_VERSIONS_REPORTS_OK_FILE);
        when(batchReportClient.readComputedDetailsFromReport(any(), any())).thenReturn(
            getFromFile(deleteGotVersionsFile)
        );
        RequestResponseOK<AccessionRegisterDetailModel> accessionRegisterResponse =
            JsonHandler.getFromFileAsTypeReference(
                PropertiesUtils.getResourceFile(ACCESSION_REGISTER_RESPONSE),
                new TypeReference<>() {}
            );
        when(adminManagementClient.getAccessionRegisterDetail(any())).thenReturn(accessionRegisterResponse);

        ItemStatus itemStatus = deleteGotVersionsAccessionRegisterUpdatePlugin.execute(params, handlerIO);

        assertEquals(OK, itemStatus.getGlobalStatus());
        verify(adminManagementClient, times(1)).createOrUpdateAccessionRegister(any());
    }

    @Test
    @RunWithCustomExecutor
    public void givenOkAndWarningResultsThenDeleteGotVersionsStorageWarning() throws Exception {
        when(batchReportClient.readComputedDetailsFromReport(any(), any())).thenReturn(createObjectNode());
        RequestResponseOK<AccessionRegisterDetailModel> accessionRegisterResponse =
            JsonHandler.getFromFileAsTypeReference(
                PropertiesUtils.getResourceFile(ACCESSION_REGISTER_RESPONSE),
                new TypeReference<>() {}
            );
        when(adminManagementClient.getAccessionRegisterDetail(any())).thenReturn(accessionRegisterResponse);

        ItemStatus itemStatus = deleteGotVersionsAccessionRegisterUpdatePlugin.execute(params, handlerIO);

        assertEquals(WARNING, itemStatus.getGlobalStatus());
        verify(adminManagementClient, times(0)).createOrUpdateAccessionRegister(any());
    }

    @Test
    @RunWithCustomExecutor
    public void givenErrorWhileRetreivingAcessionRegisterThenThrowException() throws Exception {
        File deleteGotVersionsFile = PropertiesUtils.getResourceFile(DELETE_GOT_VERSIONS_REPORTS_OK_FILE);
        when(batchReportClient.readComputedDetailsFromReport(any(), any())).thenReturn(
            getFromFile(deleteGotVersionsFile)
        );
        final VitamError error = new VitamError("0");
        Response response = getOutboundResponse(
            Response.Status.BAD_REQUEST,
            error.toString(),
            MediaType.APPLICATION_JSON,
            null
        );
        RequestResponse requestResponse = RequestResponse.parseVitamError(response);
        when(adminManagementClient.getAccessionRegisterDetail(any())).thenReturn(requestResponse);

        ItemStatus itemStatus = deleteGotVersionsAccessionRegisterUpdatePlugin.execute(params, handlerIO);

        assertEquals(FATAL, itemStatus.getGlobalStatus());
        verify(adminManagementClient, times(0)).createOrUpdateAccessionRegister(any());
    }
}

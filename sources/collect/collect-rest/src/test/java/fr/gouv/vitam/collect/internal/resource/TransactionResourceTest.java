/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.collect.internal.resource;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.collect.internal.model.CollectModel;
import fr.gouv.vitam.collect.internal.server.CollectConfiguration;
import fr.gouv.vitam.collect.internal.service.CollectService;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.ws.rs.core.Response;

import java.io.InputStream;
import java.util.Optional;

import static org.mockito.BDDMockito.given;

public class TransactionResourceTest {
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private TransactionResource transactionResource;

    @Mock
    private CollectService collectService;

    @Mock
    private CollectConfiguration collectConfiguration;

    @Mock
    private static WorkspaceClientFactory workspaceClientFactory;

    private WorkspaceClient workspaceClient;

    private static final String SAMPLE_INIT_TRANSACTION_RESPONSE_FILENAME = "init_transaction_response.json";
    private static JsonNode sampleInitTransaction;

    @Before
    public void setUp() {
        given(collectConfiguration.getWorkspaceUrl()).willReturn("http://localhost:8082");
        transactionResource = new TransactionResource(collectService, collectConfiguration);
    }

    @Test
    public void initTransactionTest_OK() throws Exception {
        // Given
        sampleInitTransaction = JsonHandler.getFromFile(PropertiesUtils.findFile(SAMPLE_INIT_TRANSACTION_RESPONSE_FILENAME));
        given(collectService.createRequestId()).willReturn("082aba2d-817f-4e5f-8fa4-f12ba7d7642f");
        Mockito.doNothing().when(collectService).createCollect(Mockito.isA(CollectModel.class));
        // When
        Response result = transactionResource.initTransaction();
        // Then
        Assertions.assertThat(result.getEntity().toString()).isEqualTo(sampleInitTransaction.toString());
    }

    @Test
    public void initTransactionTest_KO() throws Exception {
        // Given
        sampleInitTransaction = JsonHandler.getFromFile(PropertiesUtils.findFile(SAMPLE_INIT_TRANSACTION_RESPONSE_FILENAME));
        given(collectService.createRequestId()).willReturn("082aba2d-817f-4e5f-8fa4-f12ba7d764");
        Mockito.doNothing().when(collectService).createCollect(Mockito.isA(CollectModel.class));
        // When
        Response result = transactionResource.initTransaction();
        // Then
        Assertions.assertThat(result.getEntity().toString()).isNotEqualTo(sampleInitTransaction.toString());
    }


    @Test
    public void upload_OK() throws Exception {
        // Given
        String TransactionId = "082aba2d-817f-4e5f-8fa4-f12ba7d7642f";
        final InputStream inputStreamZip =
                PropertiesUtils.getResourceAsStream("SIP_bordereau_avec_objet_OK.zip");
        sampleInitTransaction = JsonHandler.getFromFile(PropertiesUtils.findFile(SAMPLE_INIT_TRANSACTION_RESPONSE_FILENAME));
        Optional<CollectModel> collectModel = Optional.of(new CollectModel(TransactionId));
        given(collectService.findCollect(TransactionId)).willReturn(collectModel);
        TransactionResource transactionResourceSpy = Mockito.spy(transactionResource);
        Mockito.doNothing().when(transactionResourceSpy).pushSipStreamToWorkspace(Mockito.any(), Mockito.any());
        // When
        Response result = transactionResourceSpy.upload(TransactionId, inputStreamZip);
        // Then
        Assertions.assertThat(result.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    }

    @Test
    public void upload_KO() throws Exception {
        // Given
        String TransactionId = "082aba2d-817f-4e5f-8fa4-f12ba7d7642f";
        final InputStream inputStreamZip =
                PropertiesUtils.getResourceAsStream("SIP_bordereau_avec_objet_OK.zip");
        sampleInitTransaction = JsonHandler.getFromFile(PropertiesUtils.findFile(SAMPLE_INIT_TRANSACTION_RESPONSE_FILENAME));
        Optional<CollectModel> collectModel = Optional.empty();
        given(collectService.findCollect(TransactionId)).willReturn(collectModel);
        TransactionResource transactionResourceSpy = Mockito.spy(transactionResource);
        Mockito.doNothing().when(transactionResourceSpy).pushSipStreamToWorkspace(Mockito.any(), Mockito.any());
        // When
        Response result = transactionResourceSpy.upload(TransactionId, inputStreamZip);
        // Then
        Assertions.assertThat(result.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    }



}
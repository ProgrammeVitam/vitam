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

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.batch.report.model.PreservationStatus;
import fr.gouv.vitam.common.format.identification.model.FormatIdentifierResponse;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.ActionTypePreservation;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.request.BulkObjectStoreRequest;
import fr.gouv.vitam.storage.engine.common.model.response.StoredInfoResult;
import fr.gouv.vitam.worker.core.plugin.preservation.model.OutputPreservation;
import fr.gouv.vitam.worker.core.plugin.preservation.model.WorkflowBatchResult;
import fr.gouv.vitam.worker.core.plugin.preservation.model.WorkflowBatchResults;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static fr.gouv.vitam.worker.core.plugin.preservation.TestWorkerParameter.TestWorkerParameterBuilder.workerParameterBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PreservationStorageMetadataAndLfcTest {

    private final String GOT_ID = "aebaaaaaaahuq4l4aa4zsalhgwcreaiaaaaq";
    private final TestWorkerParameter parameter = workerParameterBuilder()
        .withContainerName("CONTAINER_NAME_TEST")
        .withRequestId("REQUEST_ID_TEST")
        .build();
    private static final String JSON = ".json";

    private PreservationStorageMetadataAndLfc plugin;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private MetaDataClient metaDataClient;

    @Mock
    private MetaDataClientFactory metaDataClientFactory;

    @Mock
    private StorageClientFactory storageClientFactory;

    @Mock
    private StorageClient storageClient;

    @Mock
    private LogbookLifeCyclesClient logbookClient;

    @Mock
    private LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory;

    private TestHandlerIO handlerIO;

    @Before
    public void setUp() throws Exception {
        reset(storageClient);
        when(storageClientFactory.getClient()).thenReturn(storageClient);
        given(metaDataClientFactory.getClient()).willReturn(metaDataClient);
        given(logbookLifeCyclesClientFactory.getClient()).willReturn(logbookClient);
        parameter.setObjectNameList(Collections.singletonList(GOT_ID));
        handlerIO = new TestHandlerIO();
        plugin = new PreservationStorageMetadataAndLfc(
            metaDataClientFactory,
            logbookLifeCyclesClientFactory,
            storageClientFactory
        );

        FormatIdentifierResponse format = new FormatIdentifierResponse(
            "Plain Text File",
            "text/plain",
            "x-fmt/111",
            ""
        );
        StoredInfoResult value = new StoredInfoResult();
        OutputPreservation output = new OutputPreservation();
        output.setStatus(PreservationStatus.OK);
        output.setAction(ActionTypePreservation.GENERATE);
        WorkflowBatchResult.OutputExtra outputExtra = new WorkflowBatchResult.OutputExtra(
            output,
            "binaryGUID",
            Optional.of(12L),
            Optional.of("hash"),
            Optional.of(format),
            Optional.of(value),
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
        );

        WorkflowBatchResult batchResult = WorkflowBatchResult.of(
            GOT_ID,
            "unitId",
            "BinaryMaster",
            "requestId",
            Collections.singletonList(outputExtra),
            "BinaryMaster",
            "other_binary_strategy",
            Collections.singletonList("unitId")
        );
        WorkflowBatchResults batchResults = new WorkflowBatchResults(
            Paths.get("tmp"),
            Collections.singletonList(batchResult)
        );

        handlerIO.addOutputResult(0, batchResults);
        handlerIO.setInputs(batchResults);
    }

    @Test
    public void should_save_metadata_and_lfc_in_storage() throws Exception {
        // Given
        InputStream stream = getClass().getResourceAsStream("/preservation/objectGroup.json");
        JsonNode document = JsonHandler.getFromInputStream(stream);
        RequestResponse<JsonNode> responseOK = new RequestResponseOK<JsonNode>()
            .addResult(document)
            .setHttpCode(Response.Status.OK.getStatusCode());

        given(metaDataClient.getObjectGroupsByIdsRaw(eq(Collections.singletonList(GOT_ID)))).willReturn(responseOK);
        given(logbookClient.getRawObjectGroupLifeCycleById(GOT_ID)).willReturn(document);

        // When
        List<ItemStatus> itemStatus = plugin.executeList(parameter, handlerIO);

        // Then
        assertThat(itemStatus).extracting(ItemStatus::getGlobalStatus).containsOnly(StatusCode.OK);
        verify(storageClient).bulkStoreFilesFromWorkspace(
            eq("other_got_strategy"),
            argThat(this::bulkObjectStoreRequestMatches)
        );
    }

    private boolean bulkObjectStoreRequestMatches(BulkObjectStoreRequest bulkObjectStoreRequest) {
        return (
            bulkObjectStoreRequest.getObjectNames().equals(Collections.singletonList(GOT_ID + JSON)) &&
            bulkObjectStoreRequest.getType().equals(DataCategory.OBJECTGROUP)
        );
    }
}

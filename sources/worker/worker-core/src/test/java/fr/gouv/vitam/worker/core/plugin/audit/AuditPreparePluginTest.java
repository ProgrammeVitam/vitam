package fr.gouv.vitam.worker.core.plugin.audit;

import static fr.gouv.vitam.common.json.JsonHandler.getFromInputStream;
import static fr.gouv.vitam.common.json.JsonHandler.toJsonNode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.processing.ProcessingUri;
import fr.gouv.vitam.common.model.processing.UriPrefix;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.referential.model.OfferReference;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageStrategy;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import fr.gouv.vitam.worker.core.plugin.audit.model.AuditObjectGroup;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

public class AuditPreparePluginTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private MetaDataClientFactory metaDataClientFactory;

    @Mock
    private StorageClientFactory storageClientFactory;

    @Mock
    private MetaDataClient metaDataClient;

    @Mock
    private StorageClient storageClient;

    private AuditPreparePlugin auditPreparePlugin;

    @Before
    public void setUp() throws Exception {

        auditPreparePlugin = new AuditPreparePlugin(metaDataClientFactory, storageClientFactory);

        when(metaDataClientFactory.getClient()).thenReturn(metaDataClient);

        when(metaDataClient.selectUnits(any())).thenReturn(
                getFromInputStream(getClass().getResourceAsStream("/AuditObjectWorkflow/unitsResult.json")));

        when(metaDataClient.selectObjectGroups(any())).thenReturn(
                getFromInputStream(getClass().getResourceAsStream("/AuditObjectWorkflow/objectGroupsResult.json")));

        when(storageClientFactory.getClient()).thenReturn(storageClient);

        when(storageClient.getStorageStrategies()).thenReturn(loadStorageStrategiesMock());
    }

    @Test
    public void shouldCreateValidJsonLFileAndStrategiesOutput() throws ContentAddressableStorageServerException, ProcessingException,
            IOException, InvalidParseOperationException {

        // Given
        HandlerIO handler = mock(HandlerIO.class);
        WorkerParameters workerParameters = mock(WorkerParameters.class);

        JsonNode auditQuery = JsonHandler
                .getFromInputStream(getClass().getResourceAsStream("/AuditObjectWorkflow/unitsQuery.json"));
        when(handler.getJsonFromWorkspace("query.json")).thenReturn(toJsonNode(auditQuery));
        when(handler.getOutput(0)).thenReturn(new ProcessingUri(UriPrefix.WORKSPACE,"StorageInfo/strategies.json"));

        Map<String, File> files = new HashMap<>();
        doAnswer((args) -> {
            File file = temporaryFolder.newFile();
            files.put(args.getArgument(0), file);
            return file;
        }).when(handler).getNewLocalFile(anyString());

        // When
        ItemStatus itemStatus = auditPreparePlugin.execute(workerParameters, handler);

        // Then
        StatusCode globalStatus = itemStatus.getGlobalStatus();
        assertThat(globalStatus).isEqualTo(StatusCode.OK);


        List<String> lines = IOUtils.readLines(new FileInputStream(files.get(AuditPreparePlugin.OBJECT_GROUPS_TO_AUDIT_JSONL)),
                "UTF-8");
        assertThat(lines.size()).isEqualTo(5);
        JsonLineModel firstLine = JsonHandler.getFromString(lines.get(0), JsonLineModel.class);
        assertThat(firstLine.getId()).isEqualTo("aebaaaaaaahgotryaauzialjp5zkhgyaaaaq");
        assertThat(firstLine.getParams()).isNotNull();
        AuditObjectGroup firstDetailLine = JsonHandler.getFromJsonNode(firstLine.getParams(), AuditObjectGroup.class);
        assertThat(firstDetailLine.getObjects()).isNotEmpty();
        assertThat(firstDetailLine.getObjects().size()).isEqualTo(1);
        assertThat(firstDetailLine.getObjects().get(0).getQualifier()).isEqualTo("BinaryMaster");
        assertThat(firstDetailLine.getObjects().get(0).getId()).isEqualTo("aeaaaaaaaahgotryaauzialjp5zkhgiaaaaq");
        assertThat(firstDetailLine.getObjects().get(0).getOpi()).isEqualTo("aeeaaaaaachfa7z2aamwwaljp5zj3kqaaaaq");
        assertThat(firstDetailLine.getObjects().get(0).getAlgorithm()).isEqualTo("SHA-512");
        assertThat(firstDetailLine.getObjects().get(0).getMessageDigest()).isEqualTo("86c0bc701ef6b5dd21b080bc5bb2af38097baa6237275da83a52f092c9eae3e4e4b0247391620bd732fe824d18bd3bb6c37e62ec73a8cf3585c6a799399861b1");

        JsonLineModel thirdLine = JsonHandler.getFromString(lines.get(2), JsonLineModel.class);
        assertThat(thirdLine.getId()).isEqualTo("aebaaaaaaahgotryaauzialjp6aa32aaaaaq");
        assertThat(thirdLine.getParams()).isNotNull();
        AuditObjectGroup thirdDetailLine = JsonHandler.getFromJsonNode(thirdLine.getParams(), AuditObjectGroup.class);
        assertThat(thirdDetailLine.getObjects()).isNotEmpty();
        assertThat(thirdDetailLine.getObjects().size()).isEqualTo(2);
        assertThat(thirdDetailLine.getObjects().get(0).getQualifier()).isEqualTo("PhysicalMaster");
        assertThat(thirdDetailLine.getObjects().get(1).getQualifier()).isEqualTo("BinaryMaster");

        JsonNode strategies = JsonHandler.getFromInputStream(new FileInputStream(files.get("StorageInfo/strategies.json")));
        assertThat(strategies).isNotNull();
        List<StorageStrategy> strategiesFileResults = JsonHandler.getFromJsonNode(strategies, new TypeReference<List<StorageStrategy>>() { });
        assertThat(strategiesFileResults.size()).isEqualTo(1);
        assertThat(strategiesFileResults.get(0)).isNotNull();
        assertThat(strategiesFileResults.get(0).getId()).isEqualTo("default");
        assertThat(strategiesFileResults.get(0).getOffers().size()).isEqualTo(2);
    }

    @Test
    public void shouldFailFromStrategyRetrievalException() throws ContentAddressableStorageServerException, ProcessingException,
            InvalidParseOperationException, StorageServerClientException {

        // Given
        HandlerIO handler = mock(HandlerIO.class);
        WorkerParameters workerParameters = mock(WorkerParameters.class);

        JsonNode auditQuery = JsonHandler
                .getFromInputStream(getClass().getResourceAsStream("/AuditObjectWorkflow/unitsQuery.json"));
        when(handler.getJsonFromWorkspace("query.json")).thenReturn(toJsonNode(auditQuery));
        when(storageClient.getStorageStrategies()).thenThrow(new StorageServerClientException("Exception"));

        // When
        ItemStatus itemStatus = auditPreparePlugin.execute(workerParameters, handler);

        // Then
        StatusCode globalStatus = itemStatus.getGlobalStatus();
        assertThat(globalStatus).isEqualTo(StatusCode.FATAL);
    }

    @Test
    public void shouldFailFromStrategyRetrievalKO() throws ContentAddressableStorageServerException, ProcessingException,
            InvalidParseOperationException, StorageServerClientException {

        // Given
        HandlerIO handler = mock(HandlerIO.class);
        WorkerParameters workerParameters = mock(WorkerParameters.class);

        JsonNode auditQuery = JsonHandler
                .getFromInputStream(getClass().getResourceAsStream("/AuditObjectWorkflow/unitsQuery.json"));
        when(handler.getJsonFromWorkspace("query.json")).thenReturn(toJsonNode(auditQuery));
        when(storageClient.getStorageStrategies()).thenReturn(VitamCodeHelper.toVitamError(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR, "error"));

        // When
        ItemStatus itemStatus = auditPreparePlugin.execute(workerParameters, handler);

        // Then
        StatusCode globalStatus = itemStatus.getGlobalStatus();
        assertThat(globalStatus).isEqualTo(StatusCode.FATAL);
    }

    private RequestResponse<StorageStrategy> loadStorageStrategiesMock(){
        StorageStrategy defaultStrategy = new StorageStrategy();
        defaultStrategy.setId("default");
        OfferReference offer1 = new OfferReference();
        offer1.setId("offer1");
        OfferReference offer2 = new OfferReference();
        offer2.setId("offer1");
        List<OfferReference> offers = new ArrayList<>();
        offers.add(offer1);
        offers.add(offer2);
        defaultStrategy.setOffers(offers);
        return new RequestResponseOK<StorageStrategy>().addResult(defaultStrategy);
    }

}

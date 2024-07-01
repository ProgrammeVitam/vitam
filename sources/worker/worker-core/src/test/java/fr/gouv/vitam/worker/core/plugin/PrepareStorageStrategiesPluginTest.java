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
package fr.gouv.vitam.worker.core.plugin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.ProcessingUri;
import fr.gouv.vitam.common.model.processing.UriPrefix;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.referential.model.OfferReference;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageStrategy;
import fr.gouv.vitam.worker.common.HandlerIO;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PrepareStorageStrategiesPluginTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private StorageClientFactory storageClientFactory;

    @Mock
    private StorageClient storageClient;

    private PrepareStorageStrategiesPlugin prepareStorageStrategiesPlugin;

    @Before
    public void setUp() throws Exception {
        prepareStorageStrategiesPlugin = new PrepareStorageStrategiesPlugin(storageClientFactory);
        when(storageClientFactory.getClient()).thenReturn(storageClient);
        when(storageClient.getStorageStrategies()).thenReturn(loadStorageStrategiesMock());
    }

    @Test
    public void shouldCreateStrategiesOutput() throws ProcessingException, IOException, InvalidParseOperationException {
        // Given
        HandlerIO handler = mock(HandlerIO.class);
        WorkerParameters workerParameters = mock(WorkerParameters.class);

        when(handler.getOutput(0)).thenReturn(new ProcessingUri(UriPrefix.WORKSPACE, "StorageInfo/strategies.json"));

        Map<String, File> files = new HashMap<>();
        doAnswer(args -> {
            File file = temporaryFolder.newFile();
            files.put(args.getArgument(0), file);
            return file;
        })
            .when(handler)
            .getNewLocalFile(anyString());

        // When
        ItemStatus itemStatus = prepareStorageStrategiesPlugin.execute(workerParameters, handler);

        // Then
        StatusCode globalStatus = itemStatus.getGlobalStatus();
        assertThat(globalStatus).isEqualTo(StatusCode.OK);

        JsonNode strategies = JsonHandler.getFromInputStream(
            new FileInputStream(files.get("StorageInfo/strategies.json"))
        );
        assertThat(strategies).isNotNull();
        List<StorageStrategy> strategiesFileResults = JsonHandler.getFromJsonNode(strategies, new TypeReference<>() {});
        assertThat(strategiesFileResults.size()).isEqualTo(1);
        assertThat(strategiesFileResults.get(0)).isNotNull();
        assertThat(strategiesFileResults.get(0).getId()).isEqualTo("default");
        assertThat(strategiesFileResults.get(0).getOffers().size()).isEqualTo(2);
    }

    @Test
    public void shouldFailFromStrategyRetrievalException() throws ProcessingException, StorageServerClientException {
        // Given
        HandlerIO handler = mock(HandlerIO.class);
        WorkerParameters workerParameters = mock(WorkerParameters.class);

        when(storageClient.getStorageStrategies()).thenThrow(new StorageServerClientException("Exception"));

        // When
        ItemStatus itemStatus = prepareStorageStrategiesPlugin.execute(workerParameters, handler);

        // Then
        StatusCode globalStatus = itemStatus.getGlobalStatus();
        assertThat(globalStatus).isEqualTo(StatusCode.FATAL);
    }

    @Test
    public void shouldFailFromStrategyRetrievalKO() throws ProcessingException, StorageServerClientException {
        // Given
        HandlerIO handler = mock(HandlerIO.class);
        WorkerParameters workerParameters = mock(WorkerParameters.class);

        when(storageClient.getStorageStrategies()).thenReturn(
            VitamCodeHelper.toVitamError(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR, "error", StorageStrategy.class)
        );

        // When
        ItemStatus itemStatus = prepareStorageStrategiesPlugin.execute(workerParameters, handler);

        // Then
        StatusCode globalStatus = itemStatus.getGlobalStatus();
        assertThat(globalStatus).isEqualTo(StatusCode.FATAL);
    }

    private RequestResponse<StorageStrategy> loadStorageStrategiesMock() {
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

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
package fr.gouv.vitam.worker.core.plugin.traceability;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.WorkspaceConstants;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.distribution.JsonLineGenericIterator;
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

import static fr.gouv.vitam.worker.core.plugin.traceability.TraceabilityLinkedCheckPreparePlugin.LOGBOOK_OPERATIONS_JSONL_FILE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TraceabilityLinkedCheckPreparePluginTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private LogbookOperationsClientFactory logbookOperationsClientFactory;

    @Mock
    private LogbookOperationsClient logbookOperationsClient;

    private TraceabilityLinkedCheckPreparePlugin traceabilityLinkedCheckPreparePlugin;

    private static final String LINKED_CHECK_TRACEABILITY_QUERY_JSON = "linkedCheckTraceability/query.json";
    private static final String LOGBOOKS_JSON = "linkedCheckTraceability/logbooks.json";
    private static final String LOGBOOKS_WITHOUT_EV_DATA_JSON = "linkedCheckTraceability/logbooks_without_evData.json";

    private static final TypeReference<JsonLineModel> JSONLINE_MODEL_TYPE_REFERENCE = new TypeReference<>() {};

    @Before
    public void setUp() {
        when(logbookOperationsClientFactory.getClient()).thenReturn(logbookOperationsClient);
        traceabilityLinkedCheckPreparePlugin = new TraceabilityLinkedCheckPreparePlugin(logbookOperationsClientFactory);
    }

    @Test
    public void should_generate_logbook_distribution_file() throws Exception {
        // Given
        InputStream query = PropertiesUtils.getResourceAsStream(LINKED_CHECK_TRACEABILITY_QUERY_JSON);
        File distributionFile = temporaryFolder.newFile();
        HandlerIO handlerIO = mock(HandlerIO.class);
        when(handlerIO.getJsonFromWorkspace(WorkspaceConstants.QUERY)).thenReturn(
            JsonHandler.getFromInputStream(query)
        );
        when(handlerIO.getNewLocalFile(LOGBOOK_OPERATIONS_JSONL_FILE)).thenReturn(distributionFile);

        JsonNode logbookResponse = JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(LOGBOOKS_JSON));
        when(logbookOperationsClient.selectOperation(any())).thenReturn(logbookResponse);

        File resultFile = temporaryFolder.newFile();
        doAnswer(invocation -> {
            File distributionFileCaptured = invocation.getArgument(1);

            try (FileOutputStream fileOutputStream = new FileOutputStream(resultFile)) {
                Files.copy(distributionFileCaptured.toPath(), fileOutputStream);
            }
            return null;
        })
            .when(handlerIO)
            .transferFileToWorkspace(
                ArgumentMatchers.eq(LOGBOOK_OPERATIONS_JSONL_FILE),
                any(),
                ArgumentMatchers.eq(true),
                ArgumentMatchers.eq(false)
            );

        // When
        ItemStatus itemStatus = traceabilityLinkedCheckPreparePlugin.execute(null, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
        JsonLineGenericIterator<JsonLineModel> lines = new JsonLineGenericIterator<>(
            new FileInputStream(resultFile),
            JSONLINE_MODEL_TYPE_REFERENCE
        );

        List<String> logbooksIds = lines.stream().map(JsonLineModel::getId).collect(Collectors.toList());
        assertThat(logbooksIds.size()).isEqualTo(4);
        assertThat(logbooksIds).contains(
            "aecaaaaaacc4hjizabv54alrxqcrb3qaaaaq",
            "aecaaaaaacc4hjizabv54alrxqct7aqaaaaq",
            "aecaaaaaacc4hjizabv54alrxqcxxxiaaaaq",
            "aecaaaaaacc4hjizabv54alrxqcz4uqaaaaq"
        );
    }

    @Test
    public void should_generate_logbook_distribution_file_with_warning() throws Exception {
        // Given
        InputStream query = PropertiesUtils.getResourceAsStream(LINKED_CHECK_TRACEABILITY_QUERY_JSON);
        File distributionFile = temporaryFolder.newFile();
        HandlerIO handlerIO = mock(HandlerIO.class);
        when(handlerIO.getJsonFromWorkspace(WorkspaceConstants.QUERY)).thenReturn(
            JsonHandler.getFromInputStream(query)
        );
        when(handlerIO.getNewLocalFile(LOGBOOK_OPERATIONS_JSONL_FILE)).thenReturn(distributionFile);

        JsonNode logbookResponse = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream(LOGBOOKS_WITHOUT_EV_DATA_JSON)
        );
        when(logbookOperationsClient.selectOperation(any())).thenReturn(logbookResponse);

        File resultFile = temporaryFolder.newFile();
        doAnswer(invocation -> {
            File distributionFileCaptured = invocation.getArgument(1);

            try (FileOutputStream fileOutputStream = new FileOutputStream(resultFile)) {
                Files.copy(distributionFileCaptured.toPath(), fileOutputStream);
            }
            return null;
        })
            .when(handlerIO)
            .transferFileToWorkspace(
                ArgumentMatchers.eq(LOGBOOK_OPERATIONS_JSONL_FILE),
                any(),
                ArgumentMatchers.eq(true),
                ArgumentMatchers.eq(false)
            );

        // When
        ItemStatus itemStatus = traceabilityLinkedCheckPreparePlugin.execute(null, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.WARNING);
        JsonLineGenericIterator<JsonLineModel> lines = new JsonLineGenericIterator<>(
            new FileInputStream(resultFile),
            JSONLINE_MODEL_TYPE_REFERENCE
        );

        List<String> logbooksIds = lines.stream().map(JsonLineModel::getId).collect(Collectors.toList());
        assertThat(logbooksIds.size()).isEqualTo(0);
    }
}

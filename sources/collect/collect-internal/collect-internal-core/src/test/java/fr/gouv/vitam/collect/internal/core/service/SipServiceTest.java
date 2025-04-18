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
package fr.gouv.vitam.collect.internal.core.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.culture.archivesdefrance.seda.v2.LegalStatusType;
import fr.gouv.vitam.collect.common.enums.TransactionStatus;
import fr.gouv.vitam.collect.common.exception.CollectInternalException;
import fr.gouv.vitam.collect.internal.core.common.ManifestContext;
import fr.gouv.vitam.collect.internal.core.common.TransactionModel;
import fr.gouv.vitam.collect.internal.core.repository.MetadataRepository;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.utils.ScrollSpliterator;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.common.tmp.TempFolderRule;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static fr.gouv.vitam.common.model.IngestWorkflowConstants.SEDA_FILE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

public class SipServiceTest {

    private static final Integer TENANT_ID = 0;
    private static final String UNITS_WITH_COMMON_OBJECT_GROUP_PATH = "streamZip/units_with_common_object_group.json";
    private static final WorkspaceClient workspaceClient = mock(WorkspaceClient.class);

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public TempFolderRule tempFolder = new TempFolderRule();

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Mock
    private MetadataRepository metadataRepository;

    @Mock
    private WorkspaceClientFactory workspaceClientFactory;

    private SipService sipService;

    @Before
    public void setUp() throws ContentAddressableStorageServerException {
        reset(workspaceClient);
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);
        when(workspaceClient.isExistingContainer(any())).thenReturn(true);

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
    }

    /**
     * This test makes sure we're not generating DataObjectGroup with the same id more than once
     */
    @Test
    @RunWithCustomExecutor
    public void should_not_duplicate_object_groups_in_manifest_file()
        throws CollectInternalException, IOException, InvalidParseOperationException, ContentAddressableStorageServerException {
        //Given
        final int maxElementsInQuery = 2; // Should be lower than units size to make sure there are several partitions

        // Load units from JSON
        final List<JsonNode> unitsJson = JsonHandler.getFromFileAsTypeReference(
            PropertiesUtils.getResourceFile(UNITS_WITH_COMMON_OBJECT_GROUP_PATH),
            new TypeReference<>() {}
        );

        // Making sure that maxElementsInQuery is lower than units size
        assertThat(maxElementsInQuery).isLessThan(unitsJson.size());

        // Extract transaction id from first unit
        final String transactionId = unitsJson.get(0).get("#opi").asText();

        // Make selectUnits return the units
        when(metadataRepository.selectUnits(any(SelectMultiQuery.class), eq(transactionId))).thenReturn(
            new ScrollSpliterator<>(
                mock(SelectMultiQuery.class),
                query -> new RequestResponseOK<JsonNode>().addAllResults(new ArrayList<>(unitsJson)),
                0,
                0
            )
        );

        // Make selectObjectGroups return object groups corresponding to the ids in the query
        final ArgumentCaptor<JsonNode> jsonNodeArgumentCaptor = ArgumentCaptor.forClass(JsonNode.class);
        when(metadataRepository.selectObjectGroups(jsonNodeArgumentCaptor.capture(), eq(transactionId))).thenAnswer(
            invocation -> {
                final List<String> objectGroupIds = StreamSupport.stream(
                    jsonNodeArgumentCaptor.getValue().get("$query").get("$in").get("#id").spliterator(),
                    false
                )
                    .map(JsonNode::asText)
                    .toList();
                return JsonHandler.getFromString(
                    "{'$results': [" +
                    objectGroupIds
                        .stream()
                        .map(ogId -> "{'#id': '" + ogId + "', '#qualifiers': [{'versions': []}]}")
                        .collect(Collectors.joining(",")) +
                    "]}"
                );
            }
        );

        // We capture the resulting stream (manifest.xml) to be able to test its content
        final AtomicReference<String> capturedText = new AtomicReference<>();
        doAnswer(invocation -> {
            final InputStream inputStream = invocation.getArgument(2); // 3rd argument is InputStream
            capturedText.set(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
            return null;
        })
            .when(workspaceClient)
            .putObject(eq(transactionId), eq(SEDA_FILE), any(InputStream.class));

        final TransactionModel transactionModel = getTransactionModel(transactionId);
        sipService = new SipService(workspaceClientFactory, metadataRepository, maxElementsInQuery);

        //When
        sipService.generateSip(transactionModel);

        //Then
        final Set<String> expectedObjectGroupIds = unitsJson
            .stream()
            .map(unit -> unit.get("#object").asText())
            .collect(Collectors.toSet());
        final String manifestXml = capturedText.get();
        expectedObjectGroupIds.forEach(
            expectedObjectGroupId ->
                assertThat(manifestXml).containsOnlyOnce("<DataObjectGroup id=\"" + expectedObjectGroupId + "\"")
        );
    }

    private static TransactionModel getTransactionModel(String id) {
        return new TransactionModel(
            id,
            null,
            new ManifestContext(
                "acquisitionInformation",
                LegalStatusType.PUBLIC_ARCHIVE.value(),
                "archivalAgreement",
                "messageIdentifier",
                "archivalAgencyIdentifier",
                "transferringAgencyIdentifier",
                "originatingAgencyIdentifier",
                "submissionAgencyIdentifier",
                "archivalProfil",
                "comment"
            ),
            TransactionStatus.OPEN,
            null,
            null,
            null,
            null,
            null
        );
    }
}

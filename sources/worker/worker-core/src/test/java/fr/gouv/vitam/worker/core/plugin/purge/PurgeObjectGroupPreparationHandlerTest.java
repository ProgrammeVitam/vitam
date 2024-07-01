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
package fr.gouv.vitam.worker.core.plugin.purge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.batch.report.model.entry.PurgeObjectGroupReportEntry;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.collection.CloseableIteratorUtils;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

public class PurgeObjectGroupPreparationHandlerTest {

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private MetaDataClientFactory metaDataClientFactory;

    @Mock
    private MetaDataClient metaDataClient;

    @Mock
    private PurgeReportService purgeReportService;

    private ArrayList<PurgeObjectGroupReportEntry> reportEntries;

    @Mock
    private HandlerIO handler;

    private WorkerParameters params;

    @Before
    public void setUp() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        VitamThreadUtils.getVitamSession().setRequestId("opId");

        doAnswer(args -> tempFolder.newFile(args.getArgument(0))).when(handler).getNewLocalFile(any());

        doReturn(metaDataClient).when(metaDataClientFactory).getClient();

        params = WorkerParametersFactory.newWorkerParameters()
            .setWorkerGUID(GUIDFactory.newGUID().getId())
            .setContainerName(VitamThreadUtils.getVitamSession().getRequestId())
            .setRequestId(VitamThreadUtils.getVitamSession().getRequestId())
            .setProcessId(VitamThreadUtils.getVitamSession().getRequestId())
            .setObjectName("REF")
            .setCurrentStep("StepName");

        doReturn(VitamThreadUtils.getVitamSession().getRequestId()).when(handler).getContainerName();

        reportEntries = new ArrayList<>();
        doAnswer(args -> reportEntries.addAll(args.getArgument(1)))
            .when(purgeReportService)
            .appendObjectGroupEntries(any(), any());
    }

    @Test
    @RunWithCustomExecutor
    public void testExecute_OK() throws Exception {
        doReturn(
            CloseableIteratorUtils.toCloseableIterator(
                asList("id_got_1", "id_got_2", "id_got_3", "id_got_4", "id_got_5").iterator()
            )
        )
            .when(purgeReportService)
            .exportDistinctObjectGroups(any(), any());

        JsonNode objectGroups = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream(
                "EliminationAction/EliminationActionObjectGroupPreparationHandler/objectGroups.json"
            )
        );
        doReturn(objectGroups).when(metaDataClient).selectObjectGroups(any());

        JsonNode existingUnits = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream(
                "EliminationAction/EliminationActionObjectGroupPreparationHandler/existingUnits.json"
            )
        );
        doReturn(existingUnits).when(metaDataClient).selectUnits(any());

        PurgeObjectGroupPreparationHandler instance = new PurgeObjectGroupPreparationHandler(
            "PLUGIN_NAME",
            metaDataClientFactory,
            purgeReportService,
            10
        );
        ItemStatus itemStatus = instance.execute(params, handler);

        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);

        assertThat(reportEntries).hasSize(4);
        checkReportEntry(
            reportEntries,
            "id_got_1",
            "sp_1",
            "opi1",
            Arrays.asList("id_got_1_object_1", "id_got_1_object_2"),
            null,
            PurgeObjectGroupStatus.DELETED
        );
        checkReportEntry(
            reportEntries,
            "id_got_2",
            "sp_2",
            "opi2",
            null,
            Arrays.asList("id_unit_20"),
            PurgeObjectGroupStatus.PARTIAL_DETACHMENT
        );
        checkReportEntry(
            reportEntries,
            "id_got_3",
            "sp_3",
            "opi3",
            Arrays.asList("id_got_3_object_1"),
            null,
            PurgeObjectGroupStatus.DELETED
        );
        checkReportEntry(
            reportEntries,
            "id_got_5",
            "sp_5",
            "opi5",
            null,
            Arrays.asList("id_unit_50", "id_unit_51"),
            PurgeObjectGroupStatus.PARTIAL_DETACHMENT
        );

        ArgumentCaptor<File> objectGroupsToDeleteFileArgCaptor = ArgumentCaptor.forClass(File.class);
        ArgumentCaptor<File> objectGroupsToDetachFileArgCaptor = ArgumentCaptor.forClass(File.class);
        verify(handler).transferFileToWorkspace(
            eq(PurgeObjectGroupPreparationHandler.OBJECT_GROUPS_TO_DELETE_FILE),
            objectGroupsToDeleteFileArgCaptor.capture(),
            eq(true),
            eq(false)
        );
        verify(handler).transferFileToWorkspace(
            eq(PurgeObjectGroupPreparationHandler.OBJECT_GROUPS_TO_DETACH_FILE),
            objectGroupsToDetachFileArgCaptor.capture(),
            eq(true),
            eq(false)
        );

        List<JsonLineModel> objectGroupsToDelete = FileUtils.readLines(
            objectGroupsToDeleteFileArgCaptor.getValue(),
            StandardCharsets.UTF_8
        )
            .stream()
            .map(PurgeObjectGroupPreparationHandlerTest::parse)
            .collect(toList());

        assertThat(objectGroupsToDelete).hasSize(2);

        checkObjectGroupToDelete(objectGroupsToDelete, "id_got_1", "id_got_1_object_1", "id_got_1_object_2");
        checkObjectGroupToDelete(objectGroupsToDelete, "id_got_3", "id_got_3_object_1");

        List<JsonLineModel> objectGroupsToDetach = FileUtils.readLines(
            objectGroupsToDetachFileArgCaptor.getValue(),
            StandardCharsets.UTF_8
        )
            .stream()
            .map(PurgeObjectGroupPreparationHandlerTest::parse)
            .collect(toList());

        assertThat(objectGroupsToDetach).hasSize(2);
        checkObjectGroupToDetach(objectGroupsToDetach, "id_got_2", "id_unit_20");
        checkObjectGroupToDetach(objectGroupsToDetach, "id_got_5", "id_unit_50", "id_unit_51");
    }

    private void checkReportEntry(
        ArrayList<PurgeObjectGroupReportEntry> entries,
        String id,
        String sp,
        String opi,
        List<String> deletedObjectIds,
        List<String> deletedParentUnitIds,
        PurgeObjectGroupStatus status
    ) {
        PurgeObjectGroupReportEntry entry = entries.stream().filter(e -> e.getId().equals(id)).findFirst().get();

        assertThat(entry.getId()).isEqualTo(id);
        assertThat(entry.getOriginatingAgency()).isEqualTo(sp);
        assertThat(entry.getInitialOperation()).isEqualTo(opi);
        assertThat(entry.getStatus()).isEqualTo(status.name());
        assertThat(SetUtils.emptyIfNull(entry.getObjectIds())).containsExactlyInAnyOrder(
            ListUtils.emptyIfNull(deletedObjectIds).toArray(new String[0])
        );
        assertThat(SetUtils.emptyIfNull(entry.getDeletedParentUnitIds())).containsExactlyInAnyOrder(
            ListUtils.emptyIfNull(deletedParentUnitIds).toArray(new String[0])
        );
    }

    private void checkObjectGroupToDetach(
        List<JsonLineModel> objectGroupsToDetach,
        String id,
        String... removedParentIds
    ) throws InvalidParseOperationException {
        JsonLineModel objectGroupToDetach = objectGroupsToDetach
            .stream()
            .filter(o -> o.getId().equals(id))
            .findFirst()
            .get();

        assertThat(objectGroupToDetach.getId()).isEqualTo(id);
        assertThat(objectGroupToDetach.getDistribGroup()).isNull();
        assertThat(JsonHandler.getFromJsonNode(objectGroupToDetach.getParams(), Set.class)).containsExactlyInAnyOrder(
            removedParentIds
        );
    }

    private void checkObjectGroupToDelete(List<JsonLineModel> objectGroupsToDelete, String id, String... objectIds)
        throws InvalidParseOperationException {
        JsonLineModel objectGroupToDelete = objectGroupsToDelete
            .stream()
            .filter(o -> o.getId().equals(id))
            .findFirst()
            .get();

        assertThat(objectGroupToDelete.getDistribGroup()).isNull();
        ObjectNode expectedResponse = JsonHandler.createObjectNode();
        ArrayNode objects = JsonHandler.createArrayNode();
        for (String objectId : objectIds) {
            objects.add(JsonHandler.createObjectNode().put("id", objectId).put("strategyId", "default"));
        }
        expectedResponse.set("objects", objects);
        expectedResponse.put("strategyId", "default");
        assertThat(objectGroupToDelete.getParams().get("strategyId").asText()).isEqualTo("default");
        assertThat(objectGroupToDelete.getParams().get("objects").isArray()).isTrue();
        for (String objectId : objectIds) {
            assertThat(((ArrayNode) objectGroupToDelete.getParams().get("objects"))).contains(
                JsonHandler.createObjectNode().put("id", objectId).put("strategyId", "default")
            );
        }
    }

    private static JsonLineModel parse(String line) {
        try {
            return JsonHandler.getFromString(line, JsonLineModel.class);
        } catch (InvalidParseOperationException e) {
            throw new RuntimeException(e);
        }
    }
}

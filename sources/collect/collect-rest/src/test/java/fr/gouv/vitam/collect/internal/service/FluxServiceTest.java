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

package fr.gouv.vitam.collect.internal.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.collect.external.dto.ObjectDto;
import fr.gouv.vitam.collect.external.dto.ProjectDto;
import fr.gouv.vitam.collect.external.dto.TransactionDto;
import fr.gouv.vitam.collect.internal.helpers.builders.DbObjectGroupModelBuilder;
import fr.gouv.vitam.collect.internal.model.CollectUnitModel;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.utils.ScrollSpliterator;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.DataObjectVersionType;
import fr.gouv.vitam.common.model.objectgroup.DbObjectGroupModel;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.common.tmp.TempFolderRule;
import net.javacrumbs.jsonunit.JsonAssert;
import net.javacrumbs.jsonunit.core.Option;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static fr.gouv.vitam.collect.internal.service.FluxService.METADATA_CSV_FILE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FluxServiceTest {

    private static final int TENANT_ID = 0;

    private static final String UNITS_PATH = "streamZip/units.json";

    private static final String OBJECTGROUPS_PATH = "streamZip/objectgroups.json";

    private static final String UNITS_WITH_GRAPH_PATH = "streamZip/units_with_graph.json";

    private static final String QUERIES_PATH = "streamZip/queries.json";

    private static final String TRANSACTION_ZIP_PATH = "streamZip/transaction.zip";

    private static final String TRANSACTION2_ZIP_PATH = "streamZip/transaction2.zip";

    @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule public TempFolderRule tempFolder = new TempFolderRule();

    @Rule public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @Mock private CollectService collectService;

    @Mock private MetadataService metadataService;

    FluxService fluxService;

    @Before
    public void setUp() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        fluxService = new FluxService(collectService, metadataService);
    }

    @Test
    @RunWithCustomExecutor
    public void processStream() throws Exception {
        TransactionDto transactionDto = new TransactionDto();
        transactionDto.setId("TRANSACTION_ID");
        ProjectDto projectDto = new ProjectDto();
        projectDto.setId("PROJECT_ID");
        Map<String, JsonNode> units = new HashMap<>();
        Map<String, JsonNode> objectGroups = new HashMap<>();
        when(metadataService.saveArchiveUnit(any())).thenAnswer(e -> {
            final JsonNode unit = e.getArgument(0);
            units.put(unit.get(VitamFieldsHelper.id()).asText(), unit);
            return JsonHandler.toJsonNode(new RequestResponseOK<>(JsonHandler.createObjectNode(), List.of(unit), 1));
        });

        when(metadataService.selectUnitById(any())).thenAnswer(e -> {
            final JsonNode unit = units.get((String) e.getArgument(0));
            return JsonHandler.toJsonNode(new RequestResponseOK<>(JsonHandler.createObjectNode(), List.of(unit), 1));
        });

        when(collectService.updateOrSaveObjectGroup(any(), any(), anyInt(), any())).thenAnswer(e -> {
            var unit = units.get(((CollectUnitModel) e.getArgument(0)).getId());
            var usage = (DataObjectVersionType) e.getArgument(1);
            int version = e.getArgument(2);
            var objectDto = (ObjectDto) e.getArgument(3);

            DbObjectGroupModel dbObjectGroupModel = new DbObjectGroupModelBuilder().withId(
                    GUIDFactory.newObjectGroupGUID(ParameterHelper.getTenantParameter()).getId())
                .withOpi(unit.get(VitamFieldsHelper.initialOperation()).asText())
                .withFileInfoModel(objectDto.getFileInfo().getFileName())
                .withQualifiers(objectDto.getId(), objectDto.getFileInfo().getFileName(), usage, version).build();


            ((ObjectNode) unit).put(VitamFieldsHelper.object(), dbObjectGroupModel.getId());

            units.put(unit.get(VitamFieldsHelper.id()).asText(), unit);
            final JsonNode objectGroup = JsonHandler.toJsonNode(dbObjectGroupModel);
            objectGroups.put(dbObjectGroupModel.getId(), objectGroup);

            return new ObjectDto(dbObjectGroupModel.getId(), null);
        });

        when(metadataService.selectObjectGroupById(any(), anyBoolean())).thenAnswer(e -> {
            final JsonNode og = objectGroups.get((String) e.getArgument(0));
            return JsonHandler.toJsonNode(new RequestResponseOK<>(JsonHandler.createObjectNode(), List.of(og), 1));
        });

        try (final InputStream resourceAsStream = PropertiesUtils.getResourceAsStream(TRANSACTION_ZIP_PATH)) {
            fluxService.processStream(resourceAsStream, transactionDto, projectDto);
        }


        final JsonNode expectedUnits = JsonHandler.getFromFile(PropertiesUtils.getResourceFile(UNITS_PATH));

        JsonAssert.assertJsonEquals(units.values(), expectedUnits, JsonAssert.when(Option.IGNORING_ARRAY_ORDER)
            .whenIgnoringPaths(List.of("[*]." + VitamFieldsHelper.id(), "[*]." + VitamFieldsHelper.unitups(),
                "[*]." + VitamFieldsHelper.object())));



        final JsonNode expectedGots = JsonHandler.getFromFile(PropertiesUtils.getResourceFile(OBJECTGROUPS_PATH));

        JsonAssert.assertJsonEquals(JsonHandler.toJsonNode(objectGroups.values()), expectedGots,
            JsonAssert.when(Option.IGNORING_ARRAY_ORDER)
                .whenIgnoringPaths(List.of("[*]." + "_id", "[*]." + "_qualifiers[*].versions[*]._id")));
    }

    @Test
    @RunWithCustomExecutor
    public void processStream_with_metadata_update() throws Exception {
        TransactionDto transactionDto = new TransactionDto();
        transactionDto.setId("TRANSACTION_ID");
        ProjectDto projectDto = new ProjectDto();
        projectDto.setId("PROJECT_ID");
        Map<String, JsonNode> units = new HashMap<>();
        Map<String, JsonNode> objectGroups = new HashMap<>();
        when(metadataService.saveArchiveUnit(any())).thenAnswer(e -> {
            final JsonNode unit = e.getArgument(0);
            units.put(unit.get(VitamFieldsHelper.id()).asText(), unit);
            return JsonHandler.toJsonNode(new RequestResponseOK<>(JsonHandler.createObjectNode(), List.of(unit), 1));
        });

        when(metadataService.selectUnitById(any())).thenAnswer(e -> {
            final JsonNode unit = units.get((String) e.getArgument(0));
            return JsonHandler.toJsonNode(new RequestResponseOK<>(JsonHandler.createObjectNode(), List.of(unit), 1));
        });

        when(collectService.updateOrSaveObjectGroup(any(), any(), anyInt(), any())).thenAnswer(e -> {
            var unit = units.get(((CollectUnitModel) e.getArgument(0)).getId());
            var usage = (DataObjectVersionType) e.getArgument(1);
            int version = e.getArgument(2);
            var objectDto = (ObjectDto) e.getArgument(3);

            DbObjectGroupModel dbObjectGroupModel = new DbObjectGroupModelBuilder().withId(
                    GUIDFactory.newObjectGroupGUID(ParameterHelper.getTenantParameter()).getId())
                .withOpi(unit.get(VitamFieldsHelper.initialOperation()).asText())
                .withFileInfoModel(objectDto.getFileInfo().getFileName())
                .withQualifiers(objectDto.getId(), objectDto.getFileInfo().getFileName(), usage, version).build();


            ((ObjectNode) unit).put(VitamFieldsHelper.object(), dbObjectGroupModel.getId());

            units.put(unit.get(VitamFieldsHelper.id()).asText(), unit);
            final JsonNode objectGroup = JsonHandler.toJsonNode(dbObjectGroupModel);
            objectGroups.put(dbObjectGroupModel.getId(), objectGroup);

            return new ObjectDto(dbObjectGroupModel.getId(), null);
        });

        when(metadataService.selectObjectGroupById(any(), anyBoolean())).thenAnswer(e -> {
            final JsonNode og = objectGroups.get((String) e.getArgument(0));
            return JsonHandler.toJsonNode(new RequestResponseOK<>(JsonHandler.createObjectNode(), List.of(og), 1));
        });

        final List<JsonNode> unitsJson =
            JsonHandler.getFromFileAsTypeReference(PropertiesUtils.getResourceFile(UNITS_WITH_GRAPH_PATH),
                new TypeReference<>() {
                });

        when(metadataService.selectUnits(any(SelectMultiQuery.class), any())).thenReturn(
            new ScrollSpliterator<>(mock(SelectMultiQuery.class),
                (query) -> new RequestResponseOK<JsonNode>().addAllResults(new ArrayList<>(unitsJson)), 0, 0));

        final AtomicReference<File> fileReference = new AtomicReference<>();
        when(collectService.pushStreamToWorkspace(any(), any(InputStream.class), eq(METADATA_CSV_FILE))).thenAnswer(
            (e) -> {
                final InputStream is = e.getArgument(1);
                final File file = tempFolder.newFile(METADATA_CSV_FILE);
                Files.copy(is, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                fileReference.set(file);
                return "";
            });

        when(collectService.getInputStreamFromWorkspace(any(), eq(METADATA_CSV_FILE))).thenAnswer(
            (e) -> new FileInputStream(fileReference.get()));


        AtomicReference<List<JsonNode>> requestReference = new AtomicReference<>();

        when(metadataService.atomicBulkUpdate(any())).thenAnswer((e) -> {
            final List<JsonNode> argument = e.getArgument(0);
            requestReference.set(argument);
            return new RequestResponseOK<>().addAllResults(
                List.of(JsonHandler.toJsonNode(
                    new RequestResponseOK<>().addResult(JsonHandler.createObjectNode().put("#status", "OK")))));
        });

        try (final InputStream resourceAsStream = PropertiesUtils.getResourceAsStream(TRANSACTION2_ZIP_PATH)) {
            fluxService.processStream(resourceAsStream, transactionDto, projectDto);
        }

        final JsonNode expectedQueries =
            JsonHandler.getFromFile(PropertiesUtils.getResourceFile(QUERIES_PATH));

        JsonAssert.assertJsonEquals(JsonHandler.toJsonNode(requestReference.get()), expectedQueries);
    }

    @Test
    @RunWithCustomExecutor
    public void uploadArchiveUnit() {
    }

    @Test
    @RunWithCustomExecutor
    public void getUnitParentByPath() {
    }

    @Test
    @RunWithCustomExecutor
    public void uploadObjectGroup() {
    }

    @Test
    @RunWithCustomExecutor
    public void uploadBinary() {
    }
}
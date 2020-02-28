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
package fr.gouv.vitam.metadata.core.migration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Lists;
import com.mongodb.client.MongoCollection;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.collection.CloseableIterator;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.database.collections.ObjectGroup;
import fr.gouv.vitam.metadata.core.database.collections.Unit;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.mongodb.client.model.Filters.eq;
import static org.assertj.core.api.Assertions.assertThat;

public class DataMigrationRepositoryTest {

    private static final String UNIT_COLLECTION = "Unit" + GUIDFactory.newGUID().getId();
    private static final String OBJECT_GROUP_COLLECTION = "Got" + GUIDFactory.newGUID().getId();

    private static final int NB_UNITS = 30;
    private static final int NB_OBJECT_GROUPS = 15;
    private static final int TEST_BULK_SIZE = 10;
    private static DataMigrationRepository repository;

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    @ClassRule
    public static MongoRule mongoRule =
        new MongoRule(VitamCollection.getMongoClientOptions(Lists.newArrayList(Unit.class, ObjectGroup.class)),
            UNIT_COLLECTION,
            OBJECT_GROUP_COLLECTION);

    @BeforeClass
    public static void setUpBeforeClass() {
        MetadataCollections.UNIT.getVitamCollection().setName(UNIT_COLLECTION);
        MetadataCollections.UNIT.getVitamCollection().initialize(mongoRule.getMongoDatabase(), false);
        MetadataCollections.OBJECTGROUP.getVitamCollection().setName(OBJECT_GROUP_COLLECTION);
        MetadataCollections.OBJECTGROUP.getVitamCollection().initialize(mongoRule.getMongoDatabase(), false);
        repository = new DataMigrationRepository(TEST_BULK_SIZE);
    }

    @AfterClass
    public static void afterClass() {
        mongoRule.handleAfterClass();
    }

    @After
    public void tearDown() {
        mongoRule.handleAfter();
    }

    @Test
    public void testSelectUnitBulkInTopDownHierarchyLevel_emptyDataSet() {

        // Given / When
        try (CloseableIterator<List<Unit>> listCloseableIterator = repository.selectUnitBulkInTopDownHierarchyLevel()) {

            // Then
            assertThat(listCloseableIterator.hasNext()).isFalse();
        }
    }

    @Test
    public void testSelectUnitBulkInTopDownHierarchyLevel_unitHierarchyNotYetMigrated() throws Exception {

        // Given : Complex data set with R6 model (no graph fields)
        String dataSetFile = "DataMigrationR6/30UnitDataSet/R6UnitDataSet.json";
        importUnitDataSetFile(dataSetFile);

        // When
        try (CloseableIterator<List<Unit>> listCloseableIterator = repository.selectUnitBulkInTopDownHierarchyLevel()) {

            // Then
            Set<String> knownIds = new HashSet<>();

            listCloseableIterator.forEachRemaining(unitBulk -> {

                // Ensure that data is chunked with
                assertThat(unitBulk).size().isLessThanOrEqualTo(TEST_BULK_SIZE);

                // Ensure that all parents are proceeded in order
                for (Unit unit : unitBulk) {
                    assertThat(unit.getCollectionOrEmpty(Unit.UP)).isSubsetOf(knownIds);
                }

                for (Unit unit : unitBulk) {
                    knownIds.add(unit.getId());
                }
            });

            assertThat(knownIds).size().isEqualTo(NB_UNITS);
        }
    }

    @Test
    public void testSelectUnitBulkInTopDownHierarchyLevel_unitHierarchyAlreadyMigrated() throws Exception {

        // Given : Complex data set with R7 model (with graph fields)
        String dataSetFile = "DataMigrationR6/30UnitDataSet/R7UnitDataSet.json";
        importUnitDataSetFile(dataSetFile);

        // When
        try (CloseableIterator<List<Unit>> listCloseableIterator = repository.selectUnitBulkInTopDownHierarchyLevel()) {

            // Then
            assertThat(listCloseableIterator.hasNext()).isFalse();
        }
    }

    @Test
    public void testGetUnitGraphByIds_emptyIdSet() throws Exception {

        // Given : Complex data set with R6 model (no graph fields)
        String dataSetFile = "DataMigrationR6/30UnitDataSet/R6UnitDataSet.json";
        importUnitDataSetFile(dataSetFile);

        // When
        Map<String, Unit> unitGraphByIds = repository.getUnitGraphByIds(Collections.EMPTY_LIST);

        // Then
        assertThat(unitGraphByIds).isEmpty();
    }

    @Test
    public void testGetUnitGraphByIds_checkReturnedIds() throws Exception {

        // Given : Complex data set with R6 model (no graph fields)
        String dataSetFile = "DataMigrationR6/30UnitDataSet/R6UnitDataSet.json";
        importUnitDataSetFile(dataSetFile);

        // When
        Map<String, Unit> unitGraphByIds = repository.getUnitGraphByIds(Arrays.asList("1", "2", "6"));

        // Then
        assertThat(unitGraphByIds).size().isEqualTo(3);
        assertThat(unitGraphByIds.keySet()).containsExactlyInAnyOrder("1", "2", "6");

        for (String unitId : unitGraphByIds.keySet()) {
            assertThat(unitGraphByIds.get(unitId)).isNotNull();
            assertThat(unitGraphByIds.get(unitId).getId()).isEqualTo(unitId);
        }
    }

    @Test
    public void testBulkReplaceUnits_bulkUpdate() throws Exception {

        // Given : Complex data set with R6 model (no graph fields)
        String dataSetFile = "DataMigrationR6/30UnitDataSet/R6UnitDataSet.json";
        importUnitDataSetFile(dataSetFile);

        // When
        Unit u1 = (Unit) MetadataCollections.UNIT.getCollection().find(eq(Unit.ID, "1")).first();
        u1.put("TEST1", "VALUE1");

        Unit u6 = (Unit) MetadataCollections.UNIT.getCollection().find(eq(Unit.ID, "6")).first();
        u6.remove(Unit.ORIGINATING_AGENCY);
        u6.put("TEST6", "VALUE6");

        repository.bulkReplaceUnits(Arrays.asList(u1, u6));

        // Then
        Unit u1_updated = (Unit) MetadataCollections.UNIT.getCollection().find(eq(Unit.ID, "1")).first();
        Unit u6_updated = (Unit) MetadataCollections.UNIT.getCollection().find(eq(Unit.ID, "6")).first();

        assertThat(u1_updated.get("TEST1")).isEqualTo("VALUE1");
        assertThat(u6_updated.get("TEST6")).isEqualTo("VALUE6");
        assertThat(u6_updated.get(Unit.ORIGINATING_AGENCY)).isNull();
    }

    @Test
    public void testSelectObjectGroupBulk_emptyDataSet() {

        // Given / When
        try (CloseableIterator<List<String>> listCloseableIterator = repository.selectObjectGroupBulk()) {

            // Then
            assertThat(listCloseableIterator.hasNext()).isFalse();
        }
    }

    @Test
    public void testSelectObjectGroupBulk_notYetMigrated() throws Exception {

        // Given : Complex data set with R6 model (no graph fields)
        String dataSetFile = "DataMigrationR6/15ObjectGroupDataSet/R6ObjectGroupDataSet.json";
        importObjectGroupDataSetFile(dataSetFile);

        // When
        try (CloseableIterator<List<String>> listCloseableIterator = repository.selectObjectGroupBulk()) {

            // Then
            Set<String> idSet = new HashSet<>();

            listCloseableIterator.forEachRemaining(objectGroupBulk -> {

                // Ensure that data is chunked with
                assertThat(objectGroupBulk).size().isLessThanOrEqualTo(TEST_BULK_SIZE);

                // Check uniqueness
                for (String objectGroupId : objectGroupBulk) {
                    assertThat(idSet.add(objectGroupId)).isTrue();
                }
            });

            assertThat(idSet).size().isEqualTo(NB_OBJECT_GROUPS);
        }
    }

    @Test
    public void testSelectObjectGroupBulk_alreadyMigrated() throws Exception {

        // Given : Complex data set with R7 model (with graph fields)
        String dataSetFile = "DataMigrationR6/15ObjectGroupDataSet/R7ObjectGroupDataSet.json";
        importObjectGroupDataSetFile(dataSetFile);

        // When
        try (CloseableIterator<List<String>> listCloseableIterator = repository.selectObjectGroupBulk()) {

            // Then
            assertThat(listCloseableIterator.hasNext()).isFalse();
        }
    }

    @Test
    public void testBulkUpgradeObjectGroups_bulkUpdate() throws Exception {

        // Given : Complex data set with R6 model (no graph fields)
        String dataSetFile = "DataMigrationR6/15ObjectGroupDataSet/R6ObjectGroupDataSet.json";
        importObjectGroupDataSetFile(dataSetFile);

        // When
        repository.bulkUpgradeObjectGroups(Arrays.asList("1", "6"));

        // Then
        ObjectGroup og6_updated =
            (ObjectGroup) MetadataCollections.OBJECTGROUP.getCollection().find(eq(ObjectGroup.ID, "6")).first();
        ObjectGroup og4_not_updated =
            (ObjectGroup) MetadataCollections.OBJECTGROUP.getCollection().find(eq(ObjectGroup.ID, "4")).first();

        assertThat(og6_updated.get(ObjectGroup.GRAPH_LAST_PERSISTED_DATE)).isNotNull();
        assertThat(og4_not_updated.get(ObjectGroup.GRAPH_LAST_PERSISTED_DATE)).isNull();
    }

    private void importUnitDataSetFile(String dataSetFile)
        throws FileNotFoundException, InvalidParseOperationException {
        InputStream inputDataSet = PropertiesUtils.getResourceAsStream(dataSetFile);
        ArrayNode jsonDataSet = (ArrayNode) JsonHandler.getFromInputStream(inputDataSet);
        for (JsonNode jsonNode : jsonDataSet) {
            MetadataCollections.UNIT.getCollection().insertOne(new Unit(JsonHandler.unprettyPrint(jsonNode)));
        }
    }

    private void importObjectGroupDataSetFile(String dataSetFile)
        throws FileNotFoundException, InvalidParseOperationException {
        InputStream inputDataSet = PropertiesUtils.getResourceAsStream(dataSetFile);
        ArrayNode jsonDataSet = (ArrayNode) JsonHandler.getFromInputStream(inputDataSet);
        for (JsonNode jsonNode : jsonDataSet) {
            mongoRule.getMongoCollection(OBJECT_GROUP_COLLECTION)
                .insertOne(new ObjectGroup(JsonHandler.unprettyPrint(jsonNode)));
        }
    }
}

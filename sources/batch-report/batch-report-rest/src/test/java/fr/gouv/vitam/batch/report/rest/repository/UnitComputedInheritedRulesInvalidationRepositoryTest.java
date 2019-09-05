package fr.gouv.vitam.batch.report.rest.repository;

import com.google.common.collect.Streams;
import com.mongodb.client.MongoCollection;
import fr.gouv.vitam.batch.report.model.UnitComputedInheritedRulesInvalidationModel;
import fr.gouv.vitam.batch.report.model.entry.UnitComputedInheritedRulesInvalidationReportEntry;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.collection.CloseableIterator;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.database.server.mongodb.SimpleMongoDBAccess;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import org.bson.Document;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static fr.gouv.vitam.batch.report.rest.repository.UnitComputedInheritedRulesInvalidationRepository.UNIT_COMPUTED_INHERITED_RULES_INVALIDATION_COLLECTION_NAME;
import static fr.gouv.vitam.common.database.collections.VitamCollection.getMongoClientOptions;
import static org.assertj.core.api.Assertions.assertThat;

@RunWithCustomExecutor
public class UnitComputedInheritedRulesInvalidationRepositoryTest {

    private final static String TEST_COLLECTION_NAME =
        UNIT_COMPUTED_INHERITED_RULES_INVALIDATION_COLLECTION_NAME + GUIDFactory.newGUID().getId();

    private static final String METADATA_UNIT_ID = UnitComputedInheritedRulesInvalidationModel.METADATA + "." +
        UnitComputedInheritedRulesInvalidationReportEntry.UNIT_ID;
    private static final String TENANT_ID = UnitComputedInheritedRulesInvalidationModel.TENANT;
    private static final String PROCESS_ID = UnitComputedInheritedRulesInvalidationModel.PROCESS_ID;

    @ClassRule
    public static RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor());

    @Rule
    public MongoRule mongoRule =
        new MongoRule(getMongoClientOptions(), TEST_COLLECTION_NAME);

    private UnitComputedInheritedRulesInvalidationRepository repository;

    private MongoCollection<Document> mongoCollection;

    @Before
    public void setUp() {
        MongoDbAccess mongoDbAccess = new SimpleMongoDBAccess(mongoRule.getMongoClient(), MongoRule.VITAM_DB);
        repository = new UnitComputedInheritedRulesInvalidationRepository(mongoDbAccess,
            TEST_COLLECTION_NAME);
        mongoCollection = mongoRule.getMongoCollection(TEST_COLLECTION_NAME);
        VitamThreadUtils.getVitamSession().setTenantId(0);
    }

    @Test
    public void bulkAppendUnits_singleCallOK() throws Exception {
        // Given
        List<String> units =
            Arrays.asList("unit1", "unit2", "unit4");
        // When
        repository.bulkAppendReport(buildReport(units, "procId1"));

        // Then
        long count = mongoCollection.countDocuments(and(
            eq(METADATA_UNIT_ID, "unit1"),
            eq(TENANT_ID, 0),
            eq(PROCESS_ID, "procId1")));
        assertThat(count).isEqualTo(1);
        assertThat(mongoCollection.countDocuments()).isEqualTo(3);
    }

    @Test
    public void bulkAppendUnits_noDuplicates() throws Exception {
        // Given
        List<String> units1 =
            Arrays.asList("unit1", "unit2", "unit4");
        List<String> units2 =
            Arrays.asList("unit3", "unit4");
        List<String> units3 =
            Collections.singletonList("unit5");
        // When
        repository.bulkAppendReport(buildReport(units1, "procId1"));
        repository.bulkAppendReport(buildReport(units2, "procId1"));
        repository.bulkAppendReport(buildReport(units3, "procId1"));
        // Then
        long count = mongoCollection.countDocuments(and(
            eq(METADATA_UNIT_ID, "unit1"),
            eq(TENANT_ID, 0),
            eq(PROCESS_ID, "procId1")));
        assertThat(count).isEqualTo(1);
        assertThat(mongoCollection.countDocuments()).isEqualTo(5);
    }

    @Test
    public void bulkAppendUnits_multiProcess() throws Exception {
        // Given
        List<String> units1 =
            Arrays.asList("unit1", "unit2", "unit4");
        List<String> units2 =
            Arrays.asList("unit3", "unit4");
        // When
        repository.bulkAppendReport(buildReport(units1, "procId1"));
        repository.bulkAppendReport(buildReport(units2, "procId2"));
        // Then
        long count = mongoCollection.countDocuments(and(
            eq(METADATA_UNIT_ID, "unit4"),
            eq(TENANT_ID, 0),
            eq(PROCESS_ID, "procId1")));
        assertThat(count).isEqualTo(1);
        assertThat(mongoCollection.countDocuments()).isEqualTo(5);
    }

    @Test
    public void deleteUnitsAndProgeny_OK() throws Exception {
        // Given
        List<String> units1 =
            Arrays.asList("unit1", "unit2", "unit4");
        List<String> units2 =
            Arrays.asList("unit3", "unit4");

        // When
        repository.bulkAppendReport(buildReport(units1, "procId1"));
        repository.bulkAppendReport(buildReport(units2, "procId2"));
        assertThat(mongoCollection.countDocuments()).isEqualTo(5);

        repository.deleteReportByIdAndTenant("procId1", VitamThreadUtils.getVitamSession().getTenantId());

        // Then
        assertThat(mongoCollection.countDocuments()).isEqualTo(2);
    }

    @Test
    public void deleteUnitsAndProgeny_EmptyOK() throws Exception {
        // Given

        // When
        repository.deleteReportByIdAndTenant("procId1", VitamThreadUtils.getVitamSession().getTenantId());

        // Then
        assertThat(mongoCollection.countDocuments()).isEqualTo(0);
    }

    @Test
    public void findCollectionByProcessIdTenant() throws Exception {
        // Given
        List<String> units1 =
            Arrays.asList("unit1", "unit2", "unit4");
        List<String> units2 =
            Arrays.asList("unit3", "unit4");
        // When
        repository.bulkAppendReport(buildReport(units1, "procId1"));
        repository.bulkAppendReport(buildReport(units2, "procId2"));
        CloseableIterator<Document> documents =
            repository.findCollectionByProcessIdTenant("procId1", VitamThreadUtils.getVitamSession().getTenantId());
        // Then
        List<String> unitIds = Streams.stream(documents)
            .map(doc -> doc.getString("id"))
            .collect(Collectors.toList());
        assertThat(unitIds).containsExactlyInAnyOrderElementsOf(units1);
    }

    private List<UnitComputedInheritedRulesInvalidationModel> buildReport(List<String> units, String processId) {
        return units
            .stream()
            .map(
                unitId -> new UnitComputedInheritedRulesInvalidationModel(processId,
                    VitamThreadUtils.getVitamSession().getTenantId(),
                    LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()),
                    new UnitComputedInheritedRulesInvalidationReportEntry(unitId))
            ).collect(Collectors.toList());
    }
}

package fr.gouv.vitam.batch.report.rest.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.database.server.mongodb.SimpleMongoDBAccess;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import org.assertj.core.api.Assertions;
import org.bson.Document;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static fr.gouv.vitam.batch.report.rest.repository.InvalidUnitsRepository.INVALID_UNITS_COLLECTION_NAME;
import static fr.gouv.vitam.common.database.collections.VitamCollection.getMongoClientOptions;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@RunWithCustomExecutor
public class InvalidUnitsRepositoryTest {

    private final static String TEST_COLLECTION_NAME = INVALID_UNITS_COLLECTION_NAME + GUIDFactory.newGUID().getId();

    @ClassRule
    public static RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor());

    @Rule
    public MongoRule mongoRule =
        new MongoRule(getMongoClientOptions(), TEST_COLLECTION_NAME);

    private InvalidUnitsRepository repository;

    private MongoCollection<Document> mongoCollection;

    @Before
    public void setUp() {
        MongoDbAccess mongoDbAccess = new SimpleMongoDBAccess(mongoRule.getMongoClient(), MongoRule.VITAM_DB);
        repository = new InvalidUnitsRepository(mongoDbAccess, TEST_COLLECTION_NAME);
        mongoCollection = mongoRule.getMongoCollection(TEST_COLLECTION_NAME);
        VitamThreadUtils.getVitamSession().setTenantId(0);
    }

    @Test
    public void bulkAppendUnits_singleCallOK() throws Exception {
        // Given
        List<String> units =
            Arrays.asList("unit1", "unit2", "unit4");
        // When
        repository.bulkAppendUnits(units, "procId1");
        // Then
        long count = mongoCollection.countDocuments(and(
            eq(InvalidUnitsRepository.UNIT_ID, "unit1"),
            eq(InvalidUnitsRepository.TENANT_ID, 0),
            eq(InvalidUnitsRepository.PROCESS_ID, "procId1")));
        assertThat(count).isEqualTo(1);
        Assertions.assertThat(mongoCollection.countDocuments()).isEqualTo(3);
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
        repository.bulkAppendUnits(units1, "procId1");
        repository.bulkAppendUnits(units2, "procId1");
        repository.bulkAppendUnits(units3, "procId1");
        // Then
        long count = mongoCollection.countDocuments(and(
            eq(InvalidUnitsRepository.UNIT_ID, "unit1"),
            eq(InvalidUnitsRepository.TENANT_ID, 0),
            eq(InvalidUnitsRepository.PROCESS_ID, "procId1")));
        assertThat(count).isEqualTo(1);
        Assertions.assertThat(mongoCollection.countDocuments()).isEqualTo(5);
    }

    @Test
    public void bulkAppendUnits_multiProcess() throws Exception {
        // Given
        List<String> units1 =
            Arrays.asList("unit1", "unit2", "unit4");
        List<String> units2 =
            Arrays.asList("unit3", "unit4");
        // When
        repository.bulkAppendUnits(units1, "procId1");
        repository.bulkAppendUnits(units2, "procId2");
        // Then
        long count = mongoCollection.countDocuments(and(
            eq(InvalidUnitsRepository.UNIT_ID, "unit4"),
            eq(InvalidUnitsRepository.TENANT_ID, 0),
            eq(InvalidUnitsRepository.PROCESS_ID, "procId1")));
        assertThat(count).isEqualTo(1);
        Assertions.assertThat(mongoCollection.countDocuments()).isEqualTo(5);
    }

    @Test
    public void deleteUnitsAndProgeny_OK() throws Exception {
        // Given
        List<String> units1 =
            Arrays.asList("unit1", "unit2", "unit4");
        List<String> units2 =
            Arrays.asList("unit3", "unit4");

        // When
        repository.bulkAppendUnits(units1, "procId1");
        repository.bulkAppendUnits(units2, "procId2");
        Assertions.assertThat(mongoCollection.countDocuments()).isEqualTo(5);

        repository.deleteUnitsAndProgeny("procId1");

        // Then
        Assertions.assertThat(mongoCollection.countDocuments()).isEqualTo(2);
    }

    @Test
    public void deleteUnitsAndProgeny_EmptyOK() throws Exception {
        // Given

        // When
        repository.deleteUnitsAndProgeny("procId1");

        // Then
        Assertions.assertThat(mongoCollection.countDocuments()).isEqualTo(0);
    }

    @Test
    public void findUnitsByProcessId() throws Exception {
        // Given
        List<String> units1 =
            Arrays.asList("unit1", "unit2", "unit4");
        List<String> units2 =
            Arrays.asList("unit3", "unit4");
        // When
        repository.bulkAppendUnits(units1, "procId1");
        repository.bulkAppendUnits(units2, "procId2");
        repository.findUnitsByProcessId("procId1").forEachRemaining(
            doc -> doc.get("unitId")
        );
        // Then
        long count = mongoCollection.countDocuments(and(
            eq(InvalidUnitsRepository.UNIT_ID, "unit4"),
            eq(InvalidUnitsRepository.TENANT_ID, 0),
            eq(InvalidUnitsRepository.PROCESS_ID, "procId1")));
        assertThat(count).isEqualTo(1);
        Assertions.assertThat(mongoCollection.countDocuments()).isEqualTo(5);
    }
}

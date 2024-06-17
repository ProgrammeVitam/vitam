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
import java.util.List;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static fr.gouv.vitam.batch.report.rest.repository.UnitComputedInheritedRulesInvalidationRepository.UNIT_COMPUTED_INHERITED_RULES_INVALIDATION_COLLECTION_NAME;
import static org.assertj.core.api.Assertions.assertThat;

@RunWithCustomExecutor
public class UnitComputedInheritedRulesInvalidationRepositoryTest {

    private static final String TEST_COLLECTION_NAME =
        UNIT_COMPUTED_INHERITED_RULES_INVALIDATION_COLLECTION_NAME + GUIDFactory.newGUID().getId();

    private static final String METADATA_UNIT_ID =
        UnitComputedInheritedRulesInvalidationModel.METADATA +
        "." +
        UnitComputedInheritedRulesInvalidationReportEntry.UNIT_ID;
    private static final String TENANT_ID = UnitComputedInheritedRulesInvalidationModel.TENANT;
    private static final String PROCESS_ID = UnitComputedInheritedRulesInvalidationModel.PROCESS_ID;

    @ClassRule
    public static RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Rule
    public MongoRule mongoRule = new MongoRule(MongoDbAccess.getMongoClientSettingsBuilder(), TEST_COLLECTION_NAME);

    private UnitComputedInheritedRulesInvalidationRepository repository;

    private MongoCollection<Document> mongoCollection;

    @Before
    public void setUp() {
        MongoDbAccess mongoDbAccess = new SimpleMongoDBAccess(mongoRule.getMongoClient(), MongoRule.VITAM_DB);
        repository = new UnitComputedInheritedRulesInvalidationRepository(mongoDbAccess, TEST_COLLECTION_NAME);
        mongoCollection = mongoRule.getMongoCollection(TEST_COLLECTION_NAME);
        VitamThreadUtils.getVitamSession().setTenantId(0);
    }

    @Test
    public void bulkAppendUnits_singleCallOK() throws Exception {
        // Given
        List<String> units = Arrays.asList("unit1", "unit2", "unit4");
        // When
        repository.bulkAppendReport(buildReport(units, "procId1"));

        // Then
        long count = mongoCollection.countDocuments(
            and(eq(METADATA_UNIT_ID, "unit1"), eq(TENANT_ID, 0), eq(PROCESS_ID, "procId1"))
        );
        assertThat(count).isEqualTo(1);
        assertThat(mongoCollection.countDocuments()).isEqualTo(3);
    }

    @Test
    public void bulkAppendUnits_noDuplicates() throws Exception {
        // Given
        List<String> units1 = Arrays.asList("unit1", "unit2", "unit4");
        List<String> units2 = Arrays.asList("unit3", "unit4");
        List<String> units3 = Collections.singletonList("unit5");
        // When
        repository.bulkAppendReport(buildReport(units1, "procId1"));
        repository.bulkAppendReport(buildReport(units2, "procId1"));
        repository.bulkAppendReport(buildReport(units3, "procId1"));
        // Then
        long count = mongoCollection.countDocuments(
            and(eq(METADATA_UNIT_ID, "unit1"), eq(TENANT_ID, 0), eq(PROCESS_ID, "procId1"))
        );
        assertThat(count).isEqualTo(1);
        assertThat(mongoCollection.countDocuments()).isEqualTo(5);
    }

    @Test
    public void bulkAppendUnits_multiProcess() throws Exception {
        // Given
        List<String> units1 = Arrays.asList("unit1", "unit2", "unit4");
        List<String> units2 = Arrays.asList("unit3", "unit4");
        // When
        repository.bulkAppendReport(buildReport(units1, "procId1"));
        repository.bulkAppendReport(buildReport(units2, "procId2"));
        // Then
        long count = mongoCollection.countDocuments(
            and(eq(METADATA_UNIT_ID, "unit4"), eq(TENANT_ID, 0), eq(PROCESS_ID, "procId1"))
        );
        assertThat(count).isEqualTo(1);
        assertThat(mongoCollection.countDocuments()).isEqualTo(5);
    }

    @Test
    public void deleteUnitsAndProgeny_OK() throws Exception {
        // Given
        List<String> units1 = Arrays.asList("unit1", "unit2", "unit4");
        List<String> units2 = Arrays.asList("unit3", "unit4");

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
        List<String> units1 = Arrays.asList("unit1", "unit2", "unit4");
        List<String> units2 = Arrays.asList("unit3", "unit4");
        // When
        repository.bulkAppendReport(buildReport(units1, "procId1"));
        repository.bulkAppendReport(buildReport(units2, "procId2"));
        CloseableIterator<Document> documents = repository.findCollectionByProcessIdTenant(
            "procId1",
            VitamThreadUtils.getVitamSession().getTenantId()
        );
        // Then
        List<String> unitIds = Streams.stream(documents).map(doc -> doc.getString("id")).collect(Collectors.toList());
        assertThat(unitIds).containsExactlyInAnyOrderElementsOf(units1);
    }

    private List<UnitComputedInheritedRulesInvalidationModel> buildReport(List<String> units, String processId) {
        return units
            .stream()
            .map(
                unitId ->
                    new UnitComputedInheritedRulesInvalidationModel(
                        processId,
                        VitamThreadUtils.getVitamSession().getTenantId(),
                        LocalDateUtil.nowFormatted(),
                        new UnitComputedInheritedRulesInvalidationReportEntry(unitId)
                    )
            )
            .collect(Collectors.toList());
    }
}

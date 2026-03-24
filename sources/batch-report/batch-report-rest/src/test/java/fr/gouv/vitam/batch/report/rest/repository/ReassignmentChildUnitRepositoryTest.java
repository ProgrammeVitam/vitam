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

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import fr.gouv.vitam.batch.report.model.ReassignmentChildUnitModel;
import fr.gouv.vitam.batch.report.model.TransferReplyUnitModel;
import fr.gouv.vitam.batch.report.model.entry.ReassignmentChildUnitReportEntry;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.database.server.mongodb.SimpleMongoDBAccess;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.mongo.MongoRule;
import org.assertj.core.api.Assertions;
import org.bson.Document;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static org.assertj.core.api.Assertions.assertThat;

public class ReassignmentChildUnitRepositoryTest {

    private static final String TEST_COLLECTION_NAME =
        ReassignmentChildUnitRepository.COLLECTION_NAME + GUIDFactory.newGUID().getId();
    private static final int TENANT_ID = 0;
    private static final String PROCESS_ID = "123456789";

    @Rule
    public MongoRule mongoRule = new MongoRule(MongoDbAccess.getMongoClientSettingsBuilder(), TEST_COLLECTION_NAME);

    private ReassignmentChildUnitRepository repository;

    private MongoCollection<Document> reassignmentChildUnitCollection;

    @Before
    public void setUp() {
        MongoDbAccess mongoDbAccess = new SimpleMongoDBAccess(mongoRule.getMongoClient(), MongoRule.VITAM_DB);
        repository = new ReassignmentChildUnitRepository(mongoDbAccess, TEST_COLLECTION_NAME);
        reassignmentChildUnitCollection = mongoRule.getMongoCollection(TEST_COLLECTION_NAME);
    }

    @After
    public void tearDown() {
        reassignmentChildUnitCollection.drop();
    }

    @Test
    public void should_bulk_append_unit_report_and_check_metadata_id_unicity() throws Exception {
        // Given
        List<ReassignmentChildUnitModel> models = generateData(List.of("unitId1", "unitId4", "unitId4"));

        // When
        repository.bulkAppendReport(models);

        // Then
        Document first = reassignmentChildUnitCollection
            .find(
                and(eq(TransferReplyUnitModel.METADATA + "." + "id", "unitId1"), eq(TransferReplyUnitModel.TENANT, 0))
            )
            .first();
        assertThat(first)
            .isNotNull()
            .containsEntry("processId", "123456789")
            .containsEntry("_tenant", 0)
            .containsKeys("_metadata");
        Object metadata = first.get("_metadata");
        JsonNode metadataNode = JsonHandler.toJsonNode(metadata);
        JsonNode expected = JsonHandler.getFromString("{\"id\":\"unitId1\"}");
        assertThat(metadataNode).isNotNull().isEqualTo(expected);
        assertThat(reassignmentChildUnitCollection.countDocuments()).isEqualTo(2);
    }

    @Test
    public void should_bulk_append_unit_report_and_check_no_duplicate() {
        // Given
        List<ReassignmentChildUnitModel> models1 = generateData(
            List.of(
                "unitId2",
                "unitId4",
                "unitId5",
                "unitId1",
                "unitId1",
                "unitId3",
                "unitId6",
                "unitId8",
                "unitId7",
                "unitId9"
            )
        );

        List<ReassignmentChildUnitModel> models2 = generateData(
            List.of(
                "unitId2",
                "unitId4",
                "unitId5",
                "unitId1",
                "unitId1",
                "unitId3",
                "unitId6",
                "unitId8",
                "unitId7",
                "unitId9"
            )
        );

        // When
        repository.bulkAppendReport(models1);
        repository.bulkAppendReport(models2);

        // Then
        FindIterable<Document> iterable = reassignmentChildUnitCollection.find();
        MongoCursor<Document> iterator = iterable.iterator();
        List<Document> documents = new ArrayList<>();
        while (iterator.hasNext()) {
            documents.add(iterator.next());
        }
        Assertions.assertThat(documents.size()).isEqualTo(9);
    }

    @Test
    public void should_find_originating_agencies_reassignment_by_process_tenant() {
        // Given
        List<ReassignmentChildUnitModel> models = generateData(List.of("unitId1", "unitId4", "unitId4"));
        repository.bulkAppendReport(models);

        // When
        List<Document> documents;
        try (MongoCursor<Document> iterator = repository.findCollectionByProcessIdTenant(PROCESS_ID, TENANT_ID)) {
            // Then
            documents = new ArrayList<>();
            while (iterator.hasNext()) {
                Document next = iterator.next();
                documents.add(next);
            }
        }
        assertThat(documents.size()).isEqualTo(2);
    }

    @Test
    public void should_delete_originating_agencies_reassignment_unit_by_processId_and_tenant() {
        // Given
        List<ReassignmentChildUnitModel> models = generateData(List.of("unit1", "unit4", "unit4"));
        repository.bulkAppendReport(models);

        // When
        repository.deleteReportByIdAndTenant(PROCESS_ID, TENANT_ID);

        // Then
        FindIterable<Document> iterable = reassignmentChildUnitCollection.find();
        MongoCursor<Document> iterator = iterable.iterator();
        List<Document> documents = new ArrayList<>();
        while (iterator.hasNext()) {
            documents.add(iterator.next());
        }
        assertThat(documents).isEmpty();
    }

    private List<ReassignmentChildUnitModel> generateData(List<String> unitIds) {
        List<ReassignmentChildUnitModel> models = new ArrayList<>();

        for (String unitId : unitIds) {
            ReassignmentChildUnitModel reassignmentUpdateModel = new ReassignmentChildUnitModel();
            reassignmentUpdateModel.setProcessId(PROCESS_ID);
            reassignmentUpdateModel.setTenant(TENANT_ID);
            reassignmentUpdateModel.setCreationDateTime(LocalDateUtil.nowFormatted());
            reassignmentUpdateModel.setMetadata(new ReassignmentChildUnitReportEntry(unitId));
            models.add(reassignmentUpdateModel);
        }
        return models;
    }
}

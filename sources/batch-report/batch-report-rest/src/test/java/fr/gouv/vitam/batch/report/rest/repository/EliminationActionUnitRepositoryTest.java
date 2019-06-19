package fr.gouv.vitam.batch.report.rest.repository; /*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 *******************************************************************************/
import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import fr.gouv.vitam.batch.report.model.EliminationActionUnitModel;
import fr.gouv.vitam.batch.report.model.ReportBody;
import fr.gouv.vitam.batch.report.model.entry.EliminationActionUnitReportEntry;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.database.server.mongodb.SimpleMongoDBAccess;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.mongo.MongoRule;
import org.assertj.core.api.Assertions;
import org.bson.Document;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static fr.gouv.vitam.batch.report.model.EliminationActionAccessionRegisterModel.OPI;
import static fr.gouv.vitam.batch.report.model.EliminationActionAccessionRegisterModel.ORIGINATING_AGENCY;
import static fr.gouv.vitam.batch.report.model.EliminationActionAccessionRegisterModel.TOTAL_UNITS;
import static fr.gouv.vitam.common.database.collections.VitamCollection.getMongoClientOptions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

public class EliminationActionUnitRepositoryTest {

    private final static String ELIMINATION_ACTION_UNIT = "EliminationActionUnit" + GUIDFactory.newGUID().getId();
    private static final int TENANT_ID = 0;
    private static final String PROCESS_ID = "123456789";

    @Rule
    public MongoRule mongoRule =
        new MongoRule(getMongoClientOptions(), ELIMINATION_ACTION_UNIT);

    private EliminationActionUnitRepository repository;

    private MongoCollection<Document> eliminationUnitCollection;

    @Before
    public void setUp() throws Exception {
        MongoDbAccess mongoDbAccess = new SimpleMongoDBAccess(mongoRule.getMongoClient(), MongoRule.VITAM_DB);
        repository = new EliminationActionUnitRepository(mongoDbAccess, ELIMINATION_ACTION_UNIT);
        eliminationUnitCollection = mongoRule.getMongoCollection(ELIMINATION_ACTION_UNIT);
    }

    @Test
    public void should_bulk_append_unit_report_and_check_metadata_id_unicity()
        throws InvalidParseOperationException {
        // Given
        List<EliminationActionUnitModel> eliminationActionUnitModels = getDocuments("/eliminationUnitModel.json");
        // When
        repository.bulkAppendReport(eliminationActionUnitModels);
        // Then
        Document first = eliminationUnitCollection.find(and(eq(EliminationActionUnitModel.METADATA + "." +
            "id", "unitId1"), eq(EliminationActionUnitModel.TENANT, 0))).first();
        assertThat(first).isNotNull()
            .containsEntry("processId", "123456789")
            .containsEntry("_tenant", 0)
            .containsKeys("_metadata");
        Object metadata = first.get("_metadata");
        JsonNode metadataNode = JsonHandler.toJsonNode(metadata);
        JsonNode expected = JsonHandler.getFromString(
            "{\"outcome\":\"Outcome - TEST\",\"detailType\":\"Unit\",\"id\":\"unitId1\",\"unitId\":\"unitId1\",\"originatingAgency\":\"sp1\",\"opi\":\"opi0\",\"objectGroupId\":\"id2\",\"status\":\"DELETED\"}");
        assertThat(metadataNode).isNotNull().isEqualTo(expected);
        repository.bulkAppendReport(eliminationActionUnitModels);
        assertThat(eliminationUnitCollection.countDocuments()).isEqualTo(4);
    }

    @Test
    public void should_bulk_append_unit_report_and_check_no_duplicate()
        throws InvalidParseOperationException {
        // Given
        List<EliminationActionUnitModel> eliminationActionUnitModels =
            getDocuments("/eliminationUnitWithDuplicateUnit.json");
        // When
        repository.bulkAppendReport(eliminationActionUnitModels);
        repository.bulkAppendReport(eliminationActionUnitModels);
        // Then
        FindIterable<Document> iterable = eliminationUnitCollection.find();
        MongoCursor<Document> iterator = iterable.iterator();
        List<Document> documents = new ArrayList<>();
        while (iterator.hasNext()) {
            documents.add(iterator.next());
        }
        Assertions.assertThat(documents.size()).isEqualTo(9);
    }

    @Test
    public void compute_own_accession_register_ok()
        throws InvalidParseOperationException {
        // Given
        List<EliminationActionUnitModel> eliminationActionUnitModels =
            getDocuments("/eliminationUnitWithDuplicateUnit.json");
        // When
        repository.bulkAppendReport(eliminationActionUnitModels);

        EliminationActionUnitModel first = eliminationActionUnitModels.iterator().next();
        // When
        MongoCursor<Document> iterator = repository.computeOwnAccessionRegisterDetails(first.getProcessId(), TENANT_ID);
        List<Document> documents = new ArrayList<>();
        while (iterator.hasNext()) {
            documents.add(iterator.next());
        }

        // Then
        Assertions.assertThat(documents.size()).isEqualTo(4);

        Assertions.assertThat(documents)
            .extracting(OPI, ORIGINATING_AGENCY, TOTAL_UNITS)
            .containsSequence(
                tuple("opi9", "sp1", 1),
                tuple("opi3", "sp2", 2),
                tuple("opi1", "sp1", 1),
                tuple("opi0", "sp1", 3)
            );
    }

    private List<EliminationActionUnitModel> getDocuments(String filename) throws InvalidParseOperationException {
        InputStream stream = getClass().getResourceAsStream(filename);
        ReportBody<EliminationActionUnitReportEntry> reportBody = JsonHandler.getFromInputStream(stream, ReportBody.class, EliminationActionUnitReportEntry.class);
        return reportBody.getEntries().stream()
            .map(md -> {
                EliminationActionUnitModel eliminationActionUnitModel = new EliminationActionUnitModel();
                eliminationActionUnitModel.setProcessId(reportBody.getProcessId());
                eliminationActionUnitModel.setId(GUIDFactory.newGUID().toString());
                eliminationActionUnitModel.setTenant(0);
                LocalDateTime localDateTime = LocalDateTime.now();
                eliminationActionUnitModel.setCreationDateTime(localDateTime.toString());
                eliminationActionUnitModel.setMetadata(md);
                return eliminationActionUnitModel;
            }).collect(Collectors.toList());
    }

    @Test
    public void should_find_elimination_unit_by_process_tenant_status() throws Exception {
        // Given
        List<EliminationActionUnitModel> eliminationActionUnitModels = getDocuments("/eliminationUnitModel.json");
        repository.bulkAppendReport(eliminationActionUnitModels);
        // When
        MongoCursor<Document> iterator =
            repository.findCollectionByProcessIdTenant(PROCESS_ID, TENANT_ID);
        // Then
        List<Document> documents = new ArrayList<>();
        while (iterator.hasNext()) {
            Document next = iterator.next();
            documents.add(next);
        }
        assertThat(documents.size()).isEqualTo(4);
    }

    @Test
    public void should_find_distinct_objectGroup_for_the_given_unit_deleted() throws Exception {
        // Given
        List<EliminationActionUnitModel> eliminationObjectGroupModels = getDocuments("/eliminationUnitModel.json");
        repository.bulkAppendReport(eliminationObjectGroupModels);
        // When
        MongoCursor<String> mongoCursor = repository.distinctObjectGroupOfDeletedUnits(PROCESS_ID, TENANT_ID);
        // Then
        List<String> documentIds = new ArrayList<>();
        while (mongoCursor.hasNext()) {
            documentIds.add(mongoCursor.next());
        }
        assertThat(documentIds).isNotEmpty();
        assertThat(documentIds.size()).isEqualTo(3);
        assertThat(documentIds).contains("id2", "id3", "id1");
    }

    @Test
    public void should_delete_elimination_by_processId_and_tenant() throws Exception {
        // Given
        List<EliminationActionUnitModel> eliminationObjectGroupModels = getDocuments("/eliminationUnitModel.json");
        repository.bulkAppendReport(eliminationObjectGroupModels);
        // When
        repository.deleteReportByIdAndTenant(PROCESS_ID, TENANT_ID);
        // Then
        FindIterable<Document> iterable = eliminationUnitCollection.find();
        MongoCursor<Document> iterator = iterable.iterator();
        List<Document> documents = new ArrayList<>();
        while (iterator.hasNext()) {
            documents.add(iterator.next());
        }
        assertThat(documents).isEmpty();
        assertThat(documents.size()).isEqualTo(0);
    }

}

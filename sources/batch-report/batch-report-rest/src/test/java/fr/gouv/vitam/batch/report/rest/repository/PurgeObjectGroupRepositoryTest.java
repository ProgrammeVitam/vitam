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
package fr.gouv.vitam.batch.report.rest.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import fr.gouv.vitam.batch.report.model.PurgeObjectGroupModel;
import fr.gouv.vitam.batch.report.model.ReportBody;
import fr.gouv.vitam.batch.report.model.entry.PurgeObjectGroupReportEntry;
import fr.gouv.vitam.common.LocalDateUtil;
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
import static fr.gouv.vitam.batch.report.model.PurgeAccessionRegisterModel.OPI;
import static fr.gouv.vitam.batch.report.model.PurgeAccessionRegisterModel.ORIGINATING_AGENCY;
import static fr.gouv.vitam.batch.report.model.PurgeAccessionRegisterModel.TOTAL_OBJECTS;
import static fr.gouv.vitam.batch.report.model.PurgeAccessionRegisterModel.TOTAL_OBJECT_GROUPS;
import static fr.gouv.vitam.batch.report.model.PurgeAccessionRegisterModel.TOTAL_SIZE;
import static fr.gouv.vitam.common.database.collections.VitamCollection.getMongoClientOptions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

public class PurgeObjectGroupRepositoryTest {
    private static final int TENANT_ID = 0;
    private static final String PROCESS_ID = "123456789";

    private static final String Purge_OBJECT_GROUP = "PurgeObjectGroup" + GUIDFactory.newGUID().getId();
    private static final TypeReference<ReportBody<PurgeObjectGroupReportEntry>>
        TYPE_REFERENCE = new TypeReference<ReportBody<PurgeObjectGroupReportEntry>>() {
    };

    @Rule
    public MongoRule mongoRule =
        new MongoRule(getMongoClientOptions(), Purge_OBJECT_GROUP);

    private PurgeObjectGroupRepository repository;

    private MongoCollection<Document> purgeObjectGroupCollection;

    @Before
    public void setUp() {
        MongoDbAccess mongoDbAccess = new SimpleMongoDBAccess(mongoRule.getMongoClient(), MongoRule.VITAM_DB);
        repository = new PurgeObjectGroupRepository(mongoDbAccess, Purge_OBJECT_GROUP);
        purgeObjectGroupCollection = mongoRule.getMongoCollection(Purge_OBJECT_GROUP);
    }


    @Test
    public void should_bulk_append_objectgroup_report()
        throws Exception {
        // Given
        List<PurgeObjectGroupModel> purgeObjectGroupModels = getDocuments("/purgeObjectGroupModel.json");
        // When
        repository.bulkAppendReport(purgeObjectGroupModels);
        // Then
        Document first = purgeObjectGroupCollection.find(and(eq(PurgeObjectGroupModel.METADATA + "." +
            "id", "id2"), eq(PurgeObjectGroupModel.TENANT, 0))).first();
        assertThat(first).isNotNull()
            .containsEntry("processId", "123456789")
            .containsEntry("_tenant", 0)
            .containsKeys("_metadata");
        Object metadata = first.get("_metadata");
        JsonNode metadataNode = JsonHandler.toJsonNode(metadata);
        JsonNode expected = JsonHandler.getFromString(
            "{\"id\":\"id2\",\"opi\":\"opi0\",\"originatingAgency\":\"sp1\",\"status\":\"DELETED\",\"objectIds\":[\"parent\",\"parent2\"],\"objectVersions\":[{\"opi\":\"opi0\",\"size\":3},{\"opi\":\"opi0add\",\"size\":6}]}");
        assertThat(metadataNode).isNotNull().isEqualTo(expected);
    }

    @Test
    public void should_bulk_append_objectgroup_report_and_check_no_duplicate()
        throws Exception {
        // Given
        List<PurgeObjectGroupModel> purgeUnitModels =
            getDocuments("/purgeObjectGroupWithDuplicateObjectGroup.json");
        // When
        repository.bulkAppendReport(purgeUnitModels);
        repository.bulkAppendReport(purgeUnitModels);
        // Then
        FindIterable<Document> iterable = purgeObjectGroupCollection.find();
        MongoCursor<Document> iterator = iterable.iterator();
        List<Document> documents = new ArrayList<>();
        while (iterator.hasNext()) {
            documents.add(iterator.next());
        }
        Assertions.assertThat(documents.size()).isEqualTo(6);
    }

    @Test
    public void compute_own_accession_register_multiple_objects_from_different_operation_ok()
        throws Exception {
        // Given
        List<PurgeObjectGroupModel> purgeObjectGroupModels =
            getDocuments("/purgeObjectGroupMultipleObjectsDifferentOperations.json");
        // When
        repository.bulkAppendReport(purgeObjectGroupModels);

        PurgeObjectGroupModel first = purgeObjectGroupModels.iterator().next();
        // When
        MongoCursor<Document> iterator = repository.computeOwnAccessionRegisterDetails(first.getProcessId(), TENANT_ID);
        List<Document> documents = new ArrayList<>();
        while (iterator.hasNext()) {
            documents.add(iterator.next());
        }

        // Then
        Assertions.assertThat(documents.size()).isEqualTo(4);

        Assertions.assertThat(documents)
            .extracting(ORIGINATING_AGENCY, TOTAL_SIZE, TOTAL_OBJECTS, OPI, TOTAL_OBJECT_GROUPS)
            .containsSequence(
                tuple("sp1", 6, 1, "opi3", 0),
                tuple("sp1", 20, 4, "opi2", 1),
                tuple("sp1", 13, 3, "opi1", 1),
                tuple("sp1", 7, 2, "opi0", 2)
            );
    }

    @Test
    public void compute_own_accession_register_ok()
        throws Exception {
        // Given
        List<PurgeObjectGroupModel> purgeObjectGroupModels = getDocuments(
            "/purgeObjectGroupWithDuplicateObjectGroup.json");
        // When
        repository.bulkAppendReport(purgeObjectGroupModels);

        PurgeObjectGroupModel first = purgeObjectGroupModels.iterator().next();
        // When
        MongoCursor<Document> iterator = repository.computeOwnAccessionRegisterDetails(first.getProcessId(), TENANT_ID);
        List<Document> documents = new ArrayList<>();
        while (iterator.hasNext()) {
            documents.add(iterator.next());
        }

        // Then
        Assertions.assertThat(documents.size()).isEqualTo(8);

        Assertions.assertThat(documents)
            .extracting(ORIGINATING_AGENCY, TOTAL_SIZE, TOTAL_OBJECTS, OPI, TOTAL_OBJECT_GROUPS)
            .containsSequence(
                tuple("sp1", 3, 1, "opi9", 1),
                tuple("sp1", 6, 1, "opi8", 0),
                tuple("sp2", 2, 1, "opi3add", 0),
                tuple("sp2", 1, 1, "opi3", 1),
                tuple("sp1", 4, 1, "opi2add", 0),
                tuple("sp1", 3, 1, "opi2", 1),
                tuple("sp1", 10, 2, "opi0add", 0),
                tuple("sp1", 4, 2, "opi0", 2)
            );
    }

    private List<PurgeObjectGroupModel> getDocuments(String filename)
        throws InvalidParseOperationException, InvalidFormatException {
        InputStream stream = getClass().getResourceAsStream(filename);
        ReportBody<PurgeObjectGroupReportEntry> reportBody =
            JsonHandler.getFromInputStreamAsTypeReference(stream, TYPE_REFERENCE);
        return reportBody.getEntries().stream()
            .map(md -> {
                PurgeObjectGroupModel
                    PurgeObjectGroupModel = new PurgeObjectGroupModel();
                PurgeObjectGroupModel.setProcessId(reportBody.getProcessId());
                PurgeObjectGroupModel.setTenant(0);
                LocalDateTime localDateTime = LocalDateUtil.now();
                PurgeObjectGroupModel.setCreationDateTime(localDateTime.toString());
                PurgeObjectGroupModel.setMetadata(md);
                return PurgeObjectGroupModel;
            }).collect(Collectors.toList());
    }

    @Test
    public void should_find_purge_objectgroup_by_processid_and_tenant() throws Exception {
        // Given
        List<PurgeObjectGroupModel> purgeObjectGroupModels = getDocuments("/purgeObjectGroupModel.json");
        repository.bulkAppendReport(purgeObjectGroupModels);
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
    public void should_delete_elimination_by_processId_and_tenant() throws Exception {
        // Given
        List<PurgeObjectGroupModel> purgeObjectGroupModels = getDocuments("/purgeObjectGroupModel.json");
        repository.bulkAppendReport(purgeObjectGroupModels);
        // When
        repository.deleteReportByIdAndTenant(PROCESS_ID, TENANT_ID);
        // Then
        FindIterable<Document> iterable = purgeObjectGroupCollection.find();
        MongoCursor<Document> iterator = iterable.iterator();
        List<Document> documents = new ArrayList<>();
        while (iterator.hasNext()) {
            documents.add(iterator.next());
        }
        assertThat(documents).isEmpty();
        assertThat(documents.size()).isEqualTo(0);
    }
}

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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import fr.gouv.vitam.batch.report.model.ReportBody;
import fr.gouv.vitam.batch.report.model.TransferReplyUnitModel;
import fr.gouv.vitam.batch.report.model.entry.TransferReplyUnitReportEntry;
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
import static org.assertj.core.api.Assertions.assertThat;

public class TransferReplyUnitRepositoryTest {

    private static final String TRANSFER_REPLY_UNIT = "TransferReplyUnit" + GUIDFactory.newGUID().getId();
    private static final int TENANT_ID = 0;
    private static final String PROCESS_ID = "123456789";
    private static final TypeReference<ReportBody<TransferReplyUnitReportEntry>> TYPE_REFERENCE = new TypeReference<
        ReportBody<TransferReplyUnitReportEntry>
    >() {};

    @Rule
    public MongoRule mongoRule = new MongoRule(MongoDbAccess.getMongoClientSettingsBuilder(), TRANSFER_REPLY_UNIT);

    private TransferReplyUnitRepository repository;

    private MongoCollection<Document> eliminationUnitCollection;

    @Before
    public void setUp() {
        MongoDbAccess mongoDbAccess = new SimpleMongoDBAccess(mongoRule.getMongoClient(), MongoRule.VITAM_DB);
        repository = new TransferReplyUnitRepository(mongoDbAccess, TRANSFER_REPLY_UNIT);
        eliminationUnitCollection = mongoRule.getMongoCollection(TRANSFER_REPLY_UNIT);
    }

    @Test
    public void should_bulk_append_unit_report_and_check_metadata_id_unicity() throws Exception {
        // Given
        List<TransferReplyUnitModel> transferReplyUnitModels = getDocuments("/transferReplyUnitModel.json");
        // When
        repository.bulkAppendReport(transferReplyUnitModels);
        // Then
        Document first = eliminationUnitCollection
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
        JsonNode expected = JsonHandler.getFromString("{\"id\":\"unitId1\",\"status\":\"ALREADY_DELETED\"}");
        assertThat(metadataNode).isNotNull().isEqualTo(expected);
        repository.bulkAppendReport(transferReplyUnitModels);
        assertThat(eliminationUnitCollection.countDocuments()).isEqualTo(2);
    }

    @Test
    public void should_bulk_append_unit_report_and_check_no_duplicate() throws Exception {
        // Given
        List<TransferReplyUnitModel> transferReplyUnitModels1 = getDocuments("/eliminationUnitWithDuplicateUnit.json");
        List<TransferReplyUnitModel> transferReplyUnitModels2 = getDocuments("/eliminationUnitWithDuplicateUnit.json");
        // When
        repository.bulkAppendReport(transferReplyUnitModels1);
        repository.bulkAppendReport(transferReplyUnitModels2);
        // Then
        FindIterable<Document> iterable = eliminationUnitCollection.find();
        MongoCursor<Document> iterator = iterable.iterator();
        List<Document> documents = new ArrayList<>();
        while (iterator.hasNext()) {
            documents.add(iterator.next());
        }
        Assertions.assertThat(documents.size()).isEqualTo(9);
    }

    private List<TransferReplyUnitModel> getDocuments(String filename)
        throws InvalidParseOperationException, InvalidFormatException {
        InputStream stream = getClass().getResourceAsStream(filename);
        ReportBody<TransferReplyUnitReportEntry> reportBody = JsonHandler.getFromInputStreamAsTypeReference(
            stream,
            TYPE_REFERENCE
        );

        return reportBody
            .getEntries()
            .stream()
            .map(md -> {
                TransferReplyUnitModel transferReplyUnitModel = new TransferReplyUnitModel();
                transferReplyUnitModel.setProcessId(reportBody.getProcessId());
                transferReplyUnitModel.setTenant(0);
                LocalDateTime localDateTime = LocalDateUtil.now();
                transferReplyUnitModel.setCreationDateTime(localDateTime.toString());
                transferReplyUnitModel.setMetadata(md);
                return transferReplyUnitModel;
            })
            .collect(Collectors.toList());
    }

    @Test
    public void should_find_elimination_unit_by_process_tenant_status() throws Exception {
        // Given
        List<TransferReplyUnitModel> transferReplyUnitModels = getDocuments("/transferReplyUnitModel.json");
        repository.bulkAppendReport(transferReplyUnitModels);
        // When
        MongoCursor<Document> iterator = repository.findCollectionByProcessIdTenant(PROCESS_ID, TENANT_ID);
        // Then
        List<Document> documents = new ArrayList<>();
        while (iterator.hasNext()) {
            Document next = iterator.next();
            documents.add(next);
        }
        assertThat(documents.size()).isEqualTo(2);
    }

    @Test
    public void should_delete_elimination_by_processId_and_tenant() throws Exception {
        // Given
        List<TransferReplyUnitModel> transferReplyUnitModels = getDocuments("/transferReplyUnitModel.json");
        repository.bulkAppendReport(transferReplyUnitModels);
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

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
import com.mongodb.client.MongoCollection;
import fr.gouv.vitam.batch.report.model.entry.DeleteGotVersionsReportEntry;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.database.server.mongodb.SimpleMongoDBAccess;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.mongo.MongoRule;
import org.bson.Document;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.util.List;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static fr.gouv.vitam.batch.report.model.ReportType.DELETE_GOT_VERSIONS;
import static fr.gouv.vitam.batch.report.model.entry.DeleteGotVersionsReportEntry.OBJECT_GROUP_GLOBAL;
import static fr.gouv.vitam.batch.report.model.entry.DeleteGotVersionsReportEntry.PROCESS_ID;
import static fr.gouv.vitam.batch.report.model.entry.DeleteGotVersionsReportEntry.TENANT;
import static fr.gouv.vitam.batch.report.model.entry.ReportEntry.DETAIL_TYPE;
import static org.assertj.core.api.Assertions.assertThat;

public class DeleteGotVersionsReportRepositoryTest {

    private static final String DELETE_GOT_VERSIONS_REPORT =
        "DELETE_GOT_VERSIONS_REPORT" + GUIDFactory.newGUID().getId();

    public static final String DELETE_GOT_VERSIONS_REPORT_JSON = "deleteGotVersionsReport.json";
    public static final String PROCESS_ID_VALUE = "aeeaaaaaacgqyd36abmc4aly7crq7syaaaaq";

    @Rule
    public MongoRule mongoRule = new MongoRule(
        MongoDbAccess.getMongoClientSettingsBuilder(),
        DELETE_GOT_VERSIONS_REPORT
    );

    private DeleteGotVersionsReportRepository repository;

    private MongoCollection<Document> deleteGotVersionsReportCollection;

    @Before
    public void setUp() {
        MongoDbAccess mongoDbAccess = new SimpleMongoDBAccess(mongoRule.getMongoClient(), MongoRule.VITAM_DB);
        repository = new DeleteGotVersionsReportRepository(mongoDbAccess, DELETE_GOT_VERSIONS_REPORT);
        deleteGotVersionsReportCollection = mongoRule.getMongoCollection(DELETE_GOT_VERSIONS_REPORT);
    }

    @Test
    public void should_bulk_append_reports() throws Exception {
        // Given
        List<DeleteGotVersionsReportEntry> deleteGotVersionsReportEntries = getDocuments(
            DELETE_GOT_VERSIONS_REPORT_JSON
        );

        // When
        repository.bulkAppendReport(deleteGotVersionsReportEntries);

        // Then
        assertThat(deleteGotVersionsReportCollection.countDocuments()).isEqualTo(2);
        Document firstDocument = deleteGotVersionsReportCollection
            .find(and(eq(PROCESS_ID, PROCESS_ID_VALUE), eq(TENANT, 0)))
            .first();
        assertThat(firstDocument)
            .isNotNull()
            .containsEntry("processId", PROCESS_ID_VALUE)
            .containsEntry("_tenant", 0)
            .containsEntry(DETAIL_TYPE, DELETE_GOT_VERSIONS.toString())
            .containsKeys(OBJECT_GROUP_GLOBAL);

        Object objectGroupGlobal = firstDocument.get(OBJECT_GROUP_GLOBAL);
        JsonNode objectGroupGlobalNode = JsonHandler.toJsonNode(objectGroupGlobal);
        JsonNode expected = JsonHandler.getFromString(
            "[{\"status\":\"WARNING\",\"outCome\":\"Qualifier with this specific version 53 is inexistant!\"}]"
        );
        assertThat(objectGroupGlobalNode).isNotNull().isEqualTo(expected);
    }

    private List<DeleteGotVersionsReportEntry> getDocuments(String filename)
        throws InvalidParseOperationException, FileNotFoundException {
        return JsonHandler.getFromFileAsTypeReference(
            PropertiesUtils.getResourceFile(filename),
            new TypeReference<>() {}
        );
    }
}

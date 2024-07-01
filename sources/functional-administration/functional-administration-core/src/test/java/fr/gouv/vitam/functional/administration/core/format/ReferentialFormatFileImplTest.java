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
package fr.gouv.vitam.functional.administration.core.format;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.FileFormat;
import fr.gouv.vitam.functional.administration.common.config.ElasticsearchFunctionalAdminIndexManager;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.server.ElasticsearchAccessFunctionalAdmin;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollectionsTestUtils;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminFactory;
import fr.gouv.vitam.functional.administration.core.backup.FunctionalBackupService;
import fr.gouv.vitam.functional.administration.core.format.model.FormatImportReport;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import org.apache.commons.io.IOUtils;
import org.bson.Document;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static fr.gouv.vitam.common.guid.GUIDFactory.newOperationLogbookGUID;
import static fr.gouv.vitam.functional.administration.core.format.ReferentialFormatFileImpl.FILE_FORMAT_REPORT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;

public class ReferentialFormatFileImplTest {

    private static final Integer TENANT_ID = 0;
    private static final String PREFIX = GUIDFactory.newGUID().getId();
    private static final ElasticsearchFunctionalAdminIndexManager indexManager =
        FunctionalAdminCollectionsTestUtils.createTestIndexManager();

    @ClassRule
    public static MongoRule mongoRule = new MongoRule(MongoDbAccess.getMongoClientSettingsBuilder(FileFormat.class));

    static FunctionalBackupService functionalBackupService = Mockito.mock(FunctionalBackupService.class);
    static LogbookOperationsClient logbookOperationsClient = Mockito.mock(LogbookOperationsClient.class);
    static ReferentialFormatFileImpl formatFile;

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    String FILE_TO_TEST_KO = "FF-vitam-format-KO.xml";
    String FILE_TO_TEST_OK = "DROID_SignatureFile_V94.xml";
    String FILE_TO_TEST_OK_V1 = "FF-vitam-V1.xml";
    String FILE_TO_TEST_OK_V2 = "FF-vitam-V2.xml";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        final List<ElasticsearchNode> esNodes = new ArrayList<>();
        esNodes.add(new ElasticsearchNode(ElasticsearchRule.getHost(), ElasticsearchRule.getPort()));
        FunctionalAdminCollectionsTestUtils.beforeTestClass(
            mongoRule.getMongoDatabase(),
            PREFIX,
            new ElasticsearchAccessFunctionalAdmin(ElasticsearchRule.VITAM_CLUSTER, esNodes, indexManager)
        );

        final List<MongoDbNode> mongoDbNodes = new ArrayList<>();
        mongoDbNodes.add(new MongoDbNode("localhost", MongoRule.getDataBasePort()));

        LogbookOperationsClientFactory.changeMode(null);
        formatFile = new ReferentialFormatFileImpl(
            MongoDbAccessAdminFactory.create(
                new DbConfigurationImpl(mongoDbNodes, mongoRule.getMongoDatabase().getName()),
                Collections::emptyList,
                indexManager
            ),
            functionalBackupService,
            logbookOperationsClient
        );
    }

    @AfterClass
    public static void tearDownAfterClass() {
        FunctionalAdminCollectionsTestUtils.afterTestClass(true);
        VitamClientFactory.resetConnections();
    }

    @After
    public void cleanup() {
        FunctionalAdminCollectionsTestUtils.afterTest();
    }

    @Test
    @RunWithCustomExecutor
    public void testFormatXML() throws FileNotFoundException, ReferentialException {
        formatFile.checkFile(new FileInputStream(PropertiesUtils.findFile(FILE_TO_TEST_OK)));
    }

    @Test(expected = ReferentialException.class)
    @RunWithCustomExecutor
    public void testFormatXMLKO() throws FileNotFoundException, ReferentialException {
        formatFile.checkFile(new FileInputStream(PropertiesUtils.findFile(FILE_TO_TEST_KO)));
    }

    @Test
    @RunWithCustomExecutor
    public void testImportFormat() throws Exception {
        // Given / When
        FormatImportReport report = importFormatFileAndDownloadReport(FILE_TO_TEST_OK);

        // Then
        checkFormatsInDb(1670);
        final Select select = new Select();
        select.setQuery(QueryHelper.eq("PUID", "fmt/163"));
        final RequestResponseOK<FileFormat> fileList = formatFile.findDocuments(select.getFinalSelect());
        final String id = fileList.getResults().get(0).getString("PUID");
        final FileFormat file = formatFile.findDocumentById(id);
        assertEquals("[wps]", file.get("Extension").toString());
        assertEquals(file.get("#id"), fileList.getResults().get(0).getId());
        assertFalse(fileList.getResults().get(0).getBoolean("Alert"));
        assertEquals(fileList.getResults().get(0).getString("Group"), "");
        assertEquals(fileList.getResults().get(0).getString("Comment"), "");

        assertThat(report.getOperation().getEvType()).isEqualTo("STP_IMPORT_RULES");
        assertThat(report.getOperation().getEvDateTime()).isEqualTo("2018-11-28T15:41:10.752");
        assertThat(report.getOperation().getEvId()).isEqualTo(VitamThreadUtils.getVitamSession().getRequestId());
        assertThat(report.getPreviousPronomCreationDate()).isNull();
        assertThat(report.getPreviousPronomVersion()).isNull();
        assertThat(report.getNewPronomCreationDate()).isEqualTo("2018-09-17T12:54:53.000");
        assertThat(report.getNewPronomVersion()).isEqualTo("94");
        assertThat(report.getStatusCode()).isEqualTo(StatusCode.OK);
        assertThat(report.getWarnings()).isEmpty();
        assertThat(report.getRemovedPuids()).isEmpty();
        assertThat(report.getUpdatedPuids()).isEmpty();
        assertThat(report.getAddedPuids()).hasSize(1670);
    }

    @Test
    @RunWithCustomExecutor
    public void testImportFormatAndReImport_UpgradeVersion() throws Exception {
        // Given / When
        FormatImportReport report1 = importFormatFileAndDownloadReport(FILE_TO_TEST_OK_V1);
        final FileFormat fileFormatBefore = formatFile.findDocumentById("x-fmt/2");

        FormatImportReport report2 = importFormatFileAndDownloadReport(FILE_TO_TEST_OK_V2);

        // Then
        checkFormatsInDb(6);

        assertThat(report1.getPreviousPronomCreationDate()).isNull();
        assertThat(report1.getPreviousPronomVersion()).isNull();
        assertThat(report1.getNewPronomCreationDate()).isEqualTo("2018-01-01T01:01:01.000");
        assertThat(report1.getNewPronomVersion()).isEqualTo("1");
        assertThat(report1.getStatusCode()).isEqualTo(StatusCode.OK);
        assertThat(report1.getWarnings()).isEmpty();
        assertThat(report1.getRemovedPuids()).isEmpty();
        assertThat(report1.getUpdatedPuids()).isEmpty();
        assertThat(report1.getAddedPuids()).hasSize(3);

        assertThat(report2.getPreviousPronomCreationDate()).isEqualTo("2018-01-01T01:01:01.000");
        assertThat(report2.getPreviousPronomVersion()).isEqualTo("1");
        assertThat(report2.getNewPronomCreationDate()).isEqualTo("2018-02-02T02:02:02.000");
        assertThat(report2.getNewPronomVersion()).isEqualTo("2");
        assertThat(report2.getStatusCode()).isEqualTo(StatusCode.OK);
        assertThat(report2.getWarnings()).isEmpty();
        assertThat(report2.getRemovedPuids()).isEmpty();
        assertThat(report2.getUpdatedPuids()).containsOnlyKeys("x-fmt/1", "x-fmt/2");
        assertThat(report2.getAddedPuids()).containsExactlyInAnyOrder("x-fmt/4", "x-fmt/5", "x-fmt/6");

        assertThat(fileFormatBefore.getVersion()).isEqualTo(0);
        assertThat(fileFormatBefore.getString(FileFormat.NAME)).isEqualTo("Microsoft Word for Macintosh Document V1");
        assertThat(fileFormatBefore.getString(FileFormat.VERSION_PRONOM)).isEqualTo("1");

        final FileFormat fileFormatAfter = formatFile.findDocumentById("x-fmt/2");
        assertThat(fileFormatAfter.getVersion()).isEqualTo(1);
        assertThat(fileFormatAfter.getString(FileFormat.NAME)).isEqualTo("Microsoft Word for Macintosh Document V2");
        assertThat(fileFormatAfter.getString(FileFormat.VERSION_PRONOM)).isEqualTo("2");
    }

    @Test
    @RunWithCustomExecutor
    public void testImportFormatAndReImport_DowngradeVersion() throws Exception {
        // Given / When
        FormatImportReport report1 = importFormatFileAndDownloadReport(FILE_TO_TEST_OK_V2);
        final FileFormat fileFormatBefore = formatFile.findDocumentById("x-fmt/2");

        FormatImportReport report2 = importFormatFileAndDownloadReport(FILE_TO_TEST_OK_V1);

        // Then
        checkFormatsInDb(3);

        assertThat(report1.getPreviousPronomCreationDate()).isNull();
        assertThat(report1.getPreviousPronomVersion()).isNull();
        assertThat(report1.getNewPronomCreationDate()).isEqualTo("2018-02-02T02:02:02.000");
        assertThat(report1.getNewPronomVersion()).isEqualTo("2");
        assertThat(report1.getStatusCode()).isEqualTo(StatusCode.OK);
        assertThat(report1.getWarnings()).isEmpty();
        assertThat(report1.getRemovedPuids()).isEmpty();
        assertThat(report1.getUpdatedPuids()).isEmpty();
        assertThat(report1.getAddedPuids()).hasSize(6);

        assertThat(report2.getPreviousPronomCreationDate()).isEqualTo("2018-02-02T02:02:02.000");
        assertThat(report2.getPreviousPronomVersion()).isEqualTo("2");
        assertThat(report2.getNewPronomCreationDate()).isEqualTo("2018-01-01T01:01:01.000");
        assertThat(report2.getNewPronomVersion()).isEqualTo("1");
        assertThat(report2.getStatusCode()).isEqualTo(StatusCode.WARNING);
        /* Warnings : Pronom version + pronom date + removed puids */
        assertThat(report2.getWarnings()).hasSize(3);
        assertThat(report2.getRemovedPuids()).containsExactlyInAnyOrder("x-fmt/4", "x-fmt/5", "x-fmt/6");
        assertThat(report2.getUpdatedPuids()).containsOnlyKeys("x-fmt/1", "x-fmt/2");
        assertThat(report2.getAddedPuids()).isEmpty();

        assertThat(fileFormatBefore.getVersion()).isEqualTo(0);
        assertThat(fileFormatBefore.getString(FileFormat.NAME)).isEqualTo("Microsoft Word for Macintosh Document V2");
        assertThat(fileFormatBefore.getString(FileFormat.VERSION_PRONOM)).isEqualTo("2");

        final FileFormat fileFormatAfter = formatFile.findDocumentById("x-fmt/2");
        assertThat(fileFormatAfter.getVersion()).isEqualTo(1);
        assertThat(fileFormatAfter.getString(FileFormat.NAME)).isEqualTo("Microsoft Word for Macintosh Document V1");
        assertThat(fileFormatAfter.getString(FileFormat.VERSION_PRONOM)).isEqualTo("1");
    }

    @Test
    @RunWithCustomExecutor
    public void testImportFormatAndReImport_SameVersion() throws Exception {
        // Given / When
        FormatImportReport report1 = importFormatFileAndDownloadReport(FILE_TO_TEST_OK_V1);
        final FileFormat fileFormatBefore = formatFile.findDocumentById("x-fmt/2");

        FormatImportReport report2 = importFormatFileAndDownloadReport(FILE_TO_TEST_OK_V1);

        // Then
        checkFormatsInDb(3);

        assertThat(report1.getPreviousPronomCreationDate()).isNull();
        assertThat(report1.getPreviousPronomVersion()).isNull();
        assertThat(report1.getNewPronomCreationDate()).isEqualTo("2018-01-01T01:01:01.000");
        assertThat(report1.getNewPronomVersion()).isEqualTo("1");
        assertThat(report1.getStatusCode()).isEqualTo(StatusCode.OK);
        assertThat(report1.getWarnings()).isEmpty();
        assertThat(report1.getRemovedPuids()).isEmpty();
        assertThat(report1.getUpdatedPuids()).isEmpty();
        assertThat(report1.getAddedPuids()).hasSize(3);

        assertThat(report2.getPreviousPronomCreationDate()).isEqualTo("2018-01-01T01:01:01.000");
        assertThat(report2.getPreviousPronomVersion()).isEqualTo("1");
        assertThat(report2.getNewPronomCreationDate()).isEqualTo("2018-01-01T01:01:01.000");
        assertThat(report2.getNewPronomVersion()).isEqualTo("1");
        assertThat(report2.getStatusCode()).isEqualTo(StatusCode.WARNING);
        /* Warnings : Pronom version + pronom date */
        assertThat(report2.getWarnings()).hasSize(2);
        assertThat(report2.getRemovedPuids()).isEmpty();
        assertThat(report2.getAddedPuids()).isEmpty();

        assertThat(fileFormatBefore.getVersion()).isEqualTo(0);
        assertThat(fileFormatBefore.getString(FileFormat.NAME)).isEqualTo("Microsoft Word for Macintosh Document V1");
        assertThat(fileFormatBefore.getString(FileFormat.VERSION_PRONOM)).isEqualTo("1");

        final FileFormat fileFormatAfter = formatFile.findDocumentById("x-fmt/2");
        assertThat(fileFormatAfter.getVersion()).isEqualTo(1);
        assertThat(fileFormatAfter.getString(FileFormat.NAME)).isEqualTo("Microsoft Word for Macintosh Document V1");
        assertThat(fileFormatAfter.getString(FileFormat.VERSION_PRONOM)).isEqualTo("1");
    }

    private FormatImportReport importFormatFileAndDownloadReport(String fileToTest)
        throws VitamException, FileNotFoundException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(TENANT_ID));
        String requestId = VitamThreadUtils.getVitamSession().getRequestId();

        JsonNode result = JsonHandler.createObjectNode()
            .set(
                "$results",
                JsonHandler.createArrayNode()
                    .add(
                        JsonHandler.createObjectNode()
                            .put("evType", "STP_IMPORT_RULES")
                            .put("evDateTime", "2018-11-28T15:41:10.752")
                            .put("evId", requestId)
                    )
            );

        doReturn(result).when(logbookOperationsClient).selectOperationById(requestId);

        ByteArrayOutputStream reportStream = new ByteArrayOutputStream();
        doAnswer(args -> {
            InputStream is = args.getArgument(0);
            IOUtils.copy(is, reportStream);
            return null;
        })
            .when(functionalBackupService)
            .saveFile(any(), any(), eq(FILE_FORMAT_REPORT), eq(DataCategory.REPORT), eq(requestId + ".json"));

        // When
        formatFile.importFile(new FileInputStream(PropertiesUtils.findFile(fileToTest)), fileToTest);

        return JsonHandler.getFromInputStream(
            new ByteArrayInputStream(reportStream.toByteArray()),
            FormatImportReport.class
        );
    }

    private void checkFormatsInDb(int expected) {
        final MongoClient client = mongoRule.getMongoClient();
        final MongoCollection<Document> collection = client
            .getDatabase(mongoRule.getMongoDatabase().getName())
            .getCollection(FunctionalAdminCollections.FORMATS.getName());
        assertEquals(expected, collection.countDocuments());
    }
}

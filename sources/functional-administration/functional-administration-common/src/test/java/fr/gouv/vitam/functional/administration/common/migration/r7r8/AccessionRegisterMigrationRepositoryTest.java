package fr.gouv.vitam.functional.administration.common.migration.r7r8;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Lists;
import com.mongodb.client.MongoCollection;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.collection.CloseableIterator;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterDetail;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterSummary;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import org.assertj.core.api.Assertions;
import org.bson.Document;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class AccessionRegisterMigrationRepositoryTest {

    private static AccessionRegisterMigrationRepository repository;

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    @ClassRule
    public static MongoRule mongoRule =
            new MongoRule(VitamCollection.getMongoClientOptions(Lists.newArrayList(AccessionRegisterDetail.class, AccessionRegisterSummary.class)), "Vitam-Test",
                    FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getName(),
                    FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getName());

    @Before
    public void setUpBeforeClass() throws Exception {
        FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getVitamCollection().initialize(mongoRule.getMongoDatabase(), true);
        FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getVitamCollection().initialize(mongoRule.getMongoDatabase(), true);
        repository = new AccessionRegisterMigrationRepository();
    }

    @After
    public void tearDown() {
        mongoRule.handleAfter();
    }

    @Test
    public void testSelectAccessionRegisterDetailAndSummary_emptyDataSet() {
        // Given / When
        try (CloseableIterator<List<Document>> listCloseableIterator = repository.selectAccessionRegistesBulk(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL)) {
            // Then
            assertThat(listCloseableIterator.hasNext()).isFalse();
        }

        // Given / When
        try (CloseableIterator<List<Document>> listCloseableIterator = repository.selectAccessionRegistesBulk(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY)) {
            // Then
            assertThat(listCloseableIterator.hasNext()).isFalse();
        }
    }

    @Test
    public void testSelectAccessionRegisterDetail_ThenIteratorHasOneNextWithSizeTwo() throws Exception {

        // Given : Complex data set with R6 model (no graph fields)
        String accessionRegisterDetailDataSetFile = "migration_r7_r8/accession_register_detail.json";
        importDataSetFile(repository.getAccessionRegisterDetailCollection(), accessionRegisterDetailDataSetFile);

        // When
        try (CloseableIterator<List<Document>> listCloseableIterator = repository.selectAccessionRegistesBulk(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL)) {


            // Then
            Assertions.assertThat(listCloseableIterator.hasNext());

            Assertions.assertThat(listCloseableIterator.next()).hasSize(2);
        }
    }

    @Test
    public void testSelectAccessionRegisterDetail_ThenIteratorHasTwoNextWithSizeOne() throws Exception {
        int batch = VitamConfiguration.getBatchSize();

        VitamConfiguration.setBatchSize(1);

        // Given : Complex data set with R6 model (no graph fields)
        String accessionRegisterDetailDataSetFile = "migration_r7_r8/accession_register_detail.json";
        importDataSetFile(repository.getAccessionRegisterDetailCollection(), accessionRegisterDetailDataSetFile);

        // When
        try (CloseableIterator<List<Document>> listCloseableIterator = repository.selectAccessionRegistesBulk(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL)) {


            // Then
            Assertions.assertThat(listCloseableIterator.hasNext());
            Assertions.assertThat(listCloseableIterator.next()).hasSize(1);

            Assertions.assertThat(listCloseableIterator.hasNext());
            Assertions.assertThat(listCloseableIterator.next()).hasSize(1);
        }
        VitamConfiguration.setBatchSize(batch);


    }

    private void importDataSetFile(MongoCollection<Document> mongoCollection, String dataSetFile)
            throws FileNotFoundException, InvalidParseOperationException {
        InputStream inputDataSet = PropertiesUtils.getResourceAsStream(dataSetFile);
        ArrayNode jsonDataSet = (ArrayNode) JsonHandler.getFromInputStream(inputDataSet);
        for (JsonNode jsonNode : jsonDataSet) {
            mongoCollection.insertOne(Document.parse(JsonHandler.unprettyPrint(jsonNode)));
        }
    }
}
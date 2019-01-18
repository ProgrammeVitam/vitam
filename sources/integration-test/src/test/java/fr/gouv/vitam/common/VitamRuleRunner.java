package fr.gouv.vitam.common;

import fr.gouv.vitam.batch.report.rest.repository.EliminationActionObjectGroupRepository;
import fr.gouv.vitam.batch.report.rest.repository.EliminationActionUnitRepository;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.database.offset.OffsetRepository;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.functional.administration.common.server.ElasticsearchAccessFunctionalAdmin;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookElasticsearchAccess;
import fr.gouv.vitam.metadata.core.database.collections.ElasticsearchAccessMetadata;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.processing.data.core.ProcessDataAccessImpl;
import fr.gouv.vitam.security.internal.rest.repository.PersonalRepository;
import fr.gouv.vitam.storage.offers.common.database.OfferLogDatabaseService;
import fr.gouv.vitam.storage.offers.common.database.OfferSequenceDatabaseService;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class VitamRuleRunner {

    public static final String OFFER_FOLDER = "offer";


    enum Prefix {
        PREFIX(GUIDFactory.newGUID().getId());

        String prefix;

        Prefix(String prefix) {
            this.prefix = prefix;
        }

        public String getPrefix() {
            return prefix;
        }
    }


    @ClassRule
    public static DisableCacheContainerRule disableCacheContainerRule = new DisableCacheContainerRule();

    // Rules
    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    @ClassRule
    public static MongoRule mongoRule =
        new MongoRule(VitamCollection.getMongoClientOptions(
            merge(MetadataCollections.getClasses(), LogbookCollections.getClasses(),
                FunctionalAdminCollections.getClasses())), "Vitam-Test",
            OfferSequenceDatabaseService.OFFER_SEQUENCE_COLLECTION,
            OffsetRepository.COLLECTION_NAME,
            OfferLogDatabaseService.OFFER_LOG_COLLECTION_NAME,
            PersonalRepository.PERSONAL_COLLECTION,
            EliminationActionObjectGroupRepository.ELIMINATION_ACTION_OBJECT_GROUP,
            EliminationActionUnitRepository.ELIMINATION_ACTION_UNIT);


    @ClassRule
    public static ElasticsearchRule elasticsearchRule = new ElasticsearchRule();

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    public static void handleBeforeClass(Integer... tenants) throws Exception {
        handleBeforeClass(Prefix.PREFIX.getPrefix(), tenants);
    }

    public static void handleBeforeClass(String prefix, Integer... tenants) throws Exception {
        // ES client
        final List<ElasticsearchNode> esNodes = new ArrayList<>();
        esNodes.add(new ElasticsearchNode("localhost", elasticsearchRule.getTcpPort()));
        MetadataCollections.beforeTestClass(mongoRule.getMongoDatabase(), prefix,
            new ElasticsearchAccessMetadata(elasticsearchRule.getClusterName(), esNodes), tenants);
        FunctionalAdminCollections.beforeTestClass(mongoRule.getMongoDatabase(), prefix,
            new ElasticsearchAccessFunctionalAdmin(elasticsearchRule.getClusterName(), esNodes));
        LogbookCollections.beforeTestClass(mongoRule.getMongoDatabase(), prefix,
            new LogbookElasticsearchAccess(elasticsearchRule.getClusterName(), esNodes), tenants);
    }

    public static void handleAfterClass(Integer... tenants) throws Exception {
        final List<ElasticsearchNode> esNodes = new ArrayList<>();
        esNodes.add(new ElasticsearchNode("localhost", elasticsearchRule.getTcpPort()));

        MetadataCollections
            .afterTestClass(new ElasticsearchAccessMetadata(elasticsearchRule.getClusterName(), esNodes), tenants);
        LogbookCollections
            .afterTestClass(new LogbookElasticsearchAccess(elasticsearchRule.getClusterName(), esNodes), tenants);
        FunctionalAdminCollections
            .afterTestClass(new ElasticsearchAccessFunctionalAdmin(elasticsearchRule.getClusterName(), esNodes));
    }

    public static void handleAfter(Integer... tenants) throws Exception {
        final List<ElasticsearchNode> esNodes = new ArrayList<>();
        esNodes.add(new ElasticsearchNode("localhost", elasticsearchRule.getTcpPort()));

        MetadataCollections
                .afterTestClass(new ElasticsearchAccessMetadata(elasticsearchRule.getClusterName(), esNodes), tenants);
        LogbookCollections
                .afterTestClass(new LogbookElasticsearchAccess(elasticsearchRule.getClusterName(), esNodes), tenants);
    }

    private static List<Class<?>> merge(List<Class<?>> classes, List<Class<?>> classes1, List<Class<?>> classes2) {
        classes.addAll(classes1);
        classes.addAll(classes2);
        return classes;
    }

    public static void runAfterMongo(Set<String> collections) {
        // clean offers
        cleanOffers();
        mongoRule.handleAfter(collections);
    }

    public static void runAfterEs(Set<String> collections) {
        // clean offers
        cleanOffers();
        elasticsearchRule.handleAfter(collections);
    }

    public static void runAfter() {
        // clean offers
        cleanOffers();
        mongoRule.handleAfter();
        elasticsearchRule.handleAfter();
        ProcessDataAccessImpl.getInstance().clearWorkflow();
    }


    /**
     * Clean offers content.
     */
    public static void cleanOffers() {
        // ugly style but we don't have the digest herelo
        File directory = new File(OFFER_FOLDER);
        if (directory.exists()) {
            try {
                Files.walk(directory.toPath())
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            } catch (IOException | IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
    }
}

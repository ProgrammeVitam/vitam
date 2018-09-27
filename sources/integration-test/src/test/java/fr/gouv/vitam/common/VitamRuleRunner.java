package fr.gouv.vitam.common;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Set;

import fr.gouv.vitam.batch.report.rest.repository.EliminationActionObjectGroupRepository;
import fr.gouv.vitam.batch.report.rest.repository.EliminationActionUnitRepository;
import fr.gouv.vitam.common.database.offset.OffsetRepository;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.database.collections.MongoDbAccessMetadataImpl;
import fr.gouv.vitam.processing.data.core.ProcessDataAccessImpl;
import fr.gouv.vitam.security.internal.rest.repository.PersonalRepository;
import fr.gouv.vitam.storage.offers.common.database.OfferLogDatabaseService;
import fr.gouv.vitam.storage.offers.common.database.OfferSequenceDatabaseService;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

public class VitamRuleRunner {

    public static final String OFFER_FOLDER = "offer";



    // Rules
    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    @ClassRule
    public static MongoRule mongoRule =
        new MongoRule(MongoDbAccessMetadataImpl.getMongoClientOptions(), "Vitam-Test",
            MetadataCollections.UNIT.getName(),
            MetadataCollections.OBJECTGROUP.getName(),
            LogbookCollections.OPERATION.getName(),
            LogbookCollections.LIFECYCLE_UNIT.getName(),
            LogbookCollections.LIFECYCLE_OBJECTGROUP.getName(),
            LogbookCollections.LIFECYCLE_OBJECTGROUP_IN_PROCESS.getName(),
            LogbookCollections.LIFECYCLE_UNIT_IN_PROCESS.getName(),
            FunctionalAdminCollections.RULES.getName(),
            FunctionalAdminCollections.PROFILE.getName(),
            FunctionalAdminCollections.AGENCIES.getName(),
            FunctionalAdminCollections.CONTEXT.getName(),
            FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getName(),
            FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getName(),
                FunctionalAdminCollections.ACCESSION_REGISTER_SYMBOLIC.getName(),
            FunctionalAdminCollections.ACCESS_CONTRACT.getName(),
            FunctionalAdminCollections.FORMATS.getName(),
            FunctionalAdminCollections.INGEST_CONTRACT.getName(),
            FunctionalAdminCollections.SECURITY_PROFILE.getName(),
            FunctionalAdminCollections.ONTOLOGY.getName(),
            FunctionalAdminCollections.ARCHIVE_UNIT_PROFILE.getName(),
            //FunctionalAdminCollections.VITAM_SEQUENCE.getName(),
            OfferSequenceDatabaseService.OFFER_SEQUENCE_COLLECTION,
            OffsetRepository.COLLECTION_NAME,
            OfferLogDatabaseService.OFFER_LOG_COLLECTION_NAME,
            PersonalRepository.PERSONAL_COLLECTION,
            EliminationActionObjectGroupRepository.ELIMINATION_ACTION_OBJECT_GROUP,
            EliminationActionUnitRepository.ELIMINATION_ACTION_UNIT);

    @ClassRule
    public static ElasticsearchRule elasticsearchRule =
        new ElasticsearchRule(org.assertj.core.util.Files.newTemporaryFolder(),
            MetadataCollections.UNIT.getName().toLowerCase() + "_0",
            MetadataCollections.UNIT.getName().toLowerCase() + "_1",
            MetadataCollections.OBJECTGROUP.getName().toLowerCase() + "_0",
            MetadataCollections.OBJECTGROUP.getName().toLowerCase() + "_1",
            LogbookCollections.OPERATION.getName().toLowerCase() + "_0",
            LogbookCollections.OPERATION.getName().toLowerCase() + "_1",
            FunctionalAdminCollections.RULES.getName().toLowerCase(),
            FunctionalAdminCollections.PROFILE.getName().toLowerCase(),
            FunctionalAdminCollections.AGENCIES.getName().toLowerCase(),
            FunctionalAdminCollections.CONTEXT.getName().toLowerCase(),
            FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getName().toLowerCase(),
            FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getName().toLowerCase(),
                FunctionalAdminCollections.ACCESSION_REGISTER_SYMBOLIC.getName().toLowerCase(),
            FunctionalAdminCollections.ACCESS_CONTRACT.getName().toLowerCase(),
            FunctionalAdminCollections.FORMATS.getName().toLowerCase(),
            FunctionalAdminCollections.INGEST_CONTRACT.getName().toLowerCase(),
            FunctionalAdminCollections.SECURITY_PROFILE.getName().toLowerCase(),
            FunctionalAdminCollections.ONTOLOGY.getName().toLowerCase(),
            FunctionalAdminCollections.ARCHIVE_UNIT_PROFILE.getName().toLowerCase()
        );

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());


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

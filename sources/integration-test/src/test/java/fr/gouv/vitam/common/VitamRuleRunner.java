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
package fr.gouv.vitam.common;

import com.google.common.collect.Lists;
import fr.gouv.vitam.batch.report.rest.repository.AuditReportRepository;
import fr.gouv.vitam.batch.report.rest.repository.EliminationActionUnitRepository;
import fr.gouv.vitam.batch.report.rest.repository.PurgeObjectGroupRepository;
import fr.gouv.vitam.batch.report.rest.repository.PurgeUnitRepository;
import fr.gouv.vitam.batch.report.rest.repository.TransferReplyUnitRepository;
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
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollectionsTestUtils;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollectionsTestUtils;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookElasticsearchAccess;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollectionsTestUtils;
import fr.gouv.vitam.metadata.core.mapping.MappingLoader;
import fr.gouv.vitam.metadata.core.database.collections.ElasticsearchAccessMetadata;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.processing.data.core.ProcessDataAccessImpl;
import fr.gouv.vitam.security.internal.rest.repository.IdentityRepository;
import fr.gouv.vitam.security.internal.rest.repository.PersonalRepository;
import fr.gouv.vitam.storage.engine.common.collection.OfferCollections;
import org.junit.ClassRule;
import org.junit.Rule;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

    @ClassRule
    public static MongoRule mongoRule =
        new MongoRule(VitamCollection.getMongoClientOptions(
            merge(MetadataCollections.getClasses(), LogbookCollections.getClasses(),
                FunctionalAdminCollections.getClasses())),
            OfferCollections.OFFER_SEQUENCE.getName(),
            OffsetRepository.COLLECTION_NAME,
            OfferCollections.OFFER_LOG.getName(),
            OfferCollections.COMPACTED_OFFER_LOG.getName(),
            PersonalRepository.PERSONAL_COLLECTION,
            IdentityRepository.CERTIFICATE_COLLECTION,
            PurgeObjectGroupRepository.PURGE_OBJECT_GROUP,
            PurgeUnitRepository.PURGE_UNIT,
            EliminationActionUnitRepository.ELIMINATION_ACTION_UNIT,
            TransferReplyUnitRepository.TRANSFER_REPLY_UNIT,
            AuditReportRepository.AUDIT_OBJECT_GROUP);

    @ClassRule
    public static ElasticsearchRule elasticsearchRule = new ElasticsearchRule();

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    public static void handleBeforeClass(Integer... tenants) throws Exception {
        handleBeforeClass(Prefix.PREFIX.getPrefix(), tenants);
        FunctionalAdminCollections.VITAM_SEQUENCE.getVitamCollection()
            .setName(FunctionalAdminCollections.VITAM_SEQUENCE.getVitamCollection().getClasz().getSimpleName());
    }

    public static void handleBeforeClass(String prefix, Integer... tenants) throws Exception {
        // ES client
        List<ElasticsearchNode> esNodes =
            Lists.newArrayList(new ElasticsearchNode(ElasticsearchRule.getHost(), ElasticsearchRule.getPort()));

        MappingLoader mappingLoader = MappingLoaderTestUtils.getTestMappingLoader();

        MetadataCollectionsTestUtils.beforeTestClass(mongoRule.getMongoDatabase(), prefix,
            new ElasticsearchAccessMetadata(elasticsearchRule.getClusterName(), esNodes, mappingLoader), tenants);
        FunctionalAdminCollectionsTestUtils.beforeTestClass(mongoRule.getMongoDatabase(), prefix,
            new ElasticsearchAccessFunctionalAdmin(elasticsearchRule.getClusterName(), esNodes));
        LogbookCollectionsTestUtils.beforeTestClass(mongoRule.getMongoDatabase(), prefix,
            new LogbookElasticsearchAccess(elasticsearchRule.getClusterName(), esNodes), tenants);
    }

    public static void handleAfterClass(Integer... tenants) {
        MetadataCollectionsTestUtils
            .afterTestClass(false, tenants);
        LogbookCollectionsTestUtils
            .afterTestClass(false, tenants);
        FunctionalAdminCollectionsTestUtils.afterTestClass(false);
    }

    public static void handleAfterClassExceptReferential(Integer... tenants) {
        MetadataCollectionsTestUtils
            .afterTestClass(false, tenants);
        LogbookCollectionsTestUtils
            .afterTestClass(false, tenants);
    }
    public static void handleAfter(Integer... tenants) {
        MetadataCollectionsTestUtils.afterTest(tenants);
        LogbookCollectionsTestUtils.afterTest(tenants);
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

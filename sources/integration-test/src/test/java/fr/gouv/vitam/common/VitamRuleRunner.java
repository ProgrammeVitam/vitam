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
package fr.gouv.vitam.common;

import com.google.common.collect.Lists;
import fr.gouv.vitam.batch.report.rest.repository.AuditReportRepository;
import fr.gouv.vitam.batch.report.rest.repository.EliminationActionUnitRepository;
import fr.gouv.vitam.batch.report.rest.repository.EvidenceAuditReportRepository;
import fr.gouv.vitam.batch.report.rest.repository.PurgeObjectGroupRepository;
import fr.gouv.vitam.batch.report.rest.repository.PurgeUnitRepository;
import fr.gouv.vitam.batch.report.rest.repository.TransferReplyUnitRepository;
import fr.gouv.vitam.collect.internal.core.repository.TransactionRepository;
import fr.gouv.vitam.common.database.offset.OffsetRepository;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchIndexAlias;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.functional.administration.common.config.ElasticsearchFunctionalAdminIndexManager;
import fr.gouv.vitam.functional.administration.common.server.ElasticsearchAccessFunctionalAdmin;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollectionsTestUtils;
import fr.gouv.vitam.logbook.common.server.config.ElasticsearchLogbookIndexManager;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollectionsTestUtils;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookElasticsearchAccess;
import fr.gouv.vitam.metadata.core.config.ElasticsearchMetadataIndexManager;
import fr.gouv.vitam.metadata.core.database.collections.ElasticsearchAccessMetadata;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollectionsTestUtils;
import fr.gouv.vitam.processing.data.core.ProcessDataAccessImpl;
import fr.gouv.vitam.security.internal.rest.repository.IdentityRepository;
import fr.gouv.vitam.security.internal.rest.repository.PersonalRepository;
import fr.gouv.vitam.storage.engine.common.collection.OfferCollections;
import org.junit.ClassRule;
import org.junit.Rule;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class VitamRuleRunner {

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
    public static final MongoRule mongoRule = new MongoRule(
        MongoDbAccess.getMongoClientSettingsBuilder(
            merge(
                MetadataCollections.getClasses(),
                LogbookCollections.getClasses(),
                FunctionalAdminCollections.getClasses()
            )
        ),
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
        AuditReportRepository.AUDIT_OBJECT_GROUP,
        EvidenceAuditReportRepository.EVIDENCE_AUDIT,
        TransactionRepository.TRANSACTION_COLLECTION
    );

    @ClassRule
    public static final ElasticsearchRule elasticsearchRule = new ElasticsearchRule();

    protected static ElasticsearchLogbookIndexManager logbookIndexManager;
    protected static ElasticsearchMetadataIndexManager metadataIndexManager;
    protected static ElasticsearchFunctionalAdminIndexManager functionalAdminIndexManager;

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    public static void handleBeforeClass(List<Integer> dedicatedTenants, Map<String, List<Integer>> tenantGroups)
        throws Exception {
        handleBeforeClass(Prefix.PREFIX.getPrefix(), dedicatedTenants, tenantGroups);
        FunctionalAdminCollections.VITAM_SEQUENCE.getVitamCollection()
            .setName(FunctionalAdminCollections.VITAM_SEQUENCE.getVitamCollection().getClasz().getSimpleName());
    }

    public static void handleBeforeClass(
        String prefix,
        List<Integer> dedicatedTenants,
        Map<String, List<Integer>> tenantGroups
    ) throws Exception {
        logbookIndexManager = LogbookCollectionsTestUtils.createTestIndexManager(dedicatedTenants, tenantGroups);
        metadataIndexManager = MetadataCollectionsTestUtils.createTestIndexManager(
            dedicatedTenants,
            tenantGroups,
            MappingLoaderTestUtils.getTestMappingLoader()
        );
        functionalAdminIndexManager = FunctionalAdminCollectionsTestUtils.createTestIndexManager();

        // ES client
        List<ElasticsearchNode> esNodes = Lists.newArrayList(
            new ElasticsearchNode(ElasticsearchRule.getHost(), ElasticsearchRule.getPort())
        );

        MetadataCollectionsTestUtils.beforeTestClass(
            mongoRule.getMongoDatabase(),
            prefix,
            new ElasticsearchAccessMetadata(ElasticsearchRule.getClusterName(), esNodes, metadataIndexManager)
        );
        FunctionalAdminCollectionsTestUtils.beforeTestClass(
            mongoRule.getMongoDatabase(),
            prefix,
            new ElasticsearchAccessFunctionalAdmin(
                ElasticsearchRule.getClusterName(),
                esNodes,
                functionalAdminIndexManager
            )
        );
        LogbookCollectionsTestUtils.beforeTestClass(
            mongoRule.getMongoDatabase(),
            prefix,
            new LogbookElasticsearchAccess(ElasticsearchRule.getClusterName(), esNodes, logbookIndexManager)
        );
    }

    public static void handleAfterClass() {
        MetadataCollectionsTestUtils.afterTestClass(metadataIndexManager, false);
        LogbookCollectionsTestUtils.afterTestClass(logbookIndexManager, false);
        FunctionalAdminCollectionsTestUtils.afterTestClass(false);
    }

    public static void handleAfterClassExceptReferential() {
        MetadataCollectionsTestUtils.afterTestClass(metadataIndexManager, false);
        LogbookCollectionsTestUtils.afterTestClass(logbookIndexManager, false);
    }

    public static void handleAfter() {
        MetadataCollectionsTestUtils.afterTest(metadataIndexManager);
        LogbookCollectionsTestUtils.afterTest(logbookIndexManager);
    }

    private static List<Class<?>> merge(List<Class<?>> classes, List<Class<?>> classes1, List<Class<?>> classes2) {
        classes.addAll(classes1);
        classes.addAll(classes2);
        return classes;
    }

    public static void runAfterMongo(Set<String> collections) {
        // clean offers
        VitamServerRunner.cleanOffers();
        mongoRule.handleAfter(collections);
    }

    public static void runAfterEs(ElasticsearchIndexAlias... indexAliases) {
        // clean offers
        VitamServerRunner.cleanOffers();
        elasticsearchRule.handleAfter(
            Arrays.stream(indexAliases).map(ElasticsearchIndexAlias::getName).collect(Collectors.toSet())
        );
    }

    public static void runAfter() {
        // clean offers
        VitamServerRunner.cleanOffers();
        mongoRule.handleAfter();
        elasticsearchRule.handleAfter();
        ProcessDataAccessImpl.getInstance().clearWorkflow();
    }
}

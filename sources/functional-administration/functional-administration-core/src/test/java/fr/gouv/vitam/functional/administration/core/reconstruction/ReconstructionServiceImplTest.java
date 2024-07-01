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
package fr.gouv.vitam.functional.administration.core.reconstruction;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.api.VitamRepositoryProvider;
import fr.gouv.vitam.common.database.api.impl.VitamElasticsearchRepository;
import fr.gouv.vitam.common.database.api.impl.VitamMongoRepository;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.functional.administration.common.CollectionBackupModel;
import fr.gouv.vitam.functional.administration.common.VitamSequence;
import fr.gouv.vitam.functional.administration.common.config.ElasticsearchFunctionalAdminIndexManager;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollectionsTestUtils;
import fr.gouv.vitam.functional.administration.core.backup.RestoreBackupService;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import org.bson.Document;
import org.elasticsearch.common.Strings;
import org.elasticsearch.xcontent.XContentBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.elasticsearch.xcontent.XContentFactory.jsonBuilder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test the reconstruction services.
 */
public class ReconstructionServiceImplTest {

    /**
     * Vitam Logger.
     */
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ReconstructionServiceImplTest.class);

    private static final String STRATEGY_ID = VitamConfiguration.getDefaultStrategy();
    private static final Integer TENANT_ID_0 = 0;
    private static final Integer TENANT_ID_1 = 1;
    private static final Integer TENANT_ID_2 = 2;

    private static final String ES_BULK_EXCEPTION_MESSAGE = "ElasticSearch: Bulk Request failure.";
    private static final String MONGODB_BULK_EXCEPTION_MESSAGE = "MongoDB: Bulk Request failure.";
    public static final String SEQUENCE_NAME = "fake name";
    public static final String BACKUP_SEQUENCE_NAME = "fake backup name";
    private static final String DEFAULT_OFFER = "default";

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private VitamMongoRepository multiTenantMongoRepository;

    @Mock
    private VitamMongoRepository crossTenantMongoRepository;

    @Mock
    private VitamElasticsearchRepository mutliTenantElasticsearchRepository;

    @Mock
    private VitamElasticsearchRepository crossTenantElasticsearchRepository;

    @Mock
    private RestoreBackupService recoverBuckupService;

    @Mock
    private VitamRepositoryProvider repositoryFactory;

    @Mock
    private StorageClientFactory storageClientFactory;

    @Mock
    private StorageClient storageClient;

    @Mock
    private FunctionalAdministrationReconstructionMetricsCache reconstructionMetricsCache;

    @Captor
    private ArgumentCaptor<Integer> tenantCaptor;

    private final ElasticsearchFunctionalAdminIndexManager indexManager =
        FunctionalAdminCollectionsTestUtils.createTestIndexManager();
    private ReconstructionServiceImpl reconstructionService;

    @Before
    public void setup() {
        reconstructionService = Mockito.spy(
            new ReconstructionServiceImpl(
                repositoryFactory,
                recoverBuckupService,
                null,
                indexManager,
                reconstructionMetricsCache
            )
        );

        when(
            repositoryFactory.getVitamESRepository(eq(FunctionalAdminCollections.RULES.getVitamCollection()), any())
        ).thenReturn(mutliTenantElasticsearchRepository);
        when(
            repositoryFactory.getVitamMongoRepository(FunctionalAdminCollections.RULES.getVitamCollection())
        ).thenReturn(multiTenantMongoRepository);

        when(
            repositoryFactory.getVitamESRepository(eq(FunctionalAdminCollections.FORMATS.getVitamCollection()), any())
        ).thenReturn(crossTenantElasticsearchRepository);
        when(
            repositoryFactory.getVitamMongoRepository(FunctionalAdminCollections.FORMATS.getVitamCollection())
        ).thenReturn(crossTenantMongoRepository);
        when(storageClientFactory.getClient()).thenReturn(storageClient);
    }

    @Test
    @RunWithCustomExecutor
    public void reconstructMultiTenantCollectionByTenantOK() throws Exception {
        // mock the recoverBackupCopy service.
        Optional<CollectionBackupModel> backupCollection = getBackupCollection(TENANT_ID_0);
        when(
            recoverBuckupService.readLatestSavedFile(
                eq(STRATEGY_ID),
                eq(DEFAULT_OFFER),
                eq(FunctionalAdminCollections.RULES)
            )
        ).thenReturn(backupCollection);
        when(
            repositoryFactory.getVitamMongoRepository(FunctionalAdminCollections.VITAM_SEQUENCE.getVitamCollection())
        ).thenReturn(multiTenantMongoRepository);

        // call the reconstruction service.
        LOGGER.debug(String.format("Reconstruction of Vitam collection by tenant %s.", TENANT_ID_0));

        reconstructionService.reconstruct(FunctionalAdminCollections.RULES, TENANT_ID_0);

        verify(multiTenantMongoRepository, times(1)).purge(TENANT_ID_0);
        verify(mutliTenantElasticsearchRepository, times(1)).purge(TENANT_ID_0);
        verify(multiTenantMongoRepository, times(1)).removeByNameAndTenant(SEQUENCE_NAME, TENANT_ID_0);
        verify(multiTenantMongoRepository, times(1)).removeByNameAndTenant(BACKUP_SEQUENCE_NAME, TENANT_ID_0);

        verify(recoverBuckupService).readLatestSavedFile(
            eq(STRATEGY_ID),
            eq(DEFAULT_OFFER),
            eq(FunctionalAdminCollections.RULES)
        );

        verify(multiTenantMongoRepository, times(1)).save(backupCollection.get().getDocuments());
        verify(multiTenantMongoRepository, times(1)).save(backupCollection.get().getSequence());
        verify(multiTenantMongoRepository, times(1)).save(backupCollection.get().getBackupSequence());
        verify(mutliTenantElasticsearchRepository, times(1)).save(backupCollection.get().getDocuments());
    }

    @Test
    @RunWithCustomExecutor
    public void reconstructMultiTenantCollectionWithEmptyContentByTenantOK() throws Exception {
        // Given
        Optional<CollectionBackupModel> backupCollection = Optional.of(
            JsonHandler.getFromInputStream(
                PropertiesUtils.getResourceAsStream("0_FileRules_6.json"),
                CollectionBackupModel.class
            )
        );
        when(
            recoverBuckupService.readLatestSavedFile(
                eq(STRATEGY_ID),
                eq(DEFAULT_OFFER),
                eq(FunctionalAdminCollections.RULES)
            )
        ).thenReturn(backupCollection);
        when(
            repositoryFactory.getVitamMongoRepository(FunctionalAdminCollections.VITAM_SEQUENCE.getVitamCollection())
        ).thenReturn(multiTenantMongoRepository);

        // When
        reconstructionService.reconstruct(FunctionalAdminCollections.RULES, TENANT_ID_0);

        // Then
        verify(multiTenantMongoRepository, times(1)).purge(TENANT_ID_0);
        verify(mutliTenantElasticsearchRepository, times(1)).purge(TENANT_ID_0);
        verify(multiTenantMongoRepository, times(1)).removeByNameAndTenant("RULE", TENANT_ID_0);
        verify(multiTenantMongoRepository, times(1)).removeByNameAndTenant("BACKUP_RULE", TENANT_ID_0);

        verify(recoverBuckupService).readLatestSavedFile(
            eq(STRATEGY_ID),
            eq(DEFAULT_OFFER),
            eq(FunctionalAdminCollections.RULES)
        );

        verify(multiTenantMongoRepository, never()).save(anyList());

        verify(multiTenantMongoRepository, times(1)).save(backupCollection.get().getSequence());
        verify(multiTenantMongoRepository, times(1)).save(backupCollection.get().getBackupSequence());
    }

    @Test
    @RunWithCustomExecutor
    public void reconstructCrossTenantCollectionByTenantOK() throws Exception {
        // mock the recoverBackupCopy service.
        Optional<CollectionBackupModel> backupCollection = getBackupCollection(TENANT_ID_1);
        when(
            recoverBuckupService.readLatestSavedFile(
                eq(STRATEGY_ID),
                eq(DEFAULT_OFFER),
                eq(FunctionalAdminCollections.FORMATS)
            )
        ).thenReturn(backupCollection);

        when(
            repositoryFactory.getVitamMongoRepository(FunctionalAdminCollections.VITAM_SEQUENCE.getVitamCollection())
        ).thenReturn(crossTenantMongoRepository);

        // call the reconstruction service.
        LOGGER.debug(String.format("Reconstruction of Vitam collection by tenant %s.", TENANT_ID_0));

        reconstructionService.reconstruct(FunctionalAdminCollections.FORMATS, TENANT_ID_0);

        verify(crossTenantMongoRepository, times(1)).purge();
        verify(crossTenantMongoRepository, times(1)).removeByNameAndTenant(SEQUENCE_NAME, TENANT_ID_1);
        verify(crossTenantMongoRepository, times(1)).removeByNameAndTenant(BACKUP_SEQUENCE_NAME, TENANT_ID_1);
        verify(crossTenantElasticsearchRepository, times(1)).purge();

        verify(recoverBuckupService).readLatestSavedFile(
            eq(STRATEGY_ID),
            eq(DEFAULT_OFFER),
            eq(FunctionalAdminCollections.FORMATS)
        );

        verify(crossTenantMongoRepository, times(1)).save(backupCollection.get().getDocuments());
        verify(crossTenantMongoRepository, times(1)).save(backupCollection.get().getSequence());
        verify(crossTenantMongoRepository, times(1)).save(backupCollection.get().getBackupSequence());
        verify(crossTenantElasticsearchRepository, times(1)).save(backupCollection.get().getDocuments());
    }

    @Test
    @RunWithCustomExecutor
    public void reconstructCollectionByTenantOK_NoBackupCopy() throws Exception {
        Optional<CollectionBackupModel> backupCollection = getBackupCollection(TENANT_ID_1);
        when(
            recoverBuckupService.readLatestSavedFile(
                eq(STRATEGY_ID),
                eq(DEFAULT_OFFER),
                eq(FunctionalAdminCollections.RULES)
            )
        ).thenReturn(backupCollection);

        when(
            repositoryFactory.getVitamMongoRepository(FunctionalAdminCollections.VITAM_SEQUENCE.getVitamCollection())
        ).thenReturn(multiTenantMongoRepository);

        // call the reconstruction service.
        LOGGER.debug(String.format("Reconstruction of Vitam collection by tenant %s.", TENANT_ID_0));
        reconstructionService.reconstruct(FunctionalAdminCollections.RULES, TENANT_ID_0);

        verify(multiTenantMongoRepository).purge(TENANT_ID_0);
        verify(mutliTenantElasticsearchRepository).purge(TENANT_ID_0);
        verify(recoverBuckupService, times(1)).readLatestSavedFile(
            eq(STRATEGY_ID),
            eq(DEFAULT_OFFER),
            eq(FunctionalAdminCollections.RULES)
        );

        // No call of the saving services when no backup copy found..
        verify(multiTenantMongoRepository, never()).save(getBackupCollection(TENANT_ID_0).get().getDocuments());
        verify(multiTenantMongoRepository, never()).save(getBackupCollection(TENANT_ID_0).get().getSequence());
        verify(multiTenantMongoRepository, never()).save(getBackupCollection(TENANT_ID_0).get().getBackupSequence());
        verify(mutliTenantElasticsearchRepository, never()).save(getBackupCollection(TENANT_ID_0).get().getDocuments());
    }

    @Test
    @RunWithCustomExecutor
    public void reconstructCollectionByTenantESKO() throws Exception {
        // mock thrown database Exception by elasticSearch.
        when(mutliTenantElasticsearchRepository.purge(TENANT_ID_0)).thenThrow(
            new DatabaseException(ES_BULK_EXCEPTION_MESSAGE)
        );

        // mock the recoverBackupCopy service.
        Optional<CollectionBackupModel> backupCollection = getBackupCollection(TENANT_ID_1);
        when(
            recoverBuckupService.readLatestSavedFile(
                eq(STRATEGY_ID),
                eq(DEFAULT_OFFER),
                eq(FunctionalAdminCollections.RULES)
            )
        ).thenReturn(backupCollection);

        // verify type and message of the thrown elasticSearch Exception.
        assertThatThrownBy(() -> reconstructionService.reconstruct(FunctionalAdminCollections.RULES, TENANT_ID_0))
            .isInstanceOf(DatabaseException.class)
            .hasMessageContaining(ES_BULK_EXCEPTION_MESSAGE);
    }

    @Test
    @RunWithCustomExecutor
    public void reconstructCollectionByTenantMongoKO() throws Exception {
        // mock the recoverBackupCopy service.
        Optional<CollectionBackupModel> backupCollection = getBackupCollection(TENANT_ID_1);
        when(
            recoverBuckupService.readLatestSavedFile(
                eq(STRATEGY_ID),
                eq(DEFAULT_OFFER),
                eq(FunctionalAdminCollections.RULES)
            )
        ).thenReturn(backupCollection);

        // mock thrown database Exception by mongoDB.
        when(multiTenantMongoRepository.purge(TENANT_ID_0)).thenThrow(
            new DatabaseException(MONGODB_BULK_EXCEPTION_MESSAGE)
        );

        // verify type and message of the thrown mongoDB Exception.
        assertThatThrownBy(() -> reconstructionService.reconstruct(FunctionalAdminCollections.RULES, TENANT_ID_0))
            .isInstanceOf(DatabaseException.class)
            .hasMessageContaining(MONGODB_BULK_EXCEPTION_MESSAGE);
    }

    @Test
    @RunWithCustomExecutor
    public void reconstructCollectionOnAllVitamTenantsOK() throws Exception {
        // mock the recoverBackupCopy service.
        Optional<CollectionBackupModel> backupCollection0 = getBackupCollection(TENANT_ID_0);
        Optional<CollectionBackupModel> backupCollection1 = getBackupCollection(TENANT_ID_1);
        Optional<CollectionBackupModel> backupCollection2 = getBackupCollection(TENANT_ID_2);
        when(
            recoverBuckupService.readLatestSavedFile(
                eq(STRATEGY_ID),
                eq(DEFAULT_OFFER),
                eq(FunctionalAdminCollections.RULES)
            )
        ).thenReturn(backupCollection0);
        when(
            recoverBuckupService.readLatestSavedFile(
                eq(STRATEGY_ID),
                eq(DEFAULT_OFFER),
                eq(FunctionalAdminCollections.RULES)
            )
        ).thenReturn(backupCollection1);
        when(
            recoverBuckupService.readLatestSavedFile(
                eq(STRATEGY_ID),
                eq(DEFAULT_OFFER),
                eq(FunctionalAdminCollections.RULES)
            )
        ).thenReturn(backupCollection2);

        when(
            repositoryFactory.getVitamMongoRepository(FunctionalAdminCollections.VITAM_SEQUENCE.getVitamCollection())
        ).thenReturn(multiTenantMongoRepository);
        // testing the reconstruction service.
        LOGGER.debug(String.format("Reconstruction of Vitam tenants."));

        VitamConfiguration.setTenants(Arrays.asList(0, 1, 2));

        // Call the construction service of the all tenants.
        reconstructionService.reconstruct(FunctionalAdminCollections.RULES);

        // for the "3" tenants, the reconstruction service is called at most 3 times.
        verify(reconstructionService, times(1)).reconstruct(any(), tenantCaptor.capture());
    }

    @Test
    @RunWithCustomExecutor
    public void reconstructCollectionOnAllVitamTenantsKO() throws Exception {
        VitamConfiguration.setTenants(new ArrayList<>());

        reconstructionService.reconstruct(FunctionalAdminCollections.RULES);

        // the reconstruction is never done when no Vitam tenant.
        verify(reconstructionService, never()).reconstruct(any(), any());
    }

    /**
     * Prepare backup collections for tests.
     *
     * @return
     * @throws Exception
     */
    private Optional<CollectionBackupModel> getBackupCollection(Integer tenant) throws Exception {
        XContentBuilder builderDocument = jsonBuilder()
            .startObject()
            .field(VitamDocument.ID, GUIDFactory.newGUID().toString())
            .field(VitamDocument.TENANT_ID, tenant)
            .field("Title", "fake title A")
            .endObject();

        XContentBuilder builderDocument2 = jsonBuilder()
            .startObject()
            .field(VitamDocument.ID, GUIDFactory.newGUID().toString())
            .field(VitamDocument.TENANT_ID, tenant)
            .field("Title", "fake title B")
            .endObject();

        XContentBuilder builderSequence = jsonBuilder()
            .startObject()
            .field(VitamDocument.ID, GUIDFactory.newGUID().toString())
            .field(VitamDocument.TENANT_ID, tenant)
            .field("Name", SEQUENCE_NAME)
            .field("Counter", 3)
            .endObject();

        XContentBuilder builderBackupSequence = jsonBuilder()
            .startObject()
            .field(VitamDocument.ID, GUIDFactory.newGUID().toString())
            .field(VitamDocument.TENANT_ID, tenant)
            .field("Name", BACKUP_SEQUENCE_NAME)
            .field("Counter", 17)
            .endObject();

        // create collection of documents.
        Document document = Document.parse(Strings.toString(builderDocument));
        Document document2 = Document.parse(Strings.toString(builderDocument2));
        List<Document> documents = Arrays.asList(document, document2);

        // create sequence document.
        Document document3 = Document.parse(Strings.toString(builderSequence));
        VitamSequence vitamSequence = new VitamSequence(document3);

        // create sequence document.
        Document document4 = Document.parse(Strings.toString(builderBackupSequence));
        VitamSequence vitamBackupSequence = new VitamSequence(document4);

        // create collection backup.
        CollectionBackupModel backupCollection = new CollectionBackupModel();
        backupCollection.setDocuments(documents);
        backupCollection.setSequence(vitamSequence);
        backupCollection.setBackupSequence(vitamBackupSequence);

        return Optional.of(backupCollection);
    }
}

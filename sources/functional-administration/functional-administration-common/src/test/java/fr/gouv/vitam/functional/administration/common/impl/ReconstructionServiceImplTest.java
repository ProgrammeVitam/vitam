/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 *******************************************************************************/
package fr.gouv.vitam.functional.administration.common.impl;

import fr.gouv.vitam.common.database.api.impl.VitamElasticsearchRepository;
import fr.gouv.vitam.common.database.api.impl.VitamMongoRepository;

import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.functional.administration.common.CollectionBackupModel;
import fr.gouv.vitam.functional.administration.common.ReconstructionFactory;
import fr.gouv.vitam.functional.administration.common.VitamSequence;
import fr.gouv.vitam.functional.administration.common.api.RestoreBackupService;
import fr.gouv.vitam.functional.administration.common.server.AdminManagementConfiguration;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import org.bson.Document;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

/**
 * Test the reconstruction services.
 */
@RunWith(MockitoJUnitRunner.class)
public class ReconstructionServiceImplTest {

    /**
     * Vitam Logger.
     */
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ReconstructionServiceImplTest.class);

    private static final String STRATEGY_ID = "default";
    private static final Integer TENANT_ID_0 = 0;
    private static final Integer TENANT_ID_1 = 1;
    private static final Integer TENANT_ID_2 = 2;

    private static final String ES_BULK_EXCEPTION_MESSAGE = "ElasticSearch: Bulk Request failure.";
    private static final String MONGODB_BULK_EXCEPTION_MESSAGE = "MongoDB: Bulk Request failure.";

    @Mock
    private VitamMongoRepository mongoRepository;

    @Mock
    private VitamElasticsearchRepository elasticsearchRepository;

    @Mock
    private RestoreBackupService recoverBuckupService;

    @Mock
    private ReconstructionFactory repositoryFactory;

    @Mock
    private AdminManagementConfiguration configuration;

    @Captor
    private ArgumentCaptor<Integer> tenantCaptor;

    @Captor
    private ArgumentCaptor<FunctionalAdminCollections> collCaptor;

    @InjectMocks
    @Spy
    private ReconstructionServiceImpl reconstructionService;

    @Before
    public void setup() {
        when(repositoryFactory.getVitamESRepository(FunctionalAdminCollections.RULES))
            .thenReturn(elasticsearchRepository);
        when(repositoryFactory.getVitamMongoRepository(FunctionalAdminCollections.RULES))
            .thenReturn(mongoRepository);
    }

    @Test
    public void reconstructCollectionByTenantOK() throws Exception {

        // mock the recoverBackupCopy service.
        Optional<CollectionBackupModel> backupCollection = getBackupCollection();
        when(recoverBuckupService.readLatestSavedFile(STRATEGY_ID, FunctionalAdminCollections.RULES, TENANT_ID_0))
            .thenReturn(backupCollection);

        // call the reconstruction service.
        LOGGER.debug(String.format("Reconstruction of Vitam collection by tenant %s.", TENANT_ID_0));

        reconstructionService.reconstruct(FunctionalAdminCollections.RULES, TENANT_ID_0);

        verify(mongoRepository, times(1)).purge(tenantCaptor.capture());
        verify(elasticsearchRepository, times(1)).purge(tenantCaptor.capture());
        Assert.assertEquals(TENANT_ID_0, tenantCaptor.getValue());

        verify(recoverBuckupService)
            .readLatestSavedFile(STRATEGY_ID, FunctionalAdminCollections.RULES, TENANT_ID_0);

        verify(mongoRepository, atMost(1))
            .save(backupCollection.get().getCollections());
        verify(elasticsearchRepository, atMost(1))
            .save(backupCollection.get().getCollections());
    }

    @Test
    public void reconstructCollectionByTenantKO_NoBackupCopy() throws Exception {

        // mock the recoverBackupCopy service.
        when(recoverBuckupService.readLatestSavedFile(STRATEGY_ID, FunctionalAdminCollections.RULES, TENANT_ID_0))
            .thenReturn(Optional.empty());

        // call the reconstruction service.
        LOGGER.debug(String.format("Reconstruction of Vitam collection by tenant %s.", TENANT_ID_0));
        reconstructionService.reconstruct(FunctionalAdminCollections.RULES, TENANT_ID_0);

        verify(mongoRepository).purge(TENANT_ID_0);
        verify(elasticsearchRepository).purge(TENANT_ID_0);
        verify(recoverBuckupService, times(1))
            .readLatestSavedFile(STRATEGY_ID, FunctionalAdminCollections.RULES, TENANT_ID_0);

        // No call of the saving services when no backup copy found..
        verify(mongoRepository, never())
            .save(getBackupCollection().get().getCollections());
        verify(elasticsearchRepository, never())
            .save(getBackupCollection().get().getCollections());
    }

    @Test
    public void reconstructCollectionByTenantESKO() throws Exception {

        // mock thrown database Exception by elasticSearch.
        when(elasticsearchRepository.purge(TENANT_ID_0))
            .thenThrow(new DatabaseException(ES_BULK_EXCEPTION_MESSAGE));

        // verify type and message of the thrown elasticSearch Exception.
        assertThatThrownBy(() -> reconstructionService.reconstruct(FunctionalAdminCollections.RULES, TENANT_ID_0))
            .isInstanceOf(DatabaseException.class)
            .hasMessageContaining(ES_BULK_EXCEPTION_MESSAGE);
    }

    @Test
    public void reconstructCollectionByTenantMongoKO() throws Exception {

        // mock thrown database Exception by mongoDB.
        when(mongoRepository.purge(TENANT_ID_0))
            .thenThrow(new DatabaseException(MONGODB_BULK_EXCEPTION_MESSAGE));

        // verify type and message of the thrown mongoDB Exception.
        assertThatThrownBy(() -> reconstructionService.reconstruct(FunctionalAdminCollections.RULES, TENANT_ID_0))
            .isInstanceOf(DatabaseException.class)
            .hasMessageContaining(MONGODB_BULK_EXCEPTION_MESSAGE);
    }

    @Test
    public void reconstructCollectiontOnAllVitamTenantsOK() throws Exception {

        // mock the recoverBackupCopy service.
        Optional<CollectionBackupModel> backupCollection = getBackupCollection();
        when(recoverBuckupService.readLatestSavedFile(STRATEGY_ID, FunctionalAdminCollections.RULES, TENANT_ID_0))
            .thenReturn(backupCollection);
        when(recoverBuckupService.readLatestSavedFile(STRATEGY_ID, FunctionalAdminCollections.RULES, TENANT_ID_1))
            .thenReturn(backupCollection);
        when(recoverBuckupService.readLatestSavedFile(STRATEGY_ID, FunctionalAdminCollections.RULES, TENANT_ID_2))
            .thenReturn(backupCollection);

        // testing the reconstruction service.
        LOGGER.debug(String.format("Reconstruction of Vitam tenants."));

        // mock adminManagement configuration.
        when(configuration.getTenants())
            .thenReturn(Arrays.asList(0, 1, 2));

        // Call the construction service of the all tenants.
        reconstructionService.reconstruct(FunctionalAdminCollections.RULES);

        // for the "3" tenants, the reconstruction service is called at least once.
        verify(reconstructionService, atLeastOnce())
            .reconstruct(collCaptor.capture(), tenantCaptor.capture());

        // for the "3" tenants, the reconstruction service is called at most 3 times.
        verify(reconstructionService, atMost(3))
            .reconstruct(collCaptor.capture(), tenantCaptor.capture());
    }


    @Test
    public void reconstructCollectiontOnAllVitamTenantsKO() throws Exception {

        // mock adminManagement configuration with empty list of Vitam tenants.
        when(configuration.getTenants())
            .thenReturn(new ArrayList<>());

        // the reconstruction is never done when no Vitam tenant.
        verify(reconstructionService, never())
            .reconstruct(collCaptor.capture(), tenantCaptor.capture());
    }

    /**
     * Prepare backup collections for tests.
     *
     * @return
     * @throws Exception
     */
    private Optional<CollectionBackupModel> getBackupCollection() throws Exception {
        XContentBuilder builderDocument = jsonBuilder()
            .startObject()
            .field(VitamDocument.ID, GUIDFactory.newGUID().toString())
            .field(VitamDocument.TENANT_ID, TENANT_ID_0)
            .field("Title", "fake title A")
            .endObject();

        XContentBuilder builderDocument2 = jsonBuilder()
            .startObject()
            .field(VitamDocument.ID, GUIDFactory.newGUID().toString())
            .field(VitamDocument.TENANT_ID, TENANT_ID_0)
            .field("Title", "fake title B")
            .endObject();

        XContentBuilder builderSequence = jsonBuilder()
            .startObject()
            .field(VitamDocument.ID, GUIDFactory.newGUID().toString())
            .field(VitamDocument.TENANT_ID, TENANT_ID_0)
            .field("Name", "fake name")
            .field("Counter", 3)
            .endObject();

        // create collection of documents.
        Document document = Document.parse(builderDocument.string());
        Document document2 = Document.parse(builderDocument2.string());
        List<Document> documents = Arrays.asList(document, document2);

        // create sequence document.
        Document document3 = Document.parse(builderSequence.string());
        VitamSequence vitamSequence = new VitamSequence(document3);

        // create collection backup.
        CollectionBackupModel backupCollection = new CollectionBackupModel();
        backupCollection.setCollections(documents);
        backupCollection.setSequence(vitamSequence);

        return Optional.of(backupCollection);
    }

}

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
package fr.gouv.vitam.functional.administration.common;

import com.mongodb.client.MongoCollection;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.functional.administration.common.counter.VitamCounterService;
import fr.gouv.vitam.functional.administration.common.exception.BackupServiceException;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.storage.engine.common.model.StorageCollectionType;
import org.bson.Document;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static com.google.common.collect.Lists.newArrayList;
import static fr.gouv.vitam.common.database.collections.VitamCollection.getMongoClientOptions;

import static fr.gouv.vitam.common.guid.GUIDFactory.newEventGUID;
import static fr.gouv.vitam.common.guid.GUIDFactory.newGUID;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Matchers.any;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;



public class FunctionalBackupServiceTest {

    private final static String CLUSTER_NAME = "vitam-cluster";

    private String FUNTIONAL_COLLECTION = "AGENCIES";
    @Rule
    public MongoRule mongoRule =
        new MongoRule(getMongoClientOptions(newArrayList(Agencies.class)), CLUSTER_NAME, FUNTIONAL_COLLECTION);

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private BackupService backupService;

    @Mock
    private VitamCounterService vitamCounterService;

    @Mock
    private BackupLogbookManager backupLogbookManager;

    @InjectMocks
    private FunctionalBackupService functionalBackupService;

    private MongoCollection<Document> functionalCollection;

    @Before
    public void setUp() throws Exception {
        FunctionalAdminCollections.AGENCIES.getVitamCollection()
            .initialize(mongoRule.getMongoClient().getDatabase(CLUSTER_NAME), false);
        functionalCollection = FunctionalAdminCollections.AGENCIES.getCollection();
        functionalCollection
            .insertOne(new Document().append("_tenant", 0).append("Name", "A").append("Identifier", "ID-008")
                .append("_id", newGUID().getId()));
        functionalCollection
            .insertOne(new Document().append("_tenant", 1).append("Name", "B").append("Identifier", "ID-s08")
                .append("_id", newGUID().getId()));
        VitamSequence vitamSequence = new VitamSequence();
        vitamSequence.append("Counter", "0").append("_id", "iidd").append("Name", "A").append("_tenant", "0");
        given(vitamCounterService.getSequenceDocument(any(), any()))
            .willReturn(vitamSequence);

    }

    @Test
    public void should_save_collection_and_sequence() throws Exception {
        //Given
        final FunctionalAdminCollections agencies = FunctionalAdminCollections.AGENCIES;

        // When
        GUID guid = newEventGUID(0);
        functionalBackupService.saveCollectionAndSequence(guid, "STP_TEST",
            StorageCollectionType.AGENCIES, agencies, 0);
        functionalBackupService.saveCollectionAndSequence(guid, "STP_TEST",
            StorageCollectionType.AGENCIES, agencies, 0);
        //Then
        verify(backupLogbookManager, times(2))
            .logEventSuccess(eq(guid), eq("STP_TEST"), any(), eq("0_agencies_0.json"));
    }

    @Test
    public void should_fail_when_saving_collection() throws Exception {
        //Given
        final FunctionalAdminCollections agencies = FunctionalAdminCollections.AGENCIES;
        GUID guid = newEventGUID(0);
        willThrow(new BackupServiceException("Error Message")).given(backupService).backup(any(), any(), any());

        // When
        functionalBackupService.saveCollectionAndSequence(guid, "STP_TEST",
            StorageCollectionType.AGENCIES, agencies, 0);
        //Then
        verify(backupLogbookManager).logError(guid, "STP_TEST", "Error Message");
    }
}

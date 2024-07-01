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
package fr.gouv.vitam.storage.offers.tape.cas;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.collection.CloseableIterator;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.database.server.mongodb.SimpleMongoDBAccess;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.time.LogicalClockRule;
import fr.gouv.vitam.storage.engine.common.collection.OfferCollections;
import fr.gouv.vitam.storage.engine.common.model.TapeAccessRequestReferentialEntity;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryObjectReferentialId;
import fr.gouv.vitam.storage.offers.tape.exception.AccessRequestReferentialException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;

public class AccessRequestReferentialRepositoryTest {

    private static final String ACCESS_REQUEST_REFERENTIAL_COLLECTION =
        OfferCollections.ACCESS_REQUEST_REFERENTIAL.getName() + GUIDFactory.newGUID().getId();
    private static final int BULK_SIZE = 10;

    @ClassRule
    public static MongoRule mongoRule = new MongoRule(
        MongoDbAccess.getMongoClientSettingsBuilder(),
        ACCESS_REQUEST_REFERENTIAL_COLLECTION
    );

    @Rule
    public LogicalClockRule logicalClock = new LogicalClockRule();

    private MongoDbAccess mongoDbAccess;
    private AccessRequestReferentialRepository accessRequestReferentialRepository;

    @Before
    public void before() {
        mongoDbAccess = new SimpleMongoDBAccess(mongoRule.getMongoClient(), MongoRule.VITAM_DB);
        accessRequestReferentialRepository = new AccessRequestReferentialRepository(
            mongoDbAccess.getMongoDatabase().getCollection(ACCESS_REQUEST_REFERENTIAL_COLLECTION),
            BULK_SIZE
        );
    }

    @After
    public void after() {
        mongoRule.handleAfter();
    }

    @AfterClass
    public static void setDownAfterClass() {
        mongoRule.handleAfterClass();
    }

    @Test
    public void whenInsertNewAccessRequestThenOK() throws AccessRequestReferentialException {
        // Given
        String requestId = GUIDFactory.newGUID().getId();
        String creationDate = nextDate();
        String readyDate = nextDate();
        String expirationDate = nextDate();
        String purgeDate = nextDate();
        TapeAccessRequestReferentialEntity accessRequestReferentialEntity = new TapeAccessRequestReferentialEntity(
            requestId,
            "myContainer",
            List.of("obj1", "obj2"),
            creationDate,
            readyDate,
            expirationDate,
            purgeDate,
            List.of("tarId1", "tarId3"),
            2,
            9
        );

        // When
        accessRequestReferentialRepository.insert(accessRequestReferentialEntity);

        // Then
        Optional<TapeAccessRequestReferentialEntity> foundAccessRequest =
            accessRequestReferentialRepository.findByRequestId(requestId);
        assertThat(foundAccessRequest).isPresent();
        assertThat(foundAccessRequest.get().getRequestId()).isEqualTo(requestId);
        assertThat(foundAccessRequest.get().getObjectNames()).containsExactlyInAnyOrder("obj1", "obj2");
        assertThat(foundAccessRequest.get().getContainerName()).isEqualTo("myContainer");
        assertThat(foundAccessRequest.get().getRequestId()).isEqualTo(requestId);
        assertThat(foundAccessRequest.get().getCreationDate()).isEqualTo(creationDate);
        assertThat(foundAccessRequest.get().getReadyDate()).isEqualTo(readyDate);
        assertThat(foundAccessRequest.get().getExpirationDate()).isEqualTo(expirationDate);
        assertThat(foundAccessRequest.get().getPurgeDate()).isEqualTo(purgeDate);
        assertThat(foundAccessRequest.get().getUnavailableArchiveIds()).containsExactlyInAnyOrder("tarId1", "tarId3");
        assertThat(foundAccessRequest.get().getTenant()).isEqualTo(2);
        assertThat(foundAccessRequest.get().getVersion()).isEqualTo(9);
    }

    @Test
    public void givenExistingAccessRequestWhenFindByRequestIdThenOK() throws AccessRequestReferentialException {
        // Given
        TapeAccessRequestReferentialEntity accessRequest1 = createAccessRequest("myContainer1", "obj1", "tarId1");
        TapeAccessRequestReferentialEntity accessRequest2 = createAccessRequest("myContainer2", "obj2", "tarId2");

        accessRequestReferentialRepository.insert(accessRequest1);
        accessRequestReferentialRepository.insert(accessRequest2);

        // When
        Optional<TapeAccessRequestReferentialEntity> foundAccessRequest =
            accessRequestReferentialRepository.findByRequestId(accessRequest1.getRequestId());

        // Then
        assertThat(foundAccessRequest).isPresent();
        assertThat(foundAccessRequest.get().getRequestId()).isEqualTo(accessRequest1.getRequestId());
    }

    @Test
    public void givenNonExistingAccessRequestWhenFindByRequestIdThenNotFound()
        throws AccessRequestReferentialException {
        // Given
        TapeAccessRequestReferentialEntity accessRequest1 = createAccessRequest("myContainer1", "obj1", "tarId1");
        accessRequestReferentialRepository.insert(accessRequest1);

        // When
        Optional<TapeAccessRequestReferentialEntity> foundAccessRequest =
            accessRequestReferentialRepository.findByRequestId("unknown");

        // Then
        assertThat(foundAccessRequest).isEmpty();
    }

    @Test
    public void givenExistingAccessRequestsWhenFindByRequestIdsThenOK() throws AccessRequestReferentialException {
        // Given
        List<TapeAccessRequestReferentialEntity> accessRequests = IntStream.range(0, 6)
            .mapToObj(i -> createAccessRequest("myContainer1", "obj1", "tarId1"))
            .collect(Collectors.toList());
        for (TapeAccessRequestReferentialEntity accessRequest : accessRequests) {
            accessRequestReferentialRepository.insert(accessRequest);
        }

        // When
        List<TapeAccessRequestReferentialEntity> foundAccessRequests =
            accessRequestReferentialRepository.findByRequestIds(
                Set.of(accessRequests.get(2).getRequestId(), accessRequests.get(5).getRequestId(), "unknown")
            );

        // Then
        assertThat(foundAccessRequests).hasSize(2);
        assertThat(foundAccessRequests)
            .extracting(TapeAccessRequestReferentialEntity::getRequestId)
            .containsExactlyInAnyOrder(accessRequests.get(2).getRequestId(), accessRequests.get(5).getRequestId());
    }

    @Test
    public void whenFindByEmptyRequestIdSetThenOK() throws AccessRequestReferentialException {
        // Given

        // When
        List<TapeAccessRequestReferentialEntity> foundAccessRequests =
            accessRequestReferentialRepository.findByRequestIds(Set.of());

        // Then
        assertThat(foundAccessRequests).isEmpty();
    }

    @Test
    public void givenLargeAccessRequestDataSetWhenFindByRequestIdsThenOK() throws AccessRequestReferentialException {
        // Given
        List<TapeAccessRequestReferentialEntity> accessRequests = IntStream.range(0, 21)
            .mapToObj(i -> createAccessRequest("myContainer1", "obj1", "tarId1"))
            .collect(Collectors.toList());
        for (TapeAccessRequestReferentialEntity accessRequest : accessRequests) {
            accessRequestReferentialRepository.insert(accessRequest);
        }

        // When
        Set<String> requestIds = accessRequests
            .stream()
            .map(TapeAccessRequestReferentialEntity::getRequestId)
            .collect(Collectors.toSet());
        List<TapeAccessRequestReferentialEntity> foundAccessRequests =
            accessRequestReferentialRepository.findByRequestIds(requestIds);

        // Then
        assertThat(requestIds.size()).isGreaterThan(BULK_SIZE);
        assertThat(foundAccessRequests).hasSize(requestIds.size());
        assertThat(foundAccessRequests)
            .extracting(TapeAccessRequestReferentialEntity::getRequestId)
            .containsExactlyInAnyOrderElementsOf(requestIds);
    }

    @Test
    public void givenReadyAndNonReadyAccessRequestsWhenFindNonReadyAccessRequestsThenOK()
        throws AccessRequestReferentialException {
        // Given
        TapeAccessRequestReferentialEntity accessRequest1 = new TapeAccessRequestReferentialEntity(
            GUIDFactory.newGUID().getId(),
            "myContainer1",
            List.of("obj1", "obj2"),
            nextDate(),
            null,
            null,
            null,
            List.of("tarId1", "tarId2"),
            0,
            0
        );
        accessRequestReferentialRepository.insert(accessRequest1);

        TapeAccessRequestReferentialEntity accessRequest2 = new TapeAccessRequestReferentialEntity(
            GUIDFactory.newGUID().getId(),
            "myContainer1",
            List.of("obj3", "obj4"),
            nextDate(),
            nextDate(),
            nextDate(),
            nextDate(),
            List.of(),
            0,
            1
        );
        accessRequestReferentialRepository.insert(accessRequest2);

        // When
        List<TapeAccessRequestReferentialEntity> nonReadyAccessRequests =
            accessRequestReferentialRepository.findNonReadyAccessRequests();

        // Then
        assertThat(nonReadyAccessRequests).hasSize(1);
        assertThat(nonReadyAccessRequests)
            .extracting(TapeAccessRequestReferentialEntity::getRequestId)
            .containsExactlyInAnyOrder(accessRequest1.getRequestId());
    }

    @Test
    public void givenAccessRequestWhenFindByUnavailableArchiveIdThenMatchingArchiveIdsReturned()
        throws AccessRequestReferentialException {
        // Given
        TapeAccessRequestReferentialEntity accessRequest1 = new TapeAccessRequestReferentialEntity(
            GUIDFactory.newGUID().getId(),
            "myContainer1",
            List.of("obj1", "obj2"),
            nextDate(),
            null,
            null,
            null,
            List.of("tarId1", "tarId2"),
            0,
            0
        );
        accessRequestReferentialRepository.insert(accessRequest1);

        TapeAccessRequestReferentialEntity accessRequest2 = new TapeAccessRequestReferentialEntity(
            GUIDFactory.newGUID().getId(),
            "myContainer2",
            List.of("obj1", "obj4"),
            nextDate(),
            null,
            null,
            null,
            List.of("tarId1", "tarId4"),
            0,
            0
        );
        accessRequestReferentialRepository.insert(accessRequest2);

        TapeAccessRequestReferentialEntity accessRequest3 = new TapeAccessRequestReferentialEntity(
            GUIDFactory.newGUID().getId(),
            "myContainer1",
            List.of("obj3"),
            nextDate(),
            null,
            null,
            null,
            List.of("tarId2"),
            0,
            0
        );
        accessRequestReferentialRepository.insert(accessRequest3);

        TapeAccessRequestReferentialEntity accessRequest4 = new TapeAccessRequestReferentialEntity(
            GUIDFactory.newGUID().getId(),
            "myContainer1",
            List.of("obj5"),
            nextDate(),
            null,
            null,
            null,
            List.of("tarId3"),
            0,
            0
        );
        accessRequestReferentialRepository.insert(accessRequest4);

        // When
        List<TapeAccessRequestReferentialEntity> nonReadyAccessRequests =
            accessRequestReferentialRepository.findByUnavailableArchiveId("tarId2");

        // Then
        assertThat(nonReadyAccessRequests).hasSize(2);
        assertThat(nonReadyAccessRequests)
            .extracting(TapeAccessRequestReferentialEntity::getRequestId)
            .containsExactlyInAnyOrder(accessRequest1.getRequestId(), accessRequest3.getRequestId());
    }

    @Test
    public void givenExistingAccessRequestWhenDeleteExistingThenOK() throws AccessRequestReferentialException {
        // Given
        TapeAccessRequestReferentialEntity accessRequest1 = createAccessRequest("myContainer1", "obj1", "tarId1");
        TapeAccessRequestReferentialEntity accessRequest2 = createAccessRequest("myContainer2", "obj2", "tarId2");

        accessRequestReferentialRepository.insert(accessRequest1);
        accessRequestReferentialRepository.insert(accessRequest2);

        // When
        boolean deletedAccessRequest = accessRequestReferentialRepository.deleteAccessRequestById(
            accessRequest1.getRequestId()
        );

        // Then
        assertThat(deletedAccessRequest).isTrue();
        assertThat(accessRequestReferentialRepository.findByRequestId(accessRequest1.getRequestId())).isEmpty();
        assertThat(accessRequestReferentialRepository.findByRequestId(accessRequest2.getRequestId())).isPresent();
    }

    @Test
    public void givenNonExistingAccessRequestWhenDeleteExistingThenIgnored() throws AccessRequestReferentialException {
        // Given
        TapeAccessRequestReferentialEntity accessRequest1 = createAccessRequest("myContainer1", "obj1", "tarId1");
        accessRequestReferentialRepository.insert(accessRequest1);

        // When
        boolean deletedAccessRequest = accessRequestReferentialRepository.deleteAccessRequestById("unknown");

        // Then
        assertThat(deletedAccessRequest).isFalse();
        assertThat(accessRequestReferentialRepository.findByRequestId(accessRequest1.getRequestId())).isPresent();
    }

    @Test
    public void givenExpiredAndNonExpiredAccessRequestsWhenDeleteExpiredThenOnlyNonExpiredRemains()
        throws AccessRequestReferentialException {
        // Given :
        // - AccessRequest1 : Not ready (does not expire)
        // - AccessRequest2 : Ready, not expired, not deletable
        // - AccessRequest3 : Expired, not deletable
        // - AccessRequest4 : Deletable

        TapeAccessRequestReferentialEntity accessRequest1 = new TapeAccessRequestReferentialEntity(
            GUIDFactory.newGUID().getId(),
            "myContainer1",
            List.of("obj1", "obj2"),
            getNowMinusMinutes(10),
            null,
            null,
            null,
            List.of("tarId1", "tarId2"),
            0,
            0
        );
        accessRequestReferentialRepository.insert(accessRequest1);

        TapeAccessRequestReferentialEntity accessRequest2 = new TapeAccessRequestReferentialEntity(
            GUIDFactory.newGUID().getId(),
            "myContainer1",
            List.of("obj3", "obj4"),
            getNowMinusMinutes(30),
            getNowMinusMinutes(10),
            getNowPlusMinutes(10),
            getNowPlusMinutes(20),
            List.of(),
            0,
            1
        );
        accessRequestReferentialRepository.insert(accessRequest2);

        TapeAccessRequestReferentialEntity accessRequest3 = new TapeAccessRequestReferentialEntity(
            GUIDFactory.newGUID().getId(),
            "myContainer1",
            List.of("obj3", "obj4"),
            getNowMinusMinutes(30),
            getNowMinusMinutes(20),
            getNowMinusMinutes(10),
            getNowPlusMinutes(10),
            List.of(),
            0,
            2
        );
        accessRequestReferentialRepository.insert(accessRequest3);

        TapeAccessRequestReferentialEntity accessRequest4 = new TapeAccessRequestReferentialEntity(
            GUIDFactory.newGUID().getId(),
            "myContainer1",
            List.of("obj3", "obj4"),
            getNowMinusMinutes(40),
            getNowMinusMinutes(30),
            getNowMinusMinutes(20),
            getNowMinusMinutes(10),
            List.of(),
            0,
            2
        );
        accessRequestReferentialRepository.insert(accessRequest4);

        // When
        List<TapeAccessRequestReferentialEntity> deletedAccessRequests =
            accessRequestReferentialRepository.cleanupAndGetExpiredAccessRequests();

        // Then
        assertThat(deletedAccessRequests).hasSize(1);
        assertThat(deletedAccessRequests)
            .extracting(TapeAccessRequestReferentialEntity::getRequestId)
            .containsExactlyInAnyOrder(accessRequest4.getRequestId());
        assertThat(
            accessRequestReferentialRepository.findByRequestIds(
                Set.of(
                    accessRequest1.getRequestId(),
                    accessRequest2.getRequestId(),
                    accessRequest3.getRequestId(),
                    accessRequest4.getRequestId()
                )
            )
        )
            .extracting(TapeAccessRequestReferentialEntity::getRequestId)
            .containsExactlyInAnyOrder(
                accessRequest1.getRequestId(),
                accessRequest2.getRequestId(),
                accessRequest3.getRequestId()
            );
    }

    @Test
    public void givenExistingAccessRequestWhenUpdateThenOK() throws AccessRequestReferentialException {
        // Given
        String requestId1 = GUIDFactory.newGUID().getId();
        String req1CreationDate = nextDate();
        TapeAccessRequestReferentialEntity accessRequest1 = new TapeAccessRequestReferentialEntity(
            requestId1,
            "myContainer1",
            List.of("obj1"),
            req1CreationDate,
            null,
            null,
            null,
            List.of("tarId1", "tarId3", "tarId4"),
            0,
            6
        );
        accessRequestReferentialRepository.insert(accessRequest1);

        String requestId2 = GUIDFactory.newGUID().getId();
        String req2CreationDate = nextDate();
        TapeAccessRequestReferentialEntity accessRequest2 = new TapeAccessRequestReferentialEntity(
            requestId2,
            "myContainer2",
            List.of("obj2"),
            req2CreationDate,
            null,
            null,
            null,
            List.of("tarId2"),
            0,
            10
        );
        accessRequestReferentialRepository.insert(accessRequest2);

        // When

        String req1ReadyDate = nextDate();
        String req1ExpirationDate = nextDate();
        String req1PurgeDate = nextDate();
        TapeAccessRequestReferentialEntity newAccessRequest1 = new TapeAccessRequestReferentialEntity(
            requestId1,
            "myContainer1",
            List.of("obj1"),
            req1CreationDate,
            req1ReadyDate,
            req1ExpirationDate,
            req1PurgeDate,
            List.of(),
            0,
            7
        );

        boolean updated = accessRequestReferentialRepository.updateAccessRequest(newAccessRequest1, 6);

        // Then
        assertThat(updated).isTrue();
        Optional<TapeAccessRequestReferentialEntity> updatedAccessRequest1 =
            accessRequestReferentialRepository.findByRequestId(requestId1);
        assertThat(updatedAccessRequest1).isPresent();
        assertThat(updatedAccessRequest1.get().getRequestId()).isEqualTo(requestId1);
        assertThat(updatedAccessRequest1.get().getCreationDate()).isEqualTo(req1CreationDate);
        assertThat(updatedAccessRequest1.get().getReadyDate()).isEqualTo(req1ReadyDate);
        assertThat(updatedAccessRequest1.get().getExpirationDate()).isEqualTo(req1ExpirationDate);
        assertThat(updatedAccessRequest1.get().getPurgeDate()).isEqualTo(req1PurgeDate);
        assertThat(updatedAccessRequest1.get().getUnavailableArchiveIds()).isEmpty();
        assertThat(updatedAccessRequest1.get().getVersion()).isEqualTo(7);

        Optional<TapeAccessRequestReferentialEntity> updatedAccessRequest2 =
            accessRequestReferentialRepository.findByRequestId(requestId2);
        assertThat(updatedAccessRequest2).isPresent();
        assertThat(updatedAccessRequest2.get().getRequestId()).isEqualTo(requestId2);
        assertThat(updatedAccessRequest2.get().getCreationDate()).isEqualTo(req2CreationDate);
        assertThat(updatedAccessRequest2.get().getReadyDate()).isEqualTo(null);
        assertThat(updatedAccessRequest2.get().getExpirationDate()).isEqualTo(null);
        assertThat(updatedAccessRequest2.get().getPurgeDate()).isEqualTo(null);
        assertThat(updatedAccessRequest2.get().getUnavailableArchiveIds()).containsExactly("tarId2");
        assertThat(updatedAccessRequest2.get().getVersion()).isEqualTo(10);
    }

    @Test
    public void givenNonExistingAccessRequestWhenUpdateThenNoUpdate() throws AccessRequestReferentialException {
        // Given

        // When
        String requestId1 = GUIDFactory.newGUID().getId();
        TapeAccessRequestReferentialEntity newAccessRequest1 = new TapeAccessRequestReferentialEntity(
            requestId1,
            "myContainer1",
            List.of("obj1"),
            nextDate(),
            nextDate(),
            nextDate(),
            nextDate(),
            List.of(),
            0,
            7
        );

        boolean updated = accessRequestReferentialRepository.updateAccessRequest(newAccessRequest1, 6);

        // Then
        assertThat(updated).isFalse();
        Optional<TapeAccessRequestReferentialEntity> updatedAccessRequest1 =
            accessRequestReferentialRepository.findByRequestId(requestId1);
        assertThat(updatedAccessRequest1).isEmpty();
    }

    @Test
    public void givenExistingAccessRequestConcurrentlyUpdatedWhenUpdateThenNoUpdate()
        throws AccessRequestReferentialException {
        // Given
        String requestId1 = GUIDFactory.newGUID().getId();
        String req1Date = nextDate();
        TapeAccessRequestReferentialEntity accessRequest1 = new TapeAccessRequestReferentialEntity(
            requestId1,
            "myContainer1",
            List.of("obj1"),
            req1Date,
            null,
            null,
            null,
            List.of("tarId1", "tarId3", "tarId4"),
            0,
            6
        );
        accessRequestReferentialRepository.insert(accessRequest1);

        // When
        // Simulate another concurrent update by incrementing version
        mongoDbAccess
            .getMongoDatabase()
            .getCollection(ACCESS_REQUEST_REFERENTIAL_COLLECTION)
            .updateOne(
                Filters.eq(TapeAccessRequestReferentialEntity.ID, requestId1),
                Updates.set(TapeAccessRequestReferentialEntity.VERSION, 7)
            );

        TapeAccessRequestReferentialEntity newAccessRequest1 = new TapeAccessRequestReferentialEntity(
            requestId1,
            "myContainer1",
            List.of("obj1"),
            req1Date,
            null,
            null,
            null,
            List.of("tarId1", "tarId4"),
            0,
            7
        );

        boolean updated = accessRequestReferentialRepository.updateAccessRequest(newAccessRequest1, 6);

        // Then
        assertThat(updated).isFalse();
        Optional<TapeAccessRequestReferentialEntity> nonUpdatedAccessRequest1 =
            accessRequestReferentialRepository.findByRequestId(requestId1);
        assertThat(nonUpdatedAccessRequest1).isPresent();
        assertThat(nonUpdatedAccessRequest1.get().getUnavailableArchiveIds()).containsExactlyInAnyOrder(
            "tarId1",
            "tarId3",
            "tarId4"
        );
        assertThat(nonUpdatedAccessRequest1.get().getVersion()).isEqualTo(7);
    }

    @Test
    public void givenEmptyArchiveIdWhenExcludeArchiveIdsStillRequiredByAccessRequestsThenOK()
        throws AccessRequestReferentialException {
        // Given
        TapeAccessRequestReferentialEntity accessRequest1 = new TapeAccessRequestReferentialEntity(
            GUIDFactory.newGUID().getId(),
            "myContainer1",
            List.of("obj1"),
            nextDate(),
            null,
            null,
            null,
            List.of("tarId1", "tarId4"),
            0,
            0
        );
        accessRequestReferentialRepository.insert(accessRequest1);

        // When
        Set<String> unusedArchiveIds =
            accessRequestReferentialRepository.excludeArchiveIdsStillRequiredByAccessRequests(emptySet());

        // Then
        assertThat(unusedArchiveIds).isEmpty();
    }

    @Test
    public void givenReferencedAndNonReferencedArchiveIdsWhenExcludeArchiveIdsStillRequiredByAccessRequestsThenOK()
        throws AccessRequestReferentialException {
        // Given
        TapeAccessRequestReferentialEntity accessRequest1 = new TapeAccessRequestReferentialEntity(
            GUIDFactory.newGUID().getId(),
            "myContainer1",
            List.of("obj1"),
            nextDate(),
            null,
            null,
            null,
            List.of("tarId1", "tarId4"),
            0,
            0
        );
        accessRequestReferentialRepository.insert(accessRequest1);

        TapeAccessRequestReferentialEntity accessRequest2 = new TapeAccessRequestReferentialEntity(
            GUIDFactory.newGUID().getId(),
            "myContainer1",
            List.of("obj2", "obj3"),
            nextDate(),
            null,
            null,
            null,
            List.of("tarId1", "tarId2"),
            0,
            0
        );
        accessRequestReferentialRepository.insert(accessRequest2);

        TapeAccessRequestReferentialEntity accessRequest3 = new TapeAccessRequestReferentialEntity(
            GUIDFactory.newGUID().getId(),
            "myContainer3",
            List.of("obj4"),
            nextDate(),
            null,
            null,
            null,
            List.of("tarId6"),
            0,
            0
        );
        accessRequestReferentialRepository.insert(accessRequest3);

        // When
        Set<String> unusedArchiveIds =
            accessRequestReferentialRepository.excludeArchiveIdsStillRequiredByAccessRequests(
                Set.of("tarId1", "tarId2", "tarId3", "tarId4", "tarId5", "tarId6")
            );

        // Then
        assertThat(unusedArchiveIds).containsExactlyInAnyOrder("tarId3", "tarId5");
    }

    @Test
    public void givenActiveAndExpiredAccessRequestsWhenListObjectIdsForActiveAccessRequestsThenOK()
        throws AccessRequestReferentialException {
        // Given
        TapeAccessRequestReferentialEntity accessRequest1 = new TapeAccessRequestReferentialEntity(
            GUIDFactory.newGUID().getId(),
            "myContainer1",
            List.of("obj1", "obj2"),
            getNowMinusMinutes(10),
            null,
            null,
            null,
            List.of("tarId1", "tarId4"),
            0,
            0
        );
        accessRequestReferentialRepository.insert(accessRequest1);

        TapeAccessRequestReferentialEntity accessRequest2 = new TapeAccessRequestReferentialEntity(
            GUIDFactory.newGUID().getId(),
            "myContainer1",
            List.of("obj1", "obj3"),
            getNowMinusMinutes(20),
            getNowMinusMinutes(10),
            getNowPlusMinutes(20),
            getNowPlusMinutes(50),
            List.of(),
            0,
            0
        );
        accessRequestReferentialRepository.insert(accessRequest2);
        TapeAccessRequestReferentialEntity accessRequest3 = new TapeAccessRequestReferentialEntity(
            GUIDFactory.newGUID().getId(),
            "myContainer2",
            List.of("obj1", "obj4"),
            getNowMinusMinutes(20),
            getNowMinusMinutes(10),
            getNowPlusMinutes(20),
            getNowPlusMinutes(50),
            List.of("tarId2", "tarId3"),
            0,
            0
        );
        accessRequestReferentialRepository.insert(accessRequest3);
        TapeAccessRequestReferentialEntity accessRequest4 = new TapeAccessRequestReferentialEntity(
            GUIDFactory.newGUID().getId(),
            "myContainer3",
            List.of("obj5"),
            getNowMinusMinutes(50),
            getNowMinusMinutes(40),
            getNowMinusMinutes(10),
            getNowPlusMinutes(20),
            List.of(),
            0,
            0
        );
        accessRequestReferentialRepository.insert(accessRequest4);

        // When
        try (
            CloseableIterator<TapeLibraryObjectReferentialId> usedArchiveIds =
                accessRequestReferentialRepository.listObjectIdsForActiveAccessRequests()
        ) {
            // Then
            assertThat(usedArchiveIds).containsExactlyInAnyOrder(
                new TapeLibraryObjectReferentialId("myContainer1", "obj1"),
                new TapeLibraryObjectReferentialId("myContainer1", "obj2"),
                new TapeLibraryObjectReferentialId("myContainer1", "obj3"),
                new TapeLibraryObjectReferentialId("myContainer2", "obj1"),
                new TapeLibraryObjectReferentialId("myContainer2", "obj4")
            );
        }
    }

    @Test
    public void givenAccessRequestsWhenCountNonReadyAccessRequestsThenOK() throws AccessRequestReferentialException {
        // Given
        TapeAccessRequestReferentialEntity accessRequest1 = new TapeAccessRequestReferentialEntity(
            GUIDFactory.newGUID().getId(),
            "myContainer1",
            List.of("obj1"),
            nextDate(),
            null,
            null,
            null,
            List.of("tarId1", "tarId4"),
            0,
            0
        );
        accessRequestReferentialRepository.insert(accessRequest1);

        TapeAccessRequestReferentialEntity accessRequest2 = new TapeAccessRequestReferentialEntity(
            GUIDFactory.newGUID().getId(),
            "myContainer1",
            List.of("obj2", "obj3"),
            nextDate(),
            null,
            null,
            null,
            List.of("tarId1", "tarId2"),
            0,
            0
        );
        accessRequestReferentialRepository.insert(accessRequest2);

        TapeAccessRequestReferentialEntity accessRequest3 = new TapeAccessRequestReferentialEntity(
            GUIDFactory.newGUID().getId(),
            "myContainer3",
            List.of("obj4"),
            nextDate(),
            getNowPlusMinutes(10),
            getNowPlusMinutes(40),
            getNowPlusMinutes(60),
            emptyList(),
            0,
            0
        );
        accessRequestReferentialRepository.insert(accessRequest3);

        TapeAccessRequestReferentialEntity accessRequest4 = new TapeAccessRequestReferentialEntity(
            GUIDFactory.newGUID().getId(),
            "myContainer4",
            List.of("obj5"),
            getNowMinusMinutes(30),
            getNowMinusMinutes(10),
            getNowPlusMinutes(10),
            getNowPlusMinutes(40),
            emptyList(),
            0,
            0
        );
        accessRequestReferentialRepository.insert(accessRequest4);

        // When
        long nbNonReady = accessRequestReferentialRepository.countNonReadyAccessRequests();

        // Then
        assertThat(nbNonReady).isEqualTo(2L);
    }

    @Test
    public void givenAccessRequestsWhenCountReadyAccessRequestsThenOK() throws AccessRequestReferentialException {
        // Given
        TapeAccessRequestReferentialEntity accessRequest1 = new TapeAccessRequestReferentialEntity(
            GUIDFactory.newGUID().getId(),
            "myContainer1",
            List.of("obj1"),
            nextDate(),
            null,
            null,
            null,
            List.of("tarId1", "tarId4"),
            0,
            0
        );
        accessRequestReferentialRepository.insert(accessRequest1);

        TapeAccessRequestReferentialEntity accessRequest2 = new TapeAccessRequestReferentialEntity(
            GUIDFactory.newGUID().getId(),
            "myContainer1",
            List.of("obj2", "obj3"),
            nextDate(),
            getNowPlusMinutes(10),
            getNowPlusMinutes(40),
            getNowPlusMinutes(60),
            emptyList(),
            1,
            0
        );
        accessRequestReferentialRepository.insert(accessRequest2);

        TapeAccessRequestReferentialEntity accessRequest3 = new TapeAccessRequestReferentialEntity(
            GUIDFactory.newGUID().getId(),
            "myContainer3",
            List.of("obj4"),
            nextDate(),
            getNowPlusMinutes(10),
            getNowPlusMinutes(40),
            getNowPlusMinutes(60),
            emptyList(),
            0,
            0
        );
        accessRequestReferentialRepository.insert(accessRequest3);

        TapeAccessRequestReferentialEntity accessRequest4 = new TapeAccessRequestReferentialEntity(
            GUIDFactory.newGUID().getId(),
            "myContainer4",
            List.of("obj5"),
            getNowMinusMinutes(40),
            getNowMinusMinutes(20),
            getNowMinusMinutes(10),
            getNowPlusMinutes(40),
            emptyList(),
            0,
            0
        );
        accessRequestReferentialRepository.insert(accessRequest4);

        // When
        long nbReady = accessRequestReferentialRepository.countReadyAccessRequests();

        // Then
        assertThat(nbReady).isEqualTo(2L);
    }

    @Test
    public void givenAccessRequestsWhenExpiredAccessRequestsThenOK() throws AccessRequestReferentialException {
        // Given
        TapeAccessRequestReferentialEntity accessRequest1 = new TapeAccessRequestReferentialEntity(
            GUIDFactory.newGUID().getId(),
            "myContainer1",
            List.of("obj1"),
            nextDate(),
            null,
            null,
            null,
            List.of("tarId1", "tarId4"),
            0,
            0
        );
        accessRequestReferentialRepository.insert(accessRequest1);

        TapeAccessRequestReferentialEntity accessRequest2 = new TapeAccessRequestReferentialEntity(
            GUIDFactory.newGUID().getId(),
            "myContainer1",
            List.of("obj2", "obj3"),
            nextDate(),
            getNowPlusMinutes(10),
            getNowPlusMinutes(40),
            getNowPlusMinutes(60),
            emptyList(),
            1,
            0
        );
        accessRequestReferentialRepository.insert(accessRequest2);

        TapeAccessRequestReferentialEntity accessRequest3 = new TapeAccessRequestReferentialEntity(
            GUIDFactory.newGUID().getId(),
            "myContainer3",
            List.of("obj4"),
            nextDate(),
            getNowPlusMinutes(10),
            getNowPlusMinutes(40),
            getNowPlusMinutes(60),
            emptyList(),
            0,
            0
        );
        accessRequestReferentialRepository.insert(accessRequest3);

        TapeAccessRequestReferentialEntity accessRequest4 = new TapeAccessRequestReferentialEntity(
            GUIDFactory.newGUID().getId(),
            "myContainer4",
            List.of("obj5"),
            getNowMinusMinutes(40),
            getNowMinusMinutes(20),
            getNowMinusMinutes(10),
            getNowPlusMinutes(40),
            emptyList(),
            0,
            0
        );
        accessRequestReferentialRepository.insert(accessRequest4);

        // When
        long nbExpired = accessRequestReferentialRepository.countExpiredAccessRequests();

        // Then
        assertThat(nbExpired).isEqualTo(1L);
    }

    private TapeAccessRequestReferentialEntity createAccessRequest(String container, String objName, String tarId) {
        String requestId = GUIDFactory.newGUID().getId();
        String req4Date = nextDate();
        return new TapeAccessRequestReferentialEntity(
            requestId,
            container,
            List.of(objName),
            req4Date,
            null,
            null,
            null,
            List.of(tarId),
            0,
            0
        );
    }

    private String nextDate() {
        logicalClock.logicalSleep(1, ChronoUnit.SECONDS);
        return LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now());
    }

    private String getNowPlusMinutes(int plusMinutes) {
        return LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now().plusMinutes(plusMinutes));
    }

    private String getNowMinusMinutes(int minusMinutes) {
        return LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now().minusMinutes(minusMinutes));
    }
}

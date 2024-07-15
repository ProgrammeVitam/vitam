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

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.database.server.mongodb.SimpleMongoDBAccess;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.storage.tapelibrary.TapeLibraryBucketConfiguration;
import fr.gouv.vitam.common.storage.tapelibrary.TapeLibraryTopologyConfiguration;
import fr.gouv.vitam.common.time.LogicalClockRule;
import fr.gouv.vitam.storage.engine.common.collection.OfferCollections;
import fr.gouv.vitam.storage.engine.common.model.TapeAccessRequestReferentialEntity;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryInputFileObjectStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryObjectReferentialId;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryTarObjectStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeObjectReferentialEntity;
import fr.gouv.vitam.storage.engine.common.model.TarEntryDescription;
import fr.gouv.vitam.storage.offers.tape.cache.LRUCacheEvictionJudge;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class ArchiveCacheEvictionControllerTest {

    private static final String ACCESS_REQUEST_REFERENTIAL_COLLECTION =
        OfferCollections.ACCESS_REQUEST_REFERENTIAL.getName() + GUIDFactory.newGUID().getId();
    private static final String TAPE_OBJECT_REFERENTIAL_COLLECTION =
        OfferCollections.TAPE_OBJECT_REFERENTIAL.getName() + GUIDFactory.newGUID().getId();

    @ClassRule
    public static MongoRule mongoRule = new MongoRule(
        MongoDbAccess.getMongoClientSettingsBuilder(),
        ACCESS_REQUEST_REFERENTIAL_COLLECTION,
        TAPE_OBJECT_REFERENTIAL_COLLECTION
    );

    @Rule
    public LogicalClockRule logicalClock = new LogicalClockRule();

    private AccessRequestReferentialRepository accessRequestReferentialRepository;
    private ObjectReferentialRepository objectReferentialRepository;
    private ArchiveCacheEvictionController archiveCacheEvictionController;

    @BeforeClass
    public static void initializeClass() {
        VitamConfiguration.setTenants(Arrays.asList(0, 1, 2, 3));
        VitamConfiguration.setEnvironmentName(null);
    }

    @Before
    public void before() {
        MongoDbAccess mongoDbAccess = new SimpleMongoDBAccess(mongoRule.getMongoClient(), MongoRule.VITAM_DB);
        objectReferentialRepository = new ObjectReferentialRepository(
            mongoDbAccess.getMongoDatabase().getCollection(TAPE_OBJECT_REFERENTIAL_COLLECTION)
        );
        accessRequestReferentialRepository = new AccessRequestReferentialRepository(
            mongoDbAccess.getMongoDatabase().getCollection(ACCESS_REQUEST_REFERENTIAL_COLLECTION)
        );

        // Default configuration prevents "metadata" & "default" file buckets to be evicted from cache
        TapeLibraryTopologyConfiguration tapeLibraryTopologyConfiguration = new TapeLibraryTopologyConfiguration()
            .setBuckets(
                Map.of(
                    "test",
                    new TapeLibraryBucketConfiguration(List.of(0), 60),
                    "admin",
                    new TapeLibraryBucketConfiguration(List.of(1), 60),
                    "prod",
                    new TapeLibraryBucketConfiguration(List.of(2, 3), 60)
                )
            );
        BucketTopologyHelper bucketTopologyHelper = new BucketTopologyHelper(tapeLibraryTopologyConfiguration);
        archiveCacheEvictionController = new ArchiveCacheEvictionController(
            accessRequestReferentialRepository,
            objectReferentialRepository,
            bucketTopologyHelper
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
    public void givenMultipleAccessRequestsAndLocksWhenComputeEvictionJudgeThenOnlyNonLockedUnusedArchiveIdsFromExpirableFileBucketsCanBeEvicted()
        throws Exception {
        // Given
        logicalClock.freezeTime();

        // Non-ready access request
        accessRequestReferentialRepository.insert(
            new TapeAccessRequestReferentialEntity(
                "accessRequest1",
                "O_object",
                List.of("obj1", "obj2", "obj3"),
                getNowMinusMinutes(20),
                null,
                null,
                null,
                List.of("tarId2"),
                0,
                0
            )
        );

        // Ready access request
        accessRequestReferentialRepository.insert(
            new TapeAccessRequestReferentialEntity(
                "accessRequest2",
                "O_object",
                List.of("obj1", "obj4"),
                getNowMinusMinutes(20),
                getNowMinusMinutes(10),
                getNowPlusMinutes(20),
                getNowPlusMinutes(50),
                List.of(),
                0,
                0
            )
        );

        // Ready access request for same object name of another container
        accessRequestReferentialRepository.insert(
            new TapeAccessRequestReferentialEntity(
                "accessRequest3",
                "1_object",
                List.of("obj1", "obj3"),
                "creationDate",
                null,
                null,
                null,
                List.of("tarId3"),
                1,
                0
            )
        );

        // Expired access request
        accessRequestReferentialRepository.insert(
            new TapeAccessRequestReferentialEntity(
                "accessRequest4",
                "O_object",
                List.of("obj5"),
                getNowMinusMinutes(50),
                getNowMinusMinutes(40),
                getNowMinusMinutes(10),
                getNowPlusMinutes(20),
                List.of(),
                0,
                0
            )
        );

        // container1/obj1 : stored on 2 entries of tarId1
        objectReferentialRepository.insertOrUpdate(
            new TapeObjectReferentialEntity(
                new TapeLibraryObjectReferentialId("O_object", "obj1"),
                1234L,
                "SHA-512",
                "digest1",
                "obj1-guid1",
                new TapeLibraryTarObjectStorageLocation(
                    List.of(
                        new TarEntryDescription("tarId1", "obj1-guid1-0", 4321L, 1000L, "digest-obj1-guid1-0"),
                        new TarEntryDescription("tarId1", "obj1-guid1-1", 4321L, 321L, "digest-obj1-guid1-1")
                    )
                ),
                "creationDate",
                "updateDate"
            )
        );

        // container1/obj2 : stored on 1 entries of tarId1 & 1 entry of tarId2
        objectReferentialRepository.insertOrUpdate(
            new TapeObjectReferentialEntity(
                new TapeLibraryObjectReferentialId("O_object", "obj2"),
                1234L,
                "SHA-512",
                "digest2",
                "obj2-guid2",
                new TapeLibraryTarObjectStorageLocation(
                    List.of(
                        new TarEntryDescription("tarId1", "obj2-guid2-0", 5000L, 1000L, "digest-obj2-guid2-0"),
                        new TarEntryDescription("tarId2", "obj2-guid2-1", 0L, 321L, "digest-obj2-guid2-1")
                    )
                ),
                "creationDate",
                "updateDate"
            )
        );

        // container1/obj3 : not yet persisted on tar
        objectReferentialRepository.insertOrUpdate(
            new TapeObjectReferentialEntity(
                new TapeLibraryObjectReferentialId("O_object", "obj3"),
                1234L,
                "SHA-512",
                "digest3",
                "obj3-guid3",
                new TapeLibraryInputFileObjectStorageLocation(),
                "creationDate",
                "updateDate"
            )
        );

        // container2/obj1 : stored on 2 entries of tarId3
        objectReferentialRepository.insertOrUpdate(
            new TapeObjectReferentialEntity(
                new TapeLibraryObjectReferentialId("1_object", "obj1"),
                1234L,
                "SHA-512",
                "digest3",
                "obj1-guid1",
                new TapeLibraryTarObjectStorageLocation(
                    List.of(
                        new TarEntryDescription("tarId3", "obj1-guid1-0", 4321L, 1000L, "digest-obj1-guid1-0"),
                        new TarEntryDescription("tarId3", "obj1-guid1-1", 4321L, 321L, "digest-obj1-guid1-1")
                    )
                ),
                "creationDate",
                "updateDate"
            )
        );

        // container1/obj4 : stored on 1 entry of tarId4
        objectReferentialRepository.insertOrUpdate(
            new TapeObjectReferentialEntity(
                new TapeLibraryObjectReferentialId("O_object", "obj4"),
                200L,
                "SHA-512",
                "digest4",
                "obj4-guid4",
                new TapeLibraryTarObjectStorageLocation(
                    List.of(new TarEntryDescription("tarId4", "obj4-guid4-0", 4642L, 200L, "digest4"))
                ),
                "creationDate",
                "updateDate"
            )
        );

        // container1/obj5 : stored on 1 entry of tarId5 (only required by an expired access request)
        objectReferentialRepository.insertOrUpdate(
            new TapeObjectReferentialEntity(
                new TapeLibraryObjectReferentialId("O_object", "obj5"),
                555L,
                "SHA-512",
                "digest5",
                "obj5-guid5",
                new TapeLibraryTarObjectStorageLocation(
                    List.of(new TarEntryDescription("tarId5", "obj5-guid5-0", 0L, 555L, "digest5"))
                ),
                "creationDate",
                "updateDate"
            )
        );

        // container2/obj2 : stored on 1 entry of tarId6 (not required by any access request)
        objectReferentialRepository.insertOrUpdate(
            new TapeObjectReferentialEntity(
                new TapeLibraryObjectReferentialId("1_object", "obj2"),
                666L,
                "SHA-512",
                "digest2",
                "obj2-guid2",
                new TapeLibraryTarObjectStorageLocation(
                    List.of(new TarEntryDescription("tarId6", "obj2-guid2-0", 0L, 666L, "digest2"))
                ),
                "creationDate",
                "updateDate"
            )
        );

        // container2/obj3 : does not exist (deleted or never inserted)

        // tarId4 & tarId8 are locked then unlocked by lock1
        LockHandle lock1 = archiveCacheEvictionController.createLock(
            Set.of(new ArchiveCacheEntry("test-objects", "tarId4"), new ArchiveCacheEntry("test-objects", "tarId8"))
        );
        lock1.release();

        // tarId7 & tarId8 are locked by lock2
        LockHandle lock2 = archiveCacheEvictionController.createLock(
            Set.of(new ArchiveCacheEntry("test-objects", "tarId7"), new ArchiveCacheEntry("test-objects", "tarId8"))
        );

        // When
        LRUCacheEvictionJudge<ArchiveCacheEntry> evictionJudge = archiveCacheEvictionController.computeEvictionJudge();

        // Then
        // "metadata" file-bucket is always in cache
        assertThat(evictionJudge.canEvictEntry(new ArchiveCacheEntry("test-metadata", "any"))).isFalse();
        // "default" file-bucket is always in cache
        assertThat(evictionJudge.canEvictEntry(new ArchiveCacheEntry("test-default", "any"))).isFalse();

        // "default" file-bucket is always in cache
        assertThat(evictionJudge.canEvictEntry(new ArchiveCacheEntry("test-default", "any"))).isFalse();

        // tarId1 is required by O_object/obj1 & O_object/obj2, which are required by accessRequest1 and/or accessRequest2, and is not locked
        // tarId2 is required by O_object/obj2, which is required by accessRequest1, and is not locked
        // tarId3 is required by 1_object/obj1, which is required by accessRequest3, and is not locked
        // tarId4 is required by O_object/obj4, which is required by accessRequest2, and is not locked
        // tarId5 is required by O_object/obj5, which is only required by an expired access request accessRequest4, and no more locked by lock1
        // tarId6 is required by 1_object/obj2, which is not required by any access request, and is not locked
        // tarId7 is not required by any access request, but is locked by lock2
        // tarId8 is not required by any access request and is no more locked by lock1, but still locked by lock2
        // tarId9 is not required by any access request and is not locked

        assertThat(evictionJudge.canEvictEntry(new ArchiveCacheEntry("test-objects", "tarId1"))).isFalse();
        assertThat(evictionJudge.canEvictEntry(new ArchiveCacheEntry("test-objects", "tarId2"))).isFalse();
        assertThat(evictionJudge.canEvictEntry(new ArchiveCacheEntry("admin-objects", "tarId3"))).isFalse();
        assertThat(evictionJudge.canEvictEntry(new ArchiveCacheEntry("test-objects", "tarId4"))).isFalse();
        assertThat(evictionJudge.canEvictEntry(new ArchiveCacheEntry("test-objects", "tarId5"))).isTrue();
        assertThat(evictionJudge.canEvictEntry(new ArchiveCacheEntry("admin-objects", "tarId6"))).isTrue();
        assertThat(evictionJudge.canEvictEntry(new ArchiveCacheEntry("test-objects", "tarId7"))).isFalse();
        assertThat(evictionJudge.canEvictEntry(new ArchiveCacheEntry("test-objects", "tarId8"))).isFalse();
        assertThat(evictionJudge.canEvictEntry(new ArchiveCacheEntry("test-objects", "tarId9"))).isTrue();
    }

    private String getNowPlusMinutes(int plusMinutes) {
        return LocalDateUtil.getFormattedDateTimeForMongo(LocalDateUtil.now().plusMinutes(plusMinutes));
    }

    private String getNowMinusMinutes(int minusMinutes) {
        return LocalDateUtil.getFormattedDateTimeForMongo(LocalDateUtil.now().minusMinutes(minusMinutes));
    }
}

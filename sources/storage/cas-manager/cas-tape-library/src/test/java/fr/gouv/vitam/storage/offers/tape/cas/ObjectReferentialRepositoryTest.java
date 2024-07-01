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

import com.google.common.collect.ImmutableSet;
import fr.gouv.vitam.common.collection.CloseableIterator;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.database.server.mongodb.SimpleMongoDBAccess;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.storage.ObjectEntry;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.storage.engine.common.collection.OfferCollections;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryInputFileObjectStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryObjectReferentialId;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryTarObjectStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeObjectReferentialEntity;
import fr.gouv.vitam.storage.engine.common.model.TarEntryDescription;
import fr.gouv.vitam.storage.offers.tape.exception.ObjectReferentialException;
import org.apache.commons.collections4.IteratorUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

public class ObjectReferentialRepositoryTest {

    private static final String TAPE_OBJECT_REFERENTIAL_COLLECTION =
        OfferCollections.TAPE_OBJECT_REFERENTIAL.getName() + GUIDFactory.newGUID().getId();
    private static final int BULK_SIZE = 10;

    @ClassRule
    public static MongoRule mongoRule = new MongoRule(
        MongoDbAccess.getMongoClientSettingsBuilder(),
        TAPE_OBJECT_REFERENTIAL_COLLECTION
    );

    private static ObjectReferentialRepository objectReferentialRepository;

    @BeforeClass
    public static void setUpBeforeClass() {
        MongoDbAccess mongoDbAccess = new SimpleMongoDBAccess(mongoRule.getMongoClient(), MongoRule.VITAM_DB);
        objectReferentialRepository = new ObjectReferentialRepository(
            mongoDbAccess.getMongoDatabase().getCollection(TAPE_OBJECT_REFERENTIAL_COLLECTION),
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
    public void insertOrUpdateNewEntity() throws Exception {
        // Given
        TapeObjectReferentialEntity tapeObjectReferentialEntity1 = createObjectReferentialEntity();

        // When
        objectReferentialRepository.insertOrUpdate(tapeObjectReferentialEntity1);

        // Then
        Optional<TapeObjectReferentialEntity> tapeObjectReferentialEntity = objectReferentialRepository.find(
            "container",
            "objectName1"
        );

        assertThat(tapeObjectReferentialEntity.isPresent()).isTrue();
        assertThat(tapeObjectReferentialEntity.get().getId().getContainerName()).isEqualTo("container");
        assertThat(tapeObjectReferentialEntity.get().getId().getObjectName()).isEqualTo("objectName1");
        assertThat(tapeObjectReferentialEntity.get().getSize()).isEqualTo(10L);
        assertThat(tapeObjectReferentialEntity.get().getDigest()).isEqualTo("digest1");
        assertThat(tapeObjectReferentialEntity.get().getDigestType()).isEqualTo("SHA-512");
        assertThat(tapeObjectReferentialEntity.get().getLocation()).isInstanceOf(
            TapeLibraryInputFileObjectStorageLocation.class
        );
        assertThat(tapeObjectReferentialEntity.get().getLastObjectModifiedDate()).isEqualTo("date1");
        assertThat(tapeObjectReferentialEntity.get().getLastUpdateDate()).isEqualTo("date2");
    }

    @Test
    public void insertOrUpdateExistingEntity() throws Exception {
        // Given
        TapeObjectReferentialEntity tapeObjectReferentialEntity1 = createObjectReferentialEntity();

        TapeObjectReferentialEntity tapeObjectReferentialEntity1Version2 = JsonHandler.getFromString(
            JsonHandler.unprettyPrint(tapeObjectReferentialEntity1),
            TapeObjectReferentialEntity.class
        )
            .setSize(20L)
            .setDigest("digest2")
            .setStorageId("storageId2")
            .setLastObjectModifiedDate("date3")
            .setLastUpdateDate("date4");

        // When (insert than update)
        objectReferentialRepository.insertOrUpdate(tapeObjectReferentialEntity1);
        objectReferentialRepository.insertOrUpdate(tapeObjectReferentialEntity1Version2);

        // Then
        Optional<TapeObjectReferentialEntity> tapeObjectReferentialEntity = objectReferentialRepository.find(
            "container",
            "objectName1"
        );

        assertThat(tapeObjectReferentialEntity.isPresent()).isTrue();
        assertThat(tapeObjectReferentialEntity.get().getId().getContainerName()).isEqualTo("container");
        assertThat(tapeObjectReferentialEntity.get().getId().getObjectName()).isEqualTo("objectName1");
        assertThat(tapeObjectReferentialEntity.get().getSize()).isEqualTo(20L);
        assertThat(tapeObjectReferentialEntity.get().getDigest()).isEqualTo("digest2");
        assertThat(tapeObjectReferentialEntity.get().getDigestType()).isEqualTo("SHA-512");
        assertThat(tapeObjectReferentialEntity.get().getLocation()).isInstanceOf(
            TapeLibraryInputFileObjectStorageLocation.class
        );
        assertThat(tapeObjectReferentialEntity.get().getLastObjectModifiedDate()).isEqualTo("date3");
        assertThat(tapeObjectReferentialEntity.get().getLastUpdateDate()).isEqualTo("date4");
    }

    @Test
    public void findNonExisting() throws Exception {
        //Given

        // When
        Optional<TapeObjectReferentialEntity> tapeObjectReferentialEntity = objectReferentialRepository.find(
            "container",
            "objectName1"
        );

        // Then
        assertThat(tapeObjectReferentialEntity.isPresent()).isFalse();
    }

    @Test
    public void findExisting() throws Exception {
        // Given
        TapeObjectReferentialEntity tapeObjectReferentialEntity1 = createObjectReferentialEntity();
        objectReferentialRepository.insertOrUpdate(tapeObjectReferentialEntity1);

        // When
        Optional<TapeObjectReferentialEntity> tapeObjectReferentialEntity = objectReferentialRepository.find(
            "container",
            "objectName1"
        );

        // Then
        assertThat(tapeObjectReferentialEntity.isPresent()).isTrue();
    }

    @Test
    public void bulkFindEmptyList() throws ObjectReferentialException {
        // Given

        // When
        List<TapeObjectReferentialEntity> tapeObjectReferentialEntities = objectReferentialRepository.bulkFind(
            "container",
            emptySet()
        );

        // Then
        assertThat(tapeObjectReferentialEntities).isEmpty();
    }

    @Test
    public void bulkFindSmallDataSet() throws ObjectReferentialException {
        // Given
        int nbEntries = 6;
        for (int i = 0; i < nbEntries; i++) {
            objectReferentialRepository.insertOrUpdate(
                new TapeObjectReferentialEntity(
                    new TapeLibraryObjectReferentialId("container", "objectName" + i),
                    10L,
                    DigestType.SHA512.getName(),
                    "digest" + i,
                    "storageId" + i,
                    new TapeLibraryInputFileObjectStorageLocation(),
                    "date1-" + i,
                    "date2-" + i
                )
            );
        }

        // When
        List<TapeObjectReferentialEntity> tapeObjectReferentialEntities = objectReferentialRepository.bulkFind(
            "container",
            IntStream.range(0, 8).mapToObj(i -> "objectName" + i).collect(Collectors.toSet())
        );

        // Then
        assertThat(tapeObjectReferentialEntities.size()).isLessThan(BULK_SIZE);
        assertThat(tapeObjectReferentialEntities).hasSize(nbEntries);
        assertThat(tapeObjectReferentialEntities)
            .extracting(entity -> entity.getId().getObjectName(), TapeObjectReferentialEntity::getDigest)
            .containsExactlyInAnyOrderElementsOf(
                IntStream.range(0, nbEntries)
                    .mapToObj(i -> tuple("objectName" + i, "digest" + i))
                    .collect(Collectors.toList())
            );
    }

    @Test
    public void bulkFindLargeDataSet() throws ObjectReferentialException {
        // Given
        int nbEntries = 21;
        for (int i = 0; i < nbEntries; i++) {
            objectReferentialRepository.insertOrUpdate(
                new TapeObjectReferentialEntity(
                    new TapeLibraryObjectReferentialId("container", "objectName" + i),
                    10L,
                    DigestType.SHA512.getName(),
                    "digest" + i,
                    "storageId" + i,
                    new TapeLibraryInputFileObjectStorageLocation(),
                    "date1-" + i,
                    "date2-" + i
                )
            );
        }

        // When
        List<TapeObjectReferentialEntity> tapeObjectReferentialEntities = objectReferentialRepository.bulkFind(
            "container",
            IntStream.range(0, 25).mapToObj(i -> "objectName" + i).collect(Collectors.toSet())
        );

        // Then
        assertThat(tapeObjectReferentialEntities.size()).isGreaterThan(BULK_SIZE);
        assertThat(tapeObjectReferentialEntities).hasSize(nbEntries);
        assertThat(tapeObjectReferentialEntities)
            .extracting(entity -> entity.getId().getObjectName(), TapeObjectReferentialEntity::getDigest)
            .containsExactlyInAnyOrderElementsOf(
                IntStream.range(0, nbEntries)
                    .mapToObj(i -> tuple("objectName" + i, "digest" + i))
                    .collect(Collectors.toList())
            );
    }

    @Test
    public void bulkFindExistingPartial() throws ObjectReferentialException {
        // Given
        for (int i = 0; i < 5; i++) {
            objectReferentialRepository.insertOrUpdate(
                new TapeObjectReferentialEntity(
                    new TapeLibraryObjectReferentialId("container", "objectName" + i),
                    10L,
                    DigestType.SHA512.getName(),
                    "digest" + i,
                    "storageId" + i,
                    new TapeLibraryInputFileObjectStorageLocation(),
                    "date1-" + i,
                    "date2-" + i
                )
            );
        }

        // When
        List<TapeObjectReferentialEntity> tapeObjectReferentialEntities = objectReferentialRepository.bulkFind(
            "container",
            ImmutableSet.of("objectName1", "Unknown", "objectName3")
        );

        // Then
        assertThat(tapeObjectReferentialEntities)
            .extracting(entity -> entity.getId().getObjectName(), TapeObjectReferentialEntity::getDigest)
            .containsExactlyInAnyOrder(tuple("objectName1", "digest1"), tuple("objectName3", "digest3"));
    }

    @Test
    public void updateStorageLocationWithSameStorageId() throws Exception {
        // Given
        TapeObjectReferentialEntity tapeObjectReferentialEntity1 = createObjectReferentialEntity();
        objectReferentialRepository.insertOrUpdate(tapeObjectReferentialEntity1);

        // When
        objectReferentialRepository.updateStorageLocation(
            "container",
            "objectName1",
            "storageId1",
            new TapeLibraryTarObjectStorageLocation(
                Arrays.asList(
                    new TarEntryDescription("tarId1", "entry1", 1000L, 3L, "digest1-1"),
                    new TarEntryDescription("tarId2", "entry2", 2000L, 7L, "digest1-2")
                )
            )
        );

        // Then
        Optional<TapeObjectReferentialEntity> tapeObjectReferentialEntity = objectReferentialRepository.find(
            "container",
            "objectName1"
        );

        assertThat(tapeObjectReferentialEntity.isPresent()).isTrue();
        assertThat(tapeObjectReferentialEntity.get().getId().getContainerName()).isEqualTo("container");
        assertThat(tapeObjectReferentialEntity.get().getId().getObjectName()).isEqualTo("objectName1");
        assertThat(tapeObjectReferentialEntity.get().getSize()).isEqualTo(10L);
        assertThat(tapeObjectReferentialEntity.get().getDigest()).isEqualTo("digest1");
        assertThat(tapeObjectReferentialEntity.get().getDigestType()).isEqualTo("SHA-512");
        assertThat(tapeObjectReferentialEntity.get().getLocation()).isInstanceOf(
            TapeLibraryTarObjectStorageLocation.class
        );
        assertThat(
            ((TapeLibraryTarObjectStorageLocation) tapeObjectReferentialEntity.get().getLocation()).getTarEntries()
        )
            .extracting(
                TarEntryDescription::getTarFileId,
                TarEntryDescription::getEntryName,
                TarEntryDescription::getStartPos,
                TarEntryDescription::getSize,
                TarEntryDescription::getDigestValue
            )
            .containsExactly(
                tuple("tarId1", "entry1", 1000L, 3L, "digest1-1"),
                tuple("tarId2", "entry2", 2000L, 7L, "digest1-2")
            );
        assertThat(tapeObjectReferentialEntity.get().getLastObjectModifiedDate()).isEqualTo("date1");
        assertThat(tapeObjectReferentialEntity.get().getLastUpdateDate()).isNotEqualTo("date2");
    }

    @Test
    public void updateStorageLocationWithDifferentStorageId() throws Exception {
        // Given
        TapeObjectReferentialEntity tapeObjectReferentialEntity1 = createObjectReferentialEntity();
        objectReferentialRepository.insertOrUpdate(tapeObjectReferentialEntity1);

        // When
        objectReferentialRepository.updateStorageLocation(
            "container",
            "objectName1",
            "ANOTHER_STORAGE_ID",
            new TapeLibraryTarObjectStorageLocation(
                Arrays.asList(
                    new TarEntryDescription("tarId1", "entry1", 1000L, 3L, "digest1-1"),
                    new TarEntryDescription("tarId2", "entry2", 2000L, 7L, "digest1-2")
                )
            )
        );

        // Then
        Optional<TapeObjectReferentialEntity> tapeObjectReferentialEntity = objectReferentialRepository.find(
            "container",
            "objectName1"
        );

        assertThat(tapeObjectReferentialEntity.isPresent()).isTrue();
        assertThat(tapeObjectReferentialEntity.get().getId().getContainerName()).isEqualTo("container");
        assertThat(tapeObjectReferentialEntity.get().getId().getObjectName()).isEqualTo("objectName1");
        assertThat(tapeObjectReferentialEntity.get().getSize()).isEqualTo(10L);
        assertThat(tapeObjectReferentialEntity.get().getDigest()).isEqualTo("digest1");
        assertThat(tapeObjectReferentialEntity.get().getDigestType()).isEqualTo("SHA-512");
        assertThat(tapeObjectReferentialEntity.get().getLocation()).isInstanceOf(
            TapeLibraryInputFileObjectStorageLocation.class
        );
        assertThat(tapeObjectReferentialEntity.get().getLastObjectModifiedDate()).isEqualTo("date1");
        assertThat(tapeObjectReferentialEntity.get().getLastUpdateDate()).isEqualTo("date2");
    }

    @Test
    public void updateStorageLocationUnknownObject() throws Exception {
        // Given

        // When
        objectReferentialRepository.updateStorageLocation(
            "container",
            "objectName1",
            "ANOTHER_STORAGE_ID",
            new TapeLibraryTarObjectStorageLocation(
                Arrays.asList(
                    new TarEntryDescription("tarId1", "entry1", 1000L, 3L, "digest1-1"),
                    new TarEntryDescription("tarId2", "entry2", 2000L, 7L, "digest1-2")
                )
            )
        );
        // Then (no exception)
    }

    @Test
    public void deleteNonExistingObject() throws Exception {
        // Given

        // When
        boolean deleted = objectReferentialRepository.delete(
            new TapeLibraryObjectReferentialId("container", "objectName1")
        );

        // Then
        assertThat(deleted).isFalse();
    }

    @Test
    public void deleteExistingObject() throws Exception {
        // Given
        TapeObjectReferentialEntity tapeObjectReferentialEntity1 = createObjectReferentialEntity();
        objectReferentialRepository.insertOrUpdate(tapeObjectReferentialEntity1);

        // When
        boolean deleted = objectReferentialRepository.delete(
            new TapeLibraryObjectReferentialId("container", "objectName1")
        );

        // Then
        assertThat(deleted).isTrue();
        Optional<TapeObjectReferentialEntity> tapeObjectReferentialEntity = objectReferentialRepository.find(
            "container",
            "objectName1"
        );

        assertThat(tapeObjectReferentialEntity.isPresent()).isFalse();
    }

    @Test
    public void selectArchiveIdsByObjectIdsSmallDataSet() throws ObjectReferentialException {
        // Given
        for (int i = 0; i < 3; i++) {
            objectReferentialRepository.insertOrUpdate(
                new TapeObjectReferentialEntity(
                    new TapeLibraryObjectReferentialId("container", "objectName" + i),
                    100L,
                    DigestType.SHA512.getName(),
                    "digest" + i,
                    "storageId" + i,
                    new TapeLibraryInputFileObjectStorageLocation(),
                    "date1-" + i,
                    "date2-" + i
                )
            );
        }
        for (int i = 3; i < 6; i++) {
            objectReferentialRepository.insertOrUpdate(
                new TapeObjectReferentialEntity(
                    new TapeLibraryObjectReferentialId("container", "objectName" + i),
                    100L,
                    DigestType.SHA512.getName(),
                    "digest" + i,
                    "storageId" + i,
                    new TapeLibraryTarObjectStorageLocation(
                        List.of(
                            new TarEntryDescription(
                                "tarId" + i + "-1",
                                "storageId" + i,
                                0,
                                40,
                                "digest-tar-" + i + "-1"
                            ),
                            new TarEntryDescription(
                                "tarId" + i + "-1",
                                "storageId" + i,
                                40,
                                40,
                                "digest-tar-" + i + "-1"
                            ),
                            new TarEntryDescription(
                                "tarId" + i + "-2",
                                "storageId" + i,
                                0,
                                20,
                                "digest-tar-" + i + "-2"
                            )
                        )
                    ),
                    "date1-" + i,
                    "date2-" + i
                )
            );
        }

        // When
        Set<String> archiveIds = objectReferentialRepository.selectArchiveIdsByObjectIds(
            IntStream.range(0, 10)
                .mapToObj(i -> new TapeLibraryObjectReferentialId("container", "objectName" + i))
                .iterator()
        );

        // Then
        assertThat(archiveIds).containsExactlyInAnyOrder(
            "tarId3-1",
            "tarId3-2",
            "tarId4-1",
            "tarId4-2",
            "tarId5-1",
            "tarId5-2"
        );
    }

    @Test
    public void selectArchiveIdsByObjectIdsLargeDataSet() throws ObjectReferentialException {
        // Given
        int nbInputFileObjects = 33;
        int nbOnTarObjects = 33;
        for (int i = 0; i < nbInputFileObjects; i++) {
            objectReferentialRepository.insertOrUpdate(
                new TapeObjectReferentialEntity(
                    new TapeLibraryObjectReferentialId("container", "objectName" + i),
                    100L,
                    DigestType.SHA512.getName(),
                    "digest" + i,
                    "storageId" + i,
                    new TapeLibraryInputFileObjectStorageLocation(),
                    "date1-" + i,
                    "date2-" + i
                )
            );
        }
        for (int i = nbInputFileObjects; i < nbInputFileObjects + nbOnTarObjects; i++) {
            objectReferentialRepository.insertOrUpdate(
                new TapeObjectReferentialEntity(
                    new TapeLibraryObjectReferentialId("container", "objectName" + i),
                    100L,
                    DigestType.SHA512.getName(),
                    "digest" + i,
                    "storageId" + i,
                    new TapeLibraryTarObjectStorageLocation(
                        List.of(
                            new TarEntryDescription(
                                "tarId" + i + "-1",
                                "storageId" + i,
                                0,
                                40,
                                "digest-tar-" + i + "-1"
                            ),
                            new TarEntryDescription(
                                "tarId" + i + "-1",
                                "storageId" + i,
                                40,
                                40,
                                "digest-tar-" + i + "-1"
                            ),
                            new TarEntryDescription(
                                "tarId" + i + "-2",
                                "storageId" + i,
                                0,
                                20,
                                "digest-tar-" + i + "-2"
                            )
                        )
                    ),
                    "date1-" + i,
                    "date2-" + i
                )
            );
        }

        // When
        Set<String> archiveIds = objectReferentialRepository.selectArchiveIdsByObjectIds(
            IntStream.range(0, nbInputFileObjects + nbOnTarObjects + 10)
                .mapToObj(i -> new TapeLibraryObjectReferentialId("container", "objectName" + i))
                .iterator()
        );

        // Then
        assertThat(nbInputFileObjects).isGreaterThan(BULK_SIZE);
        assertThat(nbOnTarObjects).isGreaterThan(BULK_SIZE);
        assertThat(archiveIds).containsExactlyInAnyOrderElementsOf(
            IntStream.range(nbInputFileObjects, nbInputFileObjects + nbOnTarObjects)
                .mapToObj(i -> List.of("tarId" + i + "-1", "tarId" + i + "-2"))
                .flatMap(Collection::stream)
                .collect(Collectors.toList())
        );
    }

    @Test
    public void findAllOfEmptyContainer() throws ObjectReferentialException {
        // Given

        // When
        CloseableIterator<ObjectEntry> result = objectReferentialRepository.listContainerObjectEntries("container");

        // Then
        assertThat(result).isEmpty();
        assertThatCode(result::close).doesNotThrowAnyException();
    }

    @Test
    public void findAllObjectsOfNonEmptyContainer() throws ObjectReferentialException {
        // Given
        for (int i = 0; i < 5; i++) {
            objectReferentialRepository.insertOrUpdate(
                new TapeObjectReferentialEntity(
                    new TapeLibraryObjectReferentialId("container", "objectName" + i),
                    1_000_000_000L * i,
                    DigestType.SHA512.getName(),
                    "digest" + i,
                    "storageId" + i,
                    new TapeLibraryInputFileObjectStorageLocation(),
                    "date1-" + i,
                    "date2-" + i
                )
            );
        }

        // When
        CloseableIterator<ObjectEntry> result = objectReferentialRepository.listContainerObjectEntries("container");

        // Then
        assertThat(result)
            .extracting(ObjectEntry::getObjectId, ObjectEntry::getSize)
            .containsExactlyInAnyOrder(
                tuple("objectName0", 0L),
                tuple("objectName1", 1_000_000_000L),
                tuple("objectName2", 2_000_000_000L),
                tuple("objectName3", 3_000_000_000L),
                tuple("objectName4", 4_000_000_000L)
            );

        assertThatCode(result::close).doesNotThrowAnyException();
    }

    @Test
    public void findAllObjectsIgnoresOtherContainers() throws ObjectReferentialException {
        // Given
        objectReferentialRepository.insertOrUpdate(
            new TapeObjectReferentialEntity(
                new TapeLibraryObjectReferentialId("container", "objectName1"),
                100,
                DigestType.SHA512.getName(),
                "digest1",
                "storageId1",
                new TapeLibraryInputFileObjectStorageLocation(),
                "date1",
                "date2"
            )
        );

        objectReferentialRepository.insertOrUpdate(
            new TapeObjectReferentialEntity(
                new TapeLibraryObjectReferentialId("ANOTHER_CONTAINER", "objectName2"),
                200,
                DigestType.SHA512.getName(),
                "digest2",
                "storageId2",
                new TapeLibraryInputFileObjectStorageLocation(),
                "date1",
                "date2"
            )
        );

        // When
        CloseableIterator<ObjectEntry> result = objectReferentialRepository.listContainerObjectEntries("container");

        // Then
        assertThat(result)
            .extracting(ObjectEntry::getObjectId, ObjectEntry::getSize)
            .containsExactly(tuple("objectName1", 100L));

        assertThatCode(result::close).doesNotThrowAnyException();
    }

    @Test
    public void findAllObjectsThenCloseIteratorThenClosed() throws ObjectReferentialException {
        // Given
        for (int i = 0; i < 5; i++) {
            objectReferentialRepository.insertOrUpdate(
                new TapeObjectReferentialEntity(
                    new TapeLibraryObjectReferentialId("container", "objectName" + i),
                    1_000_000_000L * i,
                    DigestType.SHA512.getName(),
                    "digest" + i,
                    "storageId" + i,
                    new TapeLibraryInputFileObjectStorageLocation(),
                    "date1-" + i,
                    "date2-" + i
                )
            );
        }

        // When
        CloseableIterator<ObjectEntry> result = objectReferentialRepository.listContainerObjectEntries("container");

        // Then
        assertThat(result.next()).isNotNull();
        assertThat(result.next()).isNotNull();

        // When : iterator closed
        result.close();

        // Then : Cannot consume data
        assertThatThrownBy(() -> IteratorUtils.toList(result)).isInstanceOf(IllegalStateException.class);
    }

    private TapeObjectReferentialEntity createObjectReferentialEntity() {
        return new TapeObjectReferentialEntity(
            new TapeLibraryObjectReferentialId("container", "objectName1"),
            10L,
            DigestType.SHA512.getName(),
            "digest1",
            "storageId1",
            new TapeLibraryInputFileObjectStorageLocation(),
            "date1",
            "date2"
        );
    }
}

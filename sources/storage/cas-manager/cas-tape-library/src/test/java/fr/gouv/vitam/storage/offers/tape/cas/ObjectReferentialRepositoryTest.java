package fr.gouv.vitam.storage.offers.tape.cas;

import com.google.common.collect.ImmutableSet;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.database.server.mongodb.SimpleMongoDBAccess;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.storage.engine.common.collection.OfferCollections;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryInputFileObjectStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryObjectReferentialId;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryTarObjectStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeObjectReferentialEntity;
import fr.gouv.vitam.common.model.tape.TarEntryDescription;
import fr.gouv.vitam.storage.offers.tape.exception.ObjectReferentialException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static fr.gouv.vitam.common.database.collections.VitamCollection.getMongoClientOptions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class ObjectReferentialRepositoryTest {

    public static final String TAPE_OBJECT_REFERENTIAL_COLLECTION =
        OfferCollections.TAPE_OBJECT_REFERENTIAL.getName() + GUIDFactory.newGUID().getId();

    @ClassRule
    public static MongoRule mongoRule = new MongoRule(getMongoClientOptions(), TAPE_OBJECT_REFERENTIAL_COLLECTION);

    private static ObjectReferentialRepository objectReferentialRepository;

    @BeforeClass
    public static void setUpBeforeClass() {
        MongoDbAccess mongoDbAccess = new SimpleMongoDBAccess(mongoRule.getMongoClient(), MongoRule.VITAM_DB);
        objectReferentialRepository = new ObjectReferentialRepository(mongoDbAccess.getMongoDatabase()
            .getCollection(TAPE_OBJECT_REFERENTIAL_COLLECTION));
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
        Optional<TapeObjectReferentialEntity> tapeObjectReferentialEntity =
            objectReferentialRepository.find("container", "objectName1");

        assertThat(tapeObjectReferentialEntity.isPresent()).isTrue();
        assertThat(tapeObjectReferentialEntity.get().getId().getContainerName()).isEqualTo("container");
        assertThat(tapeObjectReferentialEntity.get().getId().getObjectName()).isEqualTo("objectName1");
        assertThat(tapeObjectReferentialEntity.get().getSize()).isEqualTo(10L);
        assertThat(tapeObjectReferentialEntity.get().getDigest()).isEqualTo("digest1");
        assertThat(tapeObjectReferentialEntity.get().getDigestType()).isEqualTo("SHA-512");
        assertThat(tapeObjectReferentialEntity.get().getLocation())
            .isInstanceOf(TapeLibraryInputFileObjectStorageLocation.class);
        assertThat(tapeObjectReferentialEntity.get().getLastObjectModifiedDate()).isEqualTo("date1");
        assertThat(tapeObjectReferentialEntity.get().getLastUpdateDate()).isEqualTo("date2");
    }

    @Test
    public void insertOrUpdateExistingEntity() throws Exception {

        // Given
        TapeObjectReferentialEntity tapeObjectReferentialEntity1 = createObjectReferentialEntity();

        TapeObjectReferentialEntity tapeObjectReferentialEntity1Version2 = JsonHandler.getFromString(
            JsonHandler.unprettyPrint(tapeObjectReferentialEntity1), TapeObjectReferentialEntity.class)
            .setSize(20L)
            .setDigest("digest2")
            .setStorageId("storageId2")
            .setLastObjectModifiedDate("date3")
            .setLastUpdateDate("date4");

        // When (insert than update)
        objectReferentialRepository.insertOrUpdate(tapeObjectReferentialEntity1);
        objectReferentialRepository.insertOrUpdate(tapeObjectReferentialEntity1Version2);

        // Then
        Optional<TapeObjectReferentialEntity> tapeObjectReferentialEntity =
            objectReferentialRepository.find("container", "objectName1");

        assertThat(tapeObjectReferentialEntity.isPresent()).isTrue();
        assertThat(tapeObjectReferentialEntity.get().getId().getContainerName()).isEqualTo("container");
        assertThat(tapeObjectReferentialEntity.get().getId().getObjectName()).isEqualTo("objectName1");
        assertThat(tapeObjectReferentialEntity.get().getSize()).isEqualTo(20L);
        assertThat(tapeObjectReferentialEntity.get().getDigest()).isEqualTo("digest2");
        assertThat(tapeObjectReferentialEntity.get().getDigestType()).isEqualTo("SHA-512");
        assertThat(tapeObjectReferentialEntity.get().getLocation())
            .isInstanceOf(TapeLibraryInputFileObjectStorageLocation.class);
        assertThat(tapeObjectReferentialEntity.get().getLastObjectModifiedDate()).isEqualTo("date3");
        assertThat(tapeObjectReferentialEntity.get().getLastUpdateDate()).isEqualTo("date4");
    }

    @Test
    public void findNonExisting() throws Exception {
        //Given

        // When
        Optional<TapeObjectReferentialEntity> tapeObjectReferentialEntity =
            objectReferentialRepository.find("container", "objectName1");

        // Then
        assertThat(tapeObjectReferentialEntity.isPresent()).isFalse();
    }

    @Test
    public void findExisting() throws Exception {

        // Given
        TapeObjectReferentialEntity tapeObjectReferentialEntity1 = createObjectReferentialEntity();
        objectReferentialRepository.insertOrUpdate(tapeObjectReferentialEntity1);

        // When
        Optional<TapeObjectReferentialEntity> tapeObjectReferentialEntity =
            objectReferentialRepository.find("container", "objectName1");

        // Then
        assertThat(tapeObjectReferentialEntity.isPresent()).isTrue();
    }

    @Test
    public void bulkFindNotExisting() throws ObjectReferentialException {
        // Given

        // When
        List<TapeObjectReferentialEntity> tapeObjectReferentialEntities =
            objectReferentialRepository.bulkFind("container",
                ImmutableSet.of("objectName1", "objectName2", "objectName3"));

        // Then
        assertThat(tapeObjectReferentialEntities).isEmpty();
    }

    @Test
    public void bulkFindExisting() throws ObjectReferentialException {

        // Given
        for (int i = 0; i < 5; i++) {
            objectReferentialRepository.insertOrUpdate(
                new TapeObjectReferentialEntity(
                    new TapeLibraryObjectReferentialId("container", "objectName" + i),
                    10L, DigestType.SHA512.getName(), "digest" + i, "storageId" + i,
                    new TapeLibraryInputFileObjectStorageLocation(), "date1-" + i, "date2-" + i
                ));
        }

        // When
        List<TapeObjectReferentialEntity> tapeObjectReferentialEntities =
            objectReferentialRepository.bulkFind("container",
                ImmutableSet.of("objectName1", "objectName2", "objectName3"));

        // Then
        assertThat(tapeObjectReferentialEntities).extracting(
            entity -> entity.getId().getObjectName(),
            TapeObjectReferentialEntity::getDigest).containsExactlyInAnyOrder(
            tuple("objectName1", "digest1"),
            tuple("objectName2", "digest2"),
            tuple("objectName3", "digest3")
        );
    }

    @Test
    public void bulkFindExistingPartial() throws ObjectReferentialException {

        // Given
        for (int i = 0; i < 5; i++) {
            objectReferentialRepository.insertOrUpdate(
                new TapeObjectReferentialEntity(
                    new TapeLibraryObjectReferentialId("container", "objectName" + i),
                    10L, DigestType.SHA512.getName(), "digest" + i, "storageId" + i,
                    new TapeLibraryInputFileObjectStorageLocation(), "date1-" + i, "date2-" + i
                ));
        }

        // When
        List<TapeObjectReferentialEntity> tapeObjectReferentialEntities =
            objectReferentialRepository.bulkFind("container",
                ImmutableSet.of("objectName1", "Unknown", "objectName3"));

        // Then
        assertThat(tapeObjectReferentialEntities).extracting(
            entity -> entity.getId().getObjectName(),
            TapeObjectReferentialEntity::getDigest).containsExactlyInAnyOrder(
            tuple("objectName1", "digest1"),
            tuple("objectName3", "digest3")
        );
    }

    @Test
    public void updateStorageLocationWithSameStorageId() throws Exception {

        // Given
        TapeObjectReferentialEntity tapeObjectReferentialEntity1 = createObjectReferentialEntity();
        objectReferentialRepository.insertOrUpdate(tapeObjectReferentialEntity1);

        // When
        objectReferentialRepository.updateStorageLocation(
            "container", "objectName1", "storageId1",
            new TapeLibraryTarObjectStorageLocation(
                Arrays.asList(
                    new TarEntryDescription("tarId1", "entry1", 1000L, 3L, "digest1-1"),
                    new TarEntryDescription("tarId2", "entry2", 2000L, 7L, "digest1-2")
                )
            )
        );

        // Then
        Optional<TapeObjectReferentialEntity> tapeObjectReferentialEntity =
            objectReferentialRepository.find("container", "objectName1");

        assertThat(tapeObjectReferentialEntity.isPresent()).isTrue();
        assertThat(tapeObjectReferentialEntity.get().getId().getContainerName()).isEqualTo("container");
        assertThat(tapeObjectReferentialEntity.get().getId().getObjectName()).isEqualTo("objectName1");
        assertThat(tapeObjectReferentialEntity.get().getSize()).isEqualTo(10L);
        assertThat(tapeObjectReferentialEntity.get().getDigest()).isEqualTo("digest1");
        assertThat(tapeObjectReferentialEntity.get().getDigestType()).isEqualTo("SHA-512");
        assertThat(tapeObjectReferentialEntity.get().getLocation())
            .isInstanceOf(TapeLibraryTarObjectStorageLocation.class);
        assertThat(((TapeLibraryTarObjectStorageLocation)tapeObjectReferentialEntity.get().getLocation())
            .getTarEntries()).extracting(
                TarEntryDescription::getTarFileId,
            TarEntryDescription::getEntryName,
            TarEntryDescription::getStartPos,
            TarEntryDescription::getSize,
            TarEntryDescription::getDigestValue).containsExactly(
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
            "container", "objectName1", "ANOTHER_STORAGE_ID",
            new TapeLibraryTarObjectStorageLocation(
                Arrays.asList(
                    new TarEntryDescription("tarId1", "entry1", 1000L, 3L, "digest1-1"),
                    new TarEntryDescription("tarId2", "entry2", 2000L, 7L, "digest1-2")
                )
            )
        );

        // Then
        Optional<TapeObjectReferentialEntity> tapeObjectReferentialEntity =
            objectReferentialRepository.find("container", "objectName1");

        assertThat(tapeObjectReferentialEntity.isPresent()).isTrue();
        assertThat(tapeObjectReferentialEntity.get().getId().getContainerName()).isEqualTo("container");
        assertThat(tapeObjectReferentialEntity.get().getId().getObjectName()).isEqualTo("objectName1");
        assertThat(tapeObjectReferentialEntity.get().getSize()).isEqualTo(10L);
        assertThat(tapeObjectReferentialEntity.get().getDigest()).isEqualTo("digest1");
        assertThat(tapeObjectReferentialEntity.get().getDigestType()).isEqualTo("SHA-512");
        assertThat(tapeObjectReferentialEntity.get().getLocation())
            .isInstanceOf(TapeLibraryInputFileObjectStorageLocation.class);
        assertThat(tapeObjectReferentialEntity.get().getLastObjectModifiedDate()).isEqualTo("date1");
        assertThat(tapeObjectReferentialEntity.get().getLastUpdateDate()).isEqualTo("date2");
    }

    @Test
    public void updateStorageLocationUnknownObject() throws Exception {

        // Given

        // When
        objectReferentialRepository.updateStorageLocation(
            "container", "objectName1", "ANOTHER_STORAGE_ID",
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
        boolean deleted =
            objectReferentialRepository.delete(new TapeLibraryObjectReferentialId("container", "objectName1"));

        // Then
        assertThat(deleted).isFalse();
    }

    @Test
    public void deleteExistingObject() throws Exception {

        // Given
        TapeObjectReferentialEntity tapeObjectReferentialEntity1 = createObjectReferentialEntity();
        objectReferentialRepository.insertOrUpdate(tapeObjectReferentialEntity1);

        // When
        boolean deleted =
            objectReferentialRepository.delete(new TapeLibraryObjectReferentialId("container", "objectName1"));

        // Then
        assertThat(deleted).isTrue();
        Optional<TapeObjectReferentialEntity> tapeObjectReferentialEntity =
            objectReferentialRepository.find("container", "objectName1");

        assertThat(tapeObjectReferentialEntity.isPresent()).isFalse();
    }

    private TapeObjectReferentialEntity createObjectReferentialEntity() {
        return new TapeObjectReferentialEntity(
            new TapeLibraryObjectReferentialId("container", "objectName1"),
            10L, DigestType.SHA512.getName(), "digest1", "storageId1",
            new TapeLibraryInputFileObjectStorageLocation(), "date1", "date2"
        );
    }
}

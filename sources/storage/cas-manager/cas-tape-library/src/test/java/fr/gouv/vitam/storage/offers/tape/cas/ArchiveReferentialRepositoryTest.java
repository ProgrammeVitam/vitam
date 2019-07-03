package fr.gouv.vitam.storage.offers.tape.cas;

import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.database.server.mongodb.SimpleMongoDBAccess;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.storage.engine.common.collection.OfferCollections;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryBuildingOnDiskArchiveStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryOnTapeArchiveStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryReadyOnDiskArchiveStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeArchiveReferentialEntity;
import fr.gouv.vitam.storage.offers.tape.exception.ArchiveReferentialException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Optional;

import static fr.gouv.vitam.common.database.collections.VitamCollection.getMongoClientOptions;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class ArchiveReferentialRepositoryTest {

    public static final String TAPE_TAR_REFERENTIAL_COLLECTION =
        OfferCollections.TAPE_ARCHIVE_REFERENTIAL.getName() + GUIDFactory.newGUID().getId();

    @ClassRule
    public static MongoRule mongoRule = new MongoRule(getMongoClientOptions(), TAPE_TAR_REFERENTIAL_COLLECTION);

    private static ArchiveReferentialRepository archiveReferentialRepository;

    @BeforeClass
    public static void setUpBeforeClass() {
        MongoDbAccess mongoDbAccess = new SimpleMongoDBAccess(mongoRule.getMongoClient(), MongoRule.VITAM_DB);
        archiveReferentialRepository = new ArchiveReferentialRepository(mongoDbAccess.getMongoDatabase()
            .getCollection(TAPE_TAR_REFERENTIAL_COLLECTION));
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
    public void insertNewTar() throws Exception {

        // Given
        TapeArchiveReferentialEntity tapeArchiveReferentialEntity = new TapeArchiveReferentialEntity(
            "tarId1", new TapeLibraryBuildingOnDiskArchiveStorageLocation(), 10L, "digest1", "date1"
        );

        // When
        archiveReferentialRepository.insert(tapeArchiveReferentialEntity);

        // Then
        Optional<TapeArchiveReferentialEntity> tarReferentialEntity = archiveReferentialRepository.find("tarId1");
        assertThat(tarReferentialEntity.isPresent()).isTrue();
        assertThat(tarReferentialEntity.get().getArchiveId()).isEqualTo("tarId1");
        assertThat(tarReferentialEntity.get().getLocation()).isInstanceOf(
            TapeLibraryBuildingOnDiskArchiveStorageLocation.class);
        assertThat(tarReferentialEntity.get().getSize()).isEqualTo(10L);
        assertThat(tarReferentialEntity.get().getDigestValue()).isEqualTo("digest1");
        assertThat(tarReferentialEntity.get().getLastUpdateDate()).isEqualTo("date1");
    }

    @Test
    public void insertExistingTar() throws Exception {

        // Given
        TapeArchiveReferentialEntity tapeArchiveReferentialEntity = new TapeArchiveReferentialEntity(
            "tarId1", new TapeLibraryBuildingOnDiskArchiveStorageLocation(), 10L, "digest1", "date1"
        );
        archiveReferentialRepository.insert(tapeArchiveReferentialEntity);

        // When / Then
        assertThatThrownBy(() -> archiveReferentialRepository.insert(tapeArchiveReferentialEntity))
            .isInstanceOf(ArchiveReferentialException.class);
    }

    @Test
    public void findNonExisting() throws Exception {

        // Given

        // When
        Optional<TapeArchiveReferentialEntity> tarReferentialEntity = archiveReferentialRepository.find("tarId1");

        // Then
        assertThat(tarReferentialEntity.isPresent()).isFalse();
    }

    @Test
    public void findExisting() throws Exception {

        // Given
        TapeArchiveReferentialEntity tapeArchiveReferentialEntity = new TapeArchiveReferentialEntity(
            "tarId1", new TapeLibraryBuildingOnDiskArchiveStorageLocation(), 10L, "digest1", "date1"
        );
        archiveReferentialRepository.insert(tapeArchiveReferentialEntity);

        // When
        Optional<TapeArchiveReferentialEntity> tarReferentialEntity = archiveReferentialRepository.find("tarId1");

        // Then
        assertThat(tarReferentialEntity.isPresent()).isTrue();
    }

    @Test
    public void updateLocationToReadyOnDiskExisting() throws Exception {

        // Given
        TapeArchiveReferentialEntity tapeArchiveReferentialEntity = new TapeArchiveReferentialEntity(
            "tarId1", new TapeLibraryBuildingOnDiskArchiveStorageLocation(), null, null, "date1"
        );
        archiveReferentialRepository.insert(tapeArchiveReferentialEntity);

        // When
        archiveReferentialRepository.updateLocationToReadyOnDisk("tarId1", 10L, "digest1");

        // Then
        Optional<TapeArchiveReferentialEntity> tarReferentialEntity = archiveReferentialRepository.find("tarId1");
        assertThat(tarReferentialEntity.isPresent()).isTrue();
        assertThat(tarReferentialEntity.get().getArchiveId()).isEqualTo("tarId1");
        assertThat(tarReferentialEntity.get().getLocation()).isInstanceOf(
            TapeLibraryReadyOnDiskArchiveStorageLocation.class);
        assertThat(tarReferentialEntity.get().getSize()).isEqualTo(10L);
        assertThat(tarReferentialEntity.get().getDigestValue()).isEqualTo("digest1");
        assertThat(tarReferentialEntity.get().getLastUpdateDate()).isNotEqualTo("date1");
    }

    @Test
    public void updateLocationToReadyOnDiskAlreadyReadyOnDisk() throws Exception {

        // Given
        TapeArchiveReferentialEntity tapeArchiveReferentialEntity = new TapeArchiveReferentialEntity(
            "tarId1", new TapeLibraryBuildingOnDiskArchiveStorageLocation(), null, null, "date1"
        );
        archiveReferentialRepository.insert(tapeArchiveReferentialEntity);

        // When
        archiveReferentialRepository.updateLocationToReadyOnDisk("tarId1", 10L, "digest1");
        archiveReferentialRepository.updateLocationToReadyOnDisk("tarId1", 10L, "digest1");

        // Then (no exception)
    }

    @Test
    public void updateLocationToReadyOnDiskNonExisting() throws Exception {

        // Given

        // When / Then
        assertThatThrownBy(() -> archiveReferentialRepository.updateLocationToReadyOnDisk("tarId1", 10L, "digest1"))
            .isInstanceOf(ArchiveReferentialException.class);
    }

    @Test
    public void updateLocationToOnTapeExisting() throws Exception {

        // Given
        TapeArchiveReferentialEntity tapeArchiveReferentialEntity = new TapeArchiveReferentialEntity(
            "tarId1", new TapeLibraryReadyOnDiskArchiveStorageLocation(), 10L, "digest1", "date1");
        archiveReferentialRepository.insert(tapeArchiveReferentialEntity);

        // When
        archiveReferentialRepository.updateLocationToOnTape("tarId1",
            new TapeLibraryOnTapeArchiveStorageLocation("tapeCode", 12));

        // Then
        Optional<TapeArchiveReferentialEntity> tarReferentialEntity = archiveReferentialRepository.find("tarId1");
        assertThat(tarReferentialEntity.isPresent()).isTrue();
        assertThat(tarReferentialEntity.get().getArchiveId()).isEqualTo("tarId1");
        assertThat(tarReferentialEntity.get().getLocation()).isInstanceOf(
            TapeLibraryOnTapeArchiveStorageLocation.class);
        assertThat(((TapeLibraryOnTapeArchiveStorageLocation) tarReferentialEntity.get().getLocation()).getTapeCode())
            .isEqualTo("tapeCode");
        assertThat(((TapeLibraryOnTapeArchiveStorageLocation) tarReferentialEntity.get().getLocation()).getFilePosition())
            .isEqualTo(12);
        assertThat(tarReferentialEntity.get().getLastUpdateDate()).isNotEqualTo("date1");
    }

    @Test
    public void updateLocationToOnTapeAlreadyOnTape() throws Exception {

        // Given
        TapeArchiveReferentialEntity tapeArchiveReferentialEntity = new TapeArchiveReferentialEntity(
            "tarId1", new TapeLibraryReadyOnDiskArchiveStorageLocation(), 10L, "digest1", "date1");
        archiveReferentialRepository.insert(tapeArchiveReferentialEntity);

        // When
        archiveReferentialRepository.updateLocationToOnTape("tarId1",
            new TapeLibraryOnTapeArchiveStorageLocation("tapeCode", 12));
        archiveReferentialRepository.updateLocationToOnTape("tarId1",
            new TapeLibraryOnTapeArchiveStorageLocation("tapeCode", 12));

        // Then (no exception)
    }

    @Test
    public void updateLocationToOnTapeNonExisting() throws Exception {

        // Given

        // When / Then
        assertThatThrownBy(
            () -> archiveReferentialRepository.updateLocationToOnTape("tarId1",
                new TapeLibraryOnTapeArchiveStorageLocation("tapeCode", 12)))
            .isInstanceOf(ArchiveReferentialException.class);
    }
}

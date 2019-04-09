package fr.gouv.vitam.storage.offers.tape.cas;

import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.database.server.mongodb.SimpleMongoDBAccess;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.storage.engine.common.collection.OfferCollections;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryBuildingOnDiskTarStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryOnTapeTarStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryReadyOnDiskTarStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeTarReferentialEntity;
import fr.gouv.vitam.storage.offers.tape.exception.TarReferentialException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Optional;

import static fr.gouv.vitam.common.database.collections.VitamCollection.getMongoClientOptions;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class TarReferentialRepositoryTest {

    public static final String TAPE_TAR_REFERENTIAL_COLLECTION =
        OfferCollections.TAPE_TAR_REFERENTIAL.getName() + GUIDFactory.newGUID().getId();

    @ClassRule
    public static MongoRule mongoRule = new MongoRule(getMongoClientOptions(), TAPE_TAR_REFERENTIAL_COLLECTION);

    private static TarReferentialRepository tarReferentialRepository;

    @BeforeClass
    public static void setUpBeforeClass() {
        MongoDbAccess mongoDbAccess = new SimpleMongoDBAccess(mongoRule.getMongoClient(), MongoRule.VITAM_DB);
        tarReferentialRepository = new TarReferentialRepository(mongoDbAccess.getMongoDatabase()
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
        TapeTarReferentialEntity tapeTarReferentialEntity = new TapeTarReferentialEntity(
            "tarId1", new TapeLibraryBuildingOnDiskTarStorageLocation(), 10L, "digest1", "date1"
        );

        // When
        tarReferentialRepository.insert(tapeTarReferentialEntity);

        // Then
        Optional<TapeTarReferentialEntity> tarReferentialEntity = tarReferentialRepository.find("tarId1");
        assertThat(tarReferentialEntity.isPresent()).isTrue();
        assertThat(tarReferentialEntity.get().getTarId()).isEqualTo("tarId1");
        assertThat(tarReferentialEntity.get().getLocation()).isInstanceOf(
            TapeLibraryBuildingOnDiskTarStorageLocation.class);
        assertThat(tarReferentialEntity.get().getSize()).isEqualTo(10L);
        assertThat(tarReferentialEntity.get().getDigestValue()).isEqualTo("digest1");
        assertThat(tarReferentialEntity.get().getLastUpdateDate()).isEqualTo("date1");
    }

    @Test
    public void insertExistingTar() throws Exception {

        // Given
        TapeTarReferentialEntity tapeTarReferentialEntity = new TapeTarReferentialEntity(
            "tarId1", new TapeLibraryBuildingOnDiskTarStorageLocation(), 10L, "digest1", "date1"
        );
        tarReferentialRepository.insert(tapeTarReferentialEntity);

        // When / Then
        assertThatThrownBy(() -> tarReferentialRepository.insert(tapeTarReferentialEntity))
            .isInstanceOf(TarReferentialException.class);
    }

    @Test
    public void findNonExisting() throws Exception {

        // Given

        // When
        Optional<TapeTarReferentialEntity> tarReferentialEntity = tarReferentialRepository.find("tarId1");

        // Then
        assertThat(tarReferentialEntity.isPresent()).isFalse();
    }

    @Test
    public void findExisting() throws Exception {

        // Given
        TapeTarReferentialEntity tapeTarReferentialEntity = new TapeTarReferentialEntity(
            "tarId1", new TapeLibraryBuildingOnDiskTarStorageLocation(), 10L, "digest1", "date1"
        );
        tarReferentialRepository.insert(tapeTarReferentialEntity);

        // When
        Optional<TapeTarReferentialEntity> tarReferentialEntity = tarReferentialRepository.find("tarId1");

        // Then
        assertThat(tarReferentialEntity.isPresent()).isTrue();
    }

    @Test
    public void updateLocationToReadyOnDiskExisting() throws Exception {

        // Given
        TapeTarReferentialEntity tapeTarReferentialEntity = new TapeTarReferentialEntity(
            "tarId1", new TapeLibraryBuildingOnDiskTarStorageLocation(), null, null, "date1"
        );
        tarReferentialRepository.insert(tapeTarReferentialEntity);

        // When
        tarReferentialRepository.updateLocationToReadyOnDisk("tarId1", 10L, "digest1");

        // Then
        Optional<TapeTarReferentialEntity> tarReferentialEntity = tarReferentialRepository.find("tarId1");
        assertThat(tarReferentialEntity.isPresent()).isTrue();
        assertThat(tarReferentialEntity.get().getTarId()).isEqualTo("tarId1");
        assertThat(tarReferentialEntity.get().getLocation()).isInstanceOf(
            TapeLibraryReadyOnDiskTarStorageLocation.class);
        assertThat(tarReferentialEntity.get().getSize()).isEqualTo(10L);
        assertThat(tarReferentialEntity.get().getDigestValue()).isEqualTo("digest1");
        assertThat(tarReferentialEntity.get().getLastUpdateDate()).isNotEqualTo("date1");
    }

    @Test
    public void updateLocationToReadyOnDiskAlreadyReadyOnDisk() throws Exception {

        // Given
        TapeTarReferentialEntity tapeTarReferentialEntity = new TapeTarReferentialEntity(
            "tarId1", new TapeLibraryBuildingOnDiskTarStorageLocation(), null, null, "date1"
        );
        tarReferentialRepository.insert(tapeTarReferentialEntity);

        // When
        tarReferentialRepository.updateLocationToReadyOnDisk("tarId1", 10L, "digest1");
        tarReferentialRepository.updateLocationToReadyOnDisk("tarId1", 10L, "digest1");

        // Then (no exception)
    }

    @Test
    public void updateLocationToReadyOnDiskNonExisting() throws Exception {

        // Given

        // When / Then
        assertThatThrownBy(() -> tarReferentialRepository.updateLocationToReadyOnDisk("tarId1", 10L, "digest1"))
            .isInstanceOf(TarReferentialException.class);
    }

    @Test
    public void updateLocationToOnTapeExisting() throws Exception {

        // Given
        TapeTarReferentialEntity tapeTarReferentialEntity = new TapeTarReferentialEntity(
            "tarId1", new TapeLibraryReadyOnDiskTarStorageLocation(), 10L, "digest1", "date1");
        tarReferentialRepository.insert(tapeTarReferentialEntity);

        // When
        tarReferentialRepository.updateLocationToOnTape("tarId1",
            new TapeLibraryOnTapeTarStorageLocation("tapeCode", 12));

        // Then
        Optional<TapeTarReferentialEntity> tarReferentialEntity = tarReferentialRepository.find("tarId1");
        assertThat(tarReferentialEntity.isPresent()).isTrue();
        assertThat(tarReferentialEntity.get().getTarId()).isEqualTo("tarId1");
        assertThat(tarReferentialEntity.get().getLocation()).isInstanceOf(
            TapeLibraryOnTapeTarStorageLocation.class);
        assertThat(((TapeLibraryOnTapeTarStorageLocation) tarReferentialEntity.get().getLocation()).getTapeCode())
            .isEqualTo("tapeCode");
        assertThat(((TapeLibraryOnTapeTarStorageLocation) tarReferentialEntity.get().getLocation()).getFilePosition())
            .isEqualTo(12);
        assertThat(tarReferentialEntity.get().getLastUpdateDate()).isNotEqualTo("date1");
    }

    @Test
    public void updateLocationToOnTapeAlreadyOnTape() throws Exception {

        // Given
        TapeTarReferentialEntity tapeTarReferentialEntity = new TapeTarReferentialEntity(
            "tarId1", new TapeLibraryReadyOnDiskTarStorageLocation(), 10L, "digest1", "date1");
        tarReferentialRepository.insert(tapeTarReferentialEntity);

        // When
        tarReferentialRepository.updateLocationToOnTape("tarId1",
            new TapeLibraryOnTapeTarStorageLocation("tapeCode", 12));
        tarReferentialRepository.updateLocationToOnTape("tarId1",
            new TapeLibraryOnTapeTarStorageLocation("tapeCode", 12));

        // Then (no exception)
    }

    @Test
    public void updateLocationToOnTapeNonExisting() throws Exception {

        // Given

        // When / Then
        assertThatThrownBy(
            () -> tarReferentialRepository.updateLocationToOnTape("tarId1",
                new TapeLibraryOnTapeTarStorageLocation("tapeCode", 12)))
            .isInstanceOf(TarReferentialException.class);
    }
}

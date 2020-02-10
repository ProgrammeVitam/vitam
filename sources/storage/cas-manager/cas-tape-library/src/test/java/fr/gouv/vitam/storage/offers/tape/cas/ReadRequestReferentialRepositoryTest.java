/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.database.server.mongodb.SimpleMongoDBAccess;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.storage.engine.common.collection.OfferCollections;
import fr.gouv.vitam.storage.engine.common.model.FileInTape;
import fr.gouv.vitam.storage.engine.common.model.TapeReadRequestReferentialEntity;
import fr.gouv.vitam.storage.engine.common.model.TarEntryDescription;
import fr.gouv.vitam.storage.engine.common.model.TarLocation;
import fr.gouv.vitam.storage.offers.tape.exception.ReadRequestReferentialException;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Assertions;
import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Optional;

import static fr.gouv.vitam.common.database.collections.VitamCollection.getMongoClientOptions;

public class ReadRequestReferentialRepositoryTest {

    public static final String TAPE_READ_REQUEST_REFERENTIAL_COLLECTION =
        OfferCollections.TAPE_READ_REQUEST_REFERENTIAL.getName() + GUIDFactory.newGUID().getId();

    @ClassRule
    public static MongoRule mongoRule =
        new MongoRule(getMongoClientOptions(), TAPE_READ_REQUEST_REFERENTIAL_COLLECTION);

    private static ReadRequestReferentialRepository readRequestReferentialRepository;

    @BeforeClass
    public static void setUpBeforeClass() {
        MongoDbAccess mongoDbAccess = new SimpleMongoDBAccess(mongoRule.getMongoClient(), MongoRule.VITAM_DB);
        readRequestReferentialRepository = new ReadRequestReferentialRepository(mongoDbAccess.getMongoDatabase()
            .getCollection(TAPE_READ_REQUEST_REFERENTIAL_COLLECTION));
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
    public void test_insert_find_cleanup_OK() throws ReadRequestReferentialException {
        String requestId = GUIDFactory.newGUID().getId();
        TapeReadRequestReferentialEntity tapeReadRequestReferentialEntity = new TapeReadRequestReferentialEntity();
        tapeReadRequestReferentialEntity.setRequestId(requestId);
        tapeReadRequestReferentialEntity.setContainerName("test");
        FileInTape fileInTape = new FileInTape();
        fileInTape.setFileName("aeaaaaaaaahcgk7xab4u4almjab2h5aaaaaq");
        fileInTape.setStorageId("aeaaaaaaaahcgk7xab4u4almjab2h5aaaaaq-aeaaaaaaaahgsrzsab2e2almjab3r6iaaaaq");
        TarEntryDescription tarEntryDescription =
            new TarEntryDescription("20190731123139281-3fd599c8-7747-4639-8176-14bb34f9e520.tar",
                "frigotmp2_0_object/aeaaaaaaaahcgk7xab4u4almjab2h5aaaaaq-aeaaaaaaaahgsrzsab2e2almjab3r6iaaaaq-0",
                0,
                6,
                "86c0bc701ef6b5dd21b080bc5bb2af38097baa6237275da83a52f092c9eae3e4e4b0247391620bd732fe824d18bd3bb6c37e62ec73a8cf3585c6a799399861b1");
        fileInTape.setFileSegments(Lists.newArrayList(tarEntryDescription));
        tapeReadRequestReferentialEntity.setFiles(Lists.newArrayList(fileInTape));
        HashMap<String, TarLocation> tarLocations = new HashMap<>();
        String tarNameWithoutExtension =
            StringUtils.substringBeforeLast("20190731123139281-3fd599c8-7747-4639-8176-14bb34f9e520.tar", ".");
        tarLocations.put(
            tarNameWithoutExtension, TarLocation.TAPE);
        tapeReadRequestReferentialEntity.setTarLocations(tarLocations);
        tapeReadRequestReferentialEntity.setExpireDate(LocalDateUtil.getFormattedDateForMongo(LocalDateTime.now().minusDays(1)));

        readRequestReferentialRepository.insert(tapeReadRequestReferentialEntity);

        Optional<TapeReadRequestReferentialEntity> found = readRequestReferentialRepository.find(requestId);
        Assertions.assertThat(found).isPresent();
        Assertions.assertThat(found.get().getContainerName()).isEqualTo("test");
        Assertions.assertThat(found.get().isExpired()).isTrue();


        String requestId2 = GUIDFactory.newGUID().getId();
        tapeReadRequestReferentialEntity.setRequestId(requestId2);
        tapeReadRequestReferentialEntity.setExpireDate(LocalDateUtil.getFormattedDateForMongo(LocalDateTime.now().plusDays(1)));

        readRequestReferentialRepository.insert(tapeReadRequestReferentialEntity);

        // Even two tapeReadRequestReferentialEntity, only expired one are removed
        long deleted = readRequestReferentialRepository.cleanUp();
        Assertions.assertThat(deleted).isEqualTo(1L);


        found = readRequestReferentialRepository.find(requestId2);
        Assertions.assertThat(found).isPresent();
        Assertions.assertThat(found.get().isExpired()).isFalse();

        // Invalidate will make tapeReadRequestReferentialEntity expired
        readRequestReferentialRepository.invalidate(requestId2);
        found = readRequestReferentialRepository.find(requestId2);
        Assertions.assertThat(found).isPresent();
        Assertions.assertThat(found.get().isExpired()).isTrue();

        // Assert that invalided is removed
        deleted = readRequestReferentialRepository.cleanUp();
        Assertions.assertThat(deleted).isEqualTo(1L);
    }


    @Test
    public void test_update_in_progress_OK() throws ReadRequestReferentialException {
        String requestId = GUIDFactory.newGUID().getId();
        TapeReadRequestReferentialEntity tapeReadRequestReferentialEntity = new TapeReadRequestReferentialEntity();
        tapeReadRequestReferentialEntity.setRequestId(requestId);
        tapeReadRequestReferentialEntity.setContainerName("test");
        FileInTape fileInTape = new FileInTape();
        fileInTape.setFileName("aeaaaaaaaahcgk7xab4u4almjab2h5aaaaaq");
        fileInTape.setStorageId("aeaaaaaaaahcgk7xab4u4almjab2h5aaaaaq-aeaaaaaaaahgsrzsab2e2almjab3r6iaaaaq");
        TarEntryDescription tarEntryDescription =
            new TarEntryDescription("20190731123139281-3fd599c8-7747-4639-8176-14bb34f9e520.tar",
                "frigotmp2_0_object/aeaaaaaaaahcgk7xab4u4almjab2h5aaaaaq-aeaaaaaaaahgsrzsab2e2almjab3r6iaaaaq-0",
                0,
                6,
                "86c0bc701ef6b5dd21b080bc5bb2af38097baa6237275da83a52f092c9eae3e4e4b0247391620bd732fe824d18bd3bb6c37e62ec73a8cf3585c6a799399861b1");
        fileInTape.setFileSegments(Lists.newArrayList(tarEntryDescription));
        tapeReadRequestReferentialEntity.setFiles(Lists.newArrayList(fileInTape));
        HashMap<String, TarLocation> tarLocations = new HashMap<>();
        String tarNameWithoutExtension =
            StringUtils.substringBeforeLast("20190731123139281-3fd599c8-7747-4639-8176-14bb34f9e520.tar", ".");
        tarLocations.put(
            tarNameWithoutExtension, TarLocation.TAPE);
        tapeReadRequestReferentialEntity.setTarLocations(tarLocations);
        tapeReadRequestReferentialEntity.setExpireDate(LocalDateUtil.getFormattedDateForMongo(LocalDateTime.now().plusDays(1)));


        readRequestReferentialRepository.insert(tapeReadRequestReferentialEntity);

        readRequestReferentialRepository.updateReadRequests(tarNameWithoutExtension, TarLocation.DISK);

        Optional<TapeReadRequestReferentialEntity> found = readRequestReferentialRepository.find(requestId);
        Assertions.assertThat(found).isPresent();
        Assertions.assertThat(found.get().isCompleted()).isTrue();
        Assertions.assertThat(found.get().isExpired()).isFalse();

    }
}

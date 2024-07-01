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
package fr.gouv.vitam.storage.offers.tape.rest;

import com.mongodb.client.MongoCollection;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.database.server.mongodb.SimpleMongoDBAccess;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.storage.engine.common.collection.OfferCollections;
import fr.gouv.vitam.storage.engine.common.model.QueueMessageType;
import fr.gouv.vitam.storage.engine.common.model.ReadWriteOrder;
import fr.gouv.vitam.storage.engine.common.model.TapeArchiveReferentialEntity;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryBuildingOnDiskArchiveStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryReadyOnDiskArchiveStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.WriteOrder;
import fr.gouv.vitam.storage.offers.tape.cas.ArchiveReferentialRepository;
import fr.gouv.vitam.storage.offers.tape.cas.BackupFileStorage;
import fr.gouv.vitam.storage.offers.tape.cas.BucketTopologyHelper;
import fr.gouv.vitam.storage.offers.tape.cas.WriteOrderCreator;
import fr.gouv.vitam.storage.offers.tape.impl.queue.QueueRepositoryImpl;
import fr.gouv.vitam.storage.offers.tape.spec.QueueRepository;
import org.bson.Document;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.InputStream;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class AdminTapeResourceTest {

    public static final String TAPE_TAR_REFERENTIAL_COLLECTION =
        OfferCollections.TAPE_ARCHIVE_REFERENTIAL.getName() + GUIDFactory.newGUID().getId();

    public static final String TAPE_QUEUE_MESSAGE_COLLECTION =
        OfferCollections.TAPE_QUEUE_MESSAGE.getName() + GUIDFactory.newGUID().getId();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @ClassRule
    public static MongoRule mongoRule = new MongoRule(
        MongoDbAccess.getMongoClientSettingsBuilder(),
        TAPE_TAR_REFERENTIAL_COLLECTION,
        TAPE_QUEUE_MESSAGE_COLLECTION
    );

    private static ArchiveReferentialRepository archiveReferentialRepository;
    private static QueueRepository readWriteQueue;
    private static MongoCollection<Document> tarReferentialCollection;
    private static MongoCollection<Document> queueCollection;

    @BeforeClass
    public static void setUpBeforeClass() {
        MongoDbAccess mongoDbAccess = new SimpleMongoDBAccess(mongoRule.getMongoClient(), MongoRule.VITAM_DB);

        tarReferentialCollection = mongoDbAccess.getMongoDatabase().getCollection(TAPE_TAR_REFERENTIAL_COLLECTION);
        archiveReferentialRepository = new ArchiveReferentialRepository(tarReferentialCollection);

        queueCollection = mongoDbAccess.getMongoDatabase().getCollection(TAPE_QUEUE_MESSAGE_COLLECTION);
        readWriteQueue = new QueueRepositoryImpl(queueCollection);
    }

    @After
    public void tearDown() {
        mongoRule.handleAfter();
    }

    @Test
    public void testPutObjectOk() throws Exception {
        WriteOrderCreator writeOrderCreator = new WriteOrderCreator(archiveReferentialRepository, readWriteQueue);
        BackupFileStorage backupFileStorage = new BackupFileStorage(
            archiveReferentialRepository,
            writeOrderCreator,
            BucketTopologyHelper.BACKUP_BUCKET,
            BucketTopologyHelper.BACKUP_FILE_BUCKET,
            temporaryFolder.newFolder().getPath()
        );

        AdminTapeResource adminTapeResource = new AdminTapeResource(backupFileStorage);

        InputStream backupStream = PropertiesUtils.getResourceAsStream("backup.zip");

        String objectId = "2019-01-01.backup.mongoc.zip";
        adminTapeResource.putObject(objectId, backupStream);

        Optional<TapeArchiveReferentialEntity> archiveReference = archiveReferentialRepository.find(objectId);

        assertThat(archiveReference).isPresent();
        assertThat(archiveReference.get().getLocation()).isInstanceOf(
            TapeLibraryBuildingOnDiskArchiveStorageLocation.class
        );

        writeOrderCreator.startListener();

        // Wait until writeOrder will be processed
        Thread.sleep(2000);

        assertThat(queueCollection.countDocuments()).isEqualTo(1);

        archiveReference = archiveReferentialRepository.find(objectId);

        assertThat(archiveReference).isPresent();
        assertThat(archiveReference.get().getLocation()).isInstanceOf(
            TapeLibraryReadyOnDiskArchiveStorageLocation.class
        );

        Optional<ReadWriteOrder> readWriteOrder = readWriteQueue.receive(QueueMessageType.WriteBackupOrder);
        assertThat(readWriteOrder).isPresent();
        WriteOrder writeOrder = (WriteOrder) readWriteOrder.get();
        assertThat(writeOrder.getArchiveId()).isEqualTo(objectId);
        assertThat(writeOrder.getSize()).isEqualTo(10240L);
        assertThat(writeOrder.getBucket()).isEqualTo(BucketTopologyHelper.BACKUP_BUCKET);
        assertThat(writeOrder.getFileBucketId()).isEqualTo(BucketTopologyHelper.BACKUP_FILE_BUCKET);
        assertThat(writeOrder.getDigest()).isEqualTo(
            "4f68ecd1a386def8129359761b774e56207875be2da4e2d6b29c75a61b006243a183576111e9d26bdae3666319eb5add714e845bd16dde3a35eab39ae8cf7c9f"
        );
        assertThat(writeOrder.getFilePath()).isEqualTo(BucketTopologyHelper.BACKUP_FILE_BUCKET + "/" + objectId);
    }
}

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

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.storage.engine.common.model.EntryType;
import fr.gouv.vitam.storage.engine.common.model.QueueMessageType;
import fr.gouv.vitam.storage.engine.common.model.TapeArchiveReferentialEntity;
import fr.gouv.vitam.storage.engine.common.model.WriteOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

public class BackupFileStorageTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    ArchiveReferentialRepository archiveReferentialRepository;

    @Mock
    WriteOrderCreator writeOrderCreator;

    @Test
    public void testWriteFile() throws Exception {
        InputStream backupStream = PropertiesUtils.getResourceAsStream("backup.zip");
        String objectId = "2019-01-01.backup.mongoc.zip";

        // Given
        BackupFileStorage backupFileStorage = new BackupFileStorage(
            archiveReferentialRepository,
            writeOrderCreator,
            BucketTopologyHelper.BACKUP_BUCKET,
            BucketTopologyHelper.BACKUP_FILE_BUCKET,
            temporaryFolder.getRoot().getAbsolutePath()
        );

        // When
        backupFileStorage.writeFile(objectId, backupStream);

        // Then
        ArgumentCaptor<TapeArchiveReferentialEntity> tapeArchiveReferentialEntityArgumentCaptor =
            ArgumentCaptor.forClass(TapeArchiveReferentialEntity.class);
        verify(archiveReferentialRepository).insert(tapeArchiveReferentialEntityArgumentCaptor.capture());
        TapeArchiveReferentialEntity tapeArchiveReferentialEntity =
            tapeArchiveReferentialEntityArgumentCaptor.getValue();
        assertThat(tapeArchiveReferentialEntity.getArchiveId()).isEqualTo(objectId);
        assertThat(tapeArchiveReferentialEntity.getEntryTape()).isEqualTo(EntryType.BACKUP);

        ArgumentCaptor<WriteOrder> writeOrderArgumentCaptor = ArgumentCaptor.forClass(WriteOrder.class);
        verify(writeOrderCreator).addToQueue(writeOrderArgumentCaptor.capture());

        assertThat(writeOrderArgumentCaptor.getValue().getBucket()).isEqualTo(BucketTopologyHelper.BACKUP_BUCKET);
        assertThat(writeOrderArgumentCaptor.getValue().getFileBucketId()).isEqualTo(
            BucketTopologyHelper.BACKUP_FILE_BUCKET
        );
        assertThat(writeOrderArgumentCaptor.getValue().getSize()).isEqualTo(10240L);
        assertThat(writeOrderArgumentCaptor.getValue().getDigest()).isEqualTo(
            "4f68ecd1a386def8129359761b774e56207875be2da4e2d6b29c75a61b006243a183576111e9d26bdae3666319eb5add714e845bd16dde3a35eab39ae8cf7c9f"
        );
        assertThat(writeOrderArgumentCaptor.getValue().getMessageType()).isEqualTo(QueueMessageType.WriteBackupOrder);
        assertThat(writeOrderArgumentCaptor.getValue().getArchiveId()).isEqualTo(objectId);

        File backupFile = new File(temporaryFolder.getRoot(), writeOrderArgumentCaptor.getValue().getFilePath());
        assertThat(backupFile).exists();
        assertThat(backupFile).hasSameContentAs(PropertiesUtils.getResourceFile("backup.zip"));
    }
}

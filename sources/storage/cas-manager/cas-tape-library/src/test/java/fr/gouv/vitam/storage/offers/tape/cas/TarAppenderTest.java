package fr.gouv.vitam.storage.offers.tape.cas;

import fr.gouv.vitam.storage.engine.common.model.TarEntryDescription;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.input.NullInputStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static org.assertj.core.api.Assertions.assertThat;

public class TarAppenderTest {

    private static final String TAR_FILE_ID = "myTarFile.tar";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testAppender() throws IOException {

        // Given
        Path tarFilePath = temporaryFolder.getRoot().toPath().resolve(TAR_FILE_ID);
        TarAppender tarAppender = new TarAppender(tarFilePath, TAR_FILE_ID, 1_000_000L);
        byte[] data1 = "data1".getBytes();
        byte[] data2 = "data2".getBytes();
        byte[] data3 = "data3".getBytes();

        // When
        TarEntryDescription entry1 = tarAppender.append("entry1", new ByteArrayInputStream(data1), data1.length);
        TarEntryDescription entry2 = tarAppender.append("entry2", new ByteArrayInputStream(data2), data2.length);
        TarEntryDescription entry3 = tarAppender.append("entry3", new ByteArrayInputStream(data3), data3.length);
        tarAppender.fsync();
        tarAppender.close();

        // Then
        File tarFile = new File(temporaryFolder.getRoot(), TAR_FILE_ID);
        assertThat(tarFile).exists();
        checkEntryAtPos(tarFile, entry1, data1);
        checkEntryAtPos(tarFile, entry2, data2);
        checkEntryAtPos(tarFile, entry3, data3);
        assertThat(tarAppender.getEntryCount()).isEqualTo(3);
    }

    @Test
    public void testMaxEntrySize() throws IOException {

        // Given
        Path tarFilePath = temporaryFolder.getRoot().toPath().resolve(TAR_FILE_ID);
        TarAppender tarAppender = new TarAppender(tarFilePath, TAR_FILE_ID, 1_000_000L);

        // When / Then
        assertThat(tarAppender.canAppend(600_000L)).isTrue();
        tarAppender.append("entry1", new NullInputStream(600_000L), 600_000L);

        assertThat(tarAppender.canAppend(390_000L)).isTrue();
        tarAppender.append("entry2", new NullInputStream(390_000L), 390_000L);

        assertThat(tarAppender.canAppend(10_000L)).isFalse();
    }

    private void checkEntryAtPos(File tarFile, TarEntryDescription entryDescription, byte[] expectedData)
        throws IOException {

        try (SeekableByteChannel seekableByteChannel = Files
            .newByteChannel(tarFile.toPath(), StandardOpenOption.READ)) {

            seekableByteChannel.position(entryDescription.getStartPos());

            try (InputStream inputStream = Channels.newInputStream(seekableByteChannel);
                TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(inputStream)) {

                ArchiveEntry tarEntry = tarArchiveInputStream.getNextEntry();
                assertThat(tarEntry.getName()).isEqualTo(entryDescription.getEntryName());
                assertThat(tarEntry.getSize()).isEqualTo(expectedData.length);

                assertThat(tarArchiveInputStream).hasSameContentAs(new ByteArrayInputStream(expectedData));
            }
        }
    }
}

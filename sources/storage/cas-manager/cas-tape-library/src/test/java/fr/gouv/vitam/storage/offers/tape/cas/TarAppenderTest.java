package fr.gouv.vitam.storage.offers.tape.cas;

import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.model.tape.TarEntryDescription;
import org.apache.commons.io.input.NullInputStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;

import static fr.gouv.vitam.storage.offers.tape.cas.TarTestHelper.checkEntryAtPos;
import static org.assertj.core.api.Assertions.assertThat;

public class TarAppenderTest {

    private static final String TAR_FILE_ID = "myTarFile.tar";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testAppender() throws IOException {

        // Given
        Path tarFilePath = temporaryFolder.getRoot().toPath().resolve(TAR_FILE_ID);
        try (TarAppender tarAppender = new TarAppender(tarFilePath, TAR_FILE_ID, 1_000_000L)) {

            byte[] data1 = "data1".getBytes();
            byte[] data2 = "data2".getBytes();
            byte[] data3 = "data3".getBytes();

            // When
            TarEntryDescription entry1 = tarAppender.append("entry1", new ByteArrayInputStream(data1), data1.length);
            TarEntryDescription entry2 = tarAppender.append("entry2", new ByteArrayInputStream(data2), data2.length);
            TarEntryDescription entry3 = tarAppender.append("entry3", new ByteArrayInputStream(data3), data3.length);
            tarAppender.flush();

            // Then
            String digest1 = new Digest(VitamConfiguration.getDefaultDigestType()).update(data1).digestHex();
            String digest2 = new Digest(VitamConfiguration.getDefaultDigestType()).update(data2).digestHex();
            String digest3 = new Digest(VitamConfiguration.getDefaultDigestType()).update(data3).digestHex();
            assertThat(entry1.getDigestValue()).isEqualTo(digest1);
            assertThat(entry2.getDigestValue()).isEqualTo(digest2);
            assertThat(entry3.getDigestValue()).isEqualTo(digest3);

            checkEntryAtPos(tarFilePath, entry1);
            checkEntryAtPos(tarFilePath, entry2);
            checkEntryAtPos(tarFilePath, entry3);
            assertThat(tarAppender.getEntryCount()).isEqualTo(3);

        }
    }

    @Test
    public void testMaxEntrySize() throws IOException {

        // Given
        Path tarFilePath = temporaryFolder.getRoot().toPath().resolve(TAR_FILE_ID);
        try (TarAppender tarAppender = new TarAppender(tarFilePath, TAR_FILE_ID, 1_000_000L)) {

            // When / Then
            assertThat(tarAppender.canAppend(600_000L)).isTrue();
            tarAppender.append("entry1", new NullInputStream(600_000L), 600_000L);

            assertThat(tarAppender.canAppend(390_000L)).isTrue();
            tarAppender.append("entry2", new NullInputStream(390_000L), 390_000L);

            assertThat(tarAppender.canAppend(10_000L)).isFalse();
        }
    }
}

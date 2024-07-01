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

import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.storage.engine.common.model.TarEntryDescription;
import fr.gouv.vitam.storage.offers.tape.exception.ObjectReferentialException;
import org.apache.commons.collections4.OrderedMap;
import org.apache.commons.collections4.map.ListOrderedMap;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarConstants;
import org.apache.commons.io.input.CloseShieldInputStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static fr.gouv.vitam.storage.offers.tape.cas.TarTestHelper.checkEntryAtPos;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class TarFileRapairerTest {

    private static final String TAR_FILE_ID = "myTarFile.tar";
    private static final String TARGET_TAR_FILE_ID = "myRepairedTarFile.tar";
    public static final int FOOTER_PADDING_SIZE = TarConstants.DEFAULT_RCDSIZE * 2;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    TarFileDigestVerifier tarFileDigestVerifier;

    @Test
    public void repairAndValidateEmptyTarFileWithoutFooter() throws Exception {
        // Given
        TarFileRapairer tarFileRapairer = new TarFileRapairer(() -> tarFileDigestVerifier);

        Path tarFilePath = temporaryFolder.getRoot().toPath().resolve(TAR_FILE_ID);
        try (TarAppender tarAppender = new TarAppender(tarFilePath, TAR_FILE_ID, 1_000_000L)) {
            tarAppender.flush();
            // No close
            // tarAppender.close();

            // When
            Path repairedTarFilePath = temporaryFolder.getRoot().toPath().resolve(TARGET_TAR_FILE_ID);
            TarFileRapairer.DigestWithSize digestWithSize = repairAndVerify(
                tarFilePath,
                tarFileRapairer,
                repairedTarFilePath
            );

            // Then
            assertThat(tarFilePath.toFile().length()).isEqualTo(0);
            verifyTarContent(repairedTarFilePath, emptyMap(), emptyMap(), digestWithSize);
        }
    }

    @Test
    public void repairAndValidateEmptyTarFile() throws Exception {
        // Given
        TarFileRapairer tarFileRapairer = new TarFileRapairer(() -> tarFileDigestVerifier);

        Path tarFilePath = temporaryFolder.getRoot().toPath().resolve(TAR_FILE_ID);
        try (TarAppender tarAppender = new TarAppender(tarFilePath, TAR_FILE_ID, 1_000_000L)) {
            tarAppender.close();

            // When
            Path repairedTarFilePath = temporaryFolder.getRoot().toPath().resolve(TARGET_TAR_FILE_ID);
            TarFileRapairer.DigestWithSize digestWithSize = repairAndVerify(
                tarFilePath,
                tarFileRapairer,
                repairedTarFilePath
            );

            // Then
            assertThat(tarFilePath.toFile().length()).isEqualTo(FOOTER_PADDING_SIZE);
            verifyTarContent(repairedTarFilePath, emptyMap(), emptyMap(), digestWithSize);
        }
    }

    @Test
    public void repairAndValidateSingleEntryTarFile() throws Exception {
        // Given
        TarFileRapairer tarFileRapairer = new TarFileRapairer(() -> tarFileDigestVerifier);

        OrderedMap<String, byte[]> entries = new ListOrderedMap<>();
        entries.put("entry1", "test data".getBytes());

        Path tarFilePath = temporaryFolder.getRoot().toPath().resolve(TAR_FILE_ID);
        try (TarAppender tarAppender = new TarAppender(tarFilePath, TAR_FILE_ID, 1_000_000L)) {
            Map<String, TarEntryDescription> entryDescriptions = appendEntries(entries, tarAppender);
            tarAppender.close();

            // When
            Path repairedTarFilePath = temporaryFolder.getRoot().toPath().resolve(TARGET_TAR_FILE_ID);
            TarFileRapairer.DigestWithSize digestWithSize = repairAndVerify(
                tarFilePath,
                tarFileRapairer,
                repairedTarFilePath
            );

            // Then
            verifyTarContent(repairedTarFilePath, entries, entryDescriptions, digestWithSize);
        }
    }

    @Test
    public void repairAndValidateSingleMultipleEntries() throws Exception {
        // Given
        TarFileRapairer tarFileRapairer = new TarFileRapairer(() -> tarFileDigestVerifier);

        OrderedMap<String, byte[]> entries = new ListOrderedMap<>();
        entries.put("entry1", "test data".getBytes());
        entries.put("entry2", "test data 2".getBytes());
        entries.put("entry3", "another test data".getBytes());

        Path tarFilePath = temporaryFolder.getRoot().toPath().resolve(TAR_FILE_ID);
        try (TarAppender tarAppender = new TarAppender(tarFilePath, TAR_FILE_ID, 1_000_000L)) {
            Map<String, TarEntryDescription> entryDescriptions = appendEntries(entries, tarAppender);
            tarAppender.close();

            // When
            Path repairedTarFilePath = temporaryFolder.getRoot().toPath().resolve(TARGET_TAR_FILE_ID);
            TarFileRapairer.DigestWithSize digestWithSize = repairAndVerify(
                tarFilePath,
                tarFileRapairer,
                repairedTarFilePath
            );

            // Then
            verifyTarContent(repairedTarFilePath, entries, entryDescriptions, digestWithSize);
        }
    }

    @Test
    public void repairAndValidateSingleEntryWithoutFooter() throws Exception {
        // Given
        TarFileRapairer tarFileRapairer = new TarFileRapairer(() -> tarFileDigestVerifier);

        OrderedMap<String, byte[]> entries = new ListOrderedMap<>();
        entries.put("entry1", "test data".getBytes());

        Path tarFilePath = temporaryFolder.getRoot().toPath().resolve(TAR_FILE_ID);
        try (TarAppender tarAppender = new TarAppender(tarFilePath, TAR_FILE_ID, 1_000_000L)) {
            Map<String, TarEntryDescription> entryDescriptions = appendEntries(entries, tarAppender);
            tarAppender.flush();
            // No close
            // tarAppender.close();

            // When
            Path repairedTarFilePath = temporaryFolder.getRoot().toPath().resolve(TARGET_TAR_FILE_ID);
            TarFileRapairer.DigestWithSize digestWithSize = repairAndVerify(
                tarFilePath,
                tarFileRapairer,
                repairedTarFilePath
            );

            // Then
            verifyTarContent(repairedTarFilePath, entries, entryDescriptions, digestWithSize);
        }
    }

    @Test
    public void repairAndValidateIncompleteFirstEntryHeader() throws Exception {
        // Given
        TarFileRapairer tarFileRapairer = new TarFileRapairer(() -> tarFileDigestVerifier);

        OrderedMap<String, byte[]> entries = new ListOrderedMap<>();
        entries.put("entry1", "test data".getBytes());

        Path tarFilePath = temporaryFolder.getRoot().toPath().resolve(TAR_FILE_ID);
        try (TarAppender tarAppender = new TarAppender(tarFilePath, TAR_FILE_ID, 1_000_000L)) {
            appendEntries(entries, tarAppender);
            tarAppender.close();

            // Truncate entry header
            truncateFile(tarFilePath, 100);

            // When
            Path repairedTarFilePath = temporaryFolder.getRoot().toPath().resolve(TARGET_TAR_FILE_ID);
            TarFileRapairer.DigestWithSize digestWithSize = repairAndVerify(
                tarFilePath,
                tarFileRapairer,
                repairedTarFilePath
            );

            // Then
            verifyTarContent(repairedTarFilePath, emptyMap(), emptyMap(), digestWithSize);
        }
    }

    @Test
    public void repairAndValidateIncompleteFirstEntryContent() throws Exception {
        // Given
        TarFileRapairer tarFileRapairer = new TarFileRapairer(() -> tarFileDigestVerifier);

        OrderedMap<String, byte[]> entries = new ListOrderedMap<>();
        entries.put("entry1", "test data".getBytes());

        Path tarFilePath = temporaryFolder.getRoot().toPath().resolve(TAR_FILE_ID);
        try (TarAppender tarAppender = new TarAppender(tarFilePath, TAR_FILE_ID, 1_000_000L)) {
            Map<String, TarEntryDescription> entryDescriptions = appendEntries(entries, tarAppender);
            tarAppender.close();

            // Truncate entry content
            truncateFile(tarFilePath, TarConstants.DEFAULT_RCDSIZE + 5);

            // When
            Path repairedTarFilePath = temporaryFolder.getRoot().toPath().resolve(TARGET_TAR_FILE_ID);
            TarFileRapairer.DigestWithSize digestWithSize = repairAndVerify(
                tarFilePath,
                tarFileRapairer,
                repairedTarFilePath
            );

            // Then
            verifyTarContent(repairedTarFilePath, emptyMap(), emptyMap(), digestWithSize);
        }
    }

    @Test
    public void repairAndValidateIncompleteFirstEntryContentPadding() throws Exception {
        // Given
        TarFileRapairer tarFileRapairer = new TarFileRapairer(() -> tarFileDigestVerifier);

        OrderedMap<String, byte[]> entries = new ListOrderedMap<>();
        entries.put("entry1", "test data".getBytes());

        Path tarFilePath = temporaryFolder.getRoot().toPath().resolve(TAR_FILE_ID);
        try (TarAppender tarAppender = new TarAppender(tarFilePath, TAR_FILE_ID, 1_000_000L)) {
            Map<String, TarEntryDescription> entryDescriptions = appendEntries(entries, tarAppender);
            tarAppender.close();

            // Truncate entry content
            truncateFile(tarFilePath, tarFilePath.toFile().length() - FOOTER_PADDING_SIZE - 10);

            // When
            Path repairedTarFilePath = temporaryFolder.getRoot().toPath().resolve(TARGET_TAR_FILE_ID);
            TarFileRapairer.DigestWithSize digestWithSize = repairAndVerify(
                tarFilePath,
                tarFileRapairer,
                repairedTarFilePath
            );

            // Then
            verifyTarContent(repairedTarFilePath, entries, entryDescriptions, digestWithSize);
        }
    }

    @Test
    public void repairAndValidateMultipleEntriesWithoutFooter() throws Exception {
        // Given
        TarFileRapairer tarFileRapairer = new TarFileRapairer(() -> tarFileDigestVerifier);

        OrderedMap<String, byte[]> entries = new ListOrderedMap<>();
        entries.put("entry1", "test data".getBytes());
        entries.put("entry2", "test data 2".getBytes());
        entries.put("entry3", "another test data".getBytes());

        Path tarFilePath = temporaryFolder.getRoot().toPath().resolve(TAR_FILE_ID);
        try (TarAppender tarAppender = new TarAppender(tarFilePath, TAR_FILE_ID, 1_000_000L)) {
            Map<String, TarEntryDescription> entryDescriptions = appendEntries(entries, tarAppender);
            tarAppender.flush();
            // No close
            // tarAppender.close();

            // When
            Path repairedTarFilePath = temporaryFolder.getRoot().toPath().resolve(TARGET_TAR_FILE_ID);
            TarFileRapairer.DigestWithSize digestWithSize = repairAndVerify(
                tarFilePath,
                tarFileRapairer,
                repairedTarFilePath
            );

            // Then
            verifyTarContent(repairedTarFilePath, entries, entryDescriptions, digestWithSize);
        }
    }

    @Test
    public void repairAndValidateIncompleteLastEntryHeader() throws Exception {
        // Given
        TarFileRapairer tarFileRapairer = new TarFileRapairer(() -> tarFileDigestVerifier);

        OrderedMap<String, byte[]> entries = new ListOrderedMap<>();
        entries.put("entry1", "test data".getBytes());
        entries.put("entry2", "test data 2".getBytes());
        entries.put("entry3", "another test data".getBytes());

        Path tarFilePath = temporaryFolder.getRoot().toPath().resolve(TAR_FILE_ID);
        try (TarAppender tarAppender = new TarAppender(tarFilePath, TAR_FILE_ID, 1_000_000L)) {
            Map<String, TarEntryDescription> entryDescriptions = appendEntries(entries, tarAppender);
            tarAppender.close();

            // Truncate last entry header
            truncateFile(tarFilePath, entryDescriptions.get("entry3").getStartPos() + 10);

            // When
            Path repairedTarFilePath = temporaryFolder.getRoot().toPath().resolve(TARGET_TAR_FILE_ID);
            TarFileRapairer.DigestWithSize digestWithSize = repairAndVerify(
                tarFilePath,
                tarFileRapairer,
                repairedTarFilePath
            );

            // Then
            ListOrderedMap<String, byte[]> expectedEntries = new ListOrderedMap<>();
            expectedEntries.putAll(entries);
            expectedEntries.remove(entries.lastKey());
            Map<String, TarEntryDescription> expectedEntryDescriptions = new ListOrderedMap<>();
            expectedEntryDescriptions.putAll(entryDescriptions);
            expectedEntryDescriptions.remove(entries.lastKey());

            verifyTarContent(repairedTarFilePath, expectedEntries, expectedEntryDescriptions, digestWithSize);
        }
    }

    @Test
    public void repairAndValidateIncompleteLastEntryContent() throws Exception {
        // Given
        TarFileRapairer tarFileRapairer = new TarFileRapairer(() -> tarFileDigestVerifier);

        OrderedMap<String, byte[]> entries = new ListOrderedMap<>();
        entries.put("entry1", "test data".getBytes());
        entries.put("entry2", "test data 2".getBytes());
        entries.put("entry3", "another test data".getBytes());

        Path tarFilePath = temporaryFolder.getRoot().toPath().resolve(TAR_FILE_ID);
        try (TarAppender tarAppender = new TarAppender(tarFilePath, TAR_FILE_ID, 1_000_000L)) {
            Map<String, TarEntryDescription> entryDescriptions = appendEntries(entries, tarAppender);
            tarAppender.close();

            // Truncate last entry content
            truncateFile(tarFilePath, entryDescriptions.get("entry3").getStartPos() + TarConstants.DEFAULT_RCDSIZE + 5);

            // When
            Path repairedTarFilePath = temporaryFolder.getRoot().toPath().resolve(TARGET_TAR_FILE_ID);
            TarFileRapairer.DigestWithSize digestWithSize = repairAndVerify(
                tarFilePath,
                tarFileRapairer,
                repairedTarFilePath
            );

            // Then
            ListOrderedMap<String, byte[]> expectedEntries = new ListOrderedMap<>();
            expectedEntries.putAll(entries);
            expectedEntries.remove(entries.lastKey());
            Map<String, TarEntryDescription> expectedEntryDescriptions = new ListOrderedMap<>();
            expectedEntryDescriptions.putAll(entryDescriptions);
            expectedEntryDescriptions.remove(entries.lastKey());

            verifyTarContent(repairedTarFilePath, expectedEntries, expectedEntryDescriptions, digestWithSize);
        }
    }

    @Test
    public void repairAndValidateIncompleteLastEntryContentPadding() throws Exception {
        // Given
        TarFileRapairer tarFileRapairer = new TarFileRapairer(() -> tarFileDigestVerifier);

        OrderedMap<String, byte[]> entries = new ListOrderedMap<>();
        entries.put("entry1", "test data".getBytes());
        entries.put("entry2", "test data 2".getBytes());
        entries.put("entry3", "another test data".getBytes());

        Path tarFilePath = temporaryFolder.getRoot().toPath().resolve(TAR_FILE_ID);
        try (TarAppender tarAppender = new TarAppender(tarFilePath, TAR_FILE_ID, 1_000_000L)) {
            Map<String, TarEntryDescription> entryDescriptions = appendEntries(entries, tarAppender);
            tarAppender.close();

            // Truncate last entry content padding
            truncateFile(tarFilePath, tarFilePath.toFile().length() - FOOTER_PADDING_SIZE - 10);

            // When
            Path repairedTarFilePath = temporaryFolder.getRoot().toPath().resolve(TARGET_TAR_FILE_ID);
            TarFileRapairer.DigestWithSize digestWithSize = repairAndVerify(
                tarFilePath,
                tarFileRapairer,
                repairedTarFilePath
            );

            // Then
            verifyTarContent(repairedTarFilePath, entries, entryDescriptions, digestWithSize);
        }
    }

    private Map<String, TarEntryDescription> appendEntries(OrderedMap<String, byte[]> entries, TarAppender tarAppender)
        throws IOException {
        Map<String, TarEntryDescription> entryDescriptions = new HashMap<>();
        for (String entryName : entries.keySet()) {
            TarEntryDescription entryDescription = tarAppender.append(
                entryName,
                new ByteArrayInputStream(entries.get(entryName)),
                entries.get(entryName).length
            );
            entryDescriptions.put(entryName, entryDescription);
        }
        return entryDescriptions;
    }

    private TarFileRapairer.DigestWithSize repairAndVerify(
        Path tarFilePath,
        TarFileRapairer tarFileRapairer,
        Path repairedTarFilePath
    ) throws IOException, ObjectReferentialException {
        TarFileRapairer.DigestWithSize digestWithSize;
        try (
            InputStream inputStream = Files.newInputStream(tarFilePath);
            OutputStream outputStream = Files.newOutputStream(repairedTarFilePath)
        ) {
            digestWithSize = tarFileRapairer.repairAndVerifyTarArchive(inputStream, outputStream, TAR_FILE_ID);
            outputStream.flush();
        }
        return digestWithSize;
    }

    private void verifyTarContent(
        Path repairedTarFilePath,
        Map<String, byte[]> entries,
        Map<String, TarEntryDescription> entryDescriptions,
        TarFileRapairer.DigestWithSize digestWithSize
    ) throws IOException, ObjectReferentialException {
        assertThat(digestWithSize.getSize()).isEqualTo(repairedTarFilePath.toFile().length());
        assertThat(digestWithSize.getDigestValue()).isEqualTo(
            new Digest(DigestType.SHA512).update(repairedTarFilePath.toFile()).digestHex()
        );

        verifyTarContent(repairedTarFilePath, entries);

        for (String entryName : entryDescriptions.keySet()) {
            checkEntryAtPos(repairedTarFilePath, entryDescriptions.get(entryName));
        }

        for (String entryName : entryDescriptions.keySet()) {
            String expectedDigest = new Digest(DigestType.SHA512).update(entries.get(entryName)).digestHex();
            verify(tarFileDigestVerifier).addDigestToCheck(entryName, expectedDigest);
        }

        verify(tarFileDigestVerifier).finalizeChecks();
        verifyNoMoreInteractions(tarFileDigestVerifier);

        int expectedSize = 0;
        for (byte[] entryContent : entries.values()) {
            // Add header record + content records padded to record size
            expectedSize +=
            TarConstants.DEFAULT_RCDSIZE +
            ((entryContent.length + TarConstants.DEFAULT_RCDSIZE - 1) / TarConstants.DEFAULT_RCDSIZE) *
                TarConstants.DEFAULT_RCDSIZE;
        }
        // Add footer records (2 0-filled records)
        expectedSize += FOOTER_PADDING_SIZE;
        assertThat(repairedTarFilePath.toFile().length()).isEqualTo(expectedSize);
    }

    private void verifyTarContent(Path repairedTarFilePath, Map<String, byte[]> entries) throws IOException {
        try (
            TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(
                Files.newInputStream(repairedTarFilePath)
            )
        ) {
            for (String entryName : entries.keySet()) {
                InputStream expectedContent = new ByteArrayInputStream(entries.get(entryName));

                TarArchiveEntry nextTarEntry = tarArchiveInputStream.getNextTarEntry();
                assertThat(nextTarEntry.getName()).isEqualTo(entryName);
                assertThat(CloseShieldInputStream.wrap(tarArchiveInputStream)).hasSameContentAs(expectedContent);
            }

            assertThat(tarArchiveInputStream.getNextTarEntry()).isNull();
        }
    }

    private void truncateFile(Path filePath, long newSize) throws IOException {
        try (FileChannel outChan = new FileOutputStream(filePath.toFile(), true).getChannel()) {
            outChan.truncate(newSize);
        }
    }
}

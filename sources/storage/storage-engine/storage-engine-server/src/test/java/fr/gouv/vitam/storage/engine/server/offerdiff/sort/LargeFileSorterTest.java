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

package fr.gouv.vitam.storage.engine.server.offerdiff.sort;

import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.storage.ObjectEntry;
import org.apache.commons.collections4.IteratorUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

public class LargeFileSorterTest {

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    private LargeFileSorter<ObjectEntry> instance;
    private Function<File, LargeFileReader<ObjectEntry>> fileReaderFactory;
    private Function<File, LargeFileWriter<ObjectEntry>> fileWriterFactory;
    private int tmpFileCounter = 0;

    @Before
    public void initialize() throws IOException {
        tempFolder.create();

        fileReaderFactory = ObjectEntryLargeFileReader::new;
        fileWriterFactory = ObjectEntryLargeFileWriter::new;

        Supplier<File> tempFileCreator = () -> {
            try {
                this.tmpFileCounter++;
                return tempFolder.newFile();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };

        instance = new LargeFileSorter<>(
            10,
            5,
            fileReaderFactory,
            fileWriterFactory,
            Comparator.comparing(ObjectEntry::getObjectId),
            tempFileCreator
        );
    }

    @After
    public void cleanup() {
        tempFolder.delete();
    }

    @Test
    public void testEmpty() throws IOException {
        // Given input file with 0 entries
        File inputFile = tempFolder.newFile();
        try (LargeFileWriter<ObjectEntry> writer = fileWriterFactory.apply(inputFile)) {}

        // When
        File sortedFile = instance.sortLargeFile(inputFile);

        // Then
        try (LargeFileReader<ObjectEntry> reader = fileReaderFactory.apply(sortedFile)) {
            assertThat(reader).isEmpty();
        }
        assertThat(tmpFileCounter).isEqualTo(1);
        ensureAllTempFileCleaned(inputFile, sortedFile);
    }

    @Test
    public void testSingleEntry() throws IOException {
        // Given input file with 0 entries
        File inputFile = tempFolder.newFile();
        try (LargeFileWriter<ObjectEntry> writer = fileWriterFactory.apply(inputFile)) {
            writer.writeEntry(new ObjectEntry().setObjectId("obj1").setSize(1L));
        }

        // When
        File sortedFile = instance.sortLargeFile(inputFile);

        // Then
        try (LargeFileReader<ObjectEntry> reader = fileReaderFactory.apply(sortedFile)) {
            List<ObjectEntry> objectEntries = IteratorUtils.toList(reader);
            assertThat(objectEntries).hasSize(1);
            assertThat(objectEntries.get(0).getObjectId()).isEqualTo("obj1");
            assertThat(objectEntries.get(0).getSize()).isEqualTo(1L);
        }
        assertThat(tmpFileCounter).isEqualTo(1);
        ensureAllTempFileCleaned(inputFile, sortedFile);
    }

    @Test
    public void testSortAlreadySortedLargeFiles() throws IOException {
        // Given input file with 1000 sorted entries
        File inputFile = tempFolder.newFile();

        List<ObjectEntry> sortedDataSet = IntStream.range(0, 1000)
            .mapToObj(i -> new ObjectEntry().setObjectId("obj" + i).setSize(i))
            .sorted(Comparator.comparing(ObjectEntry::getObjectId))
            .collect(Collectors.toList());

        try (LargeFileWriter<ObjectEntry> writer = fileWriterFactory.apply(inputFile)) {
            // Append
            for (ObjectEntry objectEntry : sortedDataSet) {
                writer.writeEntry(objectEntry);
            }
        }

        // When
        File sortedFile = instance.sortLargeFile(inputFile);

        // Then
        try (LargeFileReader<ObjectEntry> reader = fileReaderFactory.apply(sortedFile)) {
            List<ObjectEntry> objectEntries = IteratorUtils.toList(reader);

            assertThat(JsonHandler.unprettyPrint(objectEntries)).isEqualTo(JsonHandler.unprettyPrint(sortedDataSet));
        }
        assertThat(tmpFileCounter).isGreaterThan(100);
        ensureAllTempFileCleaned(inputFile, sortedFile);
    }

    @Test
    public void testSortShuffledLargeFiles() throws IOException {
        // Given input file with 1000 shuffled entries
        File inputFile = tempFolder.newFile();

        List<ObjectEntry> dataSet = IntStream.range(0, 1000)
            .mapToObj(i -> new ObjectEntry().setObjectId("obj" + i).setSize(i))
            .collect(Collectors.toList());

        try (LargeFileWriter<ObjectEntry> writer = fileWriterFactory.apply(inputFile)) {
            // Shuffle
            List<ObjectEntry> shuffled = new ArrayList<>(dataSet);
            Collections.shuffle(shuffled);

            // Append
            for (ObjectEntry objectEntry : shuffled) {
                writer.writeEntry(objectEntry);
            }
        }

        // When
        File sortedFile = instance.sortLargeFile(inputFile);

        // Then
        try (LargeFileReader<ObjectEntry> reader = fileReaderFactory.apply(sortedFile)) {
            List<ObjectEntry> objectEntries = IteratorUtils.toList(reader);

            // Expected sorted entries
            List<ObjectEntry> expectedEntrySet = new ArrayList<>(dataSet);
            expectedEntrySet.sort(Comparator.comparing(ObjectEntry::getObjectId));

            assertThat(JsonHandler.unprettyPrint(objectEntries)).isEqualTo(JsonHandler.unprettyPrint(expectedEntrySet));
        }
        assertThat(tmpFileCounter).isGreaterThan(100);
        ensureAllTempFileCleaned(inputFile, sortedFile);
    }

    private void ensureAllTempFileCleaned(File remainingInputFile, File remainingFinalSortedFile) {
        assertThat(
            Arrays.stream(Objects.requireNonNull(tempFolder.getRoot().listFiles()))
                .map(file -> file.getAbsoluteFile().toString())
                .collect(Collectors.toList())
        ).containsExactlyInAnyOrder(
            remainingInputFile.getAbsoluteFile().toString(),
            remainingFinalSortedFile.getAbsoluteFile().toString()
        );
    }
}

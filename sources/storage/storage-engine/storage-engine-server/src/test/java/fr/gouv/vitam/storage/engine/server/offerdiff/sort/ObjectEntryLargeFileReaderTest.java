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

import fr.gouv.vitam.common.model.storage.ObjectEntry;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class ObjectEntryLargeFileReaderTest {

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testEmpty() throws IOException {
        // Given
        File file = tempFolder.newFile();
        try (ObjectEntryLargeFileWriter writer = new ObjectEntryLargeFileWriter(file)) {
            // No entries
        }

        // When
        try (ObjectEntryLargeFileReader reader = new ObjectEntryLargeFileReader(file)) {
            // Then
            assertThat(reader).isEmpty();
        }
    }

    @Test
    public void testSingleEntry() throws IOException {
        // Given
        File file = tempFolder.newFile();
        try (ObjectEntryLargeFileWriter writer = new ObjectEntryLargeFileWriter(file)) {
            writer.writeEntry(new ObjectEntry().setObjectId("obj1").setSize(1L));
        }

        // When
        try (ObjectEntryLargeFileReader reader = new ObjectEntryLargeFileReader(file)) {
            // Then
            assertThat(reader)
                .extracting(ObjectEntry::getObjectId, ObjectEntry::getSize)
                .isEqualTo(Arrays.asList(tuple("obj1", 1L)));
        }
    }

    @Test
    public void testMultiEntries() throws IOException {
        // Given
        File file = tempFolder.newFile();
        try (ObjectEntryLargeFileWriter writer = new ObjectEntryLargeFileWriter(file)) {
            writer.writeEntry(new ObjectEntry().setObjectId("obj1").setSize(1L));
            writer.writeEntry(new ObjectEntry().setObjectId("obj2").setSize(2L));
        }

        // When
        try (ObjectEntryLargeFileReader reader = new ObjectEntryLargeFileReader(file)) {
            // Then
            assertThat(reader)
                .extracting(ObjectEntry::getObjectId, ObjectEntry::getSize)
                .isEqualTo(Arrays.asList(tuple("obj1", 1L), tuple("obj2", 2L)));
        }
    }
}

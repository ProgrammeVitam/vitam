/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.model.storage;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.Test;

import java.io.IOException;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ObjectEntryReaderTest {

    @Test
    public void testEmptyWithEof() throws IOException {
        // Given
        ByteArrayOutputStream innerStream = new ByteArrayOutputStream();
        try (ObjectEntryWriter writer = new ObjectEntryWriter(innerStream)) {
            writer.writeEof();
        }

        // When // Then
        try (ObjectEntryReader reader = new ObjectEntryReader(innerStream.toInputStream())) {
            assertThat(reader.hasNext()).isFalse();
        }
    }

    @Test
    public void testEmptyWithoutEof() throws IOException {
        // Given
        ByteArrayOutputStream innerStream = new ByteArrayOutputStream();
        try (ObjectEntryWriter writer = new ObjectEntryWriter(innerStream)) {
            // NO EOF
        }

        // When // Then
        try (ObjectEntryReader reader = new ObjectEntryReader(innerStream.toInputStream())) {
            assertThatThrownBy(reader::hasNext)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Premature EOF");

            assertThatThrownBy(reader::next).isInstanceOf(RuntimeException.class).hasMessageContaining("Premature EOF");
        }
    }

    @Test
    public void testEntriesWithEof() throws IOException {
        // Given
        ByteArrayOutputStream innerStream = new ByteArrayOutputStream();
        try (ObjectEntryWriter writer = new ObjectEntryWriter(innerStream)) {
            writer.write(new ObjectEntry("file1", 10L));
            writer.write(new ObjectEntry("file2", 20L));
            writer.writeEof();
        }

        // When / Then
        try (ObjectEntryReader reader = new ObjectEntryReader(innerStream.toInputStream())) {
            assertThat(reader.hasNext()).isTrue();
            assertThat(reader.next())
                .extracting(ObjectEntry::getObjectId, ObjectEntry::getSize)
                .isEqualTo(asList("file1", 10L));
            assertThat(reader.hasNext()).isTrue();
            assertThat(reader.next())
                .extracting(ObjectEntry::getObjectId, ObjectEntry::getSize)
                .isEqualTo(asList("file2", 20L));
            assertThat(reader.hasNext()).isFalse();
        }
    }

    @Test
    public void testEntriesWithoutEof() throws IOException {
        // Given
        ByteArrayOutputStream innerStream = new ByteArrayOutputStream();
        try (ObjectEntryWriter writer = new ObjectEntryWriter(innerStream)) {
            writer.write(new ObjectEntry("file1", 10L));
            writer.write(new ObjectEntry("file2", 20L));
            // No EOF
        }

        // When / Then
        try (ObjectEntryReader reader = new ObjectEntryReader(innerStream.toInputStream())) {
            assertThat(reader.hasNext()).isTrue();
            assertThat(reader.next())
                .extracting(ObjectEntry::getObjectId, ObjectEntry::getSize)
                .isEqualTo(asList("file1", 10L));
            assertThat(reader.hasNext()).isTrue();
            assertThat(reader.next())
                .extracting(ObjectEntry::getObjectId, ObjectEntry::getSize)
                .isEqualTo(asList("file2", 20L));

            assertThatThrownBy(reader::hasNext)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Premature EOF");

            assertThatThrownBy(reader::next).isInstanceOf(RuntimeException.class).hasMessageContaining("Premature EOF");
        }
    }
}

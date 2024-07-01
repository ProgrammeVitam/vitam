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

import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.storage.engine.common.model.TarEntryDescription;
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

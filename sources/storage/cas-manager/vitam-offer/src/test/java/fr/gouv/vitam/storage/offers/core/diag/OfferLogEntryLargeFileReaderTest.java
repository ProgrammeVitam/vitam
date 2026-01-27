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

package fr.gouv.vitam.storage.offers.core.diag;

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.tmp.TempFolderRule;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.OfferLogAction;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class OfferLogEntryLargeFileReaderTest {

    @Rule
    public TempFolderRule tempFolder = new TempFolderRule();

    @Test
    public void testEmptyFileThenHasNotNextEntry() throws IOException {
        File emptyFile = tempFolder.newFile();
        try (OfferLogEntryLargeFileReader reader = new OfferLogEntryLargeFileReader(emptyFile)) {
            assertThat(reader.hasNext()).isFalse();
            assertThatThrownBy(reader::next).isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    public void testSingleEntryFileThenParseEntry() throws IOException {
        File file = tempFolder.newFile();
        OfferLog writtenEntry1 = new OfferLog(1234L, LocalDateUtil.now(), "container", "obj1", OfferLogAction.WRITE);
        try (OfferLogEntryLargeFileWriter writer = new OfferLogEntryLargeFileWriter(file)) {
            writer.writeEntry(writtenEntry1);
        }

        try (OfferLogEntryLargeFileReader reader = new OfferLogEntryLargeFileReader(file)) {
            assertThat(reader.hasNext()).isTrue();
            OfferLog readEntry1 = reader.next();
            assertThat(readEntry1).usingRecursiveComparison().isEqualTo(writtenEntry1);

            assertThat(reader.hasNext()).isFalse();
        }
    }

    @Test
    public void testMultipleEntryFileThenParseEntry() throws IOException {
        File file = tempFolder.newFile();
        OfferLog writtenEntry1 = new OfferLog(1234L, LocalDateUtil.now(), "container1", "obj1", OfferLogAction.WRITE);
        OfferLog writtenEntry2 = new OfferLog(1235L, LocalDateUtil.now(), "container2", "obj2", OfferLogAction.DELETE);
        OfferLog writtenEntry3 = new OfferLog(1236L, LocalDateUtil.now(), "container3", "obj3", OfferLogAction.WRITE);
        try (OfferLogEntryLargeFileWriter writer = new OfferLogEntryLargeFileWriter(file)) {
            writer.writeEntry(writtenEntry1);
            writer.writeEntry(writtenEntry2);
            writer.writeEntry(writtenEntry3);
        }

        try (OfferLogEntryLargeFileReader reader = new OfferLogEntryLargeFileReader(file)) {
            assertThat(reader.hasNext()).isTrue();
            OfferLog readEntry1 = reader.next();
            assertThat(reader.hasNext()).isTrue();
            OfferLog readEntry2 = reader.next();
            assertThat(reader.hasNext()).isTrue();
            OfferLog readEntry3 = reader.next();
            assertThat(reader.hasNext()).isFalse();

            assertThat(readEntry1).usingRecursiveComparison().isEqualTo(writtenEntry1);
            assertThat(readEntry2).usingRecursiveComparison().isEqualTo(writtenEntry2);
            assertThat(readEntry3).usingRecursiveComparison().isEqualTo(writtenEntry3);
        }
    }
}

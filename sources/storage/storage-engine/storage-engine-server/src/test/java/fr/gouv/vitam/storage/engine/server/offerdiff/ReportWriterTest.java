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
package fr.gouv.vitam.storage.engine.server.offerdiff;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

public class ReportWriterTest {

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testEmptyReport() throws Exception {
        // Given
        File reportFile = tempFolder.newFile();

        // When
        ReportWriter reportWriter = new ReportWriter(reportFile);
        reportWriter.close();

        // Then
        assertThat(reportFile.length()).isEqualTo(0L);
        assertThat(reportWriter.getErrorCount()).isEqualTo(0L);
        assertThat(reportWriter.getTotalObjectCount()).isEqualTo(0L);
    }

    @Test
    public void testSuccessReport() throws Exception {
        // Given
        File reportFile = tempFolder.newFile();

        // When
        ReportWriter reportWriter = new ReportWriter(reportFile);
        reportWriter.reportMatchingObject("obj1");
        reportWriter.reportMatchingObject("obj2");
        reportWriter.close();

        // Then
        assertThat(reportFile.length()).isEqualTo(0L);
        assertThat(reportWriter.getErrorCount()).isEqualTo(0L);
        assertThat(reportWriter.getTotalObjectCount()).isEqualTo(2L);
    }

    @Test
    public void testSingleEntryReport() throws Exception {
        // Given
        File reportFile = tempFolder.newFile();

        // When
        ReportWriter reportWriter = new ReportWriter(reportFile);
        reportWriter.reportObjectMismatch("obj1", 10L, 20L);
        reportWriter.close();

        // Then
        assertThat(reportFile).hasContent("{\"objectId\":\"obj1\",\"sizeInOffer1\":10,\"sizeInOffer2\":20}");
        assertThat(reportWriter.getErrorCount()).isEqualTo(1L);
        assertThat(reportWriter.getTotalObjectCount()).isEqualTo(1L);
    }

    @Test
    public void testMultiEntryReport() throws Exception {
        // Given
        File reportFile = tempFolder.newFile();

        // When
        ReportWriter reportWriter = new ReportWriter(reportFile);
        reportWriter.reportObjectMismatch("obj1", 10L, 20L);
        reportWriter.reportMatchingObject("obj2");
        reportWriter.reportObjectMismatch("obj3", null, 20L);
        reportWriter.reportObjectMismatch("obj4", 10L, null);
        reportWriter.reportMatchingObject("obj5");
        reportWriter.close();

        // Then
        assertThat(reportFile).hasContent(
            "" +
            "{\"objectId\":\"obj1\",\"sizeInOffer1\":10,\"sizeInOffer2\":20}\n" +
            "{\"objectId\":\"obj3\",\"sizeInOffer1\":null,\"sizeInOffer2\":20}\n" +
            "{\"objectId\":\"obj4\",\"sizeInOffer1\":10,\"sizeInOffer2\":null}"
        );
        assertThat(reportWriter.getErrorCount()).isEqualTo(3L);
        assertThat(reportWriter.getTotalObjectCount()).isEqualTo(5L);
    }
}

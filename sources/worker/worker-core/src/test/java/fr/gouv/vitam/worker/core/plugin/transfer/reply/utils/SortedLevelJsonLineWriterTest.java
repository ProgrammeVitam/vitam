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
package fr.gouv.vitam.worker.core.plugin.transfer.reply.utils;

import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import fr.gouv.vitam.worker.core.distribution.JsonLineWriter;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import static fr.gouv.vitam.worker.core.utils.JsonLineTestUtils.assertJsonlReportsEqual;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

public class SortedLevelJsonLineWriterTest {

    private static final String EXPORT_FILE = "file";

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    @Mock
    private HandlerIO handler;

    @InjectMocks
    private SortedLevelJsonLineWriter instance;

    @Before
    public void setup() {
        doAnswer(args -> {
            String filename = args.getArgument(0);
            return tempFolder.newFile(filename);
        })
            .when(handler)
            .getNewLocalFile((anyString()));
    }

    @After
    public void cleanup() throws IOException {
        instance.close();
    }

    @Test
    public void testEmpty() throws Exception {
        // Given

        // When
        instance.exportToWorkspace(EXPORT_FILE, true);

        // Then
        ArgumentCaptor<File> fileArgumentCaptor = ArgumentCaptor.forClass(File.class);
        verify(handler).transferFileToWorkspace(eq(EXPORT_FILE), fileArgumentCaptor.capture(), eq(true), eq(false));

        // Expected empty report
        assertThat(fileArgumentCaptor.getValue().length()).isEqualTo(0);
    }

    @Test
    public void testOneEntry() throws Exception {
        // Given
        JsonLineModel line1 = new JsonLineModel("id13-1", 13, JsonHandler.createObjectNode());
        instance.addEntry(line1);

        // When
        instance.exportToWorkspace(EXPORT_FILE, true);

        // Then
        ArgumentCaptor<File> fileArgumentCaptor = ArgumentCaptor.forClass(File.class);
        verify(handler).transferFileToWorkspace(eq(EXPORT_FILE), fileArgumentCaptor.capture(), eq(true), eq(false));

        // Expected empty report
        InputStream expectedReport = buildExpectedReport(line1);
        assertJsonlReportsEqual(new FileInputStream(fileArgumentCaptor.getValue()), expectedReport);
    }

    @Test
    public void testMultipleEntriesSameLevel() throws Exception {
        // Given
        JsonLineModel line1 = new JsonLineModel("id13-1", 13, JsonHandler.createObjectNode());
        JsonLineModel line2 = new JsonLineModel("id13-2", 13, JsonHandler.createObjectNode());
        JsonLineModel line3 = new JsonLineModel("id13-3", 13, JsonHandler.createObjectNode());
        instance.addEntry(line1);
        instance.addEntry(line2);
        instance.addEntry(line3);

        // When
        instance.exportToWorkspace(EXPORT_FILE, true);

        // Then
        ArgumentCaptor<File> fileArgumentCaptor = ArgumentCaptor.forClass(File.class);
        verify(handler).transferFileToWorkspace(eq(EXPORT_FILE), fileArgumentCaptor.capture(), eq(true), eq(false));

        // Expected empty report
        InputStream expectedReport = buildExpectedReport(line1, line2, line3);
        assertJsonlReportsEqual(new FileInputStream(fileArgumentCaptor.getValue()), expectedReport);
    }

    @Test
    public void testMultipleEntriesAscending() throws Exception {
        // Given
        JsonLineModel line1 = new JsonLineModel("id13-1", 13, JsonHandler.createObjectNode());
        JsonLineModel line2 = new JsonLineModel("id12-1", 12, JsonHandler.createObjectNode());
        JsonLineModel line3 = new JsonLineModel("id12-2", 12, JsonHandler.createObjectNode());
        JsonLineModel line4 = new JsonLineModel("id13-2", 13, JsonHandler.createObjectNode());
        JsonLineModel line5 = new JsonLineModel("id10-2", 10, JsonHandler.createObjectNode());
        JsonLineModel line6 = new JsonLineModel("id13-3", 13, JsonHandler.createObjectNode());
        instance.addEntry(line1);
        instance.addEntry(line2);
        instance.addEntry(line3);
        instance.addEntry(line4);
        instance.addEntry(line5);
        instance.addEntry(line6);

        // When
        instance.exportToWorkspace(EXPORT_FILE, true);

        // Then
        ArgumentCaptor<File> fileArgumentCaptor = ArgumentCaptor.forClass(File.class);
        verify(handler).transferFileToWorkspace(eq(EXPORT_FILE), fileArgumentCaptor.capture(), eq(true), eq(false));

        // Expected empty report
        InputStream expectedReport = buildExpectedReport(line5, line2, line3, line1, line4, line6);
        assertJsonlReportsEqual(new FileInputStream(fileArgumentCaptor.getValue()), expectedReport);
    }

    @Test
    public void testMultipleEntriesDescending() throws Exception {
        // Given
        JsonLineModel line1 = new JsonLineModel("id13-1", 13, JsonHandler.createObjectNode());
        JsonLineModel line2 = new JsonLineModel("id12-1", 12, JsonHandler.createObjectNode());
        JsonLineModel line3 = new JsonLineModel("id12-2", 12, JsonHandler.createObjectNode());
        JsonLineModel line4 = new JsonLineModel("id13-2", 13, JsonHandler.createObjectNode());
        JsonLineModel line5 = new JsonLineModel("id10-2", 10, JsonHandler.createObjectNode());
        JsonLineModel line6 = new JsonLineModel("id13-3", 13, JsonHandler.createObjectNode());
        instance.addEntry(line1);
        instance.addEntry(line2);
        instance.addEntry(line3);
        instance.addEntry(line4);
        instance.addEntry(line5);
        instance.addEntry(line6);

        // When
        instance.exportToWorkspace(EXPORT_FILE, false);

        // Then
        ArgumentCaptor<File> fileArgumentCaptor = ArgumentCaptor.forClass(File.class);
        verify(handler).transferFileToWorkspace(eq(EXPORT_FILE), fileArgumentCaptor.capture(), eq(true), eq(false));

        // Expected empty report
        InputStream expectedReport = buildExpectedReport(line1, line4, line6, line2, line3, line5);
        assertJsonlReportsEqual(new FileInputStream(fileArgumentCaptor.getValue()), expectedReport);
    }

    @Test
    public void testTooManyLevelThenException() throws Exception {
        // Given
        for (int i = 1; i <= 100; i++) {
            instance.addEntry(new JsonLineModel("id" + i, i, JsonHandler.createObjectNode()));
        }

        // When / Then
        assertThatThrownBy(
            () -> instance.addEntry(new JsonLineModel("id101", 101, JsonHandler.createObjectNode()))
        ).isInstanceOf(IllegalStateException.class);
    }

    private InputStream buildExpectedReport(JsonLineModel... lines) throws IOException {
        try (
            ByteArrayOutputStream expectedOS = new ByteArrayOutputStream();
            JsonLineWriter jsonLineWriter = new JsonLineWriter(expectedOS)
        ) {
            for (JsonLineModel line : lines) {
                jsonLineWriter.addEntry(line);
            }
            jsonLineWriter.close();
            return expectedOS.toInputStream();
        }
    }
}

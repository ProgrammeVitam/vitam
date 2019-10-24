package fr.gouv.vitam.worker.core.plugin.transfer.reply.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.distribution.JsonLineGenericIterator;
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import fr.gouv.vitam.worker.core.distribution.JsonLineWriter;
import net.javacrumbs.jsonunit.JsonAssert;
import org.apache.commons.collections4.IteratorUtils;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

public class SortedLevelJsonLineWriterTest {

    private static final TypeReference<JsonNode> JSON_NODE_TYPE_REFERENCE = new TypeReference<JsonNode>() {
    };
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
        doAnswer((args) -> {
            String filename = args.getArgument(0);
            return tempFolder.newFile(filename);
        }).when(handler).getNewLocalFile((anyString()));
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
        assertThatThrownBy(() -> instance.addEntry(new JsonLineModel("id101", 101, JsonHandler.createObjectNode())))
            .isInstanceOf(IllegalStateException.class);
    }

    private InputStream buildExpectedReport(JsonLineModel... lines) throws IOException {

        try (
            ByteArrayOutputStream expectedOS = new ByteArrayOutputStream();
            JsonLineWriter jsonLineWriter = new JsonLineWriter(expectedOS)) {
            for (JsonLineModel line : lines) {
                jsonLineWriter.addEntry(line);
            }
            jsonLineWriter.close();
            return expectedOS.toInputStream();
        }
    }



    private void assertJsonlReportsEqual(InputStream actualInputStream, InputStream expectedReportInputStream)
        throws InvalidParseOperationException {
        try (
            JsonLineGenericIterator<JsonNode> resultReportIterator = new JsonLineGenericIterator<>(
                actualInputStream, JSON_NODE_TYPE_REFERENCE);
            JsonLineGenericIterator<JsonNode> expectedReportIterator = new JsonLineGenericIterator<>(
                expectedReportInputStream,
                JSON_NODE_TYPE_REFERENCE);
        ) {

            JsonAssert.assertJsonEquals(
                JsonHandler.toJsonNode(IteratorUtils.toList(resultReportIterator)),
                JsonHandler.toJsonNode(IteratorUtils.toList(expectedReportIterator))
            );
        }
    }
}

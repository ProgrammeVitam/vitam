package fr.gouv.vitam.worker.core.service;

import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.processing.model.ChainedFileModel;
import fr.gouv.vitam.worker.common.HandlerIO;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class ChainedFileWriterTest {

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void addEntry_empty() throws Exception {

        // Given
        HandlerIO handlerIO = mock(HandlerIO.class);
        String folder = "folder";
        String filename = "filename.json";

        File chainedFile0 = tempFolder.newFile();
        given(handlerIO.getNewLocalFile(filename)).willReturn(chainedFile0);

        // When
        try (ChainedFileWriter instance = new ChainedFileWriter(handlerIO, folder, filename, 3)) {
            // Empty
        }


        // Then
        ChainedFileModel chainedFileModel0 = JsonHandler.getFromFile(chainedFile0, ChainedFileModel.class);

        assertThat(chainedFileModel0.getElements()).isEmpty();
        assertThat(chainedFileModel0.getNextFile()).isNull();
    }

    @Test
    public void addEntry_overflow_with_remaining_entries_in_last_file() throws Exception {

        // Given
        HandlerIO handlerIO = mock(HandlerIO.class);
        String folder = "folder";
        String filename = "filename.json";

        File chainedFile0 = tempFolder.newFile();
        given(handlerIO.getNewLocalFile(filename)).willReturn(chainedFile0);

        File chainedFile1 = tempFolder.newFile();
        given(handlerIO.getNewLocalFile(filename + ".1")).willReturn(chainedFile1);

        // When
        try (ChainedFileWriter instance = new ChainedFileWriter(handlerIO, folder, filename, 3)) {

            instance.addEntry("1");
            instance.addEntry("2");
            instance.addEntry("3");
            instance.addEntry("4");
        }


        // Then
        ChainedFileModel chainedFileModel0 = JsonHandler.getFromFile(chainedFile0, ChainedFileModel.class);
        ChainedFileModel chainedFileModel1 = JsonHandler.getFromFile(chainedFile1, ChainedFileModel.class);

        assertThat(chainedFileModel0.getElements()).containsExactly("1", "2", "3");
        assertThat(chainedFileModel0.getNextFile()).isEqualTo(filename + ".1");

        assertThat(chainedFileModel1.getElements()).containsExactly("4");
        assertThat(chainedFileModel1.getNextFile()).isNull();
    }

    @Test
    public void addEntry_overflow_without_remaining_entries_in_last_file() throws Exception {

        // Given
        HandlerIO handlerIO = mock(HandlerIO.class);
        String folder = "folder";
        String filename = "filename.json";

        File chainedFile0 = tempFolder.newFile();
        given(handlerIO.getNewLocalFile(filename)).willReturn(chainedFile0);

        File chainedFile1 = tempFolder.newFile();
        given(handlerIO.getNewLocalFile(filename + ".1")).willReturn(chainedFile1);

        // When
        try (ChainedFileWriter instance = new ChainedFileWriter(handlerIO, folder, filename, 3)) {

            instance.addEntry("1");
            instance.addEntry("2");
            instance.addEntry("3");
        }

        // Then
        ChainedFileModel chainedFileModel0 = JsonHandler.getFromFile(chainedFile0, ChainedFileModel.class);
        ChainedFileModel chainedFileModel1 = JsonHandler.getFromFile(chainedFile1, ChainedFileModel.class);

        assertThat(chainedFileModel0.getElements()).containsExactly("1", "2", "3");
        assertThat(chainedFileModel0.getNextFile()).isEqualTo(filename + ".1");

        assertThat(chainedFileModel1.getElements()).isEmpty();
        assertThat(chainedFileModel1.getNextFile()).isNull();
    }
}

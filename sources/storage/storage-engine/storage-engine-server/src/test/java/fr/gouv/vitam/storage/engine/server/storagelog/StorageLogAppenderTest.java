package fr.gouv.vitam.storage.engine.server.storagelog;

import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.storage.engine.server.storagelog.parameters.StorageLogbookOutcome;
import fr.gouv.vitam.storage.engine.server.storagelog.parameters.StorageLogbookParameterName;
import fr.gouv.vitam.storage.engine.server.storagelog.parameters.StorageLogbookParameters;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StorageLogAppenderTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void setUp() throws IOException {
        folder.create();
    }

    @Test()
    public void appendTest() throws IOException {

        StorageLogbookParameters params1 = buildStorageParameters("params1");
        StorageLogbookParameters params2 = buildStorageParameters("params2");
        StorageLogbookParameters params3 = buildStorageParameters("params3");

        Path filePath = folder.getRoot().toPath().resolve(GUIDFactory.newGUID().toString());

        try (StorageLogAppender instance = new StorageLogAppender(filePath)) {
            instance.append(params1);
            instance.append(params2);
            instance.append(params3);
        }

        assertThat(filePath).exists();
        assertThat(Files.readAllBytes(filePath))
            .isEqualTo("params1\nparams2\nparams3\n".getBytes());
    }

    private StorageLogbookParameters buildStorageParameters(String str) {
        StorageLogbookParameters params = mock(StorageLogbookParameters.class);
        Map<StorageLogbookParameterName, String> mapParameters = mock(Map.class);
        when(mapParameters.toString()).thenReturn(str);
        when(params.getMapParameters()).thenReturn(mapParameters);
        return params;
    }
}

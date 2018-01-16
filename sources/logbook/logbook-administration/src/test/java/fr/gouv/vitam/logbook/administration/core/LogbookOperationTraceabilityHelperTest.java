package fr.gouv.vitam.logbook.administration.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.InputStream;

import org.apache.commons.codec.binary.Base64;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.timestamp.TimestampGenerator;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookOperation;
import fr.gouv.vitam.logbook.operations.api.LogbookOperations;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

public class LogbookOperationTraceabilityHelperTest {

    private static final String LOGBOOK_OPERATION_WITH_TOKEN = "/logbookOperationWithToken.json";
    public static final Integer OPERATION_TRACEABILITY_OVERLAP_DELAY = 300;
    
    @Test
    @RunWithCustomExecutor
    public void should_extract_correctly_timestamp_token() throws Exception {
        // Given
        LogbookOperations logbookOperations = mock(LogbookOperations.class);
        WorkspaceClientFactory workspaceClientFactory = mock(WorkspaceClientFactory.class);

        LogbookOperationTraceabilityHelper helper = 
            new LogbookOperationTraceabilityHelper(workspaceClientFactory.getClient(), logbookOperations, null, OPERATION_TRACEABILITY_OVERLAP_DELAY);
        
        InputStream stream = getClass().getResourceAsStream(LOGBOOK_OPERATION_WITH_TOKEN);
        JsonNode jsonNode = JsonHandler.getFromInputStream(stream);
        LogbookOperation logbookOperation = new LogbookOperation(jsonNode);

        // When
        byte[] token = helper.extractTimestampToken(logbookOperation);

        // Then
        assertThat(Base64.encodeBase64String(token)).isEqualTo(
            "AQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQ=");
    }
    
}

package fr.gouv.vitam.worker.core.plugin.preservation;

import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.PreservationRequest;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PreservationPreparationPluginTest {

    @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
    @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock private AdminManagementClientFactory adminManagementClientFactory;

    @Mock private AdminManagementClient adminManagementClient;

    @Mock private MetaDataClientFactory metaDataClientFactory;

    @Mock private MetaDataClient metaDataClient;



    private PreservationPreparationPlugin preservationPreparationPlugin;

    @Before
    public void setUp() throws Exception {

        preservationPreparationPlugin =
            new PreservationPreparationPlugin(adminManagementClientFactory, metaDataClientFactory);

        when(metaDataClientFactory.getClient()).thenReturn(metaDataClient);
        when(adminManagementClientFactory.getClient()).thenReturn(adminManagementClient);

    }

    @Test
    public void shouldCreateJsonLFile()
        throws ContentAddressableStorageServerException, ProcessingException, IOException,
        InvalidParseOperationException {

        HandlerIO handler = mock(HandlerIO.class);
        WorkerParameters workerParameters = mock(WorkerParameters.class);
        File file = temporaryFolder.newFile();
        PreservationRequest preservationRequest = new PreservationRequest(new Select().getFinalSelect(),"id",singletonList("BinaryMaster"),"v1");
        when(handler.getJsonFromWorkspace("preservationRequest")).thenReturn(JsonHandler.toJsonNode(preservationRequest));
        when(handler.getNewLocalFile(anyString())).thenReturn(file);
        ItemStatus execute = preservationPreparationPlugin.execute(workerParameters, handler);

        assertThat(execute.getGlobalStatus()).isEqualTo(StatusCode.OK);
    }
}

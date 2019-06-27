package fr.gouv.vitam.worker.core.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.ProcessingUri;
import fr.gouv.vitam.common.model.processing.UriPrefix;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class PrepareStorageInfoActionHandlerTest {

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void setUp() throws IOException {
        File tempFolder = temporaryFolder.newFolder();
        SystemPropertyUtil.set("vitam.tmp.folder", tempFolder.getAbsolutePath());
    }

    @Test
    @RunWithCustomExecutor
    public void testExecute() throws Exception {

        VitamThreadUtils.getVitamSession().setTenantId(1);
        StorageClientFactory.changeMode(null);

        PrepareStorageInfoActionHandler instance = new PrepareStorageInfoActionHandler();

        WorkerParameters params = mock(WorkerParameters.class);
        HandlerIO handlerIO = mock(HandlerIO.class);

        File output = new File(temporaryFolder.newFolder(), "storageInfo.json");
        String filePath = output.getAbsolutePath();
        when(handlerIO.getOutput(0)).thenReturn(new ProcessingUri(UriPrefix.WORKSPACE, filePath));
        when(handlerIO.getNewLocalFile(filePath)).thenReturn(output);

        ItemStatus response = instance.execute(params, handlerIO);

        assertEquals(StatusCode.OK, response.getGlobalStatus());

        verify(handlerIO).addOutputResult(0, output, true, false);

        JsonNode storageInfo = JsonHandler.getFromFile(output);

        assertThat(storageInfo.get(SedaConstants.TAG_NB).asInt()).isEqualTo(2);
        assertThat((storageInfo.get(SedaConstants.OFFER_IDS)).size()).isEqualTo(2);
        assertThat((storageInfo.get(SedaConstants.OFFER_IDS)).get(0).asText()).isEqualTo("offer1");
        assertThat((storageInfo.get(SedaConstants.OFFER_IDS)).get(1).asText()).isEqualTo("offer2");
        assertThat(storageInfo.get(SedaConstants.STRATEGY_ID).asText()).isEqualTo(VitamConfiguration.getDefaultStrategy());
    }
}

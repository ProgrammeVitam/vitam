package fr.gouv.vitam.worker.core.plugin;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.IOParameter;
import fr.gouv.vitam.common.model.processing.ProcessingUri;
import fr.gouv.vitam.common.model.processing.UriPrefix;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.processing.common.parameter.WorkerParameterName;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.worker.core.impl.HandlerIOImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;


public class CheckExistenceObjectPluginTest {

    private CheckExistenceObjectPlugin plugin;
    private StorageClient storageClient;
    private StorageClientFactory storageClientFactory;

    private HandlerIOImpl action;
    private GUID guid = GUIDFactory.newGUID();
    private List<IOParameter> out;

    private static final String OG_NODE = "ogNode.json";
    private InputStream og;
    private JsonNode ogNode;


    private static final String SEARCH_RESULTS = "PrepareAuditHandler/searchResults.json";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private final WorkerParameters params =
        WorkerParametersFactory.newWorkerParameters().setUrlWorkspace("http://localhost:8083")
            .setUrlMetadata("http://localhost:8083")
            .setObjectName("archiveUnit.json").setCurrentStep("currentStep")
            .setContainerName(guid.getId()).setLogbookTypeProcess(LogbookTypeProcess.UPDATE)
            .putParameterValue(WorkerParameterName.auditType, "tenant")
            .putParameterValue(WorkerParameterName.objectId, "0");

    public CheckExistenceObjectPluginTest() throws FileNotFoundException, InvalidParseOperationException {
        og = PropertiesUtils.getResourceAsStream(OG_NODE);
        ogNode = JsonHandler.getFromInputStream(og);
    }

    @Before
    public void setUp() throws Exception {

        File tempFolder = folder.newFolder();
        System.setProperty("vitam.tmp.folder", tempFolder.getAbsolutePath());
        SystemPropertyUtil.refresh();

        storageClient = mock(StorageClient.class);
        storageClientFactory = mock(StorageClientFactory.class);

       when(storageClientFactory.getClient()) .thenReturn(storageClient);
        plugin = new CheckExistenceObjectPlugin(storageClientFactory);
        action = new HandlerIOImpl(guid.getId(), "workerId", Lists.newArrayList());
        out = new ArrayList<>();
        out.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.MEMORY, "shouldWriteLFC")));
        action.getInput().add(ogNode);

    }

    @Test
    public void executeOK() throws Exception {
        action.addOutIOParameters(out);

        reset(storageClient);
        // binary master -> binary exists on offers
        when(storageClient.exists(any(), any(), eq("aeaaaaaaaaesq6c3abnimak6qzrse5qaaaaq"), any()))
            .thenReturn(true);
        // physical master -> binary exists on offers
        when(storageClient.exists(any(), any(), eq("aeaaaaaaaagesnmfaaglialcsywj2haaaaba"), any()))
            .thenReturn(false);

        final ItemStatus response = plugin.execute(params, action);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
        assertTrue(((String) response.getData("eventDetailData")).contains("\"errors\":[]"));
    }

    @Test
    public void executeKO() throws Exception {
        action.addOutIOParameters(out);
        reset(storageClient);
        // binary master -> binary does not exist on offers
        when(storageClient.exists(any(), any(), eq("aeaaaaaaaaesq6c3abnimak6qzrse5qaaaaq"), any()))
            .thenReturn(false);
        // physical master -> binary does not exists on offers
        when(storageClient.exists(any(), any(), eq("aeaaaaaaaagesnmfaaglialcsywj2haaaaba"), any()))
            .thenReturn(false);

        final ItemStatus response = plugin.execute(params, action);
        assertEquals(StatusCode.KO, response.getGlobalStatus());
    }

    @Test
    public void executeWithBinaryForPhysicalThenKO() throws Exception {
        action.addOutIOParameters(out);

        final JsonNode searchResults =
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(SEARCH_RESULTS));

        reset(storageClient);
        // binary master -> binary exists on offers
        when(storageClient.exists(any(), any(), eq("aeaaaaaaaaesq6c3abnimak6qzrse5qaaaaq"), any()))
            .thenReturn(true);
        // physical master -> binary exists on offers
        when(storageClient.exists(any(), any(), eq("aeaaaaaaaagesnmfaaglialcsywj2haaaaba"), any()))
            .thenReturn(true);

        final ItemStatus response = plugin.execute(params, action);
        assertEquals(StatusCode.KO, response.getGlobalStatus());
        assertTrue(((String) response.getData("eventDetailData")).contains("\"errors\":[]"));
        assertTrue(((String) response.getData("eventDetailData")).contains(
            "\"errorsPhysical\":[{\"IdObj\":\"aeaaaaaaaagesnmfaaglialcsywj2haaaaba\",\"Usage\":\"PhysicalMaster_1\",\"IdAU\":[\"aeaqaaaaaaesq6c3abnimak6qzrsfziaaaaq\"]}]"));
    }

}

package fr.gouv.vitam.worker.core.plugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import fr.gouv.vitam.storage.driver.model.StorageMetadataResult;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.worker.core.impl.HandlerIOImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.net.ssl.*")
@PrepareForTest({StorageClientFactory.class})
public class CheckIntegrityObjectPluginTest {
    CheckIntegrityObjectPlugin plugin = new CheckIntegrityObjectPlugin();

    private StorageClient storageClient;
    private StorageClientFactory storageClientFactory;

    private HandlerIOImpl action;
    private GUID guid = GUIDFactory.newGUID();
    private List<IOParameter> out;
    
    private static final String OG_NODE = "ogNode.json";
    private InputStream og;
    private JsonNode ogNode;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private final WorkerParameters params =
        WorkerParametersFactory.newWorkerParameters().setUrlWorkspace("http://localhost:8083")
            .setUrlMetadata("http://localhost:8083")
            .setObjectName("archiveUnit.json").setCurrentStep("currentStep")
            .setContainerName(guid.getId()).setLogbookTypeProcess(LogbookTypeProcess.UPDATE)
            .putParameterValue(WorkerParameterName.auditType, "tenant")
            .putParameterValue(WorkerParameterName.objectId, "0");
    
    public CheckIntegrityObjectPluginTest() throws FileNotFoundException, InvalidParseOperationException {
        og = PropertiesUtils.getResourceAsStream(OG_NODE);
        ogNode = JsonHandler.getFromInputStream(og);
    }

    @Before
    public void setUp() throws Exception {

        File tempFolder = folder.newFolder();
        System.setProperty("vitam.tmp.folder", tempFolder.getAbsolutePath());
        SystemPropertyUtil.refresh();

        PowerMockito.mockStatic(StorageClientFactory.class);
        storageClient = mock(StorageClient.class);
        storageClientFactory = mock(StorageClientFactory.class);

        PowerMockito.when(StorageClientFactory.getInstance()).thenReturn(storageClientFactory);
        PowerMockito.when(StorageClientFactory.getInstance().getClient())
            .thenReturn(storageClient);

        action = new HandlerIOImpl(guid.getId(), "workerId", Lists.newArrayList());
        out = new ArrayList<>();
        out.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.MEMORY, "shouldWriteLFC")));
        action.getInput().add(ogNode);

    }

    @Test
    public void executeOK() throws Exception {
        action.addOutIOParameters(out);
        ObjectNode offerIdToMetadata = JsonHandler.createObjectNode();
        StorageMetadataResult metaData = new StorageMetadataResult("aeaaaaaaaacu6xzeabinwak6t5ecmlaaaaaq", "object",
            "c117854cbca3e51ea94c4bd2bcf4a6756209e6c65ddbf696313e1801b2235ff33d44b2bb272e714c335a44a3b4f92d399056b94dff4dfe6b7038fa56f23b438e",
            6096, "Vitam_0", "Tue Aug 31 10:20:56 SGT 2016", "Tue Aug 31 10:20:56 SGT 2016");
        offerIdToMetadata.set("localhost", JsonHandler.toJsonNode(metaData));
        reset(storageClient);
        when(storageClient.getInformation(any(), eq(DataCategory.OBJECT), any(), any(), eq(true))).thenReturn(offerIdToMetadata);

        final ItemStatus response = plugin.execute(params, action);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
        assertTrue(((String) response.getData("eventDetailData")).contains("\"errors\":[]"));
    }

    @Test
    public void executeKO() throws Exception {
        action.addOutIOParameters(out);
        JsonNode metadataInfo = JsonHandler.getFromString("{}");
        reset(storageClient);
        when(storageClient.getInformation(any(), eq(DataCategory.OBJECT), any(), any(), eq(true))).thenReturn(metadataInfo);

        final ItemStatus response = plugin.execute(params, action);
        assertEquals(StatusCode.KO, response.getGlobalStatus());
    }
}

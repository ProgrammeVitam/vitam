/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 *******************************************************************************/
package fr.gouv.vitam.worker.core.plugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.CanonicalJsonFormatter;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.IngestWorkflowConstants;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.LifeCycleTraceabilitySecureFileObject;
import fr.gouv.vitam.common.model.MetadataType;
import fr.gouv.vitam.common.model.ObjectGroupDocumentHash;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookDocument;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.metadata.core.database.collections.MetadataDocument;
import fr.gouv.vitam.metadata.core.database.collections.ObjectGroup;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.storage.driver.model.StorageMetadatasResult;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.impl.HandlerIOImpl;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;

public class CreateObjectSecureFileActionPluginTest {

    CreateObjectSecureFileActionPlugin plugin;

    private static final Integer TENANT_ID = 0;
    private static final String OBJECT_LFC_1 = "CreateObjectSecureFileActionPlugin/object1.json";
    private static final String OBJECT_GROUP_MD = "CreateObjectSecureFileActionPlugin/objectGroup_md.json";
    private static final String LFC_GOT_MD_FILE = "CreateObjectSecureFileActionPlugin/lfc_md_ObjectGroup.json";

    private GUID guid = GUIDFactory.newGUID();
    private String guidOg = "aebaaaaaaahgausqab7boak55jchzyqaaaaq";

    private final DigestType digestType = VitamConfiguration.getDefaultDigestType();
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();
    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    private HandlerIO handler = mock(HandlerIO.class);
    private final WorkerParameters params =
        WorkerParametersFactory.newWorkerParameters().setUrlWorkspace("http://localhost:8083")
            .setUrlMetadata("http://localhost:8083")
            .setObjectName(guidOg + ".json").setCurrentStep("currentStep")
            .setContainerName(guid.getId())
            .setLogbookTypeProcess(LogbookTypeProcess.TRACEABILITY);
    @Mock
    private WorkspaceClient workspaceClient;
    @Mock
    private MetaDataClient metadataClient;
    @Mock
    private MetaDataClientFactory metadataClientFactory;

    @Mock
    private StorageClientFactory storageClientFactory;
    @Mock
    private StorageClient storageClient;

    public CreateObjectSecureFileActionPluginTest() {
        // do nothing
    }

    @Before
    public void setUp() throws Exception {
        File tempFolder = folder.newFolder();
        System.setProperty("vitam.tmp.folder", tempFolder.getAbsolutePath());
        SystemPropertyUtil.refresh();

        when(metadataClientFactory.getClient()).thenReturn(metadataClient);
        when(storageClientFactory.getClient()).thenReturn(storageClient);
        plugin = new CreateObjectSecureFileActionPlugin(metadataClientFactory, storageClientFactory);
        handler = new HandlerIOImpl(workspaceClient, "CreateObjectSecureFileActionPluginTest", "workerId");
    }

    @Test
    @RunWithCustomExecutor
    public void givenNothingDoneWhenExecuteThenReturnResponseKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        reset(workspaceClient);
        reset(metadataClient);
        when(workspaceClient.getObject(anyObject(),
            eq(IngestWorkflowConstants.OBJECT_GROUP_FOLDER + "/" + guidOg + ".json")))
            .thenThrow(
                new ContentAddressableStorageNotFoundException("ContentAddressableStorageNotFoundException"));
        final ItemStatus response = plugin.execute(params, handler);
        assertEquals(response.getGlobalStatus(), StatusCode.FATAL);
    }

    @Test
    @RunWithCustomExecutor
    public void givenExistingAndCorrectFilesWhenExecuteThenReturnResponseOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final JsonNode objectGroupFound =
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(OBJECT_GROUP_MD));
        reset(workspaceClient);
        reset(metadataClient);
        final InputStream object1 = PropertiesUtils.getResourceAsStream(OBJECT_LFC_1);
        when(workspaceClient.getObject(anyObject(),
            eq(IngestWorkflowConstants.OBJECT_GROUP_FOLDER + "/" + guidOg + ".json")))
            .thenReturn(Response.status(Status.OK).entity(object1).build());
        when(metadataClient.selectObjectGrouptbyId(anyObject(), anyObject()))
            .thenReturn(objectGroupFound);
        when(metadataClient.getObjectGroupByIdRaw(anyObject()))
            .thenReturn(new RequestResponseOK<JsonNode>().addResult(objectGroupFound));
        saveWorkspacePutObject(SedaConstants.LFC_OBJECTS_FOLDER + "/" + guidOg + ".json");

        final String[] offerIds = new String[] {"vitam-iaas-app-02.int", "vitam-iaas-app-03.int"};
        JsonNode mdWithLfc = JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(LFC_GOT_MD_FILE));
        final String expectedMDLFCGlobalHashFromStorage = generateExpectedDigest(
            mdWithLfc);

        final StorageMetadatasResult storageMDResult = new StorageMetadatasResult(guidOg, "",
            expectedMDLFCGlobalHashFromStorage, 17011, "file_owner",
            LocalDateUtil.getString(LocalDateTime.now()), LocalDateUtil.getString(LocalDateTime.now()));

        final ObjectNode objectInformationResult = JsonHandler.createObjectNode();

        objectInformationResult.set(offerIds[0], JsonHandler.toJsonNode(storageMDResult));

        storageMDResult.setDigest("bad_digest_7654678299DGDIKSOqfzgzer$f$*lfkdj_just-/=first-digest-one");

        objectInformationResult.set(offerIds[1], JsonHandler.toJsonNode(storageMDResult));

        when(storageClient.getInformation(eq("default"), anyObject(), anyString(), eq(Arrays.asList(offerIds))))
            .thenReturn(objectInformationResult);

        ItemStatus response = plugin.execute(params, handler);

        assertEquals(StatusCode.FATAL, response.getGlobalStatus());

        // Test with same global digest for all storage offers
        storageMDResult.setDigest(expectedMDLFCGlobalHashFromStorage);
        objectInformationResult.set(offerIds[0], JsonHandler.toJsonNode(storageMDResult));
        objectInformationResult.set(offerIds[1], JsonHandler.toJsonNode(storageMDResult));

        response = plugin.execute(params, handler);

        assertEquals(response.getGlobalStatus(), StatusCode.OK);
        String fileAsString = getSavedWorkspaceObject(SedaConstants.LFC_OBJECTS_FOLDER + "/" + guidOg + ".json");
        assertNotNull(fileAsString);
        System.out.println(fileAsString);
        // got have 11 objects and the last one is an Array with the elements ==> 13 json separators (ie ',')
        assertEquals(15, StringUtils.countMatches(fileAsString, ","));

        // check hash for LFC and for MD
        ObjectNode got = (ObjectNode) JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream(OBJECT_GROUP_MD));

        final String gotMDHash = generateExpectedDigest(got);

        JsonNode lfc = JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(OBJECT_LFC_1));
        final String gotLFCHash = generateExpectedDigest(lfc);
        final String gotLFCHashEvents = generateExpectedDigest(lfc.get(LogbookDocument.EVENTS));


        final LifeCycleTraceabilitySecureFileObject lfcTraceSecFileDataLineExpected =
            new LifeCycleTraceabilitySecureFileObject(
                "aedqaaaaacgxm4z4abt3gak55jccrjyaaaba",
                "INGEST",
                "2017-08-16T09:29:25.562",
                guidOg,
                MetadataType.OBJECTGROUP,
                0,
                "OK",
                gotLFCHash,
                gotLFCHashEvents,
                gotMDHash,
                expectedMDLFCGlobalHashFromStorage,
                null

            );
        List<String> auUp = new ArrayList<>();
        auUp.add("aeaqaaaaaahgausqab7boak55jcmttqaaaaq");
        lfcTraceSecFileDataLineExpected.setUp(auUp);
        // add object documents (qualifiers->version)
        List<ObjectGroupDocumentHash> objectGroupDocumentHashToList = new ArrayList<>();

        objectGroupDocumentHashToList.add(
            new ObjectGroupDocumentHash("aeaaaaaaaahgausqab7boak55jchzzqaaaaq", expectedMDLFCGlobalHashFromStorage));
        objectGroupDocumentHashToList.add(
            new ObjectGroupDocumentHash("aeaaaaaaaahgausqab7boak55jchzyiaaabq", expectedMDLFCGlobalHashFromStorage));

        lfcTraceSecFileDataLineExpected.setObjectGroupDocumentHashList(objectGroupDocumentHashToList);

        String expected = new String(
            CanonicalJsonFormatter.serializeToByteArray(JsonHandler.toJsonNode(lfcTraceSecFileDataLineExpected)),
            StandardCharsets.UTF_8);
        assertEquals(expected, fileAsString);

    }

    private String generateExpectedDigest(JsonNode jsonNode) {
        Digest digest = new Digest(digestType);
        digest.update(CanonicalJsonFormatter.serializeToByteArray(jsonNode));
        return digest.digest64();
    }

    private void saveWorkspacePutObject(String filename) throws ContentAddressableStorageServerException {
        doAnswer(invocation -> {
            InputStream inputStream = invocation.getArgumentAt(2, InputStream.class);
            java.nio.file.Path file =
                java.nio.file.Paths
                    .get(System.getProperty("vitam.tmp.folder") + "/" + handler.getContainerName() + "_" +
                        handler.getWorkerId() + "/" + filename.replaceAll("/", "_"));
            java.nio.file.Files.copy(inputStream, file);
            return null;
        }).when(workspaceClient).putObject(anyString(),
            org.mockito.Matchers.eq(filename), org.mockito.Matchers.any(InputStream.class));
    }

    private String getSavedWorkspaceObject(String filename) throws IOException {
        byte[] encoded = Files
            .readAllBytes(Paths.get(System.getProperty("vitam.tmp.folder") + "/" + handler.getContainerName() + "_" +
                handler.getWorkerId() + "/" + filename.replaceAll("/", "_")));
        return new String(encoded, "UTF-8");
    }
}

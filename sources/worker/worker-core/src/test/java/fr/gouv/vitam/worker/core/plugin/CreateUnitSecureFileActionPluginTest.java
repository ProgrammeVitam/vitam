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
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.UpdateWorkflowConstants;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.impl.HandlerIOImpl;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.net.ssl.*")
@PrepareForTest({MetaDataClientFactory.class, WorkspaceClientFactory.class})
public class CreateUnitSecureFileActionPluginTest {

    CreateUnitSecureFileActionPlugin plugin = new CreateUnitSecureFileActionPlugin();

    private static final Integer TENANT_ID = 0;
    private static final String HANDLER_ID = "OG_CREATE_SECURED_FILE";
    private static final String UNIT_LFC_1 = "CreateUnitSecureFileActionPlugin/lfc_unit1.json";
    private static final String UNIT = "CreateUnitSecureFileActionPlugin/unit_response.json";
    private static final String UNIT_MD = "CreateUnitSecureFileActionPlugin/unit_md.json";
    private GUID guid = GUIDFactory.newGUID();
    private String guidUnit = "aeaqaaaaaqhgausqab7boak55nw5vqaaaaaq";

    private final DigestType digestType = VitamConfiguration.getDefaultDigestType();

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());
    
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    private HandlerIO handler = mock(HandlerIO.class);
    private final WorkerParameters params =
        WorkerParametersFactory.newWorkerParameters().setUrlWorkspace("http://localhost:8083")
            .setUrlMetadata("http://localhost:8083")
            .setObjectName(guidUnit + ".json").setCurrentStep("currentStep")
            .setContainerName(guid.getId())
            .setLogbookTypeProcess(LogbookTypeProcess.TRACEABILITY);
    private WorkspaceClient workspaceClient;
    private WorkspaceClientFactory workspaceClientFactory;
    private MetaDataClient metadataClient;
    private MetaDataClientFactory metadataClientFactory;
    
    public CreateUnitSecureFileActionPluginTest() {
        // do nothing
    }
    
    @Before
    public void setUp() throws Exception {
        File tempFolder = folder.newFolder();
        System.setProperty("vitam.tmp.folder", tempFolder.getAbsolutePath());
        workspaceClient = mock(WorkspaceClient.class);
        workspaceClientFactory = mock(WorkspaceClientFactory.class);
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);
        PowerMockito.mockStatic(MetaDataClientFactory.class);
        metadataClient = mock(MetaDataClient.class);
        metadataClientFactory = mock(MetaDataClientFactory.class);
        PowerMockito.when(MetaDataClientFactory.getInstance()).thenReturn(metadataClientFactory);
        PowerMockito.when(MetaDataClientFactory.getInstance().getClient())
            .thenReturn(metadataClient);
        SystemPropertyUtil.refresh();
        handler = new HandlerIOImpl(workspaceClient, "CreateUnitSecureFileActionPluginTest", "workerId");
    }
    
    @Test
    @RunWithCustomExecutor
    public void givenNothingDoneWhenExecuteThenReturnResponseKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        reset(workspaceClient);
        reset(metadataClient);
        when(workspaceClient.getObject(anyObject(),
            eq(UpdateWorkflowConstants.UNITS_FOLDER + "/" + guidUnit + ".json")))
                .thenThrow(
                    new ContentAddressableStorageNotFoundException("ContentAddressableStorageNotFoundException"));
        final ItemStatus response = plugin.execute(params, handler);
        assertEquals(StatusCode.FATAL, response.getGlobalStatus());
    }
    
    @Test
    @RunWithCustomExecutor
    public void givenExistingAndCorrectFilesWhenExecuteThenReturnResponseOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final JsonNode archiveFound =
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(UNIT));
        reset(workspaceClient);
        reset(metadataClient);
        final InputStream object1 = PropertiesUtils.getResourceAsStream(UNIT_LFC_1);
        when(workspaceClient.getObject(anyObject(),
            eq(UpdateWorkflowConstants.UNITS_FOLDER + "/" + guidUnit + ".json")))
                .thenReturn(Response.status(Status.OK).entity(object1).build());
        when(metadataClient.selectUnitbyId(anyObject(), anyObject()))
            .thenReturn(archiveFound);
        saveWorkspacePutObject(SedaConstants.LFC_UNITS_FOLDER + "/" + guidUnit + ".json");
        final ItemStatus response = plugin.execute(params, handler);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
        String fileAsString = getSavedWorkspaceObject(SedaConstants.LFC_UNITS_FOLDER + "/" + guidUnit + ".json");
        assertNotNull(fileAsString);
        //units have only 6 separators, objects 7
        assertEquals(6, StringUtils.countMatches(fileAsString, "|"));

        // check hash for LFC and for MD
        final String unitMDHash = generateExpectedDigest(UNIT_MD);
        final String unitLFCHash = generateExpectedDigest(UNIT_LFC_1);
        assertTrue(fileAsString.startsWith("aedqaaaaasgxm4z4abt3gak55nwzxtyaaaba | INGEST | 2017-08-16T14:34:40.426 | " +
            guidUnit+ " | OK | " + unitLFCHash + " | " + unitMDHash));
    }

    private String generateExpectedDigest(String resource) throws Exception {
        JsonNode jsonNode = JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(resource));
        Digest digest = new Digest(digestType);
        digest.update(JsonHandler.unprettyPrint(jsonNode).getBytes());
        return digest.toString();
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
        }).when(workspaceClient).putObject(org.mockito.Matchers.anyString(),
            org.mockito.Matchers.eq(filename), org.mockito.Matchers.any(InputStream.class));
    }
    
    private String getSavedWorkspaceObject(String filename) throws IOException {
        byte[] encoded = Files
            .readAllBytes(Paths.get(System.getProperty("vitam.tmp.folder") + "/" + handler.getContainerName() + "_" +
                handler.getWorkerId() + "/" + filename.replaceAll("/", "_")));
        return new String(encoded, "UTF-8");
    }
}
/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "https://cecill.info".
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
 */
package fr.gouv.vitam.worker.core.plugin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.UpdateWorkflowConstants;
import fr.gouv.vitam.common.model.processing.IOParameter;
import fr.gouv.vitam.common.model.processing.ProcessingUri;
import fr.gouv.vitam.common.model.processing.UriPrefix;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.core.impl.HandlerIOImpl;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.apache.commons.io.IOUtils;
import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ArchiveUnitRulesUpdateActionPluginTest {

    private MetaDataClient metadataClient;
    private MetaDataClientFactory metadataClientFactory;
    private WorkspaceClient workspaceClient;
    private WorkspaceClientFactory workspaceClientFactory;

    private LogbookLifeCyclesClient logbookLifeCyclesClient;
    private LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory;

    private HandlerIOImpl action;
    private GUID guid = GUIDFactory.newGUID();
    private String guidAu = "aeaqaaaaaaagwmb5aajf4ak5ujrmnkiaaaba";
    private List<IOParameter> out;

    private static final String AU_RULES = "ArchiveUnitRulesUpdateActionPlugin/archiveUnitRules.json";
    private static final String AU_DETAIL = "ArchiveUnitRulesUpdateActionPlugin/archiveUnit.json";
    private static final String AU_DETAIL_NO_START_DATE =
        "ArchiveUnitRulesUpdateActionPlugin/archiveUnitNoStartDate.json";
    private static final String UPDATED_AU = "ArchiveUnitRulesUpdateActionPlugin/updatedAu.json";
    private ArchiveUnitRulesUpdateActionPlugin plugin;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    private final WorkerParameters params = WorkerParametersFactory.newWorkerParameters()
        .setUrlWorkspace("http://localhost:8083")
        .setUrlMetadata("http://localhost:8083")
        .setObjectNameList(Lists.newArrayList(guidAu + ".json"))
        .setObjectName(guidAu + ".json")
        .setCurrentStep("currentStep")
        .setContainerName(guid.getId())
        .setLogbookTypeProcess(LogbookTypeProcess.UPDATE);

    @Before
    public void setUp() throws Exception {
        File tempFolder = folder.newFolder();
        System.setProperty("vitam.tmp.folder", tempFolder.getAbsolutePath());
        SystemPropertyUtil.refresh();

        metadataClient = mock(MetaDataClient.class);
        metadataClientFactory = mock(MetaDataClientFactory.class);
        workspaceClient = mock(WorkspaceClient.class);
        workspaceClientFactory = mock(WorkspaceClientFactory.class);

        logbookLifeCyclesClient = mock(LogbookLifeCyclesClient.class);
        logbookLifeCyclesClientFactory = mock(LogbookLifeCyclesClientFactory.class);

        when(metadataClientFactory.getClient()).thenReturn(metadataClient);
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);
        when(logbookLifeCyclesClientFactory.getClient()).thenReturn(logbookLifeCyclesClient);

        plugin = new ArchiveUnitRulesUpdateActionPlugin(metadataClientFactory);
        action = new HandlerIOImpl(
            workspaceClientFactory,
            logbookLifeCyclesClientFactory,
            guid.getId(),
            "workerId",
            com.google.common.collect.Lists.newArrayList()
        );
        out = new ArrayList<>();
        out.add(
            new IOParameter()
                .setUri(
                    new ProcessingUri(
                        UriPrefix.WORKSPACE,
                        UpdateWorkflowConstants.PROCESSING_FOLDER + "/" + UpdateWorkflowConstants.AU_TO_BE_UPDATED_JSON
                    )
                )
        );
    }

    @After
    public void clean() {
        action.partialClose();
    }

    @RunWithCustomExecutor
    @Test
    public void givenRunningProcessWhenExecuteThenReturnResponseOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        action.addOutIOParameters(out);
        final JsonNode archiveUnitToBeUpdated = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream(AU_DETAIL)
        );
        final InputStream archiveUnitRules = PropertiesUtils.getResourceAsStream(AU_RULES);
        final JsonNode archiveUnitUpdated = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream(UPDATED_AU)
        );
        try {
            when(metadataClient.selectUnitbyId(any(), eq(guidAu))).thenReturn(archiveUnitToBeUpdated);
            when(
                workspaceClient.getObject(any(), eq(UpdateWorkflowConstants.UNITS_FOLDER + "/" + guidAu + ".json"))
            ).thenReturn(Response.status(Status.OK).entity(archiveUnitRules).build());
            when(metadataClient.updateUnitById(any(), eq(guidAu))).thenReturn(archiveUnitUpdated);
            params.setProcessId(GUIDFactory.newOperationLogbookGUID(0).toString());
            final ItemStatus response = plugin.execute(params, action);
            assertEquals(StatusCode.OK, response.getGlobalStatus());
            response
                .getItemsStatus()
                .forEach((k, v) -> {
                    try {
                        assertEquals(v.getEvDetailData(), "{}");
                    } catch (Exception e) {
                        fail("should not failed at this moment, evDetailData value must be {}");
                    }
                });
        } finally {
            archiveUnitRules.close();
        }
    }

    @RunWithCustomExecutor
    @Test
    public void givenNoStartDateOnAUWhenExecuteThenReturnResponseOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        action.addOutIOParameters(out);
        final JsonNode archiveUnitToBeUpdated = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream(AU_DETAIL_NO_START_DATE)
        );
        final InputStream archiveUnitRules = PropertiesUtils.getResourceAsStream(AU_RULES);
        final JsonNode archiveUnitUpdated = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream(UPDATED_AU)
        );
        try {
            when(metadataClient.selectUnitbyId(any(), eq(guidAu))).thenReturn(archiveUnitToBeUpdated);
            when(
                workspaceClient.getObject(any(), eq(UpdateWorkflowConstants.UNITS_FOLDER + "/" + guidAu + ".json"))
            ).thenReturn(Response.status(Status.OK).entity(archiveUnitRules).build());
            when(metadataClient.updateUnitById(any(), eq(guidAu))).thenReturn(archiveUnitUpdated);
            params.setProcessId(GUIDFactory.newOperationLogbookGUID(0).toString());
            final ItemStatus response = plugin.execute(params, action);
            assertEquals(StatusCode.OK, response.getGlobalStatus());
            response
                .getItemsStatus()
                .forEach((k, v) -> {
                    try {
                        assertEquals(v.getEvDetailData(), "{}");
                    } catch (Exception e) {
                        fail("should not failed at this moment, evDetailData value must be {}");
                    }
                });
        } finally {
            archiveUnitRules.close();
        }
    }

    @Test
    public void givenMetadataErrorWhenExecuteThenReturnResponseFatal() throws Exception {
        action.addOutIOParameters(out);
        when(metadataClient.selectUnitbyId(any(), eq(guidAu))).thenThrow(
            new MetaDataExecutionException("MetaDataExecutionException")
        );
        final ItemStatus response = plugin.execute(params, action);
        assertEquals(StatusCode.FATAL, response.getGlobalStatus());
    }

    @Test
    public void givenMetadataReturnsNoUnitWhenExecuteThenReturnResponseFatal() throws Exception {
        action.addOutIOParameters(out);
        final JsonNode result = JsonHandler.createArrayNode();
        ObjectNode archiveUnitToBeUpdated = JsonHandler.createObjectNode();
        archiveUnitToBeUpdated.set("$results", result);
        when(metadataClient.selectUnitbyId(any(), eq(guidAu))).thenReturn(archiveUnitToBeUpdated);
        final ItemStatus response = plugin.execute(params, action);
        assertEquals(StatusCode.KO, response.getGlobalStatus());
    }

    @Test
    public void givenIncorrectXMLFileFromWorkspaceWhenExecuteThenReturnResponseFatal() throws Exception {
        action.addOutIOParameters(out);
        final JsonNode archiveUnitToBeUpdated = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream(AU_DETAIL)
        );

        when(metadataClient.selectUnitbyId(any(), eq(guidAu))).thenReturn(archiveUnitToBeUpdated);
        when(
            workspaceClient.getObject(any(), eq(UpdateWorkflowConstants.UNITS_FOLDER + "/" + guidAu + ".json"))
        ).thenReturn(
            Response.status(Status.OK)
                .entity(IOUtils.toInputStream("<root><random>Random XML tags</random></root>", "UTF-8"))
                .build()
        );
        final ItemStatus response2 = plugin.execute(params, action);
        assertEquals(StatusCode.FATAL, response2.getGlobalStatus());
    }

    @Test
    public void givenWorkspaceErrorWhenExecuteThenReturnResponseFatal() throws Exception {
        action.addOutIOParameters(out);
        final JsonNode archiveUnitToBeUpdated = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream(AU_DETAIL)
        );

        when(metadataClient.selectUnitbyId(any(), eq(guidAu))).thenReturn(archiveUnitToBeUpdated);
        when(
            workspaceClient.getObject(any(), eq(UpdateWorkflowConstants.UNITS_FOLDER + "/" + guidAu + ".json"))
        ).thenThrow(new ContentAddressableStorageNotFoundException("ContentAddressableStorageNotFoundException"));
        final ItemStatus response2 = plugin.execute(params, action);
        assertEquals(StatusCode.FATAL, response2.getGlobalStatus());
    }

    @Test
    public void givenUpdateErrorWhenExecuteThenReturnResponseFatal() throws Exception {
        action.addOutIOParameters(out);
        final JsonNode archiveUnitToBeUpdated = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream(AU_DETAIL)
        );
        final InputStream archiveUnitRules = PropertiesUtils.getResourceAsStream(AU_RULES);
        try {
            when(metadataClient.selectUnitbyId(any(), eq(guidAu))).thenReturn(archiveUnitToBeUpdated);
            when(
                workspaceClient.getObject(any(), eq(UpdateWorkflowConstants.UNITS_FOLDER + "/" + guidAu + ".json"))
            ).thenReturn(Response.status(Status.OK).entity(archiveUnitRules).build());

            when(metadataClient.updateUnitById(any(), eq(guidAu))).thenThrow(
                new InvalidParseOperationException("Bad Request")
            );

            final ItemStatus response2 = plugin.execute(params, action);
            assertEquals(StatusCode.KO, response2.getGlobalStatus());
        } finally {
            archiveUnitRules.close();
        }
    }
}

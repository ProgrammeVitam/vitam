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
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.client.ClientMockResultHelper;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.ArchiveUnitProfileModel;
import fr.gouv.vitam.common.model.administration.ArchiveUnitProfileStatus;
import fr.gouv.vitam.common.model.processing.IOParameter;
import fr.gouv.vitam.common.model.processing.ProcessingUri;
import fr.gouv.vitam.common.model.processing.UriPrefix;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialNotFoundException;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.metadata.core.validation.CachedArchiveUnitProfileLoader;
import fr.gouv.vitam.metadata.core.validation.CachedSchemaValidatorLoader;
import fr.gouv.vitam.metadata.core.validation.UnitValidator;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.core.impl.HandlerIOImpl;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static fr.gouv.vitam.common.model.StatusCode.FATAL;
import static fr.gouv.vitam.common.model.StatusCode.KO;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWithCustomExecutor
public class CheckArchiveUnitProfileActionPluginTest {

    @ClassRule
    public static RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    private CheckArchiveUnitProfileActionPlugin plugin;
    private WorkspaceClient workspaceClient;
    private AdminManagementClient adminManagementClient;

    private static final String ARCHIVE_UNIT = "checkArchiveUnitProfileActionPlugin/archive-unit_OK.json";
    private static final String ARCHIVE_UNIT_INVALID_XML =
        "checkArchiveUnitProfileActionPlugin/archive-unit_Invalid.xml";
    private static final String ARCHIVE_UNIT_INVALID_DESCRIPTION =
        "checkArchiveUnitProfileActionPlugin/archive-unit_KO_DescriptionLevel.json";

    private static final String ARCHIVE_UNIT_PROFILE_SCHEMA =
        "checkArchiveUnitProfileActionPlugin/archive-unit-profile.json";
    private static final String ARCHIVE_UNIT_PROFILE_SCHEMA_CONVERAGE =
        "checkArchiveUnitProfileActionPlugin/archive-unit-profile-with-mandatory-coverage.json";
    private static final String ARCHIVE_UNIT_PROFILE_SCHEMA_CUSTOM_DESCRIPTION_LEVEL =
        "checkArchiveUnitProfileActionPlugin/archive-unit-profile-description-level.json";

    private static final String GUID_MAP_JSON = "GUID_TO_ARCHIVE_ID_MAP.json";

    private HandlerIOImpl handlerIO;
    private GUID guid = GUIDFactory.newGUID();

    private final WorkerParameters params = WorkerParametersFactory.newWorkerParameters()
        .setUrlWorkspace("http://localhost:8083")
        .setUrlMetadata("http://localhost:8083")
        .setObjectNameList(Lists.newArrayList("archiveUnit.json"))
        .setObjectName("archiveUnit.json")
        .setCurrentStep("currentStep")
        .setContainerName(guid.getId())
        .setLogbookTypeProcess(LogbookTypeProcess.INGEST);

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        workspaceClient = mock(WorkspaceClient.class);
        WorkspaceClientFactory workspaceClientFactory = mock(WorkspaceClientFactory.class);
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);

        adminManagementClient = mock(AdminManagementClient.class);
        AdminManagementClientFactory adminManagementClientFactory = mock(AdminManagementClientFactory.class);
        when(adminManagementClientFactory.getClient()).thenReturn(adminManagementClient);

        LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory = mock(LogbookLifeCyclesClientFactory.class);
        LogbookLifeCyclesClient logbookLifeCyclesClient = mock(LogbookLifeCyclesClient.class);
        when(logbookLifeCyclesClientFactory.getClient()).thenReturn(logbookLifeCyclesClient);

        UnitValidator unitValidator = new UnitValidator(
            new CachedArchiveUnitProfileLoader(adminManagementClientFactory, 100, 300),
            new CachedSchemaValidatorLoader(100, 300)
        );
        plugin = new CheckArchiveUnitProfileActionPlugin(unitValidator);

        String objectId = "objectId";
        handlerIO = new HandlerIOImpl(
            workspaceClientFactory,
            logbookLifeCyclesClientFactory,
            guid.getId(),
            "workerId",
            com.google.common.collect.Lists.newArrayList(objectId)
        );
        handlerIO.setCurrentObjectId(objectId);

        List<IOParameter> out = new ArrayList<>();
        out.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.MEMORY, "unitId.json")));
        handlerIO.addOutIOParameters(out);

        List<IOParameter> in = new ArrayList<>();
        in.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.WORKSPACE, "Maps/GUID_TO_ARCHIVE_ID_MAP.json")));
        final InputStream guidMapInfo = PropertiesUtils.getResourceAsStream(GUID_MAP_JSON);
        when(workspaceClient.getObject(any(), eq("Maps/GUID_TO_ARCHIVE_ID_MAP.json"))).thenReturn(
            Response.status(Status.OK).entity(guidMapInfo).build()
        );
        handlerIO.addInIOParameters(in);

        File tempFolder = temporaryFolder.newFolder();
        SystemPropertyUtil.set("vitam.tmp.folder", tempFolder.getAbsolutePath());
    }

    @After
    public void clean() {
        handlerIO.partialClose();
    }

    @Test
    public void givenWorkspaceNotExistWhenExecuteThenReturnResponseFATAL() throws ProcessingException {
        final ItemStatus response = plugin.execute(params, handlerIO);
        assertEquals(response.getGlobalStatus(), StatusCode.FATAL);
    }

    @Test
    public void givenCorrectArchiveUnitJsonWhenExecuteThenReturnResponseOK() throws Exception {
        givenArchiveUnit(ARCHIVE_UNIT);
        givenArchiveUnitProfile(ARCHIVE_UNIT_PROFILE_SCHEMA);

        final ItemStatus response = plugin.execute(params, handlerIO);
        assertEquals(response.getGlobalStatus(), OK);
    }

    @Test
    public void givenInvalidArchiveUnitXMLWhenExecuteThenReturnResponseKO() throws Exception {
        // Invalid archive unit -> XML File
        givenArchiveUnit(ARCHIVE_UNIT_INVALID_XML);
        givenArchiveUnitProfile(ARCHIVE_UNIT_PROFILE_SCHEMA);

        final ItemStatus response = plugin.execute(params, handlerIO);
        assertEquals(response.getGlobalStatus(), FATAL);
    }

    @Test
    public void givenNoCoverageArchiveUnitJsonWhenExecuteThenReturnResponseKO() throws Exception {
        // archive unit has no Coverage when schema requires one
        givenArchiveUnit(ARCHIVE_UNIT);
        givenArchiveUnitProfile(ARCHIVE_UNIT_PROFILE_SCHEMA_CONVERAGE);

        final ItemStatus response = plugin.execute(params, handlerIO);
        assertEquals(response.getGlobalStatus(), KO);
        assertThat(response.getGlobalOutcomeDetailSubcode()).isEqualTo(
            CheckArchiveUnitProfileActionPlugin.OUTCOME_DETAILS_NOT_AU_JSON_VALID
        );
    }

    @Test
    public void givenInvalidDescriptionLevelInArchiveUnitJsonWhenExecuteThenReturnResponseKO() throws Exception {
        // archive unit has no description when schema requires one
        givenArchiveUnit(ARCHIVE_UNIT_INVALID_DESCRIPTION);
        givenArchiveUnitProfile(ARCHIVE_UNIT_PROFILE_SCHEMA_CUSTOM_DESCRIPTION_LEVEL);

        final ItemStatus response = plugin.execute(params, handlerIO);
        assertEquals(response.getGlobalStatus(), KO);
        assertThat(response.getGlobalOutcomeDetailSubcode()).isEqualTo(
            CheckArchiveUnitProfileActionPlugin.OUTCOME_DETAILS_NOT_AU_JSON_VALID
        );
    }

    @Test
    public void givenInactiveSchemaControlAUProfileInArchiveUnitJsonWhenExecuteThenReturnResponseKO() throws Exception {
        // archive unit has no description when schema requires one
        givenArchiveUnit(ARCHIVE_UNIT);
        //Inactive Status
        givenArchiveUnitProfile(
            ARCHIVE_UNIT_PROFILE_SCHEMA,
            ArchiveUnitProfileStatus.INACTIVE,
            CtrlSchemaValueSetterFlagEnum.NOT_SET
        );

        ItemStatus response = plugin.execute(params, handlerIO);
        assertEquals(KO, response.getGlobalStatus());
        assertThat(response.getGlobalOutcomeDetailSubcode()).isEqualTo(
            CheckArchiveUnitProfileActionPlugin.OUTCOME_DETAILS_INACTIVE_STATUS
        );
    }

    @Test
    public void givenNotSetSchemaControlAUProfileInArchiveUnitJsonWhenExecuteThenReturnResponseKO() throws Exception {
        // archive unit has no description when schema requires one
        givenArchiveUnit(ARCHIVE_UNIT);

        //Empty control schema
        givenArchiveUnitProfile(
            ARCHIVE_UNIT_PROFILE_SCHEMA,
            ArchiveUnitProfileStatus.ACTIVE,
            CtrlSchemaValueSetterFlagEnum.NOT_SET
        );

        ItemStatus response = plugin.execute(params, handlerIO);
        assertEquals(KO, response.getGlobalStatus());
        assertThat(response.getGlobalOutcomeDetailSubcode()).isEqualTo(
            CheckArchiveUnitProfileActionPlugin.OUTCOME_DETAILS_EMPTY_CONTROL_SCHEMA
        );
    }

    @Test
    public void givenEmptySchemaControlAUProfileInArchiveUnitJsonWhenExecuteThenReturnResponseKO() throws Exception {
        // archive unit has no description when schema requires one
        givenArchiveUnit(ARCHIVE_UNIT);

        //JSon Empty control schema
        givenArchiveUnitProfile(
            ARCHIVE_UNIT_PROFILE_SCHEMA,
            ArchiveUnitProfileStatus.ACTIVE,
            CtrlSchemaValueSetterFlagEnum.SET_AS_EMPTY_JSON
        );

        ItemStatus response = plugin.execute(params, handlerIO);
        assertEquals(KO, response.getGlobalStatus());
        assertThat(response.getGlobalOutcomeDetailSubcode()).isEqualTo(
            CheckArchiveUnitProfileActionPlugin.OUTCOME_DETAILS_EMPTY_CONTROL_SCHEMA
        );
    }

    @Test
    public void givenValidSchemaControlAUProfileInArchiveUnitJsonWhenExecuteThenReturnResponseOK() throws Exception {
        // archive unit has no description when schema requires one
        givenArchiveUnit(ARCHIVE_UNIT);

        //All OK
        givenArchiveUnitProfile(
            ARCHIVE_UNIT_PROFILE_SCHEMA,
            ArchiveUnitProfileStatus.ACTIVE,
            CtrlSchemaValueSetterFlagEnum.SET
        );

        ItemStatus response = plugin.execute(params, handlerIO);
        assertEquals(OK, response.getGlobalStatus());
    }

    private RequestResponse<ArchiveUnitProfileModel> createArchiveUnitProfile(InputStream schema)
        throws InvalidParseOperationException {
        ArchiveUnitProfileModel archiveUnitProfileModel = new ArchiveUnitProfileModel();
        archiveUnitProfileModel.setIdentifier("AUP_0001");
        archiveUnitProfileModel.setId(GUIDFactory.newProfileGUID(0).toString());
        archiveUnitProfileModel.setStatus(ArchiveUnitProfileStatus.ACTIVE);

        JsonNode node = JsonHandler.getFromInputStream(schema);
        archiveUnitProfileModel.setControlSchema(JsonHandler.unprettyPrint(node));
        return ClientMockResultHelper.createResponse(archiveUnitProfileModel);
    }

    private RequestResponse<ArchiveUnitProfileModel> createCustomArchiveUnitProfile(
        InputStream schema,
        ArchiveUnitProfileStatus aupStatus,
        CtrlSchemaValueSetterFlagEnum ctrlSchemaSetterFlag
    ) throws InvalidParseOperationException {
        ArchiveUnitProfileModel archiveUnitProfileModel = new ArchiveUnitProfileModel();
        archiveUnitProfileModel.setIdentifier("AUP_0001");
        archiveUnitProfileModel.setId(GUIDFactory.newProfileGUID(0).toString());
        archiveUnitProfileModel.setStatus(aupStatus);

        if (CtrlSchemaValueSetterFlagEnum.SET.equals(ctrlSchemaSetterFlag)) {
            JsonNode node = JsonHandler.getFromInputStream(schema);
            archiveUnitProfileModel.setControlSchema(JsonHandler.unprettyPrint(node));
        }

        if (CtrlSchemaValueSetterFlagEnum.SET_AS_EMPTY_JSON.equals(ctrlSchemaSetterFlag)) {
            archiveUnitProfileModel.setControlSchema("{}");
        }

        return ClientMockResultHelper.createResponse(archiveUnitProfileModel);
    }

    private void givenArchiveUnit(String archiveUnit)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException, FileNotFoundException {
        when(workspaceClient.getObject(any(), eq("Units/archiveUnit.json"))).thenReturn(
            Response.status(Status.OK).entity(PropertiesUtils.getResourceAsStream(archiveUnit)).build()
        );
    }

    private void givenArchiveUnitProfile(String archiveUnitSchema)
        throws InvalidParseOperationException, AdminManagementClientServerException, ReferentialNotFoundException, FileNotFoundException {
        when(adminManagementClient.findArchiveUnitProfilesByID("AUP_0001")).thenReturn(
            createArchiveUnitProfile(PropertiesUtils.getResourceAsStream(archiveUnitSchema))
        );
    }

    private void givenArchiveUnitProfile(
        String resourceFile,
        ArchiveUnitProfileStatus inactive,
        CtrlSchemaValueSetterFlagEnum notSet
    )
        throws InvalidParseOperationException, AdminManagementClientServerException, ReferentialNotFoundException, FileNotFoundException {
        when(adminManagementClient.findArchiveUnitProfilesByID("AUP_0001")).thenReturn(
            createCustomArchiveUnitProfile(PropertiesUtils.getResourceAsStream(resourceFile), inactive, notSet)
        );
    }

    private enum CtrlSchemaValueSetterFlagEnum {
        SET,
        NOT_SET,
        SET_AS_EMPTY_JSON,
    }
}

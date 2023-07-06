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
package fr.gouv.vitam.worker.core.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.IOParameter;
import fr.gouv.vitam.common.model.processing.ProcessingUri;
import fr.gouv.vitam.common.model.processing.UriPrefix;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.common.tmp.TempFolderRule;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.metadata.core.database.collections.Unit;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.core.extractseda.IngestSession;
import fr.gouv.vitam.worker.core.impl.HandlerIOImpl;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.api.model.FileParams;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.gouv.vitam.common.json.JsonHandler.getFromInputStream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ExtractSedaActionHandlerTest {

    private static final String INGEST_CONTRACT_MASTER_MANDATORY_FALSE =
        "checkMasterMandatoryInOGAndAttachmentInOG/IngestContractMasterMandatoryIsFalse.json";
    private static final String INGEST_CONTRACT_MASTER_MANDATORY_TRUE =
        "checkMasterMandatoryInOGAndAttachmentInOG/IngestContractMasterMandatoryIsTrue.json";
    private static final String INGEST_CONTRACT_EVERYDATAOBJECTVERSION_FALSE =
        "checkMasterMandatoryInOGAndAttachmentInOG/IngestContractEveryDataObjectVersionIsFalse.json";
    private static final String INGEST_CONTRACT_EVERYDATAOBJECTVERSION_TRUE =
        "checkMasterMandatoryInOGAndAttachmentInOG/IngestContractEveryDataObjectVersionIsTrue.json";
    private static final String INGEST_CONTRACT_NO_CHECK =
        "checkMasterMandatoryInOGAndAttachmentInOG/IngestContractNoCheck.json";
    private static final String MANIFEST_WITHOUT_BINARY_OR_PHYSICAL =
        "checkMasterMandatoryInOGAndAttachmentInOG/DataObjectGroupDontContainMaster.xml";
    private static final String MANIFEST_WITH_BINARYMASTER =
        "checkMasterMandatoryInOGAndAttachmentInOG/DataObjectGroupAttachementToExistingOne.xml";
    private static final String MANIFEST_WITH_ATTACHMENT_AND_USAGES_WITHOUT_MASTER =
        "checkMasterMandatoryInOGAndAttachmentInOG/DataObjectGroupAttachmentToExistingWithTunbnail.xml";
    private static final String MANIFEST_WITH_ATTACHMENT_AND_USAGES_WITH_MASTER =
        "checkMasterMandatoryInOGAndAttachmentInOG/DataObjectGroupAttachmentToExistingWithTunbnailAndOtherMaster.xml";
    private static final String MANIFEST_WITH_ATTACHMENT_TO_EXISTANT_WITH_DIFFERENT_SP =
        "checkMasterMandatoryInOGAndAttachmentInOG/DataObjectGroupAttachmentToExistingWithDifferentOriginatingAgency.xml";
    private static final String UNIT_ATTACHED_DB_RESPONSE = "extractSedaActionHandler/addLink/_Unit_CHILD.json";
    private static final String UNIT_ATTACHED_SP_DB_RESPONSE = "extractSedaActionHandler/addLink/Unit_Link.json";
    private static final String SEDA_PARAMS_FIELD = "Maps/sedaParams.json";
    private ExtractSedaActionHandler handler;

    private static final String HANDLER_ID = "CHECK_MANIFEST";
    private static final String SIP_TEST = "extractSedaActionHandler/rules-test.xml";
    private static final String SIP_ADD_LINK = "extractSedaActionHandler/addLink/SIP_Add_Link.xml";
    private static final String SIP_ADD_UNIT = "extractSedaActionHandler/addUnit/SIP_Add_Unit.xml";
    private static final String OK_EHESS_LIGHT = "extractSedaActionHandler/OK_EHESS_LIGHT.xml";
    private static final String MERCIER = "extractSedaActionHandler/Mercier.xml";
    private static final String EMPTY_TEXT_TYPE = "extractSedaActionHandler/empty_text_type.xml";

    private static final String SIP_GOT_MULTI_VERSION = "extractSedaActionHandler/got_multi_version.xml";

    private static final String SIP_GOT_MULTI_VERSION_NO_BINARY_MASTER = "extractSedaActionHandler/got_multi_version_without_binary_master.xml";
    private static final String SIP_RULES = "extractSedaActionHandler/rules-test.xml";
    private static final String SIP_ARBO_RULES_MD = "extractSedaActionHandler/OK_arbo_RG_MD_complexe.xml";
    private static final String SIP_RULES_INHERITANCE = "extractSedaActionHandler/1066_SIP_RULES_INHERITENCE.xml";
    private static final String SIP_REFID_RULES_INHERITANCE =
        "extractSedaActionHandler/1069_SIP_REFID_RULES_INHERITENCE.xml";
    private static final String SIP_RULES_COMPLEXE =
        "extractSedaActionHandler/complexe_rules_global_management.xml";
    private static final String SIP_REFNONRULEID_PREVENTINHERITANCE =
        "extractSedaActionHandler/refnonruleid_and_preventinheritence.xml";
    private static final String SIP_REFNONRULEID_MULT_PREVENTINHERITANCE =
        "extractSedaActionHandler/refnonruleid_multiple_and_preventinheritence.xml";
    private static final String SIP_PHYSICAL_DATA_OBJECT = "extractSedaActionHandler/SIP_PHYSICAL_DATA_OBJECT.xml";
    private static final String SIP_GROUP_PHYSICAL_DATA_OBJECT =
        "extractSedaActionHandler/SIP_WRAPPED_PHYSICAL_DATA_OBJECT.xml";
    private static final String SIP_WITH_SPECIAL_CHARACTERS =
        "extractSedaActionHandler/SIP_WITH_SPECIAL_CHARACTERS.xml";
    private static final String SIP_ARBORESCENCE = "SIP_Arborescence.xml";
    private static final String STORAGE_INFO_JSON = "extractSedaActionHandler/storageInfo.json";
    private static final String STORAGE_INFO_MC_JSON = "extractSedaActionHandler/storageInfo_mc.json";
    private static final String CONTRACTS_JSON = "extractSedaActionHandler/contracts.json";
    private static final String SEDA_PARAMS = "extractSedaActionHandler/SedaParams.json";
    private static final String CONTRACTS_MC_JSON = "extractSedaActionHandler/contracts_mc.json";
    private static final String INGEST_CONTRACT_JSON_COMPUTEINHERITEDRULESATINGEST =
        "extractSedaActionHandler/INGEST_CONTRACT_COMPUTEINHERITEDRULESATINGEST.json";
    private static final String OK_MULTI_COMMENT = "extractSedaActionHandler/OK_multi_comment.xml";
    private static final String OK_SIGNATURE = "extractSedaActionHandler/signature.xml";
    private static final String OK_RULES_WOUT_ID = "extractSedaActionHandler/manifestRulesWithoutId.xml";
    private static final String KO_CYCLE = "extractSedaActionHandler/KO_cycle.xml";
    private static final String KO_AU_REF_OBJ = "extractSedaActionHandler/KO_AU_REF_OBJ.xml";
    private static final String ARCHIVE_UNIT = "ArchiveUnit";
    private static final String EMPTY_SIZE_TYPE = "extractSedaActionHandler/empty_size_type.xml";
    private static final String INCORRECT_SIZE_TYPE = "extractSedaActionHandler/incorrect_size_type.xml";

    private static final String OBJECT_ID = "SIP/manifest.xml";
    private List<IOParameter> out;
    private List<IOParameter> in;
    private static final Integer TENANT_ID = 0;
    private final WorkerParameters params =
        WorkerParametersFactory.newWorkerParameters().setUrlWorkspace("http://localhost:8083")
            .setUrlMetadata("http://localhost:8083")
            .setObjectNameList(Lists.newArrayList("objectName.json"))
            .setObjectName("objectName.json").setCurrentStep("currentStep")
            .setLogbookTypeProcess(LogbookTypeProcess.INGEST)
            .setContainerName("ExtractSedaActionHandlerTest");

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @Rule
    public TempFolderRule folder = new TempFolderRule();

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private WorkspaceClient workspaceClient;

    @Mock
    private MetaDataClient metadataClient;

    @Mock
    private WorkspaceClientFactory workspaceClientFactory;

    @Mock
    private MetaDataClientFactory metadataClientFactory;

    @Mock
    private AdminManagementClientFactory adminManagementClientFactory;

    @Mock
    private AdminManagementClient adminManagementClient;

    @Mock
    private LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory;

    @Mock
    private LogbookLifeCyclesClient logbookLifeCyclesClient;

    @InjectMocks
    private ExtractSedaHandlerIO handlerIO;

    @Spy
    private IngestSession ingestSession;

    @Before
    public void setUp() throws Exception {

        reset(workspaceClient);

        LogbookOperationsClientFactory.changeMode(null);
        when(adminManagementClientFactory.getClient()).thenReturn(adminManagementClient);
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);
        when(metadataClientFactory.getClient()).thenReturn(metadataClient);
        when(logbookLifeCyclesClientFactory.getClient()).thenReturn(logbookLifeCyclesClient);

        handler = new ExtractSedaActionHandler(metadataClientFactory, adminManagementClientFactory, logbookLifeCyclesClientFactory);

        Map<String,Long> filesWithParamsFromWorkspace =  (Map<String,Long>) mock(Map.class);
        when(filesWithParamsFromWorkspace.get(anyString())).thenReturn(0L);
        when(ingestSession.getFileWithParmsFromFolder()).thenReturn(filesWithParamsFromWorkspace);

        handlerIO.setCurrentObjectId(OBJECT_ID);

        out = new ArrayList<>();
        out.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.WORKSPACE, "UnitsLevel/ingestLevelStack.json")));
        out.add(
            new IOParameter()
                .setUri(new ProcessingUri(UriPrefix.WORKSPACE, "Maps/DATA_OBJECT_TO_OBJECT_GROUP_ID_MAP.json")));
        out.add(new IOParameter()
            .setUri(new ProcessingUri(UriPrefix.WORKSPACE, "Maps/DATA_OBJECT_ID_TO_GUID_MAP.json")));
        out.add(
            new IOParameter().setUri(new ProcessingUri(UriPrefix.WORKSPACE, "Maps/OBJECT_GROUP_ID_TO_GUID_MAP.json")));
        out.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.MEMORY, "MapsMemory/OG_TO_ARCHIVE_ID_MAP.json")));
        out.add(new IOParameter()
            .setUri(new ProcessingUri(UriPrefix.WORKSPACE, "Maps/DATA_OBJECT_ID_TO_DATA_OBJECT_DETAIL_MAP.json")));
        out.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.WORKSPACE, "Maps/ARCHIVE_ID_TO_GUID_MAP.json")));
        out.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.WORKSPACE, "ATR/globalSEDAParameters.json")));
        out.add(new IOParameter()
            .setUri(new ProcessingUri(UriPrefix.MEMORY, "MapsMemory/OBJECT_GROUP_ID_TO_GUID_MAP.json")));
        out.add(new IOParameter()
            .setUri(new ProcessingUri(UriPrefix.WORKSPACE, "UpdateObjectGroup/existing_object_group.json")));
        out.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.WORKSPACE, "Maps/GUID_TO_ARCHIVE_ID_MAP.json")));

        out.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.WORKSPACE, "Ontology/ontology.json")));

        out.add(new IOParameter().setUri(
            new ProcessingUri(UriPrefix.WORKSPACE, "Maps/EXISTING_GOT_TO_NEW_GOT_GUID_FOR_ATTACHMENT_MAP.json")));

        out.add(new IOParameter().setUri(
            new ProcessingUri(UriPrefix.WORKSPACE, "Maps/EXISTING_UNITS_GUID_FOR_ATTACHMENT_MAP.json")));

        out.add(new IOParameter().setUri(
            new ProcessingUri(UriPrefix.WORKSPACE, "Maps/EXISTING_GOTS_GUID_FOR_ATTACHMENT_MAP.json")));

        in = new ArrayList<>();
        in.add(new IOParameter()
            .setUri(new ProcessingUri(UriPrefix.VALUE, "false")));
        in.add(new IOParameter()
            .setUri(new ProcessingUri(UriPrefix.VALUE, "INGEST")));
        in.add(new IOParameter()
            .setUri(new ProcessingUri(UriPrefix.WORKSPACE, "StorageInfo/storageInfo.json")));
        in.add(new IOParameter()
            .setUri(new ProcessingUri(UriPrefix.WORKSPACE, "referential/contracts.json")));

        final InputStream storageInfo = PropertiesUtils.getResourceAsStream(STORAGE_INFO_JSON);
        when(workspaceClient.getObject(any(), eq("StorageInfo/storageInfo.json")))
            .thenReturn(Response.status(Status.OK).entity(storageInfo).build());
        final InputStream ingestContract = PropertiesUtils.getResourceAsStream(CONTRACTS_JSON);
        when(workspaceClient.getObject(any(), eq("referential/contracts.json")))
                .thenReturn(Response.status(Status.OK).entity(ingestContract).build());
        final InputStream sedaParams = PropertiesUtils.getResourceAsStream(SEDA_PARAMS);
        when(workspaceClient.isExistingObject(any(), eq(SEDA_PARAMS_FIELD))).thenReturn(true);
        when(workspaceClient.getObject(any(), eq(SEDA_PARAMS_FIELD)))
            .thenReturn(Response.status(Status.OK).entity(sedaParams).build());
        when(workspaceClient.isExistingFolder(any(), any())).thenReturn(true);
        handlerIO.addInIOParameters(in);
    }

    @After
    public void clean() {
        handlerIO.partialClose();
        reset(ingestSession);
    }

    @Test
    @RunWithCustomExecutor
    public void givenWorkspaceNotExistWhenExecuteThenReturnResponseFATAL() {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertNotNull(ExtractSedaActionHandler.getId());
        assertEquals(ExtractSedaActionHandler.getId(), HANDLER_ID);

        handlerIO.addOutIOParameters(out);
        final ItemStatus response = handler.execute(params, handlerIO, ingestSession);
        assertEquals(StatusCode.FATAL, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void givenWorkspaceExistWhenExecuteThenReturnResponseOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertNotNull(ExtractSedaActionHandler.getId());
        prepareIngestContractsAndManagementContracts(INGEST_CONTRACT_MASTER_MANDATORY_FALSE);
        final InputStream seda_arborescence =
            PropertiesUtils.getResourceAsStream(SIP_ARBORESCENCE);
        when(workspaceClient.getObject(any(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(seda_arborescence).build());
        handlerIO.addOutIOParameters(out);

        final ItemStatus response = handler.execute(params, handlerIO, ingestSession);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
        assertTrue(response.getEvDetailData()
            .contains("ArchivalProfile0"));
    }

    @Test
    @RunWithCustomExecutor
    public void givenManifestEhessWhenExecuteThenReturnResponseOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertNotNull(ExtractSedaActionHandler.getId());

        final InputStream seda_arborescence =
            PropertiesUtils.getResourceAsStream(OK_EHESS_LIGHT);
        prepareIngestContractsAndManagementContracts(INGEST_CONTRACT_MASTER_MANDATORY_FALSE);
        when(workspaceClient.getObject(any(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(seda_arborescence).build());
        handlerIO.addOutIOParameters(out);

        final ItemStatus response = handler.execute(params, handlerIO, ingestSession);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }


    @Test
    @RunWithCustomExecutor
    public void givenManifestWithEmptyTextTypeThenOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertNotNull(ExtractSedaActionHandler.getId());
        prepareIngestContractsAndManagementContracts(INGEST_CONTRACT_MASTER_MANDATORY_FALSE);
        final InputStream seda_arborescence =
            PropertiesUtils.getResourceAsStream(EMPTY_TEXT_TYPE);
        when(workspaceClient.getObject(any(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(seda_arborescence).build());
        handlerIO.addOutIOParameters(out);

        final ItemStatus response = handler.execute(params, handlerIO, ingestSession);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }


    @Test
    @RunWithCustomExecutor
    public void givenManifestWithGotMultiVersionThenOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertNotNull(ExtractSedaActionHandler.getId());
        prepareIngestContractsAndManagementContracts(INGEST_CONTRACT_MASTER_MANDATORY_FALSE);
        final InputStream seda_arborescence =
            PropertiesUtils.getResourceAsStream(SIP_GOT_MULTI_VERSION);
        when(workspaceClient.getObject(any(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(seda_arborescence).build());
        handlerIO.addOutIOParameters(out);

        final ItemStatus response = handler.execute(params, handlerIO, ingestSession);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void givenManifestWithGotMultiVersionAndNoBinaryMaster1ThenKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertNotNull(ExtractSedaActionHandler.getId());
        prepareIngestContractsAndManagementContracts(INGEST_CONTRACT_MASTER_MANDATORY_FALSE);
        final InputStream seda_arborescence =
            PropertiesUtils.getResourceAsStream(SIP_GOT_MULTI_VERSION_NO_BINARY_MASTER);
        when(workspaceClient.getObject(any(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(seda_arborescence).build());
        handlerIO.addOutIOParameters(out);

        final ItemStatus response = handler.execute(params, handlerIO, ingestSession);
        assertEquals(StatusCode.KO, response.getGlobalStatus());
    }


    @Test
    @RunWithCustomExecutor
    public void givenManifestMercierWhenExecuteThenReturnResponseOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertNotNull(ExtractSedaActionHandler.getId());
        prepareIngestContractsAndManagementContracts(INGEST_CONTRACT_MASTER_MANDATORY_FALSE);
        final InputStream seda_arborescence =
            PropertiesUtils.getResourceAsStream(MERCIER);
        when(workspaceClient.getObject(any(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(seda_arborescence).build());
        handlerIO.addOutIOParameters(out);

        final ItemStatus response = handler.execute(params, handlerIO, ingestSession);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void givenManifestRulesWhenExecuteThenReturnResponseOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertNotNull(ExtractSedaActionHandler.getId());
        prepareIngestContractsAndManagementContracts(INGEST_CONTRACT_MASTER_MANDATORY_FALSE);
        final InputStream seda_arborescence =
            PropertiesUtils.getResourceAsStream(SIP_RULES);
        when(workspaceClient.getObject(any(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(seda_arborescence).build());
        handlerIO.addOutIOParameters(out);

        final ItemStatus response = handler.execute(params, handlerIO, ingestSession);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void givenManifestArboRulesMDWhenExecuteThenReturnResponseOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertNotNull(ExtractSedaActionHandler.getId());
        prepareIngestContractsAndManagementContracts(INGEST_CONTRACT_NO_CHECK);
        final InputStream seda_arborescence =
            PropertiesUtils.getResourceAsStream(SIP_ARBO_RULES_MD);
        when(workspaceClient.getObject(any(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(seda_arborescence).build());
        handlerIO.addOutIOParameters(out);

        final ItemStatus response = handler.execute(params, handlerIO, ingestSession);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void givenSipWithBdoWithoutGoWhenReadSipThenDetectBdoWithoutGo() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        prepareIngestContractsAndManagementContracts(INGEST_CONTRACT_NO_CHECK);
        final InputStream sedaLocal = new FileInputStream(PropertiesUtils.findFile("sip-bdo-orphan-ok1.xml"));
        when(workspaceClient.getObject(any(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(sedaLocal).build());
        handlerIO.addOutIOParameters(out);
        final ItemStatus response = handler.execute(params, handlerIO, ingestSession);
        assertNotNull(ingestSession);
        assertThat(ingestSession.getUnitIdToGroupId())
            .containsEntry("ID035", ingestSession.getDataObjectIdToObjectGroupId().get("ID009"))
            .containsEntry("ID015", ingestSession.getDataObjectIdToObjectGroupId().get("ID011"));
        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void givenSipWithBdoWithGoWithArchiveUnitReferenceGoWhenReadSipThenReadSuccess() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        prepareIngestContractsAndManagementContracts(INGEST_CONTRACT_MASTER_MANDATORY_FALSE);
        final InputStream sedaLocal = new FileInputStream(PropertiesUtils.findFile("sip-bdo-orphan-ok2.xml"));
        when(workspaceClient.getObject(any(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(sedaLocal).build());
        handlerIO.addOutIOParameters(out);
        final ItemStatus response = handler.execute(params, handlerIO, ingestSession);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void givenSipFctTestWithBdoWithGoWithArchiveUnitReferenceBDOWhenReadSipThenThrowException()
        throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        prepareIngestContractsAndManagementContracts(INGEST_CONTRACT_MASTER_MANDATORY_FALSE);
        final InputStream sedaLocal = new FileInputStream(PropertiesUtils.findFile("sip-bdo-orphan-ok3-listBDO.xml"));
        when(workspaceClient.getObject(any(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(sedaLocal).build());
        handlerIO.addOutIOParameters(out);
        final ItemStatus response = handler.execute(params, handlerIO, ingestSession);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void givenSipWithBdoWithGoWithArchiveUnitNotReferenceGoWhenReadSipThenReadSuccess() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        prepareIngestContractsAndManagementContracts(INGEST_CONTRACT_MASTER_MANDATORY_FALSE);
        final InputStream sedaLocal = new FileInputStream(PropertiesUtils.findFile("sip-bdo-orphan-ok4.xml"));
        when(workspaceClient.getObject(any(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(sedaLocal).build());
        handlerIO.addOutIOParameters(out);
        final ItemStatus response = handler.execute(params, handlerIO, ingestSession);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void givenSipWithDoubleBMThenKO()
        throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        prepareIngestContractsAndManagementContracts(INGEST_CONTRACT_MASTER_MANDATORY_FALSE);
        final InputStream sedaLocal = new FileInputStream(PropertiesUtils.findFile("manifest_doubleBM.xml"));
        when(workspaceClient.getObject(any(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(sedaLocal).build());
        handlerIO.addOutIOParameters(out);
        final ItemStatus response = handler.execute(params, handlerIO, ingestSession);
        assertEquals(StatusCode.KO, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void givenSipTransformToUsage_1ThenSuccess() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        prepareIngestContractsAndManagementContracts(INGEST_CONTRACT_MASTER_MANDATORY_FALSE);
        final InputStream sedaLocal = new FileInputStream(PropertiesUtils.findFile("manifest_BM_TC.xml"));
        when(workspaceClient.getObject(any(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(sedaLocal).build());
        handlerIO.addOutIOParameters(out);
        final ItemStatus response = handler.execute(params, handlerIO, ingestSession);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void givenSipWithBdoWithGoWithArchiveUnitReferenceGoWhenReadSipThenThrowException() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        prepareIngestContractsAndManagementContracts(INGEST_CONTRACT_MASTER_MANDATORY_FALSE);
        final InputStream sedaLocal = new FileInputStream(PropertiesUtils.findFile("sip-bdo-orphan-err2.xml"));
        when(workspaceClient.getObject(any(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(sedaLocal).build());
        handlerIO.addOutIOParameters(out);
        final ItemStatus response = handler.execute(params, handlerIO, ingestSession);
        assertNotNull(ingestSession);
        assertThat(ingestSession.getUnitIdToGroupId()).containsEntry("ID015", "ID011");
        assertEquals(StatusCode.KO, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void givenManifestWithMngMdAndAuWithMngtWhenExtractSedaThenReadSuccess() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        final InputStream sedaLocal = new FileInputStream(PropertiesUtils.findFile("sip-management-metadata-ok1.xml"));
        prepareIngestContractsAndManagementContracts(INGEST_CONTRACT_MASTER_MANDATORY_FALSE);
        when(workspaceClient.getObject(any(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(sedaLocal).build());
        handlerIO.addOutIOParameters(out);
        final ItemStatus response = handler.execute(params, handlerIO, ingestSession);

        assertEquals(StatusCode.OK, response.getGlobalStatus());
        assertNotNull(ingestSession);
        assertThat(ingestSession.getUnitIdToSetOfRuleId().get("ID015")).containsExactlyInAnyOrder(
            "ID022", "ID024", "ID020", "ID018", "ID019", "ID025", "ID017");
    }

    @Test
    @RunWithCustomExecutor
    public void should_not_fill_unitIdToSetOfRuleId_when_rules_has_no_ruleid() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        prepareIngestContractsAndManagementContracts(INGEST_CONTRACT_MASTER_MANDATORY_FALSE);
        final InputStream sedaLocal =
            new FileInputStream(PropertiesUtils.findFile("sip_with_rules_without_rule_id.xml"));

        when(workspaceClient.getObject(any(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(sedaLocal).build());
        handlerIO.addOutIOParameters(out);
        final ItemStatus response = handler.execute(params, handlerIO, ingestSession);

        assertEquals(StatusCode.OK, response.getGlobalStatus());
        assertNotNull(ingestSession);
        assertThat(ingestSession.getUnitIdToSetOfRuleId().get("ID015")).containsExactlyInAnyOrder(
            "ID022", "ID020", "ID018", "ID019", "ID025", "ID017");
    }

    @Test
    @RunWithCustomExecutor
    public void givenManifestWithMngMdAndAuTreeWhenExtractSedaThenReadSuccess() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        prepareIngestContractsAndManagementContracts(INGEST_CONTRACT_MASTER_MANDATORY_FALSE);
        final InputStream sedaLocal = new FileInputStream(PropertiesUtils.findFile("sip-management-metadata-ok2.xml"));

        when(workspaceClient.getObject(any(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(sedaLocal).build());
        handlerIO.addOutIOParameters(out);
        final ItemStatus response = handler.execute(params, handlerIO, ingestSession);

        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void given_manifest_with_arbo1() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        prepareIngestContractsAndManagementContracts(INGEST_CONTRACT_NO_CHECK);
        final InputStream sedaLocal = new FileInputStream(PropertiesUtils.findFile("SIP_ARBO.xml"));

        when(workspaceClient.getObject(any(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(sedaLocal).build());
        handlerIO.addOutIOParameters(out);
        final ItemStatus response = handler.execute(params, handlerIO, ingestSession);

        assertThat(response.getGlobalStatus()).isEqualTo(StatusCode.OK);
        assertNotNull(ingestSession);
        assertThat(ingestSession.getArchiveUnitTree().get("ID027")).isEqualTo(JsonHandler.createObjectNode());
        assertThat(ingestSession.getArchiveUnitTree().get("ID028")).isEqualTo(createObjectNodeWithUpValue("ID027"));
        assertThat(ingestSession.getArchiveUnitTree().get("ID029")).isEqualTo(createObjectNodeWithUpValue("ID028", "ID030"));
        assertThat(ingestSession.getArchiveUnitTree().get("ID030")).isEqualTo(createObjectNodeWithUpValue("ID027"));
        assertThat(ingestSession.getArchiveUnitTree().get("ID031")).isEqualTo(createObjectNodeWithUpValue("ID027"));
    }

    @Test
    @RunWithCustomExecutor
    public void given_manifest_with_arbo2() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        prepareIngestContractsAndManagementContracts(INGEST_CONTRACT_NO_CHECK);
        final InputStream sedaLocal = new FileInputStream(PropertiesUtils.findFile("SIP_ARBO2.xml"));

        when(workspaceClient.getObject(any(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(sedaLocal).build());
        handlerIO.addOutIOParameters(out);
        final ItemStatus response = handler.execute(params, handlerIO, ingestSession);
        assertNotNull(ingestSession);
        assertThat(ingestSession.getArchiveUnitTree().get("ID3")).as("comparison failed for ID: ID3")
            .isEqualTo(JsonHandler.createObjectNode());
        assertThat(ingestSession.getArchiveUnitTree().get("ID4")).as("comparison failed for ID: ID4")
            .isEqualTo(createObjectNodeWithUpValue("ID3"));
        assertThat(ingestSession.getArchiveUnitTree().get("ID15")).as("comparison failed for ID: ID15")
            .isEqualTo(createObjectNodeWithUpValue("ID3"));
        assertThat(ingestSession.getArchiveUnitTree().get("ID6")).as("comparison failed for ID: ID6")
            .isEqualTo(createObjectNodeWithUpValue("ID4"));
        assertThat(ingestSession.getArchiveUnitTree().get("ID8")).as("comparison failed for ID: ID8")
            .isEqualTo(createObjectNodeWithUpValue("ID6"));
        assertThat(ingestSession.getArchiveUnitTree().get("ID12")).as("comparison failed for ID: ID12")
            .isEqualTo(createObjectNodeWithUpValue("ID8", "ID19"));
        assertThat(ingestSession.getArchiveUnitTree().get("ID17")).as("comparison failed for ID: ID17")
            .isEqualTo(createObjectNodeWithUpValue("ID15"));
        assertThat(ingestSession.getArchiveUnitTree().get("ID19")).as("comparison failed for ID: ID19")
            .isEqualTo(createObjectNodeWithUpValue("ID17"));

        assertThat(response.getGlobalStatus()).isEqualTo(StatusCode.OK);
    }

    @Test
    @RunWithCustomExecutor
    public void given_manifest_with_simple_metadata() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        prepareIngestContractsAndManagementContracts(INGEST_CONTRACT_NO_CHECK);
        final InputStream sedaLocal = new FileInputStream(PropertiesUtils.findFile("SIP_with_metadata_simple.xml"));

        when(workspaceClient.getObject(any(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(sedaLocal).build());
        handlerIO.addOutIOParameters(out);
        final ItemStatus response = handler.execute(params, handlerIO, ingestSession);

        assertThat(response.getGlobalStatus()).isEqualTo(StatusCode.OK);
    }

    @Test
    @RunWithCustomExecutor
    public void given_manifest_with_signature_in_content() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        prepareIngestContractsAndManagementContracts(INGEST_CONTRACT_NO_CHECK);
        final InputStream sedaLocal =
            new FileInputStream(PropertiesUtils.findFile("SIP_with_signature_in_content.xml"));

        when(workspaceClient.getObject(any(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(sedaLocal).build());
        handlerIO.addOutIOParameters(out);
        final ItemStatus response = handler.execute(params, handlerIO, ingestSession);

        assertThat(response.getGlobalStatus()).isEqualTo(StatusCode.OK);
    }

    private ObjectNode createObjectNodeWithUpValue(String... ids) {
        ObjectNode objectNode = JsonHandler.createObjectNode();
        ArrayNode arrayNode = JsonHandler.createArrayNode();
        for (String id : ids) {
            arrayNode.add(id);
        }

        objectNode.set("_up", arrayNode);
        return objectNode;
    }

    // TODO : US 1686 test for add link between 2 existing units
    @Test
    @RunWithCustomExecutor
    public void givenManifestWithUpdateLinkExtractSedaThenReadSuccess() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        prepareIngestContractsAndManagementContracts(INGEST_CONTRACT_NO_CHECK);
        final InputStream sedaLocal = new FileInputStream(PropertiesUtils.findFile(SIP_ADD_LINK));
        JsonNode child = JsonHandler
            .getFromFile(PropertiesUtils.getResourceFile("extractSedaActionHandler/addLink/_Unit_CHILD.json"));
        JsonNode parent = JsonHandler
            .getFromFile(PropertiesUtils.getResourceFile("extractSedaActionHandler/addLink/_Unit_PARENT.json"));

        when(metadataClient.selectUnits(any())).thenReturn(child).thenReturn(parent);

        when(workspaceClient.getObject(any(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(sedaLocal).build());
        handlerIO.addOutIOParameters(out);

        final ItemStatus response = handler.execute(params, handlerIO, ingestSession);

        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void givenManifestWithUpdateAddLinkedUnitExtractSedaThenReadSuccess()
        throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        prepareIngestContractsAndManagementContracts(INGEST_CONTRACT_NO_CHECK);
        final InputStream sedaLocal = new FileInputStream(PropertiesUtils.findFile(SIP_ADD_UNIT));
        JsonNode parent = JsonHandler
            .getFromFile(PropertiesUtils.getResourceFile("extractSedaActionHandler/addLink/_Unit_PARENT.json"));

        when(metadataClient.selectUnits(any())).thenReturn(parent);

        when(workspaceClient.getObject(any(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(sedaLocal).build());
        handlerIO.addOutIOParameters(out);
        final ItemStatus response = handler.execute(params, handlerIO, ingestSession);

        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void givenManifestWithIngestContractContainsAUInThePlan() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final InputStream sedaLocal = new FileInputStream(PropertiesUtils.findFile(SIP_TEST));
        prepareIngestContractsAndManagementContracts(INGEST_CONTRACT_NO_CHECK);
        JsonNode parent = JsonHandler
            .getFromFile(PropertiesUtils.getResourceFile("extractSedaActionHandler/addLink/_Unit_PARENT.json"));
        when(metadataClient.selectUnits(any())).thenReturn(parent);

        when(workspaceClient.getObject(any(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(sedaLocal).build());
        handlerIO.addOutIOParameters(out);
        final ItemStatus response = handler.execute(params, handlerIO, ingestSession);

        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void givenManifestWithUpdateAddUnitExtractSedaThenCheckEvDetData()
        throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        prepareIngestContractsAndManagementContracts(INGEST_CONTRACT_NO_CHECK);

        final InputStream sedaLocal = new FileInputStream(PropertiesUtils.findFile(SIP_ADD_UNIT));
        JsonNode parent = JsonHandler
            .getFromFile(PropertiesUtils.getResourceFile("extractSedaActionHandler/addLink/_Unit_PARENT.json"));

        when(metadataClient.selectUnits(any())).thenReturn(parent).thenReturn(parent);

        when(workspaceClient.getObject(any(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(sedaLocal).build());
        handlerIO.addOutIOParameters(out);
        final ItemStatus response = handler.execute(params, handlerIO, ingestSession);

        // Check master evDetData
        String evDetDataString = response.getEvDetailData();
        JsonNode evDetData = JsonHandler.getFromString(evDetDataString);
        assertNotNull(evDetData);
        String masterEvDetDataString =
            (String) response.getMasterData().get(LogbookParameterName.eventDetailData.name());
        JsonNode masterEvDetData = JsonHandler.getFromString(masterEvDetDataString);
        assertNotNull(masterEvDetData);

        // Check comment
        assertEquals("Commentaire français", evDetData.get("EvDetailReq").asText());
        assertEquals("Commentaire français", evDetData.get("EvDetailReq_fr").asText());
        assertEquals("English Comment", evDetData.get("EvDetailReq_en").asText());
        assertEquals("Deutsch Kommentare", evDetData.get("EvDetailReq_de").asText());

        // Check other fields
        assertEquals("ArchivalAgreement0", evDetData.get("ArchivalAgreement").asText());
        assertNotNull(response.getData("agIdExt"));
        JsonNode node = JsonHandler.getFromString(response.getData("agIdExt").toString());
        assertEquals("Identifier1", node.get("TransferringAgency").asText());
        assertEquals("2016-06-23T09:45:51.0", evDetData.get("EvDateTimeReq").asText());

        //check ManagementMetaData
        assertEquals("AcquisitionInformation0", masterEvDetData.get("AcquisitionInformation").asText());
        assertEquals("Public and Private Archive", masterEvDetData.get("LegalStatus").asText());
    }

    @Test
    @RunWithCustomExecutor
    public void givenManifestWithUpdateAddLinkedUnitExtractSedaThenReadKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        prepareIngestContractsAndManagementContracts(INGEST_CONTRACT_NO_CHECK);

        final InputStream sedaLocal = new FileInputStream(PropertiesUtils.findFile(SIP_ADD_UNIT));
        when(workspaceClient.getObject(any(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(sedaLocal).build());
        handlerIO.addOutIOParameters(out);
        final ItemStatus response = handler.execute(params, handlerIO, ingestSession);

        assertEquals(StatusCode.KO, response.getGlobalStatus());
        assertEquals("NOT_FOUND_ATTACHMENT", response.getGlobalOutcomeDetailSubcode());
    }

    @Test
    @RunWithCustomExecutor
    public void should_not_call_lifecycle_when_guid_is_invalid() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        prepareIngestContractsAndManagementContracts(INGEST_CONTRACT_NO_CHECK);
        final InputStream sedaLocal =
            new FileInputStream(PropertiesUtils.findFile("extractSedaActionHandler/SIP_Add_GOT.xml"));
        when(workspaceClient.getObject(any(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(sedaLocal).build());

        handlerIO.addOutIOParameters(out);

        // When
        final ItemStatus response = handler.execute(params, handlerIO, ingestSession);

        // Then
        assertThat(response.getGlobalStatus()).isEqualTo(StatusCode.KO);
        assertThat(response.getGlobalOutcomeDetailSubcode()).isEqualTo("NOT_FOUND_ATTACHMENT");
        verify(logbookLifeCyclesClient, never()).create(any());
    }

    @Test
    @RunWithCustomExecutor
    public void givenSipWithAURefToBDOThenExtractKO()
        throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        prepareIngestContractsAndManagementContracts(INGEST_CONTRACT_NO_CHECK);

        final InputStream sedaLocal = new FileInputStream(PropertiesUtils.findFile("sip-ko-bdo-ref-group.xml"));
        when(workspaceClient.getObject(any(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(sedaLocal).build());
        handlerIO.addOutIOParameters(out);
        final ItemStatus response = handler.execute(params, handlerIO, ingestSession);
        assertEquals(StatusCode.KO, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void givenManifestPhysicalDataObjectExtractSedaThenReadSuccess() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertNotNull(ExtractSedaActionHandler.getId());
        prepareIngestContractsAndManagementContracts(INGEST_CONTRACT_MASTER_MANDATORY_FALSE);
        final InputStream seda_arborescence =
            PropertiesUtils.getResourceAsStream(SIP_PHYSICAL_DATA_OBJECT);
        when(workspaceClient.getObject(any(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(seda_arborescence).build());
        handlerIO.addOutIOParameters(out);

        final ItemStatus response = handler.execute(params, handlerIO, ingestSession);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void givenManifestWithGroupAndPhysicalDataObjectExtractSedaThenReadSuccess() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertNotNull(ExtractSedaActionHandler.getId());
        prepareIngestContractsAndManagementContracts(INGEST_CONTRACT_MASTER_MANDATORY_FALSE);
        final InputStream seda_arborescence =
            PropertiesUtils.getResourceAsStream(SIP_GROUP_PHYSICAL_DATA_OBJECT);
        when(workspaceClient.getObject(any(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(seda_arborescence).build());
        handlerIO.addOutIOParameters(out);

        final ItemStatus response = handler.execute(params, handlerIO, ingestSession);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void givenManifestInheritanceExtractSedaThenReadSuccess() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertNotNull(ExtractSedaActionHandler.getId());

        prepareIngestContractsAndManagementContracts(INGEST_CONTRACT_MASTER_MANDATORY_FALSE);
        final InputStream seda_arborescence =
            PropertiesUtils.getResourceAsStream(SIP_RULES_INHERITANCE);
        JsonNode parent = JsonHandler
            .getFromFile(PropertiesUtils.getResourceFile("extractSedaActionHandler/addLink/_Unit_PARENT.json"));
        when(metadataClient.selectUnits(any())).thenReturn(parent);
        when(workspaceClient.getObject(any(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(seda_arborescence).build());
        handlerIO.addOutIOParameters(out);

        final ItemStatus response = handler.execute(params, handlerIO, ingestSession);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void should_test_extract_seda_with_multi_value_field() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertNotNull(ExtractSedaActionHandler.getId());
        prepareIngestContractsAndManagementContracts(INGEST_CONTRACT_MASTER_MANDATORY_FALSE);
        final InputStream sedaLocal =
            PropertiesUtils.getResourceAsStream("seda_multivalue.xml");
        when(workspaceClient.getObject(any(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(sedaLocal).build());
        handlerIO.addOutIOParameters(out);

        final ItemStatus response = handler.execute(params, handlerIO, ingestSession);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

    private void prepareIngestContractsAndManagementContracts(String ingestContractName)
        throws Exception {
        final InputStream ingestContractInputStream =
            PropertiesUtils.getResourceAsStream(ingestContractName);
        in.set(3, new IOParameter()
            .setUri(new ProcessingUri(UriPrefix.WORKSPACE, ingestContractName)));
        handlerIO.reset();
        handlerIO.addInIOParameters(in);
        when(workspaceClient.getObject(any(), eq(ingestContractName)))
            .thenReturn(Response.status(Status.OK).entity(ingestContractInputStream).build());
    }

    @Test
    @RunWithCustomExecutor
    public void should_check_ingestContract_for_objectGroup_when_master_is_not_present_then_ok() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertNotNull(ExtractSedaActionHandler.getId());
        final InputStream sedaLocal =
            PropertiesUtils.getResourceAsStream(MANIFEST_WITHOUT_BINARY_OR_PHYSICAL);
        prepareIngestContractsAndManagementContracts(INGEST_CONTRACT_MASTER_MANDATORY_FALSE);
        when(workspaceClient.getObject(any(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(sedaLocal).build());
        handlerIO.addOutIOParameters(out);
        // Then
        final ItemStatus response = handler.execute(params, handlerIO, ingestSession);
        assertEquals(StatusCode.OK, response.getGlobalStatus());

    }


    @Test
    @RunWithCustomExecutor
    public void should_check_ingestContract_for_objectGroup_when_master_is_not_present_then_ko() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertNotNull(ExtractSedaActionHandler.getId());
        final InputStream sedaLocal =
            PropertiesUtils.getResourceAsStream(MANIFEST_WITHOUT_BINARY_OR_PHYSICAL);
        prepareIngestContractsAndManagementContracts(INGEST_CONTRACT_MASTER_MANDATORY_TRUE);
        // When
        when(workspaceClient.getObject(any(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(sedaLocal).build());
        handlerIO.addOutIOParameters(out);
        // Then
        final ItemStatus response = handler.execute(params, handlerIO, ingestSession);
        assertEquals(StatusCode.KO, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void should_check_ingestContract_for_objectGroup_when_attachment_objectGroup_to_an_existing_one_then_ko()
        throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertNotNull(ExtractSedaActionHandler.getId());
        final InputStream sedaLocal =
            PropertiesUtils.getResourceAsStream(MANIFEST_WITH_BINARYMASTER);
        prepareIngestContractsAndManagementContracts(INGEST_CONTRACT_EVERYDATAOBJECTVERSION_FALSE);
        JsonNode objectGroupLinkedToExistingOne = JsonHandler
            .getFromFile(PropertiesUtils.getResourceFile(UNIT_ATTACHED_DB_RESPONSE));
        // When
        when(metadataClient.selectUnits(any()))
            .thenReturn(objectGroupLinkedToExistingOne);
        when(workspaceClient.getObject(any(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(sedaLocal).build());
        handlerIO.addOutIOParameters(out);
        // Then
        final ItemStatus response = handler.execute(params, handlerIO, ingestSession);
        assertEquals(StatusCode.KO, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void should_check_ingestContract_dataObjectVersion_when_binary_is_attached_to_existing_objectGroup_then_ko()
        throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertNotNull(ExtractSedaActionHandler.getId());
        final InputStream sedaLocal =
            PropertiesUtils.getResourceAsStream(MANIFEST_WITH_BINARYMASTER);
        prepareIngestContractsAndManagementContracts(INGEST_CONTRACT_EVERYDATAOBJECTVERSION_FALSE);
        JsonNode objectGroupLinkedToExistingOne = JsonHandler
            .getFromFile(PropertiesUtils.getResourceFile(UNIT_ATTACHED_DB_RESPONSE));
        // When
        when(metadataClient.selectUnits(any()))
            .thenReturn(objectGroupLinkedToExistingOne);
        when(workspaceClient.getObject(any(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(sedaLocal).build());
        handlerIO.addOutIOParameters(out);
        // Then
        final ItemStatus response = handler.execute(params, handlerIO, ingestSession);
        assertEquals(StatusCode.KO, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void should_check_ingestContract_when_multi_Au_in_Sip_updated_one_and_created_one_with_master_then_ok()
        throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertNotNull(ExtractSedaActionHandler.getId());
        final InputStream sedaLocal =
            PropertiesUtils.getResourceAsStream(MANIFEST_WITH_ATTACHMENT_AND_USAGES_WITH_MASTER);
        prepareIngestContractsAndManagementContracts(INGEST_CONTRACT_MASTER_MANDATORY_TRUE);
        JsonNode objectGroupLinkedToExistingOne = JsonHandler
            .getFromFile(PropertiesUtils.getResourceFile(UNIT_ATTACHED_DB_RESPONSE));
        // When
        JsonNode objectGroupStream = JsonHandler
            .getFromFile(PropertiesUtils.getResourceFile("checkMasterMandatoryInOGAndAttachmentInOG/og.json"));

        when(metadataClient.selectObjectGroups(any())).thenReturn(
            getFromInputStream(getClass().getResourceAsStream("/checkMasterMandatoryInOGAndAttachmentInOG/og.json")));
        when(metadataClient.selectUnits(any()))
            .thenReturn(objectGroupLinkedToExistingOne);
        when(workspaceClient.getObject(any(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(sedaLocal).build());
        handlerIO.addOutIOParameters(out);
        // Then
        final ItemStatus response = handler.execute(params, handlerIO, ingestSession);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void should_check_originating_agency_when_attachement_to_existant_with_different_sp_then_ko()
        throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertNotNull(ExtractSedaActionHandler.getId());
        final InputStream sedaLocal =
            PropertiesUtils.getResourceAsStream(MANIFEST_WITH_ATTACHMENT_TO_EXISTANT_WITH_DIFFERENT_SP);
        prepareIngestContractsAndManagementContracts(INGEST_CONTRACT_MASTER_MANDATORY_TRUE);
        JsonNode objectGroupLinkedToExistingOne = JsonHandler
            .getFromFile(PropertiesUtils.getResourceFile(UNIT_ATTACHED_SP_DB_RESPONSE));
        // When
        when(metadataClient.selectObjectGroups(any())).thenReturn(
            getFromInputStream(
                getClass().getResourceAsStream("/checkMasterMandatoryInOGAndAttachmentInOG/og_results.json")));
        when(metadataClient.selectUnits(any()))
            .thenReturn(objectGroupLinkedToExistingOne);
        when(workspaceClient.getObject(any(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(sedaLocal).build());
        handlerIO.addOutIOParameters(out);
        // Then
        final ItemStatus response = handler.execute(params, handlerIO, ingestSession);
        assertEquals(StatusCode.KO, response.getGlobalStatus());
        JsonNode evDetData = JsonHandler.getFromString((String) response.getData("eventDetailData"));
        assertEquals(
            "Not allowed object attachement of originating agency (SomeOriginatingAgency) to other originating agency",
            evDetData.get("evDetTechData").asText());
    }

    @Test
    @RunWithCustomExecutor
    public void should_check_ingestContract_when_multi_Au_in_Sip_updated_one_and_created_one_without_master_then_ko()
        throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertNotNull(ExtractSedaActionHandler.getId());
        final InputStream sedaLocal =
            PropertiesUtils.getResourceAsStream(MANIFEST_WITH_ATTACHMENT_AND_USAGES_WITHOUT_MASTER);
        prepareIngestContractsAndManagementContracts(INGEST_CONTRACT_MASTER_MANDATORY_TRUE);
        JsonNode objectGroupLinkedToExistingOne = JsonHandler
            .getFromFile(PropertiesUtils.getResourceFile(UNIT_ATTACHED_DB_RESPONSE));
        // When
        when(metadataClient.selectUnits(any()))
            .thenReturn(objectGroupLinkedToExistingOne);
        when(workspaceClient.getObject(any(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(sedaLocal).build());
        handlerIO.addOutIOParameters(out);
        // Then
        final ItemStatus response = handler.execute(params, handlerIO, ingestSession);
        assertEquals(StatusCode.KO, response.getGlobalStatus());
    }



    @Test
    @RunWithCustomExecutor
    public void should_check_ingestContract_in_case_of_attachment_when_exist_then_ko() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertNotNull(ExtractSedaActionHandler.getId());
        final InputStream sedaLocal =
            PropertiesUtils.getResourceAsStream(MANIFEST_WITH_BINARYMASTER);
        prepareIngestContractsAndManagementContracts(INGEST_CONTRACT_MASTER_MANDATORY_FALSE);
        JsonNode objectGroupLinkedToExistingOne = JsonHandler
            .getFromFile(PropertiesUtils.getResourceFile(UNIT_ATTACHED_DB_RESPONSE));
        // When
        when(metadataClient.selectUnits(any()))
            .thenReturn(objectGroupLinkedToExistingOne);
        when(workspaceClient.getObject(any(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(sedaLocal).build());
        handlerIO.addOutIOParameters(out);
        // Then
        final ItemStatus response = handler.execute(params, handlerIO, ingestSession);
        assertEquals(StatusCode.KO, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void should_check_ingestContract_in_case_of_attachment_when_exist_then_ok() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertNotNull(ExtractSedaActionHandler.getId());
        final InputStream sedaLocal =
            PropertiesUtils.getResourceAsStream(MANIFEST_WITH_BINARYMASTER);
        prepareIngestContractsAndManagementContracts(
            INGEST_CONTRACT_EVERYDATAOBJECTVERSION_TRUE);
        when(metadataClient.selectObjectGroups(any())).thenReturn(
            getFromInputStream(getClass().getResourceAsStream("/checkMasterMandatoryInOGAndAttachmentInOG/og.json")));
        JsonNode objectGroupLinkedToExistingOne = JsonHandler
            .getFromFile(PropertiesUtils.getResourceFile(UNIT_ATTACHED_DB_RESPONSE));
        // When
        when(metadataClient.selectUnits(any()))
            .thenReturn(objectGroupLinkedToExistingOne);
        when(workspaceClient.getObject(any(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(sedaLocal).build());
        handlerIO.addOutIOParameters(out);
        // Then
        final ItemStatus response = handler.execute(params, handlerIO, ingestSession);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void should_test_extract_seda_with_keyword_type() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertNotNull(ExtractSedaActionHandler.getId());

        prepareIngestContractsAndManagementContracts(INGEST_CONTRACT_MASTER_MANDATORY_FALSE);
        final InputStream sedaLocal =
            PropertiesUtils.getResourceAsStream("sip_with_keyword_type.xml");
        when(workspaceClient.getObject(any(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(sedaLocal).build());
        handlerIO.addOutIOParameters(out);

        final ItemStatus response = handler.execute(params, handlerIO, ingestSession);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void givenManifestRefIdInheritanceExtractSedaThenReadSuccess() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertNotNull(ExtractSedaActionHandler.getId());
        prepareIngestContractsAndManagementContracts(INGEST_CONTRACT_MASTER_MANDATORY_FALSE);
        final InputStream seda_arborescence =
            PropertiesUtils.getResourceAsStream(SIP_REFID_RULES_INHERITANCE);
        JsonNode parent = JsonHandler
            .getFromFile(PropertiesUtils.getResourceFile("extractSedaActionHandler/addLink/_Unit_PARENT.json"));
        when(metadataClient.selectUnits(any())).thenReturn(parent);
        when(workspaceClient.getObject(any(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(seda_arborescence).build());
        handlerIO.addOutIOParameters(out);

        final ItemStatus response = handler.execute(params, handlerIO, ingestSession);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void givenManifestRefnonruleidPreventinheritanceExtractSedaThenReadSuccess() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertNotNull(ExtractSedaActionHandler.getId());

        prepareIngestContractsAndManagementContracts(INGEST_CONTRACT_MASTER_MANDATORY_FALSE);
        final InputStream seda_arborescence =
            PropertiesUtils.getResourceAsStream(SIP_REFNONRULEID_PREVENTINHERITANCE);
        JsonNode parent = JsonHandler
            .getFromFile(PropertiesUtils.getResourceFile("extractSedaActionHandler/addLink/_Unit_PARENT.json"));
        when(metadataClient.selectUnits(any())).thenReturn(parent);
        when(workspaceClient.getObject(any(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(seda_arborescence).build());
        handlerIO.addOutIOParameters(out);

        final ItemStatus response = handler.execute(params, handlerIO, ingestSession);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void givenManifestRefnonruleidMultiplePreventinheritanceExtractSedaThenReadSuccess() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertNotNull(ExtractSedaActionHandler.getId());

        prepareIngestContractsAndManagementContracts(INGEST_CONTRACT_MASTER_MANDATORY_FALSE);
        final InputStream seda_arborescence =
            PropertiesUtils.getResourceAsStream(SIP_REFNONRULEID_MULT_PREVENTINHERITANCE);
        JsonNode parent = JsonHandler
            .getFromFile(PropertiesUtils.getResourceFile("extractSedaActionHandler/addLink/_Unit_PARENT.json"));
        // When
        when(metadataClient.selectUnits(any())).thenReturn(parent);
        when(workspaceClient.getObject(any(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(seda_arborescence).build());
        handlerIO.addOutIOParameters(out);

        final ItemStatus response = handler.execute(params, handlerIO, ingestSession);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void givenManifestComplexeRulesGlobalManagementExtractSedaThenReadSuccess() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertNotNull(ExtractSedaActionHandler.getId());

        prepareIngestContractsAndManagementContracts(INGEST_CONTRACT_NO_CHECK);
        final InputStream seda_arborescence =
            PropertiesUtils.getResourceAsStream(SIP_RULES_COMPLEXE);
        JsonNode parent = JsonHandler
            .getFromFile(PropertiesUtils.getResourceFile("extractSedaActionHandler/addLink/_Unit_PARENT.json"));
        when(metadataClient.selectUnits(any())).thenReturn(parent);
        when(workspaceClient.getObject(any(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(seda_arborescence).build());
        handlerIO.addOutIOParameters(out);

        final ItemStatus response = handler.execute(params, handlerIO, ingestSession);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }


    @Test
    @RunWithCustomExecutor
    public void givenManifestWithSpecialCharactersWhenExecuteThenReturnResponseOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertNotNull(ExtractSedaActionHandler.getId());
        prepareIngestContractsAndManagementContracts(INGEST_CONTRACT_MASTER_MANDATORY_FALSE);
        final InputStream seda_arborescence =
            PropertiesUtils.getResourceAsStream(SIP_WITH_SPECIAL_CHARACTERS);
        when(workspaceClient.getObject(any(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(seda_arborescence).build());
        handlerIO.addOutIOParameters(out);

        final ItemStatus response = handler.execute(params, handlerIO, ingestSession);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void givenManifestWithMultiCommentWhenExecuteThenReturnResponseOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertNotNull(ExtractSedaActionHandler.getId());
        prepareIngestContractsAndManagementContracts(INGEST_CONTRACT_MASTER_MANDATORY_FALSE);
        final InputStream seda_arborescence =
            PropertiesUtils.getResourceAsStream(OK_MULTI_COMMENT);
        when(workspaceClient.getObject(any(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(seda_arborescence).build());
        handlerIO.addOutIOParameters(out);

        final ItemStatus response = handler.execute(params, handlerIO, ingestSession);
        assertEquals(StatusCode.OK, response.getGlobalStatus());

        JsonNode evDetData = JsonHandler.getFromString((String) response.getData("eventDetailData"));
        assertEquals(
            "Ceci est le premier commentaire_Voici le deuxième commentaire_Exemple de 3ème commentaire",
            evDetData.get("EvDetailReq").asText());
    }

    @Test
    @RunWithCustomExecutor
    public void givenManifestWithSignatureWhenExecuteThenReturnResponseOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertNotNull(ExtractSedaActionHandler.getId());
        prepareIngestContractsAndManagementContracts(INGEST_CONTRACT_MASTER_MANDATORY_FALSE);
        final InputStream seda_arborescence =
            PropertiesUtils.getResourceAsStream(OK_SIGNATURE);
        when(workspaceClient.getObject(any(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(seda_arborescence).build());
        handlerIO.addOutIOParameters(out);

        final ItemStatus response = handler.execute(params, handlerIO, ingestSession);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void givenManifestWithRulesWithoutIdWhenExecuteThenReturnResponseOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertNotNull(ExtractSedaActionHandler.getId());
        prepareIngestContractsAndManagementContracts(INGEST_CONTRACT_MASTER_MANDATORY_FALSE);
        final InputStream seda_arborescence =
            PropertiesUtils.getResourceAsStream(OK_RULES_WOUT_ID);
        when(workspaceClient.getObject(any(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(seda_arborescence).build());
        handlerIO.addOutIOParameters(out);

        final ItemStatus response = handler.execute(params, handlerIO, ingestSession);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }


    @Test
    @RunWithCustomExecutor
    public void givenManifestWithCycleWhenExecuteThenReturnResponseKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertNotNull(ExtractSedaActionHandler.getId());
        prepareIngestContractsAndManagementContracts(INGEST_CONTRACT_MASTER_MANDATORY_FALSE);
        final InputStream seda_arborescence =
            PropertiesUtils.getResourceAsStream(KO_CYCLE);
        when(workspaceClient.getObject(any(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(seda_arborescence).build());
        handlerIO.addOutIOParameters(out);

        final ItemStatus response = handler.execute(params, handlerIO, ingestSession);
        assertEquals(StatusCode.KO, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void givenManifestWithAUDeclaringObjWhenExecuteThenReturnResponseKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        prepareIngestContractsAndManagementContracts(INGEST_CONTRACT_NO_CHECK);

        assertNotNull(ExtractSedaActionHandler.getId());
        final InputStream seda_arborescence =
            PropertiesUtils.getResourceAsStream(KO_AU_REF_OBJ);
        when(workspaceClient.getObject(any(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(seda_arborescence).build());
        handlerIO.addOutIOParameters(out);

        final ItemStatus response = handler.execute(params, handlerIO, ingestSession);
        assertEquals(StatusCode.KO, response.getGlobalStatus());
        JsonNode evDetData = JsonHandler.getFromString((String) response.getData("eventDetailData"));
        assertNotNull(evDetData);
        assertNotNull(evDetData.get("evDetTechData"));
        assertTrue(evDetData.get("evDetTechData").asText().contains("ArchiveUnit"));
        assertTrue(evDetData.get("evDetTechData").asText().contains("BinaryDataObjectID"));
        assertTrue(evDetData.get("evDetTechData").asText().contains("DataObjectGroupId"));
        assertTrue(evDetData.get("evDetTechData").asText().contains("ID22"));
    }

    @Test
    @RunWithCustomExecutor
    public void givenContractMCWhenExecuteThenReturnResponseOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertNotNull(ExtractSedaActionHandler.getId());
        final InputStream storageInfo = PropertiesUtils.getResourceAsStream(STORAGE_INFO_MC_JSON);
        when(workspaceClient.getObject(any(), eq("StorageInfo/storageInfo.json")))
            .thenReturn(Response.status(Status.OK).entity(storageInfo).build());

        prepareIngestContractsAndManagementContracts(CONTRACTS_MC_JSON);
        final InputStream seda_arborescence = PropertiesUtils.getResourceAsStream(SIP_ARBORESCENCE);
        when(workspaceClient.getObject(any(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(seda_arborescence).build());

        String objectId = "SIP/manifest.xml";
        HandlerIOImpl handlerIO =
            new HandlerIOImpl(workspaceClientFactory, logbookLifeCyclesClientFactory, "ExtractSedaActionHandlerTest",
                "workerId",
                Lists.newArrayList(objectId));
        handlerIO.setCurrentObjectId(objectId);

        handlerIO.addInIOParameters(in);
        handlerIO.addOutIOParameters(out);

        saveWorkspacePutObject();

        final ItemStatus response = handler.execute(params, handlerIO, ingestSession);
        handlerIO.close();

        ArgumentCaptor<String> fileNameArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(workspaceClient).putObject(anyString(), ArgumentMatchers.startsWith("Units/"), any(InputStream.class));
        verify(workspaceClient).putObject(anyString(), ArgumentMatchers.startsWith("ObjectGroup/"),
            any(InputStream.class));
        verify(workspaceClient, atLeastOnce())
            .putObject(anyString(), fileNameArgumentCaptor.capture(), any(InputStream.class));

        JsonNode unitJson = getSavedWorkspaceObject(fileNameArgumentCaptor.getAllValues().stream()
            .filter(str -> str.startsWith("Units/"))
            .findFirst().get());
        assertNotNull(unitJson);
        assertEquals("default",
            unitJson.get(ARCHIVE_UNIT).get(SedaConstants.STORAGE).get(SedaConstants.STRATEGY_ID).asText());

        JsonNode gotJson = getSavedWorkspaceObject(fileNameArgumentCaptor.getAllValues().stream()
            .filter(str -> str.startsWith("ObjectGroup/"))
            .findFirst().get());
        assertNotNull(gotJson);
        assertEquals("default", gotJson.get(SedaConstants.STORAGE).get(SedaConstants.STRATEGY_ID).asText());
        Assert.assertFalse(gotJson.get(SedaConstants.STORAGE).has(SedaConstants.OFFER_IDS));
        Assert.assertFalse(gotJson.get(SedaConstants.PREFIX_WORK).get(SedaConstants.PREFIX_QUALIFIERS)
            .get("PhysicalMaster_1").get(SedaConstants.TAG_VERSIONS).get(0).has(SedaConstants.STORAGE));
        assertEquals("offerId",
            gotJson.get(SedaConstants.PREFIX_WORK).get(SedaConstants.PREFIX_QUALIFIERS).get("BinaryMaster_1")
                .get(SedaConstants.TAG_VERSIONS).get(0).get(SedaConstants.STORAGE)
                .get(SedaConstants.STRATEGY_ID).asText());
        Assert.assertFalse(
            gotJson.get(SedaConstants.PREFIX_WORK).get(SedaConstants.PREFIX_QUALIFIERS).get("BinaryMaster_1")
                .get(SedaConstants.TAG_VERSIONS).get(0).get(SedaConstants.STORAGE)
                .has(SedaConstants.OFFER_IDS));

        assertEquals(StatusCode.OK, response.getGlobalStatus());
        JsonNode evDetData = JsonHandler.getFromString((String) response.getData("eventDetailData"));
        assertNotNull(evDetData);
    }

    @Test
    @RunWithCustomExecutor
    public void givenSipWithIngestContractHavingComputeInheritedRulesThenUnitsContainsValidComputedInheritedRules()
        throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        prepareIngestContractsAndManagementContracts(
            INGEST_CONTRACT_JSON_COMPUTEINHERITEDRULESATINGEST);
        final InputStream sedaLocal = new FileInputStream(PropertiesUtils.findFile(SIP_ARBORESCENCE));
        when(workspaceClient.getObject(any(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(sedaLocal).build());
        final InputStream ingestContract = PropertiesUtils.getResourceAsStream(CONTRACTS_JSON);
        when(workspaceClient.getObject(any(), eq("referential/contracts.json")))
            .thenReturn(Response.status(Status.OK).entity(ingestContract).build());
        handlerIO.addOutIOParameters(out);

        saveWorkspacePutObject();

        final ItemStatus response = handler.execute(params, handlerIO, ingestSession);
        handlerIO.close();
        ArgumentCaptor<String> fileNameArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(workspaceClient).putObject(anyString(), ArgumentMatchers.startsWith("Units/"), any(InputStream.class));
        verify(workspaceClient, atLeastOnce())
            .putObject(anyString(), fileNameArgumentCaptor.capture(), any(InputStream.class));

        JsonNode unitJson = getSavedWorkspaceObject(fileNameArgumentCaptor.getAllValues().stream()
            .filter(str -> str.startsWith("Units/"))
            .findFirst().get());

        assertNotNull(unitJson);
        Assert.assertFalse(unitJson.get(ARCHIVE_UNIT).get(Unit.VALID_COMPUTED_INHERITED_RULES).booleanValue());
        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void givenManifestWithIncorrectSizeThenOk() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertNotNull(ExtractSedaActionHandler.getId());
        prepareIngestContractsAndManagementContracts(INGEST_CONTRACT_MASTER_MANDATORY_FALSE);
        final InputStream sedaArborescenceCheckObjectSize =
            PropertiesUtils.getResourceAsStream(INCORRECT_SIZE_TYPE);
        when(workspaceClient.getFilesWithParamsFromFolder(any(), any()))
            .thenReturn(new RequestResponseOK().addResult(new HashMap<>()));
        when(workspaceClient.getObject(any(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(sedaArborescenceCheckObjectSize).build());
        handlerIO.addOutIOParameters(out);

        // When
        final ItemStatus response = handler.execute(params, handlerIO, ingestSession);

        // Then
        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void givenManifestWithEmptySizeThenOK() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertNotNull(ExtractSedaActionHandler.getId());
        prepareIngestContractsAndManagementContracts(INGEST_CONTRACT_MASTER_MANDATORY_FALSE);
        final InputStream sedaArborescenceCheckObjectSize =
            PropertiesUtils.getResourceAsStream(EMPTY_SIZE_TYPE);
        when(workspaceClient.getFilesWithParamsFromFolder(any(), any()))
            .thenReturn(new RequestResponseOK().addResult(new HashMap<>()));
        when(workspaceClient.getObject(any(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(sedaArborescenceCheckObjectSize).build());
        handlerIO.addOutIOParameters(out);

        // When
        final ItemStatus response = handler.execute(params, handlerIO, ingestSession);

        // Then
        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

    private Map<String, FileParams> getFilesWithParams() {
        Map<String, FileParams> fileParamsMap = new HashMap<>();
        fileParamsMap.put("Content/Lake1.jpeg", new FileParams(38628L));
        fileParamsMap.put("Content/Lake2.jpeg", new FileParams(38628L));
        return fileParamsMap;
    }

    private void saveWorkspacePutObject() throws ContentAddressableStorageServerException {
        doAnswer(invocation -> {
            String filename = invocation.getArgument(1);
            InputStream inputStream = invocation.getArgument(2);
            Path filePath = Paths.get(folder.getRoot().getAbsolutePath(), filename);
            Files.createDirectories(filePath.getParent());
            Files.copy(inputStream, filePath);
            return null;
        }).when(workspaceClient).putObject(anyString(), anyString(), any(InputStream.class));
    }

    private JsonNode getSavedWorkspaceObject(String filename) throws InvalidParseOperationException {
        Path filePath = Paths.get(folder.getRoot().getAbsolutePath(), filename);
        return JsonHandler.getFromFile(filePath.toFile());
    }

    private static class ExtractSedaHandlerIO extends HandlerIOImpl {

        @VisibleForTesting
        public ExtractSedaHandlerIO(WorkspaceClientFactory workspaceClientFactory,
            LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory) {
            super(workspaceClientFactory, logbookLifeCyclesClientFactory, "ExtractSedaActionHandlerTest",
                "workerId",
                Lists.newArrayList(OBJECT_ID));
        }
    }
}

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
package fr.gouv.vitam.worker.core.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.*;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.AssertTrue;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.stream.XMLStreamException;

import fr.gouv.vitam.common.SystemPropertyUtil;
import org.junit.After;
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
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.model.IOParameter;
import fr.gouv.vitam.processing.common.model.ProcessingUri;
import fr.gouv.vitam.processing.common.model.UriPrefix;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.core.impl.HandlerIOImpl;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.net.ssl.*")
@PrepareForTest({WorkspaceClientFactory.class, MetaDataClientFactory.class})
public class ExtractSedaActionHandlerTest {
    ExtractSedaActionHandler handler = new ExtractSedaActionHandler();
    private static final String HANDLER_ID = "CHECK_MANIFEST";
    private static final String SIP_TEST = "extractSedaActionHandler/rules-test.xml";
    private static final String SIP_ADD_LINK = "extractSedaActionHandler/addLink/SIP_Add_Link.xml";
    private static final String SIP_ADD_UNIT = "extractSedaActionHandler/addUnit/SIP_Add_Unit.xml";
    private static final String OK_EHESS_LIGHT = "extractSedaActionHandler/OK_EHESS_LIGHT.xml";
    private static final String MERCIER = "extractSedaActionHandler/Mercier.xml";
    private static final String SIP_RULES = "extractSedaActionHandler/rules-test.xml";
    private static final String SIP_ARBO_RULES_MD = "extractSedaActionHandler/OK_arbo_RG_MD_complexe.xml";
    private static final String SIP_WITHOUT_ORIGINATING_AGENCY =
        "extractSedaActionHandler/manifestKO/originating_agency_not_set.xml";
    private static final String SIP_RULES_INHERITENCE = "extractSedaActionHandler/1066_SIP_RULES_INHERITENCE.xml";
    private static final String SIP_REFID_RULES_INHERITENCE =
        "extractSedaActionHandler/1069_SIP_REFID_RULES_INHERITENCE.xml";
    private static final String SIP_REFNONRULEID_PREVENTINHERITENCE =
        "extractSedaActionHandler/refnonruleid_and_preventinheritence.xml";
    private static final String SIP_REFNONRULEID_MULT_PREVENTINHERITENCE =
        "extractSedaActionHandler/refnonruleid_multiple_and_preventinheritence.xml";
    private static final String SIP_PHYSICAL_DATA_OBJECT = "extractSedaActionHandler/SIP_PHYSICAL_DATA_OBJECT.xml";
    private static final String SIP_WITH_SPECIAL_CHARACTERS =
        "extractSedaActionHandler/SIP_WITH_SPECIAL_CHARACTERS.xml";
    private static final String SIP_ARBORESCENCE = "SIP_Arborescence.xml";
    private static final String OK_MULTI_COMMENT = "extractSedaActionHandler/OK_multi_comment.xml";
    private WorkspaceClient workspaceClient;
    private MetaDataClient metadataClient;
    private WorkspaceClientFactory workspaceClientFactory;
    private MetaDataClientFactory metadataClientFactory;
    private HandlerIOImpl action;
    private List<IOParameter> out;
    private List<IOParameter> in;
    private static final Integer TENANT_ID = 0;
    private final WorkerParameters params =
        WorkerParametersFactory.newWorkerParameters().setUrlWorkspace("http://localhost:8083")
            .setUrlMetadata("http://localhost:8083")
            .setObjectName("objectName.json").setCurrentStep("currentStep")
            .setLogbookTypeProcess(LogbookTypeProcess.INGEST)
            .setContainerName("ExtractSedaActionHandlerTest");

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    public ExtractSedaActionHandlerTest() throws FileNotFoundException {}

    @Before
    public void setUp() throws URISyntaxException, IOException {

        File tempFolder = folder.newFolder();
        System.setProperty("vitam.tmp.folder", tempFolder.getAbsolutePath());

        SystemPropertyUtil.refresh();

        PowerMockito.mockStatic(WorkspaceClientFactory.class);
        workspaceClient = mock(WorkspaceClient.class);
        workspaceClientFactory = mock(WorkspaceClientFactory.class);
        PowerMockito.when(WorkspaceClientFactory.getInstance()).thenReturn(workspaceClientFactory);
        PowerMockito.when(WorkspaceClientFactory.getInstance().getClient()).thenReturn(workspaceClient);

        PowerMockito.mockStatic(MetaDataClientFactory.class);
        metadataClient = mock(MetaDataClient.class);
        metadataClientFactory = mock(MetaDataClientFactory.class);
        PowerMockito.when(MetaDataClientFactory.getInstance()).thenReturn(metadataClientFactory);
        PowerMockito.when(MetaDataClientFactory.getInstance().getClient()).thenReturn(metadataClient);


        action = new HandlerIOImpl("ExtractSedaActionHandlerTest", "workerId");
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
        in = new ArrayList<>();
        in.add(new IOParameter()
                .setUri(new ProcessingUri(UriPrefix.VALUE, "false")));
        in.add(new IOParameter()
                .setUri(new ProcessingUri(UriPrefix.VALUE, "INGEST")));
    }

    @After
    public void clean() {
        action.partialClose();
    }

    @Test
    @RunWithCustomExecutor
    public void givenWorkspaceNotExistWhenExecuteThenReturnResponseFATAL()
        throws XMLStreamException, IOException, ProcessingException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertNotNull(ExtractSedaActionHandler.getId());
        assertEquals(ExtractSedaActionHandler.getId(), HANDLER_ID);

        action.addOutIOParameters(out);
        final ItemStatus response = handler.execute(params, action);
        assertEquals(StatusCode.FATAL, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void givenWorkspaceExistWhenExecuteThenReturnResponseOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertNotNull(ExtractSedaActionHandler.getId());
        final InputStream seda_arborescence =
            PropertiesUtils.getResourceAsStream(SIP_ARBORESCENCE);
        when(workspaceClient.getObject(anyObject(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(seda_arborescence).build());
        action.addOutIOParameters(out);

        final ItemStatus response = handler.execute(params, action);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
        assertTrue(((String) response.getData().get(LogbookParameterName.eventDetailData.name()))
            .contains("ArchivalProfile0"));
    }

    @Test
    @RunWithCustomExecutor
    public void givenManifestEhessWhenExecuteThenReturnResponseOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertNotNull(ExtractSedaActionHandler.getId());

        final InputStream seda_arborescence =
            PropertiesUtils.getResourceAsStream(OK_EHESS_LIGHT);
        when(workspaceClient.getObject(anyObject(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(seda_arborescence).build());
        action.addOutIOParameters(out);

        final ItemStatus response = handler.execute(params, action);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void givenManifestMercierWhenExecuteThenReturnResponseOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertNotNull(ExtractSedaActionHandler.getId());
        final InputStream seda_arborescence =
            PropertiesUtils.getResourceAsStream(MERCIER);
        when(workspaceClient.getObject(anyObject(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(seda_arborescence).build());
        action.addOutIOParameters(out);

        final ItemStatus response = handler.execute(params, action);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void givenManifestRulesWhenExecuteThenReturnResponseOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertNotNull(ExtractSedaActionHandler.getId());
        final InputStream seda_arborescence =
            PropertiesUtils.getResourceAsStream(SIP_RULES);
        when(workspaceClient.getObject(anyObject(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(seda_arborescence).build());
        action.addOutIOParameters(out);

        final ItemStatus response = handler.execute(params, action);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void givenManifestArboRulesMDWhenExecuteThenReturnResponseOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertNotNull(ExtractSedaActionHandler.getId());
        final InputStream seda_arborescence =
            PropertiesUtils.getResourceAsStream(SIP_ARBO_RULES_MD);
        when(workspaceClient.getObject(anyObject(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(seda_arborescence).build());
        action.addOutIOParameters(out);

        final ItemStatus response = handler.execute(params, action);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void givenSipWithBdoWithoutGoWhenReadSipThenDetectBdoWithoutGo()
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException,
        FileNotFoundException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        final InputStream sedaLocal = new FileInputStream(PropertiesUtils.findFile("sip-bdo-orphan-ok1.xml"));
        when(workspaceClient.getObject(anyObject(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(sedaLocal).build());
        action.addOutIOParameters(out);
        final ItemStatus response = handler.execute(params, action);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void givenSipWithBdoWithGoWithArchiveUnitReferenceGoWhenReadSipThenReadSuccess()
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException,
        FileNotFoundException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        final InputStream sedaLocal = new FileInputStream(PropertiesUtils.findFile("sip-bdo-orphan-ok2.xml"));
        when(workspaceClient.getObject(anyObject(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(sedaLocal).build());
        action.addOutIOParameters(out);
        final ItemStatus response = handler.execute(params, action);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void givenSipFctTestWithBdoWithGoWithArchiveUnitReferenceBDOWhenReadSipThenThrowException()
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException,
        FileNotFoundException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        final InputStream sedaLocal = new FileInputStream(PropertiesUtils.findFile("sip-bdo-orphan-ok3-listBDO.xml"));
        when(workspaceClient.getObject(anyObject(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(sedaLocal).build());
        action.addOutIOParameters(out);
        final ItemStatus response = handler.execute(params, action);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void givenSipWithBdoWithGoWithArchiveUnitNotReferenceGoWhenReadSipThenReadSuccess()
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException,
        FileNotFoundException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        final InputStream sedaLocal = new FileInputStream(PropertiesUtils.findFile("sip-bdo-orphan-ok4.xml"));
        when(workspaceClient.getObject(anyObject(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(sedaLocal).build());
        action.addOutIOParameters(out);
        final ItemStatus response = handler.execute(params, action);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void givenSipWithDoubleBMThenFatal()
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException,
        FileNotFoundException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        final InputStream sedaLocal = new FileInputStream(PropertiesUtils.findFile("manifest_doubleBM.xml"));
        when(workspaceClient.getObject(anyObject(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(sedaLocal).build());
        action.addOutIOParameters(out);
        final ItemStatus response = handler.execute(params, action);
        assertEquals(StatusCode.KO, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void givenSipTransformToUsage_1ThenSuccess()
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException,
        FileNotFoundException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        final InputStream sedaLocal = new FileInputStream(PropertiesUtils.findFile("manifest_BM_TC.xml"));
        when(workspaceClient.getObject(anyObject(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(sedaLocal).build());
        action.addOutIOParameters(out);
        final ItemStatus response = handler.execute(params, action);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void givenSipWithBdoWithGoWithArchiveUnitReferenceGoWhenReadSipThenThrowException()
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException,
        FileNotFoundException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        final InputStream sedaLocal = new FileInputStream(PropertiesUtils.findFile("sip-bdo-orphan-err2.xml"));
        when(workspaceClient.getObject(anyObject(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(sedaLocal).build());
        action.addOutIOParameters(out);
        final ItemStatus response = handler.execute(params, action);
        assertEquals(StatusCode.KO, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void givenManifestWithMngMdAndAuWithMngtWhenExtractSedaThenReadSuccess()
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException,
        FileNotFoundException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        final InputStream sedaLocal = new FileInputStream(PropertiesUtils.findFile("sip-management-metadata-ok1.xml"));

        when(workspaceClient.getObject(anyObject(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(sedaLocal).build());
        action.addOutIOParameters(out);
        final ItemStatus response = handler.execute(params, action);

        assertEquals(StatusCode.OK, response.getGlobalStatus());

    }

    @Test
    @RunWithCustomExecutor
    public void givenManifestWithMngMdAndAuTreeWhenExtractSedaThenReadSuccess()
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException,
        FileNotFoundException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        final InputStream sedaLocal = new FileInputStream(PropertiesUtils.findFile("sip-management-metadata-ok2.xml"));

        when(workspaceClient.getObject(anyObject(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(sedaLocal).build());
        action.addOutIOParameters(out);
        final ItemStatus response = handler.execute(params, action);

        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

    // TODO : US 1686 test for add link between 2 existing units
    @Test
    @RunWithCustomExecutor
    public void givenManifestWithUpdateLinkExtractSedaThenReadSuccess()
        throws VitamException, IOException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        final InputStream sedaLocal = new FileInputStream(PropertiesUtils.findFile(SIP_ADD_LINK));
        JsonNode child = JsonHandler
            .getFromFile(PropertiesUtils.getResourceFile("extractSedaActionHandler/addLink/_Unit_CHILD.json"));
        JsonNode parent = JsonHandler
            .getFromFile(PropertiesUtils.getResourceFile("extractSedaActionHandler/addLink/_Unit_PARENT.json"));

        when(metadataClient.selectUnitbyId(any(), eq("GUID_ARCHIVE_UNIT_CHILD"))).thenReturn(child);
        when(metadataClient.selectUnitbyId(any(), eq("GUID_ARCHIVE_UNIT_PARENT"))).thenReturn(parent);

        when(workspaceClient.getObject(anyObject(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(sedaLocal).build());
        action.addOutIOParameters(out);

        final ItemStatus response = handler.execute(params, action);

        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void givenManifestWithUpdateAddLinkedUnitExtractSedaThenReadSuccess()
        throws VitamException, IOException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        final InputStream sedaLocal = new FileInputStream(PropertiesUtils.findFile(SIP_ADD_UNIT));
        JsonNode parent = JsonHandler
            .getFromFile(PropertiesUtils.getResourceFile("extractSedaActionHandler/addLink/_Unit_PARENT.json"));

        when(metadataClient.selectUnitbyId(any(), eq("GUID_ARCHIVE_UNIT_PARENT"))).thenReturn(parent);

        when(workspaceClient.getObject(anyObject(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(sedaLocal).build());
        action.addOutIOParameters(out);
        final ItemStatus response = handler.execute(params, action);

        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void givenManifestWithIngestContractContainsAUInThePlan() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        final InputStream sedaLocal = new FileInputStream(PropertiesUtils.findFile(SIP_TEST));
        AdminManagementClientFactory.changeMode(null);
        JsonNode parent = JsonHandler
            .getFromFile(PropertiesUtils.getResourceFile("extractSedaActionHandler/addLink/_Unit_PARENT.json"));
        when(metadataClient.selectUnitbyId(any(), eq("FilingParentId"))).thenReturn(parent);

        when(workspaceClient.getObject(anyObject(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(sedaLocal).build());
        action.addOutIOParameters(out);
        final ItemStatus response = handler.execute(params, action);

        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void givenManifestWithUpdateAddUnitExtractSedaThenCheckEvDetData()
        throws VitamException, IOException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        AdminManagementClientFactory.changeMode(null);;

        final InputStream sedaLocal = new FileInputStream(PropertiesUtils.findFile(SIP_ADD_UNIT));
        JsonNode parent = JsonHandler
            .getFromFile(PropertiesUtils.getResourceFile("extractSedaActionHandler/addLink/_Unit_PARENT.json"));

        when(metadataClient.selectUnitbyId(any(), eq("GUID_ARCHIVE_UNIT_PARENT"))).thenReturn(parent);
        when(metadataClient.selectUnitbyId(any(), eq("FilingParentId"))).thenReturn(parent);

        when(workspaceClient.getObject(anyObject(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(sedaLocal).build());
        action.addOutIOParameters(out);
        final ItemStatus response = handler.execute(params, action);

        // Check master evDetData
        String evDetDataString = (String) response.getData().get(LogbookParameterName.eventDetailData.name());
        JsonNode evDetData = JsonHandler.getFromString(evDetDataString);
        assertNotNull(evDetData);
        assertEquals("MASTER", evDetData.get("evDetDataType").asText());

        // Check comment
        assertEquals("Commentaire français", evDetData.get("EvDetailReq").asText());
        assertEquals("Commentaire français", evDetData.get("EvDetailReq_fr").asText());
        assertEquals("English Comment", evDetData.get("EvDetailReq_en").asText());
        assertEquals("Deutsch Kommentare", evDetData.get("EvDetailReq_de").asText());

        // Check other fields
        assertEquals("ArchivalAgreement0", evDetData.get("ArchivalAgreement").asText());
        assertEquals("Identifier1", evDetData.get("AgIfTrans").asText());
        assertEquals("2016-06-23T09:45:51.0", evDetData.get("EvDateTimeReq").asText());
    }

    @Test
    @RunWithCustomExecutor
    public void givenManifestWithUpdateAddLinkedUnitExtractSedaThenReadKO()
        throws VitamException, IOException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        final InputStream sedaLocal = new FileInputStream(PropertiesUtils.findFile(SIP_ADD_UNIT));
        when(workspaceClient.getObject(anyObject(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(sedaLocal).build());
        action.addOutIOParameters(out);
        final ItemStatus response = handler.execute(params, action);

        assertEquals(StatusCode.KO, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void givenSipWithAURefToBDOThenExtractKO()
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException,
        FileNotFoundException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        final InputStream sedaLocal = new FileInputStream(PropertiesUtils.findFile("sip-ko-bdo-ref-group.xml"));
        when(workspaceClient.getObject(anyObject(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(sedaLocal).build());
        action.addOutIOParameters(out);
        final ItemStatus response = handler.execute(params, action);
        assertEquals(StatusCode.KO, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void givenManifestPhysicalDataObjectExtractSedaThenReadSuccess() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertNotNull(ExtractSedaActionHandler.getId());

        final InputStream seda_arborescence =
            PropertiesUtils.getResourceAsStream(SIP_PHYSICAL_DATA_OBJECT);
        when(workspaceClient.getObject(anyObject(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(seda_arborescence).build());
        action.addOutIOParameters(out);

        final ItemStatus response = handler.execute(params, action);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void givenManifestInheritenceExtractSedaThenReadSuccess() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertNotNull(ExtractSedaActionHandler.getId());

        AdminManagementClientFactory.changeMode(null);
        final InputStream seda_arborescence =
            PropertiesUtils.getResourceAsStream(SIP_RULES_INHERITENCE);
        JsonNode parent = JsonHandler
            .getFromFile(PropertiesUtils.getResourceFile("extractSedaActionHandler/addLink/_Unit_PARENT.json"));
        when(metadataClient.selectUnitbyId(any(), eq("FilingParentId"))).thenReturn(parent);
        when(workspaceClient.getObject(anyObject(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(seda_arborescence).build());
        action.addOutIOParameters(out);

        final ItemStatus response = handler.execute(params, action);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void givenManifestRefIdInheritenceExtractSedaThenReadSuccess() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertNotNull(ExtractSedaActionHandler.getId());

        AdminManagementClientFactory.changeMode(null);
        final InputStream seda_arborescence =
            PropertiesUtils.getResourceAsStream(SIP_REFID_RULES_INHERITENCE);
        JsonNode parent = JsonHandler
            .getFromFile(PropertiesUtils.getResourceFile("extractSedaActionHandler/addLink/_Unit_PARENT.json"));
        when(metadataClient.selectUnitbyId(any(), eq("FilingParentId"))).thenReturn(parent);
        when(workspaceClient.getObject(anyObject(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(seda_arborescence).build());
        action.addOutIOParameters(out);

        final ItemStatus response = handler.execute(params, action);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void givenManifestRefnonruleidPreventinheritenceExtractSedaThenReadSuccess() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertNotNull(ExtractSedaActionHandler.getId());

        AdminManagementClientFactory.changeMode(null);
        final InputStream seda_arborescence =
            PropertiesUtils.getResourceAsStream(SIP_REFNONRULEID_PREVENTINHERITENCE);
        JsonNode parent = JsonHandler
            .getFromFile(PropertiesUtils.getResourceFile("extractSedaActionHandler/addLink/_Unit_PARENT.json"));
        when(metadataClient.selectUnitbyId(any(), eq("FilingParentId"))).thenReturn(parent);
        when(workspaceClient.getObject(anyObject(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(seda_arborescence).build());
        action.addOutIOParameters(out);

        final ItemStatus response = handler.execute(params, action);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void givenManifestRefnonruleidMultiplePreventinheritenceExtractSedaThenReadSuccess() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertNotNull(ExtractSedaActionHandler.getId());

        AdminManagementClientFactory.changeMode(null);
        final InputStream seda_arborescence =
            PropertiesUtils.getResourceAsStream(SIP_REFNONRULEID_MULT_PREVENTINHERITENCE);
        JsonNode parent = JsonHandler
            .getFromFile(PropertiesUtils.getResourceFile("extractSedaActionHandler/addLink/_Unit_PARENT.json"));
        when(metadataClient.selectUnitbyId(any(), eq("FilingParentId"))).thenReturn(parent);
        when(workspaceClient.getObject(anyObject(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(seda_arborescence).build());
        action.addOutIOParameters(out);

        final ItemStatus response = handler.execute(params, action);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void givenManifestWithSpecialCharactersWhenExecuteThenReturnResponseOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertNotNull(ExtractSedaActionHandler.getId());
        final InputStream seda_arborescence =
            PropertiesUtils.getResourceAsStream(SIP_WITH_SPECIAL_CHARACTERS);
        when(workspaceClient.getObject(anyObject(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(seda_arborescence).build());
        action.addOutIOParameters(out);

        final ItemStatus response = handler.execute(params, action);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void givenManifestWithMultiCommentWhenExecuteThenReturnResponseOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertNotNull(ExtractSedaActionHandler.getId());
        final InputStream seda_arborescence =
            PropertiesUtils.getResourceAsStream(OK_MULTI_COMMENT);
        when(workspaceClient.getObject(anyObject(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(seda_arborescence).build());
        action.addOutIOParameters(out);

        final ItemStatus response = handler.execute(params, action);
        assertEquals(StatusCode.OK, response.getGlobalStatus());

        JsonNode evDetData = JsonHandler.getFromString((String) response.getData().get("eventDetailData"));
        assertEquals(
            "Ceci est le premier commentaire_Voici le deuxi\u00E8me commentaire_Exemple de 3\u00E8me commentaire",
            evDetData.get("EvDetailReq").asText());
    }

}

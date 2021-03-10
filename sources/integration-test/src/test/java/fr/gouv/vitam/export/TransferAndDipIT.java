/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.export;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import fr.gouv.vitam.access.internal.client.AccessInternalClient;
import fr.gouv.vitam.access.internal.client.AccessInternalClientFactory;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalClientServerException;
import fr.gouv.vitam.access.internal.rest.AccessInternalMain;
import fr.gouv.vitam.batch.report.rest.BatchReportMain;
import fr.gouv.vitam.common.DataLoader;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.VitamTestHelper;
import fr.gouv.vitam.common.client.IngestCollection;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.client.VitamClientFactoryInterface;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.AccessUnauthorizedException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.format.identification.FormatIdentifierFactory;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.dip.DataObjectVersions;
import fr.gouv.vitam.common.model.export.ExportRequest;
import fr.gouv.vitam.common.model.export.ExportRequestParameters;
import fr.gouv.vitam.common.model.export.ExportType;
import fr.gouv.vitam.common.model.logbook.LogbookEventOperation;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClient;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClientFactory;
import fr.gouv.vitam.ingest.internal.common.exception.IngestInternalClientNotFoundException;
import fr.gouv.vitam.ingest.internal.common.exception.IngestInternalClientServerException;
import fr.gouv.vitam.ingest.internal.upload.rest.IngestInternalMain;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.parameters.Contexts;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.processing.management.rest.ProcessManagementMain;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.rest.DefaultOfferMain;
import fr.gouv.vitam.worker.core.plugin.transfer.reply.SaveAtrPlugin;
import fr.gouv.vitam.worker.core.plugin.transfer.reply.VerifyAtrPlugin;
import fr.gouv.vitam.worker.server.rest.WorkerMain;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static fr.gouv.vitam.common.VitamTestHelper.verifyOperation;
import static fr.gouv.vitam.common.guid.GUIDFactory.newOperationLogbookGUID;
import static fr.gouv.vitam.common.model.RequestResponseOK.TAG_RESULTS;
import static fr.gouv.vitam.common.model.StatusCode.STARTED;
import static fr.gouv.vitam.common.model.logbook.LogbookEvent.EV_ID_PROC;
import static fr.gouv.vitam.common.model.ProcessAction.RESUME;
import static fr.gouv.vitam.common.model.logbook.LogbookEvent.EV_TYPE;
import static fr.gouv.vitam.common.model.logbook.LogbookEvent.OUTCOME;
import static fr.gouv.vitam.common.thread.VitamThreadUtils.getVitamSession;
import static fr.gouv.vitam.logbook.common.parameters.Contexts.DEFAULT_WORKFLOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.junit.Assert.assertNotNull;

public class TransferAndDipIT extends VitamRuleRunner {
    private static final Integer TENANT_ID = 0;
    private static final String CONTRACT_ID = "aName5";
    private static final String CONTRACT_RULE_ID = "contract_rule";
    private static final String CONTEXT_ID = "Context_IT";

    private static final String SIP_OK_PHYSICAL_ARCHIVE = "integration-ingest-internal/OK_ArchivesPhysiques.zip";
    private static final String UNIT_WITHOUT_OBJECT_TRANSFER =
        "integration-ingest-internal/unit_without_object_transfer.zip";

    @ClassRule
    public static VitamServerRunner runner =
        new VitamServerRunner(TransferAndDipIT.class, mongoRule.getMongoDatabase().getName(),
            ElasticsearchRule.getClusterName(),
            Sets.newHashSet(
                MetadataMain.class,
                WorkerMain.class,
                AdminManagementMain.class,
                LogbookMain.class,
                WorkspaceMain.class,
                ProcessManagementMain.class,
                AccessInternalMain.class,
                IngestInternalMain.class,
                StorageMain.class,
                DefaultOfferMain.class,
                BatchReportMain.class
            )
        );

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        handleBeforeClass(Arrays.asList(0, 1), Collections.emptyMap());
        String CONFIG_SIEGFRIED_PATH =
            PropertiesUtils.getResourcePath("integration-ingest-internal/format-identifiers.conf").toString();

        FormatIdentifierFactory.getInstance().changeConfigurationFile(CONFIG_SIEGFRIED_PATH);

        new DataLoader("integration-ingest-internal").prepareData();
    }

    @AfterClass
    public static void tearDownAfterClass() {
        handleAfterClass();
        StorageClientFactory storageClientFactory = StorageClientFactory.getInstance();
        storageClientFactory.setVitamClientType(VitamClientFactoryInterface.VitamClientType.PRODUCTION);
        runAfter();
        VitamClientFactory.resetConnections();
    }

    @Before
    public void setUpBefore() {
        VitamTestHelper.prepareVitamSession(TENANT_ID, CONTRACT_ID, CONTEXT_ID);
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(TENANT_ID));
    }

    @Test
    @RunWithCustomExecutor
    public void should_export_DIP_with_all_data() throws Exception {
        // Given
        final String ingestOpId = VitamTestHelper.doIngest(TENANT_ID, SIP_OK_PHYSICAL_ARCHIVE);
        // As FormatIdentifierMock is used, pdf is identified as Plain Text File => WARNING
        verifyOperation(ingestOpId, StatusCode.WARNING);

        SelectMultiQuery select = new SelectMultiQuery();
        select.setQuery(QueryHelper.in(VitamFieldsHelper.operations(), ingestOpId));

        ExportRequest exportRequest = new ExportRequest(
            new DataObjectVersions(Collections.singleton("BinaryMaster")),
            select.getFinalSelect(),
            true
            // <- here with logbook
        );

        exportRequest.setExportType(ExportType.ArchiveDeliveryRequestReply);
        ExportRequestParameters exportRequestParameters = getExportRequestParameters();
        exportRequest.setExportRequestParameters(exportRequestParameters);

        // When ArchiveDeliveryRequestReply
        String exportOperationId = exportDIP(exportRequest);
        VitamTestHelper.verifyOperation(exportOperationId, StatusCode.OK);

        JsonNode logbook = VitamTestHelper.findLogbook(exportOperationId);
        assertNotNull(logbook);
        RequestResponseOK<JsonNode> response = RequestResponseOK.getFromJsonNode(logbook);
        assertThat(response.getResults()).isNotEmpty();
        assertThat(response.getResults().get(0).get(EV_TYPE).asText()).isEqualTo(Contexts.EXPORT_DIP.getEventType());

        String manifest = getManifestString(getDip(exportOperationId));

        // Then
        assertThat(manifest).contains("<?xml version=\"1.0\" ?><ArchiveDeliveryRequestReply");
        assertThat(manifest).contains("</ArchiveDeliveryRequestReply>");
        String header = PropertiesUtils.getResourceAsString("dip/header1");
        header = header.replace("OPERATION_ID_REPLACE", getVitamSession().getRequestId());
        assertThat(manifest).contains(header);

        String footer = PropertiesUtils.getResourceAsString("dip/footer1");
        assertThat(manifest).contains(footer);
        assertThat(manifest)
            .contains("</BinaryDataObject><LogBook><Event><EventIdentifier>"); // tag for GOT logbook LFC
        assertThat(manifest).contains("<Management><LogBook><Event><EventIdentifier>"); // tag for AU logbook LFC
    }

    @Test
    @RunWithCustomExecutor
    public void should_export_DIP_with_only_accessible_units() throws Exception {
        // Given
        final String ingestOpId = VitamTestHelper.doIngest(TENANT_ID, SIP_OK_PHYSICAL_ARCHIVE);
        // As FormatIdentifierMock is used, pdf is identified as Plain Text File => WARNING
        verifyOperation(ingestOpId, StatusCode.WARNING);


        SelectMultiQuery select = new SelectMultiQuery();
        select.setQuery(QueryHelper.in(VitamFieldsHelper.initialOperation(), ingestOpId));


        ExportRequest exportRequest = new ExportRequest(
            new DataObjectVersions(Collections.singleton("BinaryMaster")),
            select.getFinalSelect(),
            true
            // <- here with logbook
        );

        exportRequest.setExportType(ExportType.ArchiveDeliveryRequestReply);
        ExportRequestParameters exportRequestParameters = getExportRequestParameters();
        exportRequest.setExportRequestParameters(exportRequestParameters);

        // When ArchiveDeliveryRequestReply
        VitamThreadUtils.getVitamSession().setContractId(CONTRACT_RULE_ID);
        String exportOperationId = exportDIP(exportRequest);
        VitamTestHelper.verifyOperation(exportOperationId, StatusCode.KO);

        VitamThreadUtils.getVitamSession().setContractId(CONTRACT_ID);
        String computeInheritedRulesOperationId = computeInheritedRules(select.getFinalSelect());
        VitamTestHelper.verifyOperation(computeInheritedRulesOperationId, StatusCode.OK);

        VitamThreadUtils.getVitamSession().setContractId(CONTRACT_RULE_ID);

        exportOperationId = exportDIP(exportRequest);
        VitamTestHelper.verifyOperation(exportOperationId, StatusCode.OK);

        JsonNode logbook = VitamTestHelper.findLogbook(exportOperationId);
        assertNotNull(logbook);
        RequestResponseOK<JsonNode> response = RequestResponseOK.getFromJsonNode(logbook);
        assertThat(response.getResults()).isNotEmpty();
        assertThat(response.getResults().get(0).get(EV_TYPE).asText()).isEqualTo(Contexts.EXPORT_DIP.getEventType());

        String manifest = getManifestString(getDip(exportOperationId));

        // Then
        assertThat(manifest).contains("<?xml version=\"1.0\" ?><ArchiveDeliveryRequestReply");
        assertThat(manifest).contains("</ArchiveDeliveryRequestReply>");
        String header = PropertiesUtils.getResourceAsString("dip/header1");
        header = header.replace("OPERATION_ID_REPLACE", getVitamSession().getRequestId());
        assertThat(manifest).contains(header);

        String footer = PropertiesUtils.getResourceAsString("dip/footer1");
        assertThat(manifest).contains(footer);
        assertThat(manifest)
            .contains("</BinaryDataObject><LogBook><Event><EventIdentifier>"); // tag for GOT logbook LFC
        assertThat(manifest).contains("<Management><LogBook><Event><EventIdentifier>"); // tag for AU logbook LFC
    }

    @Test
    @RunWithCustomExecutor
    public void should_export_DIP_without_logbook() throws Exception {
        // Given
        final String ingestOpId = VitamTestHelper.doIngest(TENANT_ID, SIP_OK_PHYSICAL_ARCHIVE);
        // As FormatIdentifierMock is used, pdf is identified as Plain Text File => WARNING
        verifyOperation(ingestOpId, StatusCode.WARNING);

        SelectMultiQuery select = new SelectMultiQuery();
        select.setQuery(QueryHelper.in(VitamFieldsHelper.operations(), ingestOpId));


        ExportRequest exportRequest = new ExportRequest(
            new DataObjectVersions(Collections.singleton("BinaryMaster")),
            select.getFinalSelect(),
            false
            // <- here without logbook
        );

        exportRequest.setExportType(ExportType.MinimalArchiveDeliveryRequestReply);

        // When
        String exportOperationId = exportDIP(exportRequest);
        VitamTestHelper.verifyOperation(exportOperationId, StatusCode.OK);

        JsonNode logbook = VitamTestHelper.findLogbook(exportOperationId);
        assertNotNull(logbook);
        RequestResponseOK<JsonNode> response = RequestResponseOK.getFromJsonNode(logbook);
        assertThat(response.getResults()).isNotEmpty();
        assertThat(response.getResults().get(0).get(EV_TYPE).asText()).isEqualTo(Contexts.EXPORT_DIP.getEventType());

        String manifest = getManifestString(getDip(exportOperationId));

        // Then
        assertThat(manifest).contains("<?xml version=\"1.0\" ?><ArchiveDeliveryRequestReply");
        assertThat(manifest).contains("</ArchiveDeliveryRequestReply>");

        // tag for GOT logbook LFC
        assertThat(manifest).doesNotContain("</BinaryDataObject><LogBook><Event><EventIdentifier>");
        assertThat(manifest).doesNotContain("<Management><LogBook><Event><EventIdentifier>"); // tag for AU logbook LFC
    }

    @Test
    @RunWithCustomExecutor
    public void should_transfer_without_logbook() throws Exception {
        // Given
        final String ingestOpId = VitamTestHelper.doIngest(TENANT_ID, SIP_OK_PHYSICAL_ARCHIVE);
        // As FormatIdentifierMock is used, pdf is identified as Plain Text File => WARNING
        verifyOperation(ingestOpId, StatusCode.WARNING);

        SelectMultiQuery select = new SelectMultiQuery();
        select.setQuery(QueryHelper.in(VitamFieldsHelper.operations(), ingestOpId));

        ExportRequest exportRequest = new ExportRequest(
            new DataObjectVersions(Collections.singleton("BinaryMaster")),
            select.getFinalSelect(),
            false
            // <- here without logbook
        );

        exportRequest.setExportType(ExportType.ArchiveTransfer);

        ExportRequestParameters exportRequestParameters = getExportRequestParameters(ingestOpId);
        exportRequestParameters.setComment("Not Required comment for ArchiveDeliveryRequestReply");
        exportRequestParameters
            .setAuthorizationRequestReplyIdentifier("Not Required  AuthorizationRequestReplyIdentifier");
        exportRequestParameters.setTransferRequestReplyIdentifier("Not required TransferRequestReplyIdentifier");
        exportRequest.setExportRequestParameters(exportRequestParameters);



        String exportOperationId = exportDIP(exportRequest);
        VitamTestHelper.verifyOperation(exportOperationId, StatusCode.OK);

        JsonNode logbook = VitamTestHelper.findLogbook(exportOperationId);
        assertNotNull(logbook);
        RequestResponseOK<JsonNode> response = RequestResponseOK.getFromJsonNode(logbook);
        assertThat(response.getResults()).isNotEmpty();
        assertThat(response.getResults().get(0).get(EV_TYPE).asText())
            .isEqualTo(Contexts.ARCHIVE_TRANSFER.getEventType());

        String manifest = getManifestString(getTransferSIP(exportOperationId));

        // Then
        assertThat(manifest).contains("<?xml version=\"1.0\" ?><ArchiveTransfer");
        assertThat(manifest).contains("</ArchiveTransfer>");

        String header = PropertiesUtils.getResourceAsString("dip/header3");
        header = header.replace("OPERATION_ID_REPLACE", getVitamSession().getRequestId());
        assertThat(manifest).contains(header);

        String footer = PropertiesUtils.getResourceAsString("dip/footer3");
        assertThat(manifest).contains(footer);

        // tag for GOT logbook LFC
        assertThat(manifest).doesNotContain("</BinaryDataObject><LogBook><Event><EventIdentifier>");
        assertThat(manifest).doesNotContain("<Management><LogBook><Event><EventIdentifier>"); // tag for AU logbook LFC

        // try Ingest the Transfer SIP
        try (InputStream transferSipStream = getTransferSIP(getVitamSession().getRequestId())) {
            String opId = VitamTestHelper.doIngest(TENANT_ID, transferSipStream, DEFAULT_WORKFLOW, RESUME, STARTED);
            // As FormatIdentifierMock is used, pdf signature was modified in the first ingest. After transferByIngestGuid manifest and FormatIdentifierMock return the same mime type => status code OK
            VitamTestHelper.verifyOperation(opId, StatusCode.OK);
        }
    }

    @Test
    @RunWithCustomExecutor
    public void should_transfer_with_logbook() throws Exception {
        // Given
        final String ingestOpId = VitamTestHelper.doIngest(TENANT_ID, SIP_OK_PHYSICAL_ARCHIVE);
        // As FormatIdentifierMock is used, pdf is identified as Plain Text File => WARNING
        verifyOperation(ingestOpId, StatusCode.WARNING);

        SelectMultiQuery select = new SelectMultiQuery();
        select.setQuery(QueryHelper.in(VitamFieldsHelper.operations(), ingestOpId));

        ExportRequest exportRequest = new ExportRequest(
            new DataObjectVersions(Collections.singleton("BinaryMaster")),
            select.getFinalSelect(),
            true
            // <- here without logbook
        );

        exportRequest.setExportType(ExportType.ArchiveTransfer);

        ExportRequestParameters exportRequestParameters = getExportRequestParameters(ingestOpId);
        exportRequestParameters.setComment("Not Required comment for ArchiveDeliveryRequestReply");
        exportRequestParameters
            .setAuthorizationRequestReplyIdentifier("Not Required  AuthorizationRequestReplyIdentifier");
        exportRequestParameters.setTransferRequestReplyIdentifier("Not required TransferRequestReplyIdentifier");
        exportRequest.setExportRequestParameters(exportRequestParameters);

        String exportOperationId = exportDIP(exportRequest);
        VitamTestHelper.verifyOperation(exportOperationId, StatusCode.OK);

        JsonNode logbook = VitamTestHelper.findLogbook(exportOperationId);
        assertNotNull(logbook);
        RequestResponseOK<JsonNode> response = RequestResponseOK.getFromJsonNode(logbook);
        assertThat(response.getResults()).isNotEmpty();
        assertThat(response.getResults().get(0).get(EV_TYPE).asText())
            .isEqualTo(Contexts.ARCHIVE_TRANSFER.getEventType());

        String manifest = getManifestString(getTransferSIP(exportOperationId));

        // Then
        assertThat(manifest).contains("<?xml version=\"1.0\" ?><ArchiveTransfer");
        assertThat(manifest).contains("</ArchiveTransfer>");

        String header = PropertiesUtils.getResourceAsString("dip/header3");
        header = header.replace("OPERATION_ID_REPLACE", getVitamSession().getRequestId());
        assertThat(manifest).contains(header);

        String footer = PropertiesUtils.getResourceAsString("dip/footer3");
        assertThat(manifest).contains(footer);

        assertThat(manifest)
            .contains("</BinaryDataObject><LogBook><Event><EventIdentifier>"); // tag for GOT logbook LFC
        assertThat(manifest).contains("<Management><LogBook><Event><EventIdentifier>"); // tag for AU logbook LFC

        // try Ingest the Transfer SIP
        try (InputStream transferSipStream = getTransferSIP(getVitamSession().getRequestId())) {
            final String ingestTransfertOpId = VitamTestHelper.doIngest(TENANT_ID, transferSipStream, DEFAULT_WORKFLOW, RESUME, STARTED);
            // As FormatIdentifierMock is used, pdf signature was modified in the first ingest. After transferByIngestGuid manifest and FormatIdentifierMock return the same mime type => status code OK
            verifyOperation(ingestTransfertOpId, StatusCode.OK);
        }

    }

    @Test
    @RunWithCustomExecutor
    public void should_ingest_then_transfer_then_start_transfer_reply_in_OK() throws Exception {
        // Given
        final String ingestOpId = VitamTestHelper.doIngest(TENANT_ID, SIP_OK_PHYSICAL_ARCHIVE);
        // As FormatIdentifierMock is used, pdf is identified as Plain Text File => WARNING
        verifyOperation(ingestOpId, StatusCode.WARNING);

        GUID transferGuid = newOperationLogbookGUID(TENANT_ID);

        VitamThreadUtils.getVitamSession().setRequestId(transferGuid);
        SelectMultiQuery select = new SelectMultiQuery();
        select.setQuery(QueryHelper.in(VitamFieldsHelper.operations(), ingestOpId));

        ExportRequest exportRequest = new ExportRequest(
            new DataObjectVersions(Collections.singleton("BinaryMaster")),
            select.getFinalSelect(),
            false
        );

        ExportRequestParameters exportRequestParameters = getExportRequestParameters(ingestOpId);

        exportRequest.setExportType(ExportType.ArchiveTransfer);
        exportRequest.setExportRequestParameters(exportRequestParameters);


        String transferOpId = transfer(exportRequest);
        VitamTestHelper.verifyOperation(transferOpId, StatusCode.OK);
        InputStream transferSip = getTransferSIP(transferOpId);


        String transferredIngestedSipOpId = VitamTestHelper.doIngest(TENANT_ID, transferSip, DEFAULT_WORKFLOW, RESUME, STARTED);
        verifyOperation(transferredIngestedSipOpId, StatusCode.OK);
        String atr = getAtrTransferredSip(transferredIngestedSipOpId);

        // When
        GUID transferReplyWorkflowGuid = makeTransferReplyWorkflow(atr);
        List<LogbookEventOperation> logbookEvents = getLogbookEvents(transferReplyWorkflowGuid);

        // Then
        assertThat(logbookEvents).extracting(EV_TYPE, OUTCOME)
            .contains(
                tuple(VerifyAtrPlugin.PLUGIN_NAME, StatusCode.OK.name()),
                tuple(SaveAtrPlugin.PLUGIN_NAME, StatusCode.OK.name()),
                tuple(LogbookTypeProcess.TRANSFER_REPLY.name(), StatusCode.OK.name())
            );
    }

    @Test
    @RunWithCustomExecutor
    public void should_transfer_unit_with_no_binary() throws Exception {
        // Given
        String ingestOpId = VitamTestHelper.doIngest(TENANT_ID, UNIT_WITHOUT_OBJECT_TRANSFER);
        VitamTestHelper.verifyOperation(ingestOpId, StatusCode.WARNING);

        String manifest = getAtrTransferredSip(ingestOpId);
        String archiveGuid = retrieveArchiveUnitGuidById(manifest);
        assertThatCode(() -> {
            SelectMultiQuery select = new SelectMultiQuery();
            select.setQuery(QueryHelper.in(VitamFieldsHelper.id(), archiveGuid));

            ExportRequest exportRequest = new ExportRequest(
                new DataObjectVersions(Collections.singleton("BinaryMaster")),
                select.getFinalSelect(),
                false
            );

            ExportRequestParameters exportRequestParameters = getExportRequestParameters(archiveGuid);

            exportRequest.setExportType(ExportType.ArchiveTransfer);
            exportRequest.setExportRequestParameters(exportRequestParameters);

            String transferOpId = transfer(exportRequest);
            VitamTestHelper.verifyOperation(transferOpId, StatusCode.WARNING);
            getTransferSIP(transferOpId);
        }).doesNotThrowAnyException();
    }

    private String retrieveArchiveUnitGuidById(String manifest) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            org.w3c.dom.Document doc = builder.parse(new InputSource(new StringReader(manifest)));
            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();
            String archiveUnitElement = String.format("//ArchiveUnit[@id=\"%s\"]", "ID6");
            XPathExpression expr = xpath.compile(archiveUnitElement);
            NodeList nl = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
            return nl.item(0).getChildNodes().item(1).getFirstChild().getTextContent();
        } catch (SAXException | XPathExpressionException | IOException | ParserConfigurationException e) {
            e.printStackTrace();
            fail("Error while getting unit GUID by ID", e);
            return null;
        }
    }

    private List<LogbookEventOperation> getLogbookEvents(GUID transferReplyWorkflowGuid)
        throws LogbookClientException, InvalidParseOperationException, AccessUnauthorizedException {
        try (AccessInternalClient client = AccessInternalClientFactory.getInstance().getClient()) {
            JsonNode logbookEvents =
                client.selectOperationById(transferReplyWorkflowGuid.getId())
                    .toJsonNode()
                    .get("$results")
                    .get(0)
                    .get("events");

            return JsonHandler.getFromJsonNode(logbookEvents, new TypeReference<>() {
            });
        }
    }

    private GUID makeTransferReplyWorkflow(String atr) throws AccessInternalClientServerException {
        GUID transferReplyWorkflowGuid = GUIDFactory.newOperationLogbookGUID(TENANT_ID);
        VitamThreadUtils.getVitamSession().setRequestId(transferReplyWorkflowGuid);
        try (AccessInternalClient client = AccessInternalClientFactory.getInstance().getClient()) {
            client.startTransferReplyWorkflow(new ByteArrayInputStream(atr.getBytes(StandardCharsets.UTF_8)));
            VitamTestHelper.waitOperation(transferReplyWorkflowGuid.getId());
            VitamTestHelper.verifyOperation(transferReplyWorkflowGuid.getId(), StatusCode.OK);
        }
        return transferReplyWorkflowGuid;
    }

    private String getAtrTransferredSip(String transferredIngestedSipOpId)
        throws InvalidParseOperationException, IngestInternalClientServerException,
        IngestInternalClientNotFoundException, IOException {
        try (IngestInternalClient ingestExternalClient = IngestInternalClientFactory.getInstance().getClient()) {
            Response response =
                ingestExternalClient.downloadObjectAsync(transferredIngestedSipOpId, IngestCollection.REPORTS);

            try (InputStream manifestAsStream = response.readEntity(InputStream.class)) {
                return IOUtils.toString(manifestAsStream, StandardCharsets.UTF_8.name());
            }

        }
    }

    private String transfer(ExportRequest exportRequest)
        throws Exception {
        GUID transferGuid = newOperationLogbookGUID(TENANT_ID);
        getVitamSession().setRequestId(transferGuid);
        try (AccessInternalClient client = AccessInternalClientFactory.getInstance().getClient()) {
            client.exportByUsageFilter(exportRequest);
            VitamTestHelper.waitOperation(transferGuid.getId());

            return transferGuid.getId();
        }

    }

    private ExportRequestParameters getExportRequestParameters() {
        ExportRequestParameters exportRequestParameters = new ExportRequestParameters();
        exportRequestParameters.setMessageRequestIdentifier("Required MessageRequestIdentifier");
        exportRequestParameters.setArchivalAgencyIdentifier("Required ArchivalAgencyIdentifier");
        exportRequestParameters.setRequesterIdentifier("Required RequesterIdentifier");
        exportRequestParameters.setComment("Not Required comment for ArchiveDeliveryRequestReply");
        exportRequestParameters
            .setAuthorizationRequestReplyIdentifier("Not Required  AuthorizationRequestReplyIdentifier");
        exportRequestParameters.setArchivalAgreement("Not Required ArchivalAgreement");
        exportRequestParameters.setOriginatingAgencyIdentifier("Not Required OriginatingAgencyIdentifier");
        exportRequestParameters.setSubmissionAgencyIdentifier("Not Required SubmissionAgencyIdentifier");
        exportRequestParameters
            .setRelatedTransferReference(Lists.newArrayList("RelatedTransferReference1", "RelatedTransferReference2"));
        return exportRequestParameters;
    }


    private ExportRequestParameters getExportRequestParameters(String opi) {
        ExportRequestParameters exportRequestParameters = new ExportRequestParameters();
        exportRequestParameters.setMessageRequestIdentifier(opi);
        exportRequestParameters.setArchivalAgencyIdentifier("Identifier4");
        exportRequestParameters.setRequesterIdentifier("Required RequesterIdentifier");
        exportRequestParameters.setArchivalAgreement("ArchivalAgreement0");
        exportRequestParameters.setOriginatingAgencyIdentifier("FRAN_NP_050056");
        exportRequestParameters.setSubmissionAgencyIdentifier("FRAN_NP_050056");
        exportRequestParameters.setRelatedTransferReference(List.of("RelatedTransferReference1",
            "RelatedTransferReference2"));
        return exportRequestParameters;
    }

    private String exportDIP(ExportRequest exportRequest) {
        GUID operationGuid = GUIDFactory.newOperationLogbookGUID(TENANT_ID);
        getVitamSession().setRequestId(operationGuid);
        try (AccessInternalClient client = AccessInternalClientFactory.getInstance().getClient()) {
            client.exportByUsageFilter(exportRequest);
            VitamTestHelper.waitOperation(operationGuid.getId());
            return operationGuid.getId();
        } catch (AccessInternalClientServerException e) {
            e.printStackTrace();
            fail("Error while running export DIP");
            return null;
        }
    }

    private String getManifestString(InputStream dip) throws Exception {
        File dipFile = File.createTempFile("tmp", ".zip", new File(VitamConfiguration.getVitamTmpFolder()));
        try (dip) {
            IOUtils.copy(dip, new FileOutputStream(dipFile));
        }
        ZipFile zipFile = new ZipFile(dipFile);
        ZipArchiveEntry manifest = zipFile.getEntry("manifest.xml");
        try (InputStream is = zipFile.getInputStream(manifest)) {
            return IOUtils.toString(is, StandardCharsets.UTF_8.name());
        }
    }

    private InputStream getDip(String operationId) throws Exception {
        try (AccessInternalClient client = AccessInternalClientFactory.getInstance().getClient()) {
            return client.findExportByID(operationId).readEntity(InputStream.class);
        }
    }

    private InputStream getTransferSIP(String operationId) throws Exception {
        try (AccessInternalClient client = AccessInternalClientFactory.getInstance().getClient()) {
            JsonNode logbook = client.selectOperationById(operationId).toJsonNode().get(TAG_RESULTS).get(0);
            String evIdProc = logbook.get(EV_ID_PROC).asText();
            return client.findTransferSIPByID(evIdProc).readEntity(InputStream.class);
        }
    }

    private String computeInheritedRules(JsonNode query) {
        try (AccessInternalClient accessInternalClient = AccessInternalClientFactory.getInstance().getClient()) {
            GUID guid = newOperationLogbookGUID(TENANT_ID);
            VitamThreadUtils.getVitamSession().setRequestId(guid);
            accessInternalClient.startComputeInheritedRules(query);
            VitamTestHelper.waitOperation(guid.getId());
            return guid.getId();
        } catch (AccessInternalClientServerException e) {
            e.printStackTrace();
            fail("Error while computing inherited rules");
            return null;
        }
    }
}

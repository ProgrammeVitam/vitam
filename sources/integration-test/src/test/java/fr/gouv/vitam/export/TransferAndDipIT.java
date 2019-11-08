/*
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
 */
package fr.gouv.vitam.export;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mongodb.client.model.Filters;
import fr.gouv.vitam.access.internal.client.AccessInternalClient;
import fr.gouv.vitam.access.internal.client.AccessInternalClientFactory;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalClientServerException;
import fr.gouv.vitam.access.internal.rest.AccessInternalMain;
import fr.gouv.vitam.batch.report.rest.BatchReportMain;
import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.DataLoader;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.client.IngestCollection;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.client.VitamClientFactoryInterface;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.AccessUnauthorizedException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.format.identification.FormatIdentifierFactory;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.dip.DataObjectVersions;
import fr.gouv.vitam.common.model.export.ExportRequest;
import fr.gouv.vitam.common.model.export.ExportRequestParameters;
import fr.gouv.vitam.common.model.export.ExportType;
import fr.gouv.vitam.common.model.logbook.LogbookEventOperation;
import fr.gouv.vitam.common.model.processing.WorkFlow;
import fr.gouv.vitam.common.stream.StreamUtils;
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
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.processing.common.model.ProcessWorkflow;
import fr.gouv.vitam.processing.engine.core.monitoring.ProcessMonitoringImpl;
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
import org.bson.Document;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static fr.gouv.vitam.common.VitamServerRunner.PORT_SERVICE_INGEST_INTERNAL;
import static fr.gouv.vitam.common.guid.GUIDFactory.newOperationLogbookGUID;
import static fr.gouv.vitam.common.model.logbook.LogbookEvent.EV_TYPE;
import static fr.gouv.vitam.common.model.logbook.LogbookEvent.OUTCOME;
import static fr.gouv.vitam.common.thread.VitamThreadUtils.getVitamSession;
import static fr.gouv.vitam.preservation.ProcessManagementWaiter.waitOperation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TransferAndDipIT extends VitamRuleRunner {
    private static final Integer tenantId = 0;
    private static final long SLEEP_TIME = 20L;
    private static final long NB_TRY = 18000; // equivalent to 16 minute
    private static final String WORKFLOW_ID = "DEFAULT_WORKFLOW";
    private static final String CONTEXT_ID = "PROCESS_SIP_UNITARY";
    private static final String TRANSFERS_OPERATION = "Transfers";
    private final static TypeReference<List<LogbookEventOperation>> TYPE_REFERENCE = new TypeReference<List<LogbookEventOperation>>() {};

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
        handleBeforeClass(0, 1);
        String CONFIG_SIEGFRIED_PATH =
            PropertiesUtils.getResourcePath("integration-ingest-internal/format-identifiers.conf").toString();

        FormatIdentifierFactory.getInstance().changeConfigurationFile(CONFIG_SIEGFRIED_PATH);

        new DataLoader("integration-ingest-internal").prepareData();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        handleAfterClass(0, 1);
        StorageClientFactory storageClientFactory = StorageClientFactory.getInstance();
        storageClientFactory.setVitamClientType(VitamClientFactoryInterface.VitamClientType.PRODUCTION);
        runAfter();
        VitamClientFactory.resetConnections();
    }

    @Before
    public void setUpBefore() {
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(0));
    }

    @Test
    @RunWithCustomExecutor
    public void should_export_DIP_with_all_data() throws Exception {
        // Given
        String SIP_OK_PHYSICAL_ARCHIVE = "integration-ingest-internal/OK_ArchivesPhysiques.zip";
        InputStream zipInputStreamSipObject = PropertiesUtils.getResourceAsStream(SIP_OK_PHYSICAL_ARCHIVE);
        final GUID ingestGUID = ingestSip(zipInputStreamSipObject,
            StatusCode.WARNING); // As FormatIdentifierMock is used, pdf is identified as Plain Text File => WARNING

        SelectMultiQuery select = new SelectMultiQuery();
        select.setQuery(QueryHelper.in(VitamFieldsHelper.operations(), ingestGUID.toString()));

        ExportRequest exportRequest = new ExportRequest(
            new DataObjectVersions(Collections.singleton("BinaryMaster")),
            select.getFinalSelect(),
            true
            // <- here with logbook
        );

        exportRequest.setExportType(ExportType.ArchiveDeliveryRequestReply);

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

        exportRequest.setExportRequestParameters(exportRequestParameters);

        // When ArchiveDeliveryRequestReply
        String manifest = createAndExtractDip(exportRequest, "ExportDIP");

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
        String SIP_OK_PHYSICAL_ARCHIVE = "integration-ingest-internal/OK_ArchivesPhysiques.zip";
        InputStream zipInputStreamSipObject = PropertiesUtils.getResourceAsStream(SIP_OK_PHYSICAL_ARCHIVE);
        final GUID ingestGUID = ingestSip(zipInputStreamSipObject,
            StatusCode.WARNING); // As FormatIdentifierMock is used, pdf is identified as Plain Text File => WARNING
        SelectMultiQuery select = new SelectMultiQuery();
        select.setQuery(QueryHelper.in(VitamFieldsHelper.operations(), ingestGUID.toString()));

        ExportRequest exportRequest = new ExportRequest(
            new DataObjectVersions(Collections.singleton("BinaryMaster")),
            select.getFinalSelect(),
            false
            // <- here without logbook
        );

        exportRequest.setExportType(ExportType.MinimalArchiveDeliveryRequestReply);

        // When
        String manifest = createAndExtractDip(exportRequest, "ExportDIP");

        // Then
        assertThat(manifest).contains("<?xml version=\"1.0\" ?><ArchiveDeliveryRequestReply");
        assertThat(manifest).contains("</ArchiveDeliveryRequestReply>");

        assertThat(manifest)
            .doesNotContain("</BinaryDataObject><LogBook><Event><EventIdentifier>"); // tag for GOT logbook LFC
        assertThat(manifest).doesNotContain("<Management><LogBook><Event><EventIdentifier>"); // tag for AU logbook LFC
    }

    @Test
    @RunWithCustomExecutor
    public void should_transfer_without_logbook() throws Exception {
        // Given
        String SIP_OK_PHYSICAL_ARCHIVE = "integration-ingest-internal/OK_ArchivesPhysiques.zip";
        InputStream zipInputStreamSipObject = PropertiesUtils.getResourceAsStream(SIP_OK_PHYSICAL_ARCHIVE);

        final GUID ingestGUID = ingestSip(zipInputStreamSipObject,
            StatusCode.WARNING); // As FormatIdentifierMock is used, pdf is identified as Plain Text File => WARNING

        SelectMultiQuery select = new SelectMultiQuery();
        select.setQuery(QueryHelper.in(VitamFieldsHelper.operations(), ingestGUID.toString()));

        ExportRequest exportRequest = new ExportRequest(
            new DataObjectVersions(Collections.singleton("BinaryMaster")),
            select.getFinalSelect(),
            false
            // <- here without logbook
        );

        exportRequest.setExportType(ExportType.ArchiveTransfer);

        ExportRequestParameters exportRequestParameters = new ExportRequestParameters();
        exportRequestParameters.setMessageRequestIdentifier(ingestGUID.getId());
        exportRequestParameters.setArchivalAgencyIdentifier("Identifier4");
        exportRequestParameters.setRequesterIdentifier("Required RequesterIdentifier");

        exportRequestParameters.setComment("Not Required comment for ArchiveDeliveryRequestReply");
        exportRequestParameters
            .setAuthorizationRequestReplyIdentifier("Not Required  AuthorizationRequestReplyIdentifier");
        exportRequestParameters.setArchivalAgreement("ArchivalAgreement0");
        exportRequestParameters.setOriginatingAgencyIdentifier("FRAN_NP_050056");
        exportRequestParameters.setSubmissionAgencyIdentifier("FRAN_NP_050056");
        exportRequestParameters
            .setRelatedTransferReference(Lists.newArrayList("RelatedTransferReference1", "RelatedTransferReference2"));
        exportRequestParameters.setTransferRequestReplyIdentifier("Not required TransferRequestReplyIdentifier");
        exportRequest.setExportRequestParameters(exportRequestParameters);

        String manifest = createAndExtractDip(exportRequest, "Transfers");

        // Then
        assertThat(manifest).contains("<?xml version=\"1.0\" ?><ArchiveTransfer");
        assertThat(manifest).contains("</ArchiveTransfer>");

        String header = PropertiesUtils.getResourceAsString("dip/header3");
        header = header.replace("OPERATION_ID_REPLACE", getVitamSession().getRequestId());
        assertThat(manifest).contains(header);

        String footer = PropertiesUtils.getResourceAsString("dip/footer3");
        assertThat(manifest).contains(footer);

        assertThat(manifest)
            .doesNotContain("</BinaryDataObject><LogBook><Event><EventIdentifier>"); // tag for GOT logbook LFC
        assertThat(manifest).doesNotContain("<Management><LogBook><Event><EventIdentifier>"); // tag for AU logbook LFC

        // try Ingest the Transfer SIP
        InputStream transferSipStream = getTransferSIP(getVitamSession().getRequestId());

        ingestSip(transferSipStream,
            StatusCode.OK); // As FormatIdentifierMock is used, pdf signature was modified in the first ingest. After transfer manifest and FormatIdentifierMock return the same mime type => status code OK

    }

    @Test
    @RunWithCustomExecutor
    public void should_transfer_with_logbook() throws Exception {
        // Given
        String SIP_OK_PHYSICAL_ARCHIVE = "integration-ingest-internal/OK_ArchivesPhysiques.zip";
        InputStream zipInputStreamSipObject = PropertiesUtils.getResourceAsStream(SIP_OK_PHYSICAL_ARCHIVE);

        final GUID ingestGUID = ingestSip(zipInputStreamSipObject,
            StatusCode.WARNING); // As FormatIdentifierMock is used, pdf is identified as Plain Text File => WARNING

        SelectMultiQuery select = new SelectMultiQuery();
        select.setQuery(QueryHelper.in(VitamFieldsHelper.operations(), ingestGUID.toString()));

        ExportRequest dipExportRequest = new ExportRequest(
            new DataObjectVersions(Collections.singleton("BinaryMaster")),
            select.getFinalSelect(),
            true
            // <- here without logbook
        );

        dipExportRequest.setExportType(ExportType.ArchiveTransfer);

        ExportRequestParameters exportRequestParameters = new ExportRequestParameters();
        exportRequestParameters.setMessageRequestIdentifier(ingestGUID.getId());
        exportRequestParameters.setArchivalAgencyIdentifier("Identifier4");
        exportRequestParameters.setRequesterIdentifier("Required RequesterIdentifier");

        exportRequestParameters.setComment("Not Required comment for ArchiveDeliveryRequestReply");
        exportRequestParameters
            .setAuthorizationRequestReplyIdentifier("Not Required  AuthorizationRequestReplyIdentifier");
        exportRequestParameters.setArchivalAgreement("ArchivalAgreement0");
        exportRequestParameters.setOriginatingAgencyIdentifier("FRAN_NP_050056");
        exportRequestParameters.setSubmissionAgencyIdentifier("FRAN_NP_050056");
        exportRequestParameters
            .setRelatedTransferReference(Lists.newArrayList("RelatedTransferReference1", "RelatedTransferReference2"));
        exportRequestParameters.setTransferRequestReplyIdentifier("Not required TransferRequestReplyIdentifier");
        dipExportRequest.setExportRequestParameters(exportRequestParameters);

        String manifest = createAndExtractDip(dipExportRequest, "Transfers");

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
        InputStream transferSipStream = getTransferSIP(getVitamSession().getRequestId());

        ingestSip(transferSipStream,
            StatusCode.OK); // As FormatIdentifierMock is used, pdf signature was modified in the first ingest. After transfer manifest and FormatIdentifierMock return the same mime type => status code OK

    }

    @Test
    @RunWithCustomExecutor
    public void should_ingest_then_transfer_then_start_transfer_reply_in_OK() throws Exception {
        // Given
        GUID ingestGuid =
            ingestSip(PropertiesUtils.getResourceAsStream("integration-ingest-internal/OK_ArchivesPhysiques.zip"),
                StatusCode.WARNING);

        GUID transferGuid = newOperationLogbookGUID(tenantId);
        InputStream transferSip = transfer(ingestGuid, transferGuid);

        GUID transferredIngestedSip = ingestSip(transferSip, StatusCode.OK);
        String atr = getAtrTransferredSip(transferredIngestedSip);

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

    private List<LogbookEventOperation> getLogbookEvents(GUID transferReplyWorkflowGuid) throws LogbookClientException, InvalidParseOperationException, AccessUnauthorizedException {
        try (AccessInternalClient client = AccessInternalClientFactory.getInstance().getClient()){
            JsonNode logbookEvents = client.selectOperationById(transferReplyWorkflowGuid.getId(), new SelectMultiQuery().getFinalSelect()).toJsonNode()
                    .get("$results")
                    .get(0)
                    .get("events");

            return JsonHandler.getFromJsonNode(logbookEvents, TYPE_REFERENCE);
        }
    }

    private GUID makeTransferReplyWorkflow(String atr) throws AccessInternalClientServerException {
        GUID transferReplyWorkflowGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        VitamThreadUtils.getVitamSession().setRequestId(transferReplyWorkflowGuid);
        try (AccessInternalClient client = AccessInternalClientFactory.getInstance().getClient()){
            client.startTransferReplyWorkflow(new ByteArrayInputStream(atr.getBytes(StandardCharsets.UTF_8)));
            awaitForWorkflowTerminationWithStatus(transferReplyWorkflowGuid, StatusCode.OK);
        }
        return transferReplyWorkflowGuid;
    }

    private String getAtrTransferredSip(GUID transferredIngestedSip)
        throws InvalidParseOperationException, IngestInternalClientServerException,
        IngestInternalClientNotFoundException, IOException {
        try (IngestInternalClient ingestExternalClient = IngestInternalClientFactory.getInstance().getClient()) {
            Response response =
                ingestExternalClient.downloadObjectAsync(transferredIngestedSip.getId(), IngestCollection.REPORTS);

            try (InputStream manifestAsStream = response.readEntity(InputStream.class)) {
                return IOUtils.toString(manifestAsStream, StandardCharsets.UTF_8.name());
            }

        }
    }

    private InputStream transfer(GUID ingestGUID, GUID transferGuid) throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(transferGuid);
        SelectMultiQuery select = new SelectMultiQuery();
        select.setQuery(QueryHelper.in(VitamFieldsHelper.operations(), ingestGUID.toString()));

        ExportRequest exportRequest = new ExportRequest(
            new DataObjectVersions(Collections.singleton("BinaryMaster")),
            select.getFinalSelect(),
            false
        );

        ExportRequestParameters exportRequestParameters = new ExportRequestParameters();
        exportRequestParameters.setMessageRequestIdentifier(ingestGUID.getId());
        exportRequestParameters.setArchivalAgencyIdentifier("Identifier4");
        exportRequestParameters.setRequesterIdentifier("Required RequesterIdentifier");
        exportRequestParameters.setArchivalAgreement("ArchivalAgreement0");
        exportRequestParameters.setOriginatingAgencyIdentifier("FRAN_NP_050056");
        exportRequestParameters.setSubmissionAgencyIdentifier("FRAN_NP_050056");
        exportRequestParameters
            .setRelatedTransferReference(Lists.newArrayList("RelatedTransferReference1", "RelatedTransferReference2"));

        exportRequest.setExportType(ExportType.ArchiveTransfer);
        exportRequest.setExportRequestParameters(exportRequestParameters);

        prepareVitamSession();
        getVitamSession().setRequestId(transferGuid);
        try (AccessInternalClient client = AccessInternalClientFactory.getInstance().getClient()) {
            client.exportByUsageFilter(exportRequest);

            awaitForWorkflowTerminationWithStatus(transferGuid, StatusCode.OK);

            return getTransferSIP(transferGuid.getId());
        }
    }

    private String createAndExtractDip(ExportRequest exportRequest, String operation) throws Exception {
        GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        prepareVitamSession();
        getVitamSession().setRequestId(operationGuid);
        try (AccessInternalClient client = AccessInternalClientFactory.getInstance().getClient()) {
            client.exportByUsageFilter(exportRequest);

            awaitForWorkflowTerminationWithStatus(operationGuid, StatusCode.OK);
            Document document = (Document) LogbookCollections.OPERATION.getCollection()
                .find(Filters.eq(getVitamSession().getRequestId())).first();

            switch (exportRequest.getExportType()) {
                case ArchiveTransfer:
                    assertThat(document.getString("evType")).isEqualTo(Contexts.ARCHIVE_TRANSFER.getEventType());
                    break;
                case ArchiveDeliveryRequestReply:
                case MinimalArchiveDeliveryRequestReply:
                    assertThat(document.getString("evType")).isEqualTo(Contexts.EXPORT_DIP.getEventType());
                    break;
            }
            if(operation.equals(TRANSFERS_OPERATION))
            {
                try (InputStream dip = getTransferSIP(operationGuid.getId())) {
                    File dipFile = File.createTempFile("tmp", ".zip", new File(VitamConfiguration.getVitamTmpFolder()));
                    IOUtils.copy(dip, new FileOutputStream(dipFile));
                    ZipFile zipFile = new ZipFile(dipFile);
                    ZipArchiveEntry manifest = zipFile.getEntry("manifest.xml");
                    return IOUtils.toString(zipFile.getInputStream(manifest), StandardCharsets.UTF_8.name());
                }

            } else {
                try (InputStream dip = getDip(operationGuid.getId())) {
                    File dipFile = File.createTempFile("tmp", ".zip", new File(VitamConfiguration.getVitamTmpFolder()));
                    IOUtils.copy(dip, new FileOutputStream(dipFile));
                    ZipFile zipFile = new ZipFile(dipFile);
                    ZipArchiveEntry manifest = zipFile.getEntry("manifest.xml");
                    return IOUtils.toString(zipFile.getInputStream(manifest), StandardCharsets.UTF_8.name());
                }
            }
        }
    }

    private InputStream getDip(String operationId)
        throws Exception {
        try (AccessInternalClient client = AccessInternalClientFactory.getInstance().getClient()) {
            return client.findExportByID(operationId).readEntity(InputStream.class);
        }
    }

    private InputStream getTransferSIP(String operationId)
        throws Exception {
        try (AccessInternalClient client = AccessInternalClientFactory.getInstance().getClient()) {
            JsonNode logbook =
                client.selectOperationById(operationId, new SelectMultiQuery().getFinalSelect()).toJsonNode()
                    .get("$results")
                    .get(0);

            String evIdProc = logbook.get("evIdProc").asText();

            return client.findTransferSIPByID(evIdProc).readEntity(InputStream.class);
        }
    }

    private GUID ingestSip(InputStream inputStream, StatusCode expectedStatusCode) throws VitamException {
        GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        prepareVitamSession();
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);

        List<LogbookOperationParameters> params = new ArrayList<>();
        LogbookOperationParameters initParameters = LogbookParametersFactory.newLogbookOperationParameters(
            operationGuid,
            "Process_SIP_unitary",
            operationGuid,
            LogbookTypeProcess.INGEST, StatusCode.STARTED,
            operationGuid.toString(),
            operationGuid
        );
        params.add(initParameters);

        IngestInternalClientFactory.getInstance().changeServerPort(PORT_SERVICE_INGEST_INTERNAL);
        WorkFlow ingest = WorkFlow.of(WORKFLOW_ID, CONTEXT_ID, "INGEST");
        try (IngestInternalClient client = IngestInternalClientFactory.getInstance().getClient()) {
            client.uploadInitialLogbook(params);

            client.initWorkflow(ingest);

            client.upload(inputStream, CommonMediaType.ZIP_TYPE, ingest, ProcessAction.RESUME.name());

            awaitForWorkflowTerminationWithStatus(operationGuid, expectedStatusCode);
        } finally {
            StreamUtils.closeSilently(inputStream);
        }
        return operationGuid;
    }

    public void prepareVitamSession() {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        VitamThreadUtils.getVitamSession().setContractId("aName5");
        VitamThreadUtils.getVitamSession().setContextId("Context_IT");
    }

    private void awaitForWorkflowTerminationWithStatus(GUID operationGuid, StatusCode status) {
        waitOperation(NB_TRY, SLEEP_TIME, operationGuid.toString());

        ProcessWorkflow processWorkflow =
            ProcessMonitoringImpl.getInstance().findOneProcessWorkflow(operationGuid.toString(), tenantId);

        assertNotNull(processWorkflow);
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(status, processWorkflow.getStatus());
    }
}

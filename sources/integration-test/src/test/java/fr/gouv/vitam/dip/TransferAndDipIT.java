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
package fr.gouv.vitam.dip;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import fr.gouv.vitam.access.internal.client.AccessInternalClient;
import fr.gouv.vitam.access.internal.client.AccessInternalClientFactory;
import fr.gouv.vitam.access.internal.rest.AccessInternalMain;
import fr.gouv.vitam.batch.report.rest.BatchReportMain;
import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.DataLoader;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.client.VitamClientFactoryInterface;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.format.identification.FormatIdentifierFactory;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.dip.DataObjectVersions;
import fr.gouv.vitam.common.model.dip.DipExportRequest;
import fr.gouv.vitam.common.model.dip.ExportRequestParameters;
import fr.gouv.vitam.common.model.dip.ExportType;
import fr.gouv.vitam.common.model.processing.WorkFlow;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClient;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClientFactory;
import fr.gouv.vitam.ingest.internal.upload.rest.IngestInternalMain;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.processing.common.model.ProcessWorkflow;
import fr.gouv.vitam.processing.engine.core.monitoring.ProcessMonitoringImpl;
import fr.gouv.vitam.processing.management.rest.ProcessManagementMain;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.rest.DefaultOfferMain;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static fr.gouv.vitam.common.VitamServerRunner.PORT_SERVICE_INGEST_INTERNAL;
import static fr.gouv.vitam.common.guid.GUIDFactory.newOperationLogbookGUID;
import static fr.gouv.vitam.common.thread.VitamThreadUtils.getVitamSession;
import static fr.gouv.vitam.preservation.ProcessManagementWaiter.waitOperation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TransferAndDipIT extends VitamRuleRunner {
    private static final Integer tenantId = 0;
    private static final long SLEEP_TIME = 20L;
    private static final long NB_TRY = 18000; // equivalent to 16 minute
    private static final String WORKFLOW_ID = "DEFAULT_WORKFLOW";
    private static final String CONTEXT_ID = "PROCESS_SIP_UNITARY";

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
    public void should_export_DIP_with_logbook() throws Exception {
        // Given
        String SIP_OK_PHYSICAL_ARCHIVE = "integration-ingest-internal/OK_ArchivesPhysiques.zip";
        InputStream zipInputStreamSipObject = PropertiesUtils.getResourceAsStream(SIP_OK_PHYSICAL_ARCHIVE);
        final GUID ingestGUID = ingestSip(zipInputStreamSipObject,
            StatusCode.WARNING); // As FormatIdentifierMock is used, pdf is identified as Plain Text File => WARNING

        SelectMultiQuery select = new SelectMultiQuery();
        select.setQuery(QueryHelper.in(VitamFieldsHelper.operations(), ingestGUID.toString()));

        DipExportRequest dipExportRequest = new DipExportRequest(
            new DataObjectVersions(Collections.singleton("BinaryMaster")),
            select.getFinalSelect(),
            true
            // <- here with logbook
        );

        dipExportRequest.setExportType(ExportType.ArchiveDeliveryRequestReply);

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

        dipExportRequest.setExportRequestParameters(exportRequestParameters);

        // When ArchiveDeliveryRequestReply
        String manifest = createAndExtractDip(dipExportRequest);

        // Then
        assertThat(manifest).contains("<?xml version=\"1.0\" ?><ArchiveDeliveryRequestReply");
        assertThat(manifest).contains("</ArchiveDeliveryRequestReply>");
        assertThat(manifest).contains(
            "</Date><MessageIdentifier>" + getVitamSession().getRequestId() +
                "</MessageIdentifier><ArchivalAgreement>Not Required ArchivalAgreement</ArchivalAgreement><CodeListVersions><ReplyCodeListVersion>ReplyCodeListVersion0</ReplyCodeListVersion><MessageDigestAlgorithmCodeListVersion>MessageDigestAlgorithmCodeListVersion0</MessageDigestAlgorithmCodeListVersion><MimeTypeCodeListVersion>MimeTypeCodeListVersion0</MimeTypeCodeListVersion><EncodingCodeListVersion>EncodingCodeListVersion0</EncodingCodeListVersion><FileFormatCodeListVersion>FileFormatCodeListVersion0</FileFormatCodeListVersion><CompressionAlgorithmCodeListVersion>CompressionAlgorithmCodeListVersion0</CompressionAlgorithmCodeListVersion><DataObjectVersionCodeListVersion>DataObjectVersionCodeListVersion0</DataObjectVersionCodeListVersion><StorageRuleCodeListVersion>StorageRuleCodeListVersion0</StorageRuleCodeListVersion><AppraisalRuleCodeListVersion>AppraisalRuleCodeListVersion0</AppraisalRuleCodeListVersion><AccessRuleCodeListVersion>AccessRuleCodeListVersion0</AccessRuleCodeListVersion><DisseminationRuleCodeListVersion>DisseminationRuleCodeListVersion0</DisseminationRuleCodeListVersion><ReuseRuleCodeListVersion>ReuseRuleCodeListVersion0</ReuseRuleCodeListVersion><ClassificationRuleCodeListVersion>ClassificationRuleCodeListVersion0</ClassificationRuleCodeListVersion><AuthorizationReasonCodeListVersion>AuthorizationReasonCodeListVersion0</AuthorizationReasonCodeListVersion><RelationshipCodeListVersion>RelationshipCodeListVersion0</RelationshipCodeListVersion></CodeListVersions><DataObjectPackage>");
        assertThat(manifest).contains(
            "</DescriptiveMetadata><ManagementMetadata><OriginatingAgencyIdentifier>FRAN_NP_050056</OriginatingAgencyIdentifier><SubmissionAgencyIdentifier>Not Required SubmissionAgencyIdentifier</SubmissionAgencyIdentifier></ManagementMetadata></DataObjectPackage><MessageRequestIdentifier>Required MessageRequestIdentifier</MessageRequestIdentifier><AuthorizationRequestReplyIdentifier>Not Required  AuthorizationRequestReplyIdentifier</AuthorizationRequestReplyIdentifier><UnitIdentifier>Not Implemented</UnitIdentifier><ArchivalAgency><Identifier>Required ArchivalAgencyIdentifier</Identifier></ArchivalAgency><Requester><Identifier>Required RequesterIdentifier</Identifier></Requester></ArchiveDeliveryRequestReply>");
        assertThat(manifest)
            .contains("</BinaryDataObject><LogBook><Event><EventIdentifier>"); // tag for GOT logbook LFC
        assertThat(manifest).contains("<Management><LogBook><Event><EventIdentifier>"); // tag for AU logbook LFC
    }

    @Test
    @RunWithCustomExecutor
    public void should_export_without_logbook() throws Exception {
        // Given
        String SIP_OK_PHYSICAL_ARCHIVE = "integration-ingest-internal/OK_ArchivesPhysiques.zip";
        InputStream zipInputStreamSipObject = PropertiesUtils.getResourceAsStream(SIP_OK_PHYSICAL_ARCHIVE);

        final GUID ingestGUID = ingestSip(zipInputStreamSipObject,
            StatusCode.WARNING); // As FormatIdentifierMock is used, pdf is identified as Plain Text File => WARNING

        SelectMultiQuery select = new SelectMultiQuery();
        select.setQuery(QueryHelper.in(VitamFieldsHelper.operations(), ingestGUID.toString()));

        DipExportRequest dipExportRequest = new DipExportRequest(
            new DataObjectVersions(Collections.singleton("BinaryMaster")),
            select.getFinalSelect(),
            false
            // <- here without logbook
        );

        dipExportRequest.setExportType(ExportType.ArchiveDeliveryRequestReply);

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

        dipExportRequest.setExportRequestParameters(exportRequestParameters);

        // When
        String manifest = createAndExtractDip(dipExportRequest);

        // Then
        assertThat(manifest).contains("<?xml version=\"1.0\" ?><ArchiveDeliveryRequestReply");
        assertThat(manifest).contains("</ArchiveDeliveryRequestReply>");
        assertThat(manifest).contains(
            "</Date><MessageIdentifier>" + getVitamSession().getRequestId() +
                "</MessageIdentifier><ArchivalAgreement>ArchivalAgreement0</ArchivalAgreement><CodeListVersions><ReplyCodeListVersion>ReplyCodeListVersion0</ReplyCodeListVersion><MessageDigestAlgorithmCodeListVersion>MessageDigestAlgorithmCodeListVersion0</MessageDigestAlgorithmCodeListVersion><MimeTypeCodeListVersion>MimeTypeCodeListVersion0</MimeTypeCodeListVersion><EncodingCodeListVersion>EncodingCodeListVersion0</EncodingCodeListVersion><FileFormatCodeListVersion>FileFormatCodeListVersion0</FileFormatCodeListVersion><CompressionAlgorithmCodeListVersion>CompressionAlgorithmCodeListVersion0</CompressionAlgorithmCodeListVersion><DataObjectVersionCodeListVersion>DataObjectVersionCodeListVersion0</DataObjectVersionCodeListVersion><StorageRuleCodeListVersion>StorageRuleCodeListVersion0</StorageRuleCodeListVersion><AppraisalRuleCodeListVersion>AppraisalRuleCodeListVersion0</AppraisalRuleCodeListVersion><AccessRuleCodeListVersion>AccessRuleCodeListVersion0</AccessRuleCodeListVersion><DisseminationRuleCodeListVersion>DisseminationRuleCodeListVersion0</DisseminationRuleCodeListVersion><ReuseRuleCodeListVersion>ReuseRuleCodeListVersion0</ReuseRuleCodeListVersion><ClassificationRuleCodeListVersion>ClassificationRuleCodeListVersion0</ClassificationRuleCodeListVersion><AuthorizationReasonCodeListVersion>AuthorizationReasonCodeListVersion0</AuthorizationReasonCodeListVersion><RelationshipCodeListVersion>RelationshipCodeListVersion0</RelationshipCodeListVersion></CodeListVersions><DataObjectPackage>");
        assertThat(manifest).contains(
            "</DescriptiveMetadata><ManagementMetadata><OriginatingAgencyIdentifier>FRAN_NP_050056</OriginatingAgencyIdentifier><SubmissionAgencyIdentifier>FRAN_NP_050056</SubmissionAgencyIdentifier></ManagementMetadata></DataObjectPackage><MessageRequestIdentifier>" +
                ingestGUID.getId() +
                "</MessageRequestIdentifier><AuthorizationRequestReplyIdentifier>Not Required  AuthorizationRequestReplyIdentifier</AuthorizationRequestReplyIdentifier><UnitIdentifier>Not Implemented</UnitIdentifier><ArchivalAgency><Identifier>Identifier4</Identifier></ArchivalAgency><Requester><Identifier>Required RequesterIdentifier</Identifier></Requester></ArchiveDeliveryRequestReply>");

        assertThat(manifest)
            .doesNotContain("</BinaryDataObject><LogBook><Event><EventIdentifier>"); // tag for GOT logbook LFC
        assertThat(manifest).doesNotContain("<Management><LogBook><Event><EventIdentifier>"); // tag for AU logbook LFC


        // Test Transfer
        dipExportRequest.setExportType(ExportType.ArchiveTransfer);
        dipExportRequest.getExportRequestParameters()
            .setTransferRequestReplyIdentifier("Not required TransferRequestReplyIdentifier");
        manifest = createAndExtractDip(dipExportRequest);


        // Then
        assertThat(manifest).contains("<?xml version=\"1.0\" ?><ArchiveTransfer");
        assertThat(manifest).contains("</ArchiveTransfer>");
        assertThat(manifest).contains(
            "</Date><MessageIdentifier>" + getVitamSession().getRequestId() +
                "</MessageIdentifier><ArchivalAgreement>ArchivalAgreement0</ArchivalAgreement><CodeListVersions><ReplyCodeListVersion>ReplyCodeListVersion0</ReplyCodeListVersion><MessageDigestAlgorithmCodeListVersion>MessageDigestAlgorithmCodeListVersion0</MessageDigestAlgorithmCodeListVersion><MimeTypeCodeListVersion>MimeTypeCodeListVersion0</MimeTypeCodeListVersion><EncodingCodeListVersion>EncodingCodeListVersion0</EncodingCodeListVersion><FileFormatCodeListVersion>FileFormatCodeListVersion0</FileFormatCodeListVersion><CompressionAlgorithmCodeListVersion>CompressionAlgorithmCodeListVersion0</CompressionAlgorithmCodeListVersion><DataObjectVersionCodeListVersion>DataObjectVersionCodeListVersion0</DataObjectVersionCodeListVersion><StorageRuleCodeListVersion>StorageRuleCodeListVersion0</StorageRuleCodeListVersion><AppraisalRuleCodeListVersion>AppraisalRuleCodeListVersion0</AppraisalRuleCodeListVersion><AccessRuleCodeListVersion>AccessRuleCodeListVersion0</AccessRuleCodeListVersion><DisseminationRuleCodeListVersion>DisseminationRuleCodeListVersion0</DisseminationRuleCodeListVersion><ReuseRuleCodeListVersion>ReuseRuleCodeListVersion0</ReuseRuleCodeListVersion><ClassificationRuleCodeListVersion>ClassificationRuleCodeListVersion0</ClassificationRuleCodeListVersion><AuthorizationReasonCodeListVersion>AuthorizationReasonCodeListVersion0</AuthorizationReasonCodeListVersion><RelationshipCodeListVersion>RelationshipCodeListVersion0</RelationshipCodeListVersion></CodeListVersions><DataObjectPackage><DataObjectGroup");
        assertThat(manifest).contains(
            "</DescriptiveMetadata><ManagementMetadata><OriginatingAgencyIdentifier>FRAN_NP_050056</OriginatingAgencyIdentifier><SubmissionAgencyIdentifier>FRAN_NP_050056</SubmissionAgencyIdentifier></ManagementMetadata></DataObjectPackage><RelatedTransferReference>RelatedTransferReference1</RelatedTransferReference><RelatedTransferReference>RelatedTransferReference2</RelatedTransferReference><TransferRequestReplyIdentifier>Not required TransferRequestReplyIdentifier</TransferRequestReplyIdentifier><ArchivalAgency><Identifier>Identifier4</Identifier></ArchivalAgency><TransferringAgency><Identifier>VITAM</Identifier></TransferringAgency></ArchiveTransfer>");

        assertThat(manifest)
            .doesNotContain("</BinaryDataObject><LogBook><Event><EventIdentifier>"); // tag for GOT logbook LFC
        assertThat(manifest).doesNotContain("<Management><LogBook><Event><EventIdentifier>"); // tag for AU logbook LFC

        // try Ingest the Transfer SIP

        InputStream transferSipStream = getDip(getVitamSession().getRequestId());

        ingestSip(transferSipStream,
            StatusCode.OK); // As FormatIdentifierMock is used, pdf signature was modified in the first ingest. After transfer manifest and FormatIdentifierMock return the same mime type => status code OK

    }

    private String createAndExtractDip(DipExportRequest dipExportRequest)
        throws Exception {
        GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        prepareVitamSession();
        getVitamSession().setRequestId(operationGuid);
        try (AccessInternalClient client = AccessInternalClientFactory.getInstance().getClient()) {
            client.exportByUsageFilter(dipExportRequest);

            awaitForWorkflowTerminationWithStatus(operationGuid, StatusCode.OK);
            try (InputStream dip = getDip(operationGuid.getId())) {
                File dipFile = File.createTempFile("tmp", ".zip", new File(VitamConfiguration.getVitamTmpFolder()));
                IOUtils.copy(dip, new FileOutputStream(dipFile));
                ZipFile zipFile = new ZipFile(dipFile);
                ZipArchiveEntry manifest = zipFile.getEntry("manifest.xml");
                return IOUtils.toString(zipFile.getInputStream(manifest), StandardCharsets.UTF_8.name());
            }
        }
    }

    private InputStream getDip(String operationId)
        throws Exception {
        try (AccessInternalClient client = AccessInternalClientFactory.getInstance().getClient()) {
            JsonNode logbook =
                client.selectOperationById(operationId, new SelectMultiQuery().getFinalSelect()).toJsonNode()
                    .get("$results")
                    .get(0);

            String evIdProc = logbook.get("evIdProc").asText();

            return client.findExportByID(evIdProc).readEntity(InputStream.class);
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

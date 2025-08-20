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

package fr.gouv.vitam.collect.internal.core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import fr.gouv.vitam.collect.common.enums.TransactionStatus;
import fr.gouv.vitam.collect.common.exception.CollectInternalException;
import fr.gouv.vitam.collect.common.exception.CollectInternalInvalidRequestException;
import fr.gouv.vitam.collect.common.exception.CollectInternalNotFoundException;
import fr.gouv.vitam.collect.common.exception.CollectInternalServerSideException;
import fr.gouv.vitam.collect.internal.core.common.BatchStatus;
import fr.gouv.vitam.collect.internal.core.common.TransactionModel;
import fr.gouv.vitam.collect.internal.core.helpers.CollectHelper;
import fr.gouv.vitam.collect.internal.core.helpers.SipHelper;
import fr.gouv.vitam.collect.internal.core.repository.MetadataRepository;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.builder.query.InQuery;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.database.utils.ScrollSpliterator;
import fr.gouv.vitam.common.exception.ExportException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.manifest.ManifestBuilder;
import fr.gouv.vitam.common.manifest.naming.CollectFilenameResolver;
import fr.gouv.vitam.common.manifest.naming.FlatFolderResolver;
import fr.gouv.vitam.common.mapping.mapper.VitamObjectMapper;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.export.ExportRequest;
import fr.gouv.vitam.common.model.export.ExportRequestParameters;
import fr.gouv.vitam.common.model.export.ExportType;
import fr.gouv.vitam.common.model.unit.ArchiveUnitModel;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceCollectClientFactory;
import fr.gouv.vitam.workspace.common.CompressInformation;
import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.JAXBException;
import org.apache.commons.io.FileUtils;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.google.common.collect.Iterables.partition;
import static fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper.id;
import static fr.gouv.vitam.common.model.IngestWorkflowConstants.CONTENT_FOLDER;
import static fr.gouv.vitam.common.model.IngestWorkflowConstants.SEDA_FILE;

public class SipService {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(SipService.class);
    private static final String SIP_EXTENSION = ".zip";
    private static final int DEFAULT_MAX_ELEMENT_IN_QUERY = 1000;

    private final int maxElementsInQuery;
    private final WorkspaceCollectClientFactory workspaceCollectClientFactory;
    private final MetadataRepository metadataRepository;
    private final TransactionService transactionService;

    public SipService(
        WorkspaceCollectClientFactory workspaceCollectClientFactory,
        MetadataRepository metadataRepository,
        TransactionService transactionService
    ) {
        this(workspaceCollectClientFactory, metadataRepository, transactionService, DEFAULT_MAX_ELEMENT_IN_QUERY);
    }

    @VisibleForTesting
    public SipService(
        WorkspaceCollectClientFactory workspaceCollectClientFactory,
        MetadataRepository metadataRepository,
        TransactionService transactionService,
        int maxElementsInQuery
    ) {
        this.workspaceCollectClientFactory = workspaceCollectClientFactory;
        this.metadataRepository = metadataRepository;
        this.transactionService = transactionService;
        this.maxElementsInQuery = maxElementsInQuery;
    }

    public void generateSipAsync(final TransactionModel transaction) {
        VitamThreadPoolExecutor.getDefaultExecutor()
            .execute(() -> {
                try {
                    LOGGER.info(
                        "Starting background generation and validation of SIP for transaction " + transaction.getId()
                    );
                    preSipGenerationChecks(transaction);

                    generateSip(transaction);

                    transactionService.changeTransactionStatus(TransactionStatus.VALIDATED, transaction);
                } catch (Exception exception) {
                    LOGGER.error(
                        "Cannot generate SIP for transaction " +
                        transaction.getId() +
                        ". Trying to updating status to KO",
                        exception
                    );
                    tryUpdateTransactionStatusToKO(transaction, exception);
                }
            });
    }

    private void preSipGenerationChecks(TransactionModel transaction) throws CollectInternalException {
        boolean hasBatchKo =
            transaction.getBatches() != null &&
            transaction.getBatches().stream().anyMatch(batch -> BatchStatus.KO.equals(batch.getBatchStatus()));
        if (hasBatchKo) {
            throw new CollectInternalInvalidRequestException(
                "Cannot generate the SIP for the transaction " +
                transaction.getId() +
                " because it has at least one batch KO."
            );
        }

        if (transactionService.isTransactionContentEmpty(transaction.getId())) {
            throw new CollectInternalInvalidRequestException(
                "Cannot generate the SIP of an empty transaction (" + transaction.getId() + ")"
            );
        }
    }

    private void generateSip(TransactionModel transactionModel)
        throws CollectInternalException, XMLStreamException, JAXBException, IOException, InvalidCreateOperationException, InvalidParseOperationException, ExportException, ContentAddressableStorageServerException {
        File manifestFile = PropertiesUtils.fileFromTmpFolder(
            transactionModel.getId() + "-" + VitamThreadUtils.getVitamSession().getRequestId() + "-" + SEDA_FILE
        );
        try {
            generateManifest(transactionModel, manifestFile);

            saveManifestInWorkspace(transactionModel, manifestFile);

            compressSipInWorkspace(transactionModel);
        } finally {
            FileUtils.deleteQuietly(manifestFile);
        }
    }

    private void generateManifest(TransactionModel transactionModel, File manifestFile)
        throws IOException, XMLStreamException, InvalidCreateOperationException, JAXBException, InvalidParseOperationException, CollectInternalException, ExportException {
        try (
            final OutputStream outputStream = new FileOutputStream(manifestFile);
            final ManifestBuilder manifestBuilder = new ManifestBuilder(outputStream, null)
        ) {
            final ExportRequestParameters exportRequestParameters = SipHelper.buildExportRequestParameters(
                transactionModel
            );
            final ExportRequest exportRequest = SipHelper.buildExportRequest(transactionModel, exportRequestParameters);

            manifestBuilder.startDocument(
                transactionModel.getManifestContext().getMessageIdentifier(),
                ExportType.ArchiveTransfer,
                exportRequestParameters
            );

            final ListMultimap<String, String> multimap = ArrayListMultimap.create();
            final Set<String> originatingAgencies = new HashSet<>();
            final Map<String, String> ogs = new HashMap<>();

            final SelectParserMultiple parser = new SelectParserMultiple();
            parser.parse(exportRequest.getDslRequest());
            final SelectMultiQuery request = parser.getRequest();

            ScrollSpliterator<JsonNode> scrollRequest = metadataRepository.selectUnits(
                request,
                transactionModel.getId()
            );

            StreamSupport.stream(scrollRequest, false).forEach(
                item -> CollectHelper.createGraph(multimap, originatingAgencies, ogs, item)
            );

            manifestBuilder.startDataObjectPackage();
            final Select select = new Select();
            final Iterable<List<Map.Entry<String, String>>> partitions = partition(ogs.entrySet(), maxElementsInQuery);
            final Set<String> exportedObjectGroupIds = new HashSet<>();
            for (List<Map.Entry<String, String>> partition : partitions) {
                ListMultimap<String, String> unitsForObjectGroupId = partition
                    .stream()
                    .collect(
                        ArrayListMultimap::create,
                        (map, entry) -> map.put(entry.getValue(), entry.getKey()),
                        (list1, list2) -> list1.putAll(list2)
                    );
                InQuery in = QueryHelper.in(id(), partition.stream().map(Map.Entry::getValue).toArray(String[]::new));

                select.setQuery(in);

                JsonNode response = metadataRepository.selectObjectGroups(
                    select.getFinalSelect(),
                    transactionModel.getId()
                );
                ArrayNode objectGroups = (ArrayNode) response.get(RequestResponseOK.TAG_RESULTS);
                for (JsonNode object : objectGroups) {
                    final String objectGroupId = object.get(id()).textValue();
                    if (exportedObjectGroupIds.contains(objectGroupId)) continue; // Do not add object group more than once
                    final List<String> linkedUnits = unitsForObjectGroupId.get(objectGroupId);
                    manifestBuilder.writeGOT(
                        object,
                        linkedUnits.getLast(),
                        Stream.empty(),
                        FlatFolderResolver.INSTANCE,
                        CollectFilenameResolver.INSTANCE
                    );
                    exportedObjectGroupIds.add(objectGroupId);
                }
            }
            manifestBuilder.startDescriptiveMetadata();
            SelectParserMultiple initialQueryParser = new SelectParserMultiple();
            initialQueryParser.parse(exportRequest.getDslRequest());

            scrollRequest = metadataRepository.selectUnits(initialQueryParser.getRequest(), transactionModel.getId());

            StreamSupport.stream(scrollRequest, false).forEach(result -> {
                try {
                    ArchiveUnitModel archiveUnitModel = VitamObjectMapper.getDeserializationObjectMapper()
                        .treeToValue(result, ArchiveUnitModel.class);
                    manifestBuilder.writeArchiveUnit(archiveUnitModel, multimap, ogs);
                } catch (JsonProcessingException | JAXBException | DatatypeConfigurationException | ExportException e) {
                    throw new VitamRuntimeException(e);
                }
            });
            manifestBuilder.endDescriptiveMetadata();

            String archivalProfile = transactionModel.getManifestContext().getArchivalProfile();
            String submissionAgencyIdentifier = transactionModel.getManifestContext().getSubmissionAgencyIdentifier();
            String legalStatus = transactionModel.getManifestContext().getLegalStatus();
            String acquisitionInformation = transactionModel.getManifestContext().getAcquisitionInformation();
            if (submissionAgencyIdentifier == null) {
                submissionAgencyIdentifier = transactionModel.getManifestContext().getOriginatingAgencyIdentifier();
            }

            manifestBuilder.writeManagementMetadata(
                acquisitionInformation,
                legalStatus,
                transactionModel.getManifestContext().getOriginatingAgencyIdentifier(),
                submissionAgencyIdentifier,
                archivalProfile
            );
            manifestBuilder.endDataObjectPackage();

            manifestBuilder.writeFooter(ExportType.ArchiveTransfer, exportRequest.getExportRequestParameters());
            manifestBuilder.closeManifest();
        }
    }

    private void saveManifestInWorkspace(TransactionModel transactionModel, File manifestFile)
        throws IOException, ContentAddressableStorageServerException {
        LOGGER.debug("Try to push manifest to workspace...");
        try (
            WorkspaceClient workspaceClient = workspaceCollectClientFactory.getClient();
            InputStream inputStream = new FileInputStream(manifestFile)
        ) {
            workspaceClient.putObject(transactionModel.getId(), SEDA_FILE, inputStream);
            LOGGER.debug(" -> push manifest to workspace finished");
        }
    }

    private void compressSipInWorkspace(TransactionModel transactionModel) throws CollectInternalException {
        try (WorkspaceClient workspaceClient = workspaceCollectClientFactory.getClient()) {
            // compress
            CompressInformation compressInformation = new CompressInformation();
            compressInformation.getFiles().add(SEDA_FILE);
            compressInformation.getFiles().add(CONTENT_FOLDER);
            compressInformation.setOutputFile(transactionModel.getId() + SIP_EXTENSION);
            compressInformation.setOutputContainer(transactionModel.getId());
            workspaceClient.compress(transactionModel.getId(), compressInformation);
        } catch (ContentAddressableStorageException e) {
            throw new CollectInternalException(e);
        }
    }

    private void tryUpdateTransactionStatusToKO(TransactionModel transactionModel, Exception exception) {
        try {
            transactionService.changeTransactionStatus(TransactionStatus.KO, transactionModel);
        } catch (Exception e) {
            LOGGER.error("Cannot mark status to KO for transaction " + transactionModel.getId(), exception);
        }
    }

    public InputStream getIngestedFileFromWorkspace(String transactionId) throws CollectInternalException {
        LOGGER.debug("Try to get Zip from workspace...");
        try (WorkspaceClient workspaceClient = workspaceCollectClientFactory.getClient()) {
            Response response = workspaceClient.getObject(transactionId, transactionId + SIP_EXTENSION);
            return response.readEntity(InputStream.class);
        } catch (ContentAddressableStorageNotFoundException e) {
            throw new CollectInternalNotFoundException("No SIP found for transaction " + transactionId, e);
        } catch (ContentAddressableStorageServerException e) {
            throw new CollectInternalException(
                "An error occurred during access to the SIP of the transaction " + transactionId,
                e
            );
        }
    }

    public void cleanupSip(String transactionId) throws CollectInternalException {
        LOGGER.info("Deleting SIP if transaction " + transactionId + " from workspace...");
        try (WorkspaceClient workspaceClient = workspaceCollectClientFactory.getClient()) {
            workspaceClient.deleteObject(transactionId, transactionId + SIP_EXTENSION);
        } catch (ContentAddressableStorageNotFoundException ignored) {
            LOGGER.info("No SIP to delete for transaction " + transactionId);
        } catch (ContentAddressableStorageServerException e) {
            throw new CollectInternalServerSideException(
                "An error occurred during SIP cleanup for transaction " + transactionId,
                e
            );
        }
    }
}

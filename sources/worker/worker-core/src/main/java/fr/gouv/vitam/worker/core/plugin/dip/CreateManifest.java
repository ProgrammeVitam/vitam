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

package fr.gouv.vitam.worker.core.plugin.dip;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.collection.CloseableIterable;
import fr.gouv.vitam.common.collection.CloseableIteratorUtils;
import fr.gouv.vitam.common.database.builder.query.InQuery;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.query.action.SetAction;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.database.utils.ScrollSpliterator;
import fr.gouv.vitam.common.exception.ExportException;
import fr.gouv.vitam.common.exception.InternalServerException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.manifest.ManifestBuilder;
import fr.gouv.vitam.common.manifest.naming.ArchiveUnitTreeExportModel;
import fr.gouv.vitam.common.manifest.naming.FilenameResolver;
import fr.gouv.vitam.common.manifest.naming.FlatFolderResolver;
import fr.gouv.vitam.common.manifest.naming.FolderResolver;
import fr.gouv.vitam.common.manifest.naming.GuidFilenameResolver;
import fr.gouv.vitam.common.manifest.naming.OriginalFilenameResolver;
import fr.gouv.vitam.common.manifest.naming.UnitTreeFolderResolver;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.administration.DataObjectVersionType;
import fr.gouv.vitam.common.model.dip.BinarySizePlatformThreshold;
import fr.gouv.vitam.common.model.dip.BinarySizeTenantThreshold;
import fr.gouv.vitam.common.model.dip.QualifierVersion;
import fr.gouv.vitam.common.model.export.ExportRequest;
import fr.gouv.vitam.common.model.objectgroup.ObjectGroupResponse;
import fr.gouv.vitam.common.model.objectgroup.QualifiersModel;
import fr.gouv.vitam.common.model.objectgroup.VersionsModel;
import fr.gouv.vitam.common.model.unit.ArchiveUnitModel;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.common.utils.SupportedSedaVersions;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.AccessContract;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleObjectGroup;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleUnit;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.distribution.JsonLineGenericIterator;
import fr.gouv.vitam.worker.core.distribution.JsonLineWriter;
import fr.gouv.vitam.worker.core.exception.ProcessingStatusException;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.plugin.ScrollSpliteratorHelper;
import fr.gouv.vitam.worker.core.plugin.transfer.TransferReportHeader;
import fr.gouv.vitam.worker.core.plugin.transfer.TransferReportLine;
import fr.gouv.vitam.worker.core.plugin.transfer.TransferStatus;
import fr.gouv.vitam.worker.core.utils.DataObjectVersionToPatternsConvertor;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.ListUtils;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.stream.XMLStreamException;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.collect.Iterables.partition;
import static fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper.id;
import static fr.gouv.vitam.common.json.JsonHandler.unprettyPrint;
import static fr.gouv.vitam.common.mapping.mapper.VitamObjectMapper.getDeserializationObjectMapper;
import static fr.gouv.vitam.common.model.RequestResponseOK.TAG_RESULTS;
import static fr.gouv.vitam.common.model.export.ExportRequest.EXPORT_QUERY_FILE_NAME;
import static fr.gouv.vitam.common.model.export.ExportType.ArchiveTransfer;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Comparator.comparing;

/**
 * create manifest and put in on workspace
 */
public class CreateManifest extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CreateManifest.class);

    static final int MANIFEST_XML_RANK = 0;
    static final int GUID_TO_INFO_RANK = 1;
    static final int BINARIES_RANK = 2;
    static final int REPORT = 3;

    public static final String PLUGIN_NAME = "CREATE_MANIFEST";
    private static final int MAX_ELEMENT_IN_QUERY = 1000;
    private static final String REASON_FIELD = "Reason";
    private static final String JSONL_EXTENSION = ".jsonl";
    public static final String UNITS_JSONL_FILE = "units.jsonl";

    private final MetaDataClientFactory metaDataClientFactory;
    private final LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory =
        LogbookLifeCyclesClientFactory.getInstance();

    private final ObjectMapper objectMapper;

    /**
     * constructor use for plugin instantiation
     */
    @SuppressWarnings("unused")
    public CreateManifest() {
        this(MetaDataClientFactory.getInstance());
    }

    @VisibleForTesting
    CreateManifest(MetaDataClientFactory metaDataClientFactory) {
        this.metaDataClientFactory = metaDataClientFactory;
        this.objectMapper = getDeserializationObjectMapper();
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handlerIO) throws ProcessingException {
        final ItemStatus itemStatus = new ItemStatus(PLUGIN_NAME);
        File manifestFile = handlerIO.getNewLocalFile(handlerIO.getOutput(MANIFEST_XML_RANK).getPath());
        File report = handlerIO.getNewLocalFile(handlerIO.getOutput(REPORT).getPath());

        try (
            MetaDataClient client = metaDataClientFactory.getClient();
            OutputStream outputStream = new FileOutputStream(manifestFile);
            FileOutputStream fileOutputStream = new FileOutputStream(report);
            FileInputStream reportFile = new FileInputStream(report);
            BufferedOutputStream buffOut = new BufferedOutputStream(fileOutputStream);
            LogbookLifeCyclesClient logbookLifeCyclesClient = logbookLifeCyclesClientFactory.getClient();
            AdminManagementClient adminManagementClient = AdminManagementClientFactory.getInstance().getClient()
        ) {
            ExportRequest exportRequest = JsonHandler.getFromJsonNode(
                handlerIO.getJsonFromWorkspace(EXPORT_QUERY_FILE_NAME),
                ExportRequest.class
            );

            // Get Wanted Seda Version
            Optional<SupportedSedaVersions> sedaVersionForExport =
                SupportedSedaVersions.getSupportedSedaVersionByVersion(exportRequest.getSedaVersion());
            if (sedaVersionForExport.isEmpty()) {
                itemStatus.increment(StatusCode.KO);
                ObjectNode infoNode = JsonHandler.createObjectNode();
                infoNode.put(REASON_FIELD, "The wanted seda version is not valid !");
                String evdev = JsonHandler.unprettyPrint(infoNode);
                itemStatus.setEvDetailData(evdev);
                return itemStatus;
            }

            final String sedaVersionToExport = sedaVersionForExport.get().getVersion();

            ManifestBuilder manifestBuilder = new ManifestBuilder(outputStream, sedaVersionForExport.get());

            TransferReportHeader reportHeader = new TransferReportHeader(exportRequest.getDslRequest());

            switch (exportRequest.getExportType()) {
                case ArchiveDeliveryRequestReply:
                    manifestBuilder.validate(exportRequest.getExportType(), exportRequest.getExportRequestParameters());
                    break;
                case ArchiveTransfer:
                    buffOut.write(unprettyPrint(JsonHandler.createObjectNode()).getBytes(UTF_8)); // header empty
                    buffOut.write(System.lineSeparator().getBytes(StandardCharsets.UTF_8));
                    buffOut.write(unprettyPrint(reportHeader).getBytes(StandardCharsets.UTF_8)); // context
                    buffOut.write(System.lineSeparator().getBytes(StandardCharsets.UTF_8));
                    manifestBuilder.validate(exportRequest.getExportType(), exportRequest.getExportRequestParameters());
                    break;
                default:
                    break;
            }

            // Write manifest first line information
            manifestBuilder.startDocument(
                param.getContainerName(),
                exportRequest.getExportType(),
                exportRequest.getExportRequestParameters()
            );

            ListMultimap<String, String> unitIdToChildUnitIds = ArrayListMultimap.create();
            Set<String> originatingAgencies = new HashSet<>();

            File unitsJsonlFile = exportUnits(handlerIO, exportRequest, client);

            String originatingAgency = VitamConfiguration.getDefaultOriginatingAgencyForExport(
                ParameterHelper.getTenantParameter()
            );
            Map<String, String> unitIdToObjectGroupId = new HashMap<>();
            try (
                CloseableIterable<ArchiveUnitExportGraphModel> units = listUnits(
                    unitsJsonlFile,
                    new TypeReference<>() {}
                )
            ) {
                for (ArchiveUnitExportGraphModel unitModel : units) {
                    prepareGraphCreation(
                        unitIdToChildUnitIds,
                        originatingAgencies,
                        unitIdToObjectGroupId,
                        unitModel,
                        sedaVersionToExport
                    );
                }
            }

            if (originatingAgencies.size() == 1) {
                originatingAgency = Iterables.getOnlyElement(originatingAgencies);
            }

            manifestBuilder.startDataObjectPackage();

            Select select = new Select();

            Map<String, JsonNode> idBinaryWithFileName = new HashMap<>();
            Set<String> exportedObjectGroupIds = new HashSet<>();
            boolean exportWithLogBookLFC = exportRequest.isExportWithLogBookLFC();
            boolean exportWithoutObjects = exportRequest.isExportWithoutObjects();
            boolean useOriginalFilenames = exportRequest.isUseOriginalFilenames();
            boolean exportWithTree = exportRequest.isExportWithTree();

            FolderResolver folderResolver = createFolderResolver(unitsJsonlFile, exportWithTree);
            FilenameResolver filenameResolver = createFilenameResolver(useOriginalFilenames);

            final Map<DataObjectVersionType, Set<QualifierVersion>> dataObjectVersions =
                DataObjectVersionToPatternsConvertor.computeDataObjectVersionsPatterns(
                    exportRequest.getDataObjectVersionToExport()
                );

            long exportSize = 0;

            final AccessContractModel accessContractModel = getAccessContractModel(adminManagementClient);

            Iterable<List<Entry<String, String>>> partitions = partition(
                unitIdToObjectGroupId.entrySet(),
                MAX_ELEMENT_IN_QUERY
            );
            for (List<Entry<String, String>> partition : partitions) {
                ListMultimap<String, String> unitsForObjectGroupId = partition
                    .stream()
                    .collect(
                        ArrayListMultimap::create,
                        (map, entry) -> map.put(entry.getValue(), entry.getKey()),
                        (list1, list2) -> list1.putAll(list2)
                    );

                InQuery in = QueryHelper.in(id(), unitsForObjectGroupId.keySet().toArray(String[]::new));
                select.setQuery(in);
                JsonNode response = client.selectObjectGroups(select.getFinalSelect());
                ArrayNode objectGroups = (ArrayNode) response.get(TAG_RESULTS);
                for (JsonNode object : objectGroups) {
                    String objectGroupId = object.get(id()).textValue();
                    List<String> parentUnits = unitsForObjectGroupId.get(objectGroupId);
                    JsonNode selectObjectGroupLifeCycleById = logbookLifeCyclesClient.selectObjectGroupLifeCycleById(
                        objectGroupId,
                        new Select().getFinalSelect()
                    );

                    ObjectGroupResponse objectGroup = objectMapper.treeToValue(object, ObjectGroupResponse.class);
                    keepOnlySelectedQualifiers(
                        objectGroup,
                        dataObjectVersions,
                        accessContractModel,
                        exportWithoutObjects
                    );

                    if (objectGroup.getQualifiers().isEmpty()) {
                        continue;
                    }

                    JsonNode currentObject = JsonHandler.toJsonNode(objectGroup);
                    Stream<LogbookLifeCycleObjectGroup> logbookLifeCycleObjectGroupStream = exportWithLogBookLFC
                        ? RequestResponseOK.getFromJsonNode(selectObjectGroupLifeCycleById)
                            .getResults()
                            .stream()
                            .map(LogbookLifeCycleObjectGroup::new)
                        : Stream.empty();

                    idBinaryWithFileName.putAll(
                        manifestBuilder.writeGOT(
                            currentObject,
                            parentUnits.get(parentUnits.size() - 1),
                            logbookLifeCycleObjectGroupStream,
                            folderResolver,
                            filenameResolver
                        )
                    );

                    exportSize +=
                    objectGroup
                        .getQualifiers()
                        .stream()
                        .map(QualifiersModel::getVersions)
                        .flatMap(Collection::stream)
                        .mapToLong(VersionsModel::getSize)
                        .sum();
                    exportedObjectGroupIds.add(objectGroupId);
                }
            }

            // If we export no GOT, we exclude object groups
            final Map<String, String> filteredOgs = unitIdToObjectGroupId
                .entrySet()
                .stream()
                .filter(entry -> exportedObjectGroupIds.contains(entry.getValue()))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

            manifestBuilder.startDescriptiveMetadata();

            try (
                CloseableIterable<ArchiveUnitModel> archiveUnitModels = listUnits(
                    unitsJsonlFile,
                    new TypeReference<>() {}
                )
            ) {
                for (ArchiveUnitModel archiveUnitModel : archiveUnitModels) {
                    final JsonNode response = exportWithLogBookLFC
                        ? logbookLifeCyclesClient.selectUnitLifeCycleById(
                            archiveUnitModel.getId(),
                            select.getFinalSelect()
                        )
                        : null;
                    final LogbookLifeCycleUnit logbookLFC = Optional.ofNullable(response)
                        .map(r -> r.get(TAG_RESULTS))
                        .map(node -> node.get(0))
                        .map(LogbookLifeCycleUnit::new)
                        .orElse(null);

                    manifestBuilder.writeArchiveUnitWithLFC(
                        archiveUnitModel,
                        unitIdToChildUnitIds,
                        filteredOgs,
                        logbookLFC
                    );

                    if (ArchiveTransfer.equals(exportRequest.getExportType())) {
                        List<String> opts = ListUtils.defaultIfNull(archiveUnitModel.getOpts(), new ArrayList<>());
                        TransferStatus status = opts.isEmpty() ? TransferStatus.OK : TransferStatus.ALREADY_IN_TRANSFER;
                        opts.add(param.getContainerName());
                        ObjectNode updateMultiQuery = getUpdateQuery(opts);

                        if (TransferStatus.ALREADY_IN_TRANSFER.equals(status)) {
                            itemStatus.increment(StatusCode.WARNING);
                            ObjectNode infoNode = JsonHandler.createObjectNode();
                            infoNode.put(
                                REASON_FIELD,
                                String.format("unit %s already in transfer", archiveUnitModel.getId())
                            );
                            String evDetData = JsonHandler.unprettyPrint(infoNode);
                            itemStatus.setEvDetailData(evDetData);
                        }

                        client.updateUnitById(updateMultiQuery, archiveUnitModel.getId());
                        TransferReportLine reportLine = new TransferReportLine(archiveUnitModel.getId(), status);
                        buffOut.write(unprettyPrint(reportLine).getBytes(StandardCharsets.UTF_8));
                        buffOut.write(System.lineSeparator().getBytes(StandardCharsets.UTF_8));
                    }
                }
            }

            buffOut.flush();
            manifestBuilder.endDescriptiveMetadata();

            switch (exportRequest.getExportType()) {
                case ArchiveTransfer:
                    originatingAgency = exportRequest.getExportRequestParameters().getOriginatingAgencyIdentifier();
                    break;
                case ArchiveDeliveryRequestReply:
                    if (Strings.isNullOrEmpty(originatingAgency)) {
                        originatingAgency = exportRequest.getExportRequestParameters().getOriginatingAgencyIdentifier();
                    }
                    break;
                default:
                    break;
            }

            String submissionAgencyIdentifier = exportRequest.getExportRequestParameters() != null
                ? exportRequest.getExportRequestParameters().getSubmissionAgencyIdentifier()
                : null;
            manifestBuilder.writeManagementMetadata(originatingAgency, submissionAgencyIdentifier);

            manifestBuilder.endDataObjectPackage();

            switch (exportRequest.getExportType()) {
                case ArchiveDeliveryRequestReply:
                case ArchiveTransfer:
                    if (Strings.isNullOrEmpty(exportRequest.getExportRequestParameters().getTransferringAgency())) {
                        exportRequest
                            .getExportRequestParameters()
                            .setTransferringAgency(VitamConfiguration.getVitamDefaultTransferringAgency());
                    }
                    manifestBuilder.writeFooter(
                        exportRequest.getExportType(),
                        exportRequest.getExportRequestParameters()
                    );
                    break;
                default:
                    break;
            }

            manifestBuilder.closeManifest();

            exportSize += manifestFile.length();
            int tenant = VitamThreadUtils.getVitamSession().getTenantId();
            long threshold = retrieveRelevantThreshold(exportRequest.getMaxSizeThreshold(), tenant);
            checkSize(itemStatus, exportSize, threshold, tenant);

            storeBinaryInformationOnWorkspace(handlerIO, idBinaryWithFileName);

            handlerIO.addOutputResult(MANIFEST_XML_RANK, manifestFile, true, false);

            itemStatus.increment(StatusCode.OK);

            if (ArchiveTransfer.equals(exportRequest.getExportType())) {
                handlerIO.transferInputStreamToWorkspace(
                    handlerIO.getContainerName() + JSONL_EXTENSION,
                    reportFile,
                    null,
                    false
                );
            }
            return new ItemStatus(PLUGIN_NAME).setItemsStatus(PLUGIN_NAME, itemStatus);
        } catch (ExportException e) {
            LOGGER.error(String.format("Export failed with message [%s]", e.getMessage()), e);
            ObjectNode infoNode = JsonHandler.createObjectNode();
            infoNode.put(REASON_FIELD, e.getMessage());
            return buildItemStatus(PLUGIN_NAME, StatusCode.KO, infoNode);
        } catch (
            IOException
            | MetaDataExecutionException
            | InvalidCreateOperationException
            | MetaDataClientServerException
            | XMLStreamException
            | JAXBException
            | LogbookClientException
            | MetaDataDocumentSizeException
            | InvalidParseOperationException
            | InternalServerException
            | MetaDataNotFoundException
            | DatatypeConfigurationException e
        ) {
            throw new ProcessingException(e);
        } catch (ProcessingStatusException e) {
            LOGGER.error(String.format("Report generation failed with status [%s]", e.getStatusCode()), e);
            return buildItemStatus(PLUGIN_NAME, e.getStatusCode(), e.getEventDetails());
        }
    }

    private File exportUnits(HandlerIO handlerIO, ExportRequest exportRequest, MetaDataClient client)
        throws IOException, InvalidParseOperationException, ProcessingStatusException {
        File unitsJsonl = handlerIO.getNewLocalFile(UNITS_JSONL_FILE);
        try (JsonLineWriter unitsJsonlWriter = new JsonLineWriter(new FileOutputStream(unitsJsonl))) {
            SelectParserMultiple parser = new SelectParserMultiple();
            parser.parse(exportRequest.getDslRequest());
            SelectMultiQuery request = parser.getRequest();
            request.resetUsedProjection();
            ScrollSpliterator<JsonNode> unitsToExportScrollSpliterator =
                ScrollSpliteratorHelper.createUnitScrollSplitIterator(client, request);

            checkEmptinessSelectedUnits(unitsToExportScrollSpliterator.estimateSize());

            for (JsonNode unit : unitsToExportScrollSpliterator) {
                unitsJsonlWriter.addEntry(unit);
            }
        }
        return unitsJsonl;
    }

    private <T> CloseableIterable<T> listUnits(File unitsJsonl, TypeReference<T> typeReference) throws IOException {
        return CloseableIteratorUtils.toCloseableIterable(
            new JsonLineGenericIterator<>(new FileInputStream(unitsJsonl), typeReference)
        );
    }

    private void keepOnlySelectedQualifiers(
        ObjectGroupResponse objectGroup,
        Map<DataObjectVersionType, Set<QualifierVersion>> dataObjectVersions,
        AccessContractModel accessContract,
        boolean exportWithoutObjects
    ) {
        final List<QualifiersModel> qualifiers = objectGroup.getQualifiers();

        if (Boolean.FALSE.equals(accessContract.getEveryDataObjectVersion())) {
            qualifiers.removeIf(qualifier -> !accessContract.getDataObjectVersion().contains(qualifier.getQualifier()));
        }

        if (exportWithoutObjects) {
            qualifiers.clear();
        } else if (!dataObjectVersions.isEmpty()) {
            qualifiers.removeIf(
                qualifier -> !dataObjectVersions.containsKey(DataObjectVersionType.fromName(qualifier.getQualifier()))
            );

            qualifiers.forEach(qualifier -> {
                final List<VersionsModel> versionsToExport = dataObjectVersions
                    .getOrDefault(DataObjectVersionType.fromName(qualifier.getQualifier()), Collections.emptySet())
                    .stream()
                    .flatMap(version -> getVersions(qualifier, version).stream())
                    .distinct()
                    .sorted(comparing(VersionsModel::getVersion))
                    .toList();
                qualifier.setVersions(versionsToExport);
            });
        }
    }

    private List<VersionsModel> getVersions(QualifiersModel qualifier, QualifierVersion wantedVersion) {
        switch (wantedVersion) {
            case FIRST:
                return getFirstVersion(qualifier).map(List::of).orElse(Collections.emptyList());
            case LAST:
                return getLastVersion(qualifier).map(List::of).orElse(Collections.emptyList());
            case ALL:
                return qualifier.getVersions();
            default:
                return Collections.emptyList();
        }
    }

    private AccessContractModel getAccessContractModel(AdminManagementClient adminManagementClient)
        throws ProcessingException {
        AccessContractModel accessContractModel = VitamThreadUtils.getVitamSession().getContract();

        if (accessContractModel == null) {
            Select accessContractSelect = new Select();

            try {
                Query query = QueryHelper.eq(
                    AccessContract.IDENTIFIER,
                    VitamThreadUtils.getVitamSession().getContractId()
                );
                accessContractSelect.setQuery(query);
                accessContractModel = ((RequestResponseOK<
                            AccessContractModel
                        >) adminManagementClient.findAccessContracts(
                        accessContractSelect.getFinalSelect()
                    )).getResults()
                    .get(0);
            } catch (
                InvalidCreateOperationException
                | AdminManagementClientServerException
                | InvalidParseOperationException e
            ) {
                throw new ProcessingException(e.getMessage(), e.getCause());
            }
        }
        return accessContractModel;
    }

    private void checkSize(ItemStatus itemStatus, long exportSize, long threshold, int tenant) throws ExportException {
        if (exportSize > threshold) {
            throw new ExportException(
                String.format(
                    "export size exceeds threshold. \n Export Size = [%d]\n Threshold = [%d]",
                    exportSize,
                    threshold
                )
            );
        }
        Optional<BinarySizeTenantThreshold> first = VitamConfiguration.getBinarySizeTenantThreshold()
            .stream()
            .filter(e -> e.getTenant() == tenant)
            .findFirst();
        if (first.isPresent()) {
            if (exportSize > first.get().getThreshold()) {
                if (first.get().isAuthorized()) {
                    updateItemStatus(
                        itemStatus,
                        exportSize,
                        threshold,
                        "export size exceeds tenant threshold.\n Export Size = [%d]\n Tenant Threshold = [%d]"
                    );
                } else {
                    throw new ExportException(
                        String.format(
                            "export size exceeds tenant threshold. \n Export Size = [%d]\n Tenant Threshold = [%d]",
                            exportSize,
                            threshold
                        )
                    );
                }
            }
        } else {
            if (exportSize > VitamConfiguration.getBinarySizePlatformThreshold().getThreshold()) {
                updateItemStatus(
                    itemStatus,
                    exportSize,
                    threshold,
                    "export size exceeds platform threshold.\n Export Size = [%d]\n Platform Threshold = [%d]"
                );
            }
        }
    }

    private void updateItemStatus(ItemStatus itemStatus, long exportSize, long threshold, String s) {
        itemStatus.increment(StatusCode.WARNING);
        ObjectNode infoNode = JsonHandler.createObjectNode();
        infoNode.put(REASON_FIELD, String.format(s, exportSize, threshold));
        String evDetData = JsonHandler.unprettyPrint(infoNode);
        itemStatus.setEvDetailData(evDetData);
    }

    private long retrieveRelevantThreshold(Long threshold, int tenant) {
        if (threshold != null) {
            return threshold;
        }
        return VitamConfiguration.getBinarySizeTenantThreshold()
            .stream()
            .filter(e -> e.getTenant() == tenant)
            .findFirst()
            .map(BinarySizePlatformThreshold::getThreshold)
            .orElseGet(() -> VitamConfiguration.getBinarySizePlatformThreshold().getThreshold());
    }

    private Optional<VersionsModel> getLastVersion(QualifiersModel qualifier) {
        return qualifier.getVersions().stream().max(comparing(VersionsModel::getVersion));
    }

    private Optional<VersionsModel> getFirstVersion(QualifiersModel qualifier) {
        return qualifier.getVersions().stream().min(comparing(VersionsModel::getVersion));
    }

    private void checkEmptinessSelectedUnits(long total) throws ProcessingStatusException {
        if (total == 0) {
            ObjectNode infoNode = JsonHandler.createObjectNode();
            infoNode.put(REASON_FIELD, "the DSL query has no result");
            throw new ProcessingStatusException(StatusCode.KO, infoNode, "Empty result list");
        }
    }

    private void prepareGraphCreation(
        ListMultimap<String, String> unitIdToChildUnitIds,
        Set<String> originatingAgencies,
        Map<String, String> unitIdToObjectGroupId,
        ArchiveUnitExportGraphModel unit,
        String sedaVersionToExport
    ) throws ExportException {
        if (!SupportedSedaVersions.isSedaVersionsCompatible(unit.getSedaVersion(), sedaVersionToExport)) {
            final String errorMsg =
                "Incompatible seda version to export (" + sedaVersionToExport + ") for unit : " + unit.getId();
            throw new ExportException(errorMsg);
        }
        for (String parentUnitId : unit.getParentUnitIds()) {
            unitIdToChildUnitIds.put(parentUnitId, unit.getId());
        }
        if (unit.getOriginatingAgency() != null) {
            originatingAgencies.add(unit.getOriginatingAgency());
        }
        if (unit.getObjectGroupId() != null) {
            unitIdToObjectGroupId.put(unit.getId(), unit.getObjectGroupId());
        }
    }

    private void storeBinaryInformationOnWorkspace(HandlerIO handlerIO, Map<String, JsonNode> maps)
        throws ProcessingException {
        File guidToInfo = handlerIO.getNewLocalFile(handlerIO.getOutput(GUID_TO_INFO_RANK).getPath());
        File binaryListFile = handlerIO.getNewLocalFile(handlerIO.getOutput(BINARIES_RANK).getPath());

        try {
            JsonHandler.writeAsFile(maps, guidToInfo);
            JsonHandler.writeAsFile(maps.keySet(), binaryListFile);
        } catch (InvalidParseOperationException e) {
            throw new ProcessingException(e);
        }

        // put file in workspace
        handlerIO.addOutputResult(GUID_TO_INFO_RANK, guidToInfo, true, false);
        handlerIO.addOutputResult(BINARIES_RANK, binaryListFile, true, false);
    }

    private ObjectNode getUpdateQuery(List<String> opts)
        throws InvalidParseOperationException, InvalidCreateOperationException {
        Map<String, JsonNode> action = new HashMap<>();
        UpdateMultiQuery updateMultiQuery = new UpdateMultiQuery();
        action.put(VitamFieldsHelper.opts(), JsonHandler.toJsonNode(opts));

        SetAction setOPTS = new SetAction(action);

        updateMultiQuery.addActions(setOPTS);
        return updateMultiQuery.getFinalUpdateById();
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // TODO: add check on file listUnit.json.
    }

    private FolderResolver createFolderResolver(File unitsJsonl, boolean exportUnitTree)
        throws ExportException, IOException {
        if (!exportUnitTree) {
            return FlatFolderResolver.INSTANCE;
        }
        try (CloseableIterable<ArchiveUnitTreeExportModel> units = listUnits(unitsJsonl, new TypeReference<>() {})) {
            return new UnitTreeFolderResolver(IterableUtils.toList(units));
        }
    }

    private static FilenameResolver createFilenameResolver(boolean useOriginalFilenames) {
        if (!useOriginalFilenames) {
            return GuidFilenameResolver.INSTANCE;
        }
        return new OriginalFilenameResolver();
    }
}

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
import fr.gouv.vitam.common.exception.InternalServerException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.manifest.ExportException;
import fr.gouv.vitam.common.manifest.ManifestBuilder;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.dip.BinarySizePlatformThreshold;
import fr.gouv.vitam.common.model.dip.BinarySizeTenantThreshold;
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
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.plugin.ScrollSpliteratorHelper;
import fr.gouv.vitam.worker.core.plugin.transfer.TransferReportHeader;
import fr.gouv.vitam.worker.core.plugin.transfer.TransferReportLine;
import fr.gouv.vitam.worker.core.plugin.transfer.TransferStatus;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;

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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.google.common.collect.Iterables.partition;
import static fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper.id;
import static fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper.sedaVersion;
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTION.FIELDS;
import static fr.gouv.vitam.common.database.parser.query.ParserTokens.PROJECTIONARGS.ID;
import static fr.gouv.vitam.common.database.parser.query.ParserTokens.PROJECTIONARGS.OBJECT;
import static fr.gouv.vitam.common.database.parser.query.ParserTokens.PROJECTIONARGS.ORIGINATING_AGENCY;
import static fr.gouv.vitam.common.database.parser.query.ParserTokens.PROJECTIONARGS.SEDAVERSION;
import static fr.gouv.vitam.common.database.parser.query.ParserTokens.PROJECTIONARGS.UNITUPS;
import static fr.gouv.vitam.common.json.JsonHandler.unprettyPrint;
import static fr.gouv.vitam.common.mapping.mapper.VitamObjectMapper.buildDeserializationObjectMapper;
import static fr.gouv.vitam.common.model.RequestResponseOK.TAG_RESULTS;
import static fr.gouv.vitam.common.model.export.ExportRequest.EXPORT_QUERY_FILE_NAME;
import static fr.gouv.vitam.common.model.export.ExportType.ArchiveTransfer;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * create manifest and put in on workspace
 */
public class CreateManifest extends ActionHandler {
    static final int MANIFEST_XML_RANK = 0;
    static final int GUID_TO_INFO_RANK = 1;
    static final int BINARIES_RANK = 2;
    static final int REPORT = 3;

    public static final String PLUGIN_NAME = "CREATE_MANIFEST";
    private static final int MAX_ELEMENT_IN_QUERY = 1000;
    private static final String REASON_FIELD = "Reason";
    private static final String JSONL_EXTENSION = ".jsonl";

    private final MetaDataClientFactory metaDataClientFactory;
    private LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory =
        LogbookLifeCyclesClientFactory.getInstance();

    private final ObjectNode projection;
    private final ObjectMapper objectMapper;


    /**
     * constructor use for plugin instantiation
     */
    public CreateManifest() {
        this(MetaDataClientFactory.getInstance());
    }

    @VisibleForTesting
    CreateManifest(MetaDataClientFactory metaDataClientFactory) {
        this.metaDataClientFactory = metaDataClientFactory;

        ObjectNode fields = JsonHandler.createObjectNode();
        fields.put(UNITUPS.exactToken(), 1);
        fields.put(ID.exactToken(), 1);
        fields.put(ORIGINATING_AGENCY.exactToken(), 1);
        fields.put(OBJECT.exactToken(), 1);
        fields.put(SEDAVERSION.exactToken(), 1);

        this.projection = JsonHandler.createObjectNode();
        this.projection.set(FIELDS.exactToken(), fields);
        this.objectMapper = buildDeserializationObjectMapper();
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handlerIO) throws ProcessingException {
        final ItemStatus itemStatus = new ItemStatus(PLUGIN_NAME);
        File manifestFile = handlerIO.getNewLocalFile(handlerIO.getOutput(MANIFEST_XML_RANK).getPath());
        File report = handlerIO.getNewLocalFile(handlerIO.getOutput(REPORT).getPath());

        try (MetaDataClient client = metaDataClientFactory.getClient();
            OutputStream outputStream = new FileOutputStream(manifestFile);
            FileOutputStream fileOutputStream = new FileOutputStream(report);
            FileInputStream reportFile = new FileInputStream(report);
            BufferedOutputStream buffOut = new BufferedOutputStream(fileOutputStream);
            LogbookLifeCyclesClient logbookLifeCyclesClient = logbookLifeCyclesClientFactory.getClient();
            ManifestBuilder manifestBuilder = new ManifestBuilder(outputStream);
            AdminManagementClient adminManagementClient = AdminManagementClientFactory.getInstance().getClient()) {

            ExportRequest exportRequest = JsonHandler
                .getFromJsonNode(handlerIO.getJsonFromWorkspace(EXPORT_QUERY_FILE_NAME), ExportRequest.class);
            TransferReportHeader reportHeader = new TransferReportHeader(exportRequest.getDslRequest());

            switch (exportRequest.getExportType()) {
                case ArchiveDeliveryRequestReply:
                    manifestBuilder.validate(exportRequest.getExportType(), exportRequest.getExportRequestParameters());
                    break;
                case ArchiveTransfer:
                    buffOut.write(unprettyPrint(JsonHandler.createObjectNode()).getBytes(UTF_8)); // header empty
                    buffOut.write(System.lineSeparator().getBytes(StandardCharsets.UTF_8));
                    buffOut.write(unprettyPrint(reportHeader).getBytes(StandardCharsets.UTF_8));  // context
                    buffOut.write(System.lineSeparator().getBytes(StandardCharsets.UTF_8));
                    manifestBuilder.validate(exportRequest.getExportType(), exportRequest.getExportRequestParameters());
                    break;
                default:
                    break;
            }

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

            // Write manifest first line information
            manifestBuilder.startDocument(param.getContainerName(), exportRequest.getExportType(),
                exportRequest.getExportRequestParameters(), sedaVersionForExport.get());


            ListMultimap<String, String> multimap = ArrayListMultimap.create();
            Set<String> originatingAgencies = new HashSet<>();
            String originatingAgency =
                VitamConfiguration.getDefaultOriginatingAgencyForExport(
                    ParameterHelper.getTenantParameter());
            Map<String, String> ogs = new HashMap<>();

            SelectParserMultiple parser = new SelectParserMultiple();
            parser.parse(exportRequest.getDslRequest());

            SelectMultiQuery request = parser.getRequest();
            request.setProjection(projection);

            ScrollSpliterator<JsonNode> scrollRequest = ScrollSpliteratorHelper
                .createUnitScrollSplitIterator(client, request);

            for (JsonNode item : StreamSupport.stream(scrollRequest, false).collect(Collectors.toList())) {
                prepareGraphCreation(multimap, originatingAgencies, ogs, item, sedaVersionToExport);
            }

            if (checkEmptinessSelectedUnits(itemStatus, scrollRequest.estimateSize())) {
                return new ItemStatus(PLUGIN_NAME).setItemsStatus(PLUGIN_NAME, itemStatus);
            }

            if (originatingAgencies.size() == 1) {
                originatingAgency = Iterables.getOnlyElement(originatingAgencies);
            }

            manifestBuilder.startDataObjectPackage();

            Select select = new Select();

            Map<String, JsonNode> idBinaryWithFileName = new HashMap<>();
            boolean exportWithLogBookLFC = exportRequest.isExportWithLogBookLFC();
            Set<String> dataObjectVersions = Objects.nonNull(exportRequest.getDataObjectVersionToExport())
                ? exportRequest.getDataObjectVersionToExport().getDataObjectVersions()
                : Collections.emptySet();

            long exportSize = 0;

            Iterable<List<Entry<String, String>>> partitions = partition(ogs.entrySet(), MAX_ELEMENT_IN_QUERY);
            for (List<Entry<String, String>> partition : partitions) {

                ListMultimap<String, String> unitsForObjectGroupId = partition.stream()
                    .collect(
                        ArrayListMultimap::create,
                        (map, entry) -> map.put(entry.getValue(), entry.getKey()),
                        (list1, list2) -> list1.putAll(list2)
                    );

                InQuery in = QueryHelper.in(id(), partition.stream().map(Entry::getValue).toArray(String[]::new));

                select.setQuery(in);
                JsonNode response = client.selectObjectGroups(select.getFinalSelect());
                ArrayNode objects = (ArrayNode) response.get(TAG_RESULTS);

                for (JsonNode object : objects) {
                    String id = object.get(id()).textValue();
                    List<String> linkedUnits = unitsForObjectGroupId.get(id);
                    JsonNode selectObjectGroupLifeCycleById =
                        logbookLifeCyclesClient.selectObjectGroupLifeCycleById(id, new Select().getFinalSelect());

                    AccessContractModel accessContract = getAccessContractModel(adminManagementClient);

                    ObjectGroupResponse objectGroup = objectMapper.treeToValue(object, ObjectGroupResponse.class);

                    List<QualifiersModel> qualifiersToRemove;
                    if (Boolean.FALSE.equals(accessContract.isEveryDataObjectVersion())) {
                        qualifiersToRemove = objectGroup.getQualifiers().stream()
                            .filter(
                                qualifier -> !accessContract.getDataObjectVersion().contains(qualifier.getQualifier()))
                            .collect(Collectors.toList());
                        objectGroup.getQualifiers().removeAll(qualifiersToRemove);
                    }

                    if (!dataObjectVersions.isEmpty()) {
                        qualifiersToRemove = objectGroup.getQualifiers().stream()
                            .filter(qualifier -> !dataObjectVersions.contains(qualifier.getQualifier()))
                            .collect(Collectors.toList());
                        objectGroup.getQualifiers().removeAll(qualifiersToRemove);
                    }

                    JsonNode currentObject = JsonHandler.toJsonNode(objectGroup);

                    Stream<LogbookLifeCycleObjectGroup> logbookLifeCycleObjectGroupStream =
                        (exportRequest.isExportWithLogBookLFC()) ?
                            RequestResponseOK.getFromJsonNode(selectObjectGroupLifeCycleById)
                                .getResults()
                                .stream()
                                .map(LogbookLifeCycleObjectGroup::new) :
                            Stream.empty();

                    idBinaryWithFileName.putAll(manifestBuilder
                        .writeGOT(currentObject, linkedUnits.get(linkedUnits.size() - 1),
                            logbookLifeCycleObjectGroupStream));
                    exportSize += computeSize(currentObject, dataObjectVersions);
                }
            }

            SelectParserMultiple initialQueryParser = new SelectParserMultiple();
            initialQueryParser.parse(exportRequest.getDslRequest());

            scrollRequest = new ScrollSpliterator<>(initialQueryParser.getRequest(),
                query -> {
                    try {
                        JsonNode node = client.selectUnits(query.getFinalSelect());
                        return RequestResponseOK.getFromJsonNode(node);
                    } catch (MetaDataExecutionException | MetaDataDocumentSizeException |
                             MetaDataClientServerException | InvalidParseOperationException e) {
                        throw new IllegalStateException(e);
                    }
                }, VitamConfiguration.getElasticSearchScrollTimeoutInMilliseconds(),
                VitamConfiguration.getElasticSearchScrollLimit());

            manifestBuilder.startDescriptiveMetadata();
            StreamSupport.stream(scrollRequest, false)
                .forEach(result -> {
                    try {
                        ArchiveUnitModel unit;
                        ArchiveUnitModel archiveUnitModel = objectMapper.treeToValue(result, ArchiveUnitModel.class);

                        final JsonNode response =
                            exportWithLogBookLFC ?
                                logbookLifeCyclesClient.selectUnitLifeCycleById(archiveUnitModel.getId(),
                                    select.getFinalSelect()) :
                                null;
                        final LogbookLifeCycleUnit logbookLFC = Optional.ofNullable(response)
                            .map(r -> r.get(TAG_RESULTS))
                            .map(node -> node.get(0))
                            .map(LogbookLifeCycleUnit::new)
                            .orElse(null);
                        // If we export no GOT, we exclude object groups
                        final Map<String, String> filteredOgs = MapUtils.isEmpty(idBinaryWithFileName) ? Map.of() : ogs;
                        unit = manifestBuilder.writeArchiveUnitWithLFC(archiveUnitModel, multimap, filteredOgs,
                            logbookLFC);

                        if (ArchiveTransfer.equals(exportRequest.getExportType())) {
                            List<String> opts = ListUtils.defaultIfNull(unit.getOpts(), new ArrayList<>());
                            TransferStatus status = opts.isEmpty() ?
                                TransferStatus.OK :
                                TransferStatus.ALREADY_IN_TRANSFER;
                            opts.add(param.getContainerName());
                            ObjectNode updateMultiQuery = getUpdateQuery(opts);

                            if (TransferStatus.ALREADY_IN_TRANSFER.equals(status)) {
                                itemStatus.increment(StatusCode.WARNING);
                                ObjectNode infoNode = JsonHandler.createObjectNode();
                                infoNode.put(REASON_FIELD, String.format("unit %s already in transfer", unit.getId()));
                                String evDetData = JsonHandler.unprettyPrint(infoNode);
                                itemStatus.setEvDetailData(evDetData);
                            }

                            client.updateUnitById(updateMultiQuery, unit.getId());
                            TransferReportLine reportLine = new TransferReportLine(unit.getId(), status);
                            buffOut.write(unprettyPrint(reportLine).getBytes(StandardCharsets.UTF_8));
                            buffOut.write(System.lineSeparator().getBytes(StandardCharsets.UTF_8));
                        }
                    } catch (JAXBException | DatatypeConfigurationException | IOException |
                             InvalidParseOperationException | InvalidCreateOperationException |
                             MetaDataNotFoundException | MetaDataExecutionException | MetaDataDocumentSizeException |
                             MetaDataClientServerException | LogbookClientException e) {
                        throw new IllegalArgumentException(e);
                    }
                });
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

            String submissionAgencyIdentifier = exportRequest.getExportRequestParameters() != null ?
                exportRequest.getExportRequestParameters().getSubmissionAgencyIdentifier() :
                null;
            manifestBuilder
                .writeManagementMetadata(originatingAgency, submissionAgencyIdentifier);

            manifestBuilder.endDataObjectPackage();

            switch (exportRequest.getExportType()) {
                case ArchiveDeliveryRequestReply:
                case ArchiveTransfer:
                    if (Strings.isNullOrEmpty(exportRequest.getExportRequestParameters().getTransferringAgency())) {
                        exportRequest.getExportRequestParameters()
                            .setTransferringAgency(VitamConfiguration.getVitamDefaultTransferringAgency());
                    }
                    manifestBuilder
                        .writeFooter(exportRequest.getExportType(), exportRequest.getExportRequestParameters());
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
                handlerIO
                    .transferInputStreamToWorkspace(handlerIO.getContainerName() + JSONL_EXTENSION, reportFile, null,
                        false);
            }

        } catch (ExportException e) {
            itemStatus.increment(StatusCode.KO);
            ObjectNode infoNode = JsonHandler.createObjectNode();
            infoNode.put(REASON_FIELD, e.getMessage());
            String evDetData = JsonHandler.unprettyPrint(infoNode);
            itemStatus.setEvDetailData(evDetData);
        } catch (IOException | MetaDataExecutionException | InvalidCreateOperationException |
                 MetaDataClientServerException | XMLStreamException | JAXBException | LogbookClientException |
                 MetaDataDocumentSizeException | InvalidParseOperationException | InternalServerException e) {
            throw new ProcessingException(e);
        }
        return new ItemStatus(PLUGIN_NAME).setItemsStatus(PLUGIN_NAME, itemStatus);
    }

    private AccessContractModel getAccessContractModel(AdminManagementClient adminManagementClient)
        throws ProcessingException {
        AccessContractModel accessContractModel = VitamThreadUtils.getVitamSession().getContract();

        if (accessContractModel == null) {
            Select accessContractSelect = new Select();

            try {
                Query query =
                    QueryHelper.eq(AccessContract.IDENTIFIER,
                        VitamThreadUtils.getVitamSession().getContractId());
                accessContractSelect.setQuery(query);
                accessContractModel =
                    ((RequestResponseOK<AccessContractModel>) adminManagementClient.findAccessContracts(
                        accessContractSelect.getFinalSelect()))
                        .getResults().get(0);
            } catch (InvalidCreateOperationException | AdminManagementClientServerException |
                     InvalidParseOperationException e) {
                throw new ProcessingException(e.getMessage(), e.getCause());
            }
        }
        return accessContractModel;
    }

    private void checkSize(ItemStatus itemStatus, long exportSize, long threshold, int tenant) throws ExportException {
        if (exportSize > threshold) {
            throw new ExportException(
                String.format("export size exceeds threshold. \n Export Size = [%d]\n Threshold = [%d]", exportSize,
                    threshold));
        }
        Optional<BinarySizeTenantThreshold> first = VitamConfiguration.getBinarySizeTenantThreshold().stream()
            .filter(e -> e.getTenant() == tenant).findFirst();
        if (first.isPresent()) {
            if (exportSize > first.get().getThreshold()) {
                if (first.get().isAuthorized()) {
                    updateItemStatus(itemStatus, exportSize, threshold,
                        "export size exceeds tenant threshold.\n Export Size = [%d]\n Tenant Threshold = [%d]");
                } else {
                    throw new ExportException(String
                        .format("export size exceeds tenant threshold. \n Export Size = [%d]\n Tenant Threshold = [%d]",
                            exportSize, threshold));
                }
            }
        } else {
            if (exportSize > VitamConfiguration.getBinarySizePlatformThreshold().getThreshold()) {
                updateItemStatus(itemStatus, exportSize, threshold,
                    "export size exceeds platform threshold.\n Export Size = [%d]\n Platform Threshold = [%d]");
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
        return VitamConfiguration.getBinarySizeTenantThreshold().stream()
            .filter(e -> e.getTenant() == tenant).findFirst().map(BinarySizePlatformThreshold::getThreshold)
            .orElseGet(() -> VitamConfiguration.getBinarySizePlatformThreshold().getThreshold());
    }

    private AccessContractModel findAccessContract() throws ProcessingException {
        AccessContractModel accessContractModel = VitamThreadUtils.getVitamSession().getContract();
        if (accessContractModel != null) {
            return accessContractModel;
        }

        final AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient();
        Select select = new Select();
        try {
            Query query =
                QueryHelper.eq(AccessContract.IDENTIFIER, VitamThreadUtils.getVitamSession().getContractId());
            select.setQuery(query);
            return
                ((RequestResponseOK<AccessContractModel>) client.findAccessContracts(select.getFinalSelect()))
                    .getResults().get(0);
        } catch (InvalidCreateOperationException | AdminManagementClientServerException |
                 InvalidParseOperationException e) {
            throw new ProcessingException(e);
        }
    }

    private long computeSize(JsonNode og, Set<String> dataObjectVersionFilter)
        throws InvalidParseOperationException, ProcessingException {
        AccessContractModel accessContract = findAccessContract();
        ObjectGroupResponse objectGroup = JsonHandler.getFromJsonNode(og, ObjectGroupResponse.class);

        Stream<QualifiersModel> stream = objectGroup.getQualifiers().stream();
        if (!accessContract.isEveryDataObjectVersion()) {
            stream =
                stream.filter(qualifier -> accessContract.getDataObjectVersion().contains(qualifier.getQualifier()));
        }

        if (!dataObjectVersionFilter.isEmpty()) {
            stream = stream.filter(qualifier -> dataObjectVersionFilter.contains(qualifier.getQualifier()));
        }

        return stream.map(this::getLastVersion)
            .map(VersionsModel::getSize).reduce(0L, Long::sum);
    }

    private VersionsModel getLastVersion(QualifiersModel qualifier) {
        return Iterables.getLast(qualifier.getVersions());
    }

    private boolean checkEmptinessSelectedUnits(ItemStatus itemStatus, long total) {
        if (total == 0) {
            itemStatus.increment(StatusCode.KO);
            ObjectNode infoNode = JsonHandler.createObjectNode();
            infoNode.put(REASON_FIELD, "the DSL query has no result");
            String evdev = JsonHandler.unprettyPrint(infoNode);
            itemStatus.setEvDetailData(evdev);
            return true;
        }
        return false;
    }

    private void prepareGraphCreation(ListMultimap<String, String> multimap, Set<String> originatingAgencies,
        Map<String, String> ogs, JsonNode unit, String sedaVersionToExport) throws ExportException {
        String unitSedaVersion = unit.get(sedaVersion()).asText();
        if (!SupportedSedaVersions.isSedaVersionsCompatible(unitSedaVersion, sedaVersionToExport)) {
            final String errorMsg = "Incompatible seda version to export (" + sedaVersionToExport + ") for unit : " +
                unit.get(id()).asText();
            throw new ExportException(errorMsg);
        }
        createGraph(multimap, originatingAgencies, ogs, unit);
    }

    private void createGraph(ListMultimap<String, String> multimap,
        Set<String> originatingAgencies,
        Map<String, String> ogs, JsonNode unit) {
        String archiveUnitId = unit.get(id()).asText();
        ArrayNode nodes = (ArrayNode) unit.get(VitamFieldsHelper.unitups());
        for (JsonNode node : nodes) {
            multimap.put(node.asText(), archiveUnitId);
        }
        Optional<JsonNode> originatingAgency = Optional.ofNullable(unit.get(VitamFieldsHelper.originatingAgency()));
        originatingAgency.ifPresent(jsonNode -> originatingAgencies.add(jsonNode.asText()));
        JsonNode objectIdNode = unit.get(VitamFieldsHelper.object());
        if (objectIdNode != null) {
            ogs.put(archiveUnitId, objectIdNode.asText());
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

}

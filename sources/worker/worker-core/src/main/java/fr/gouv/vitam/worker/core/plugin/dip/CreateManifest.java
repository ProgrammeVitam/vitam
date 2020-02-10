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
package fr.gouv.vitam.worker.core.plugin.dip;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.InQuery;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.query.action.SetAction;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.parser.query.ParserTokens;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.database.utils.ScrollSpliterator;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.export.ExportRequest;
import fr.gouv.vitam.common.model.unit.ArchiveUnitModel;
import fr.gouv.vitam.common.parameter.ParameterHelper;
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
import java.util.stream.StreamSupport;

import static com.google.common.collect.Iterables.partition;
import static fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper.id;
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTION.FIELDS;
import static fr.gouv.vitam.common.database.parser.query.ParserTokens.PROJECTIONARGS.ID;
import static fr.gouv.vitam.common.database.parser.query.ParserTokens.PROJECTIONARGS.OBJECT;
import static fr.gouv.vitam.common.database.parser.query.ParserTokens.PROJECTIONARGS.ORIGINATING_AGENCY;
import static fr.gouv.vitam.common.database.parser.query.ParserTokens.PROJECTIONARGS.UNITUPS;
import static fr.gouv.vitam.common.json.JsonHandler.unprettyPrint;
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

    private static final String CREATE_MANIFEST = "CREATE_MANIFEST";
    private static final int MAX_ELEMENT_IN_QUERY = 1000;
    private static final String REASON_FIELD = "Reason";
    private static final String JSONL_EXTENSION = ".jsonl";

    private MetaDataClientFactory metaDataClientFactory;
    private ObjectNode projection;

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

        this.projection = JsonHandler.createObjectNode();
        this.projection.set(FIELDS.exactToken(), fields);
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handlerIO) throws ProcessingException {
        final ItemStatus itemStatus = new ItemStatus(CREATE_MANIFEST);
        File manifestFile = handlerIO.getNewLocalFile(handlerIO.getOutput(MANIFEST_XML_RANK).getPath());
        File report = handlerIO.getNewLocalFile(handlerIO.getOutput(REPORT).getPath());

        try (MetaDataClient client = metaDataClientFactory.getClient();
            OutputStream outputStream = new FileOutputStream(manifestFile);
            FileOutputStream fileOutputStream = new FileOutputStream(report);
            FileInputStream reportFile = new FileInputStream(report);
            BufferedOutputStream buffOut = new BufferedOutputStream(fileOutputStream);
            ManifestBuilder manifestBuilder = new ManifestBuilder(outputStream)) {

            ExportRequest exportRequest = JsonHandler
                .getFromJsonNode(handlerIO.getJsonFromWorkspace(EXPORT_QUERY_FILE_NAME), ExportRequest.class);
            TransferReportHeader reportHeader = new TransferReportHeader(exportRequest.getDslRequest());

            if (ArchiveTransfer.equals(exportRequest.getExportType())) {
                buffOut.write(unprettyPrint(JsonHandler.createObjectNode()).getBytes(UTF_8)); // header empty
                buffOut.write(System.lineSeparator().getBytes(StandardCharsets.UTF_8));
                buffOut.write(unprettyPrint(reportHeader).getBytes(StandardCharsets.UTF_8));  // context
                buffOut.write(System.lineSeparator().getBytes(StandardCharsets.UTF_8));
            }

            switch (exportRequest.getExportType()) {
                case ArchiveDeliveryRequestReply:
                case ArchiveTransfer:
                    // Validate request
                    manifestBuilder.validate(exportRequest.getExportType(), exportRequest.getExportRequestParameters());
                    break;
                default:
                    break;
            }


            // Write manifest first line information
            manifestBuilder.startDocument(param.getContainerName(), exportRequest.getExportType(),
                exportRequest.getExportRequestParameters());


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

            StreamSupport.stream(scrollRequest, false)
                .forEach(item -> createGraph(multimap, originatingAgencies, ogs, item));

            if (checkNumberOfUnit(itemStatus, scrollRequest.estimateSize())) {
                return new ItemStatus(CREATE_MANIFEST).setItemsStatus(CREATE_MANIFEST, itemStatus);
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
                ArrayNode objects = (ArrayNode) response.get("$results");

                for (JsonNode object : objects) {
                    List<String> linkedUnits = unitsForObjectGroupId.get(
                        object.get(ParserTokens.PROJECTIONARGS.ID.exactToken()).textValue());
                    idBinaryWithFileName.putAll(manifestBuilder
                        .writeGOT(object, linkedUnits.get(linkedUnits.size() - 1), dataObjectVersions,
                            exportWithLogBookLFC));

                }
            }

            storeBinaryInformationOnWorkspace(handlerIO, idBinaryWithFileName);

            SelectParserMultiple initialQueryParser = new SelectParserMultiple();
            initialQueryParser.parse(exportRequest.getDslRequest());

                scrollRequest = new ScrollSpliterator<>(initialQueryParser.getRequest(),
                    query -> {
                        try {
                            JsonNode node = client.selectUnits(query.getFinalSelect());
                            return RequestResponseOK.getFromJsonNode(node);
                        } catch (MetaDataExecutionException | MetaDataDocumentSizeException | MetaDataClientServerException | InvalidParseOperationException e) {
                            throw new IllegalStateException(e);
                        }
                    }, VitamConfiguration.getElasticSearchScrollTimeoutInMilliseconds(), VitamConfiguration.getElasticSearchScrollLimit());

            manifestBuilder.startDescriptiveMetadata();
            StreamSupport.stream(scrollRequest, false)
                .forEach(result -> {
                    try {
                        ArchiveUnitModel unit =
                            manifestBuilder.writeArchiveUnit(result, multimap, ogs, exportWithLogBookLFC);
                        if (ArchiveTransfer.equals(exportRequest.getExportType())) {
                            List<String> opts = ListUtils.defaultIfNull(unit.getOpts(), new ArrayList<>());
                            TransferStatus status = opts.isEmpty()?
                                TransferStatus.OK:
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
                    } catch (JAXBException | DatatypeConfigurationException | IOException | ProcessingException |
                        InvalidParseOperationException | InvalidCreateOperationException | MetaDataNotFoundException |
                        MetaDataExecutionException | MetaDataDocumentSizeException | MetaDataClientServerException e) {
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

            handlerIO.addOutputResult(MANIFEST_XML_RANK, manifestFile, true, false);

            itemStatus.increment(StatusCode.OK);

            if (ArchiveTransfer.equals(exportRequest.getExportType())) {
                handlerIO.transferInputStreamToWorkspace(handlerIO.getContainerName() + JSONL_EXTENSION, reportFile, null, false);
            }

        } catch (ExportException e) {
            itemStatus.increment(StatusCode.KO);
            ObjectNode infoNode = JsonHandler.createObjectNode();
            infoNode.put(REASON_FIELD, e.getMessage());
            String evDetData = JsonHandler.unprettyPrint(infoNode);
            itemStatus.setEvDetailData(evDetData);
        } catch (IOException | MetaDataExecutionException | InvalidCreateOperationException | MetaDataClientServerException
            | XMLStreamException | JAXBException | MetaDataDocumentSizeException | InvalidParseOperationException e) {
            throw new ProcessingException(e);
        }

        return new ItemStatus(CREATE_MANIFEST).setItemsStatus(CREATE_MANIFEST, itemStatus);
    }

    private boolean checkNumberOfUnit(ItemStatus itemStatus, long total) {
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

    private void createGraph(ListMultimap<String, String> multimap, Set<String> originatingAgencies,
        Map<String, String> ogs, JsonNode result) {
        String archiveUnitId = result.get(id()).asText();
        ArrayNode nodes = (ArrayNode) result.get(VitamFieldsHelper.unitups());
        for (JsonNode node : nodes) {
            multimap.put(node.asText(), archiveUnitId);
        }
        Optional<JsonNode> originatingAgency = Optional.ofNullable(result.get(VitamFieldsHelper.originatingAgency()));
        originatingAgency.ifPresent(jsonNode -> originatingAgencies.add(jsonNode.asText()));
        JsonNode objectIdNode = result.get(VitamFieldsHelper.object());
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

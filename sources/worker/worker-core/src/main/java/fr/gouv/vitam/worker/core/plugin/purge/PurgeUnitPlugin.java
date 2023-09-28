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
package fr.gouv.vitam.worker.core.plugin.purge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.batch.report.model.entry.PurgeUnitReportEntry;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.utils.MetadataDocumentHelper;
import fr.gouv.vitam.common.exception.InvalidGuidOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.guid.GUIDReader;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.LifeCycleStatusCode;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.objectgroup.PersistentIdentifierModel;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.exception.ProcessingStatusException;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.plugin.elimination.EliminationUtils;
import fr.gouv.vitam.worker.core.utils.PluginHelper;
import org.apache.commons.collections4.SetUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.common.model.StatusCode.WARNING;
import static fr.gouv.vitam.worker.core.plugin.transfer.reply.TransferReplyDeleteUnitPlugin.TRANSFER_REPLY_DELETE_UNIT;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;
import static java.util.Collections.singletonList;


/**
 * Purge unit plugin.
 */
public class PurgeUnitPlugin extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(PurgeUnitPlugin.class);

    private static final String UNIT_DELETION_ABORT = "UNIT_DELETION_ABORT";
    private static final String REPORT_PERSISTENT_IDENTIFIER_FIELD = "PersistentIdentifier";

    private final String actionId;
    private final PurgeDeleteService purgeDeleteService;
    private final MetaDataClientFactory metaDataClientFactory;
    private final PurgeReportService purgeReportService;
    private final LogbookLifeCyclesClientFactory lfcClientFactory;

    /**
     * Default constructor
     *
     * @param actionId
     */
    public PurgeUnitPlugin(String actionId) {
        this(actionId, new PurgeDeleteService(), MetaDataClientFactory.getInstance(), new PurgeReportService(),
            LogbookLifeCyclesClientFactory.getInstance());
    }

    /***
     * Test only constructor
     */
    @VisibleForTesting
    protected PurgeUnitPlugin(String actionId, PurgeDeleteService purgeDeleteService,
        MetaDataClientFactory metaDataClientFactory, PurgeReportService purgeReportService,
        LogbookLifeCyclesClientFactory llfcClientFactory) {
        this.actionId = actionId;
        this.purgeDeleteService = purgeDeleteService;
        this.metaDataClientFactory = metaDataClientFactory;
        this.purgeReportService = purgeReportService;
        this.lfcClientFactory = llfcClientFactory;
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) throws ProcessingException {
        throw new ProcessingException("No need to implements method");
    }

    @Override
    public List<ItemStatus> executeList(WorkerParameters param, HandlerIO handler) {

        try {
            return processUnits(param.getContainerName(), param);
        } catch (ProcessingStatusException e) {
            LOGGER.error("Unit purge failed with status " + e.getStatusCode(), e);
            return singletonList(buildItemStatus(actionId, e.getStatusCode(), e.getEventDetails()));
        }
    }

    private List<ItemStatus> processUnits(String processId, WorkerParameters param) throws ProcessingStatusException {
        List<JsonNode> units = param.getObjectMetadataList();
        List<String> unitIds =
            units.stream().map(unit -> unit.get(VitamFieldsHelper.id()).asText()).collect(Collectors.toList());

        Map<String, JsonNode> unitsById =
            units.stream().collect(Collectors.toMap(unit -> unit.get(VitamFieldsHelper.id()).asText(), unit -> unit));

        List<ItemStatus> itemStatuses = new ArrayList<>();

        List<PurgeUnitReportEntry> purgeUnitReportEntries = new ArrayList<>();

        Set<String> unitsToDelete = getUnitsToDelete(unitsById.keySet());

        Map<String, String> unitIdsWithStrategiesToDelete =
            unitsById.entrySet().stream().filter(e -> unitsToDelete.contains(e.getKey())).collect(
                Collectors.toMap(Entry::getKey,
                    entry -> MetadataDocumentHelper.getStrategyIdFromUnit(entry.getValue())));

        for (String unitId : unitIds) {

            PurgeUnitStatus purgeUnitStatus;
            if (unitsToDelete.contains(unitId)) {
                LOGGER.info("Unit " + unitId + " will be deleted");
                purgeUnitStatus = PurgeUnitStatus.DELETED;
                itemStatuses.add(buildItemStatus(actionId, StatusCode.OK, null));
            } else {
                String msg = "Unit cannot be deleted because it has child units attached to it.";
                LOGGER.info(unitId + " " + msg);
                purgeUnitStatus = PurgeUnitStatus.NON_DESTROYABLE_HAS_CHILD_UNITS;
                itemStatuses.add(buildItemStatus(actionId, WARNING, PluginHelper.EventDetails.of(msg)));
                if (TRANSFER_REPLY_DELETE_UNIT.equals(actionId)) {
                    try {
                        writeLfcForUnpurgedUnit(lfcClientFactory.getClient(), param, unitId);
                    } catch (InvalidGuidOperationException | LogbookClientServerException |
                             LogbookClientBadRequestException | LogbookClientNotFoundException e) {
                        LOGGER.error(e);
                    }
                }
            }

            PurgeUnitReportEntry reportEntry = createReportEntry(unitsById, unitId, purgeUnitStatus);
            purgeUnitReportEntries.add(reportEntry);

        }

        purgeReportService.appendUnitEntries(processId, purgeUnitReportEntries);

        try {
            purgeDeleteService.deleteUnits(unitIdsWithStrategiesToDelete);
        } catch (MetaDataExecutionException | MetaDataClientServerException | LogbookClientBadRequestException |
                 StorageServerClientException | LogbookClientServerException e) {
            throw new ProcessingStatusException(StatusCode.FATAL,
                "Could not delete units [" + String.join(", ", unitsToDelete) + "]", e);
        }

        return itemStatuses;
    }

    private static PurgeUnitReportEntry createReportEntry(Map<String, JsonNode> unitsById, String unitId,
        PurgeUnitStatus purgeUnitStatus) {
        JsonNode unit = unitsById.get(unitId);
        String initialOperation = unit.get(VitamFieldsHelper.initialOperation()).asText();
        String objectGroupId =
            unit.has(VitamFieldsHelper.object()) ? unit.get(VitamFieldsHelper.object()).asText() : null;
        String originatingAgency = unit.has(VitamFieldsHelper.originatingAgency()) ?
            unit.get(VitamFieldsHelper.originatingAgency()).asText() :
            null;
        String unitType =
            unit.has(VitamFieldsHelper.unitType()) ? unit.get(VitamFieldsHelper.unitType()).asText() : null;

        if (purgeUnitStatus.equals(PurgeUnitStatus.DELETED)) {
            List<PersistentIdentifierModel> persistentIdentifier = extractPersistentIdentifiersFromUnit(unit);
            JsonNode extraInfo = extractExtraInfoFromUnit(unit);
            return new PurgeUnitReportEntry(unitId, originatingAgency, initialOperation, objectGroupId,
                purgeUnitStatus.name(), extraInfo, persistentIdentifier, unitType);
        } else {
            return new PurgeUnitReportEntry(unitId, originatingAgency, initialOperation, objectGroupId,
                purgeUnitStatus.name(), null, null, unitType);
        }
    }

    @VisibleForTesting
    protected static List<PersistentIdentifierModel> extractPersistentIdentifiersFromUnit(JsonNode unit) {

        List<PersistentIdentifierModel> persistentIdentifiers = new ArrayList<>();

        if (unit.has(REPORT_PERSISTENT_IDENTIFIER_FIELD)) {
            JsonNode persistentIdentifierArray = unit.get(REPORT_PERSISTENT_IDENTIFIER_FIELD);

            if (persistentIdentifierArray.isArray()) {
                for (JsonNode identifierNode : persistentIdentifierArray) {
                    PersistentIdentifierModel identifierModel = mapJsonToPersistentIdentifierModel(identifierNode);
                    persistentIdentifiers.add(identifierModel);
                }
            }
        }

        return persistentIdentifiers;
    }

    @VisibleForTesting
    protected static PersistentIdentifierModel mapJsonToPersistentIdentifierModel(JsonNode identifierNode) {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.convertValue(identifierNode, PersistentIdentifierModel.class);
    }


    private static JsonNode extractExtraInfoFromUnit(JsonNode unit) {
        ObjectNode extraInfo = JsonHandler.createObjectNode();
        for (String metadataKey : EliminationUtils.getReportExtraFields()) {
            if (unit.has(metadataKey)) {
                extraInfo.set(metadataKey, unit.get(metadataKey));
            }
        }
        return extraInfo;
    }

    private Set<String> getUnitsToDelete(Set<String> unitIds) throws ProcessingStatusException {
        Set<String> unitsWithChildren = getUnitsWithChildren(unitIds);
        return SetUtils.difference(unitIds, unitsWithChildren);
    }

    private Set<String> getUnitsWithChildren(Set<String> unitIds) throws ProcessingStatusException {

        try (MetaDataClient metaDataClient = metaDataClientFactory.getClient()) {

            Set<String> unitsToFetch = new HashSet<>(unitIds);
            Set<String> result = new HashSet<>();

            while (!unitsToFetch.isEmpty()) {

                RequestResponseOK<JsonNode> responseOK = selectChildUnits(metaDataClient, unitsToFetch);

                Set<String> unitsWithChildren = parseUnitsWithChildren(responseOK.getResults(), unitsToFetch);

                result.addAll(unitsWithChildren);
                unitsToFetch.removeAll(unitsWithChildren);

                if (noMoreResults(responseOK)) {
                    break;
                }
            }

            return result;

        } catch (InvalidParseOperationException | InvalidCreateOperationException | MetaDataExecutionException |
                 MetaDataDocumentSizeException | MetaDataClientServerException e) {
            throw new ProcessingStatusException(StatusCode.FATAL, "Could not check child units", e);
        }
    }

    private RequestResponseOK<JsonNode> selectChildUnits(MetaDataClient metaDataClient, Set<String> unitsToFetch)
        throws InvalidCreateOperationException, InvalidParseOperationException, MetaDataExecutionException,
        MetaDataDocumentSizeException, MetaDataClientServerException {
        SelectMultiQuery selectAllUnitsUp = new SelectMultiQuery();
        selectAllUnitsUp.addQueries(QueryHelper.in(VitamFieldsHelper.unitups(), unitsToFetch.toArray(new String[0])));
        selectAllUnitsUp.setLimitFilter(0, VitamConfiguration.getBatchSize());
        selectAllUnitsUp.addUsedProjection(VitamFieldsHelper.unitups());
        JsonNode response = metaDataClient.selectUnits(selectAllUnitsUp.getFinalSelect());
        return RequestResponseOK.getFromJsonNode(response);
    }

    private Set<String> parseUnitsWithChildren(List<JsonNode> results, Set<String> unitsToFetch) {
        Set<String> foundUnitIds = new HashSet<>();

        for (JsonNode childUnit : results) {
            childUnit.get(VitamFieldsHelper.unitups()).elements().forEachRemaining(jsonNode -> {
                String unitId = jsonNode.asText();
                if (unitsToFetch.contains(unitId)) {
                    foundUnitIds.add(unitId);
                }
            });
        }

        return foundUnitIds;
    }

    private void writeLfcForUnpurgedUnit(LogbookLifeCyclesClient lfcClient, WorkerParameters param, String unitId)
        throws InvalidGuidOperationException, LogbookClientNotFoundException, LogbookClientBadRequestException,
        LogbookClientServerException {
        LogbookLifeCycleParameters logbookLfcParam = LogbookParameterHelper.newLogbookLifeCycleUnitParameters(
            GUIDFactory.newEventGUID(ParameterHelper.getTenantParameter()),
            VitamLogbookMessages.getEventTypeLfc(UNIT_DELETION_ABORT), GUIDReader.getGUID(param.getContainerName()),
            param.getLogbookTypeProcess(), OK, VitamLogbookMessages.getOutcomeDetailLfc(UNIT_DELETION_ABORT, OK),
            VitamLogbookMessages.getCodeLfc(UNIT_DELETION_ABORT, OK), GUIDReader.getGUID(unitId));
        lfcClient.update(logbookLfcParam, LifeCycleStatusCode.LIFE_CYCLE_COMMITTED);
    }

    private boolean noMoreResults(RequestResponseOK<JsonNode> responseOK) {
        return responseOK.getHits().getTotal() < VitamConfiguration.getBatchSize();
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // NOP.
    }
}

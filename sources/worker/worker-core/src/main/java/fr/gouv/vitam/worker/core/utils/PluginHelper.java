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
package fr.gouv.vitam.worker.core.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Iterators;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.database.utils.ScrollSpliterator;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.iterables.SpliteratorIterator;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.DeleteGotVersionsRequest;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.objectgroup.ObjectGroupResponse;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleUnitParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import fr.gouv.vitam.worker.core.distribution.JsonLineWriter;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.and;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.exists;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.in;
import static fr.gouv.vitam.common.database.parser.query.ParserTokens.PROJECTIONARGS.ID;
import static fr.gouv.vitam.common.database.parser.query.ParserTokens.PROJECTIONARGS.OBJECT;
import static fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper.newLogbookLifeCycleUnitParameters;
import static fr.gouv.vitam.worker.core.plugin.ScrollSpliteratorHelper.createUnitScrollSplitIterator;

/**
 * Basic helper methods for reclassification plugins
 */
public class PluginHelper {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(PluginHelper.class);

    private PluginHelper() {
        throw new IllegalStateException("Utility class");
    }

    public static class EventDetails {

        public String event;
        public String secondEvent;

        private EventDetails(String event) {
            this.event = event;
        }

        private EventDetails(String event, String secondEvent) {
            this.event = event;
            this.secondEvent = secondEvent;
        }

        public static EventDetails of(String event) {
            return new EventDetails(event);
        }

        public static EventDetails of(String event, String secondEvent) {
            return new EventDetails(event, secondEvent);
        }
    }

    public static ItemStatus buildItemStatus(String action, StatusCode statusCode) {
        return buildItemStatus(action, statusCode, null);
    }

    public static <TEventDetails> ItemStatus buildItemStatus(
        String action,
        StatusCode statusCode,
        TEventDetails eventDetails
    ) {
        final ItemStatus itemStatus = new ItemStatus(action);
        itemStatus.increment(statusCode);
        setEvDetData(itemStatus, eventDetails);
        return new ItemStatus(action).setItemsStatus(action, itemStatus);
    }

    public static ItemStatus buildItemStatusWithMessage(String action, StatusCode statusCode, String message) {
        final ItemStatus itemStatus = new ItemStatus(action);
        itemStatus.increment(statusCode);
        itemStatus.setMessage(message);
        setEvDetData(itemStatus, EventDetails.of(message));
        return new ItemStatus(action).setItemsStatus(action, itemStatus).setMessage(message);
    }

    public static <TEventDetails> ItemStatus buildItemStatusWithMasterData(
        String action,
        StatusCode statusCode,
        TEventDetails eventDetails,
        Object masterDataValue
    ) {
        final ItemStatus itemStatus = new ItemStatus(action);
        itemStatus.increment(statusCode);
        setEvDetData(itemStatus, eventDetails);
        itemStatus.setMasterData(LogbookParameterName.eventDetailData.name(), masterDataValue);
        return new ItemStatus(action).setItemsStatus(action, itemStatus);
    }

    public static <T> ItemStatus buildItemStatusSubItems(
        String itemId,
        Stream<String> subItemIds,
        StatusCode statusCode,
        T eventDetails
    ) {
        final ItemStatus itemStatus = new ItemStatus(itemId);
        itemStatus.increment(statusCode);
        setEvDetData(itemStatus, eventDetails);
        Map<String, ItemStatus> obIds = subItemIds.collect(Collectors.toMap(id -> id, id -> new ItemStatus(itemId)));
        itemStatus.setSubTasksStatus(obIds);
        return new ItemStatus(itemId).setItemsStatus(itemId, itemStatus);
    }

    private static <TEventDetails> void setEvDetData(ItemStatus itemStatus, TEventDetails eventDetails) {
        if (eventDetails != null) {
            try {
                itemStatus.setEvDetailData(JsonHandler.unprettyPrint(JsonHandler.toJsonNode(eventDetails)));
            } catch (InvalidParseOperationException e1) {
                throw new VitamRuntimeException("Could not serialize event details" + eventDetails, e1);
            }
        }
    }

    public static ObjectNode eventDetails(Throwable e) {
        return JsonHandler.createObjectNode().put("error", e.getMessage());
    }

    public static List<ItemStatus> buildBulkItemStatus(WorkerParameters param, String action, StatusCode statusCode) {
        List<ItemStatus> itemStatuses = new ArrayList<>();

        for (int i = 0; i < param.getObjectNameList().size(); i++) {
            final ItemStatus itemStatus = new ItemStatus(action);
            itemStatus.increment(statusCode);
            ItemStatus itemsStatus = new ItemStatus(action).setItemsStatus(action, itemStatus);
            itemStatuses.add(itemsStatus);
        }
        return itemStatuses;
    }

    public static <TEventDetails> List<ItemStatus> buildBulkItemStatus(
        WorkerParameters param,
        String action,
        StatusCode statusCode,
        TEventDetails eventDetails
    ) {
        List<ItemStatus> itemStatuses = new ArrayList<>();

        for (int i = 0; i < param.getObjectNameList().size(); i++) {
            final ItemStatus itemStatus = new ItemStatus(action);
            itemStatus.increment(statusCode);
            ItemStatus itemsStatus = new ItemStatus(action).setItemsStatus(action, itemStatus);
            setEvDetData(itemStatus, eventDetails);
            itemStatuses.add(itemsStatus);
        }
        return itemStatuses;
    }

    public static <TEventDetails> LogbookLifeCycleUnitParameters createParameters(
        GUID eventIdentifierProcess,
        StatusCode logbookOutcome,
        GUID objectIdentifier,
        String action,
        TEventDetails eventDetails,
        LogbookTypeProcess logbookTypeProcess
    ) {
        final GUID updateGuid = GUIDFactory.newEventGUID(ParameterHelper.getTenantParameter());
        LogbookLifeCycleUnitParameters parameters = newLogbookLifeCycleUnitParameters(
            updateGuid,
            VitamLogbookMessages.getEventTypeLfc(action),
            eventIdentifierProcess,
            logbookTypeProcess,
            logbookOutcome,
            VitamLogbookMessages.getOutcomeDetailLfc(action, logbookOutcome),
            VitamLogbookMessages.getCodeLfc(action, logbookOutcome),
            objectIdentifier
        );

        if (eventDetails != null) {
            try {
                parameters.putParameterValue(
                    LogbookParameterName.eventDetailData,
                    (JsonHandler.unprettyPrint(JsonHandler.toJsonNode(eventDetails)))
                );
            } catch (InvalidParseOperationException e1) {
                throw new VitamRuntimeException("Could not serialize event details" + eventDetails);
            }
        }
        return parameters;
    }

    public static InputStream createUnitsByGotFile(
        MetaDataClient metaDataClient,
        DeleteGotVersionsRequest deleteGotVersionsRequest,
        HandlerIO handler
    ) throws VitamException {
        SelectMultiQuery selectMultiQuery = prepareUnitsWithObjectGroupsQuery(deleteGotVersionsRequest.getDslQuery());
        ScrollSpliterator<JsonNode> scrollRequest = createUnitScrollSplitIterator(metaDataClient, selectMultiQuery);
        Iterator<JsonNode> iterator = new SpliteratorIterator<>(scrollRequest);
        Iterator<Pair<String, String>> gotIdUnitIdIterator = getGotIdUnitIdIterator(iterator);
        Iterator<List<Pair<String, List<String>>>> bulksUnitsByObjectGroup = Iterators.partition(
            new GroupByObjectIterator(gotIdUnitIdIterator),
            VitamConfiguration.getBatchSize()
        );
        return generateUnitsByGotFile(bulksUnitsByObjectGroup, handler);
    }

    private static SelectMultiQuery prepareUnitsWithObjectGroupsQuery(JsonNode initialQuery) {
        try {
            SelectParserMultiple parser = new SelectParserMultiple();
            parser.parse(initialQuery);
            SelectMultiQuery selectMultiQuery = parser.getRequest();
            selectMultiQuery.resetUsageProjection();
            selectMultiQuery.addUsedProjection(ID.exactToken(), OBJECT.exactToken());
            selectMultiQuery.addOrderByAscFilter(OBJECT.exactToken());
            List<Query> queryList = new ArrayList<>(parser.getRequest().getQueries());

            if (queryList.isEmpty()) {
                selectMultiQuery.addQueries(and().add(exists(OBJECT.exactToken())).setDepthLimit(0));
                return selectMultiQuery;
            }

            final Query query = queryList.get(queryList.size() - 1);
            Query restrictedQuery = and().add(exists(OBJECT.exactToken()), query);
            parser.getRequest().getQueries().set(queryList.size() - 1, restrictedQuery);
            return selectMultiQuery;
        } catch (InvalidParseOperationException | InvalidCreateOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Iterator<Pair<String, String>> getGotIdUnitIdIterator(Iterator<JsonNode> iterator) {
        return IteratorUtils.transformedIterator(
            iterator,
            item -> new ImmutablePair<>(item.get(OBJECT.exactToken()).asText(), item.get(ID.exactToken()).asText())
        );
    }

    private static InputStream generateUnitsByGotFile(
        Iterator<List<Pair<String, List<String>>>> unitsByObjectGroup,
        HandlerIO handler
    ) throws VitamException {
        File unitsByGotTempFile = handler.getNewLocalFile("unitsByGotTempFile.jsonl");
        try (
            final OutputStream outputStream = new FileOutputStream(unitsByGotTempFile);
            JsonLineWriter writer = new JsonLineWriter(outputStream)
        ) {
            while (unitsByObjectGroup.hasNext()) {
                List<Pair<String, List<String>>> unitsByObjectGroupByRange = unitsByObjectGroup.next();
                for (Pair<String, List<String>> unitsByGot : unitsByObjectGroupByRange) {
                    writer.addEntry(
                        new JsonLineModel(unitsByGot.getLeft(), null, JsonHandler.toJsonNode(unitsByGot.getRight()))
                    );
                }
            }
            return new FileInputStream(unitsByGotTempFile);
        } catch (IOException e) {
            throw new VitamException("Could not save distribution file", e);
        }
    }

    public static Map<String, ObjectGroupResponse> getObjectGroups(String[] gotIds, MetaDataClient metadataClient)
        throws ProcessingException {
        try {
            Select select = new Select();
            select.setQuery(in("#id", gotIds));

            ObjectNode finalSelect = select.getFinalSelect();
            JsonNode response = metadataClient.selectObjectGroups(finalSelect);

            List<ObjectGroupResponse> resultsResponse = JsonHandler.getFromJsonNode(
                response.get("$results"),
                new TypeReference<>() {}
            );

            if (resultsResponse.isEmpty() || resultsResponse.size() != gotIds.length) {
                throw new IllegalStateException("Object groups are missing from database!");
            }

            return resultsResponse
                .stream()
                .collect(Collectors.toMap(ObjectGroupResponse::getId, objectGroup -> objectGroup));
        } catch (
            InvalidParseOperationException
            | MetaDataExecutionException
            | MetaDataDocumentSizeException
            | MetaDataClientServerException
            | InvalidCreateOperationException e
        ) {
            throw new ProcessingException("A problem occured when retrieving ObjectGroups :  " + e.getMessage());
        }
    }
}

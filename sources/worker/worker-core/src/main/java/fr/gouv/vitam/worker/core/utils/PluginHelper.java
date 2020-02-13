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
package fr.gouv.vitam.worker.core.utils;

import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleObjectGroupParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleUnitParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper.newLogbookLifeCycleObjectGroupParameters;
import static fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper.newLogbookLifeCycleUnitParameters;

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

    public static <TEventDetails> ItemStatus buildItemStatus(String action, StatusCode statusCode,
        TEventDetails eventDetails) {
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
        return new ItemStatus(action).setItemsStatus(action, itemStatus);
    }

    public static <TEventDetails> ItemStatus buildItemStatusWithMasterData(String action, StatusCode statusCode, TEventDetails eventDetails, Object masterDataValue) {
        final ItemStatus itemStatus = new ItemStatus(action);
        itemStatus.increment(statusCode);
        setEvDetData(itemStatus, eventDetails);
        itemStatus.setMasterData(LogbookParameterName.eventDetailData.name(), masterDataValue);
        return new ItemStatus(action).setItemsStatus(action, itemStatus);
    }

    public static <T> ItemStatus buildItemStatusSubItems(String itemId, Stream<String> subItemIds, StatusCode statusCode, T eventDetails) {
        final ItemStatus itemStatus = new ItemStatus(itemId);
        itemStatus.increment(statusCode);
        setEvDetData(itemStatus, eventDetails);
        Map<String, ItemStatus> obIds = subItemIds.collect(Collectors.toMap(id -> id, id -> itemStatus));
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
            ItemStatus itemsStatus = new ItemStatus(action)
                .setItemsStatus(action, itemStatus);
            itemStatuses.add(itemsStatus);
        }
        return itemStatuses;
    }

    public static <TEventDetails> List<ItemStatus> buildBulkItemStatus(WorkerParameters param, String action,
        StatusCode statusCode, TEventDetails eventDetails) {
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

    public static <TEventDetails> LogbookLifeCycleUnitParameters createParameters(GUID eventIdentifierProcess,
        StatusCode logbookOutcome, GUID objectIdentifier, String action, TEventDetails eventDetails,
        LogbookTypeProcess logbookTypeProcess) {

        final GUID updateGuid = GUIDFactory.newEventGUID(ParameterHelper.getTenantParameter());
        LogbookLifeCycleUnitParameters parameters = newLogbookLifeCycleUnitParameters(updateGuid,
            VitamLogbookMessages.getEventTypeLfc(action),
            eventIdentifierProcess,
            logbookTypeProcess, logbookOutcome,
            VitamLogbookMessages.getOutcomeDetailLfc(action, logbookOutcome),
            VitamLogbookMessages.getCodeLfc(action, logbookOutcome), objectIdentifier);

        if (eventDetails != null) {
            try {
                parameters.putParameterValue(LogbookParameterName.eventDetailData,
                    (JsonHandler.unprettyPrint(JsonHandler.toJsonNode(eventDetails))));
            } catch (InvalidParseOperationException e1) {
                throw new VitamRuntimeException("Could not serialize event details" + eventDetails);
            }
        }
        return parameters;
    }

    public static <TEventDetails> LogbookLifeCycleObjectGroupParameters createObjectGroupLfcParameters(
        GUID eventIdentifierProcess,
        StatusCode logbookOutcome, GUID objectIdentifier, String action, TEventDetails eventDetails,
        LogbookTypeProcess logbookTypeProcess) {
        final GUID updateGuid = GUIDFactory.newEventGUID(ParameterHelper.getTenantParameter());
        LogbookLifeCycleObjectGroupParameters parameters = newLogbookLifeCycleObjectGroupParameters(updateGuid,
            VitamLogbookMessages.getEventTypeLfc(action),
            eventIdentifierProcess,
            logbookTypeProcess, logbookOutcome,
            VitamLogbookMessages.getOutcomeDetailLfc(action, logbookOutcome),
            VitamLogbookMessages.getCodeLfc(action, logbookOutcome), objectIdentifier);
        if (eventDetails != null) {
            try {
                parameters.putParameterValue(LogbookParameterName.eventDetailData,
                    (JsonHandler.unprettyPrint(JsonHandler.toJsonNode(eventDetails))));
            } catch (InvalidParseOperationException e1) {
                throw new VitamRuntimeException("Could not serialize event details" + eventDetails);
            }
        }
        return parameters;
    }
}

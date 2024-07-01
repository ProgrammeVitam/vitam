/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.processing.StatusAggregationBehavior;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static fr.gouv.vitam.common.model.StatusCode.KO;
import static fr.gouv.vitam.common.model.StatusCode.WARNING;

/**
 * Composite Item Status
 */
@JsonIgnoreProperties
public class ItemStatus {

    private static final String EVENT_DETAIL_DATA = "eventDetailData";
    private static final String MANDATORY_PARAMETER = "Mandatory parameter";
    private static final String MASTER_DATA = "masterData";

    @JsonProperty("itemsStatus")
    private LinkedHashMap<String, ItemStatus> itemsStatus = new LinkedHashMap<>();

    @JsonProperty("subTaskStatus")
    private LinkedHashMap<String, ItemStatus> subTaskStatus = new LinkedHashMap<>();

    @JsonProperty("itemId")
    private String itemId;

    @JsonProperty("message")
    protected String message;

    @JsonProperty("globalStatus")
    private StatusCode globalStatus;

    @JsonProperty("statusMeter")
    private List<Integer> statusMeter;

    @JsonProperty("data")
    protected Map<String, Object> data;

    @JsonProperty("globalState")
    private ProcessState globalState;

    @JsonProperty("globalOutcomeDetailSubcode")
    private String globalOutcomeDetailSubcode;

    @JsonProperty("lifecycleEnable")
    private boolean lifecycleEnable = true;

    @JsonIgnore
    private String logbookTypeProcess;

    public ItemStatus() {
        statusMeter = new ArrayList<>();
        for (int i = StatusCode.UNKNOWN.getStatusLevel(); i <= StatusCode.FATAL.getStatusLevel(); i++) {
            statusMeter.add(0);
        }

        globalStatus = StatusCode.UNKNOWN;
        data = new HashMap<>();
    }

    public ItemStatus(
        @JsonProperty("itemId") String itemId,
        @JsonProperty("message") String message,
        @JsonProperty("globalStatus") StatusCode globalStatus,
        @JsonProperty("statusMeter") List<Integer> statusMeter,
        @JsonProperty("data") Map<String, Object> data,
        @JsonProperty("itemsStatus") LinkedHashMap<String, ItemStatus> itemsStatus,
        @JsonProperty("globalState") ProcessState globalState
    ) {
        this.itemsStatus = itemsStatus;
        this.itemId = itemId;
        this.message = message;
        this.globalStatus = globalStatus;
        this.statusMeter = statusMeter;
        this.data = data;
        this.globalState = globalState;
    }

    /**
     * Constructor
     *
     * @param itemId
     */
    public ItemStatus(String itemId) {
        this();
        this.itemId = itemId;
    }

    /**
     * @return the message
     */
    public String getMessage() {
        if (Strings.isNullOrEmpty(message)) {
            return "";
        }
        return message;
    }

    /**
     * @param message the message to set
     * @return this
     */
    public ItemStatus setMessage(String message) {
        ParametersChecker.checkParameter(MANDATORY_PARAMETER, message);
        this.message = message;
        return this;
    }

    /**
     * @return the itemId
     */
    public String getItemId() {
        if (Strings.isNullOrEmpty(itemId)) {
            return "";
        }
        return itemId;
    }

    /**
     * @param itemId the itemId to set
     * @return this
     */
    public ItemStatus setItemId(String itemId) {
        ParametersChecker.checkParameter(MANDATORY_PARAMETER, itemId);
        this.itemId = itemId;
        return this;
    }

    /**
     * @return the statusMeter
     */
    public List<Integer> getStatusMeter() {
        return statusMeter;
    }

    /**
     * @param statusCode the statusCode to increment
     * @return this
     */
    public ItemStatus increment(StatusCode statusCode) {
        return increment(statusCode, 1);
    }

    /**
     * @param statusCode the statusCode to increment
     * @param increment
     * @return this
     */
    public ItemStatus increment(StatusCode statusCode, int increment) {
        ParametersChecker.checkParameter(MANDATORY_PARAMETER, statusCode);
        // update statusMeter
        statusMeter.set(statusCode.getStatusLevel(), increment + statusMeter.get(statusCode.getStatusLevel()));
        // update globalStatus
        globalStatus = globalStatus.compareTo(statusCode) > 0 ? globalStatus : statusCode;

        return this;
    }

    /**
     * @param itemStatus1 the statusCode to increment
     * @param itemStatus2
     * @return this
     */
    protected ItemStatus increment(ItemStatus itemStatus1, ItemStatus itemStatus2) {
        ParametersChecker.checkParameter(MANDATORY_PARAMETER, itemStatus1, itemStatus2);
        // update statusMeter
        for (int i = StatusCode.UNKNOWN.getStatusLevel(); i <= StatusCode.FATAL.getStatusLevel(); i++) {
            itemStatus1
                .getStatusMeter()
                .set(i, itemStatus1.getStatusMeter().get(i) + itemStatus2.getStatusMeter().get(i));
        }
        // update globalStatus
        itemStatus1.setGlobalStatus(
            itemStatus1.getGlobalStatus().compareTo(itemStatus2.getGlobalStatus()) >= 1
                ? itemStatus1.getGlobalStatus()
                : itemStatus2.getGlobalStatus()
        );

        // update itemStatus
        Set<String> keySet1 = itemStatus1.getItemsStatus().keySet();
        Set<String> keySet2 = itemStatus2.getItemsStatus().keySet();
        for (String key : keySet2) {
            if (keySet1.contains(key) && !key.equals(itemStatus1.getItemId())) {
                itemStatus1
                    .getItemsStatus()
                    .put(key, increment(itemStatus1.getItemsStatus().get(key), itemStatus2.getItemsStatus().get(key)));
            }
        }

        if (
            itemStatus2.getGlobalOutcomeDetailSubcode() != null && itemStatus1.getGlobalOutcomeDetailSubcode() == null
        ) {
            itemStatus1.setGlobalOutcomeDetailSubcode(itemStatus2.getGlobalOutcomeDetailSubcode());
        }

        return itemStatus1;
    }

    /**
     * @return the globalStatus
     */
    public StatusCode getGlobalStatus() {
        return globalStatus;
    }

    /**
     * @param globalStatus the globalStatus to set
     * @return this
     */
    private ItemStatus setGlobalStatus(StatusCode globalStatus) {
        ParametersChecker.checkParameter(MANDATORY_PARAMETER, globalStatus);
        this.globalStatus = globalStatus;
        return this;
    }

    /**
     * @return the data
     */
    public Object getData(String key) {
        return data.get(key);
    }

    /**
     * @return
     * @Deprecated use getData(Sting key)
     */
    @Deprecated
    public Map<String, Object> getData() {
        return data;
    }

    /**
     * @param key
     * @param value
     * @return this
     */
    public ItemStatus setData(String key, Object value) {
        if (EVENT_DETAIL_DATA.equals(key)) {
            throw new IllegalArgumentException("Invalid key, Use apropriate methode  setEvDetailData ");
        }
        data.put(key, value);
        return this;
    }

    /**
     * @return masterData
     */
    @JsonIgnore
    public Map<String, Object> getMasterData() {
        return (Map<String, Object>) data.get(MASTER_DATA);
    }

    @JsonIgnore
    public ItemStatus setMasterData(String key, Object value) {
        data.computeIfAbsent(MASTER_DATA, k -> new HashMap<>());
        ((Map) data.get(MASTER_DATA)).put(key, value);
        return this;
    }

    /**
     * @return String message
     */
    public String computeStatusMeterMessage() {
        final StringBuilder computeMessage = new StringBuilder();
        final StatusCode[] statusCodes = StatusCode.values();
        for (int i = StatusCode.UNKNOWN.getStatusLevel(); i <= StatusCode.FATAL.getStatusLevel(); i++) {
            if (statusMeter.get(i) > 0) {
                computeMessage.append(" ").append(statusCodes[i]).append(":").append(statusMeter.get(i));
            }
        }
        return computeMessage.toString();
    }

    /**
     * @return the itemsStatus
     */
    public Map<String, ItemStatus> getItemsStatus() {
        return itemsStatus;
    }

    public ItemStatus setItemsStatus(String itemId, ItemStatus statusDetails) {
        return setItemsStatus(itemId, statusDetails, StatusAggregationBehavior.DEFAULT);
    }

    public ItemStatus setItemsStatus(
        String itemId,
        ItemStatus statusDetails,
        StatusAggregationBehavior statusAggregationBehavior
    ) {
        ParametersChecker.checkParameter(MANDATORY_PARAMETER, itemId, statusDetails);
        // update itemStatus

        if (itemsStatus.containsKey(itemId)) {
            itemsStatus.put(itemId, increment(itemsStatus.get(itemId), statusDetails));
        } else {
            itemsStatus.put(itemId, statusDetails);
        }

        // update globalStatus
        if (
            StatusAggregationBehavior.KO_CAUSES_WARNING.equals(statusAggregationBehavior) &&
            KO.equals(statusDetails.getGlobalStatus())
        ) {
            globalStatus = WARNING;
        } else {
            globalStatus = globalStatus.compareTo(statusDetails.getGlobalStatus()) > 0
                ? globalStatus
                : statusDetails.getGlobalStatus();
        }

        // update statusMeter
        for (int i = StatusCode.UNKNOWN.getStatusLevel(); i <= StatusCode.FATAL.getStatusLevel(); i++) {
            statusMeter.set(i, statusMeter.get(i) + statusDetails.getStatusMeter().get(i));
        }

        if (statusDetails.getData() != null) {
            computeEvDetData(statusDetails);
        }
        // update MasterData Map
        if (statusDetails.getMasterData() != null) {
            computeMasterData(statusDetails);
        }
        if (statusDetails.getGlobalOutcomeDetailSubcode() != null) {
            globalOutcomeDetailSubcode = statusDetails.getGlobalOutcomeDetailSubcode();
        }

        return this;
    }

    /**
     * @param compositeItemStatus
     * @return this
     */
    public ItemStatus setItemsStatus(ItemStatus compositeItemStatus) {
        ParametersChecker.checkParameter(MANDATORY_PARAMETER, compositeItemStatus);
        // update statusMeter, globalStatus
        // As now we bulk in worker, we have to increment by status meter of the current status code
        increment(
            compositeItemStatus.getGlobalStatus(),
            compositeItemStatus.getStatusMeter().get(compositeItemStatus.getGlobalStatus().ordinal())
        );

        if (compositeItemStatus.getItemsStatus() != null && !compositeItemStatus.getItemsStatus().isEmpty()) {
            // update itemStatus
            for (final Entry<String, ItemStatus> itemStatus : compositeItemStatus.getItemsStatus().entrySet()) {
                final String key = itemStatus.getKey();
                final ItemStatus value = itemStatus.getValue();
                final ItemStatus is = itemsStatus.get(key);

                if (is != null) {
                    if (
                        value.getGlobalStatus().isGreaterOrEqualToKo() &&
                        null != value.getData() &&
                        value.getData().size() > 0
                    ) {
                        is.getData().putAll(value.getData());
                    }

                    itemsStatus.put(key, increment(is, value));
                } else {
                    itemsStatus.put(itemStatus.getKey(), itemStatus.getValue());
                }
            }
            // update data Map
            if (compositeItemStatus.getData() != null) {
                computeEvDetData(compositeItemStatus);
            }
            // update MasterData Map
            if (compositeItemStatus.getMasterData() != null) {
                computeMasterData(compositeItemStatus);
            }
            if (compositeItemStatus.getGlobalOutcomeDetailSubcode() != null) {
                globalOutcomeDetailSubcode = compositeItemStatus.getGlobalOutcomeDetailSubcode();
            }
        }
        return this;
    }

    /**
     * Get the global state
     *
     * @return globalState as type ProcessState
     */
    public ProcessState getGlobalState() {
        return globalState;
    }

    /**
     * @param globalState the golbal state to set
     * @return this
     */
    public ItemStatus setGlobalState(ProcessState globalState) {
        ParametersChecker.checkParameter(MANDATORY_PARAMETER, globalState);
        this.globalState = globalState;
        return this;
    }

    /**
     * @param blocking True if the step or handler is blocking
     * @return True if this item shall stop the Step or Handler
     */
    @JsonIgnore
    public boolean shallStop(boolean blocking) {
        return getGlobalStatus().isGreaterOrEqualToFatal() || (blocking && getGlobalStatus().isGreaterOrEqualToKo());
    }

    /**
     * @return the subTaskStatus
     */
    public LinkedHashMap<String, ItemStatus> getSubTaskStatus() {
        return subTaskStatus;
    }

    /**
     * @param taskId the taskId to set
     * @param taskStatus the taskStatus to set
     * @return this
     */
    public ItemStatus setSubTaskStatus(String taskId, ItemStatus taskStatus) {
        ParametersChecker.checkParameterDefault("taskId", taskId);
        this.subTaskStatus.put(taskId, taskStatus);
        return this;
    }

    public ItemStatus setSubTasksStatus(Map<String, ItemStatus> tasks) {
        ParametersChecker.checkParameterDefault("taskId", "TaskId is required");
        this.subTaskStatus.putAll(tasks);
        return this;
    }

    /**
     * @return evDetailData
     */
    @JsonIgnore
    public String getEvDetailData() {
        String evDetailData = (String) data.get(EVENT_DETAIL_DATA);
        if (Strings.isNullOrEmpty(evDetailData)) {
            return "{}";
        }
        return evDetailData;
    }

    /**
     * set EvDetailData
     *
     * @param evDetailData
     * @return this
     */
    @JsonIgnore
    public ItemStatus setEvDetailData(String evDetailData) {
        ParametersChecker.checkParameterDefault("evDetailData", evDetailData);
        try {
            JsonHandler.validate(evDetailData);
        } catch (InvalidParseOperationException e) {
            throw new IllegalArgumentException("Value of eventDetailData has to be a Valid Json", e);
        }
        data.put(EVENT_DETAIL_DATA, evDetailData);
        return this;
    }

    public String getLogbookTypeProcess() {
        return logbookTypeProcess;
    }

    public ItemStatus setLogbookTypeProcess(String logbookTypeProcess) {
        this.logbookTypeProcess = logbookTypeProcess;
        return this;
    }

    private void computeEvDetData(ItemStatus statusDetails) {
        String detailDataString = "";

        if (!Strings.isNullOrEmpty(statusDetails.getEvDetailData()) && data.containsKey(EVENT_DETAIL_DATA)) {
            try {
                ObjectNode subDetailData = (ObjectNode) JsonHandler.getFromString(statusDetails.getEvDetailData());
                ObjectNode detailData = (ObjectNode) JsonHandler.getFromString((String) data.get(EVENT_DETAIL_DATA));
                subDetailData.setAll(detailData);
                detailDataString = JsonHandler.unprettyPrint(subDetailData);
            } catch (InvalidParseOperationException e) {
                throw new IllegalArgumentException("value of eventDetailData has to be a Valid Json");
            }
        }
        data.putAll(statusDetails.getData());
        if (!detailDataString.isEmpty()) {
            data.put(EVENT_DETAIL_DATA, detailDataString);
        }
    }

    private void computeMasterData(ItemStatus statusDetails) {
        String detailDataString = "";
        if (
            statusDetails.getMasterData().containsKey(EVENT_DETAIL_DATA) &&
            getMasterData().containsKey(EVENT_DETAIL_DATA)
        ) {
            try {
                ObjectNode subDetailData = (ObjectNode) JsonHandler.getFromString(
                    (String) statusDetails.getMasterData().get(EVENT_DETAIL_DATA)
                );
                ObjectNode detailData = (ObjectNode) JsonHandler.getFromString(
                    (String) getMasterData().get(EVENT_DETAIL_DATA)
                );
                subDetailData.setAll(detailData);
                detailDataString = JsonHandler.unprettyPrint(subDetailData);
            } catch (InvalidParseOperationException e) {
                throw new IllegalArgumentException("value of eventDetailData has to be a Valid Json");
            }
        }
        getMasterData().putAll(statusDetails.getMasterData());
        if (!detailDataString.isEmpty()) {
            getMasterData().put(EVENT_DETAIL_DATA, detailDataString);
        }
    }

    public String getGlobalOutcomeDetailSubcode() {
        return globalOutcomeDetailSubcode;
    }

    public ItemStatus setGlobalOutcomeDetailSubcode(String globalOutcomeDetailSubcode) {
        this.globalOutcomeDetailSubcode = globalOutcomeDetailSubcode;
        return this;
    }

    public boolean isLifecycleEnable() {
        return lifecycleEnable;
    }

    public ItemStatus disableLfc() {
        this.lifecycleEnable = false;
        return this;
    }

    /**
     * Set the value of the specified statusCode in this statusMeter
     *
     * @param statusCode
     * @param value
     */
    public void setStatusMeterValue(StatusCode statusCode, Integer value) {
        this.statusMeter.set(statusCode.getStatusLevel(), value);
    }

    /**
     * Reinit all the values of this statusMeter to 0
     */
    public void reinitStatusMeter() {
        for (int i = 0; i < this.statusMeter.size(); i++) {
            this.statusMeter.set(i, 0);
        }
    }

    public void clearStatusMeterFatal() {
        this.statusMeter.set(StatusCode.FATAL.ordinal(), 0);
        globalStatus = StatusCode.STARTED;

        for (StatusCode statusCode : StatusCode.values()) {
            Integer count = this.statusMeter.get(statusCode.ordinal());
            if (count > 0) {
                globalStatus = globalStatus.compareTo(statusCode) > 0 ? globalStatus : statusCode;
            }
        }

        for (ItemStatus is : itemsStatus.values()) {
            is.clearStatusMeterFatal();
            globalStatus = globalStatus.compareTo(is.globalStatus) > 0 ? globalStatus : is.globalStatus;
        }

        for (ItemStatus is : subTaskStatus.values()) {
            is.clearStatusMeterFatal();
            globalStatus = globalStatus.compareTo(is.globalStatus) > 0 ? globalStatus : is.globalStatus;
        }
    }
}

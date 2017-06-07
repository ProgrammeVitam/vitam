/**
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
package fr.gouv.vitam.common.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;

import fr.gouv.vitam.common.ParametersChecker;

/**
 * Composite Item Status
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public class ItemStatus {

    private static final String MANDATORY_PARAMETER = "Mandatory parameter";
    @JsonProperty("itemsStatus")
    private LinkedHashMap<String, ItemStatus> itemsStatus = new LinkedHashMap<>();
    private LinkedHashMap<String, ItemStatus> subTaskStatus = new LinkedHashMap<>();

    @JsonProperty("itemId")
    protected String itemId;
    @JsonProperty("message")
    protected String message;
    @JsonProperty("evDetailData")
    protected String evDetailData;
    @JsonProperty("globalStatus")
    protected StatusCode globalStatus;
    @JsonProperty("statusMeter")
    protected List<Integer> statusMeter;
    @JsonProperty("data")
    protected Map<String, Object> data;
    @JsonProperty("globalState")
    protected ProcessState globalState;

    @JsonIgnore
    private String logbookTypeProcess;

    /**
     * Empty Constructor
     */
    public ItemStatus() {
        statusMeter = new ArrayList<>();
        for (int i = StatusCode.UNKNOWN.getStatusLevel(); i <= StatusCode.FATAL.getStatusLevel(); i++) {
            statusMeter.add(0);
        }

        globalStatus = StatusCode.UNKNOWN;
        data = new HashMap<>();
    }

    /**
     * @param message
     * @param itemId
     * @param statusMeter
     * @param globalStatus
     * @param data
     * @param itemsStatus
     * @param evDetailData
     * @param globalState
     */
    public ItemStatus(@JsonProperty("itemId") String itemId, @JsonProperty("message") String message,
        @JsonProperty("globalStatus") StatusCode globalStatus,
        @JsonProperty("statusMeter") List<Integer> statusMeter, @JsonProperty("data") Map<String, Object> data,
        @JsonProperty("itemsStatus") LinkedHashMap<String, ItemStatus> itemsStatus,
        @JsonProperty("evDetailData") String evDetailData,
        @JsonProperty("globalState") ProcessState globalState) {
        this.itemsStatus = itemsStatus;
        this.itemId = itemId;
        this.message = message;
        this.globalStatus = globalStatus;
        this.statusMeter = statusMeter;
        this.data = data;
        this.evDetailData = evDetailData;
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
     *
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
     *
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
     *
     * @return this
     */
    public ItemStatus increment(StatusCode statusCode) {

        return increment(statusCode, 1);
    }

    /**
     * @param statusCode the statusCode to increment
     * @param increment
     *
     * @return this
     */
    public ItemStatus increment(StatusCode statusCode, int increment) {
        ParametersChecker.checkParameter(MANDATORY_PARAMETER, statusCode);
        // update statusMeter
        statusMeter.set(statusCode.getStatusLevel(),
            increment + statusMeter.get(statusCode.getStatusLevel()));
        // update globalStatus
        globalStatus = globalStatus.compareTo(statusCode) > 0
            ? globalStatus : statusCode;

        return this;
    }

    /**
     * @param itemStatus1 the statusCode to increment
     * @param itemStatus2
     *
     * @return this
     */
    protected ItemStatus increment(ItemStatus itemStatus1, ItemStatus itemStatus2) {
        ParametersChecker.checkParameter(MANDATORY_PARAMETER, itemStatus1, itemStatus2);
        // update statusMeter
        for (int i = StatusCode.UNKNOWN.getStatusLevel(); i <= StatusCode.FATAL.getStatusLevel(); i++) {
            itemStatus1.getStatusMeter().set(i,
                itemStatus1.getStatusMeter().get(i) + itemStatus2.getStatusMeter().get(i));
        }
        // update globalStatus
        itemStatus1.setGlobalStatus(itemStatus1.getGlobalStatus().compareTo(itemStatus2.getGlobalStatus()) >= 1
            ? itemStatus1.getGlobalStatus() : itemStatus2.getGlobalStatus());

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
     *
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
    public Map<String, Object> getData() {
        return data;
    }

    /**
     * @param key
     * @param value
     * @return this
     */
    public ItemStatus setData(String key, Object value) {
        data.put(key, value);
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

    /**
     * @param itemId
     * @param statusDetails
     *
     * @return this
     */
    public ItemStatus setItemsStatus(String itemId, ItemStatus statusDetails) {

        ParametersChecker.checkParameter(MANDATORY_PARAMETER, itemId, statusDetails);
        // update itemStatus

        if (itemsStatus.containsKey(itemId)) {
            itemsStatus.put(itemId, increment(itemsStatus.get(itemId), statusDetails));
        } else {
            itemsStatus.put(itemId, statusDetails);
        }

        // update globalStatus
        globalStatus = globalStatus.compareTo(statusDetails.getGlobalStatus()) > 0
            ? globalStatus : statusDetails.getGlobalStatus();
        // update statusMeter
        for (int i = StatusCode.UNKNOWN.getStatusLevel(); i <= StatusCode.FATAL.getStatusLevel(); i++) {
            statusMeter.set(i, statusMeter.get(i) + statusDetails.getStatusMeter().get(i));
        }

        if (statusDetails.getData() != null) {
            data.putAll(statusDetails.getData());
        }

        return this;
    }

    /**
     * @param compositeItemStatus
     *
     * @return this
     */
    public ItemStatus setItemsStatus(ItemStatus compositeItemStatus) {

        ParametersChecker.checkParameter(MANDATORY_PARAMETER, compositeItemStatus);
        if (compositeItemStatus.getItemsStatus() != null && !compositeItemStatus.getItemsStatus().isEmpty()) {
            // update statusMeter, globalStatus
            increment(compositeItemStatus.getGlobalStatus());

            // update itemStatus
            for (final Entry<String, ItemStatus> itemStatus : compositeItemStatus.getItemsStatus()
                .entrySet()) {
                if (itemsStatus.containsKey(itemStatus.getKey())) {
                    itemsStatus.put(itemStatus.getKey(),
                        increment(itemsStatus.get(itemStatus.getKey()), itemStatus.getValue()));
                } else {
                    itemsStatus.put(itemStatus.getKey(), itemStatus.getValue());
                }
            }
            // update data Map
            if (compositeItemStatus.getData() != null) {
                data.putAll(compositeItemStatus.getData());
            }
        }
        return this;
    }

    /**
     * Get the global state
     * @return
     */
    public ProcessState getGlobalState() {
        return globalState;
    }

    /**
     * @param globalState the golbal state to set
     *
     * @return this
     */
    public ItemStatus setGlobalState(ProcessState globalState) {
        ParametersChecker.checkParameter(MANDATORY_PARAMETER, globalState);
        this.globalState = globalState;
        return this;
    }

    /**
     *
     * @param blocking True if the step or handler is blocking
     * @return True if this item shall stop the Step or Handler
     */
    @JsonIgnore
    public boolean shallStop(boolean blocking) {
        return getGlobalStatus().isGreaterOrEqualToFatal() ||
            blocking && getGlobalStatus().isGreaterOrEqualToKo();
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
     *
     * @return this
     */
    public ItemStatus setSubTaskStatus(String taskId, ItemStatus taskStatus) {
        ParametersChecker.checkParameterDefault("taskId", taskId);
        this.subTaskStatus.put(taskId, taskStatus);
        return this;
    }

    /**
     * @return evDetailData
     */
    public String getEvDetailData() {
        if (Strings.isNullOrEmpty(evDetailData)) {
            return "";
        }
        return evDetailData;
    }

    /**
     * set EvDetailData
     *
     * @param evDetailData
     * @return this
     */
    public ItemStatus setEvDetailData(String evDetailData) {
        ParametersChecker.checkParameterDefault("evDetailData", evDetailData);
        this.evDetailData = evDetailData;
        return this;
    }

    public String getLogbookTypeProcess() {
        return logbookTypeProcess;
    }

    public ItemStatus setLogbookTypeProcess(String logbookTypeProcess) {
        this.logbookTypeProcess = logbookTypeProcess;
        return this;
    }
}

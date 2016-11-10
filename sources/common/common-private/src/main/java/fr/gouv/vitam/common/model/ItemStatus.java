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
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;

import fr.gouv.vitam.common.ParametersChecker;

/**
 * Item Status (Step, Action, ...)
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public class ItemStatus {

    private static final String MANDATORY_PARAMETER = "Mandatory parameter";

    @JsonProperty("itemId")
    protected String itemId;
    @JsonProperty("message")
    protected String message;
    @JsonProperty("globalStatus")
    protected StatusCode globalStatus;
    @JsonProperty("statusMeter")
    protected List<Integer> statusMeter;
    @JsonProperty("data")
    protected Map<String, Object> data;



    /**
     * @param message
     * @param itemId
     * @param statusMeter
     * @param globalStatus
     * @param data
     */
    @JsonCreator
    public ItemStatus(@JsonProperty("itemId") String itemId,
        @JsonProperty("message") String message,
        @JsonProperty("globalStatus") StatusCode globalStatus,
        @JsonProperty("statusMeter") List<Integer> statusMeter,
        @JsonProperty("data") Map<String, Object> data) {

        this.itemId = itemId;
        this.message = message;
        this.globalStatus = globalStatus;
        this.statusMeter = statusMeter;
        this.data = data;
    }

    /**
     * Constructor
     * 
     * @param itemId
     */
    public ItemStatus(String itemId) {
        statusMeter = new ArrayList<Integer>();
        for (int i = StatusCode.UNKNOWN.getStatusLevel(); i <= StatusCode.FATAL.getStatusLevel(); i++) {
            statusMeter.add(0);
        }

        this.globalStatus = StatusCode.UNKNOWN;
        this.data = new HashMap<>();
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
     * @param statusCode the statusCode to increment
     * @param increment
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
        itemStatus1.setGlobalStatus(itemStatus1.getGlobalStatus().compareTo(itemStatus2.getGlobalStatus()) > 1
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
        this.data.put(key, value);
        return this;
    }

    /**
     * @return String message
     */
    public String computeStatusMeterMessage() {
        StringBuilder computeMessage = new StringBuilder();
        StatusCode[] statusCodes = StatusCode.values();
        for (int i = StatusCode.UNKNOWN.getStatusLevel(); i <= StatusCode.FATAL.getStatusLevel(); i++) {
            if (statusMeter.get(i) > 0) {
                computeMessage.append(" ").append(statusCodes[i]).append(":").append(statusMeter.get(i));
            }
        }
        return computeMessage.toString();
    }


}

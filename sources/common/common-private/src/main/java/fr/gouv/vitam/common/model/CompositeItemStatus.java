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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import fr.gouv.vitam.common.ParametersChecker;

/**
 * Composite Item Status
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public class CompositeItemStatus extends ItemStatus {

    private static final String MANDATORY_PARAMETER = "Mandatory parameter";
    // FIXME P0 should be Map of CompositeItemStatus
    @JsonProperty("itemsStatus")
    private LinkedHashMap<String, ItemStatus> itemsStatus = new LinkedHashMap<>();

    /**
     * @param message
     * @param itemId
     * @param statusMeter
     * @param globalStatus
     * @param data
     * @param itemsStatus
     */
    @JsonCreator
    public CompositeItemStatus(@JsonProperty("itemId") String itemId, @JsonProperty("message") String message,
        @JsonProperty("globalStatus") StatusCode globalStatus,
        @JsonProperty("statusMeter") List<Integer> statusMeter, @JsonProperty("data") Map<String, Object> data,
        @JsonProperty("itemsStatus") LinkedHashMap<String, ItemStatus> itemsStatus) {
        super(itemId, message, globalStatus, statusMeter, data);
        this.itemsStatus = itemsStatus;

    }

    /**
     * Constructor
     * 
     * @param itemId
     */
    public CompositeItemStatus(String itemId) {
        super(itemId);
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
    public CompositeItemStatus setItemsStatus(String itemId, ItemStatus statusDetails) {

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
            this.data.putAll(statusDetails.getData());
        }

        return this;
    }

    /**
     * @param compositeItemStatus
     *
     * @return this
     */
    public CompositeItemStatus setItemsStatus(CompositeItemStatus compositeItemStatus) {

        ParametersChecker.checkParameter(MANDATORY_PARAMETER, compositeItemStatus);
        if ((compositeItemStatus.getItemsStatus() != null) && (!compositeItemStatus.getItemsStatus().isEmpty())) {
            // update statusMeter, globalStatus
            increment(compositeItemStatus.getGlobalStatus());

            // update itemStatus
            for (final Entry<String, ItemStatus> itemStatus : compositeItemStatus.getItemsStatus().entrySet()) {
                if (itemsStatus.containsKey(itemStatus.getKey())) {
                    itemsStatus.put(itemStatus.getKey(),
                        increment(itemsStatus.get(itemStatus.getKey()), itemStatus.getValue()));
                } else {
                    itemsStatus.put(itemStatus.getKey(), itemStatus.getValue());
                }
            }
            // update data Map
            if (compositeItemStatus.getData() != null) {
                this.data.putAll(compositeItemStatus.getData());
            }
        }
        return this;
    }

}

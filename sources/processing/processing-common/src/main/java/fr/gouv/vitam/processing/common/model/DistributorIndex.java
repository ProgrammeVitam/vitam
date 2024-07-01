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
package fr.gouv.vitam.processing.common.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import fr.gouv.vitam.common.model.ItemStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Composite Item Status
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public class DistributorIndex {

    /**
     * By default when not level exists, the value is _no_level
     */
    private String level = "_no_level";

    /**
     * Ths index in the current elements list
     */
    private int offset;
    /**
     * The latest itemStatus
     */
    private ItemStatus itemStatus;
    private String requestId;
    private String stepId;
    /**
     * When pause occurs, somme elements may not be executed
     * To be sure that when recover those elements should be executed
     * the offset is not sufficient as in the batch size of 10
     * we can have 2 elements finished and 8 not finished
     * With only offset the 2 finished elements will be executed after recover,
     * But adding information of remaining elements, the 2 finished elements will be skipped
     */
    private List<String> remainingElements;
    /**
     * This boolean is equivalent to offset >= elements list size
     */
    private boolean levelFinished = false;

    public DistributorIndex() {}

    public DistributorIndex(
        String level,
        int offset,
        ItemStatus itemStatus,
        String requestId,
        String stepId,
        List<String> remainingElements
    ) {
        this.level = level;
        this.offset = offset;
        this.itemStatus = itemStatus;
        this.requestId = requestId;
        this.stepId = stepId;
        this.remainingElements = remainingElements;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public ItemStatus getItemStatus() {
        return itemStatus;
    }

    public void setItemStatus(ItemStatus itemStatus) {
        this.itemStatus = itemStatus;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getStepId() {
        return stepId;
    }

    public void setStepId(String stepId) {
        this.stepId = stepId;
    }

    public List<String> getRemainingElements() {
        return remainingElements == null ? new ArrayList<>() : remainingElements;
    }

    public void setRemainingElements(List<String> remainingElements) {
        this.remainingElements = remainingElements;
    }

    public boolean isLevelFinished() {
        return levelFinished;
    }

    public void setLevelFinished(boolean levelFinished) {
        this.levelFinished = levelFinished;
    }
}

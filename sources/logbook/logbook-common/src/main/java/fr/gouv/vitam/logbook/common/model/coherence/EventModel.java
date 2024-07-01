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
package fr.gouv.vitam.logbook.common.model.coherence;

/**
 * Description of the logbook event model. <br/>
 */
public class EventModel {

    /**
     * Logbook event type
     */
    private LogbookEventType logbookEventType;

    /**
     * Operation identifer.
     */
    private String operationId;

    /**
     * Unit or Got lifeCycle identifer.
     */
    private String lfcId;

    /**
     * evId.
     */
    private String evId;

    /**
     * evParentId.
     */
    private String evParentId;

    /**
     * evType.
     */
    private String evType;

    /**
     * evTypeParent.
     */
    private String evTypeParent;

    /**
     * outcome.
     */
    private String outcome;

    /**
     * outcome details.
     */
    private String outDetail;

    /**
     * Default constructor/
     */
    public EventModel() {}

    /**
     * EventModel constructor.
     *
     * @param logbookEventType
     * @param operationId
     * @param lfcId
     * @param evId
     * @param evParentId
     * @param evType
     * @param evTypeParent
     * @param outcome
     * @param outDetail
     */
    public EventModel(
        LogbookEventType logbookEventType,
        String operationId,
        String lfcId,
        String evId,
        String evParentId,
        String evType,
        String evTypeParent,
        String outcome,
        String outDetail
    ) {
        this.logbookEventType = logbookEventType;
        this.operationId = operationId;
        this.lfcId = lfcId;
        this.evId = evId;
        this.evParentId = evParentId;
        this.evType = evType;
        this.evTypeParent = evTypeParent;
        this.outcome = outcome;
        this.outDetail = outDetail;
    }

    public LogbookEventType getLogbookEventType() {
        return logbookEventType;
    }

    public void setLogbookEventType(LogbookEventType logbookEventType) {
        this.logbookEventType = logbookEventType;
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public String getLfcId() {
        return lfcId;
    }

    public void setLfcId(String lfcId) {
        this.lfcId = lfcId;
    }

    public String getEvId() {
        return evId;
    }

    public void setEvId(String evId) {
        this.evId = evId;
    }

    public String getEvParentId() {
        return evParentId;
    }

    public void setEvParentId(String evParentId) {
        this.evParentId = evParentId;
    }

    public String getEvType() {
        return evType;
    }

    public void setEvType(String evType) {
        this.evType = evType;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public String getOutDetail() {
        return outDetail;
    }

    public void setOutDetail(String outDetail) {
        this.outDetail = outDetail;
    }

    public String getEvTypeParent() {
        return evTypeParent;
    }

    public void setEvTypeParent(String evTypeParent) {
        this.evTypeParent = evTypeParent;
    }
}

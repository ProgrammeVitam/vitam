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
package fr.gouv.vitam.ingest.internal.api.response;

import java.util.ArrayList;
import java.util.List;

/**
 * Class Ingest Reponse
 */
public class IngestResponse {

    private String cause;
    private String message;
    private String containerGuid;
    private String sedaGuid;

    /*
     * if error occured during any process ( unzip, seda anlysis ..)
     */
    private Boolean errorOccured;
    private Long numberOfNumericalObject;
    // number of elements in the received SIP
    private Long numberOfElements;

    private final List<String> guidNumObjList = new ArrayList<>();

    /**
     * @return the numberOfElements in a received SIP
     */
    public Long getNumberOfElements() {
        return numberOfElements;
    }

    /**
     * @param numberOfElements the numberOfElements to set from a received SIP
     * @return the current instance of IngestResponse
     */
    public IngestResponse setNumberOfElements(Long numberOfElements) {
        this.numberOfElements = numberOfElements;
        return this;
    }

    /**
     * @return the cause
     */
    public String getCause() {
        return cause;
    }

    /**
     * @param cause the cause to set
     * @return this
     */
    public IngestResponse setCause(String cause) {
        this.cause = cause;
        return this;
    }

    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * @param message the message to set
     * @return this
     */
    public IngestResponse setMessage(String message) {
        this.message = message;
        return this;
    }

    /**
     * @return the errorOccured
     */
    public Boolean getErrorOccured() {
        return errorOccured;
    }

    /**
     * @param errorOccured the errorOccured to set
     * @return this
     */
    public IngestResponse setErrorOccured(Boolean errorOccured) {
        this.errorOccured = errorOccured;
        return this;
    }

    /**
     * @return the NumericalObject in a received SIP
     */
    public Long getNumericalObject() {
        return numberOfNumericalObject;
    }

    /**
     * @param numberOfNumericalObject the NumericalObject to set from a received SIP
     * @return this
     */
    public IngestResponse setNumericalObject(Long numberOfNumericalObject) {
        this.numberOfNumericalObject = numberOfNumericalObject;
        return this;
    }

    /**
     * @return the containerGuid
     */
    public String getContainerGuid() {
        return containerGuid;
    }

    /**
     * @param containerGuid the containerGuid to set
     * @return this
     */
    public IngestResponse setContainerGuid(String containerGuid) {
        this.containerGuid = containerGuid;
        return this;
    }

    /**
     * @return the sedaGuid
     */
    public String getSedaGuid() {
        return sedaGuid;
    }

    /**
     * @param sedaGuid the sedaGuid to set
     * @return this
     */
    public IngestResponse setSedaGuid(String sedaGuid) {
        this.sedaGuid = sedaGuid;
        return this;
    }

    /**
     * @return the numberOfElements in a received SIP
     */
    public Long getNumberOfNumericalObject() {
        return numberOfNumericalObject;
    }

    /**
     * @param numberOfNumericalObject to set
     * @return this
     */
    public IngestResponse setNumberOfNumericalObject(Long numberOfNumericalObject) {
        this.numberOfNumericalObject = numberOfNumericalObject;
        return this;
    }

    /**
     * @return list of String
     */
    public List<String> getGuidNumObjList() {
        return guidNumObjList;
    }

    /**
     * Setter GuidNumObjList
     *
     * @param guidNumObjList to set
     * @return this
     */
    public IngestResponse setGuidNumObjList(List<String> guidNumObjList) {
        if (guidNumObjList != null && !guidNumObjList.isEmpty()) {
            this.guidNumObjList.addAll(guidNumObjList);
        }
        return this;
    }

    /**
     * @param guid to add in response
     * @return this
     */
    public IngestResponse addGuidNumericObject(String guid) {
        guidNumObjList.add(guid);
        return this;
    }

    /**
     * @return listGuid such as String
     */
    public String getListGuidObjectAsString() {
        return guidNumObjList.toString();
    }
}

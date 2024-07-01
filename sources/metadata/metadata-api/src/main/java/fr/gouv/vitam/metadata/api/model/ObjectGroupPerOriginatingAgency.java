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
package fr.gouv.vitam.metadata.api.model;

/**
 * ObjectGroupPerOriginatingAgency class describing ObjectGroup
 */
public class ObjectGroupPerOriginatingAgency {

    private String operation;

    private String agency;

    private long numberOfObject = 0L;

    private long numberOfGOT = 0L;

    private long size = 0L;

    /**
     * Constructor
     */
    public ObjectGroupPerOriginatingAgency() {
        // empty constructor
    }

    /**
     * Constructor
     *
     * @param operation operation id
     * @param agency originating agency
     * @param numberOfObject total of objects in the objects groups
     * @param numberOfGOT total of objects groups
     * @param size size of al objects
     */
    public ObjectGroupPerOriginatingAgency(
        String operation,
        String agency,
        long numberOfObject,
        long numberOfGOT,
        long size
    ) {
        this.operation = operation;
        this.agency = agency;
        this.numberOfObject = numberOfObject;
        this.numberOfGOT = numberOfGOT;
        this.size = size;
    }

    /**
     * Getter
     *
     * @return operation id
     */
    public String getOperation() {
        return operation;
    }

    /**
     * Setter
     *
     * @param operation
     */
    public void setOperation(String operation) {
        this.operation = operation;
    }

    /**
     * getNumberOfObject
     *
     * @return numberOfObject
     */
    public long getNumberOfObject() {
        return numberOfObject;
    }

    /**
     * setNumberOfObject
     *
     * @param numberOfObject
     */
    public void setNumberOfObject(long numberOfObject) {
        this.numberOfObject = numberOfObject;
    }

    /**
     * getNumberOfGOT
     *
     * @return numberOfGOT
     */
    public long getNumberOfGOT() {
        return numberOfGOT;
    }

    /**
     * setNumberOfGOT
     *
     * @param numberOfGOT
     */
    public void setNumberOfGOT(long numberOfGOT) {
        this.numberOfGOT = numberOfGOT;
    }

    /**
     * getSize
     *
     * @return size
     */
    public long getSize() {
        return size;
    }

    /**
     * setSize
     *
     * @param size
     */
    public void setSize(long size) {
        this.size = size;
    }

    /**
     * Getter
     *
     * @return agency
     */
    public String getAgency() {
        return agency;
    }

    /**
     * Setter
     *
     * @param agency
     */
    public ObjectGroupPerOriginatingAgency setAgency(String agency) {
        this.agency = agency;
        return this;
    }
}

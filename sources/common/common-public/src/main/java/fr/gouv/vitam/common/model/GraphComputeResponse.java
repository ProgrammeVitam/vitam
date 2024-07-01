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

/**
 * Response given when compute graph (Unit/GOT)
 */
public class GraphComputeResponse {

    public enum GraphComputeAction {
        /**
         * Compute graph only for Unit
         */
        UNIT,
        /**
         * Compute graph only for ObjectGroup
         */
        OBJECTGROUP,
        /**
         * Compute graph for Unit and there ObjectGroup
         */
        UNIT_OBJECTGROUP,
    }

    private Integer unitCount = 0;
    private Integer gotCount = 0;

    private String errorMessage;

    /**
     * Default constructor
     */
    public GraphComputeResponse() {}

    /**
     * Constructor
     *
     * @param unitCount
     * @param gotCount
     */
    public GraphComputeResponse(Integer unitCount, Integer gotCount) {
        this.unitCount = unitCount;
        this.gotCount = gotCount;
    }

    /**
     * Increment unit of got count
     *
     * @param graphComputeAction
     * @param count
     * @return GraphComputeResponse
     */
    public GraphComputeResponse increment(GraphComputeAction graphComputeAction, int count) {
        switch (graphComputeAction) {
            case UNIT:
                unitCount = unitCount + count;
                break;
            case OBJECTGROUP:
                gotCount = gotCount + count;
                break;
            default:
            // Nothing
        }

        return this;
    }

    /**
     * Increment unit and got count from given graphComputeResponse
     *
     * @param graphComputeResponse
     * @return GraphComputeResponse
     */
    public GraphComputeResponse increment(GraphComputeResponse graphComputeResponse) {
        unitCount = unitCount + graphComputeResponse.getUnitCount();
        gotCount = gotCount + graphComputeResponse.getGotCount();

        return this;
    }

    /**
     * Getter
     *
     * @return unitCount
     */
    public Integer getUnitCount() {
        return unitCount;
    }

    /**
     * Setter
     *
     * @param unitCount
     * @return GraphComputeResponse
     */
    public GraphComputeResponse setUnitCount(Integer unitCount) {
        this.unitCount = unitCount;
        return this;
    }

    /**
     * Getter
     *
     * @return gotCount
     */
    public Integer getGotCount() {
        return gotCount;
    }

    /**
     * Setter
     *
     * @param gotCount
     * @return GraphComputeResponse
     */
    public GraphComputeResponse setGotCount(Integer gotCount) {
        this.gotCount = gotCount;
        return this;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}

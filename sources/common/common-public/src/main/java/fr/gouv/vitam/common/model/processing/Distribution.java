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
package fr.gouv.vitam.common.model.processing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.common.model.StatusCode;

/**
 * Distribution object in each step of workflow processing
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class Distribution {

    @JsonProperty("kind")
    private DistributionKind kind;

    @JsonProperty("element")
    private String element;

    @JsonProperty("type")
    private DistributionType type;

    @JsonProperty("statusOnEmptyDistribution")
    private StatusCode statusOnEmptyDistribution = StatusCode.WARNING;

    @JsonProperty("bulkSize")
    private Integer bulkSize;

    /**
     * getKind(), get the object kind
     *
     * @return the reference of DistributionKind
     */
    public DistributionKind getKind() {
        if (kind == null) {
            return DistributionKind.REF;
        }
        return kind;
    }

    /**
     * setKind, set the kind of Distribution object
     *
     * @param kind of DistributionKind
     * @return Distribution object with kind setted
     */
    public Distribution setKind(DistributionKind kind) {
        this.kind = kind;
        return this;
    }

    /**
     * getElement(), return the element of Distribution
     *
     * @return the element as String
     */
    public String getElement() {
        if (element == null) {
            return "";
        }
        return element;
    }

    /**
     * setElement, set the value of element
     *
     * @param element as String
     * @return Distribution instance with element setted
     */
    public Distribution setElement(String element) {
        this.element = element;
        return this;
    }

    /**
     * get the type of the distribution
     *
     * @return Distribution instance with element setted
     */
    public DistributionType getType() {
        return type;
    }

    /**
     * set the type of the distribution
     *
     * @param type the type of the distribution
     * @return Distribution instance with element setted
     */
    public Distribution setType(DistributionType type) {
        this.type = type;
        return this;
    }

    /**
     * Get the status to be used in the logbook if no distribution occurred
     *
     * @return StatusCode
     */
    public StatusCode getStatusOnEmptyDistribution() {
        return statusOnEmptyDistribution;
    }

    /**
     * Set the status to be used in the logbook if no distribution occurred
     *
     * @param statusOnEmptyDistribution
     * @return Distribution instance with element setted
     */
    public Distribution setStatusOnEmptyDistribution(StatusCode statusOnEmptyDistribution) {
        this.statusOnEmptyDistribution = statusOnEmptyDistribution;
        return this;
    }

    /**
     * @return bulkSize
     */

    public Integer getBulkSize() {
        return bulkSize;
    }

    /**
     * @param bulkSize bulkSize
     * @return the distribution
     */
    public Distribution setBulkSize(Integer bulkSize) {
        this.bulkSize = bulkSize;
        return this;
    }
}

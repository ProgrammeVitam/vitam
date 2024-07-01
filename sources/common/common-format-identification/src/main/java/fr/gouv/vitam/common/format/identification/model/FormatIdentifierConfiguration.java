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
package fr.gouv.vitam.common.format.identification.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.common.format.identification.FormatIdentifierType;

import java.util.HashMap;
import java.util.Map;

/**
 * Format Identifier Configuration : contains all the parameters to instantiate a FormatIdentifier implementation. Only
 * the type is mandatory since it is used to choose the implementation class, all other properties are in
 * configurationProperties.
 */

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FormatIdentifierConfiguration {

    @JsonProperty("type")
    private FormatIdentifierType type;

    @JsonIgnore
    private final Map<String, Object> configurationProperties = new HashMap<>();

    /**
     * Get the type
     *
     * @return the type
     */
    @JsonProperty("type")
    public FormatIdentifierType getType() {
        return type;
    }

    /**
     * Set the type
     *
     * @param type the type
     */
    @JsonProperty("type")
    public void setType(FormatIdentifierType type) {
        this.type = type;
    }

    /**
     * Get the configuration properties
     *
     * @return map of configuration properties
     */
    @JsonAnyGetter
    public Map<String, Object> getConfigurationProperties() {
        return configurationProperties;
    }

    /**
     * Add a property to the configuration properties
     *
     * @param name the property name
     * @param value the property value
     */
    @JsonAnySetter
    public void setConfigurationProperty(String name, Object value) {
        configurationProperties.put(name, value);
    }
}

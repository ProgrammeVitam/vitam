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
package fr.gouv.vitam.common.security.rest;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.Objects;

/**
 * REST endpoint information
 */
public class EndpointInfo {

    @JsonProperty("permission")
    private String permission;

    @JsonProperty("verb")
    private String verb;

    @JsonProperty("endpoint")
    private String endpoint;

    @JsonProperty("consumedMediaTypes")
    private String[] consumedMediaTypes;

    @JsonProperty("producedMediaTypes")
    private String[] producedMediaTypes;

    @JsonProperty("description")
    private String description;

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public String getVerb() {
        return verb;
    }

    public void setVerb(String verb) {
        this.verb = verb;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String[] getConsumedMediaTypes() {
        return consumedMediaTypes;
    }

    public void setConsumedMediaTypes(String[] consumedMediaTypes) {
        this.consumedMediaTypes = consumedMediaTypes;
    }

    public String[] getProducedMediaTypes() {
        return producedMediaTypes;
    }

    public void setProducedMediaTypes(String[] producedMediaTypes) {
        this.producedMediaTypes = producedMediaTypes;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "EndpointInfo{" +
            "permission='" + permission + '\'' +
            ", verb='" + verb + '\'' +
            ", endpoint='" + endpoint + '\'' +
            ", consumedMediaTypes=" + Arrays.toString(consumedMediaTypes) +
            ", producedMediaTypes=" + Arrays.toString(producedMediaTypes) +
            ", description='" + description + '\'' +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EndpointInfo)) return false;
        EndpointInfo that = (EndpointInfo) o;
        return Objects.equals(permission, that.permission) && Objects.equals(verb, that.verb) && Objects.equals(endpoint, that.endpoint) && Arrays.equals(consumedMediaTypes, that.consumedMediaTypes) && Arrays.equals(producedMediaTypes, that.producedMediaTypes) && Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(permission, verb, endpoint, description);
        result = 31 * result + Arrays.hashCode(consumedMediaTypes);
        result = 31 * result + Arrays.hashCode(producedMediaTypes);
        return result;
    }

}

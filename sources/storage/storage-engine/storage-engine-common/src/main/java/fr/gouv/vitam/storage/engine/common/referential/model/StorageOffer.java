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
package fr.gouv.vitam.storage.engine.common.referential.model;

import fr.gouv.vitam.common.model.administration.ActivationStatus;

import java.util.Map;

/**
 * Define a Storage offer configuration
 */
public class StorageOffer {

    private String id;
    private String baseUrl;
    private boolean asyncRead = false;
    private Map<String, String> parameters;
    private ActivationStatus status;

    /**
     * @return the base url
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * @param baseUrl url set to storage offer
     */
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * @return the parameters
     */
    public Map<String, String> getParameters() {
        return parameters;
    }

    /**
     * @param parameters map of parameters set to offer
     */
    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id to set to offer
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the status
     */
    public ActivationStatus getStatus() {
        return status;
    }

    /**
     * principally, updated by {@link StorageStrategy} configuration and prevail upon offers configuration file
     *
     * @param status to set
     */

    public void setStatus(ActivationStatus status) {
        this.status = status;
    }

    public void setEnabled(boolean enable) {
        this.status = enable ? ActivationStatus.ACTIVE : ActivationStatus.INACTIVE;
    }

    public boolean isEnabled() {
        return ActivationStatus.ACTIVE.equals(this.getStatus());
    }

    public boolean isAsyncRead() {
        return asyncRead;
    }

    public void setAsyncRead(boolean asyncRead) {
        this.asyncRead = asyncRead;
    }
}

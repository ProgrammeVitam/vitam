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
package fr.gouv.vitam.processing.engine.core.operation;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.common.thread.VitamThreadUtils;

public class OperationContextModel {

    @JsonProperty("accessContractIdentifier")
    private String accessContractIdentifier;

    @JsonProperty("contextIdentifier")
    private String contextIdentifier;

    @JsonProperty("applicationSessionId")
    private String applicationSessionId;

    @JsonProperty("requestId")
    private String requestId;

    @JsonProperty("tenant")
    private Integer tenant;

    @JsonProperty("request")
    private Object request;

    public OperationContextModel() {
        //Empty
    }

    public OperationContextModel(Object request) {
        this.accessContractIdentifier = VitamThreadUtils.getVitamSession().getContractId();
        this.contextIdentifier = VitamThreadUtils.getVitamSession().getContextId();
        this.applicationSessionId = VitamThreadUtils.getVitamSession().getApplicationSessionId();
        this.tenant = VitamThreadUtils.getVitamSession().getTenantId();
        this.requestId = VitamThreadUtils.getVitamSession().getRequestId();
        this.request = request;
    }

    public static OperationContextModel get(Object request) {
        return new OperationContextModel(request);
    }

    public String getAccessContractIdentifier() {
        return accessContractIdentifier;
    }

    public void setAccessContractIdentifier(String accessContractIdentifier) {
        this.accessContractIdentifier = accessContractIdentifier;
    }

    public String getContextIdentifier() {
        return contextIdentifier;
    }

    public void setContextIdentifier(String contextIdentifier) {
        this.contextIdentifier = contextIdentifier;
    }

    public String getApplicationSessionId() {
        return applicationSessionId;
    }

    public void setApplicationSessionId(String applicationSessionId) {
        this.applicationSessionId = applicationSessionId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Integer getTenant() {
        return tenant;
    }

    public void setTenant(Integer tenant) {
        this.tenant = tenant;
    }

    public Object getRequest() {
        return request;
    }

    public void setRequest(Object request) {
        this.request = request;
    }
}

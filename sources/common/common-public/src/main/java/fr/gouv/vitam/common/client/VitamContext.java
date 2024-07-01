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
package fr.gouv.vitam.common.client;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.util.Objects;

import static fr.gouv.vitam.common.GlobalDataRest.X_ACCESS_CONTRAT_ID;
import static fr.gouv.vitam.common.GlobalDataRest.X_APPLICATION_ID;
import static fr.gouv.vitam.common.GlobalDataRest.X_PERSONAL_CERTIFICATE;
import static fr.gouv.vitam.common.GlobalDataRest.X_TENANT_ID;

/**
 * Defines commons client parameters for vitam external rest API
 */
public final class VitamContext {

    private Integer tenantId;
    private String accessContract;
    private String applicationSessionId;
    private String personalCertificate;

    /**
     * @param tenantId the tenant id
     */
    public VitamContext(Integer tenantId) {
        this.tenantId = tenantId;
    }

    /**
     * Gets the tenant id
     *
     * @return the tenant id
     */
    public Integer getTenantId() {
        return tenantId;
    }

    /**
     * Sets the tenant id
     *
     * @param tenantId the tenant id to set
     * @return "this" instance. May be used for fluent instance creation.
     */
    public VitamContext setTenantId(Integer tenantId) {
        this.tenantId = tenantId;
        return this;
    }

    /**
     * Gets the access contract
     *
     * @return the access contract
     */
    public String getAccessContract() {
        return accessContract;
    }

    /**
     * Sets the access contract
     *
     * @param accessContract the access contract to set
     * @return "this" instance. May be used for fluent instance creation.
     */
    public VitamContext setAccessContract(String accessContract) {
        this.accessContract = accessContract;
        return this;
    }

    /**
     * Gets the application session id
     *
     * @return the application session id
     */
    public String getApplicationSessionId() {
        return applicationSessionId;
    }

    /**
     * Sets the application session id
     *
     * @param applicationSessionId the application session id to set
     * @return "this" instance. May be used for fluent instance creation.
     */
    public VitamContext setApplicationSessionId(String applicationSessionId) {
        this.applicationSessionId = applicationSessionId;
        return this;
    }

    /**
     * Gets the personnal certificate.
     *
     * @return
     */
    public String getPersonalCertificate() {
        return personalCertificate;
    }

    /**
     * Sets personalCertificate
     *
     * @param personalCertificate the personalCertificate to set
     * @return "this" instance. May be used for fluent instance creation.
     */
    public VitamContext setPersonalCertificate(String personalCertificate) {
        this.personalCertificate = personalCertificate;
        return this;
    }

    /**
     * Returns a vitam context parameters as headers
     *
     * @return header-name/value map of vitam context parameters
     */
    public MultivaluedMap<String, Object> getHeaders() {
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();

        if (this.tenantId != null) {
            headers.add(X_TENANT_ID, this.tenantId);
        }
        if (this.accessContract != null) {
            headers.add(X_ACCESS_CONTRAT_ID, this.accessContract);
        }
        if (this.applicationSessionId != null) {
            headers.add(X_APPLICATION_ID, this.applicationSessionId);
        }
        if (this.personalCertificate != null) {
            headers.add(X_PERSONAL_CERTIFICATE, this.personalCertificate);
        }

        return headers;
    }

    @Override
    public String toString() {
        return (
            "VitamContext{" +
            "tenantId=" +
            tenantId +
            ", accessContract='" +
            accessContract +
            '\'' +
            ", applicationSessionId='" +
            applicationSessionId +
            '\'' +
            ", personalCertificate='" +
            personalCertificate +
            '\'' +
            '}'
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VitamContext that = (VitamContext) o;
        return (
            Objects.equals(tenantId, that.tenantId) &&
            Objects.equals(accessContract, that.accessContract) &&
            Objects.equals(applicationSessionId, that.applicationSessionId) &&
            Objects.equals(personalCertificate, that.personalCertificate)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(tenantId, accessContract, applicationSessionId, personalCertificate);
    }
}

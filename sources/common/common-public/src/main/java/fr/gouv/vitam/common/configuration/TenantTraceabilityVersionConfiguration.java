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
package fr.gouv.vitam.common.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.regex.Pattern;

/**
 * Configuration class for tenant-specific securisation version
 */
public class TenantTraceabilityVersionConfiguration {

    /**
     * Regex pattern for version validation (V1, V2, V3, etc.)
     */
    private static final Pattern VERSION_PATTERN = Pattern.compile("^V[1-9]\\d*$");

    @JsonProperty("tenant")
    private Integer tenant;

    @JsonProperty("logbookOperation")
    private String logbookOperation;

    @JsonProperty("lfcUnit")
    private String lfcUnit;

    @JsonProperty("lfcGot")
    private String lfcGot;

    /**
     * Default constructor for deserialization
     */
    public TenantTraceabilityVersionConfiguration() {
        // Default constructor for deserialization
    }

    /**
     * Constructor with consolidated parameters
     *
     * @param tenant Tenant ID
     * @param logbookOperation Logbook operation traceability version
     * @param lfcUnit LFC unit traceability version
     * @param lfcGot LFC GOT traceability version
     * @throws IllegalArgumentException if any version doesn't match the required format (V1, V2, V3, etc.)
     */
    public TenantTraceabilityVersionConfiguration(
        Integer tenant,
        String logbookOperation,
        String lfcUnit,
        String lfcGot
    ) {
        this.tenant = tenant;
        setLogbookOperation(logbookOperation);
        setLfcUnit(lfcUnit);
        setLfcGot(lfcGot);
    }

    /**
     * Get the tenant ID
     *
     * @return Tenant ID
     */
    public Integer getTenant() {
        return tenant;
    }

    /**
     * Set the tenant ID
     *
     * @param tenant Tenant ID
     * @return This instance for chaining
     */
    public TenantTraceabilityVersionConfiguration setTenant(Integer tenant) {
        this.tenant = tenant;
        return this;
    }

    /**
     * Get the logbook operation traceability version
     *
     * @return Logbook operation traceability version
     */
    public String getLogbookOperation() {
        return logbookOperation;
    }

    /**
     * Set the logbook operation traceability version
     *
     * @param logbookOperation Logbook operation traceability version
     * @return This instance for chaining
     * @throws IllegalArgumentException if version doesn't match the required format (V1, V2, V3, etc.)
     */
    public TenantTraceabilityVersionConfiguration setLogbookOperation(String logbookOperation) {
        if (logbookOperation != null && !VERSION_PATTERN.matcher(logbookOperation).matches()) {
            throw new IllegalArgumentException(
                "Version must follow the format V1, V2, V3, etc. Invalid version: " + logbookOperation
            );
        }
        this.logbookOperation = logbookOperation;
        return this;
    }

    /**
     * Get the LFC unit traceability version
     *
     * @return LFC unit traceability version
     */
    public String getLfcUnit() {
        return lfcUnit;
    }

    /**
     * Set the LFC unit traceability version
     *
     * @param lfcUnit LFC unit traceability version
     * @return This instance for chaining
     * @throws IllegalArgumentException if version doesn't match the required format (V1, V2, V3, etc.)
     */
    public TenantTraceabilityVersionConfiguration setLfcUnit(String lfcUnit) {
        if (lfcUnit != null && !VERSION_PATTERN.matcher(lfcUnit).matches()) {
            throw new IllegalArgumentException(
                "Version must follow the format V1, V2, V3, etc. Invalid version: " + lfcUnit
            );
        }
        this.lfcUnit = lfcUnit;
        return this;
    }

    /**
     * Get the LFC GOT traceability version
     *
     * @return LFC GOT traceability version
     */
    public String getLfcGot() {
        return lfcGot;
    }

    /**
     * Set the LFC GOT traceability version
     *
     * @param lfcGot LFC GOT traceability version
     * @return This instance for chaining
     * @throws IllegalArgumentException if version doesn't match the required format (V1, V2, V3, etc.)
     */
    public TenantTraceabilityVersionConfiguration setLfcGot(String lfcGot) {
        if (lfcGot != null && !VERSION_PATTERN.matcher(lfcGot).matches()) {
            throw new IllegalArgumentException(
                "Version must follow the format V1, V2, V3, etc. Invalid version: " + lfcGot
            );
        }
        this.lfcGot = lfcGot;
        return this;
    }

    @Override
    public String toString() {
        return (
            "TenantTraceabilityVersionConfiguration{" +
            "tenant=" +
            tenant +
            ", logbookOperation='" +
            logbookOperation +
            '\'' +
            ", lfcUnit='" +
            lfcUnit +
            '\'' +
            ", lfcGot='" +
            lfcGot +
            '\'' +
            '}'
        );
    }
}

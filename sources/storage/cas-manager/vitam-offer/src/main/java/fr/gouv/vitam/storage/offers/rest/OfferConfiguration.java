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
package fr.gouv.vitam.storage.offers.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;

/**
 * Offer configuration
 */
public class OfferConfiguration extends DbConfigurationImpl {

    private String provider;
    private String swiftKeystoneAuthUrl;
    private String swiftDomain;
    private String swiftUser;
    private String swiftPassword;
    private String storagePath;
    private String contextPath;
    private boolean authentication;
    private String swiftProjectName;

    @JsonProperty("offerLogCompaction")
    private OfferLogCompactionConfiguration offerLogCompactionConfiguration;

    /**
     * Max thread pool size for batch processing
     */
    @JsonProperty("maxBatchThreadPoolSize")
    private int maxBatchThreadPoolSize = 32;

    /**
     * Timeout (in seconds) for batch metadata retrieval
     */
    @JsonProperty("batchMetadataComputationTimeout")
    private int batchMetadataComputationTimeout = 600;

    /**
     * @return the provider
     */
    public String getProvider() {
        return provider;
    }

    /**
     * @param provider the provider to set
     * @return this
     */
    public OfferConfiguration setProvider(String provider) {
        this.provider = provider;
        return this;
    }

    /**
     * @return the swiftKeystoneAuthUrl
     */
    public String getSwiftKeystoneAuthUrl() {
        return swiftKeystoneAuthUrl;
    }

    /**
     * @param swiftKeystoneAuthUrl the swiftKeystoneAuthUrl to set
     * @return this
     */
    public OfferConfiguration setSwiftKeystoneAuthUrl(String swiftKeystoneAuthUrl) {
        this.swiftKeystoneAuthUrl = swiftKeystoneAuthUrl;
        return this;
    }

    /**
     * @return the tenantName
     */
    public String getSwiftDomain() {
        return swiftDomain;
    }

    /**
     * @param swiftDomain the tenantName to set
     * @return this
     */
    public OfferConfiguration setSwiftDomain(String swiftDomain) {
        this.swiftDomain = swiftDomain;
        return this;
    }

    /**
     * @return the swiftUser
     */
    public String getSwiftUser() {
        return swiftUser;
    }

    /**
     * @param swiftUser the userName to set
     * @return this
     */
    public OfferConfiguration setSwiftUser(String swiftUser) {
        this.swiftUser = swiftUser;
        return this;
    }

    /**
     * @return the swiftPassword
     */
    public String getSwiftPassword() {
        return swiftPassword;
    }

    /**
     * @param swiftPassword the swiftPassword to set
     * @return this
     */
    public OfferConfiguration setSwiftPassword(String swiftPassword) {
        this.swiftPassword = swiftPassword;
        return this;
    }

    /**
     * @return the storagePath
     */
    public String getStoragePath() {
        return storagePath;
    }

    /**
     * @param storagePath the storagePath to set
     * @return this
     */
    public OfferConfiguration setStoragePath(String storagePath) {
        this.storagePath = storagePath;
        return this;
    }

    /**
     * @return the contextPath
     */
    public String getContextPath() {
        return contextPath;
    }

    /**
     * @param contextPath the contextPath to set
     * @return this
     */
    public OfferConfiguration setContextPath(String contextPath) {
        this.contextPath = contextPath;
        return this;
    }

    /**
     * @return boolean
     */
    public boolean isAuthentication() {
        return authentication;
    }

    /**
     * @param authentication to set or unset
     * @return OfferConfiguration
     */
    public OfferConfiguration setAuthentication(boolean authentication) {
        this.authentication = authentication;
        return this;
    }

    /**
     * @return swiftProjectName
     */
    public String getSwiftProjectName() {
        return swiftProjectName;
    }

    /**
     * @param swiftProjectName the swiftProjectName to set or unset
     * @return OfferConfiguration
     */
    public OfferConfiguration setSwiftProjectName(String swiftProjectName) {
        this.swiftProjectName = swiftProjectName;
        return this;
    }

    public OfferLogCompactionConfiguration getOfferLogCompactionConfiguration() {
        return offerLogCompactionConfiguration;
    }

    public OfferConfiguration setOfferLogCompactionConfiguration(
        OfferLogCompactionConfiguration offerLogCompactionConfiguration
    ) {
        this.offerLogCompactionConfiguration = offerLogCompactionConfiguration;
        return this;
    }

    public int getMaxBatchThreadPoolSize() {
        return maxBatchThreadPoolSize;
    }

    public OfferConfiguration setMaxBatchThreadPoolSize(int maxBatchThreadPoolSize) {
        this.maxBatchThreadPoolSize = maxBatchThreadPoolSize;
        return this;
    }

    public int getBatchMetadataComputationTimeout() {
        return batchMetadataComputationTimeout;
    }

    public OfferConfiguration setBatchMetadataComputationTimeout(int batchMetadataComputationTimeout) {
        this.batchMetadataComputationTimeout = batchMetadataComputationTimeout;
        return this;
    }
}

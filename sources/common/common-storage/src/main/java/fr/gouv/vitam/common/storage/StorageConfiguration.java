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
package fr.gouv.vitam.common.storage;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fr.gouv.vitam.common.server.application.configuration.DefaultVitamApplicationConfiguration;
import fr.gouv.vitam.common.storage.swift.VitamCustomizedHeader;
import fr.gouv.vitam.common.storage.tapelibrary.TapeLibraryConfiguration;

import java.util.List;

/**
 * Storage configuration contains all configuration items for storage offers
 */
public class StorageConfiguration extends DefaultVitamApplicationConfiguration {

    private String provider;
    private String swiftKeystoneAuthUrl;
    private String swiftDomain;
    private String swiftUser;
    private String swiftPassword;
    private String storagePath;
    private String contextPath;
    private boolean authentication;
    private String swiftProjectName;
    private String swiftUrl;
    private String swiftTrustStore;
    private String swiftTrustStorePassword;
    private int swiftMaxConnectionsPerRoute;
    private int swiftMaxConnections;
    private int swiftConnectionTimeout;
    private int swiftReadTimeout;
    /**
     * swiftRenewTokenDelayBeforeExpireTime is the time in seconds to handle a token
     * renew before a token expiration occurs
     */
    private long swiftSoftRenewTokenDelayBeforeExpireTime;
    private long swiftHardRenewTokenDelayBeforeExpireTime;

    /**
     * S3 Region Name
     */
    private String s3RegionName;
    /**
     * S3 URL
     */
    private String s3Endpoint;
    /**
     * S3 trust store path
     */
    private String s3TrustStore;
    /**
     * S3 trust store password
     */
    private String s3TrustStorePassword;
    /**
     * S3 Access Key ID
     */
    private String s3AccessKey;
    /**
     * S3 Access Key Login
     */
    private String s3SecretKey;
    /**
     * S3 Signature algorithm (default null for V4, or 'S3SignerType' for V2 or
     * 'AWSS3V4SignerType' for V4)
     */
    private String s3SignerType;
    /**
     * S3 access bucket in 'path-style' instead of default 'virtual-hosted-style'
     */
    private boolean s3PathStyleAccessEnabled;
    /**
     * S3 max number of open http connections
     */
    private int s3MaxConnections;
    /**
     * S3 connection timeout
     */
    private int s3ConnectionTimeout;
    /**
     * S3 socket timeout
     */
    private int s3SocketTimeout;
    /**
     * S3 request timeout
     */
    private int s3RequestTimeout;
    /**
     * S3 client execution timeout
     */
    private int s3ClientExecutionTimeout;

    /**
     * Tape library configuration
     */
    private TapeLibraryConfiguration tapeLibraryConfiguration;

    /**
     * Enable / Disable use of vitam custom headers for offers requests
     */
    private Boolean enableCustomHeaders;

    private List<VitamCustomizedHeader> customHeaders;

    private int swiftNbRetries = 1;
    private int swiftWaitingTimeInMilliseconds = 10_000;
    private int swiftRandomRangeSleepInMilliseconds = 10_000;

    /**
     * @return the swiftUrl
     */
    public String getSwiftUrl() {
        return swiftUrl;
    }

    /**
     * @param swiftUrl the swiftUrl to set
     * @return this
     */
    public StorageConfiguration setSwiftUrl(String swiftUrl) {
        this.swiftUrl = swiftUrl;
        return this;
    }

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
    public StorageConfiguration setProvider(String provider) {
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
    public StorageConfiguration setSwiftKeystoneAuthUrl(String swiftKeystoneAuthUrl) {
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
    public StorageConfiguration setSwiftDomain(String swiftDomain) {
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
    public StorageConfiguration setSwiftUser(String swiftUser) {
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
    public StorageConfiguration setSwiftPassword(String swiftPassword) {
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
    public StorageConfiguration setStoragePath(String storagePath) {
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
    public StorageConfiguration setContextPath(String contextPath) {
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
     * @param authentication to set ou unset
     * @return StorageConfiguration
     */
    public StorageConfiguration setAuthentication(boolean authentication) {
        this.authentication = authentication;
        return this;
    }

    public String getSwiftProjectName() {
        return swiftProjectName;
    }

    public StorageConfiguration setSwiftProjectName(String swiftProjectName) {
        this.swiftProjectName = swiftProjectName;
        return this;
    }

    public String getSwiftTrustStore() {
        return swiftTrustStore;
    }

    public void setSwiftTrustStore(String swiftTrustStore) {
        this.swiftTrustStore = swiftTrustStore;
    }

    public String getSwiftTrustStorePassword() {
        return swiftTrustStorePassword;
    }

    public void setSwiftTrustStorePassword(String swiftTrustStorePassword) {
        this.swiftTrustStorePassword = swiftTrustStorePassword;
    }

    public int getSwiftMaxConnectionsPerRoute() {
        return swiftMaxConnectionsPerRoute;
    }

    public void setSwiftMaxConnectionsPerRoute(int swiftMaxConnectionsPerRoute) {
        this.swiftMaxConnectionsPerRoute = swiftMaxConnectionsPerRoute;
    }

    public int getSwiftMaxConnections() {
        return swiftMaxConnections;
    }

    public void setSwiftMaxConnections(int swiftMaxConnections) {
        this.swiftMaxConnections = swiftMaxConnections;
    }

    public int getSwiftConnectionTimeout() {
        return swiftConnectionTimeout;
    }

    public void setSwiftConnectionTimeout(int swiftConnectionTimeout) {
        this.swiftConnectionTimeout = swiftConnectionTimeout;
    }

    public int getSwiftReadTimeout() {
        return swiftReadTimeout;
    }

    public void setSwiftReadTimeout(int swiftReadTimeout) {
        this.swiftReadTimeout = swiftReadTimeout;
    }

    public long getSwiftSoftRenewTokenDelayBeforeExpireTime() {
        return swiftSoftRenewTokenDelayBeforeExpireTime;
    }

    public void setSwiftSoftRenewTokenDelayBeforeExpireTime(long swiftSoftRenewTokenDelayBeforeExpireTime) {
        this.swiftSoftRenewTokenDelayBeforeExpireTime = swiftSoftRenewTokenDelayBeforeExpireTime;
    }

    public long getSwiftHardRenewTokenDelayBeforeExpireTime() {
        return swiftHardRenewTokenDelayBeforeExpireTime;
    }

    public void setSwiftHardRenewTokenDelayBeforeExpireTime(long swiftHardRenewTokenDelayBeforeExpireTime) {
        this.swiftHardRenewTokenDelayBeforeExpireTime = swiftHardRenewTokenDelayBeforeExpireTime;
    }

    public String getS3RegionName() {
        return s3RegionName;
    }

    public StorageConfiguration setS3RegionName(String s3RegionName) {
        this.s3RegionName = s3RegionName;
        return this;
    }

    public String getS3Endpoint() {
        return s3Endpoint;
    }

    public StorageConfiguration setS3Endpoint(String s3Endpoint) {
        this.s3Endpoint = s3Endpoint;
        return this;
    }

    public String getS3TrustStore() {
        return s3TrustStore;
    }

    public StorageConfiguration setS3TrustStore(String s3TrustStore) {
        this.s3TrustStore = s3TrustStore;
        return this;
    }

    public String getS3TrustStorePassword() {
        return s3TrustStorePassword;
    }

    public StorageConfiguration setS3TrustStorePassword(String s3TrustStorePassword) {
        this.s3TrustStorePassword = s3TrustStorePassword;
        return this;
    }

    public String getS3AccessKey() {
        return s3AccessKey;
    }

    public StorageConfiguration setS3AccessKey(String s3AccessKey) {
        this.s3AccessKey = s3AccessKey;
        return this;
    }

    public String getS3SecretKey() {
        return s3SecretKey;
    }

    public StorageConfiguration setS3SecretKey(String s3SecretKey) {
        this.s3SecretKey = s3SecretKey;
        return this;
    }

    public String getS3SignerType() {
        return s3SignerType;
    }

    public StorageConfiguration setS3SignerType(String s3SignerType) {
        this.s3SignerType = s3SignerType;
        return this;
    }

    public boolean isS3PathStyleAccessEnabled() {
        return s3PathStyleAccessEnabled;
    }

    public StorageConfiguration setS3PathStyleAccessEnabled(boolean s3PathStyleAccessEnabled) {
        this.s3PathStyleAccessEnabled = s3PathStyleAccessEnabled;
        return this;
    }

    public int getS3MaxConnections() {
        return s3MaxConnections;
    }

    public StorageConfiguration setS3MaxConnections(int s3MaxConnections) {
        this.s3MaxConnections = s3MaxConnections;
        return this;
    }

    public int getS3ConnectionTimeout() {
        return s3ConnectionTimeout;
    }

    public StorageConfiguration setS3ConnectionTimeout(int s3ConnectionTimeout) {
        this.s3ConnectionTimeout = s3ConnectionTimeout;
        return this;
    }

    public int getS3SocketTimeout() {
        return s3SocketTimeout;
    }

    public StorageConfiguration setS3SocketTimeout(int s3SocketTimeout) {
        this.s3SocketTimeout = s3SocketTimeout;
        return this;
    }

    public int getS3RequestTimeout() {
        return s3RequestTimeout;
    }

    public StorageConfiguration setS3RequestTimeout(int s3RequestTimeout) {
        this.s3RequestTimeout = s3RequestTimeout;
        return this;
    }

    public int getS3ClientExecutionTimeout() {
        return s3ClientExecutionTimeout;
    }

    public StorageConfiguration setS3ClientExecutionTimeout(int s3ClientExecutionTimeout) {
        this.s3ClientExecutionTimeout = s3ClientExecutionTimeout;
        return this;
    }

    public TapeLibraryConfiguration getTapeLibraryConfiguration() {
        return tapeLibraryConfiguration;
    }

    public StorageConfiguration setTapeLibraryConfiguration(TapeLibraryConfiguration tapeLibraryConfiguration) {
        this.tapeLibraryConfiguration = tapeLibraryConfiguration;
        return this;
    }

    public int getSwiftNbRetries() {
        return swiftNbRetries;
    }

    public StorageConfiguration setSwiftNbRetries(int swiftNbRetries) {
        this.swiftNbRetries = swiftNbRetries;
        return this;
    }

    public int getSwiftWaitingTimeInMilliseconds() {
        return swiftWaitingTimeInMilliseconds;
    }

    public StorageConfiguration setSwiftWaitingTimeInMilliseconds(int swiftWaitingTimeInMilliseconds) {
        this.swiftWaitingTimeInMilliseconds = swiftWaitingTimeInMilliseconds;
        return this;
    }

    public int getSwiftRandomRangeSleepInMilliseconds() {
        return swiftRandomRangeSleepInMilliseconds;
    }

    public StorageConfiguration setSwiftRandomRangeSleepInMilliseconds(int swiftRandomRangeSleepInMilliseconds) {
        this.swiftRandomRangeSleepInMilliseconds = swiftRandomRangeSleepInMilliseconds;
        return this;
    }

    public Boolean getEnableCustomHeaders() {
        return enableCustomHeaders;
    }

    public void setEnableCustomHeaders(Boolean enableCustomHeaders) {
        this.enableCustomHeaders = enableCustomHeaders;
    }

    public List<VitamCustomizedHeader> getCustomHeaders() {
        return customHeaders;
    }

    public void setCustomHeaders(List<VitamCustomizedHeader> customHeaders) {
        this.customHeaders = customHeaders;
    }

    @JsonIgnore
    @Override
    public String getBaseUrl() {
        return contextPath;
    }
}

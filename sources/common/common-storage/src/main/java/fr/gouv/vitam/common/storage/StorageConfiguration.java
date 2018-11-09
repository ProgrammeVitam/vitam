/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 * 
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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

import fr.gouv.vitam.common.server.application.configuration.DefaultVitamApplicationConfiguration;

/**
 * Storage configuration Contains all configuration items for storage offers
 */
public class StorageConfiguration extends DefaultVitamApplicationConfiguration {

    private String provider;
    private String keystoneEndPoint;
    private String swiftUid;
    private String swiftSubUser;
    private String credential;
    private String storagePath;
    private Boolean cephMode;
    private String contextPath;
    private boolean authentication;
    private String projectName;
    private String swiftUrl;
    private String swiftTrustStore;
    private String swiftTrustStorePassword;
    private int swiftMaxConnectionsPerRoute;
    private int swiftMaxConnections;
    private int swiftConnectionTimeout;
    private int swiftReadTimeout;

    /**
     * @return the swiftUrl
     */
    public String getSwiftUrl() { return swiftUrl; }

    /**
     * @param swiftUrl
     *            the swiftUrl to set
     *
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
     * @param provider
     *            the provider to set
     *
     * @return this
     */
    public StorageConfiguration setProvider(String provider) {
        this.provider = provider;
        return this;
    }

    /**
     * @return the keystoneEndPoint
     */
    public String getKeystoneEndPoint() {
        return keystoneEndPoint;
    }

    /**
     * @param keystoneEndPoint
     *            the keystoneEndPoint to set
     *
     * @return this
     */
    public StorageConfiguration setKeystoneEndPoint(String keystoneEndPoint) {
        this.keystoneEndPoint = keystoneEndPoint;
        return this;
    }

    /**
     * @return the tenantName
     */
    public String getSwiftUid() {
        return swiftUid;
    }

    /**
     * @param swiftUid
     *            the tenantName to set
     *
     * @return this
     */
    public StorageConfiguration setSwiftUid(String swiftUid) {
        this.swiftUid = swiftUid;
        return this;
    }

    /**
     * @return the swiftSubUser
     */
    public String getSwiftSubUser() {
        return swiftSubUser;
    }

    /**
     * @param swiftSubUser
     *            the userName to set
     *
     * @return this
     */
    public StorageConfiguration setSwiftSubUser(String swiftSubUser) {
        this.swiftSubUser = swiftSubUser;
        return this;
    }

    /**
     * @return the credential
     */
    public String getCredential() {
        return credential;
    }

    /**
     * @param credential
     *            the credential to set
     *
     * @return this
     */
    public StorageConfiguration setCredential(String credential) {
        this.credential = credential;
        return this;
    }

    /**
     * @return the storagePath
     */
    public String getStoragePath() {
        return storagePath;
    }

    /**
     * @param storagePath
     *            the storagePath to set
     *
     * @return this
     */
    public StorageConfiguration setStoragePath(String storagePath) {
        this.storagePath = storagePath;
        return this;
    }

    /**
     * @return the cephMode
     */
    public Boolean isCephMode() {
        return cephMode;
    }

    /**
     * @param cephMode
     *            the cephMode to set
     *
     * @return this
     */
    public StorageConfiguration setCephMode(Boolean cephMode) {
        this.cephMode = cephMode;
        return this;
    }

    /**
     * @return the contextPath
     */
    public String getContextPath() {
        return contextPath;
    }

    /**
     * @param contextPath
     *            the contextPath to set
     *
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

    public String getProjectName() {
        return projectName;
    }

    public StorageConfiguration setProjectName(String projectName) {
        this.projectName = projectName;
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

    /**
     * getter for swiftConnectionTimeout
     *
     * @return swiftConnectionTimeout value
     */
    public int getSwiftConnectionTimeout() {
        return swiftConnectionTimeout;
    }

    /**
     * set swiftConnectionTimeout
     */
    public void setSwiftConnectionTimeout(int swiftConnectionTimeout) {
        this.swiftConnectionTimeout = swiftConnectionTimeout;
    }

    /**
     * getter for swiftReadTimeout
     *
     * @return swiftReadTimeout value
     */
    public int getSwiftReadTimeout() {
        return swiftReadTimeout;
    }

    /**
     * set swiftReadTimeout
     */
    public void setSwiftReadTimeout(int swiftReadTimeout) {
        this.swiftReadTimeout = swiftReadTimeout;
    }
}

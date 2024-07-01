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
package fr.gouv.vitam.storage.engine.server.rest;

import fr.gouv.vitam.common.server.application.configuration.DefaultVitamApplicationConfiguration;

public final class StorageConfiguration extends DefaultVitamApplicationConfiguration {

    private String urlWorkspace;
    private Integer timeoutMsPerKB;
    private int minWriteTimeoutMs = 60_000;
    private int minBulkWriteTimeoutMsPerObject = 10_000;
    private String loggingDirectory;
    private String zippingDirecorty;
    private String p12LogbookPassword;
    private String p12LogbookFile;
    private Integer storageTraceabilityOverlapDelay;
    private Boolean isReadOnly;

    private int offerSynchronizationBulkSize = 1000;
    private int offerSyncThreadPoolSize = 32;

    private int offerSyncNumberOfRetries = 3;
    private int offerSyncFirstAttemptWaitingTime = 15;
    private int offerSyncWaitingTime = 30;
    private int offerSyncAccessRequestCheckWaitingTime = 10;
    private int storageLogBackupThreadPoolSize = 16;
    private int storageLogTraceabilityThreadPoolSize = 16;

    /**
     * StorageConfiguration empty constructor for YAMLFactory
     */
    public StorageConfiguration() {
        // Empty constructor
    }

    /**
     * @return the urlWorkspace
     */
    public String getUrlWorkspace() {
        return urlWorkspace;
    }

    /**
     * @param urlWorkspace the urlWorkspace to set
     * @return this
     */
    public StorageConfiguration setUrlWorkspace(String urlWorkspace) {
        this.urlWorkspace = urlWorkspace;
        return this;
    }

    /**
     * @return the timeout in millisecond for one kB
     */
    public Integer getTimeoutMsPerKB() {
        return timeoutMsPerKB;
    }

    /**
     * @param timeoutMsPerKB the timeout for on kB transfered in milliseconds
     * @return this
     */
    public StorageConfiguration setTimeoutMsPerKB(Integer timeoutMsPerKB) {
        this.timeoutMsPerKB = timeoutMsPerKB;
        return this;
    }

    /**
     * @return loggingDirectory
     */
    public String getLoggingDirectory() {
        return loggingDirectory;
    }

    /**
     * @param loggingDirectory
     */
    public void setLoggingDirectory(String loggingDirectory) {
        this.loggingDirectory = loggingDirectory;
    }

    /**
     * @return zippingDirecorty
     */
    public String getZippingDirecorty() {
        return zippingDirecorty;
    }

    /**
     * @param zippingDirecorty
     */
    public void setZippingDirecorty(String zippingDirecorty) {
        this.zippingDirecorty = zippingDirecorty;
    }

    /**
     * @return password of p12
     */
    public String getP12LogbookPassword() {
        return p12LogbookPassword;
    }

    /**
     * @param p12LogbookPassword file to set
     */
    public void setP12LogbookPassword(String p12LogbookPassword) {
        this.p12LogbookPassword = p12LogbookPassword;
    }

    /**
     * @return p12 logbook file
     */
    public String getP12LogbookFile() {
        return p12LogbookFile;
    }

    /**
     * @param p12LogbookFile file to set
     */
    public void setP12LogbookFile(String p12LogbookFile) {
        this.p12LogbookFile = p12LogbookFile;
    }

    /**
     * Gets the overlap delay (in seconds) for logbook operation traceability events. Used to catch up possibly missed events
     * due to clock difference.
     *
     * @return The overlap delay (in seconds).
     */
    public Integer getStorageTraceabilityOverlapDelay() {
        return storageTraceabilityOverlapDelay;
    }

    /**
     * Sets the overlap delay (in seconds) for logbook operation traceability events.
     */
    public void setStorageTraceabilityOverlapDelay(Integer storageTraceabilityOverlapDelay) {
        this.storageTraceabilityOverlapDelay = storageTraceabilityOverlapDelay;
    }

    public int getOfferSynchronizationBulkSize() {
        return offerSynchronizationBulkSize;
    }

    public StorageConfiguration setOfferSynchronizationBulkSize(int offerSynchronizationBulkSize) {
        this.offerSynchronizationBulkSize = offerSynchronizationBulkSize;
        return this;
    }

    public int getOfferSyncThreadPoolSize() {
        return offerSyncThreadPoolSize;
    }

    public StorageConfiguration setOfferSyncThreadPoolSize(int offerSyncThreadPoolSize) {
        this.offerSyncThreadPoolSize = offerSyncThreadPoolSize;
        return this;
    }

    public int getOfferSyncNumberOfRetries() {
        return offerSyncNumberOfRetries;
    }

    public void setOfferSyncNumberOfRetries(int offerSyncNumberOfRetries) {
        this.offerSyncNumberOfRetries = offerSyncNumberOfRetries;
    }

    public int getOfferSyncFirstAttemptWaitingTime() {
        return offerSyncFirstAttemptWaitingTime;
    }

    public void setOfferSyncFirstAttemptWaitingTime(int offerSyncFirstAttemptWaitingTime) {
        this.offerSyncFirstAttemptWaitingTime = offerSyncFirstAttemptWaitingTime;
    }

    public int getOfferSyncWaitingTime() {
        return offerSyncWaitingTime;
    }

    public void setOfferSyncWaitingTime(int offerSyncWaitingTime) {
        this.offerSyncWaitingTime = offerSyncWaitingTime;
    }

    public int getOfferSyncAccessRequestCheckWaitingTime() {
        return offerSyncAccessRequestCheckWaitingTime;
    }

    public void setOfferSyncAccessRequestCheckWaitingTime(int offerSyncAccessRequestCheckWaitingTime) {
        this.offerSyncAccessRequestCheckWaitingTime = offerSyncAccessRequestCheckWaitingTime;
    }

    public int getStorageLogBackupThreadPoolSize() {
        return storageLogBackupThreadPoolSize;
    }

    public void setStorageLogBackupThreadPoolSize(int storageLogBackupThreadPoolSize) {
        this.storageLogBackupThreadPoolSize = storageLogBackupThreadPoolSize;
    }

    public int getStorageLogTraceabilityThreadPoolSize() {
        return storageLogTraceabilityThreadPoolSize;
    }

    public void setStorageLogTraceabilityThreadPoolSize(int storageLogTraceabilityThreadPoolSize) {
        this.storageLogTraceabilityThreadPoolSize = storageLogTraceabilityThreadPoolSize;
    }

    public int getMinWriteTimeoutMs() {
        return minWriteTimeoutMs;
    }

    public StorageConfiguration setMinWriteTimeoutMs(int minWriteTimeoutMs) {
        this.minWriteTimeoutMs = minWriteTimeoutMs;
        return this;
    }

    public int getMinBulkWriteTimeoutMsPerObject() {
        return minBulkWriteTimeoutMsPerObject;
    }

    public StorageConfiguration setMinBulkWriteTimeoutMsPerObject(int minBulkWriteTimeoutMsPerObject) {
        this.minBulkWriteTimeoutMsPerObject = minBulkWriteTimeoutMsPerObject;
        return this;
    }

    public Boolean isReadOnly() {
        return isReadOnly;
    }

    public StorageConfiguration setReadOnly(Boolean readOnly) {
        isReadOnly = readOnly;
        return this;
    }
}

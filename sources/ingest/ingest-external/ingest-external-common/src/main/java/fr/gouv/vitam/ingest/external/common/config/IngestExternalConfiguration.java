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
package fr.gouv.vitam.ingest.external.common.config;

import fr.gouv.vitam.common.model.LocalFileAction;
import fr.gouv.vitam.common.server.application.configuration.DefaultVitamApplicationConfiguration;

/**
 * IngestExternalConfiguration contains access informations of ingest
 */
public class IngestExternalConfiguration extends DefaultVitamApplicationConfiguration {

    private String path;
    private String antiVirusScriptName;
    private long timeoutScanDelay;
    private String baseUploadPath;
    private LocalFileAction fileActionAfterUpload;
    private String successfulUploadDir;
    private String failedUploadDir;

    private boolean allowSslClientHeader = false;

    /**
     * IngestExternalConfiguration empty constructor for YAMLFactory
     */
    public IngestExternalConfiguration() {
        // Empty constructor
    }

    /**
     * @return path
     */
    public String getPath() {
        return path;
    }

    /**
     * @param path to set to configuration
     * @return IngestExternalConfiguration
     */
    public IngestExternalConfiguration setPath(String path) {
        this.path = path;
        return this;
    }

    /**
     * @return antiVirusScriptName
     */
    public String getAntiVirusScriptName() {
        return antiVirusScriptName;
    }

    /**
     * @param antiVirusScriptName the antivirus script name to set
     * @return IngestExternalConfiguration
     */
    public IngestExternalConfiguration setAntiVirusScriptName(String antiVirusScriptName) {
        this.antiVirusScriptName = antiVirusScriptName;
        return this;
    }

    /**
     * @return long
     */
    public long getTimeoutScanDelay() {
        return timeoutScanDelay;
    }

    /**
     * @param timeoutScanDelay set to configuration
     * @return IngestExternalConfiguration
     */
    public IngestExternalConfiguration setTimeoutScanDelay(long timeoutScanDelay) {
        this.timeoutScanDelay = timeoutScanDelay;
        return this;
    }

    /**
     * @return baseUploadPath
     */
    public String getBaseUploadPath() {
        return baseUploadPath;
    }

    /**
     * @param baseUploadPath set to configuration
     * @return IngestExternalConfiguration
     */
    public IngestExternalConfiguration setBaseUploadPath(String baseUploadPath) {
        this.baseUploadPath = baseUploadPath;
        return this;
    }

    public LocalFileAction getFileActionAfterUpload() {
        return fileActionAfterUpload;
    }

    public IngestExternalConfiguration setFileActionAfterUpload(String fileActionAfterUpload) {
        this.fileActionAfterUpload = LocalFileAction.getLocalFileAction(fileActionAfterUpload);
        return this;
    }

    public String getSuccessfulUploadDir() {
        return successfulUploadDir;
    }

    public IngestExternalConfiguration setSuccessfulUploadDir(String successfulUploadDir) {
        this.successfulUploadDir = successfulUploadDir;
        return this;
    }

    public String getFailedUploadDir() {
        return failedUploadDir;
    }

    public IngestExternalConfiguration setFailedUploadDir(String failedUploadDir) {
        this.failedUploadDir = failedUploadDir;
        return this;
    }

    public boolean isAllowSslClientHeader() {
        return allowSslClientHeader;
    }

    public void setAllowSslClientHeader(boolean allowSslClientHeader) {
        this.allowSslClientHeader = allowSslClientHeader;
    }
}

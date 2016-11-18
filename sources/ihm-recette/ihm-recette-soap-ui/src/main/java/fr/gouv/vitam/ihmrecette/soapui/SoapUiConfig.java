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
package fr.gouv.vitam.ihmrecette.soapui;

import fr.gouv.vitam.common.ParametersChecker;

/**
 * Web Application Configuration class
 */
public class SoapUiConfig {

    private String ingestProtocol;
    private int ingestPort;
    private String ingestHost;
    
    private String accessExternalProtocol;
    private int accessExternalPort;
    private String accessExternalHost;

    private String certfile;
    private String dataDir;
    private String reportingDir;
    private String configDir;
    private String soapUiExecutable;
    
    /**
     * @return ingestPort
     */
    public int getIngestPort() {
        return ingestPort;
    }

    /**
     * @param ingestPort port of the ingest module
     * @return SoapUiConfig
     */
    public SoapUiConfig setIngestPort(int ingestPort) {
        ParametersChecker.checkParameter("ingestPort is mandatory", ingestPort);
        this.ingestPort = ingestPort;
        return this;
    }

    /**
     * @return ingestHost
     */
    public String getIngestHost() {
        return ingestHost;
    }

    /**
     * @param ingestHost host of the ingest module
     * @return SoapUiConfig
     */
    public SoapUiConfig setIngestHost(String ingestHost) {
        ParametersChecker.checkParameter("ingestHost is mandatory", ingestHost);
        this.ingestHost = ingestHost;
        return this;
    }

    /**
     * @return ingestProtocol
     */
    public String getIngestProtocol() {
        return ingestProtocol;
    }

    /**
     * @param ingestProtocol protocol used to call the ingest module
     * @return SoapUiConfig
     */
    public SoapUiConfig setIngestProtocol(String ingestProtocol) {
        ParametersChecker.checkParameter("ingestProtocol is mandatory", ingestProtocol);
        this.ingestProtocol = ingestProtocol;
        return this;
    }

    /**
     * @return access external port
     */
    public int getAccessExternalPort() {
        return accessExternalPort;
    }

    /**
     * @param accessExternalPort port of the logbook module
     * @return SoapUiConfig
     */
    public SoapUiConfig setAccessExternalPort(int accessExternalPort) {
        ParametersChecker.checkParameter("logbookPort is mandatory", accessExternalPort);
        this.accessExternalPort = accessExternalPort;
        return this;
    }

    /**
     * @return access external Host
     */
    public String getAccessExternalHost() {
        return accessExternalHost;
    }

    /**
     * @param accessExternalHost host of the logbook module
     * @return SoapUiConfig
     */
    public SoapUiConfig setAccessExternalHost(String accessExternalHost) {
        ParametersChecker.checkParameter("logbookHost is mandatory", accessExternalHost);
        this.accessExternalHost = accessExternalHost;
        return this;
    }

    /**
     * @return access external Protocol
     */
    public String getAccessExternalProtocol() {
        return accessExternalProtocol;
    }

    /**
     * @param accessExternalProtocol protocol used to call the logbook module
     * @return SoapUiConfig
     */
    public SoapUiConfig setAccessExternalProtocol(String accessExternalProtocol) {
        ParametersChecker.checkParameter("logbookProtocol is mandatory", accessExternalProtocol);
        this.accessExternalProtocol = accessExternalProtocol;
        return this;
    }

    /**
     * @return certfile
     */
    public String getCertfile() {
        return certfile;
    }

    /**
     * @param certfile certfile that need to be used
     * @return SoapUiConfig
     */
    public SoapUiConfig setCertfile(String certfile) {
        ParametersChecker.checkParameter("certfile is mandatory", certfile);
        this.certfile = certfile;
        return this;
    }
    
    /**
     * @return dataDir
     */
    public String getDataDir() {
        return dataDir;
    }

    /**
     * @param dataDir dataDir containing input SIP for SOAP-UI
     * @return SoapUiConfig
     */
    public SoapUiConfig setDataDir(String dataDir) {
        ParametersChecker.checkParameter("dataDir is mandatory", dataDir);
        this.dataDir = dataDir;
        return this;
    }    
    
    
    /**
     * @return reportingDir
     */
    public String getReportingDir() {
        return reportingDir;
    }

    /**
     * @param reportingDir reportingDir containing output for SOAP-UI
     * @return SoapUiConfig
     */
    public SoapUiConfig setReportingDir(String reportingDir) {
        ParametersChecker.checkParameter("reportingDir is mandatory", reportingDir);
        this.reportingDir = reportingDir;
        return this;
    }

    /**
     * @return configDir
     */
    public String getConfigDir() {
        return configDir;
    }

    /**
     * @param configDir configDir containing input configuration for SOAP-UI
     * @return SoapUiConfig
     */
    public SoapUiConfig setConfigDir(String configDir) {
        ParametersChecker.checkParameter("configDir is mandatory", configDir);
        this.configDir = configDir;
        return this;
    }
    
    /**
     * @return soapUiExecutable
     */
    public String getSoapUiExecutable() {
        return soapUiExecutable;
    }

    /**
     * @param soapUiExecutable path to the binary file that launch soapUI tests
     * @return SoapUiConfig
     */
    public SoapUiConfig setSoapUiExecutable(String soapUiExecutable) {
        ParametersChecker.checkParameter("soapUiExecutable is mandatory", soapUiExecutable);
        this.soapUiExecutable = soapUiExecutable;
        return this;
    }
    
}

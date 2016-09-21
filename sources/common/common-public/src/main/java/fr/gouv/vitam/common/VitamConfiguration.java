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
package fr.gouv.vitam.common;

import java.io.File;

import fr.gouv.vitam.common.logging.SysErrLogger;

/**
 * This class contains default values shared among all services in Vitam
 */
public class VitamConfiguration {
    /**
     * Property Vitam Config Folder
     */
    protected static final String VITAM_CONFIG_PROPERTY = "vitam.config.folder";
    /**
     * Property Vitam Data Folder
     */
    protected static final String VITAM_DATA_PROPERTY = "vitam.data.folder";
    /**
     * Property Vitam Log Folder
     */
    protected static final String VITAM_LOG_PROPERTY = "vitam.log.folder";
    /**
     * Property Vitam Tmp Folder
     */
    protected static final String VITAM_TMP_PROPERTY = "vitam.tmp.folder";
    /**
     * Default Vitam Config Folder
     */
    private static final String VITAM_CONFIG_FOLDER_DEFAULT = "/vitam/conf";
    /**
     * Default Vitam Config Folder
     */
    private static final String VITAM_DATA_FOLDER_DEFAULT = "/vitam/data";
    /**
     * Default Vitam Config Folder
     */
    private static final String VITAM_LOG_FOLDER_DEFAULT = "/vitam/log";
    /**
     * Default Vitam Config Folder
     */
    // TODO change to /vitam/tmp when configured on the PIC
    private static final String VITAM_TMP_FOLDER_DEFAULT = "/vitam/data/tmp";

    private static final VitamConfiguration DEFAULT_CONFIGURATION = new VitamConfiguration().setDefault();

    private String config;
    private String log;
    private String data;
    private String tmp;

    /**
     * Empty constructor
     */
    VitamConfiguration() {
        // empty
    }

    /**
     * Full argument constructor
     * 
     * @param config
     * @param log
     * @param data
     * @param tmp
     * 
     * @throws IllegalArgumentException if one argument is null or empty
     */
    VitamConfiguration(String config, String log, String data, String tmp) {
        setConfig(config).setData(data).setLog(log).setTmp(tmp);
        checkValues();
    }

    /**
     * 
     * @return the default Vitam Configuration
     */
    public static VitamConfiguration getConfiguration() {
        return DEFAULT_CONFIGURATION;
    }

    /**
     * Replace the default values with values embedded in the VitamConfiguration parameter
     * 
     * @param vitamConfiguration the new parameter
     * @throws IllegalArgumentException if one argument is null or empty
     */
    static void setConfiguration(VitamConfiguration vitamConfiguration) {
        DEFAULT_CONFIGURATION.setConfig(vitamConfiguration.getConfig())
            .setData(vitamConfiguration.getData())
            .setLog(vitamConfiguration.getLog())
            .setTmp(vitamConfiguration.getTmp()).checkValues();
    }

    /**
     * Replace the default values with values embedded in the VitamConfiguration parameter
     * 
     * @param config
     * @param log
     * @param data
     * @param tmp
     * @throws IllegalArgumentException if one argument is null or empty
     */
    static void setConfiguration(String config, String log, String data, String tmp) {
        DEFAULT_CONFIGURATION.setConfig(config)
            .setData(data).setLog(log).setTmp(tmp).checkValues();
    }

    /**
     * Set the default values
     * 
     * @return this
     */
    VitamConfiguration setDefault() {
        setConfig(VITAM_CONFIG_FOLDER_DEFAULT)
            .setData(VITAM_DATA_FOLDER_DEFAULT)
            .setLog(VITAM_LOG_FOLDER_DEFAULT)
            .setTmp(VITAM_TMP_FOLDER_DEFAULT);
        checkValues();
        return this;
    }

    /**
     * Get Config directory
     * 
     * @return the Config directory
     */
    public String getConfig() {
        return config;
    }

    /**
     * Set Config directory
     * 
     * @param config the config directory
     * @return this
     */
    public VitamConfiguration setConfig(String config) {
        ParametersChecker.checkParameter("Config directory", config);
        this.config = config;
        return this;
    }

    /**
     * Get Log directory
     * 
     * @return the Log directory
     */
    public String getLog() {
        return log;
    }

    /**
     * Set Log directory
     * 
     * @param log the Log directory
     * @return this
     */
    public VitamConfiguration setLog(String log) {
        ParametersChecker.checkParameter("Log directory", log);
        this.log = log;
        return this;
    }

    /**
     * Get Data directory
     * 
     * @return the Data directory
     */
    public String getData() {
        return data;
    }

    /**
     * Set Data directory
     * 
     * @param data the Data directory
     * @return this
     */
    public VitamConfiguration setData(String data) {
        ParametersChecker.checkParameter("Data directory", data);
        this.data = data;
        return this;
    }

    /**
     * Get Tmp directory
     * 
     * @return the Tmp directory
     */
    public String getTmp() {
        return tmp;
    }

    /**
     * Set Tmp directory
     * 
     * @param tmp tmp the Tmp directory
     * @return this
     */
    public VitamConfiguration setTmp(String tmp) {
        ParametersChecker.checkParameter("Tmp directory", tmp);
        this.tmp = tmp;
        return this;
    }

    /**
     * Check if each directory not null and exists
     * 
     * @throws IllegalArgumentException if one condition failed
     */
    void checkValues() {
        ParametersChecker.checkParameter("Check directories", tmp, data, log, config);
        File tmpDir = new File(tmp);
        File logDir = new File(log);
        File dataDir = new File(data);
        File configDir = new File(config);
        if (!tmpDir.isDirectory()) {
            tmpDir.mkdirs();
        }
        if (!(tmpDir.isDirectory() && logDir.isDirectory() && dataDir.isDirectory() && configDir.isDirectory())) {
            SysErrLogger.FAKE_LOGGER.syserr("One of the directories in the VitamConfiguration is not correct");
        }
    }

    /**
     * Check if Vitam Configuration is specified using directives on JVM.
     * If an issue is detected, it only logs the status on STDERR.
     */
    static void checkVitamConfiguration() {
        if (! (SystemPropertyUtil.contains(VITAM_TMP_PROPERTY)
            && SystemPropertyUtil.contains(VITAM_CONFIG_PROPERTY) &&
            SystemPropertyUtil.contains(VITAM_DATA_PROPERTY) &&
            SystemPropertyUtil.contains(VITAM_LOG_PROPERTY))) {
            SysErrLogger.FAKE_LOGGER.syserr("One of the directives is not specified: -Dxxx=path where xxx is one of -D"
                + VITAM_TMP_PROPERTY + " -D" + VITAM_CONFIG_PROPERTY
                + " -D" + VITAM_DATA_PROPERTY + " -D" + VITAM_LOG_PROPERTY);
        }
    }

    /**
     *
     * @return the VitamTmpFolder path
     */
    public static String getVitamTmpFolder() {
        if (SystemPropertyUtil.contains(VITAM_TMP_PROPERTY)) {
            return SystemPropertyUtil.get(VITAM_TMP_PROPERTY);
        }
        return getConfiguration().getTmp();
    }

    /**
     *
     * @return the VitamLogFolder path
     */
    public static String getVitamLogFolder() {
        if (SystemPropertyUtil.contains(VITAM_LOG_PROPERTY)) {
            return SystemPropertyUtil.get(VITAM_LOG_PROPERTY);
        }
        return getConfiguration().getLog();
    }

    /**
     *
     * @return the VitamDataFolder path
     */
    public static String getVitamDataFolder() {
        if (SystemPropertyUtil.contains(VITAM_DATA_PROPERTY)) {
            return SystemPropertyUtil.get(VITAM_DATA_PROPERTY);
        }
        return getConfiguration().getData();
    }

    /**
     *
     * @return the VitamConfigFolder path
     */
    public static String getVitamConfigFolder() {
        if (SystemPropertyUtil.contains(VITAM_CONFIG_PROPERTY)) {
            return SystemPropertyUtil.get(VITAM_CONFIG_PROPERTY);
        }
        return getConfiguration().getConfig();
    }
}

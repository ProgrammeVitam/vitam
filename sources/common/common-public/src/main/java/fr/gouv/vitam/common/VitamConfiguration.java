/*******************************************************************************
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
 *******************************************************************************/
package fr.gouv.vitam.common;

import java.io.File;

import com.google.common.base.Strings;

import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.logging.SysErrLogger;

/**
 * This class contains default values shared among all services in Vitam
 */
public class VitamConfiguration {

    private static final VitamConfiguration DEFAULT_CONFIGURATION = new VitamConfiguration();
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
    private static final String VITAM_TMP_FOLDER_DEFAULT = "/vitam/data/tmp";
    /**
     * Default Chunk Size
     */
    private static final int CHUNK_SIZE = 65536;
    /**
     * Default Connection timeout
     */
    private static final int CONNECT_TIMEOUT = 2000;
    /**
     * Default Read Timeout
     */
    private static final int READ_TIMEOUT = 86400;

    /**
     * Max total concurrent clients
     */
    private static final int MAX_TOTAL_CLIENT = 500;
    /**
     * Max concurrent clients associated to one host
     */
    private static final int MAX_CLIENT_PER_HOST = 100;
    /**
     * Max delay to check an unused client in pool before being returned (Apache Only)
     */
    private static final int DELAY_VALIDATION_AFTER_INACTIVITY = 10000;
    /**
     * No check of unused client within pool (Apache Only)
     */
    public static final int NO_VALIDATION_AFTER_INACTIVITY = -1;
    /**
     * Max delay to get a client (Apache Only)
     */
    private static final int DELAY_GET_CLIENT = 10000;
    /**
     * Specify the delay where connections returned to pool will be checked (Apache Only)
     */
    private static final int INTERVAL_DELAY_CHECK_IDLE = 5000;
    /**
     * Specify the delay of unused connection returned in the pool before being really closed (Apache Only)
     */
    private static final int MAX_DELAY_UNUSED_CONNECTION = 10000;
    /**
     * General Admin path
     */
    public static final String ADMIN_PATH = "/admin/v1";
    /**
     * General status path
     */
    public static final String STATUS_URL = "/status";
    /**
     * Default Digest Type for SECURITY
     */
    private static final DigestType SECURITY_DIGEST_TYPE = DigestType.SHA256;
    /**
     * Default Digest Type for Vitam
     */
    private static final DigestType DEFAULT_DIGEST_TYPE = DigestType.SHA512;
    /**
     *  Default Digest Type for time stamp generation
     */
    private static final DigestType DEFAULT_TIMESTAMP_DIGEST_TYPE = DigestType.SHA512;
    /**
     * Acceptable Request Time
     */
    private static final long ACCEPTABLE_REQUEST_TIME = 10;
    /**
     * MongoDB client configuration
     */
    private static final int THREADS_ALLOWED_TO_BLOCK_FOR_CONNECTION_MULTIPLIERS = 1500;
    /**
     * Retry repetition
     */
    private static final int RETRY_NUMBER = 3;
    /**
     * Retry delay
     */
    private static final int RETRY_DELAY = 30000;
    /**
     * Waiting delay (for wait(delay) method)
     */
    private static final int WAITING_DELAY = 1000;

    private String config;
    private String log;
    private String data;
    private String tmp;
    private static String secret;
    private static boolean filterActivation;
    private int connectTimeout = CONNECT_TIMEOUT;

    static {
        getConfiguration().setDefault();
    }
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
     *
     * @param vitamConfiguration
     */
    void setInternalConfiguration(VitamConfiguration vitamConfiguration) {
        setConfig(vitamConfiguration.getConfig())
            .setData(vitamConfiguration.getData())
            .setLog(vitamConfiguration.getLog())
            .setTmp(vitamConfiguration.getTmp()).checkValues();
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
        checkVitamConfiguration();
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
        final File tmpDir = new File(tmp);
        final File logDir = new File(log);
        final File dataDir = new File(data);
        final File configDir = new File(config);
        if (!tmpDir.isDirectory()) {
            tmpDir.mkdirs();
        }
        if (!(tmpDir.isDirectory() && logDir.isDirectory() && dataDir.isDirectory() && configDir.isDirectory())) {
            SysErrLogger.FAKE_LOGGER.syserr("One of the directories in the VitamConfiguration is not correct");
        }
    }

    /**
     * Check if Vitam Configuration is specified using directives on JVM. If an issue is detected, it only logs the
     * status on STDERR.
     */
    static void checkVitamConfiguration() {
        if (!(SystemPropertyUtil.contains(VITAM_TMP_PROPERTY) && SystemPropertyUtil.contains(VITAM_CONFIG_PROPERTY) &&
            SystemPropertyUtil.contains(VITAM_DATA_PROPERTY) &&
            SystemPropertyUtil.contains(VITAM_LOG_PROPERTY))) {
            SysErrLogger.FAKE_LOGGER.syserr(
                "One of the directives is not specified: -Dxxx=path where xxx is one of -D" + VITAM_TMP_PROPERTY +
                    " -D" + VITAM_CONFIG_PROPERTY + " -D" + VITAM_DATA_PROPERTY + " -D" + VITAM_LOG_PROPERTY);
        }
        String data = VITAM_DATA_FOLDER_DEFAULT;
        if (SystemPropertyUtil.contains(VITAM_DATA_PROPERTY)) {
            data = SystemPropertyUtil.get(VITAM_DATA_PROPERTY);
        }
        String tmp = VITAM_TMP_FOLDER_DEFAULT;
        if (SystemPropertyUtil.contains(VITAM_TMP_PROPERTY)) {
            tmp = SystemPropertyUtil.get(VITAM_TMP_PROPERTY);
        }
        String config = VITAM_CONFIG_FOLDER_DEFAULT;
        if (SystemPropertyUtil.contains(VITAM_CONFIG_PROPERTY)) {
            config = SystemPropertyUtil.get(VITAM_CONFIG_PROPERTY);
        }
        String log = VITAM_LOG_FOLDER_DEFAULT;
        if (SystemPropertyUtil.contains(VITAM_LOG_PROPERTY)) {
            log = SystemPropertyUtil.get(VITAM_LOG_PROPERTY);
        }
        setConfiguration(config, log, data, tmp);
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
        return getConfiguration().getLog();
    }

    /**
     *
     * @return the VitamDataFolder path
     */
    public static String getVitamDataFolder() {
        return getConfiguration().getData();
    }

    /**
     *
     * @return the VitamConfigFolder path
     */
    public static String getVitamConfigFolder() {
        return getConfiguration().getConfig();
    }

    /**
     * @return the default chunk size
     */
    public static int getChunkSize() {
        return CHUNK_SIZE;
    }

    /**
     * @return the default connect timeout
     */
    public static int getConnectTimeout() {
        return getConfiguration().connectTimeout;
    }

    /**
     * Junit facility
     *
     * @param timeout
     */
    public static void setConnectTimeout(int timeout) {
        getConfiguration().connectTimeout = timeout;
    }

    /**
     * @return the default read timeout
     */
    public static int getReadTimeout() {
        return READ_TIMEOUT;
    }

    /**
     * @return the maxTotalClient
     */
    public static int getMaxTotalClient() {
        return MAX_TOTAL_CLIENT;
    }

    /**
     * @return the maxClientPerHost
     */
    public static int getMaxClientPerHost() {
        return MAX_CLIENT_PER_HOST;
    }

    /**
     * @return the delayValidationAfterInactivity
     */
    public static int getDelayValidationAfterInactivity() {
        return DELAY_VALIDATION_AFTER_INACTIVITY;
    }

    /**
     * @return the delayGetClient
     */
    public static int getDelayGetClient() {
        return DELAY_GET_CLIENT;
    }

    /**
     * @return the intervalDelayCheckIdle
     */
    public static int getIntervalDelayCheckIdle() {
        return INTERVAL_DELAY_CHECK_IDLE;
    }

    /**
     * @return the maxDelayUnusedConnection
     */
    public static int getMaxDelayUnusedConnection() {
        return MAX_DELAY_UNUSED_CONNECTION;
    }

    /**
     * @return the secret
     */
    public static String getSecret() {
        if (Strings.isNullOrEmpty(secret)) {
            return "";
        }
        return secret;
    }

    /**
     * @param secretValue the secret to set
     *
     */
    public static void setSecret(String secretValue) {
        ParametersChecker.checkParameter("Platform secret", secretValue);
        secret = secretValue;
    }

    /**
     * @return the filterActivation
     */
    public static boolean isFilterActivation() {
        return filterActivation;
    }

    /**
     * @param filterActivationValue the filterActivation to set
     *
     */
    public static void setFilterActivation(boolean filterActivationValue) {
        filterActivation = filterActivationValue;
    }

    /**
     * @return the acceptableRequestTime
     */
    public static long getAcceptableRequestTime() {
        return ACCEPTABLE_REQUEST_TIME;
    }

    /**
     * @return the securityDigestType
     */
    public static DigestType getSecurityDigestType() {
        return SECURITY_DIGEST_TYPE;
    }

    /**
     * @return the Default DigestType
     */
    public static DigestType getDefaultDigestType() {
        return DEFAULT_DIGEST_TYPE;
    }

    /**
     * @return the Default DigestType for time stamp generation
     */
    public static DigestType getDefaultTimestampDigestType() {
        return DEFAULT_TIMESTAMP_DIGEST_TYPE;
    }


    /**
     * @return the threadsAllowedToBlockForConnectionMultipliers for MongoDb Client
     */
    public static int getThreadsAllowedToBlockForConnectionMultipliers() {
        return THREADS_ALLOWED_TO_BLOCK_FOR_CONNECTION_MULTIPLIERS;
    }

    /**
     * @return the retryNumber
     */
    public static int getRetryNumber() {
        return RETRY_NUMBER;
    }

    /**
     * @return the retryDelay
     */
    public static int getRetryDelay() {
        return RETRY_DELAY;
    }

    /**
     * @return the waiting Delay (wait)
     */
    public static int getWaitingDelay() {
        return WAITING_DELAY;
    }
}

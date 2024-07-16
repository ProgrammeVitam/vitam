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
package fr.gouv.vitam.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.SysErrLogger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * Server Identity containing ServerName, ServerRole, Global PlatformId<br>
 * This is a private Common.<br>
 * <br>
 * By default this class is initialized with default values:<br>
 * <ul>
 * <li>ServerName: hostname or UnknownHostname if not found</li>
 * <li>ServerRole: UnknownRole</li>
 * <li>ServerId: partial MAC ADDRESS as integer</li>
 * <li>SiteId : 0</li>
 * </ul>
 * <br>
 * One should initialize its server instance by calling:<br>
 *
 * <pre>
 * ServerIdentity serverIdentity = ServerIdentity.getInstance();
 * serverIdentity.setName(name).setRole(role).setPlatformId(platformId);
 * or
 * ServerIdentity.getInstance().setFromMap(map);
 * or
 * ServerIdentity.getInstance().setFromPropertyFile(file);
 * </pre>
 *
 * Where name, role and platformID comes from a configuration file for instance.<br>
 * <br>
 * Role could be a multiple, noted as role1_role2_role3. <br>
 * Usage:<br>
 *
 * <pre>
 * ServerIdentity serverIdentity = ServerIdentity.getInstance();
 * String name = serverIdentity.getName();
 * String role = serverIdentity.getRole();
 * int platformId = serverIdentity.getPlatformId();
 * </pre>
 *
 * Main usages are for:<br/>
 * <ul>
 * <li>GUID for PlatformId</li>
 * <li>Logger and Logbook: for all</li>
 * </ul>
 * <br>
 * <br>
 * NOTE for developers:<br/>
 * <ul>
 * <li>Do not add LOGGER there</li>
 * <li>Do not modify directly serverId attribute . Use setServerId</li>
 * <li>Do not modify directly siteId attribute . Use setSiteId</li>
 * </ul>
 */

public final class ServerIdentity implements ServerIdentityInterface {

    private static final int MAC_ADDRESS_SUBSTRACT_LENGTH = 4;
    private static final String SERVER_IDENTITY_CONF_FILE_NAME = "server-identity.conf";
    private static final int OTHER_ADDRESS = 4;
    private static final int SITE_LOCAL_ADDRESS = 3;
    private static final int LINKLOCAL_ADDRESS = 2;
    private static final int MULTICAST_ADDRESS = 1;
    private static final int LOCAL_ADDRESS = 0;
    private static final String UNKNOWN_ROLE = "UnknownRole";
    private static final String UNKNOWN_HOSTNAME = "UnknownHostname";
    private static final String HOSTNAME_CMD = "hostname";
    private static final String HOSTNAME = "HOSTNAME";
    private static final String COMPUTERNAME = "COMPUTERNAME";
    private static final String WIN = "win";
    private static final String OS_NAME = "os.name";
    /**
     * MAC ADDRESS support: support for default 6 and 8 bytes length, but also special 4 bytes
     */
    private static final Pattern MACHINE_ID_PATTERN = Pattern.compile("^(?:[0-9a-fA-F][:-]?){4,8}$");
    private static final int MACHINE_ID_LEN = 4;
    private static final int BYTE_LENGTH = 8;
    private static final int MAC_SIZE = 3;
    private static final int MACHINE_VALUE_PAIR = 2;
    private static final int HEXA_BASE = 16;
    private static final int MINIMAL_ADDRESS_LENGTH = 6;
    private static final int MULTICAST_VALUE = 1;
    private static final int GLOBAL_ADDRESS_VALUE = 1;

    private static final ServerIdentity SERVER_IDENTITY = new ServerIdentity();

    private String name;
    private String role;
    // This attribute must NEVER be modified directly . Use setServerId(value)
    private int serverId;
    // This attribute must NEVER be modified directly . Use setSiteId(value)
    private int siteId;
    private final StringBuilder preMessage = new StringBuilder();
    private String preMessageString;
    private int globalPlatformId;

    private ServerIdentity() {
        defaultServerIdentity();
        ServerIdentityConfigurationImpl serverIdentityConf;
        try {
            final File file = PropertiesUtils.findFile(SERVER_IDENTITY_CONF_FILE_NAME);
            serverIdentityConf = PropertiesUtils.readYaml(file, ServerIdentityConfigurationImpl.class);
            setYamlConfiguration(serverIdentityConf);
            initializeCommentFormat();
        } catch (final IOException e) {
            SysErrLogger.FAKE_LOGGER.syserr("Issue while getting configuration File: " + e.getMessage());
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }
    }

    void defaultServerIdentity() {
        boolean found = false;
        // Compute name from Hostname
        try {
            if (SystemPropertyUtil.get(OS_NAME).toLowerCase().startsWith(WIN)) {
                // Just for fun
                name = System.getenv(COMPUTERNAME);
                found = true;
            }
        } catch (final Exception e) {
            // ignore
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }
        if (!found) {
            try {
                name = System.getenv(HOSTNAME);
                found = true;
            } catch (final SecurityException e) {
                // ignore
                SysErrLogger.FAKE_LOGGER.ignoreLog(e);
            }
            if (!found || name == null) {
                // Some Unix do return null
                executeHostnameCommand();
                if (name == null) {
                    getHostnameThroughInetAddress();
                }
            }
        }
        name = name.replaceAll("[\n\r]", "");
        role = UNKNOWN_ROLE;
        setServerId(macAddress(macAddress()));
        setSiteId(0);
        initializeCommentFormat();
    }

    /**
     * Get the Hostname through InetAddess
     */
    private void getHostnameThroughInetAddress() {
        // Warning since it could return the real IP
        try {
            name = InetAddress.getLocalHost().getHostName();
        } catch (final UnknownHostException e) {
            name = UNKNOWN_HOSTNAME;
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }
    }

    /**
     * Execute the Hostname Command
     */
    private void executeHostnameCommand() {
        Process proc = null;
        try {
            proc = Runtime.getRuntime().exec(HOSTNAME_CMD);
            try (InputStream stream = proc.getInputStream()) {
                try (Scanner scanner = new Scanner(stream)) {
                    name = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
                }
            }
        } catch (final IOException e) {
            // ignore since will be checked just after
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        } finally {
            if (proc != null) {
                proc.destroy();
            }
        }
    }

    /**
     * initialize after each configuration change the Logger pre-message
     */
    private void initializeCommentFormat() {
        preMessage.setLength(0);
        preMessage
            .append('[')
            .append(getName())
            .append(':')
            .append(getRole())
            .append(':')
            .append(getGlobalPlatformId())
            .append("] ");
        preMessageString = preMessage.toString();
    }

    @JsonIgnore
    @Override
    public final String getLoggerMessagePrepend() {
        return preMessageString;
    }

    /**
     * @return the Json representation of the ServerIdentity
     */
    @JsonIgnore
    public final String getJsonIdentity() {
        return JsonHandler.unprettyPrint(this);
    }

    /**
     * @return the current instance of ServerIdentity
     */
    public static final ServerIdentity getInstance() {
        return SERVER_IDENTITY;
    }

    /**
     * Map key for setFromMap
     */
    public enum MAP_KEYNAME {
        /**
         * Server Hostname: String
         */
        NAME,
        /**
         * Server Role: String
         */
        ROLE,
        /**
         * SiteId: Integer or String representing integer
         */
        SITEID,
        /**
         * ServerId (Id of the VM/server): Integer or String representing integer
         */
        SERVERID,
    }

    /**
     * Assign the ServerIdentity from a Property file where Key are elements from MAP_KEYNAME. <br>
     * Other keys are ignored. Illegal values are ignored.
     *
     * @param propertiesFile file
     * @return this
     * @throws FileNotFoundException if the property file is not found
     */
    @JsonIgnore
    public final ServerIdentity setFromPropertyFile(File propertiesFile) throws FileNotFoundException {
        try {
            final Properties properties = PropertiesUtils.readProperties(propertiesFile);
            String svalue = properties.getProperty(MAP_KEYNAME.NAME.name());
            if (svalue != null) {
                name = svalue;
            }
            svalue = properties.getProperty(MAP_KEYNAME.ROLE.name());
            if (svalue != null) {
                role = svalue;
            }
            svalue = properties.getProperty(MAP_KEYNAME.SERVERID.name());
            if (svalue != null) {
                setServerId(Integer.parseInt(svalue));
            }
            svalue = properties.getProperty(MAP_KEYNAME.SITEID.name());
            if (svalue != null) {
                setSiteId(Integer.parseInt(svalue));
            }
        } catch (final IOException | NumberFormatException e) {
            // ignore
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }
        initializeCommentFormat();
        return this;
    }

    /**
     * Assign the ServerIdentity from a Yaml file where Key are elements from MAP_KEYNAME. <br>
     * Other keys are ignored. Illegal values are ignored.
     *
     * @param yamlFile file
     * @return this
     * @throws FileNotFoundException if the Yaml file is not found
     */
    @JsonIgnore
    public final ServerIdentity setFromYamlFile(File yamlFile) throws FileNotFoundException {
        try {
            final ServerIdentityConfigurationImpl serverIdentityConf = PropertiesUtils.readYaml(
                yamlFile,
                ServerIdentityConfigurationImpl.class
            );
            setYamlConfiguration(serverIdentityConf);
        } catch (final IOException e) {
            // ignore
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }
        initializeCommentFormat();
        return this;
    }

    private final void setYamlConfiguration(ServerIdentityConfigurationImpl serverIdentityConf) {
        name = serverIdentityConf.getIdentityName();
        role = serverIdentityConf.getIdentityRole();
        if (serverIdentityConf.getIdentityServerId() > 0) {
            setServerId(serverIdentityConf.getIdentityServerId());
        }
        if (serverIdentityConf.getIdentitySiteId() > 0) {
            setSiteId(serverIdentityConf.getIdentitySiteId());
        }
    }

    private final String getStringFromMap(Map<String, Object> map, String key) {
        final Object value = map.get(key);
        if (value != null && value instanceof String) {
            final String svalue = ((String) value).trim();
            if (!svalue.isEmpty()) {
                return svalue;
            }
        }
        return null;
    }

    private final Integer getIntegerFromMap(Map<String, Object> map, String key) {
        final Object value = map.get(key);
        if (value != null) {
            if (value instanceof String) {
                final String svalue = ((String) value).trim();
                if (!svalue.isEmpty()) {
                    try {
                        return Integer.parseInt(svalue);
                    } catch (final NumberFormatException e) {
                        return null;
                    }
                }
            } else if (value instanceof Number) {
                return ((Number) value).intValue();
            }
        }
        return null;
    }

    /**
     * Assign the ServerIdentity from a Map where Key are elements from MAP_KEYNAME. <br>
     * Other keys are ignored. Illegal values are ignored.
     *
     * @param map the map from which the values are to be set
     * @return this
     * @throws IllegalArgumentException map null
     */
    @JsonIgnore
    public final ServerIdentity setFromMap(Map<String, Object> map) {
        ParametersChecker.checkParameter("map", map);
        String svalue = getStringFromMap(map, MAP_KEYNAME.NAME.name());
        if (svalue != null) {
            name = svalue;
        }
        svalue = getStringFromMap(map, MAP_KEYNAME.ROLE.name());
        if (svalue != null) {
            role = svalue;
        }
        final Integer pid = getIntegerFromMap(map, MAP_KEYNAME.SERVERID.name());
        if (pid != null) {
            setServerId(pid.intValue());
        }
        final Integer sId = getIntegerFromMap(map, MAP_KEYNAME.SITEID.name());
        if (sId != null) {
            setSiteId(sId.intValue());
        }

        initializeCommentFormat();
        return this;
    }

    @Override
    public final String getName() {
        return name;
    }

    /**
     * @param name the name of the Server to set
     * @return this
     * @throws IllegalArgumentException name null
     */
    public final ServerIdentity setName(String name) {
        ParametersChecker.checkParameter("Name", name);
        this.name = name;
        initializeCommentFormat();
        return this;
    }

    @Override
    public final String getRole() {
        return role;
    }

    /**
     * @param role the role of the Server to set
     * @return this
     * @throws IllegalArgumentException role
     */
    public final ServerIdentity setRole(String role) {
        ParametersChecker.checkParameter("Role", role);
        this.role = role;
        initializeCommentFormat();
        return this;
    }

    // TODO : P2 protect race condition on GlobalPlatform ID
    private final void calculateGlobalPlatformId() {
        globalPlatformId = ((siteId & 0x0F) << 27) + (serverId & 0x07FFFFFF);
    }

    @Override
    public final int getGlobalPlatformId() {
        return globalPlatformId;
    }

    /**
     * The PlatformId is a unique name per site (each of the 3 sites of Vitam should have a different id).
     *
     * @param serverId the platformId of the Vitam Platform to set
     * @return this
     * @throws IllegalArgumentException platformId &lt; 0
     */
    public final ServerIdentity setServerId(int serverId) {
        ParametersChecker.checkValue("server", serverId, 0);
        this.serverId = serverId;
        calculateGlobalPlatformId();
        initializeCommentFormat();
        return this;
    }

    @Override
    public final int getServerId() {
        return serverId;
    }

    /**
     * @return the siteID
     */
    @Override
    public final int getSiteId() {
        return siteId;
    }

    /**
     * Set the SideID
     *
     * @param siteId the siteId to set
     * @return this
     */
    public final ServerIdentity setSiteId(int siteId) {
        ParametersChecker.checkValue("siteID", siteId, 0);
        this.siteId = siteId;
        calculateGlobalPlatformId();
        initializeCommentFormat();
        return this;
    }

    /**
     * @return the mac address if possible, else random values
     */
    private static final byte[] macAddress() {
        try {
            byte[] machineId = null;
            final String customMachineId = SystemPropertyUtil.get("fr.gouv.vitam.machineId");
            if (customMachineId != null && MACHINE_ID_PATTERN.matcher(customMachineId).matches()) {
                machineId = parseMachineId(customMachineId);
            }
            if (machineId == null) {
                machineId = defaultMachineId();
            }
            machineId[0] &= 0x7F;
            return machineId;
        } catch (final Exception e) {
            // Could not get MAC address: generate a random one
            final byte[] machineId = StringUtils.getRandom(MACHINE_ID_LEN);
            machineId[0] &= 0x7F;
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
            return machineId;
        }
    }

    /**
     * @param mac
     * @return the last 31 bits of the macAddress
     */
    private static final int macAddress(byte[] mac) {
        int macl = 0;
        if (mac == null) {
            return macl;
        }
        int i = mac.length - MAC_ADDRESS_SUBSTRACT_LENGTH;
        if (i < 0) {
            i = 0;
        }
        macl |= (mac[i++] & 0x7F) << (MAC_SIZE * BYTE_LENGTH);
        for (int j = 1; i < mac.length; i++, j++) {
            macl |= (mac[i] & 0xFF) << ((MAC_SIZE - j) * BYTE_LENGTH);
        }
        return macl;
    }

    private static final byte[] parseMachineId(final String valueSource) {
        // Strip separators.
        final String value = valueSource.replaceAll("[:-]", "");

        final byte[] machineId = new byte[MACHINE_ID_LEN];
        int i = value.length() / MACHINE_VALUE_PAIR;
        for (int j = 0; i < value.length() && j < MACHINE_ID_LEN; i += MACHINE_VALUE_PAIR, j++) {
            machineId[j] = (byte) Integer.parseInt(value.substring(i, i + MACHINE_VALUE_PAIR), HEXA_BASE);
        }
        return machineId;
    }

    // Inspired from Netty MacAddressUtil

    /**
     * Obtains the best MAC address found on local network interfaces. Generally speaking, an active network interface
     * used on public networks is better than a local network interface.
     *
     * @return byte array containing a MAC. null if no MAC can be found.
     */
    private static final byte[] defaultMachineId() {
        final byte[] notFound = { -1 };
        final byte[] localhost4Bytes = { 127, 0, 0, 1 };
        // Find the best MAC address available.
        byte[] bestMacAddr = notFound;
        InetAddress bestInetAddr = null;
        try {
            bestInetAddr = InetAddress.getByAddress(localhost4Bytes);
        } catch (final Exception e) {
            // Should not happen
            throw new IllegalArgumentException(e);
        }
        // Retrieve the list of available network interfaces.
        final Map<NetworkInterface, InetAddress> ifaces = new LinkedHashMap<>();
        try {
            for (
                final Enumeration<NetworkInterface> i = NetworkInterface.getNetworkInterfaces();
                i.hasMoreElements();
            ) {
                final NetworkInterface iface = i.nextElement();
                // Use the interface with proper INET addresses only.
                final Enumeration<InetAddress> addrs = iface.getInetAddresses();
                if (addrs.hasMoreElements()) {
                    final InetAddress a = addrs.nextElement();
                    if (!a.isLoopbackAddress()) {
                        ifaces.put(iface, a);
                    }
                }
            }
        } catch (final SocketException e) {
            // Should not happen
            throw new IllegalArgumentException(e);
        }
        for (final Entry<NetworkInterface, InetAddress> entry : ifaces.entrySet()) {
            final NetworkInterface iface = entry.getKey();
            final InetAddress inetAddr = entry.getValue();
            if (iface.isVirtual()) {
                continue;
            }

            final byte[] macAddr;
            try {
                macAddr = iface.getHardwareAddress();
            } catch (final SocketException e) {
                SysErrLogger.FAKE_LOGGER.ignoreLog(e);
                continue;
            }
            boolean replace = false;
            int res = compareAddresses(bestMacAddr, macAddr);
            if (res < 0) {
                // Found a better MAC address.
                replace = true;
            } else if (res == 0) {
                // Two MAC addresses are of pretty much same quality.
                res = compareAddresses(bestInetAddr, inetAddr);
                if (
                    bestMacAddr != null &&
                    macAddr != null &&
                    (res < 0 || (res == 0 && bestMacAddr.length < macAddr.length))
                ) {
                    // if res < 0: Found a MAC address with better INET address.
                    // Else: Cannot tell the difference. Choose the longer one.
                    replace = true;
                }
            }
            if (replace) {
                bestMacAddr = macAddr;
                bestInetAddr = inetAddr;
            }
        }
        if (bestMacAddr == notFound) {
            bestMacAddr = StringUtils.getRandom(MACHINE_ID_LEN);
        }
        return bestMacAddr;
    }

    /**
     * @return positive - current is better, 0 - cannot tell from MAC addr, negative - candidate is better.
     */
    private static final int compareAddresses(final byte[] current, final byte[] candidate) {
        if (candidate == null) {
            return 1;
        }
        // Must be EUI-48 or longer.
        if (candidate.length < MINIMAL_ADDRESS_LENGTH) {
            return 1;
        }
        // Must not be filled with only 0 and 1.
        boolean onlyZeroAndOne = true;
        for (final byte b : candidate) {
            if (b != 0 && b != 1) {
                onlyZeroAndOne = false;
                break;
            }
        }
        if (onlyZeroAndOne) {
            return 1;
        }
        // Must not be a multicast address
        if ((candidate[0] & MULTICAST_VALUE) != 0) {
            return 1;
        }
        // Prefer globally unique address.
        if ((current[0] & GLOBAL_ADDRESS_VALUE) == 0) {
            if ((candidate[0] & GLOBAL_ADDRESS_VALUE) == 0) {
                // Both current and candidate are globally unique addresses.
                return 0;
            } else {
                // Only current is globally unique.
                return 1;
            }
        } else {
            if ((candidate[0] & GLOBAL_ADDRESS_VALUE) == 0) {
                // Only candidate is globally unique.
                return -1;
            } else {
                // Both current and candidate are non-unique.
                return 0;
            }
        }
    }

    /**
     * @return positive - current is better, 0 - cannot tell, negative - candidate is better
     */
    private static final int compareAddresses(final InetAddress current, final InetAddress candidate) {
        return scoreAddress(current) - scoreAddress(candidate);
    }

    /**
     * @param addr
     * @return the score of this address
     */
    private static final int scoreAddress(final InetAddress addr) {
        if (addr.isAnyLocalAddress() || addr.isLoopbackAddress()) {
            return LOCAL_ADDRESS;
        }
        if (addr.isMulticastAddress()) {
            return MULTICAST_ADDRESS;
        }
        if (addr.isLinkLocalAddress()) {
            return LINKLOCAL_ADDRESS;
        }
        if (addr.isSiteLocalAddress()) {
            return SITE_LOCAL_ADDRESS;
        }
        return OTHER_ADDRESS;
    }

    /**
     * Implementation of ServerIdentityConfiguration Interface
     */
    public static class ServerIdentityConfigurationImpl implements ServerIdentityConfiguration {

        private static final String CONFIGURATION_PARAMETERS = "ServerIdentityConfiguration parameters";
        private String identityName;
        private int identityServerId;
        private int identitySiteId;
        private String identityRole;

        /**
         * ServerIdentityConfiguration empty constructor for YAMLFactory
         */
        public ServerIdentityConfigurationImpl() {
            // empty
        }

        /**
         * ServerIdentityConfiguration constructor
         *
         * @param identityName database identity name
         * @param identityServerId identity server id
         * @param identitySiteId identity site id
         * @param identityRole identity role
         * @throws IllegalArgumentException if identityName or identityRole
         */
        public ServerIdentityConfigurationImpl(
            String identityName,
            int identityServerId,
            int identitySiteId,
            String identityRole
        ) {
            ParametersChecker.checkParameter(CONFIGURATION_PARAMETERS, identityName, identityRole);
            if (identitySiteId < 0) {
                throw new IllegalArgumentException("Site id must be positive");
            }
            if (identityServerId < 0) {
                throw new IllegalArgumentException("Server id must be positive");
            }
            this.identityName = identityName;
            this.identityServerId = identityServerId;
            this.identitySiteId = identitySiteId;
            this.identityRole = identityRole;
        }

        @Override
        public String getIdentityName() {
            return identityName;
        }

        @Override
        public int getIdentityServerId() {
            return identityServerId;
        }

        @Override
        public int getIdentitySiteId() {
            return identitySiteId;
        }

        @Override
        public String getIdentityRole() {
            return identityRole;
        }

        /**
         * @param identityName the identity Name to set
         * @return this
         * @throws IllegalArgumentException if identityName is null or empty
         */
        public ServerIdentityConfigurationImpl setIdentityName(String identityName) {
            ParametersChecker.checkParameter(CONFIGURATION_PARAMETERS, identityName);
            this.identityName = identityName;
            return this;
        }

        /**
         * @param identityServerId the identityServerId to set
         * @return this
         * @throws IllegalArgumentException if identityServerId &lt; 0
         */
        public ServerIdentityConfigurationImpl setIdentityServerId(int identityServerId) {
            if (identityServerId < 0) {
                throw new IllegalArgumentException("identityServerId id must be positive");
            }
            this.identityServerId = identityServerId;
            return this;
        }

        /**
         * @param identitySiteId the identitySiteId to set
         * @return this
         * @throws IllegalArgumentException if identitySiteId &lt; 0
         */
        public ServerIdentityConfigurationImpl setIdentitySiteId(int identitySiteId) {
            if (identitySiteId < 0) {
                throw new IllegalArgumentException("identitySiteId id must be positive");
            }
            this.identitySiteId = identitySiteId;
            return this;
        }

        /**
         * @param identityRole the identityRole to set
         * @return this
         * @throws IllegalArgumentException if identityRole is null or empty
         */
        public ServerIdentityConfigurationImpl setIdentityRole(String identityRole) {
            ParametersChecker.checkParameter(CONFIGURATION_PARAMETERS, identityRole);
            this.identityRole = identityRole;
            return this;
        }
    }
}

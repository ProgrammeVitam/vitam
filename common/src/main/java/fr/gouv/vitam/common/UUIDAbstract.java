/*******************************************************************************
 * This file is part of Vitam Project.
 * 
 * Copyright Vitam (2012, 2015)
 *
 * This software is governed by the CeCILL 2.1 license under French law and
 * abiding by the rules of distribution of free software. You can use, modify
 * and/ or redistribute the software under the terms of the CeCILL license as
 * circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify
 * and redistribute granted by the license, users are provided only with a
 * limited warranty and the software's author, the holder of the economic
 * rights, and the successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with
 * loading, using, modifying and/or developing or reproducing the software by
 * the user in light of its specific status of free software, that may mean that
 * it is complicated to manipulate, and that also therefore means that it is
 * reserved for developers and experienced professionals having in-depth
 * computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling
 * the security of their systems and/or data to be ensured and, more generally,
 * to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.common;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import fr.gouv.vitam.common.exception.InvalidUuidOperationException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

/**
 * UUID Abstract implementation
 * 
 * @param <E>
 *            Real Class
 *
 * 
 */
public abstract class UUIDAbstract<E> implements UUID<E> {
    /**
     * ARK header
     */
    public static final String ARK = "ark:/";

    private static final VitamLogger LOGGER =
            VitamLoggerFactory.getInstance(UUIDAbstract.class);

    /**
     * Random Generator
     */
    protected static final ThreadLocalRandom RANDOM = ThreadLocalRandom.current();
    /**
     * So MAX value on 3 bytes (64 system use 2^22 id)
     */
    private static final int MAX_PID = 4194304;
    /**
     * MAC ADDRESS support: support for default 6 and 8 bytes length, but also
     * special 4 bytes
     */
    protected static final Pattern MACHINE_ID_PATTERN =
            Pattern.compile("^(?:[0-9a-fA-F][:-]?){4,8}$");
    private static final int MACHINE_ID_LEN = 4;

    /**
     * 2^22-1 bytes value maximum
     */
    protected static final int JVMPID;
    /**
     * Try to get Mac Address but could be also changed dynamically
     */
    protected static final byte[] MAC;
    protected static final int MACI;

    static {
        MAC = macAddress();
        MACI = macAddress(MAC);
        JVMPID = jvmProcessId();
    }
    /**
     * real UUID
     */
    @JsonIgnore
    protected final byte[] uuid;

    /**
     * Internal constructor
     * 
     * @param size
     *            size of the byte representation
     */
    protected UUIDAbstract(int size) {
        uuid = new byte[size];
    }

    /**
     * Internal function
     * 
     * @param bytes
     * @param size
     *            size of the byte representation
     * @return this
     * @throws InvalidUuidOperationException
     */
    @JsonIgnore
    protected UUIDAbstract<E> setBytes(final byte[] bytes, int size)
            throws InvalidUuidOperationException {
        if (bytes.length != size) {
            throw new InvalidUuidOperationException("Attempted to parse malformed UUID: ("
                    + bytes.length + ") " + Arrays.toString(bytes));
        }
        System.arraycopy(bytes, 0, uuid, 0, size);
        return this;
    }

    @Override
    @JsonIgnore
    public String toBase32() {
        return BaseXx.getBase32(uuid);
    }

    @Override
    @JsonIgnore
    public String toBase64() {
        return BaseXx.getBase64(uuid);
    }

    @Override
    @JsonIgnore
    public String toHex() {
        return BaseXx.getBase16(uuid);
    }

    @Override
    @JsonIgnore
    public String toArk() {
        return new StringBuilder(ARK).append(getDomainId())
                .append('/').append(toArkName()).toString();
    }
    
    @JsonGetter("id")
    protected String getId() {
        return toString();
    }

    @Override
    public String toString() {
        return toBase32();
    }

    @Override
    @JsonIgnore
    public byte[] getBytes() {
        return Arrays.copyOf(uuid, uuid.length);
    }

    @Override
    @JsonIgnore
    public int hashCode() {
        return Arrays.hashCode(uuid);
    }

    @Override
	public boolean equals(Object o) {
    	if (o == null || !(o instanceof UUIDAbstract)) {
            return false;
        }
        return (this == o) || Arrays.equals(uuid, ((UUIDAbstract<?>) o).uuid);
	}

	@Override
    @JsonIgnore
    public int getPlatformId() {
        byte[] platform = getMacFragment();
        int pos = platform.length - 4;
        return ((platform[pos] & 0xFF) << 24) | ((platform[pos + 1] & 0xFF) << 16) |
                ((platform[pos + 2] & 0xFF) << 8) | (platform[pos + 3] & 0xFF);
    }

    /**
     *
     * @param length
     * @return a byte array with random values
     */
    public static final byte[] getRandom(final int length) {
        final byte[] result = new byte[length];
        RANDOM.nextBytes(result);
        return result;
    }

    /**
     *
     * @return the mac address if possible, else random values
     */
    private static final byte[] macAddress() {
        try {
            byte[] machineId = null;
            final String customMachineId =
                    SystemPropertyUtil.get("fr.gouv.vitam.machineId");
            if (customMachineId != null) {
                if (MACHINE_ID_PATTERN.matcher(customMachineId).matches()) {
                    machineId = parseMachineId(customMachineId);
                }
            }
            if (machineId == null) {
                machineId = defaultMachineId();
            }
            machineId[0] &= 0x7F;
            return machineId;
        } catch (final Exception e) {
            LOGGER.error("Could not get MAC address", e);
            byte[] machineId = getRandom(MACHINE_ID_LEN);
            machineId[0] &= 0x7F;
            return machineId;
        }
    }

    private static final int macAddress(byte[] mac) {
        int maci = 0;
        if (mac == null) {
            return maci;
        }
        int i = mac.length - 4;
        if (i < 0) {
            i = 0;
        }
        maci |= (mac[i++] & 0x7F) << (3 * 8);
        for (int j = 1; i < mac.length; i++, j++) {
            maci |= (mac[i] & 0xFF) << ((3 - j) * 8);
        }
        return maci;
    }

    private static final byte[] parseMachineId(final String valueSource) {
        // Strip separators.
        final String value = valueSource.replaceAll("[:-]", "");

        final byte[] machineId = new byte[4];
        int i = value.length() / 2;
        for (int j = 0; i < value.length() && j < 4; i += 2, j++) {
            machineId[j] = (byte) Integer.parseInt(value.substring(i, i + 2), 16);
        }
        return machineId;
    }

    // Inspired from Netty MacAddressUtil
    /**
     * Obtains the best MAC address found on local network interfaces. Generally
     * speaking, an active network interface used on public networks is better
     * than a local network interface.
     *
     * @return byte array containing a MAC. null if no MAC can be found.
     */
    private static final byte[] defaultMachineId() {
        final byte[] NOT_FOUND = { -1 };
        final byte[] LOCALHOST4_BYTES = { 127, 0, 0, 1 };
        // Find the best MAC address available.
        byte[] bestMacAddr = NOT_FOUND;
        InetAddress bestInetAddr = null;
        try {
            bestInetAddr = InetAddress.getByAddress(LOCALHOST4_BYTES);
        } catch (Exception e) {
            // Should not happen
            LOGGER.warn("Address: " + LOCALHOST4_BYTES);
            throw new IllegalArgumentException(e);
        }
        // Retrieve the list of available network interfaces.
        final Map<NetworkInterface, InetAddress> ifaces =
                new LinkedHashMap<NetworkInterface, InetAddress>();
        try {
            for (final Enumeration<NetworkInterface> i =
                    NetworkInterface.getNetworkInterfaces(); i.hasMoreElements();) {
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
        } catch (SocketException e) {
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
            } catch (SocketException e) {
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
                if (res < 0) {
                    // Found a MAC address with better INET address.
                    replace = true;
                } else if (res == 0) {
                    // Cannot tell the difference. Choose the longer one.
                    if (bestMacAddr.length < macAddr.length) {
                        replace = true;
                    }
                }
            }
            if (replace) {
                bestMacAddr = macAddr;
                bestInetAddr = inetAddr;
            }
        }
        if (bestMacAddr == NOT_FOUND) {
            bestMacAddr = getRandom(MACHINE_ID_LEN);
        }
        return bestMacAddr;
    }

    /**
     * @return positive - current is better, 0 - cannot tell from MAC addr,
     *         negative - candidate is better.
     */
    private static final int compareAddresses(final byte[] current,
            final byte[] candidate) {
        if (candidate == null) {
            return 1;
        }
        // Must be EUI-48 or longer.
        if (candidate.length < 6) {
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
        if ((candidate[0] & 1) != 0) {
            return 1;
        }
        // Prefer globally unique address.
        if ((current[0] & 2) == 0) {
            if ((candidate[0] & 2) == 0) {
                // Both current and candidate are globally unique addresses.
                return 0;
            } else {
                // Only current is globally unique.
                return 1;
            }
        } else {
            if ((candidate[0] & 2) == 0) {
                // Only candidate is globally unique.
                return -1;
            } else {
                // Both current and candidate are non-unique.
                return 0;
            }
        }
    }

    /**
     * @return positive - current is better, 0 - cannot tell, negative -
     *         candidate is better
     */
    private static final int compareAddresses(final InetAddress current,
            final InetAddress candidate) {
        return scoreAddress(current) - scoreAddress(candidate);
    }

    private static final int scoreAddress(final InetAddress addr) {
        if (addr.isAnyLocalAddress() || addr.isLoopbackAddress()) {
            return 0;
        }
        if (addr.isMulticastAddress()) {
            return 1;
        }
        if (addr.isLinkLocalAddress()) {
            return 2;
        }
        if (addr.isSiteLocalAddress()) {
            return 3;
        }
        return 4;
    }

    // inspired from Netty DefaultChannelId and
    // http://stackoverflow.com/questions/35842/how-can-a-java-program-get-its-own-process-id
    private static final ClassLoader getSystemClassLoader() {
        if (System.getSecurityManager() == null) {
            return ClassLoader.getSystemClassLoader();
        } else {
            return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                @Override
                public ClassLoader run() {
                    return ClassLoader.getSystemClassLoader();
                }
            });
        }
    }

    /**
     * @return the JVM Process ID
     */
    static final int jvmProcessId() {
        // Note: may fail in some JVM implementations
        // something like '<pid>@<hostname>', at least in SUN / Oracle JVMs
        try {
            int processId = -1;
            String customProcessId = SystemPropertyUtil.get("fr.gouv.vitam.processId");
            if (customProcessId != null) {
                try {
                    processId = Integer.parseInt(customProcessId);
                } catch (NumberFormatException e) {
                    // Malformed input.
                }
                if (processId < 0 || processId > MAX_PID) {
                    processId = -1;
                }
            }
            if (processId < 0) {
                final ClassLoader loader = getSystemClassLoader();
                String value;
                final Object[] EMPTY_OBJECTS = new Object[0];
                final Class<?>[] EMPTY_CLASSES = new Class[0];
                try {
                    // Invoke
                    // java.lang.management.ManagementFactory.getRuntimeMXBean().getName()
                    Class<?> mgmtFactoryType = Class.forName(
                            "java.lang.management.ManagementFactory", true, loader);
                    Class<?> runtimeMxBeanType = Class
                            .forName("java.lang.management.RuntimeMXBean", true, loader);

                    Method getRuntimeMXBean =
                            mgmtFactoryType.getMethod("getRuntimeMXBean", EMPTY_CLASSES);
                    Object bean = getRuntimeMXBean.invoke(null, EMPTY_OBJECTS);
                    Method getName =
                            runtimeMxBeanType.getDeclaredMethod("getName", EMPTY_CLASSES);
                    value = (String) getName.invoke(bean, EMPTY_OBJECTS);
                } catch (Exception e) {
                    try {
                        // Invoke android.os.Process.myPid()
                        Class<?> processType =
                                Class.forName("android.os.Process", true, loader);
                        Method myPid = processType.getMethod("myPid", EMPTY_CLASSES);
                        value = myPid.invoke(null, EMPTY_OBJECTS).toString();
                    } catch (Exception e2) {
                        value = "";
                    }
                }
                int atIndex = value.indexOf('@');
                if (atIndex >= 0) {
                    value = value.substring(0, atIndex);
                }
                try {
                    processId = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    // value did not contain an integer.
                    processId = -1;
                }

                if (processId < 0 || processId > MAX_PID) {
                    processId = RANDOM.nextInt(MAX_PID + 1);
                }
            }
            return processId;
        } catch (final Exception e) {
            LOGGER.error("Error while getting JVMPID", e);
            return RANDOM.nextInt(MAX_PID + 1);
        }
    }

}

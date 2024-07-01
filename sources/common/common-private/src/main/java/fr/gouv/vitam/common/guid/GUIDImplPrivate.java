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
package fr.gouv.vitam.common.guid;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.exception.InvalidGuidOperationException;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.SecureRandom;

/**
 * GUID Generator (also Global UID Generator) <br>
 * <br>
 * Inspired from com.groupon locality-uuid which used combination of internal counter value - process id - fragment of
 * Platform Id and Timestamp. see https://github.com/groupon/locality-uuid.java <br>
 * <br>
 * But force sequence and take care of errors and improves some performance issues. <br>
 * Moreover it adds a baseId, to separate virtually multiple generators which should have no intersection (such as done
 * in ARK GUID), using a BaseId set for one "instance". <br>
 * This version uses a 22 bytes length version, more precise but incompatible with standard UUID. <br>
 * <br>
 * <br>
 * To override the processId, one can use the following property:<br>
 *
 * <pre>
 *  -Dfr.gouv.vitam.processId=nnnnn
 * </pre>
 *
 * Where nnnnn is a number between 0 and 2^22 (4194304).
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
final class GUIDImplPrivate extends GUIDImpl {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(GUIDImplPrivate.class);

    private static final String FR_GOUV_VITAM_PROCESS_ID = "fr.gouv.vitam.processId";
    private static final Object FORSYNC = new Object();
    /**
     * So MAX value on 3 bytes (64 system use 2^22 id)
     */
    private static final int MAX_PID = 4194304;
    /**
     * 2^22-1 bytes value maximum
     */
    private static final int JVMPID;
    /**
     * Random Generator
     */
    private static final SecureRandom RANDOM = new SecureRandom();

    static {
        JVMPID = jvmProcessId();
    }

    /**
     * Counter part
     */
    private static volatile int counter = 0;
    /**
     * Counter reset
     */
    private static volatile long lastTimeStamp = 0;

    /**
     * Constructor that generates a new GUID using the current process id, Platform Id and timestamp with no object type
     * and no tenant
     */
    GUIDImplPrivate() {
        this(0, 0, ServerIdentity.getInstance().getGlobalPlatformId(), false);
    }

    /**
     * Constructor that generates a new GUID using the current process id, Platform Id and timestamp with no tenant
     *
     * @param objectTypeId object type id between 0 and 255
     * @throws IllegalArgumentException if any of the argument are out of range
     */
    GUIDImplPrivate(final int objectTypeId) {
        this(objectTypeId, 0, ServerIdentity.getInstance().getGlobalPlatformId(), false);
    }

    /**
     * Constructor that generates a new GUID using the current process id, Platform Id and timestamp
     *
     * @param objectTypeId object type id between 0 and 255
     * @param tenantId tenant id between 0 and 2^30-1
     * @throws IllegalArgumentException if any of the argument are out of range
     */
    GUIDImplPrivate(final int objectTypeId, final int tenantId) {
        this(objectTypeId, tenantId, ServerIdentity.getInstance().getGlobalPlatformId(), false);
    }

    /**
     * Constructor that generates a new GUID using the current process id, Platform Id and timestamp with no object type
     * and no tenant
     *
     * @param worm True if Worm GUID
     * @throws IllegalArgumentException if any of the argument are out of range
     */
    GUIDImplPrivate(final boolean worm) {
        this(0, 0, ServerIdentity.getInstance().getGlobalPlatformId(), worm);
    }

    /**
     * Constructor that generates a new GUID using the current process id, Platform Id and timestamp with no tenant
     *
     * @param objectTypeId object type id between 0 and 255
     * @param worm True if Worm GUID
     * @throws IllegalArgumentException if any of the argument are out of range
     */
    GUIDImplPrivate(final int objectTypeId, final boolean worm) {
        this(objectTypeId, 0, ServerIdentity.getInstance().getGlobalPlatformId(), worm);
    }

    /**
     * Constructor that generates a new GUID using the current process id, Platform Id and timestamp
     *
     * @param objectTypeId object type id between 0 and 255
     * @param tenantId tenant id between 0 and 2^30-1
     * @param worm True if Worm GUID
     * @throws IllegalArgumentException if any of the argument are out of range
     */
    GUIDImplPrivate(final int objectTypeId, final int tenantId, final boolean worm) {
        this(objectTypeId, tenantId, ServerIdentity.getInstance().getGlobalPlatformId(), worm);
    }

    /**
     * Constructor that generates a new GUID using the current process id and timestamp
     *
     * @param objectTypeId object type id between 0 and 255
     * @param tenantId tenant id between 0 and 2^30-1
     * @param platformId platform Id between 0 and 2^31-1
     * @throws IllegalArgumentException if any of the argument are out of range
     */
    GUIDImplPrivate(final int objectTypeId, final int tenantId, final int platformId) {
        this(objectTypeId, tenantId, platformId, false);
    }

    /**
     * Constructor that generates a new GUID using the current process id and timestamp
     *
     * @param objectTypeId object type id between 0 and 255
     * @param tenantId tenant id between 0 and 2^30-1
     * @param platformId platform Id between 0 and 2^31-1
     * @param worm True if Worm GUID
     * @throws IllegalArgumentException if any of the argument are out of range
     */
    GUIDImplPrivate(final int objectTypeId, final int tenantId, final int platformId, final boolean worm) {
        super();
        if (objectTypeId < 0 || objectTypeId > 0xFF) {
            throw new IllegalArgumentException("Object Type ID must be between 0 and 255: " + objectTypeId);
        }
        if (tenantId < 0 || tenantId > 0x3FFFFFFF) {
            throw new IllegalArgumentException("DomainId must be between 0 and 2^30-1: " + tenantId);
        }
        if (platformId < 0) {
            throw new IllegalArgumentException("PlatformId must be between 0 and 2^31-1: " + platformId);
        }

        // atomically
        final long time;
        final int count;
        synchronized (FORSYNC) {
            long tmptime = System.currentTimeMillis();
            if (lastTimeStamp != tmptime) {
                counter = 0;
                lastTimeStamp = tmptime;
            }
            count = ++counter;
            if (count > 0xFFFFFF) {
                try {
                    FORSYNC.wait(1);
                } catch (final InterruptedException e) {
                    // ignore
                    SysErrLogger.FAKE_LOGGER.ignoreLog(e);
                }
                tmptime = System.currentTimeMillis();
                counter = 0;
                lastTimeStamp = tmptime;
            }
            time = tmptime;
        }
        // 2 bytes = Version (8) + Object Id (8)
        guid[HEADER_POS] = (byte) VERSION;
        guid[HEADER_POS + 1] = (byte) (objectTypeId & 0xFF);

        // 4 bytes - 2 bits = Domain (30)
        int value = tenantId;
        guid[TENANT_POS + 3] = (byte) (value & 0xFF);
        value >>>= BYTE_SIZE;
        guid[TENANT_POS + 2] = (byte) (value & 0xFF);
        value >>>= BYTE_SIZE;
        guid[TENANT_POS + 1] = (byte) (value & 0xFF);
        value >>>= BYTE_SIZE;
        guid[TENANT_POS] = (byte) (value & 0x3F);

        // 4 bytes = Worm status + Platform (31)
        value = platformId;
        guid[PLATFORM_POS + 3] = (byte) (value & 0xFF);
        value >>>= BYTE_SIZE;
        guid[PLATFORM_POS + 2] = (byte) (value & 0xFF);
        value >>>= BYTE_SIZE;
        guid[PLATFORM_POS + 1] = (byte) (value & 0xFF);
        value >>>= BYTE_SIZE;
        if (worm) {
            guid[PLATFORM_POS] = (byte) (0x80 | (value & 0x7F));
        } else {
            guid[PLATFORM_POS] = (byte) (value & 0x7F);
        }

        // 3 bytes = -2 bits JVMPID (22)
        value = JVMPID;
        guid[PID_POS + 2] = (byte) (value & 0xFF);
        value >>>= BYTE_SIZE;
        guid[PID_POS + 1] = (byte) (value & 0xFF);
        value >>>= BYTE_SIZE;
        guid[PID_POS] = (byte) (value & 0xFF);

        // 6 bytes = timestamp (so up to 8 925 years after Time 0 so year 10
        // 895)
        long lvalue = time;
        guid[TIME_POS + 5] = (byte) (lvalue & 0xFF);
        lvalue >>>= BYTE_SIZE;
        guid[TIME_POS + 4] = (byte) (lvalue & 0xFF);
        lvalue >>>= BYTE_SIZE;
        guid[TIME_POS + 3] = (byte) (lvalue & 0xFF);
        lvalue >>>= BYTE_SIZE;
        guid[TIME_POS + 2] = (byte) (lvalue & 0xFF);
        lvalue >>>= BYTE_SIZE;
        guid[TIME_POS + 1] = (byte) (lvalue & 0xFF);
        lvalue >>>= BYTE_SIZE;
        guid[TIME_POS] = (byte) (lvalue & 0xFF);

        // 3 bytes = counter against collision
        value = count;
        guid[COUNTER_POS + 2] = (byte) (value & 0xFF);
        value >>>= BYTE_SIZE;
        guid[COUNTER_POS + 1] = (byte) (value & 0xFF);
        value >>>= BYTE_SIZE;
        guid[COUNTER_POS] = (byte) (value & 0xFF);
    }

    /**
     * Constructor that takes a byte array as this GUID's content
     *
     * @param bytes GUID content
     * @throws InvalidGuidOperationException
     */
    GUIDImplPrivate(final byte[] bytes) throws InvalidGuidOperationException {
        super(bytes);
    }

    /**
     * Build from String key
     *
     * @param idsource
     * @throws InvalidGuidOperationException
     */
    GUIDImplPrivate(final String idsource) throws InvalidGuidOperationException {
        super(idsource);
    }

    // inspired from Netty DefaultChannelId and
    // http://stackoverflow.com/questions/35842/how-can-a-java-program-get-its-own-process-id
    private static ClassLoader getSystemClassLoader() {
        if (System.getSecurityManager() == null) {
            return ClassLoader.getSystemClassLoader();
        } else {
            return AccessController.doPrivileged((PrivilegedAction<ClassLoader>) ClassLoader::getSystemClassLoader);
        }
    }

    /**
     * @return the JVM Process ID
     */
    static int jvmProcessId() {
        // Note: may fail in some JVM implementations
        // something like '<pid>@<hostname>', at least in SUN / Oracle JVMs
        try {
            int processId = -1;
            final String customProcessId = SystemPropertyUtil.getNoCheck(FR_GOUV_VITAM_PROCESS_ID);
            if (customProcessId != null) {
                processId = parseProcessId(processId, customProcessId);
            }
            if (processId < 0) {
                final ClassLoader loader = getSystemClassLoader();
                String value;
                final Object[] emptyObjects = new Object[0];
                final Class<?>[] emptyClasses = new Class[0];
                value = jvmProcessIdManagementFactory(loader, emptyObjects, emptyClasses);
                final int atIndex = value.indexOf('@');
                if (atIndex >= 0) {
                    value = value.substring(0, atIndex);
                }
                processId = parseProcessId(processId, value);
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

    /**
     * @param oldProcessId
     * @param customProcessId
     * @return the processId
     */
    private static int parseProcessId(int oldProcessId, final String customProcessId) {
        int processId = oldProcessId;
        try {
            processId = Integer.parseInt(customProcessId);
        } catch (final NumberFormatException e) {
            // Malformed input.
        }
        if (processId < 0 || processId > MAX_PID) {
            processId = -1;
        }
        return processId;
    }

    /**
     * @param loader
     * @param emptyObjects
     * @param emptyClasses
     * @return the processId as String
     */
    private static String jvmProcessIdManagementFactory(
        final ClassLoader loader,
        final Object[] emptyObjects,
        final Class<?>[] emptyClasses
    ) {
        String value;
        try {
            // Invoke
            // java.lang.management.ManagementFactory.getRuntimeMXBean().getName()
            final Class<?> mgmtFactoryType = Class.forName("java.lang.management.ManagementFactory", true, loader);
            final Class<?> runtimeMxBeanType = Class.forName("java.lang.management.RuntimeMXBean", true, loader);

            final Method getRuntimeMXBean = mgmtFactoryType.getMethod("getRuntimeMXBean", emptyClasses);
            final Object bean = getRuntimeMXBean.invoke(null, emptyObjects);
            final Method getName = runtimeMxBeanType.getDeclaredMethod("getName", emptyClasses);
            value = (String) getName.invoke(bean, emptyObjects);
        } catch (final Exception e) {
            LOGGER.debug("Unable to get PID, try another way", e);
            try {
                // Invoke android.os.Process.myPid()
                final Class<?> processType = Class.forName("android.os.Process", true, loader);
                final Method myPid = processType.getMethod("myPid", emptyClasses);
                value = myPid.invoke(null, emptyObjects).toString();
            } catch (final Exception e2) {
                LOGGER.debug("Unable to get PID", e2);
                value = "";
            }
        }
        return value;
    }
}

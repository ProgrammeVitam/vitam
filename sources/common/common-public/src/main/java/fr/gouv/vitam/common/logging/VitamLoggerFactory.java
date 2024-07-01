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
package fr.gouv.vitam.common.logging;

/**
 * Creates an {@link VitamLogger} or changes the default factory implementation. This factory allows you to choose what
 * logging framework VITAM should use. The default factory is {@link LogbackLoggerFactory}. If SLF4J is not available,
 * {@link JdkLoggerFactory} is used. You can change it to your preferred logging framework before other VITAM classes are loaded:
 *
 * Please note that the new default factory is effective only for the classes which were loaded after the default
 * factory is changed. Therefore, {@link #setDefaultFactory(VitamLoggerFactory)} should be called as early as possible
 * and shouldn't be called more than once.
 */
public abstract class VitamLoggerFactory {

    private static volatile VitamLoggerFactory defaultFactory;
    protected static VitamLogLevel currentLevel = null;
    private static boolean initialized = false;
    static Object serverIdentity = null;

    static {
        final String name = VitamLoggerFactory.class.getName();
        VitamLoggerFactory f;
        try {
            f = new LogbackLoggerFactory(true);
            f.newInstance(name).debug("Using Logback (SLF4J) as the default logging framework");
            defaultFactory = f;
        } catch (final Exception t1) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(t1);
            f = new JdkLoggerFactory(null);
            f.newInstance(name).debug("Using java.util.logging as the default logging framework", t1);
        }

        defaultFactory = f;
        initIdentity();
    }

    private static void initIdentity() {
        if (initialized) {
            return;
        }
        try {
            final Class<?> clasz = Class.forName(
                "fr.gouv.vitam.common.ServerIdentity",
                true,
                VitamLoggerFactory.class.getClassLoader()
            );
            serverIdentity = clasz.getMethod("getInstance").invoke(null);
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            // ignore
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        } catch (Throwable e) {
            System.err.println("Issue while initializing Identiy :" + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Issue while initializing Identiy", e);
        }
        initialized = true;
    }

    /**
     * @param level the vitam log level
     */
    public VitamLoggerFactory(final VitamLogLevel level) {
        setInternalLogLevel(level);
        if (currentLevel == null) {
            setInternalLogLevel(getLevelSpecific());
        }
    }

    /**
     * Returns the default factory. The initial default factory is {@link JdkLoggerFactory}.
     *
     * @return the current default Factory
     */
    public static VitamLoggerFactory getDefaultFactory() {
        return defaultFactory;
    }

    /**
     * Changes the default factory.
     *
     * @param defaultFactory instance of VitamLoggerFactory
     */
    public static void setDefaultFactory(final VitamLoggerFactory defaultFactory) {
        if (defaultFactory == null) {
            throw new IllegalArgumentException("defaultFactory must not be null");
        }
        VitamLoggerFactory.defaultFactory = defaultFactory;
    }

    /**
     * Creates a new logger instance with the name of the specified class.
     *
     * @param clazz specified class
     * @return the logger instance
     */
    public static VitamLogger getInstance(final Class<?> clazz) {
        return getInstance(clazz.getName());
    }

    /**
     * Creates a new logger instance with the specified name.
     *
     * @param name to create new logger instance
     * @return the logger instance
     */
    public static VitamLogger getInstance(final String name) {
        return getDefaultFactory().newInstance(name);
    }

    /**
     * @return the current Level used
     */
    public static VitamLogLevel getLogLevel() {
        return getDefaultFactory().getLevelSpecific();
    }

    /**
     * @param level the vitam log level
     */
    public static void setLogLevel(final VitamLogLevel level) {
        setInternalLogLevel(level);
        if (currentLevel != null) {
            getDefaultFactory().seLevelSpecific(currentLevel);
        }
    }

    protected static synchronized void setInternalLogLevel(final VitamLogLevel level) {
        if (level != null) {
            currentLevel = level;
        }
    }

    /**
     * @return should return the current Level for the specific implementation
     */
    protected abstract VitamLogLevel getLevelSpecific();

    /**
     * Set the level for the specific implementation
     *
     * @param level
     */
    protected abstract void seLevelSpecific(VitamLogLevel level);

    /**
     * Creates a new logger instance with the specified name.
     */
    protected abstract VitamLogger newInstance(String name);
}

/**
 * This file is part of Waarp Project.
 *
 * Copyright 2009, Frederic Bregier, and individual contributors by the author
 * tags. See the COPYRIGHT.txt in the distribution for a full listing of
 * individual contributors.
 *
 * All Waarp Project is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * Waarp is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Waarp . If not, see <http://www.gnu.org/licenses/>.
 */
package fr.gouv.vitam.common;

import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.Properties;

/**
 * A collection of utility methods to retrieve and parse the values of the Java
 * system properties.
 */
public final class SystemPropertyUtil {
    /**
     * Default File encoding field
     */
    public static final String FILE_ENCODING = "file.encoding";

    private static final Properties PROPS = new Properties();

    // Retrieve all system properties at once so that there's no need to deal
    // with
    // security exceptions from next time. Otherwise, we might end up with
    // logging every
    // security exceptions on every system property access or introducing more
    // complexity
    // just because of less verbose logging.
    static {
        refresh();
    }

    /**
     * Re-retrieves all system properties so that any post-launch properties
     * updates are retrieved.
     */
    public static void refresh() {
        Properties newProps = null;
        try {
            newProps = System.getProperties();
        } catch (final SecurityException e) {
            System.err.println(
                    "Unable to retrieve the system properties; default values will be used: "
                            + e.getMessage());
            newProps = new Properties();
        }

        synchronized (PROPS) {
            PROPS.clear();
            PROPS.putAll(newProps);
        }
        if (!contains(FILE_ENCODING)
                || !get(FILE_ENCODING).equalsIgnoreCase(FileUtil.UTF_8)) {
            try {
                // Try to set UTF-8 as default file encoding: use
                // -Dfile.encoding=UTF-8 as java command argument to ensure
                // correctness
                System.setProperty(FILE_ENCODING, FileUtil.UTF_8);
                final Field charset = Charset.class.getDeclaredField("defaultCharset");
                charset.setAccessible(true);
                charset.set(null, null);
                synchronized (PROPS) {
                    PROPS.clear();
                    PROPS.putAll(newProps);
                }
            } catch (final Exception e1) {
                // ignore since it is a security issue and -Dfile.encoding=UTF-8
                // should be used
                System.err
                        .println(
                                "Issue while trying to set UTF-8 as default file encoding: use -Dfile.encoding=UTF-8 as java command argument: "
                                        + e1.getMessage());
                System.err.println("Currently file.encoding is: " + get(FILE_ENCODING));
            }
        }
    }

    /**
     *
     * @return True if Encoding is Correct
     */
    public static boolean isFileEncodingCorrect() {
        return (contains(FILE_ENCODING)
                && get(FILE_ENCODING).equalsIgnoreCase(FileUtil.UTF_8));
    }

    /**
     * Returns {@code true} if and only if the system property with the
     * specified {@code key} exists.
     *
     * @param key
     * @return True if the key is contained
     */
    public static final boolean contains(final String key) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        return PROPS.containsKey(key);
    }

    /**
     * Returns the value of the Java system property with the specified
     * {@code key}, while falling back to {@code null} if the property access
     * fails.
     *
     * @param key
     * @return the property value or {@code null}
     */
    public static final String get(final String key) {
        return get(key, null);
    }

    /**
     * Returns the value of the Java system property with the specified
     * {@code key}, while falling back to the specified default value if the
     * property access fails.
     *
     * @param key
     * @param def
     * @return the property value. {@code def} if there's no such property or if
     *         an access to the specified property is not allowed.
     */
    public static final String get(final String key, final String def) {
        if (key == null) {
            throw new NullPointerException("key");
        }

        final String value = PROPS.getProperty(key);
        if (value == null) {
            return def;
        }

        return value;
    }

    /**
     * Returns the value of the Java system property with the specified
     * {@code key}, while falling back to the specified default value if the
     * property access fails.
     *
     * @param key
     * @param def
     * @return the property value. {@code def} if there's no such property or if
     *         an access to the specified property is not allowed.
     */
    public static boolean getBoolean(final String key, final boolean def) {
        if (key == null) {
            throw new NullPointerException("key");
        }

        String value = PROPS.getProperty(key);
        if (value == null) {
            return def;
        }

        value = value.trim().toLowerCase();
        if (value.isEmpty()) {
            return true;
        }

        if (value.equals("true") || value.equals("yes") || value.equals("1")) {
            return true;
        }

        if (value.equals("false") || value.equals("no") || value.equals("0")) {
            return false;
        }

        System.err.println("Unable to parse the boolean system property '" + key + "':"
                + value + " - "
                + "using the default value: " + def);

        return def;
    }

    /**
     * Returns the value of the Java system property with the specified
     * {@code key}, while falling back to the specified default value if the
     * property access fails.
     *
     * @param key
     * @param def
     * @return the property value. {@code def} if there's no such property or if
     *         an access to the specified property is not allowed.
     */
    public static int getInt(final String key, final int def) {
        if (key == null) {
            throw new NullPointerException("key");
        }

        String value = PROPS.getProperty(key);
        if (value == null) {
            return def;
        }

        value = value.trim().toLowerCase();
        if (value.matches("-?[0-9]+")) {
            try {
                return Integer.parseInt(value);
            } catch (final Exception e) {
                // Ignore
            }
        }

        System.err.println("Unable to parse the integer system property '" + key + "':"
                + value + " - "
                + "using the default value: " + def);

        return def;
    }

    /**
     * Returns the value of the Java system property with the specified
     * {@code key}, while falling back to the specified default value if the
     * property access fails.
     *
     * @param key
     * @param def
     * @return the property value. {@code def} if there's no such property or if
     *         an access to the specified property is not allowed.
     */
    public static long getLong(final String key, final long def) {
        if (key == null) {
            throw new NullPointerException("key");
        }

        String value = PROPS.getProperty(key);
        if (value == null) {
            return def;
        }

        value = value.trim().toLowerCase();
        if (value.matches("-?[0-9]+")) {
            try {
                return Long.parseLong(value);
            } catch (final Exception e) {
                // Ignore
            }
        }

        System.err.println("Unable to parse the long integer system property '" + key
                + "':" + value + " - "
                + "using the default value: " + def);

        return def;
    }

    /**
     * Returns the value of the Java system property with the specified
     * {@code key}, while falling back to the specified default value if the
     * property access fails.
     * 
     * @param key
     * @param def
     * @return the property value. {@code def} if there's no such property or if
     *         an access to the specified property is not allowed.
     */
    public static String getAndSet(String key, String def) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        if (!PROPS.containsKey(key)) {
            System.setProperty(key, def);
            refresh();
            return def;
        }
        return PROPS.getProperty(key);
    }

    /**
     * Returns the value of the Java system property with the specified
     * {@code key}, while falling back to the specified default value if the
     * property access fails.
     * 
     * @param key
     * @param def
     * @return the property value. {@code def} if there's no such property or if
     *         an access to the specified property is not allowed.
     */
    public static boolean getAndSetBoolean(String key, boolean def) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        if (!PROPS.containsKey(key)) {
            System.setProperty(key, Boolean.toString(def));
            refresh();
            return def;
        }
        return getBoolean(key, def);
    }

    /**
     * Returns the value of the Java system property with the specified
     * {@code key}, while falling back to the specified default value if the
     * property access fails.
     * 
     * @param key
     * @param def
     * @return the property value. {@code def} if there's no such property or if
     *         an access to the specified property is not allowed.
     */
    public static int getAndSetInt(String key, int def) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        if (!PROPS.containsKey(key)) {
            System.setProperty(key, Integer.toString(def));
            refresh();
            return def;
        }
        return getInt(key, def);
    }

    /**
     * Returns the value of the Java system property with the specified
     * {@code key}, while falling back to the specified default value if the
     * property access fails.
     * 
     * @param key
     * @param def
     * @return the property value. {@code def} if there's no such property or if
     *         an access to the specified property is not allowed.
     */
    public static long getAndSetLong(String key, long def) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        if (!PROPS.containsKey(key)) {
            System.setProperty(key, Long.toString(def));
            refresh();
            return def;
        }
        return getLong(key, def);
    }

    /**
     * Set the value of the Java system property with the specified {@code key}
     * to the specified default value.
     * 
     * @param key
     * @param def
     * @return the ancient value.
     */
    public static String set(String key, String def) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        String old = null;
        if (PROPS.containsKey(key)) {
            old = PROPS.getProperty(key);
        }
        System.setProperty(key, def);
        refresh();
        return old;
    }

    /**
     * Set the value of the Java system property with the specified {@code key}
     * to the specified default value.
     * 
     * @param key
     * @param def
     * @return the ancient value.
     */
    public static boolean setBoolean(String key, boolean def) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        boolean old = false;
        if (PROPS.containsKey(key)) {
            old = getBoolean(key, def);
        }
        System.setProperty(key, Boolean.toString(def));
        refresh();
        return old;
    }

    /**
     * Set the value of the Java system property with the specified {@code key}
     * to the specified default value.
     * 
     * @param key
     * @param def
     * @return the ancient value.
     */
    public static int setInt(String key, int def) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        int old = 0;
        if (PROPS.containsKey(key)) {
            old = getInt(key, def);
        }
        System.setProperty(key, Integer.toString(def));
        refresh();
        return old;
    }

    /**
     * Set the value of the Java system property with the specified {@code key}
     * to the specified default value.
     * 
     * @param key
     * @param def
     * @return the ancient value.
     */
    public static long setLong(String key, long def) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        long old = 0;
        if (PROPS.containsKey(key)) {
            old = getLong(key, def);
        }
        System.setProperty(key, Long.toString(def));
        refresh();
        return old;
    }

    /**
     * Print to System.out the content of the properties
     */
    public static void debug() {
        PROPS.list(System.out);
    }

    /**
     * Inspired from
     * http://commons.apache.org/lang/api-2.4/org/apache/commons/lang/
     * SystemUtils.html
     */
    @SuppressWarnings("javadoc")
    public static enum Platform {
        Windows,
        Mac,
        Unix,
        Solaris,
        unsupported
    }

    private static Platform m_os = null;

    /**
     * @return the Platform
     */
    public static Platform getOS() {
        if (m_os == null) {
            String os = System.getProperty("os.name").toLowerCase();
            m_os = Platform.unsupported;
            if (os.indexOf("win") >= 0)
                m_os = Platform.Windows; // Windows
            if (os.indexOf("mac") >= 0)
                m_os = Platform.Mac; // Mac
            if (os.indexOf("nux") >= 0)
                m_os = Platform.Unix; // Linux
            if (os.indexOf("nix") >= 0)
                m_os = Platform.Unix; // Unix
            if (os.indexOf("sunos") >= 0)
                m_os = Platform.Solaris; // Solaris
        }
        return m_os;
    }

    /**
     * @return True if Windows
     */
    public static boolean isWindows() {
        return (getOS() == Platform.Windows);
    }

    /**
     * @return True if Mac
     */
    public static boolean isMac() {
        return (getOS() == Platform.Mac);
    }

    /**
     * @return True if Unix
     */
    public static boolean isUnix() {
        return (getOS() == Platform.Unix);
    }

    /**
     * @return True if Solaris
     */
    public static boolean isSolaris() {
        return (getOS() == Platform.Solaris);
    }

    private SystemPropertyUtil() {
        // Unused
    }
}

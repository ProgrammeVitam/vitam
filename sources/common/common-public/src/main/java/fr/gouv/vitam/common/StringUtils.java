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

import fr.gouv.vitam.common.exception.InvalidParseOperationException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * String utils
 */
public final class StringUtils {

    /**
     * Random Generator
     */
    private static final SecureRandom RANDOM = new SecureRandom();

    // default parameters for XML check
    private static final String CDATA_TAG_UNESCAPED = "<![CDATA[";
    private static final String CDATA_TAG_ESCAPED = "&lt;![CDATA[";
    private static final String ENTITY_TAG_UNESCAPED = "<!ENTITY";
    private static final String ENTITY_TAG_ESCAPED = "&lt;!ENTITY";
    // default parameters for Javascript check
    private static final String SCRIPT_TAG_UNESCAPED = "<script>";
    private static final String SCRIPT_TAG_ESCAPED = "&lt;script&gt;";
    // default parameters for Json check
    private static final String TAG_START =
        "\\<\\w+((\\s+\\w+(\\s*\\=\\s*(?:\".*?\"|'.*?'|[^'\"\\>\\s]+))?)+\\s*|\\s*)\\>";
    private static final String TAG_END = "\\</\\w+\\>";
    private static final String TAG_SELF_CLOSING =
        "\\<\\w+((\\s+\\w+(\\s*\\=\\s*(?:\".*?\"|'.*?'|[^'\"\\>\\s]+))?)+\\s*|\\s*)/\\>";
    private static final String HTML_ENTITY = "&[a-zA-Z][a-zA-Z0-9]+;";
    public static final Pattern HTML_PATTERN = Pattern.compile(
        "(" + TAG_START + ".*" + TAG_END + ")|(" + TAG_SELF_CLOSING + ")|(" + HTML_ENTITY + ")",
        Pattern.DOTALL
    );
    // Default ASCII for Param check
    public static final Pattern UNPRINTABLE_PATTERN = Pattern.compile("[\\p{Cntrl}&&[^\r\n\t]]");
    public static final List<String> RULES = new ArrayList<>();

    static {
        StringUtils.RULES.add(CDATA_TAG_UNESCAPED);
        StringUtils.RULES.add(CDATA_TAG_ESCAPED);
        StringUtils.RULES.add(ENTITY_TAG_UNESCAPED);
        StringUtils.RULES.add(ENTITY_TAG_ESCAPED);
        StringUtils.RULES.add(SCRIPT_TAG_UNESCAPED);
        StringUtils.RULES.add(SCRIPT_TAG_ESCAPED);
    }

    private StringUtils() {
        // empty
    }

    /**
     * Check external argument to avoid Path Traversal attack
     *
     * @param value to check
     * @throws InvalidParseOperationException
     */
    public static String checkSanityString(String value) throws InvalidParseOperationException {
        checkSanityString(new String[] { value });
        return value;
    }

    /**
     * Check external argument
     *
     * @param strings
     * @throws InvalidParseOperationException
     */
    public static void checkSanityString(String... strings) throws InvalidParseOperationException {
        for (String field : strings) {
            if (StringUtils.UNPRINTABLE_PATTERN.matcher(field).find()) {
                throw new InvalidParseOperationException("Invalid input bytes");
            }
            for (final String rule : StringUtils.RULES) {
                if (field != null && rule != null && field.contains(rule)) {
                    throw new InvalidParseOperationException("Invalid tag sanity check");
                }
            }
        }
    }

    /**
     * @param length the length of rray
     * @return a byte array with random values
     */
    public static final byte[] getRandom(final int length) {
        if (length <= 0) {
            return new byte[0];
        }
        final byte[] result = new byte[length];
        for (int i = 0; i < result.length; i++) {
            result[i] = (byte) (RANDOM.nextInt(95) + 32);
        }
        return result;
    }

    /**
     * Revert Arrays.toString for bytes
     *
     * @param bytesString the string to transform
     * @return the array of bytes
     * @throws IllegalArgumentException if bytesString is null or empty
     */
    public static final byte[] getBytesFromArraysToString(final String bytesString) {
        ParametersChecker.checkParameter("Should not be null or empty", bytesString);
        final String[] strings = bytesString.replace("[", "").replace("]", "").split(", ");
        final byte[] result = new byte[strings.length];
        try {
            for (int i = 0; i < result.length; i++) {
                result[i] = (byte) (Integer.parseInt(strings[i]) & 0xFF);
            }
        } catch (final NumberFormatException e) {
            throw new IllegalArgumentException(e);
        }
        return result;
    }

    /**
     * @param object to get its class name
     * @return the short name of the Class of this object
     */
    public static final String getClassName(Object object) {
        final Class<?> clasz = object.getClass();
        String name = clasz.getSimpleName();
        if (name != null && !name.isEmpty()) {
            return name;
        } else {
            name = clasz.getName();
            final int pos = name.lastIndexOf('.');
            if (pos < 0) {
                return name;
            }
            return name.substring(pos + 1);
        }
    }

    /**
     * Gets the String that is nested in between two Strings. Only the first match is returned.
     *
     * @param source
     * @param start
     * @param end
     * @return the substring, null if no match
     */
    public static final String substringBetween(String source, String start, String end) {
        return org.apache.commons.lang3.StringUtils.substringBetween(source, start, end);
    }

    /**
     * Gets the substring before the last occurrence of a separator. The separator is not returned.
     *
     * @param source
     * @param separator
     * @return the substring before the last occurrence of the separator, null if null String input
     */
    public static final String substringBeforeLast(String source, String separator) {
        return org.apache.commons.lang3.StringUtils.substringBeforeLast(source, separator);
    }

    /**
     * Get text content of an input stream
     *
     * @param is input stream
     * @return text content
     * @throws IOException when input stream unreadable
     */
    public static String getStringFromInputStream(InputStream is) throws IOException {
        final String content;
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(is))) {
            content = buffer.lines().collect(Collectors.joining());
        }
        return content;
    }
}

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

import fr.gouv.vitam.common.ParametersChecker;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * Formats messages according to very simple substitution rules. Substitutions can be made 1, 2 or more arguments.
 * <p/>
 * <p/>
 * For example,
 * <p/>
 *
 * <pre>
 * MessageFormatter.format(&quot;Hi {}.&quot;, &quot;there&quot;)
 * </pre>
 * <p/>
 * will return the string "Hi there.".
 * <p/>
 * The {} pair is called the <em>formatting anchor</em>. It serves to designate the location where arguments need to be
 * substituted within the message pattern.
 * <p/>
 * In case your message contains the '{' or the '}' character, you do not have to do anything special unless the '}'
 * character immediately follows '{'. For example,
 * <p/>
 *
 * <pre>
 * MessageFormatter.format(&quot;Set {1,2,3} is not equal to {}.&quot;, &quot;1,2&quot;);
 * </pre>
 * <p/>
 * will return the string "Set {1,2,3} is not equal to 1,2.".
 * <p/>
 * <p/>
 * If for whatever reason you need to place the string "{}" in the message without its <em>formatting anchor</em>
 * meaning, then you need to escape the '{' character with '\', that is the backslash character. Only the '{' character
 * should be escaped. There is no need to escape the '}' character. For example,
 * <p/>
 *
 * <pre>
 * MessageFormatter.format(&quot;Set \\{} is not equal to {}.&quot;, &quot;1,2&quot;);
 * </pre>
 * <p/>
 * will return the string "Set {} is not equal to 1,2.".
 * <p/>
 * <p/>
 * The escaping behavior just described can be overridden by escaping the escape character '\'. Calling
 * <p/>
 *
 * <pre>
 * MessageFormatter.format(&quot;File name is C:\\\\{}.&quot;, &quot;file.zip&quot;);
 * </pre>
 * <p/>
 * will return the string "File name is C:\file.zip".
 * <p/>
 * <p/>
 * The formatting conventions are different than those of {@link MessageFormat} which ships with the Java platform. This
 * is justified by the fact that SLF4J's implementation is 10 times faster than that of {@link MessageFormat}. This
 * local performance difference is both measurable and significant in the larger context of the complete logging
 * processing chain.
 * <p/>
 * <p/>
 * See also {@link #format(String, Object)}, {@link #format(String, Object, Object)} and
 * {@link #arrayFormat(String, Object[])} methods for more details. <br>
 * Inspired from Netty
 */
final class MessageFormatter {

    static final char DELIM_START = '{';
    static final char DELIM_STOP = '}';
    static final String DELIM_STR = "{}";
    private static final char ESCAPE_CHAR = '\\';

    private MessageFormatter() {
        // Empty
    }

    /**
     * Performs single argument substitution for the 'messagePattern' passed as parameter.
     * <p/>
     * For example,
     * <p/>
     *
     * <pre>
     * MessageFormatter.format(&quot;Hi {}.&quot;, &quot;there&quot;);
     * </pre>
     * <p/>
     * will return the string "Hi there.".
     * <p/>
     *
     * @param messagePattern The message pattern which will be parsed and formatted
     * @param arg The argument to be substituted in place of the formatting anchor
     * @return The formatted message
     */
    static final FormattingTuple format(final String messagePattern, final Object arg) {
        return arrayFormat(messagePattern, new Object[] { arg });
    }

    /**
     * Performs a two argument substitution for the 'messagePattern' passed as parameter.
     * <p/>
     * For example,
     * <p/>
     *
     * <pre>
     * MessageFormatter.format(&quot;Hi {}. My name is {}.&quot;, &quot;Alice&quot;, &quot;Bob&quot;);
     * </pre>
     * <p/>
     * will return the string "Hi Alice. My name is Bob.".
     *
     * @param messagePattern The message pattern which will be parsed and formatted
     * @param argA The argument to be substituted in place of the first formatting anchor
     * @param argB The argument to be substituted in place of the second formatting anchor
     * @return The formatted message
     */
    static final FormattingTuple format(final String messagePattern, final Object argA, final Object argB) {
        return arrayFormat(messagePattern, new Object[] { argA, argB });
    }

    static final Throwable getThrowableCandidate(final Object[] argArray) {
        if (argArray == null || argArray.length == 0) {
            return null;
        }

        final Object lastEntry = argArray[argArray.length - 1];
        if (lastEntry instanceof Throwable) {
            return (Throwable) lastEntry;
        }
        return null;
    }

    /**
     * Same principle as the {@link #format(String, Object)} and {@link #format(String, Object, Object)} methods except
     * that any number of arguments can be passed in an array.
     *
     * @param messagePattern The message pattern which will be parsed and formatted
     * @param argArray An array of arguments to be substituted in place of formatting anchors
     * @return The formatted message
     */
    static final FormattingTuple arrayFormat(final String messagePattern, final Object[] argArray) {
        final Throwable throwableCandidate = getThrowableCandidate(argArray);

        if (messagePattern == null) {
            return new FormattingTuple(null, argArray, throwableCandidate);
        }

        if (argArray == null) {
            return new FormattingTuple(messagePattern);
        }

        int i = 0;
        int j;
        final StringBuilder sbuild = new StringBuilder(messagePattern.length() + 50);

        int l;
        for (l = 0; l < argArray.length; l++) {
            j = messagePattern.indexOf(DELIM_STR, i);

            if (j == -1) {
                // no more variables
                if (i == 0) {
                    // this is a simple string
                    return new FormattingTuple(messagePattern, argArray, throwableCandidate);
                } else {
                    // add the tail string which contains no variables and return the result.
                    sbuild.append(messagePattern.substring(i, messagePattern.length()));
                    return new FormattingTuple(sbuild.toString(), argArray, throwableCandidate);
                }
            } else {
                if (isEscapedDelimeter(messagePattern, j)) {
                    if (!isDoubleEscaped(messagePattern, j)) {
                        l--;
                        // DELIM_START was escaped, thus should not be incremented
                        sbuild.append(messagePattern.substring(i, j - 1)).append(DELIM_START);
                        i = j + 1;
                    } else {
                        // The escape character preceding the delimiter start is
                        // itself escaped: "abc x:\\{}"
                        // we have to consume one backward slash
                        sbuild.append(messagePattern.substring(i, j - 1));
                        deeplyAppendParameter(sbuild, argArray[l], new HashMap<Object[], Void>());
                        i = j + 2;
                    }
                } else {
                    // normal case
                    sbuild.append(messagePattern.substring(i, j));
                    deeplyAppendParameter(sbuild, argArray[l], new HashMap<Object[], Void>());
                    i = j + 2;
                }
            }
        }
        // append the characters following the last {} pair.
        sbuild.append(messagePattern.substring(i, messagePattern.length()));
        if (l < argArray.length - 1) {
            return new FormattingTuple(sbuild.toString(), argArray, throwableCandidate);
        } else {
            return new FormattingTuple(sbuild.toString(), argArray, null);
        }
    }

    static final boolean isEscapedDelimeter(final String messagePattern, final int delimeterStartIndex) {
        ParametersChecker.checkParameterNullOnly("Must not be null", messagePattern);
        if (delimeterStartIndex == 0) {
            return false;
        }
        return messagePattern.charAt(delimeterStartIndex - 1) == ESCAPE_CHAR;
    }

    static final boolean isDoubleEscaped(final String messagePattern, final int delimeterStartIndex) {
        ParametersChecker.checkParameterNullOnly("Must not be null", messagePattern);
        return (
            delimeterStartIndex >= 2 &&
            delimeterStartIndex - 2 > messagePattern.length() &&
            messagePattern.charAt(delimeterStartIndex - 2) == ESCAPE_CHAR
        );
    }

    // special treatment of array values was suggested by 'lizongbo'
    static final void deeplyAppendParameter(
        final StringBuilder sbuild,
        final Object o,
        final Map<Object[], Void> seenMap
    ) {
        if (o == null) {
            sbuild.append("null");
            return;
        }
        if (!o.getClass().isArray()) {
            safeObjectAppend(sbuild, o);
        } else {
            // check for primitive array types because they
            // unfortunately cannot be cast to Object[]
            if (o instanceof boolean[]) {
                booleanArrayAppend(sbuild, (boolean[]) o);
            } else if (o instanceof byte[]) {
                byteArrayAppend(sbuild, (byte[]) o);
            } else if (o instanceof char[]) {
                charArrayAppend(sbuild, (char[]) o);
            } else if (o instanceof short[]) {
                shortArrayAppend(sbuild, (short[]) o);
            } else if (o instanceof int[]) {
                intArrayAppend(sbuild, (int[]) o);
            } else if (o instanceof long[]) {
                longArrayAppend(sbuild, (long[]) o);
            } else if (o instanceof float[]) {
                floatArrayAppend(sbuild, (float[]) o);
            } else if (o instanceof double[]) {
                doubleArrayAppend(sbuild, (double[]) o);
            } else {
                objectArrayAppend(sbuild, (Object[]) o, seenMap);
            }
        }
    }

    private static final void safeObjectAppend(final StringBuilder sbuild, final Object o) {
        try {
            final String oAsString = o.toString();
            sbuild.append(oAsString);
        } catch (final Exception t) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(t);
            SysErrLogger.FAKE_LOGGER.syserr(
                "SLF4J: Failed toString() invocation on an object of type [" +
                o.getClass().getName() +
                ']' +
                t.getMessage()
            );
            sbuild.append("[FAILED toString()]");
        }
    }

    private static final void objectArrayAppend(
        final StringBuilder sbuild,
        final Object[] a,
        final Map<Object[], Void> seenMap
    ) {
        sbuild.append('[');
        if (!seenMap.containsKey(a)) {
            seenMap.put(a, null);
            final int len = a.length;
            for (int i = 0; i < len; i++) {
                deeplyAppendParameter(sbuild, a[i], seenMap);
                if (i != len - 1) {
                    sbuild.append(", ");
                }
            }
            // allow repeats in siblings
            seenMap.remove(a);
        } else {
            sbuild.append("...");
        }
        sbuild.append(']');
    }

    private static final void booleanArrayAppend(final StringBuilder sbuild, final boolean[] a) {
        sbuild.append('[');
        final int len = a.length;
        for (int i = 0; i < len; i++) {
            sbuild.append(a[i]);
            if (i != len - 1) {
                sbuild.append(", ");
            }
        }
        sbuild.append(']');
    }

    private static final void byteArrayAppend(final StringBuilder sbuild, final byte[] a) {
        sbuild.append('[');
        final int len = a.length;
        for (int i = 0; i < len; i++) {
            sbuild.append(a[i]);
            if (i != len - 1) {
                sbuild.append(", ");
            }
        }
        sbuild.append(']');
    }

    private static final void charArrayAppend(final StringBuilder sbuild, final char[] a) {
        sbuild.append('[');
        final int len = a.length;
        for (int i = 0; i < len; i++) {
            sbuild.append(a[i]);
            if (i != len - 1) {
                sbuild.append(", ");
            }
        }
        sbuild.append(']');
    }

    private static final void shortArrayAppend(final StringBuilder sbuild, final short[] a) {
        sbuild.append('[');
        final int len = a.length;
        for (int i = 0; i < len; i++) {
            sbuild.append(a[i]);
            if (i != len - 1) {
                sbuild.append(", ");
            }
        }
        sbuild.append(']');
    }

    private static final void intArrayAppend(final StringBuilder sbuild, final int[] a) {
        sbuild.append('[');
        final int len = a.length;
        for (int i = 0; i < len; i++) {
            sbuild.append(a[i]);
            if (i != len - 1) {
                sbuild.append(", ");
            }
        }
        sbuild.append(']');
    }

    private static final void longArrayAppend(final StringBuilder sbuild, final long[] a) {
        sbuild.append('[');
        final int len = a.length;
        for (int i = 0; i < len; i++) {
            sbuild.append(a[i]);
            if (i != len - 1) {
                sbuild.append(", ");
            }
        }
        sbuild.append(']');
    }

    private static final void floatArrayAppend(final StringBuilder sbuild, final float[] a) {
        sbuild.append('[');
        final int len = a.length;
        for (int i = 0; i < len; i++) {
            sbuild.append(a[i]);
            if (i != len - 1) {
                sbuild.append(", ");
            }
        }
        sbuild.append(']');
    }

    private static final void doubleArrayAppend(final StringBuilder sbuild, final double[] a) {
        sbuild.append('[');
        final int len = a.length;
        for (int i = 0; i < len; i++) {
            sbuild.append(a[i]);
            if (i != len - 1) {
                sbuild.append(", ");
            }
        }
        sbuild.append(']');
    }
}

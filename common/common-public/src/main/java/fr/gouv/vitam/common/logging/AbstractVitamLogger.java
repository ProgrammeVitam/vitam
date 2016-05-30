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
package fr.gouv.vitam.common.logging;

import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * A skeletal implementation of {@link VitamLogger}. This class implements all methods that have a {@link VitamLogLevel}
 * parameter by default to call specific logger methods such as {@link #info(String)} or {@link #isInfoEnabled()}. <br>
 * Inspired from Netty
 */
public abstract class AbstractVitamLogger implements VitamLogger, Serializable {

    private static final long serialVersionUID = -6382972526573193470L;

    private static final String EXCEPTION_MESSAGE = "Unexpected exception:";

    private final String name;

    private static int baseLevel;
    private static int logLevel;

    { // NOSONAR
      // Must be dynamic to ensure correct value
        baseLevel = detectLoggingBaseLevel();
        logLevel = baseLevel + 2;
    }

    /**
     * Creates a new instance.
     */
    protected AbstractVitamLogger(final String name) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        this.name = name;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public boolean isEnabled(final VitamLogLevel level) {
        switch (level) {
            case TRACE:
                return isTraceEnabled();
            case DEBUG:
                return isDebugEnabled();
            case INFO:
                return isInfoEnabled();
            case WARN:
                return isWarnEnabled();
            case ERROR:
                return isErrorEnabled();
            default:
                return true;
        }
    }

    @Override
    public void trace(final Throwable t) {
        trace(EXCEPTION_MESSAGE, t);
    }

    @Override
    public void debug(final Throwable t) {
        debug(EXCEPTION_MESSAGE, t);
    }

    @Override
    public void info(final Throwable t) {
        info(EXCEPTION_MESSAGE, t);
    }

    @Override
    public void warn(final Throwable t) {
        warn(EXCEPTION_MESSAGE, t);
    }

    @Override
    public void error(final Throwable t) {
        error(EXCEPTION_MESSAGE, t);
    }

    @Override
    public void log(final VitamLogLevel level, final String msg, final Throwable cause) {
        switch (level) {
            case TRACE:
                trace(msg, cause);
                break;
            case DEBUG:
                debug(msg, cause);
                break;
            case INFO:
                info(msg, cause);
                break;
            case WARN:
                warn(msg, cause);
                break;
            case ERROR:
            default:
                error(msg, cause);
                break;
        }
    }

    @Override
    public void log(final VitamLogLevel level, final Throwable cause) {
        switch (level) {
            case TRACE:
                trace(cause);
                break;
            case DEBUG:
                debug(cause);
                break;
            case INFO:
                info(cause);
                break;
            case WARN:
                warn(cause);
                break;
            case ERROR:
            default:
                error(cause);
                break;
        }
    }

    @Override
    public void log(final VitamLogLevel level, final String msg) {
        switch (level) {
            case TRACE:
                trace(msg);
                break;
            case DEBUG:
                debug(msg);
                break;
            case INFO:
                info(msg);
                break;
            case WARN:
                warn(msg);
                break;
            case ERROR:
            default:
                error(msg);
                break;
        }
    }

    @Override
    public void log(final VitamLogLevel level, final String format, final Object arg) {
        switch (level) {
            case TRACE:
                trace(format, arg);
                break;
            case DEBUG:
                debug(format, arg);
                break;
            case INFO:
                info(format, arg);
                break;
            case WARN:
                warn(format, arg);
                break;
            case ERROR:
            default:
                error(format, arg);
                break;
        }
    }

    @Override
    public void log(final VitamLogLevel level, final String format, final Object argA,
        final Object argB) {
        switch (level) {
            case TRACE:
                trace(format, argA, argB);
                break;
            case DEBUG:
                debug(format, argA, argB);
                break;
            case INFO:
                info(format, argA, argB);
                break;
            case WARN:
                warn(format, argA, argB);
                break;
            case ERROR:
            default:
                error(format, argA, argB);
                break;
        }
    }

    @Override
    public void log(final VitamLogLevel level, final String format,
        final Object... arguments) {
        switch (level) {
            case TRACE:
                trace(format, arguments);
                break;
            case DEBUG:
                debug(format, arguments);
                break;
            case INFO:
                info(format, arguments);
                break;
            case WARN:
                warn(format, arguments);
                break;
            case ERROR:
            default:
                error(format, arguments);
                break;
        }
    }

    protected Object readResolve() throws ObjectStreamException {
        return VitamLoggerFactory.getInstance(name());
    }

    /**
     * @param o
     * @return the simple Class Name
     */
    public static final String simpleClassName(final Object o) {
        if (o == null) {
            return "null_object";
        } else {
            return simpleClassName(o.getClass());
        }
    }

    /**
     * @param clazz
     * @return the simple Class Name
     */
    public static final String simpleClassName(final Class<?> clazz) {
        if (clazz == null) {
            return "null_class";
        }
        final Package pkg = clazz.getPackage();
        if (pkg != null) {
            return clazz.getName().substring(pkg.getName().length() + 1);
        } else {
            return clazz.getName();
        }
    }

    @Override
    public String toString() {
        return simpleClassName(this) + '(' + name() + ')';
    }

    /**
     * Determine the good level
     *
     * @return the default base level
     */
    private static final int detectLoggingBaseLevel() {
        final StackTraceElement[] elt = Thread.currentThread().getStackTrace();
        int i;
        for (i = 0; i < elt.length; i++) {
            if ("detectLoggingBaseLevel".equalsIgnoreCase(elt[i].getMethodName())) {
                break;
            }
        }
        return i;
    }

    /**
     * To be used in message for logger (rank 2) like logger.warn(code,"message:"+getImmediateMethodAndLine(),null);
     *
     * @return "ClassAndMethodName(FileName:LineNumber)"
     */
    public static final String getImmediateMethodAndLine() {
        final StackTraceElement elt =
            Thread.currentThread().getStackTrace()[baseLevel + 1];
        return getMethodAndLine(elt);
    }

    /**
     * To be used only by Logger (rank 5)
     *
     * @return "MethodName(FileName:LineNumber)"
     */
    public static final String getLoggerMethodAndLine() {
        final StackTraceElement elt = Thread.currentThread().getStackTrace()[logLevel];
        return getMethodAndLine(elt);
    }

    /**
     * @param rank is the current depth of call+1 (immediate = 1+1=2)
     * @return "ClassAndMethodName(FileName:LineNumber)"
     */
    protected static final String getRankMethodAndLine(final int rank) {
        final StackTraceElement elt = Thread.currentThread().getStackTrace()[rank];
        return getMethodAndLine(elt);
    }

    /**
     *
     * @param elt
     * @return "MethodName(FileName:LineNumber) " from elt
     */
    private static final String getMethodAndLine(final StackTraceElement elt) {
        return new StringBuilder(elt.getClassName())
            .append('.').append(elt.getMethodName())
            .append('(').append(elt.getFileName())
            .append(':').append(elt.getLineNumber()).append(") : ").toString();
    }
}

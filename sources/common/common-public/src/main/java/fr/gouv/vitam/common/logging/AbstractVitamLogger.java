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

import fr.gouv.vitam.common.ServerIdentityInterface;

import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * This class implements all methods that have a {@link VitamLogLevel} parameter by default to call specific logger
 * methods such as {@link #info(String)} or {@link #isInfoEnabled()}. <br>
 * Inspired from Netty </br>
 * A skeletal implementation of {@link VitamLogger}
 */
public abstract class AbstractVitamLogger implements VitamLogger, Serializable {

    private static final char PACKAGE_SEPARATOR_CHAR = '.';

    private static final long serialVersionUID = -6382972526573193470L;

    private static final String EXCEPTION_MESSAGE = "Unexpected exception:";

    static final String TIME_TRACE_PREFIX = "[TIMEINFO] ";

    private final String name;

    private boolean hasServerIdentity = VitamLoggerFactory.serverIdentity != null;

    /**
     * Creates a new instance.
     */
    protected AbstractVitamLogger(final String name) {
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
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
        trace(getMessagePrepend() + EXCEPTION_MESSAGE, t);
    }

    @Override
    public void debug(final Throwable t) {
        debug(getMessagePrepend() + EXCEPTION_MESSAGE, t);
    }

    @Override
    public void info(final Throwable t) {
        info(getMessagePrepend() + EXCEPTION_MESSAGE, t);
    }

    @Override
    public void warn(final Throwable t) {
        warn(getMessagePrepend() + EXCEPTION_MESSAGE, t);
    }

    @Override
    public void error(final Throwable t) {
        error(getMessagePrepend() + EXCEPTION_MESSAGE, t);
    }

    @Override
    public void log(final VitamLogLevel level, final String msg, final Throwable cause) {
        final String newmsg = getMessagePrepend() + msg;
        switch (level) {
            case TRACE:
                trace(newmsg, cause);
                break;
            case DEBUG:
                debug(newmsg, cause);
                break;
            case INFO:
                info(newmsg, cause);
                break;
            case WARN:
                warn(newmsg, cause);
                break;
            case ERROR:
            default:
                error(newmsg, cause);
                break;
        }
    }

    @Override
    public void log(final VitamLogLevel level, final Throwable cause) {
        switch (level) {
            case TRACE:
                trace(getMessagePrepend() + cause.getMessage(), cause);
                break;
            case DEBUG:
                debug(getMessagePrepend() + cause.getMessage(), cause);
                break;
            case INFO:
                info(getMessagePrepend() + cause.getMessage(), cause);
                break;
            case WARN:
                warn(getMessagePrepend() + cause.getMessage(), cause);
                break;
            case ERROR:
            default:
                error(getMessagePrepend() + cause.getMessage(), cause);
                break;
        }
    }

    @Override
    public void log(final VitamLogLevel level, final String msg) {
        final String newmsg = getMessagePrepend() + msg;
        switch (level) {
            case TRACE:
                trace(newmsg);
                break;
            case DEBUG:
                debug(newmsg);
                break;
            case INFO:
                info(newmsg);
                break;
            case WARN:
                warn(newmsg);
                break;
            case ERROR:
            default:
                error(newmsg);
                break;
        }
    }

    @Override
    public void log(final VitamLogLevel level, final String format, final Object arg) {
        final String newmsg = getMessagePrepend() + format;
        switch (level) {
            case TRACE:
                trace(newmsg, arg);
                break;
            case DEBUG:
                debug(newmsg, arg);
                break;
            case INFO:
                info(newmsg, arg);
                break;
            case WARN:
                warn(newmsg, arg);
                break;
            case ERROR:
            default:
                error(newmsg, arg);
                break;
        }
    }

    @Override
    public void log(final VitamLogLevel level, final String format, final Object argA, final Object argB) {
        final String newmsg = getMessagePrepend() + format;
        switch (level) {
            case TRACE:
                trace(newmsg, argA, argB);
                break;
            case DEBUG:
                debug(newmsg, argA, argB);
                break;
            case INFO:
                info(newmsg, argA, argB);
                break;
            case WARN:
                warn(newmsg, argA, argB);
                break;
            case ERROR:
            default:
                error(newmsg, argA, argB);
                break;
        }
    }

    @Override
    public void log(final VitamLogLevel level, final String format, final Object... arguments) {
        final String newmsg = getMessagePrepend() + format;
        switch (level) {
            case TRACE:
                trace(newmsg, arguments);
                break;
            case DEBUG:
                debug(newmsg, arguments);
                break;
            case INFO:
                info(newmsg, arguments);
                break;
            case WARN:
                warn(newmsg, arguments);
                break;
            case ERROR:
            default:
                error(newmsg, arguments);
                break;
        }
    }

    protected Object readResolve() throws ObjectStreamException {
        return VitamLoggerFactory.getInstance(name());
    }

    /**
     * @param o the object to get its class name
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
     * @param clazz instance of a class
     * @return the simple Class Name
     */
    public static final String simpleClassName(final Class<?> clazz) {
        if (clazz == null) {
            return "null_class";
        }
        final String className = clazz.getName();
        final int lastDotIdx = className.lastIndexOf(PACKAGE_SEPARATOR_CHAR);
        if (lastDotIdx > -1) {
            return className.substring(lastDotIdx + 1);
        }
        return className;
    }

    @Override
    public String toString() {
        return simpleClassName(this) + '(' + name() + ')';
    }

    /**
     * @return Message prepend using ServerIdentity
     */
    final String getMessagePrepend() {
        if (hasServerIdentity) {
            return ((ServerIdentityInterface) VitamLoggerFactory.serverIdentity).getLoggerMessagePrepend();
        }
        return "";
    }
}

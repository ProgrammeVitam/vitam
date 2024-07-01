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
package fr.gouv.vitam.common.security;

import org.owasp.esapi.Logger;

/**
 * Dummy Implementation of Logger for ESAPI
 */
public class VitamLoggerLog implements Logger {

    private int level = 0;

    /**
     * Empty constructor
     *
     * @param name
     */
    public VitamLoggerLog(String name) {
        // Nothing to do
    }

    @Override
    public void setLevel(int level) {
        this.level = level;
    }

    @Override
    public int getESAPILevel() {
        return level;
    }

    @Override
    public void fatal(EventType type, String message) {
        // Nothing to do
    }

    @Override
    public void fatal(EventType type, String message, Throwable throwable) {
        // Nothing to do
    }

    @Override
    public boolean isFatalEnabled() {
        // Nothing to do
        return true;
    }

    @Override
    public void error(EventType type, String message) {
        // Nothing to do
    }

    @Override
    public void error(EventType type, String message, Throwable throwable) {
        // Nothing to do
    }

    @Override
    public boolean isErrorEnabled() {
        return true;
    }

    @Override
    public void warning(EventType type, String message) {
        // Nothing to do
    }

    @Override
    public void warning(EventType type, String message, Throwable throwable) {
        // Nothing to do
    }

    @Override
    public boolean isWarningEnabled() {
        return true;
    }

    @Override
    public void info(EventType type, String message) {
        // Nothing to do
    }

    @Override
    public void info(EventType type, String message, Throwable throwable) {
        // Nothing to do
    }

    @Override
    public boolean isInfoEnabled() {
        return false;
    }

    @Override
    public void debug(EventType type, String message) {
        // Nothing to do
    }

    @Override
    public void debug(EventType type, String message, Throwable throwable) {
        // Nothing to do
    }

    @Override
    public boolean isDebugEnabled() {
        return false;
    }

    @Override
    public void trace(EventType type, String message) {
        // Nothing to do
    }

    @Override
    public void trace(EventType type, String message, Throwable throwable) {
        // Nothing to do
    }

    @Override
    public boolean isTraceEnabled() {
        return false;
    }

    @Override
    public void always(EventType type, String message) {
        // Nothing to do
    }

    @Override
    public void always(EventType type, String message, Throwable throwable) {
        // Nothing to do
    }
}

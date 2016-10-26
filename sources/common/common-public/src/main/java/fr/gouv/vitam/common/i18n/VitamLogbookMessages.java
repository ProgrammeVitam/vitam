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
package fr.gouv.vitam.common.i18n;

import java.util.Locale;

import fr.gouv.vitam.common.model.StatusCode;

/**
 * Vitam Messages Herlper for Logbooks
 */
public class VitamLogbookMessages {
    private static final String DEFAULT_PROPERTY_FILENAME = "vitam-logbook-messages";
    private static final VitamLogbookMessages VITAM_MESSAGES = new VitamLogbookMessages();
    private static final String SEPARATOR = ".";
    private static final String LABEL = SEPARATOR + "LABEL";
    private static final String LIFECYCLE = SEPARATOR + "LFC";

    final Messages messages;

    /**
     * Constructor
     */
    private VitamLogbookMessages() {
        this(DEFAULT_PROPERTY_FILENAME);
    }

    /**
     * 
     * @param propertyFilename
     */
    private VitamLogbookMessages(String propertyFilename) {
        this(propertyFilename, Messages.DEFAULT_LOCALE);
    }

    /**
     * 
     * @param propertyFilename
     * @param locale
     */
    private VitamLogbookMessages(String propertyFilename, Locale locale) {
        messages = new Messages(propertyFilename, locale);
    }

    /**
     * Operation Logbook context
     * 
     * @param stepOrHandler step or handler name or full name (on Front Office Application)
     * @return the Label of this step or handler or full named
     */
    public static final String getLabelOp(String stepOrHandler) {
        return VITAM_MESSAGES.messages.getStringNotEmpty(stepOrHandler + LABEL);
    }

    /**
     * Lifecycle Logbook context
     * 
     * @param stepOrHandler step or handler name or full name (on Front Office Application)
     * @return the Label of this step or handler or full named
     */
    public static final String getLabelLfc(String stepOrHandler) {
        return VITAM_MESSAGES.messages.getStringNotEmpty(stepOrHandler + LIFECYCLE + LABEL);
    }

    /**
     * Operation Logbook context
     * 
     * @param stepOrHandler step or handler name or full name (on Front Office Application)
     * @param transaction transaction name (within this handler)
     * @return the Label of this step or handler or full named with sub transaction
     */
    public static final String getLabelOp(String stepOrHandler, String transaction) {
        return VITAM_MESSAGES.messages.getStringNotEmpty(stepOrHandler + SEPARATOR + transaction + LABEL);
    }

    /**
     * Lifecycle Logbook context
     * 
     * @param stepOrHandler step or handler name or full name (on Front Office Application)
     * @param transaction transaction name (within this handler)
     * @return the Label of this step or handler or full named with sub transaction
     */
    public static final String getLabelLfc(String stepOrHandler, String transaction) {
        return VITAM_MESSAGES.messages.getStringNotEmpty(stepOrHandler + LIFECYCLE + SEPARATOR + transaction + LABEL);
    }

    /**
     * Operation Logbook context
     * 
     * @param stepOrHandler step or handler name or full name (on Front Office Application)
     * @param code the code from which the message is needed
     * @return the code label of this step or handler or full named
     */
    public static final String getCodeOp(String stepOrHandler, StatusCode code) {
        return VITAM_MESSAGES.messages.getStringNotEmpty(stepOrHandler + SEPARATOR + code);
    }

    /**
     * Lifecycle Logbook context
     * 
     * @param stepOrHandler step or handler name or full name (on Front Office Application)
     * @param code the code from which the message is needed
     * @return the code label of this step or handler or full named
     */
    public static final String getCodeLfc(String stepOrHandler, StatusCode code) {
        return VITAM_MESSAGES.messages.getStringNotEmpty(stepOrHandler + LIFECYCLE + SEPARATOR + code);
    }

    /**
     * Operation Logbook context
     * 
     * @param stepOrHandler step or handler name or full name (on Front Office Application)
     * @param transaction transaction name (within this handler)
     * @param code the code from which the message is needed
     * @return the code label of this step or handler or full named with sub transaction
     */
    public static final String getCodeOp(String stepOrHandler, String transaction, StatusCode code) {
        return VITAM_MESSAGES.messages.getStringNotEmpty(stepOrHandler + SEPARATOR + transaction + SEPARATOR + code);
    }

    /**
     * Lifecycle Logbook context
     * 
     * @param stepOrHandler step or handler name or full name (on Front Office Application)
     * @param transaction transaction name (within this handler)
     * @param code the code from which the message is needed
     * @return the code label of this step or handler or full named with sub transaction
     */
    public static final String getCodeLfc(String stepOrHandler, String transaction, StatusCode code) {
        return VITAM_MESSAGES.messages
            .getStringNotEmpty(stepOrHandler + LIFECYCLE + SEPARATOR + transaction + SEPARATOR + code);
    }

    /**
     * Operation Logbook context
     * 
     * @param stepOrHandler step or handler name or full name (on Front Office Application)
     * @param code the code from which the message is needed
     * @param args list of extra argument to apply as MessageFormat.format(message, args)
     * @return the code label of this step or handler or full named
     */
    public static final String getCodeOp(String stepOrHandler, StatusCode code, Object... args) {
        return VITAM_MESSAGES.messages.getStringNotEmpty(stepOrHandler + SEPARATOR + code, args);
    }

    /**
     * Lifecycle Logbook context
     * 
     * @param stepOrHandler step or handler name or full name (on Front Office Application)
     * @param code the code from which the message is needed
     * @param args list of extra argument to apply as MessageFormat.format(message, args)
     * @return the code label of this step or handler or full named
     */
    public static final String getCodeLfc(String stepOrHandler, StatusCode code, Object... args) {
        return VITAM_MESSAGES.messages.getStringNotEmpty(stepOrHandler + LIFECYCLE + SEPARATOR + code, args);
    }

    /**
     * Operation Logbook context
     * 
     * @param stepOrHandler step or handler name or full name (on Front Office Application)
     * @param transaction transaction name (within this handler)
     * @param code the code from which the message is needed
     * @param args list of extra argument to apply as MessageFormat.format(message, args)
     * @return the code label of this step or handler or full named with sub transaction
     */
    public static final String getCodeOp(String stepOrHandler, String transaction, StatusCode code, Object... args) {
        return VITAM_MESSAGES.messages.getStringNotEmpty(stepOrHandler + SEPARATOR + transaction + SEPARATOR + code,
            args);
    }

    /**
     * Lifecycle Logbook context
     * 
     * @param stepOrHandler step or handler name or full name (on Front Office Application)
     * @param transaction transaction name (within this handler)
     * @param code the code from which the message is needed
     * @param args list of extra argument to apply as MessageFormat.format(message, args)
     * @return the code label of this step or handler or full named with sub transaction
     */
    public static final String getCodeLfc(String stepOrHandler, String transaction, StatusCode code, Object... args) {
        return VITAM_MESSAGES.messages.getStringNotEmpty(
            stepOrHandler + LIFECYCLE + SEPARATOR + transaction + SEPARATOR + code,
            args);
    }
}

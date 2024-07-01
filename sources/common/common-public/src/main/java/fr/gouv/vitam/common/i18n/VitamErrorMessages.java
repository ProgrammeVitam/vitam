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
package fr.gouv.vitam.common.i18n;

import java.util.Locale;
import java.util.Map;

/**
 * Vitam Error Messages Helper for take value of the key in
 * vitam-error-messages.properties
 */
public class VitamErrorMessages {

    private static final String DEFAULT_PROPERTY_FILENAME = "vitam-error-messages";
    private static final VitamErrorMessages VITAM_ERROR_MESSAGES = new VitamErrorMessages();

    final Messages messages;

    /**
     * Default Locale
     */
    public static final Locale DEFAULT_LOCALE = Locale.FRENCH;

    /**
     * Constructor
     */
    private VitamErrorMessages() {
        this(DEFAULT_PROPERTY_FILENAME);
    }

    /**
     * @param propertyFilename
     */
    private VitamErrorMessages(String propertyFilename) {
        this(propertyFilename, Messages.DEFAULT_LOCALE);
    }

    /**
     * @param propertyFilename
     * @param locale
     */
    private VitamErrorMessages(String propertyFilename, Locale locale) {
        messages = new Messages(propertyFilename, locale);
    }

    /**
     * Retrieve all the messages
     *
     * @return map of messages
     */
    public static Map<String, String> getAllMessages() {
        return VITAM_ERROR_MESSAGES.messages.getAllMessages();
    }

    /**
     * Retrieve value of the message key
     *
     * @param key key of the message
     * @return
     */
    public static final String getFromKey(String key) {
        return VITAM_ERROR_MESSAGES.messages.getStringNotEmpty(key);
    }

    /**
     * Retrieve value of the message key
     *
     * @param key of the message
     * @param args the arguments to use as MessageFormat.format(mesg, args)
     * @return value of the message key
     */
    public static final String getFromKey(String key, Object... args) {
        return VITAM_ERROR_MESSAGES.messages.getString(key, args);
    }
}

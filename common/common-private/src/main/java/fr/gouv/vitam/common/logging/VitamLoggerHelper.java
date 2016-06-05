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

import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.ServerIdentityInterface;

/**
 * Vitam Logger Helper to format LOG within Private Module within Vitam <br>
 * <br>
 * Usage:<br>
 *
 * <pre>
 * VitamLoggerHelper helper = VitamLoggerHelper.newInstance();
 * LOGGER.info(helper.format("My message: {} {}"), arg1, arg2);
 * LOGGER.info(helper.format("My message2: {}"), arg1);
 * LOGGER.info(helper.format("My message3"));
 * </pre>
 * @deprecated This class is no more useful and should not be used
 */
@Deprecated
public class VitamLoggerHelper {
    final StringBuilder preMessage = new StringBuilder();
    final int resetPosition;

    private VitamLoggerHelper() {
        // Set ServerIdentity
        final ServerIdentityInterface serverIdentity = ServerIdentity.getInstance();
        preMessage.append('[').append(serverIdentity.getName()).append(':')
            .append(serverIdentity.getRole()).append("] ");
        resetPosition = preMessage.length();
    }

    /**
     * Helper to format the Message for Internal Vitam LOG
     *
     * @param message
     * @return the new formatted String
     * @deprecated This method is no more useful and should not be used
     */
    public String format(String message) {
        final String result = preMessage.append(message).toString();
        preMessage.setLength(resetPosition);
        return result;
    }

    /**
     * Helper to format the Message for Internal Vitam LOG
     *
     * @param message
     * @return the new formatted String
     * @deprecated This method is no more useful and should not be used
     */
    public String format(StringBuilder message) {
        return message.insert(0, preMessage.toString()).toString();
    }
    // TODO To compare to Logbook and improve message format and arguments

    /**
     *
     * @return a new Instance
     * @deprecated This method is no more useful and should not be used
     */
    public static final VitamLoggerHelper newInstance() {
        return new VitamLoggerHelper();
    }
}

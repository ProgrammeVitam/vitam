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
package fr.gouv.vitam.logbook.common;

import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;

/**
 * Helper to get great process operation logbook key and message (outcome) depend on LogbookTypeProcess
 */
public class MessageLogbookEngineHelper {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(MessageLogbookEngineHelper.class);

    private LogbookTypeProcess logbookTypeProcess;

    /**
     * Default constructor
     *
     * @param logbookTypeProcess concerned logbook process type
     */
    public MessageLogbookEngineHelper(LogbookTypeProcess logbookTypeProcess) {
        this.logbookTypeProcess = logbookTypeProcess;
    }

    /**
     * Get operation logbook message
     *
     * @param stepOrHandler step or handler name or full name
     * @param code of status
     * @param args list of extra argument to apply as MessageFormat.format(message, args)
     * @return the operation logbook message
     */
    public String getLabelOp(String stepOrHandler, StatusCode code, Object... args) {
        return VitamLogbookMessages.getFromFullCodeKey(getOutcomeDetail(stepOrHandler, code), args);
    }

    /**
     * Get operation logbook message
     *
     * @param stepOrHandler step or handler name or full name
     * @param transaction name
     * @param code of status
     * @param args list of extra argument to apply as MessageFormat.format(message, args)
     * @return the operation logbook message
     */
    public String getLabelOp(String stepOrHandler, String transaction, StatusCode code, Object... args) {
        return VitamLogbookMessages.getFromFullCodeKey(getOutcomeDetail(stepOrHandler, transaction, code), args);
    }

    /**
     * Get operation logbook outcome detail (key)
     *
     * @param stepOrHandler step or handler name or full name
     * @param code of status
     * @return the outcome detail (key)
     */
    public String getOutcomeDetail(String stepOrHandler, StatusCode code) {
        String wfKey =
            logbookTypeProcess.name() +
            VitamLogbookMessages.getSeparator() +
            VitamLogbookMessages.getOutcomeDetail(stepOrHandler, code);
        LOGGER.debug("Searching for key {}", wfKey);
        if (VitamLogbookMessages.containsKey(wfKey)) {
            LOGGER.debug("Key found : {}", wfKey);
            return wfKey;
        } else {
            LOGGER.debug("Key found : {}", VitamLogbookMessages.getOutcomeDetail(stepOrHandler, code));
            return VitamLogbookMessages.getOutcomeDetail(stepOrHandler, code);
        }
    }

    /**
     * Get operation logbook outcome detail (key)
     *
     * @param stepOrHandler step or handler name or full name
     * @param transaction name
     * @param code of status
     * @return the outcome detail (key)
     */
    public String getOutcomeDetail(String stepOrHandler, String transaction, StatusCode code) {
        String wfKey =
            logbookTypeProcess.name() +
            VitamLogbookMessages.getSeparator() +
            VitamLogbookMessages.getOutcomeDetail(stepOrHandler, transaction, code);
        LOGGER.debug("Searching for key {}", wfKey);
        if (VitamLogbookMessages.containsKey(wfKey)) {
            LOGGER.debug("Key found : {}", wfKey);
            return wfKey;
        } else {
            LOGGER.debug("Key found : {}", VitamLogbookMessages.getOutcomeDetail(stepOrHandler, transaction, code));
            return VitamLogbookMessages.getOutcomeDetail(stepOrHandler, transaction, code);
        }
    }
}

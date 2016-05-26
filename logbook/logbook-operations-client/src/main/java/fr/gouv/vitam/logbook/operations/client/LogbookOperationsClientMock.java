/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital
 * archiving back-office system managing high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL 2.1
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL 2.1 license and that you accept its terms.
 */
package fr.gouv.vitam.logbook.operations.client;

import java.time.LocalDateTime;

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameters;
import fr.gouv.vitam.logbook.common.parameters.helper.LogbookParametersHelper;

/**
 * Mock client implementation for logbook operation
 */
class LogbookOperationsClientMock implements LogbookClient {

    static final String OK = "OK";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookOperationsClientMock.class);

    /**
     * Create operation logbook entry
     *
     * Actually doesn't check eventIdentity (if not exists)
     *
     * @param parameters the entry parameters
     * @return the status of the operation, OK if no problem, KO otherwise
     */
    @Override
    public String create(LogbookParameters parameters) {
        LogbookParametersHelper
            .checkNullOrEmptyParameters(parameters.getMapParameters(), parameters.getMandatoriesParameters());
        logInformation(parameters);
        return OK;
    }

    /**
     * Update operation logbook entry
     *
     * Actually doesn't check eventIdentity (if exists)
     *
     * @param parameters the entry parameters
     * @return the status of the operation, OK if no problem, KO otherwise
     */
    @Override
    public String update(LogbookParameters parameters) {
        LogbookParametersHelper
            .checkNullOrEmptyParameters(parameters.getMapParameters(), parameters.getMandatoriesParameters());
        logInformation(parameters);
        return OK;
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException();
    }

    private void logInformation(LogbookParameters parameters) {
        String name = ServerIdentity.getInstance().getName();
        LocalDateTime date = LocalDateUtil.now();
        LOGGER.info("Date: " + date);
        parameters.getMapParameters().put(LogbookParameterName.agentIdentifierApplication.name(), name);
        for (String key : parameters.getMapParameters().keySet()) {
            LOGGER.info(key + ": " + parameters.getMapParameters().get(key));
        }
    }
}

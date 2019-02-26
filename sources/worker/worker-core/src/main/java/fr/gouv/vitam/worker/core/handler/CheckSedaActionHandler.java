/*******************************************************************************
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
 *******************************************************************************/
package fr.gouv.vitam.worker.core.handler;

import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.common.utils.SedaUtils;
import fr.gouv.vitam.worker.common.utils.SedaUtils.CheckSedaValidationStatus;
import fr.gouv.vitam.worker.common.utils.SedaUtilsFactory;

/**
 * Check Seda Handler
 */
public class CheckSedaActionHandler extends ActionHandler {

    private static final String NOT_XSD_VALID = "NOT_XSD_VALID";
    private static final String NOT_XML_FILE = "NOT_XML_FILE";
    private static final String NO_FILE = "NO_FILE";
    private static final String HANDLER_ID = "CHECK_SEDA";
    private static final String CONTAINER_FORMAT = "CONTAINER_FORMAT";
    private static final String FILE = "FILE";
    private static final String DIRECTORY = "DIRECTORY";
    private static final String SUBTASK_CHECK_MULTI_MANIFEST = CONTAINER_FORMAT + "." + FILE;
    private static final String SUBTASK_CHECK_MULTI_FOLDER_CONTENT_ID = CONTAINER_FORMAT + "." + DIRECTORY;

    private final SedaUtilsFactory sedaUtilsFactory;

    /**
     * Constructor with parameter SedaUtilsFactory
     */
    public CheckSedaActionHandler() {
        this(SedaUtilsFactory.getInstance());
    }

    public CheckSedaActionHandler(SedaUtilsFactory sedaUtilsFactory) {
        this.sedaUtilsFactory = sedaUtilsFactory;
    }

    /**
     * @return HANDLER_ID
     */
    public static final String getId() {
        return HANDLER_ID;
    }

    @Override
    public ItemStatus execute(WorkerParameters params, HandlerIO handlerIO) {
        checkMandatoryParameters(params);
        final ItemStatus itemStatus = new ItemStatus(HANDLER_ID);

        final SedaUtils sedaUtils = sedaUtilsFactory.createSedaUtils(handlerIO);

        CheckSedaValidationStatus status;
        status = sedaUtils.checkSedaValidation(params, itemStatus);

        switch (status) {
            case VALID:
                itemStatus.increment(StatusCode.OK);
                return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
            case NO_FILE:
                itemStatus.setGlobalOutcomeDetailSubcode(NO_FILE);
                itemStatus.increment(StatusCode.KO);
                return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
            case NOT_XML_FILE:
                itemStatus.setGlobalOutcomeDetailSubcode(NOT_XML_FILE);
                itemStatus.increment(StatusCode.KO);
                return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
            case NOT_XSD_VALID:
                itemStatus.setGlobalOutcomeDetailSubcode(NOT_XSD_VALID);
                itemStatus.increment(StatusCode.KO);
                return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
            case MORE_THAN_ONE_MANIFEST:
                itemStatus.setGlobalOutcomeDetailSubcode(SUBTASK_CHECK_MULTI_MANIFEST);
                itemStatus.increment(StatusCode.KO);
                return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
            case MORE_THAN_ONE_FOLDER_CONTENT:
                itemStatus.setGlobalOutcomeDetailSubcode(SUBTASK_CHECK_MULTI_FOLDER_CONTENT_ID);
                itemStatus.increment(StatusCode.KO);
                return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
            default:
                itemStatus.increment(StatusCode.KO);
                return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
        }

    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // TODO P0 Add Workspace:SIP/manifest.xml and check it
    }

}

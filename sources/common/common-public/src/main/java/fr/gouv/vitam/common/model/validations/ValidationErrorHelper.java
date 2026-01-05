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
package fr.gouv.vitam.common.model.validations;

import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;

public final class ValidationErrorHelper {

    private ValidationErrorHelper() {
        // Empty constructor for static helper
    }

    public static ValidationError createMetadataValidationError(
        LogbookTypeProcess eventTypeProcess,
        String stepOrHandler,
        ObjectNode evDetData,
        String operationId
    ) {
        return getValidationError(
            eventTypeProcess,
            null,
            evDetData,
            VitamLogbookMessages.getOutcomeDetailLfc(stepOrHandler, StatusCode.KO),
            VitamLogbookMessages.getCodeLfc(stepOrHandler, StatusCode.KO),
            operationId
        );
    }

    public static ValidationError createMetadataValidationError(
        LogbookTypeProcess eventTypeProcess,
        String stepOrHandler,
        String transaction,
        ObjectNode evDetData,
        String operationId
    ) {
        return getValidationError(
            eventTypeProcess,
            null,
            evDetData,
            VitamLogbookMessages.getOutcomeDetailLfc(stepOrHandler, transaction, StatusCode.KO),
            VitamLogbookMessages.getCodeLfc(stepOrHandler, transaction, StatusCode.KO),
            operationId
        );
    }

    public static ValidationError createObjectValidationError(
        LogbookTypeProcess eventTypeProcess,
        String stepOrHandler,
        String transaction,
        String obId,
        ObjectNode evDetData,
        String operationId
    ) {
        return getValidationError(
            eventTypeProcess,
            obId,
            evDetData,
            VitamLogbookMessages.getOutcomeDetailLfc(stepOrHandler, transaction, StatusCode.KO),
            VitamLogbookMessages.getCodeLfc(stepOrHandler, transaction, StatusCode.KO),
            operationId
        );
    }

    public static ValidationError createObjectValidationError(
        LogbookTypeProcess eventTypeProcess,
        String stepOrHandler,
        String transaction,
        String detailedOutcome,
        String obId,
        ObjectNode evDetData,
        String operationId
    ) {
        return getValidationError(
            eventTypeProcess,
            obId,
            evDetData,
            VitamLogbookMessages.getOutcomeDetailLfc(stepOrHandler, transaction, detailedOutcome, StatusCode.KO),
            VitamLogbookMessages.getCodeLfc(stepOrHandler, transaction, detailedOutcome, StatusCode.KO),
            operationId
        );
    }

    private static ValidationError getValidationError(
        LogbookTypeProcess eventTypeProcess,
        String obId,
        ObjectNode evDetData,
        String outDetail,
        String outMessg,
        String operationId
    ) {
        return new ValidationError()
            .setEvId(GUIDFactory.newGUID().toString())
            .setEvTypeProc(eventTypeProcess.name())
            .setEvDetData(evDetData == null ? null : JsonHandler.unprettyPrint(evDetData))
            .setObId(obId)
            .setOutDetail(outDetail)
            .setOutMessg(outMessg)
            .setEvDateTime(LocalDateUtil.nowFormatted())
            .setEvIdProc(operationId);
    }
}

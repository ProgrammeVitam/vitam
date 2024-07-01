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
package fr.gouv.vitam.worker.core.plugin.transfer.reply;

import fr.gouv.culture.archivesdefrance.seda.v2.ArchiveTransferReplyType;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.plugin.transfer.reply.model.TransferReplyContext;
import fr.gouv.vitam.worker.core.utils.PluginHelper.EventDetails;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static fr.gouv.vitam.common.model.StatusCode.FATAL;
import static fr.gouv.vitam.common.model.StatusCode.KO;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.common.model.StatusCode.WARNING;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;

public class CheckAtrAndAddItToWorkspacePlugin extends ActionHandler {

    public static final String PLUGIN_NAME = "CHECK_ATR_AND_ADD_IT_TO_WORKSPACE";

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CheckAtrAndAddItToWorkspacePlugin.class);
    private static final int TRANSFER_REPLY_CONTEXT_OUT_RANK = 0;

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) throws ProcessingException {
        ArchiveTransferReplyType atr = (ArchiveTransferReplyType) handler.getInput(0);
        try {
            String messageIdentifier = atr.getMessageIdentifier();

            if (!isValidStatus(atr)) {
                return buildItemStatus(
                    PLUGIN_NAME,
                    KO,
                    EventDetails.of(String.format("ATR '%s' is KO, workflow will stop here.", messageIdentifier))
                );
            }

            File tempFile = handler.getNewLocalFile(handler.getOutput(TRANSFER_REPLY_CONTEXT_OUT_RANK).getPath());
            FileUtils.copyInputStreamToFile(streamFromIds(atr), tempFile);
            handler.addOutputResult(TRANSFER_REPLY_CONTEXT_OUT_RANK, tempFile, true, false);

            return buildItemStatus(
                PLUGIN_NAME,
                OK,
                EventDetails.of(String.format("ATR '%s' is OK.", messageIdentifier))
            );
        } catch (IOException | InvalidParseOperationException e) {
            LOGGER.error(e);
            return buildItemStatus(PLUGIN_NAME, FATAL, EventDetails.of(e.getMessage()));
        }
    }

    private boolean isValidStatus(ArchiveTransferReplyType atr) {
        return OK.name().equalsIgnoreCase(atr.getReplyCode()) || WARNING.name().equalsIgnoreCase(atr.getReplyCode());
    }

    private InputStream streamFromIds(ArchiveTransferReplyType atr) throws InvalidParseOperationException {
        return JsonHandler.writeToInpustream(
            new TransferReplyContext(atr.getMessageRequestIdentifier().getValue(), atr.getMessageIdentifier())
        );
    }
}

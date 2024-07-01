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
package fr.gouv.vitam.worker.core.handler;

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessQuery;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.ProcessDetail;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.worker.common.HandlerIO;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * ListRunningIngestsAction Handler.<br>
 */

public class ListRunningIngestsActionHandler extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ListRunningIngestsActionHandler.class);

    private static final String HANDLER_ID = "LIST_RUNNING_INGESTS";
    private static final int RANK_FILE = 0;

    private final ProcessingManagementClientFactory processingManagementClientFactory;

    public ListRunningIngestsActionHandler() {
        this(ProcessingManagementClientFactory.getInstance());
    }

    @VisibleForTesting
    public ListRunningIngestsActionHandler(ProcessingManagementClientFactory processingManagementClientFactory) {
        this.processingManagementClientFactory = processingManagementClientFactory;
    }

    @Override
    public ItemStatus execute(WorkerParameters params, HandlerIO handler) {
        final ItemStatus itemStatus = new ItemStatus(HANDLER_ID);

        try {
            listRunningIngests(handler);
            itemStatus.increment(StatusCode.OK);
        } catch (ProcessingException e) {
            LOGGER.error("Processing exception", e);
            itemStatus.increment(StatusCode.FATAL);
        } catch (InvalidParseOperationException e) {
            LOGGER.error("Cannot parse json", e);
            itemStatus.increment(StatusCode.KO);
        }
        return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
    }

    private void listRunningIngests(HandlerIO handlerIO) throws ProcessingException, InvalidParseOperationException {
        ProcessQuery pq = new ProcessQuery();
        List<String> listStates = new ArrayList<>();
        listStates.add(ProcessState.RUNNING.name());
        // Should we really look after the pause states ?
        listStates.add(ProcessState.PAUSE.name());
        pq.setStates(listStates);
        List<String> listProcessTypes = new ArrayList<>();
        listProcessTypes.add(LogbookTypeProcess.INGEST.toString());
        listProcessTypes.add(LogbookTypeProcess.HOLDINGSCHEME.toString());
        listProcessTypes.add(LogbookTypeProcess.FILINGSCHEME.toString());
        pq.setListProcessTypes(listProcessTypes);
        try (ProcessingManagementClient processManagementClient = processingManagementClientFactory.getClient()) {
            // FIXME: 15/09/2019 what if response is VitamError
            RequestResponseOK<ProcessDetail> response = (RequestResponseOK<
                    ProcessDetail
                >) processManagementClient.listOperationsDetails(pq);
            List<ProcessDetail> ingestsInProcess = response.getResults();
            File tempFile = handlerIO.getNewLocalFile(handlerIO.getOutput(RANK_FILE).getPath());
            JsonHandler.writeAsFile(ingestsInProcess, tempFile);
            handlerIO.addOutputResult(RANK_FILE, tempFile, true, false);
        } catch (VitamClientException e) {
            LOGGER.error("Process Management cannot be called", e);
            throw new ProcessingException(e);
        }
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // Nothing to check
    }

    /**
     * @return HANDLER_ID
     */
    public static final String getId() {
        return HANDLER_ID;
    }
}

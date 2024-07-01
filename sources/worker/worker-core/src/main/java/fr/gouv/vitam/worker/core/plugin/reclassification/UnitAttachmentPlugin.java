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
package fr.gouv.vitam.worker.core.plugin.reclassification;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUIDReader;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.LifeCycleStatusCode;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleUnitParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.exception.ProcessingStatusException;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.plugin.reclassification.model.ReclassificationEventDetails;

import java.util.Set;

import static fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper.add;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.createParameters;

/**
 * Unit attachment plugin.
 *
 * This plugin proceeds attachment requests
 */
public class UnitAttachmentPlugin extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(UnitAttachmentPlugin.class);
    private static final String UNIT_ATTACHMENT = "UNIT_ATTACHMENT";
    private static final String UNITS_TO_ATTACH_DIR = "UnitsToAttach";

    private final LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory;
    private final MetaDataClientFactory metaDataClientFactory;

    /**
     * Default constructor
     */
    public UnitAttachmentPlugin() {
        this(LogbookLifeCyclesClientFactory.getInstance(), MetaDataClientFactory.getInstance());
    }

    /**
     * Constructor for testing only
     */
    @VisibleForTesting
    UnitAttachmentPlugin(
        LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory,
        MetaDataClientFactory metaDataClientFactory
    ) {
        this.logbookLifeCyclesClientFactory = logbookLifeCyclesClientFactory;
        this.metaDataClientFactory = metaDataClientFactory;
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) throws ProcessingException {
        String unitId = param.getObjectName();
        try {
            Set<String> parentUnitsToAdd = getParentsToAdd(handler, unitId);

            updateUnit(unitId, parentUnitsToAdd);

            updateUnitLifeCycle(param, unitId, parentUnitsToAdd);
        } catch (ProcessingStatusException e) {
            LOGGER.error("Unit attachment failed with status [" + e.getStatusCode() + "]", e);
            return buildItemStatus(UNIT_ATTACHMENT, e.getStatusCode(), e.getEventDetails());
        }

        LOGGER.debug("Unit attachment succeeded: " + unitId);

        return buildItemStatus(UNIT_ATTACHMENT, StatusCode.OK, null);
    }

    private Set<String> getParentsToAdd(HandlerIO handler, String unitId)
        throws ProcessingException, ProcessingStatusException {
        try {
            JsonNode attachmentJson = handler.getJsonFromWorkspace(UNITS_TO_ATTACH_DIR + "/" + unitId);
            return JsonHandler.getFromJsonNode(attachmentJson, Set.class);
        } catch (InvalidParseOperationException e) {
            throw new ProcessingStatusException(StatusCode.FATAL, "Could not parse attachment json", e);
        }
    }

    private void updateUnit(String unitId, Set<String> parentUnitsToAdd) throws ProcessingStatusException {
        try (MetaDataClient metaDataClient = metaDataClientFactory.getClient()) {
            UpdateMultiQuery updateMultiQuery = new UpdateMultiQuery();
            updateMultiQuery.addActions(
                add(VitamFieldsHelper.unitups(), parentUnitsToAdd.toArray(new String[0])),
                add(VitamFieldsHelper.operations(), VitamThreadUtils.getVitamSession().getRequestId())
            );

            metaDataClient.updateUnitById(updateMultiQuery.getFinalUpdate(), unitId);
        } catch (
            MetaDataDocumentSizeException
            | MetaDataClientServerException
            | MetaDataExecutionException
            | MetaDataNotFoundException
            | InvalidParseOperationException
            | InvalidCreateOperationException e
        ) {
            throw new ProcessingStatusException(StatusCode.FATAL, "An error occurred during unit attachment", e);
        }
    }

    private void updateUnitLifeCycle(WorkerParameters param, String unitId, Set<String> parentUnitsToAdd)
        throws ProcessingStatusException {
        try (LogbookLifeCyclesClient logbookLifeCyclesClient = logbookLifeCyclesClientFactory.getClient()) {
            ReclassificationEventDetails eventDetails = new ReclassificationEventDetails()
                .setAddedParents(parentUnitsToAdd);
            LogbookLifeCycleUnitParameters logbookLCParam = createParameters(
                GUIDReader.getGUID(param.getContainerName()),
                StatusCode.OK,
                GUIDReader.getGUID(unitId),
                UNIT_ATTACHMENT,
                eventDetails,
                LogbookTypeProcess.RECLASSIFICATION
            );

            logbookLifeCyclesClient.update(logbookLCParam, LifeCycleStatusCode.LIFE_CYCLE_COMMITTED);
        } catch (VitamException e) {
            throw new ProcessingStatusException(
                StatusCode.FATAL,
                "An error occurred during lifecycle update for unit " + unitId,
                e
            );
        }
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // NOP
    }
}

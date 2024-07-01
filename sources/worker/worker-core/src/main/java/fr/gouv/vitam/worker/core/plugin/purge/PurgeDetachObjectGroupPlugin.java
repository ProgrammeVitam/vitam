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
package fr.gouv.vitam.worker.core.plugin.purge;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.exception.ProcessingStatusException;
import fr.gouv.vitam.worker.core.handler.ActionHandler;

import java.util.Set;

import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;

/**
 * Purge detach object group plugin.
 */
public class PurgeDetachObjectGroupPlugin extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(PurgeDetachObjectGroupPlugin.class);
    private static final TypeReference<Set<String>> STRING_SET_TYPE_REFERENCE = new TypeReference<Set<String>>() {};

    private final String actionId;
    private final PurgeDeleteService purgeDeleteService;

    /**
     * Default constructor
     */
    public PurgeDetachObjectGroupPlugin(String actionId) {
        this(actionId, new PurgeDeleteService());
    }

    /***
     * Test only constructor
     */
    @VisibleForTesting
    protected PurgeDetachObjectGroupPlugin(String actionId, PurgeDeleteService purgeDeleteService) {
        this.actionId = actionId;
        this.purgeDeleteService = purgeDeleteService;
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) throws ProcessingException {
        String objectGroupId = param.getObjectName();
        try {
            Set<String> parentUnitsToRemove = getParentUnitsToRemove(param);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(
                    "Detaching deleted parents [" +
                    String.join(", ", parentUnitsToRemove) +
                    "]" +
                    " from object group " +
                    objectGroupId
                );
            }

            purgeDeleteService.detachObjectGroupFromDeleteParentUnits(objectGroupId, parentUnitsToRemove);

            LOGGER.info("Object group " + objectGroupId + " detachment from parents succeeded");

            return buildItemStatus(actionId, StatusCode.OK, null);
        } catch (ProcessingStatusException e) {
            LOGGER.error(
                "Object group " +
                objectGroupId +
                " detachment from parents failed with status" +
                " [" +
                e.getStatusCode() +
                "]",
                e
            );
            return buildItemStatus(actionId, e.getStatusCode(), e.getEventDetails());
        }
    }

    private Set<String> getParentUnitsToRemove(WorkerParameters params) throws ProcessingStatusException {
        try {
            return JsonHandler.getFromJsonNode(params.getObjectMetadata(), STRING_SET_TYPE_REFERENCE);
        } catch (Exception e) {
            throw new ProcessingStatusException(StatusCode.FATAL, "Could not retrieve parent units to detach", e);
        }
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // NOP.
    }
}

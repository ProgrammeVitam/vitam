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
package fr.gouv.vitam.worker.core.plugin.massprocessing.description;

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.InternalActionKeysRetriever;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.utils.PluginHelper.EventDetails;

import java.util.List;

import static fr.gouv.vitam.common.model.StatusCode.KO;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatusWithMessage;

public class MassUpdateCheck extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(MassUpdateCheck.class);
    private static final String PLUGIN_NAME = "MASS_UPDATE_CHECK";

    private final InternalActionKeysRetriever internalActionKeysRetriever;

    public MassUpdateCheck() {
        this(new InternalActionKeysRetriever());
    }

    @VisibleForTesting
    public MassUpdateCheck(InternalActionKeysRetriever internalActionKeysRetriever) {
        this.internalActionKeysRetriever = internalActionKeysRetriever;
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) throws ProcessingException {
        try {
            List<String> internalKeyFields = internalActionKeysRetriever.getInternalActionKeyFields(
                handler.getJsonFromWorkspace("query.json")
            );
            if (!internalKeyFields.isEmpty()) {
                String message = String.format(
                    "Invalid DSL query: cannot contains '%s' internal field(s).",
                    String.join(", ", internalKeyFields)
                );
                return buildItemStatusWithMessage(PLUGIN_NAME, KO, message);
            }

            return buildItemStatus(PLUGIN_NAME, OK, EventDetails.of("Check OK."));
        } catch (Exception e) {
            LOGGER.error(e);
            return buildItemStatus(PLUGIN_NAME, KO, EventDetails.of("Check KO unexpected error."));
        }
    }
}

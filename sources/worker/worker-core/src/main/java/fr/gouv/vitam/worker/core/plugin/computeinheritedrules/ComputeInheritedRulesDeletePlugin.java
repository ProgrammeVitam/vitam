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
package fr.gouv.vitam.worker.core.plugin.computeinheritedrules;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.database.builder.query.InQuery;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.query.action.UnsetAction;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.metadata.api.exception.MetaDataException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;

import java.util.List;

import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildBulkItemStatus;

public class ComputeInheritedRulesDeletePlugin extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ComputeInheritedRulesDeletePlugin.class);

    private static final String PLUGIN_NAME = "COMPUTE_INHERITED_RULES_DELETE";
    private final MetaDataClientFactory metaDataClientFactory;

    public ComputeInheritedRulesDeletePlugin() {
        this(MetaDataClientFactory.getInstance());
    }

    @VisibleForTesting
    ComputeInheritedRulesDeletePlugin(MetaDataClientFactory metaDataClientFactory) {
        this.metaDataClientFactory = metaDataClientFactory;
    }

    @Override
    public List<ItemStatus> executeList(WorkerParameters workerParameters, HandlerIO handler)
        throws ProcessingException {
        LOGGER.debug("execute Compute Inherited Rules Delete Plugin");

        try (MetaDataClient metaDataClient = metaDataClientFactory.getClient()) {
            SelectMultiQuery select = new SelectMultiQuery();
            InQuery query = QueryHelper.in(
                VitamFieldsHelper.id(),
                workerParameters.getObjectNameList().toArray(new String[0])
            );
            select.setQuery(query);

            JsonNode response = metaDataClient.selectUnitsWithInheritedRules(select.getFinalSelect());

            RequestResponseOK<JsonNode> requestResponseOK = RequestResponseOK.getFromJsonNode(response);
            List<JsonNode> archiveWithInheritedRules = requestResponseOK.getResults();

            for (JsonNode archiveUnit : archiveWithInheritedRules) {
                String unitId = archiveUnit.get(VitamFieldsHelper.id()).textValue();
                ObjectNode updateMultiQuery = getUpdateQuery();
                metaDataClient.updateUnitById(updateMultiQuery, unitId);
            }

            return buildBulkItemStatus(workerParameters, PLUGIN_NAME, StatusCode.OK);
        } catch (InvalidCreateOperationException | MetaDataException e) {
            throw new ProcessingException(e);
        } catch (final InvalidParseOperationException e) {
            LOGGER.error("File couldn't be converted into json", e);
            return buildBulkItemStatus(workerParameters, PLUGIN_NAME, StatusCode.KO);
        }
    }

    private ObjectNode getUpdateQuery() throws InvalidCreateOperationException {
        UpdateMultiQuery updateMultiQuery = new UpdateMultiQuery();
        updateMultiQuery.addActions(new UnsetAction(VitamFieldsHelper.computedInheritedRules()));
        updateMultiQuery.addActions(new UnsetAction(VitamFieldsHelper.validComputedInheritedRules()));

        return updateMultiQuery.getFinalUpdateById();
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) {
        throw new VitamRuntimeException("Not implemented");
    }
}

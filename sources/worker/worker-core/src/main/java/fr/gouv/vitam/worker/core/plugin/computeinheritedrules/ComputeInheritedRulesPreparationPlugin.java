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
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.database.utils.ScrollSpliterator;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.iterables.SpliteratorIterator;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import fr.gouv.vitam.worker.core.distribution.JsonLineWriter;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.plugin.ScrollSpliteratorHelper;
import fr.gouv.vitam.worker.core.plugin.computeinheritedrules.exception.ComputedInheritedRulesException;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;

import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTION.FIELDS;
import static fr.gouv.vitam.common.database.parser.query.ParserTokens.PROJECTIONARGS.ID;
import static fr.gouv.vitam.common.json.JsonHandler.createObjectNode;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;

/**
 * ComputeInheritedRulesPreparation
 */
public class ComputeInheritedRulesPreparationPlugin extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(
        ComputeInheritedRulesPreparationPlugin.class
    );

    private static final String PLUGIN_NAME = "COMPUTE_INHERITED_RULES_PREPARATION";

    private final MetaDataClientFactory metaDataClientFactory;

    static final String UNITS_JSONL_FILE = "units.jsonl";

    public ComputeInheritedRulesPreparationPlugin() {
        this(MetaDataClientFactory.getInstance());
    }

    @VisibleForTesting
    ComputeInheritedRulesPreparationPlugin(MetaDataClientFactory metaDataClientFactory) {
        this.metaDataClientFactory = metaDataClientFactory;
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) throws ProcessingException {
        JsonNode dslQuery = handler.getJsonFromWorkspace("query.json");
        try {
            process(handler, dslQuery);
        } catch (ComputedInheritedRulesException e) {
            LOGGER.error(
                String.format("ComputeInheritedRules preparation failed with status [%s]", e.getStatusCode()),
                e
            );
            ObjectNode error = createObjectNode().put("error", e.getMessage());
            return buildItemStatus(PLUGIN_NAME, e.getStatusCode(), error);
        }
        return buildItemStatus(PLUGIN_NAME, StatusCode.OK, null);
    }

    private void process(HandlerIO handler, JsonNode dslQuery) throws ComputedInheritedRulesException {
        File unitDistributionFile = null;
        try (MetaDataClient metadataClient = metaDataClientFactory.getClient()) {
            SelectMultiQuery selectMultiQuery = createSelectMultiple(dslQuery);
            ScrollSpliterator<JsonNode> scrollRequest = ScrollSpliteratorHelper.createUnitScrollSplitIterator(
                metadataClient,
                selectMultiQuery
            );
            Iterator<JsonNode> unitIterator = new SpliteratorIterator<>(scrollRequest);
            unitDistributionFile = handler.getNewLocalFile(UNITS_JSONL_FILE);
            try (JsonLineWriter unitWriter = new JsonLineWriter(new FileOutputStream(unitDistributionFile))) {
                while (unitIterator.hasNext()) {
                    JsonNode unit = unitIterator.next();
                    String unitId = unit.get(VitamFieldsHelper.id()).asText();
                    JsonLineModel entry = new JsonLineModel(unitId);
                    unitWriter.addEntry(entry);
                }
            }
            handler.transferFileToWorkspace(UNITS_JSONL_FILE, unitDistributionFile, true, false);
        } catch (IOException | InvalidParseOperationException | ProcessingException e) {
            throw new ComputedInheritedRulesException(
                StatusCode.FATAL,
                "Could not generate unit and/or object group distributions",
                e
            );
        } finally {
            FileUtils.deleteQuietly(unitDistributionFile);
        }
    }

    private SelectMultiQuery createSelectMultiple(JsonNode initialQuery) throws InvalidParseOperationException {
        SelectParserMultiple parser = new SelectParserMultiple();
        parser.parse(initialQuery);
        SelectMultiQuery selectMultiQuery = parser.getRequest();
        ObjectNode projectionNode = getQueryProjectionToApply();
        selectMultiQuery.setProjection(projectionNode);
        return selectMultiQuery;
    }

    private ObjectNode getQueryProjectionToApply() {
        ObjectNode projectionNode = JsonHandler.createObjectNode();
        ObjectNode fields = JsonHandler.createObjectNode();
        fields.put(ID.exactToken(), 1);
        projectionNode.set(FIELDS.exactToken(), fields);
        return projectionNode;
    }
}

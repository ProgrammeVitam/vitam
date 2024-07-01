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
package fr.gouv.vitam.worker.core.plugin.probativevalue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.database.utils.ScrollSpliterator;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.iterables.SpliteratorIterator;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProbativeValueRequest;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import fr.gouv.vitam.worker.core.distribution.JsonLineWriter;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.plugin.ScrollSpliteratorHelper;
import fr.gouv.vitam.worker.core.utils.PluginHelper.EventDetails;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.and;
import static fr.gouv.vitam.common.json.JsonHandler.createObjectNode;
import static fr.gouv.vitam.common.model.StatusCode.KO;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;

public class ProbativeCreateDistributionFile extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProbativeCreateDistributionFile.class);

    private static final String HANDLER_ID = "PROBATIVE_VALUE_CREATE_DISTRIBUTION_FILE";

    private final MetaDataClientFactory metaDataClientFactory;

    @VisibleForTesting
    public ProbativeCreateDistributionFile(MetaDataClientFactory metaDataClientFactory) {
        this.metaDataClientFactory = metaDataClientFactory;
    }

    public ProbativeCreateDistributionFile() {
        this(MetaDataClientFactory.getInstance());
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) throws ProcessingException {
        File objectGroupsToCheck = handler.getNewLocalFile("OBJECT_GROUP_TO_CHECK.jsonl");

        try (
            MetaDataClient metadataClient = metaDataClientFactory.getClient();
            FileOutputStream outputStream = new FileOutputStream(objectGroupsToCheck);
            JsonLineWriter writer = new JsonLineWriter(outputStream);
            InputStream request = handler.getInputStreamFromWorkspace("request")
        ) {
            ProbativeValueRequest probativeValueRequest = JsonHandler.getFromInputStream(
                request,
                ProbativeValueRequest.class
            );
            String usageVersion = String.format(
                "%s_%s",
                probativeValueRequest.getUsage(),
                probativeValueRequest.getVersion()
            );

            SelectMultiQuery select = constructSelectMultiQuery(probativeValueRequest);
            ScrollSpliterator<JsonNode> scrollRequest = ScrollSpliteratorHelper.createUnitScrollSplitIterator(
                metadataClient,
                select
            );
            SpliteratorIterator<JsonNode> iterator = new SpliteratorIterator<>(scrollRequest);

            MultiValuedMap<String, String> unitsByObjectId = new HashSetValuedHashMap<>();
            while (iterator.hasNext()) {
                JsonNode element = iterator.next();

                JsonNode objectValue = element.path(VitamFieldsHelper.object());
                if (objectValue.isMissingNode() || objectValue.isNull() || StringUtils.isBlank(objectValue.asText())) {
                    continue;
                }

                unitsByObjectId.put(objectValue.asText(), element.get(VitamFieldsHelper.id()).asText());
            }

            for (Map.Entry<String, Collection<String>> entry : unitsByObjectId.asMap().entrySet()) {
                JsonLineModel jsonLine = toJsonLineDistribution(entry.getKey(), usageVersion, entry.getValue());
                writeLines(jsonLine, writer);
            }
        } catch (Exception e) {
            LOGGER.error(e);
            return buildItemStatus(HANDLER_ID, KO, EventDetails.of("Creation of distribution file error."));
        }

        handler.transferFileToWorkspace("distributionFile.jsonl", objectGroupsToCheck, false, false);
        return buildItemStatus(HANDLER_ID, OK, EventDetails.of("Creation of distribution file succeed."));
    }

    private void writeLines(JsonLineModel jsonLine, JsonLineWriter writer) {
        try {
            writer.addEntry(jsonLine);
        } catch (IOException e) {
            throw new VitamRuntimeException(e);
        }
    }

    private JsonLineModel toJsonLineDistribution(String objectId, String usageVersion, Collection<String> elementIds)
        throws InvalidParseOperationException {
        ObjectNode objectNode = createObjectNode();
        objectNode.set("unitIds", JsonHandler.toJsonNode(elementIds));
        objectNode.put("usageVersion", usageVersion);

        JsonLineModel model = new JsonLineModel();
        model.setId(objectId);
        model.setParams(objectNode);
        model.setDistribGroup(null);
        return model;
    }

    private SelectMultiQuery constructSelectMultiQuery(ProbativeValueRequest probativeValueRequest)
        throws InvalidParseOperationException, InvalidCreateOperationException {
        SelectParserMultiple parser = new SelectParserMultiple();

        JsonNode probativeQuery = probativeValueRequest.getDslQuery();
        parser.parse(probativeQuery);
        SelectMultiQuery select = parser.getRequest();

        List<Query> queryList = new ArrayList<>(select.getQueries());
        if (queryList.isEmpty()) {
            select.getQueries().add(QueryHelper.exists(VitamFieldsHelper.object()).setDepthLimit(0));
        } else {
            Query lastQuery = queryList.get(queryList.size() - 1);
            Query queryExistObject = and()
                .add(lastQuery, QueryHelper.exists(VitamFieldsHelper.object()).setDepthLimit(0));
            select.getQueries().set(queryList.size() - 1, queryExistObject);
        }
        select.addUsedProjection(VitamFieldsHelper.id(), VitamFieldsHelper.object());

        ObjectNode objectGroup = createObjectNode();
        objectGroup.put(VitamFieldsHelper.object(), 1);
        ObjectNode filter = createObjectNode();
        filter.set("$orderby", objectGroup);
        select.setFilter(filter);

        return select;
    }
}

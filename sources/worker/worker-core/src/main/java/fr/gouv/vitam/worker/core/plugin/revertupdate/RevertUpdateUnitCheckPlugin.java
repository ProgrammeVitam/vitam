/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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

package fr.gouv.vitam.worker.core.plugin.revertupdate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.query.action.SetAction;
import fr.gouv.vitam.common.database.builder.query.action.UnsetAction;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.database.parser.query.ParserTokens;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.database.parser.request.multiple.UpdateParserMultiple;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.logbook.LogbookOperation;
import fr.gouv.vitam.common.model.revertupdate.RevertUpdateOptions;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import fr.gouv.vitam.worker.core.distribution.JsonLineWriter;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.handler.HandlerUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.bson.Document;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class RevertUpdateUnitCheckPlugin extends ActionHandler {

    @VisibleForTesting
    static final String REVERT_UPDATE_UNITS_JSONL_FILE = "revertUpdateUnits.jsonl";

    private static final VitamLogger LOGGER =
        VitamLoggerFactory.getInstance(RevertUpdateUnitCheckPlugin.class);
    private static final String PLUGIN_ID = "REVERT_CHECK";

    private static final String REGEX_START = "^[+\\-]\\s+\\\"";
    private static final String REGEX_END = "\" :.*$";

    private static final List<List<String>> AUTORIZED_FIELDS = List.of(
        List.of(
            REGEX_START + "Title" + REGEX_END,
            REGEX_START + "Description" + REGEX_END,
            REGEX_START + "DescriptionLevel" + REGEX_END
        ),
        List.of(
            REGEX_START + "Title" + REGEX_END,
            REGEX_START + "Title_\\..*" + REGEX_END,
            REGEX_START + "Description" + REGEX_END,
            REGEX_START + "Description_\\..*" + REGEX_END,
            REGEX_START + "DescriptionLevel" + REGEX_END
        )
    );

    private final MetaDataClientFactory metaDataClientFactory;
    private final LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory;

    public RevertUpdateUnitCheckPlugin() {
        this(MetaDataClientFactory.getInstance(), LogbookLifeCyclesClientFactory.getInstance());
    }

    @VisibleForTesting
    public RevertUpdateUnitCheckPlugin(MetaDataClientFactory metaDataClientFactory,
        LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory) {
        this.metaDataClientFactory = metaDataClientFactory;
        this.logbookLifeCyclesClientFactory = logbookLifeCyclesClientFactory;
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) throws ProcessingException {
        final ItemStatus itemStatus = new ItemStatus();

        try (MetaDataClient metaDataClient = metaDataClientFactory.getClient();
            LogbookLifeCyclesClient logbookLifeCyclesClient = logbookLifeCyclesClientFactory.getClient()) {
            RevertUpdateOptions options =
                JsonHandler.getFromFile(handler.getInput(0, File.class), RevertUpdateOptions.class);
            final String operationId = options.getOperationId();

            final SelectParserMultiple parser = new SelectParserMultiple();
            parser.parse(options.getDslRequest());
            parser.getRequest()
                .addQueries(QueryHelper.in(VitamFieldsHelper.operations(), operationId));
            parser.getRequest().setProjection(JsonHandler.createObjectNode()
                .set(ParserTokens.PROJECTION.FIELDS.exactToken(),
                    JsonHandler.createObjectNode().put(VitamFieldsHelper.id(), 1)
                        .put(VitamFieldsHelper.operations(), 1)));
            JsonNode unitsIdsResult = metaDataClient.selectUnits(parser.getRequest().getFinalSelect());

            JsonNode results = unitsIdsResult.get(RequestResponseOK.TAG_RESULTS);
            Iterator<List<JsonNode>> bulkUnitIds =
                Iterators.partition(results.iterator(), VitamConfiguration.getBatchSize());

            Map<UpdateMultiQuery, Set<String>> resultMap = new HashMap<>();
            while (bulkUnitIds.hasNext()) {
                List<JsonNode> unitIds = bulkUnitIds.next();

                if (!options.isForce()) {
                    boolean canBeDone = unitIds.stream().map(e -> e.get(VitamFieldsHelper.operations()))
                        .map(e -> StreamSupport.stream(e.spliterator(), false).map(JsonNode::asText)
                            .collect(Collectors.toList()))
                        .map(e -> e.indexOf(operationId) == (e.size() - 1))
                        .reduce(true, Boolean::logicalAnd);

                    if (!canBeDone) {
                        itemStatus.increment(StatusCode.KO);
                        HandlerUtils.save(handler, Collections.emptyList(), 0);
                        return new ItemStatus(PLUGIN_ID).setItemsStatus(PLUGIN_ID, itemStatus);
                    }
                }

                List<String> unitsId = unitIds.stream().map(e -> e.get(VitamFieldsHelper.id())).map(
                    JsonNode::asText).collect(Collectors.toList());

                List<JsonNode> unitLifeCycleByIds = logbookLifeCyclesClient.getRawUnitLifeCycleByIds(unitsId);
                List<LogbookOperation> events =
                    JsonHandler.getFromJsonNodeList(unitLifeCycleByIds, new TypeReference<>() {
                    });

                Map<String, Document> unitsLFC = events.stream()
                    .map(e -> e.getEvents().stream().filter(t -> t.getEvIdProc().equals(operationId)).findFirst())
                    .map(Optional::get)
                    .map(e -> new SimpleEntry<>(e.getObId(),
                        Document.parse(e.getEvDetData())))
                    .collect(Collectors.toMap(Entry::getKey, Entry::getValue));


                for (String unitId : unitsLFC.keySet()) {
                    List<String> originalDiffLines =
                        VitamDocument.getOriginalDiffLines(unitsLFC.get(unitId).getString("diff"));
                    int version = unitsLFC.get(unitId).getInteger("version", 0);
                    Stream<String> stringStream = originalDiffLines.stream()
                        .filter(e -> AUTORIZED_FIELDS.get(version).stream().anyMatch(e::matches));

                    stringStream = (options.getFields().isEmpty()) ?
                        stringStream :
                        stringStream
                            .filter(e -> options.getFields().stream().anyMatch(t -> e.contains("\"" + t + "\" :")));

                    List<String> modifiedFields =
                        stringStream.map(StringEscapeUtils::unescapeJava).collect(Collectors.toList());

                    if (modifiedFields.isEmpty()) {
                        continue;
                    }

                    Map<String, String> oldValues =
                        modifiedFields.stream().filter(e -> e.startsWith("-")).map(e -> e.replaceFirst("-\\s+", ""))
                            .map(e -> e.replaceAll("^\"", "").replaceAll("\"$", "").split("\" : \""))
                            .collect(Collectors.toMap(e -> e[0], e -> e[1]));

                    Map<String, String> newValues =
                        modifiedFields.stream().filter(e -> e.startsWith("+")).map(e -> e.replaceFirst("\\+\\s+", ""))
                            .map(e -> e.replaceAll("^\"", "").replaceAll("\"$", "").split("\" : \""))
                            .collect(Collectors.toMap(e -> e[0], e -> e[1]));

                    UpdateParserMultiple updateParserMultiple = new UpdateParserMultiple();
                    updateParserMultiple.getRequest().resetRoots();

                    for (String keyToUpdate : Sets.intersection(oldValues.keySet(), newValues.keySet())) {
                        updateParserMultiple.getRequest()
                            .addActions(new SetAction(keyToUpdate, oldValues.get(keyToUpdate)));
                    }

                    Set<String> keysToDelete = Sets.difference(oldValues.keySet(), newValues.keySet());
                    if (!keysToDelete.isEmpty()) {
                        updateParserMultiple.getRequest()
                            .addActions(new UnsetAction(keysToDelete.toArray(String[]::new)));
                    }

                    resultMap.put(updateParserMultiple.getRequest(),
                        Sets.union(resultMap.getOrDefault(updateParserMultiple.getRequest(), new HashSet<>()),
                            Set.of(unitId)));
                }
            }



            File revertUpdateUnitsFile = handler.getNewLocalFile(REVERT_UPDATE_UNITS_JSONL_FILE);
            try (JsonLineWriter logbookOperationsWriter = new JsonLineWriter(
                new FileOutputStream(revertUpdateUnitsFile))) {
                List<String> queries = new ArrayList<>();
                for (UpdateMultiQuery query : resultMap.keySet()) {
                    query.resetRoots().addRoots(resultMap.get(query).toArray(String[]::new));
                    String queryString = query.getFinalUpdate().toString();
                    JsonLineModel entry = new JsonLineModel(queryString, null, null);
                    queries.add(queryString);
                    logbookOperationsWriter.addEntry(entry);
                }
                HandlerUtils.save(handler, queries, 0);
            }

            handler.transferFileToWorkspace(REVERT_UPDATE_UNITS_JSONL_FILE, revertUpdateUnitsFile, true, false);
        } catch (InvalidParseOperationException | InvalidCreateOperationException | MetaDataExecutionException | MetaDataDocumentSizeException | MetaDataClientServerException | LogbookClientException | IOException e) {
            throw new ProcessingException(e);
        }

        return new ItemStatus(PLUGIN_ID).setItemsStatus(PLUGIN_ID, itemStatus.increment(StatusCode.OK));
    }
}

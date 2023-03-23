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
package fr.gouv.vitam.collect.internal.core.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.collect.common.exception.CollectInternalException;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.InQuery;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.InsertMultiQuery;
import fr.gouv.vitam.common.database.builder.request.multiple.RequestMultiple;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.database.parser.request.multiple.RequestParserHelper;
import fr.gouv.vitam.common.database.parser.request.multiple.RequestParserMultiple;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.database.utils.ScrollSpliterator;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.metadata.api.model.BulkUnitInsertEntry;
import fr.gouv.vitam.metadata.api.model.BulkUnitInsertRequest;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;

import javax.annotation.Nonnull;
import javax.ws.rs.ProcessingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.and;
import static fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper.initialOperation;

public class MetadataRepository {

    private static final String DATA_CATEGORY = "Data category ";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(MetadataRepository.class);
    private static final String NOT_A_SELECT_OPERATION = "Not a Select operation";

    private final MetaDataClientFactory metaDataCollectClientFactory;

    public MetadataRepository(MetaDataClientFactory metaDataCollectClientFactory) {
        this.metaDataCollectClientFactory = metaDataCollectClientFactory;
    }

    public RequestResponseOK<JsonNode> selectUnits(JsonNode queryDsl, @Nonnull String transactionId)
        throws CollectInternalException {
        try (MetaDataClient metaDataClient = metaDataCollectClientFactory.getClient()) {
            SelectParserMultiple parser = new SelectParserMultiple();
            parser.parse(queryDsl);

            applyTransactionToQuery(transactionId, parser.getRequest());
            JsonNode jsonNode = metaDataClient.selectUnits(parser.getRequest().getFinalSelect());
            return JsonHandler.getFromJsonNode(jsonNode, RequestResponseOK.class, JsonNode.class);
        } catch (MetaDataExecutionException | MetaDataClientServerException | InvalidParseOperationException |
                 MetaDataDocumentSizeException | InvalidCreateOperationException e) {
            LOGGER.error("Error when getting units in metadata: {}", e);
            throw new CollectInternalException("Error when getting units in metadata: " + e);
        }
    }

    public ScrollSpliterator<JsonNode> selectUnits(SelectMultiQuery request, @Nonnull String transactionId) {
        return new ScrollSpliterator<>(request, query -> {
            try {
                return selectUnits(request.getFinalSelect(), transactionId);
            } catch (CollectInternalException e) {
                throw new IllegalStateException(e);
            }
        }, VitamConfiguration.getElasticSearchScrollTimeoutInMilliseconds(),
            VitamConfiguration.getElasticSearchScrollLimit());

    }

    public JsonNode selectUnitById(String unitId) throws CollectInternalException {
        try (MetaDataClient client = metaDataCollectClientFactory.getClient()) {
            final SelectMultiQuery select = new SelectMultiQuery();
            JsonNode jsonNode = client.selectUnitbyId(select.getFinalSelect(), unitId);
            RequestResponseOK<JsonNode> response =
                JsonHandler.getFromJsonNode(jsonNode, RequestResponseOK.class, JsonNode.class);
            if (response.getResults().isEmpty()) {
                throw new CollectInternalException("Cannot find unit with id = " + unitId);
            }
            return response.getFirstResult();
        } catch (final MetaDataException | InvalidParseOperationException e) {
            LOGGER.error("Error when fetching unit in metadata: {}", e);
            throw new CollectInternalException("Error when fetching unit in metadata: " + e);
        }
    }

    public JsonNode selectObjectGroups(JsonNode queryDsl, @Nonnull String transactionId)
        throws CollectInternalException {
        try (MetaDataClient metaDataClient = metaDataCollectClientFactory.getClient()) {
            SelectParserMultiple parser = new SelectParserMultiple();
            parser.parse(queryDsl);
            applyTransactionToQuery(transactionId, parser.getRequest());
            return metaDataClient.selectObjectGroups(parser.getRequest().getFinalSelect());
        } catch (MetaDataExecutionException | MetaDataClientServerException | InvalidParseOperationException |
                 MetaDataDocumentSizeException | InvalidCreateOperationException e) {
            LOGGER.error("Error when getting units in metadata: {}", e);
            throw new CollectInternalException("Error when getting units in metadata: " + e);
        }
    }

    public JsonNode selectObjectGroupById(String objectGroupId, boolean isRaw) throws CollectInternalException {
        try (MetaDataClient client = metaDataCollectClientFactory.getClient()) {
            final SelectMultiQuery select = new SelectMultiQuery();
            if (isRaw) {
                return ((RequestResponseOK<JsonNode>) client.getObjectGroupByIdRaw(objectGroupId)).getFirstResult();
            } else {
                return client.selectObjectGrouptbyId(select.getFinalSelect(), objectGroupId);
            }
        } catch (final MetaDataException | InvalidParseOperationException | VitamClientException e) {
            LOGGER.error("Error when fetching unit in metadata: {}", e);
            throw new CollectInternalException("Error when fetching unit in metadata: " + e);
        }
    }

    public RequestResponse<JsonNode> atomicBulkUpdate(@Nonnull List<JsonNode> updateMultiQueries)
        throws CollectInternalException {
        try (MetaDataClient metaDataClient = metaDataCollectClientFactory.getClient()) {
            return metaDataClient.atomicUpdateBulk(updateMultiQueries);
        } catch (MetaDataExecutionException | MetaDataNotFoundException | MetaDataClientServerException |
                 InvalidParseOperationException | MetaDataDocumentSizeException e) {
            throw new CollectInternalException(e);
        }
    }

    public JsonNode saveArchiveUnit(ObjectNode unit) throws CollectInternalException {
        try (MetaDataClient client = metaDataCollectClientFactory.getClient()) {
            BulkUnitInsertEntry insertEntry = createInsertEntry(unit);
            return client.insertUnitBulk(new BulkUnitInsertRequest(Collections.singletonList(insertEntry)));
        } catch (final MetaDataException | InvalidParseOperationException e) {
            LOGGER.error("Error while saving unit in metadata: {}", e);
            throw new CollectInternalException("Error while saving unit in metadata: " + e);
        }
    }

    public JsonNode saveArchiveUnits(List<ObjectNode> units) throws CollectInternalException {
        try (MetaDataClient client = metaDataCollectClientFactory.getClient()) {
            List<BulkUnitInsertEntry> list =
                units.stream().map(MetadataRepository::createInsertEntry).collect(Collectors.toList());
            return client.insertUnitBulk(new BulkUnitInsertRequest(list));
        } catch (final MetaDataException | InvalidParseOperationException e) {
            LOGGER.error("Error while saving unit in metadata: {}", e);
            throw new CollectInternalException("Error while saving unit in metadata: " + e);
        }
    }

    private static BulkUnitInsertEntry createInsertEntry(ObjectNode e) {
        JsonNode jsonNode = e.get(VitamFieldsHelper.unitups());
        try {
            if (jsonNode == null || jsonNode.isNull()) {
                return new BulkUnitInsertEntry(Collections.emptySet(), e);
            }
            return new BulkUnitInsertEntry(JsonHandler.getFromJsonNode(jsonNode, Set.class, String.class), e);
        } catch (InvalidParseOperationException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void updateUnitById(UpdateMultiQuery updateQuery, String transactionId, String unitId)
        throws CollectInternalException {
        try (MetaDataClient client = metaDataCollectClientFactory.getClient()) {
            applyTransactionToQuery(transactionId, updateQuery);
            client.updateUnitById(updateQuery.getFinalUpdateById(), unitId);
        } catch (final MetaDataException | InvalidParseOperationException | InvalidCreateOperationException e) {
            LOGGER.error("Error while update updating in metadata: {}", e);
            throw new CollectInternalException("Error while updating unit in metadata: " + e);
        }
    }


    public JsonNode saveObjectGroup(ObjectNode og) throws CollectInternalException {
        try (MetaDataClient client = metaDataCollectClientFactory.getClient()) {
            final InsertMultiQuery insert = new InsertMultiQuery();
            insert.resetFilter();
            insert.addHintFilter(BuilderToken.FILTERARGS.OBJECTGROUPS.exactToken());
            insert.addData(og);
            return client.insertObjectGroup(insert.getFinalInsert());
        } catch (final MetaDataException | InvalidParseOperationException e) {
            LOGGER.error("Error while saving objectGroup in metadata: {}", e);
            throw new CollectInternalException("Error while saving objectGroup in metadata: " + e);
        }
    }

    public JsonNode saveObjectGroups(List<ObjectNode> objectGroups) throws CollectInternalException {
        try (MetaDataClient client = metaDataCollectClientFactory.getClient()) {
            List<JsonNode> collect = objectGroups.stream().map(e -> {
                final InsertMultiQuery insert = new InsertMultiQuery();
                insert.resetFilter();
                insert.addData(e);
                return insert.getFinalInsert();
            }).collect(Collectors.toList());
            return client.insertObjectGroups(collect);
        } catch (final MetaDataException | InvalidParseOperationException e) {
            LOGGER.error("Error while saving objectGroup in metadata: {}", e);
            throw new CollectInternalException("Error while saving objectGroup in metadata: " + e);
        }
    }

    public void updateObjectGroupById(UpdateMultiQuery updateQuery, String objectGroupId, String transactionId)
        throws CollectInternalException {
        try (MetaDataClient client = metaDataCollectClientFactory.getClient()) {
            applyTransactionToQuery(transactionId, updateQuery);
            client.updateObjectGroupById(updateQuery.getFinalUpdate(), objectGroupId);
        } catch (final MetaDataException | InvalidParseOperationException | InvalidCreateOperationException e) {
            LOGGER.error("Error while updating objectGroup in metadata: {}", e);
            throw new CollectInternalException("Error while updating objectGroup in metadata: " + e);
        }
    }

    public void deleteUnits(Collection<String> listUnitIds) throws CollectInternalException {
        try (MetaDataClient client = metaDataCollectClientFactory.getClient()) {
            client.deleteUnitsBulk(listUnitIds);
        } catch (final MetaDataException e) {
            LOGGER.error("Error when delete units and objects in metadata: {}", e);
            throw new CollectInternalException("Error when delete units and objects in metadata: " + e);
        }
    }

    public void deleteObjectGroups(Collection<String> listGotGUID) throws CollectInternalException {
        try (MetaDataClient client = metaDataCollectClientFactory.getClient()) {
            client.deleteObjectGroupBulk(listGotGUID);
        } catch (final MetaDataException | InvalidParseOperationException e) {
            LOGGER.error("Error when delete units and objects in metadata: {}", e);
            throw new CollectInternalException("Error when delete units and objects in metadata: " + e);
        }
    }

    private void applyTransactionToQuery(String transactionId, RequestMultiple select)
        throws InvalidCreateOperationException {
        InQuery inQuery = QueryHelper.in(initialOperation(), transactionId);
        final List<Query> queries = select.getQueries();
        if (queries.isEmpty()) {
            queries.add(inQuery);
        } else {
            List<Query> queryList = new ArrayList<>(queries);
            Query lastQuery = queryList.get(queryList.size() - 1);
            Query mergedQuery = and().add(lastQuery, inQuery);
            queries.set(queryList.size() - 1, mergedQuery);
        }
    }


    public JsonNode selectUnitsWithInheritedRules(JsonNode jsonQuery, @Nonnull String transactionId)
        throws InvalidParseOperationException {

        ParametersChecker.checkParameter(DATA_CATEGORY, jsonQuery);

        // Check correctness of request
        final RequestParserMultiple parser = RequestParserHelper.getParser(jsonQuery.deepCopy());
        if (!(parser instanceof SelectParserMultiple)) {
            LOGGER.error(NOT_A_SELECT_OPERATION);
            throw new InvalidParseOperationException(NOT_A_SELECT_OPERATION);
        }

        try (MetaDataClient metaDataClient = metaDataCollectClientFactory.getClient()) {
            applyTransactionToQuery(transactionId, parser.getRequest());
            return metaDataClient.selectUnitsWithInheritedRules(jsonQuery);
        } catch (MetaDataDocumentSizeException | ProcessingException | MetaDataClientServerException |
                 MetaDataExecutionException | InvalidCreateOperationException e) {
            LOGGER.error("Error on selecting units with Inherited Rules", e);
            throw new InvalidParseOperationException(e);
        }
    }
}

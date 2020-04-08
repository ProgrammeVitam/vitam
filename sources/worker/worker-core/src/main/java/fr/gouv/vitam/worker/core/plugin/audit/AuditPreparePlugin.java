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
package fr.gouv.vitam.worker.core.plugin.audit;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.and;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.exists;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.in;
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTION.FIELDS;
import static fr.gouv.vitam.common.database.parser.query.ParserTokens.PROJECTIONARGS.ID;
import static fr.gouv.vitam.common.database.parser.query.ParserTokens.PROJECTIONARGS.OBJECT;
import static fr.gouv.vitam.common.json.JsonHandler.createObjectNode;
import static fr.gouv.vitam.common.json.JsonHandler.getFromStringAsTypeReference;
import static fr.gouv.vitam.common.model.StatusCode.FATAL;
import static fr.gouv.vitam.worker.core.plugin.ScrollSpliteratorHelper.createUnitScrollSplitIterator;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Iterators;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;

import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.database.utils.ScrollSpliterator;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.iterables.SpliteratorIterator;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.objectgroup.ObjectGroupResponse;
import fr.gouv.vitam.common.model.objectgroup.QualifiersModel;
import fr.gouv.vitam.common.model.objectgroup.VersionsModel;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import fr.gouv.vitam.worker.core.distribution.JsonLineWriter;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.plugin.audit.model.AuditObject;
import fr.gouv.vitam.worker.core.plugin.audit.model.AuditObjectGroup;
import fr.gouv.vitam.worker.core.utils.GroupByObjectIterator;

/**
 * AuditPreparePlugin.
 */
public class AuditPreparePlugin extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AuditPreparePlugin.class);
    private static final String AUDIT_PREPARATION = "LIST_OBJECTGROUP_ID";
    protected static final String OBJECT_GROUPS_TO_AUDIT_JSONL = "AUDIT_OG";

    private final MetaDataClientFactory metaDataClientFactory;

    public AuditPreparePlugin() {
        this(MetaDataClientFactory.getInstance());
    }

    @VisibleForTesting
    AuditPreparePlugin(MetaDataClientFactory metaDataClientFactory) {
        this.metaDataClientFactory = metaDataClientFactory;
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) {

        try (MetaDataClient metaDataClient = metaDataClientFactory.getClient()) {

            SelectMultiQuery query = generateAuditQuery(handler);
            computePreparation(query, handler, metaDataClient);
            return buildItemStatus(AUDIT_PREPARATION, StatusCode.OK, createObjectNode());

        } catch (InvalidParseOperationException | IOException | ProcessingException e) {
            LOGGER.error(String.format("Audit action failed with status [%s]", FATAL), e);
            ObjectNode error = createObjectNode().put("error", e.getMessage());
            return buildItemStatus(AUDIT_PREPARATION, FATAL, error);
        }
    }

    private void computePreparation(SelectMultiQuery selectMultiQuery, HandlerIO handler, MetaDataClient metaDataClient)
        throws FileNotFoundException, IOException, InvalidParseOperationException, ProcessingException {

        ScrollSpliterator<JsonNode> scrollRequest = createUnitScrollSplitIterator(metaDataClient, selectMultiQuery);
        Iterator<JsonNode> iterator = new SpliteratorIterator<>(scrollRequest);

        Iterator<Pair<String, String>> gotIdUnitIdIterator = getGotIdUnitIdIterator(iterator);

        Iterator<Pair<String, List<String>>> unitsByObjectGroupIterator = new GroupByObjectIterator(
            gotIdUnitIdIterator);

        Iterator<List<Pair<String, List<String>>>> unitsByObjectGroupBulkIterator = Iterators.partition(
            unitsByObjectGroupIterator, VitamConfiguration.getBatchSize());

        File objectGroupsToAudit = handler.getNewLocalFile(OBJECT_GROUPS_TO_AUDIT_JSONL);
        try (final FileOutputStream outputStream = new FileOutputStream(objectGroupsToAudit);
            JsonLineWriter writer = new JsonLineWriter(outputStream)) {
            while (unitsByObjectGroupBulkIterator.hasNext()) {
                List<Pair<String, List<String>>> bulkToProcess = unitsByObjectGroupBulkIterator.next();
                processBulk(bulkToProcess, handler, writer);
            }
        }
        handler.transferFileToWorkspace(OBJECT_GROUPS_TO_AUDIT_JSONL, objectGroupsToAudit, true, false);

    }

    private void processBulk(List<Pair<String, List<String>>> unitsByObjectGroupBulkIterator, HandlerIO handler,
        JsonLineWriter writer) throws InvalidParseOperationException, IOException {

        Map<String, List<String>> tempUnitsByObjectGroupMap = new HashMap<>();
        unitsByObjectGroupBulkIterator.forEach(item -> tempUnitsByObjectGroupMap.put(item.getKey(), item.getValue()));

        List<ObjectGroupResponse> objectModelsForUnitResults = getObjectModelsForUnitResults(
            tempUnitsByObjectGroupMap.keySet());

        for (ObjectGroupResponse objectGroup : objectModelsForUnitResults) {

            List<String> unitIds = tempUnitsByObjectGroupMap.get(objectGroup.getId());
            AuditObjectGroup auditDistributionLine = createAuditDistributionLine(unitIds, objectGroup);
            writer.addEntry(new JsonLineModel(auditDistributionLine.getId(), null,
                JsonHandler.toJsonNode(auditDistributionLine)));

        }

    }

    private SelectMultiQuery generateAuditQuery(HandlerIO handler) throws ProcessingException {

        JsonNode initialQuery = handler.getJsonFromWorkspace("query.json");
        return prepareUnitsWithObjectGroupsQuery(initialQuery);
    }

    private SelectMultiQuery prepareUnitsWithObjectGroupsQuery(JsonNode initialQuery) {

        try {
            SelectParserMultiple parser = new SelectParserMultiple();
            parser.parse(initialQuery);

            SelectMultiQuery selectMultiQuery = parser.getRequest();

            ObjectNode projectionNode = getQueryProjectionToApply();

            selectMultiQuery.setProjection(projectionNode);

            selectMultiQuery.addOrderByAscFilter(OBJECT.exactToken());

            List<Query> queryList = new ArrayList<>(parser.getRequest().getQueries());

            if (queryList.isEmpty()) {

                selectMultiQuery.addQueries(and().add(exists(OBJECT.exactToken())).setDepthLimit(0));
                return selectMultiQuery;

            }

            for (int i = 0; i < queryList.size(); i++) {
                final Query query = queryList.get(i);

                Query restrictedQuery = and().add(exists(OBJECT.exactToken()), query);

                parser.getRequest().getQueries().set(i, restrictedQuery);

            }
            return selectMultiQuery;
        } catch (InvalidParseOperationException | InvalidCreateOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private Iterator<Pair<String, String>> getGotIdUnitIdIterator(Iterator<JsonNode> iterator) {
        return IteratorUtils.transformedIterator(iterator,
            item -> new ImmutablePair<>(item.get(OBJECT.exactToken()).asText(),
                item.get(ID.exactToken()).asText()));
    }

    private ObjectNode getQueryProjectionToApply() {
        ObjectNode projectionNode = JsonHandler.createObjectNode();
        ObjectNode fields = JsonHandler.createObjectNode();

        fields.put(ID.exactToken(), 1);
        fields.put(OBJECT.exactToken(), 1);
        projectionNode.set(FIELDS.exactToken(), fields);

        return projectionNode;
    }

    private List<ObjectGroupResponse> getObjectModelsForUnitResults(Collection<String> objectGroupIds) {
        try {

            Select select = new Select();
            String[] ids = objectGroupIds.toArray(new String[0]);
            select.setQuery(in("#id", ids));

            ObjectNode finalSelect = select.getFinalSelect();
            JsonNode response = metaDataClientFactory.getClient().selectObjectGroups(finalSelect);

            JsonNode results = response.get("$results");
            return getFromStringAsTypeReference(results.toString(), new TypeReference<List<ObjectGroupResponse>>() {
            });

        } catch (VitamException | InvalidFormatException | InvalidCreateOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private AuditObjectGroup createAuditDistributionLine(List<String> unitUps, ObjectGroupResponse objectGroup) {

        AuditObjectGroup auditDistributionLine = new AuditObjectGroup();
        auditDistributionLine.setId(objectGroup.getId());
        auditDistributionLine.setOpi(objectGroup.getOpi());
        auditDistributionLine.setSp(objectGroup.getOriginatingAgency());
        auditDistributionLine.setUnitUps(unitUps);
        auditDistributionLine.setObjects(new ArrayList<AuditObject>());
        auditDistributionLine.setStorage(objectGroup.getStorage());

        for (QualifiersModel qualifier : objectGroup.getQualifiers()) {
            for (VersionsModel version : qualifier.getVersions()) {
                AuditObject binaryObject = new AuditObject();
                binaryObject.setId(version.getId());
                binaryObject.setOpi(version.getOpi());
                binaryObject.setAlgorithm(version.getAlgorithm());
                binaryObject.setMessageDigest(version.getMessageDigest());
                binaryObject.setQualifier(qualifier.getQualifier());
                binaryObject.setVersion(version.getDataObjectVersion());
                binaryObject.setStorage(version.getStorage());
                auditDistributionLine.getObjects().add(binaryObject);
            }

        }
        return auditDistributionLine;
    }

}

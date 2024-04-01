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
package fr.gouv.vitam.collect.internal.core.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.google.common.collect.Iterators;
import fr.gouv.culture.archivesdefrance.seda.v2.LevelType;
import fr.gouv.culture.archivesdefrance.seda.v2.UpdateOperationType;
import fr.gouv.vitam.collect.common.dto.BulkAtomicUpdateResult;
import fr.gouv.vitam.collect.common.dto.MetadataUnitUp;
import fr.gouv.vitam.collect.common.exception.CollectInternalException;
import fr.gouv.vitam.collect.common.exception.CollectInternalInvalidRequestException;
import fr.gouv.vitam.collect.common.exception.CollectInternalServerSideException;
import fr.gouv.vitam.collect.internal.core.common.CollectJsonMetadataLine;
import fr.gouv.vitam.collect.internal.core.common.ProjectModel;
import fr.gouv.vitam.collect.internal.core.common.TransactionModel;
import fr.gouv.vitam.collect.internal.core.helpers.CsvHelper;
import fr.gouv.vitam.collect.internal.core.helpers.JsonHelper;
import fr.gouv.vitam.collect.internal.core.helpers.MetadataHelper;
import fr.gouv.vitam.collect.internal.core.repository.MetadataRepository;
import fr.gouv.vitam.collect.internal.core.repository.ProjectRepository;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.builder.query.BooleanQuery;
import fr.gouv.vitam.common.database.builder.query.CompareQuery;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.query.action.SetAction;
import fr.gouv.vitam.common.database.builder.query.action.UnsetAction;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.mapping.mapper.VitamObjectMapper;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.UnitType;
import fr.gouv.vitam.common.model.VitamConstants;
import fr.gouv.vitam.common.model.unit.ArchiveUnitModel;
import fr.gouv.vitam.common.model.unit.ManagementModel;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.worker.core.distribution.JsonLineGenericIterator;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static fr.gouv.vitam.collect.internal.core.helpers.MetadataHelper.DYNAMIC_ATTACHEMENT;
import static fr.gouv.vitam.collect.internal.core.helpers.MetadataHelper.STATIC_ATTACHMENT;
import static fr.gouv.vitam.common.mapping.mapper.VitamObjectMapper.getSerializationObjectMapper;

public class MetadataService {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(MetadataService.class);

    private static final String SYSTEM_ID_FIELD_PATH =
        VitamFieldsHelper.management() + "." + "UpdateOperation" + "." + "SystemId";

    public static final int BULK_SIZE = 1000;

    private final MetadataRepository metadataRepository;

    private final ProjectRepository projectRepository;
    private final BulkAtomicUpdateMetadataService bulkAtomicUpdateMetadataService;

    public MetadataService(MetadataRepository metadataRepository, ProjectRepository projectRepository,
        BulkAtomicUpdateMetadataService bulkAtomicUpdateMetadataService) {
        this.metadataRepository = metadataRepository;
        this.projectRepository = projectRepository;
        this.bulkAtomicUpdateMetadataService = bulkAtomicUpdateMetadataService;
    }


    public JsonNode selectUnitById(String unitId) throws CollectInternalException {
        return metadataRepository.selectUnitById(unitId);
    }

    public JsonNode selectObjectGroupById(String objectGroupId) throws CollectInternalException {
        return metadataRepository.selectObjectGroupById(objectGroupId, true);
    }

    public JsonNode saveArchiveUnit(JsonNode unit, TransactionModel transactionModel)
        throws CollectInternalException, InvalidParseOperationException {
        ArchiveUnitModel unitModel =
            VitamObjectMapper.getDeserializationObjectMapper().convertValue(unit, ArchiveUnitModel.class);
        String unitId = GUIDFactory.newUnitGUID(VitamThreadUtils.getVitamSession().getTenantId()).getId();
        unitModel.setId(unitId);
        unitModel.setOpi(transactionModel.getId());
        unitModel.setUnitType(UnitType.INGEST.name());

        Optional<ProjectModel> project = projectRepository.findProjectById(transactionModel.getProjectId());

        if (project.isEmpty()) {
            throw new CollectInternalException("Cannot find project");
        }

        ProjectModel projectModel = project.get();
        Map<String, String> attachmentUnitsBySystemId = prepareAttachmentUnits(projectModel, transactionModel.getId());
        if (unitModel.getUnitups() == null || unitModel.getUnitups().isEmpty()) {
            if (projectModel.getUnitUp() != null) {
                unitModel.setUnitups(List.of(
                    attachmentUnitsBySystemId.get(projectModel.getUnitUp())));
            }
            if (projectModel.getUnitUps() != null) {
                Set<String> unitUpSet =
                    MetadataHelper.findUnitParent(((ObjectNode) unit).put(VitamFieldsHelper.id(), unitId),
                        projectModel.getUnitUps(), attachmentUnitsBySystemId);
                if (!attachmentUnitsBySystemId.isEmpty()) {
                    unitModel.setUnitups(new ArrayList<>(unitUpSet));
                }
            }
        }

        JsonNode jsonNode = getSerializationObjectMapper().convertValue(unitModel, JsonNode.class);
        insertSimpleUnit(jsonNode);
        updateSimpleUnit(transactionModel.getId(), jsonNode, unitId);
        return jsonNode;
    }

    private void updateSimpleUnit(String transactionId, JsonNode jsonNode, String unitId)
        throws CollectInternalException, InvalidParseOperationException {
        try {
            UpdateMultiQuery query = new UpdateMultiQuery();
            query.addRoots(unitId);
            final Map<String, JsonNode> metadataMap = JsonHelper.jsonToMap(jsonNode);
            query.addActions(new SetAction(metadataMap));
            metadataRepository.updateUnitById(query, transactionId, unitId);
        } catch (InvalidCreateOperationException e) {
            throw new CollectInternalException(e);
        }
    }

    private void insertSimpleUnit(JsonNode jsonNode) throws CollectInternalException {
        ObjectNode objectNode = JsonHandler.createObjectNode();
        objectNode.set(VitamFieldsHelper.id(), jsonNode.get(VitamFieldsHelper.id()));
        objectNode.set(VitamFieldsHelper.unitups(), jsonNode.get(VitamFieldsHelper.unitups()));
        objectNode.set("Title", jsonNode.get("Title"));
        metadataRepository.saveArchiveUnit(objectNode);
    }

    public void updateUnitsWithMetadataCsv(TransactionModel transaction, InputStream is)
        throws CollectInternalException {
        String requestId = VitamThreadUtils.getVitamSession().getRequestId();
        File file = PropertiesUtils.fileFromTmpFolder("metadata_" + requestId + VitamConstants.JSONL_EXTENSION);
        try {
            CsvHelper.convertCsvToJsonlMetadataFile(is, file);
            try (InputStream jsonlMetadataInputStream = new FileInputStream(file)) {
                updateUnitsWithJsonlMetadataFile(transaction.getId(), jsonlMetadataInputStream);
            }
        } catch (IOException e) {
            throw new CollectInternalException(e);
        } finally {
            FileUtils.deleteQuietly(file);
        }
    }

    public RequestResponseOK<JsonNode> selectUnitsByTransactionId(JsonNode queryDsl, String transactionId)
        throws CollectInternalException {
        return metadataRepository.selectUnits(queryDsl, transactionId);
    }

    void updateUnitsWithJsonlMetadataFile(String transactionId, InputStream is)
        throws CollectInternalException {

        try (JsonLineGenericIterator<CollectJsonMetadataLine> metadata =
            new JsonLineGenericIterator<>(is, CollectJsonMetadataLine.TYPE_REFERENCE)) {

            Iterator<List<CollectJsonMetadataLine>> iterator = Iterators.partition(metadata, BULK_SIZE);
            int nbOK = 0;
            int nbKO = 0;
            List<String> firstNErrorMessages = new ArrayList<>();
            while (iterator.hasNext()) {
                List<CollectJsonMetadataLine> jsonlMetadataLinesBatch = iterator.next();

                List<BulkAtomicUpdateResult> updateUnitsBatchResults
                    = updateUnitsBatch(transactionId, jsonlMetadataLinesBatch);

                for (BulkAtomicUpdateResult updateUnitsBatchResult : updateUnitsBatchResults) {
                    switch (updateUnitsBatchResult.getStatus()) {
                        case OK:
                            nbOK++;
                            LOGGER.debug("Update OK of unit " + updateUnitsBatchResult.getUpdatedUnitId());
                            break;
                        case KO:
                            LOGGER.debug("Update failed : " + updateUnitsBatchResult.getErrorDetails());
                            if (firstNErrorMessages.size() < 10) {
                                firstNErrorMessages.add(updateUnitsBatchResult.getErrorDetails());
                            }
                            nbKO++;
                            break;
                        default:
                            throw new IllegalStateException("Unexpected value: " + updateUnitsBatchResult.getStatus());
                    }
                }
            }

            if (nbOK == 0 && nbKO == 0) {
                throw new CollectInternalException("no update data found !");
            }

            if (nbKO == 0) {
                LOGGER.info("Metadata update succeeded. Nb OK: " + nbOK);
                return;
            }

            boolean isTruncated = nbKO > firstNErrorMessages.size();
            throw new CollectInternalInvalidRequestException(
                "Metadata update failed. Nb OK: " + nbOK + ", Nb KO: " + nbKO
                    + ". Error messages" + (isTruncated ? " (truncated)" : "") + ":" + firstNErrorMessages);
        }
    }

    private List<BulkAtomicUpdateResult> updateUnitsBatch(String transactionId,
        List<CollectJsonMetadataLine> jsonlMetadataLines)
        throws CollectInternalException {

        ArrayNode queries = JsonHandler.createArrayNode();
        for (CollectJsonMetadataLine jsonMetadataLine : jsonlMetadataLines) {
            queries.add(createUpdateQuery(jsonMetadataLine));
        }

        return this.bulkAtomicUpdateMetadataService.bulkAtomicUpdateUnits(transactionId, queries, true);
    }

    private static ObjectNode createUpdateQuery(CollectJsonMetadataLine jsonMetadataLine)
        throws CollectInternalServerSideException {

        try {
            UpdateMultiQuery updateMultiQuery = new UpdateMultiQuery();
            updateMultiQuery.addQueries(buildSelectorQuery(jsonMetadataLine));

            Map<String, JsonNode> fieldsToSet = new HashMap<>();
            List<String> fieldsToUnset = new ArrayList<>();

            jsonMetadataLine.getUnitContent().fields().forEachRemaining(e -> {
                if (e.getValue().isNull()) {
                    fieldsToUnset.add(e.getKey());
                } else {
                    fieldsToSet.put(e.getKey(), e.getValue());
                }
            });

            if (!fieldsToSet.isEmpty()) {
                updateMultiQuery.addActions(new SetAction(fieldsToSet));
            }
            if (!fieldsToUnset.isEmpty()) {
                updateMultiQuery.addActions(new UnsetAction(fieldsToUnset.toArray(String[]::new)));
            }

            return updateMultiQuery.getFinalUpdate();
        } catch (InvalidCreateOperationException e) {
            throw new CollectInternalServerSideException("Cannot build query", e);
        }
    }

    private static Query buildSelectorQuery(CollectJsonMetadataLine jsonMetadataLine)
        throws InvalidCreateOperationException {
        if (jsonMetadataLine.getFile() != null) {
            return QueryHelper.term(VitamFieldsHelper.uploadPath(), jsonMetadataLine.getFile());
        }

        Map<String, ValueNode> selectorEntries = jsonMetadataLine.getSelector().getEntries();
        if (selectorEntries.size() == 1) {
            Map.Entry<String, ValueNode> selector = selectorEntries.entrySet().iterator().next();
            return getSelectorQuery(selector);
        }
        BooleanQuery andQuery = QueryHelper.and();
        for (Map.Entry<String, ValueNode> selector : selectorEntries.entrySet()) {
            andQuery.add(getSelectorQuery(selector));
        }
        return andQuery;
    }

    private static CompareQuery getSelectorQuery(Map.Entry<String, ValueNode> selector)
        throws InvalidCreateOperationException {
        ValueNode value = selector.getValue();
        switch (selector.getValue().getNodeType()) {
            case STRING:
                return QueryHelper.eq(selector.getKey(), value.textValue());
            case BOOLEAN:
                return QueryHelper.eq(selector.getKey(), value.booleanValue());
            case NUMBER:
                return QueryHelper.eq(selector.getKey(), value.longValue());
            default:
                throw new IllegalStateException("Unexpected selector value type: " + selector.getValue().getNodeType());
        }
    }

    private ArchiveUnitModel createAttachmentUnit(String transactionId, String title, String unitUp) {
        ArchiveUnitModel unit = MetadataHelper.createUnit(transactionId, LevelType.SERIES, null, title, null);
        ManagementModel managementModel = new ManagementModel();
        UpdateOperationType updateOperationType = new UpdateOperationType();
        updateOperationType.setSystemId(unitUp);
        managementModel.setUpdateOperationType(updateOperationType);
        unit.setManagement(managementModel);
        return unit;
    }

    private Map<String, String> findAttachmentUnitIdsBySystemId(String transactionId, ProjectModel projectModel)
        throws InvalidCreateOperationException, CollectInternalException, InvalidParseOperationException {

        Set<String> unitsToFetchBySystemId = new HashSet<>();
        if (projectModel.getUnitUp() != null) {
            unitsToFetchBySystemId.add(projectModel.getUnitUp());
        }
        if (projectModel.getUnitUps() != null) {
            projectModel.getUnitUps()
                .forEach(unitUp -> unitsToFetchBySystemId.add(unitUp.getUnitUp()));
        }
        if (unitsToFetchBySystemId.isEmpty()) {
            return Collections.emptyMap();
        }

        SelectMultiQuery query = new SelectMultiQuery();
        query.addQueries(QueryHelper.in(SYSTEM_ID_FIELD_PATH, unitsToFetchBySystemId.toArray(new String[0])));
        query.addUsedProjection(VitamFieldsHelper.id(), SYSTEM_ID_FIELD_PATH);
        RequestResponseOK<JsonNode> units = metadataRepository.selectUnits(query.getFinalSelect(), transactionId);

        return units.getResults().stream()
            .collect(Collectors.toMap(
                this::findSystemId,
                attachmentUnit -> attachmentUnit.get(VitamFieldsHelper.id()).asText()
            ));
    }

    Map<String, String> prepareAttachmentUnits(ProjectModel projectModel, String transactionId)
        throws CollectInternalException {
        try {

            // Get existing virtual attachment units
            Map<String, String> attachmentUnitsBySystemId =
                findAttachmentUnitIdsBySystemId(transactionId, projectModel);

            // Create virtual attachment units in needed
            List<ArchiveUnitModel> unitsToCreate = new ArrayList<>();
            if (projectModel.getUnitUp() != null) {
                if (!attachmentUnitsBySystemId.containsKey(projectModel.getUnitUp())) {
                    ArchiveUnitModel unit =
                        createAttachmentUnit(transactionId, STATIC_ATTACHMENT, projectModel.getUnitUp());
                    unitsToCreate.add(unit);
                    attachmentUnitsBySystemId.put(projectModel.getUnitUp(), unit.getId());
                }
            }

            if (projectModel.getUnitUps() != null) {
                for (MetadataUnitUp unitUp : projectModel.getUnitUps()) {
                    if (!attachmentUnitsBySystemId.containsKey(unitUp.getUnitUp())) {
                        String unitTitle = DYNAMIC_ATTACHEMENT + "_" + unitUp.getUnitUp();
                        ArchiveUnitModel unit = createAttachmentUnit(transactionId, unitTitle, unitUp.getUnitUp());
                        unitsToCreate.add(unit);
                        attachmentUnitsBySystemId.put(unitUp.getUnitUp(), unit.getId());
                    }
                }
            }

            if (!unitsToCreate.isEmpty()) {
                // FIXME : Attachment units should be created on transaction initialization to avoid concurrent creation
                ObjectMapper objectMapper = getSerializationObjectMapper();
                metadataRepository.saveArchiveUnits(
                    unitsToCreate.stream().map(unit -> objectMapper.convertValue(unit, ObjectNode.class))
                        .collect(Collectors.toList()));
            }

            return attachmentUnitsBySystemId;

        } catch (InvalidCreateOperationException | InvalidParseOperationException e) {
            throw new CollectInternalException(e);
        }
    }

    private String findSystemId(JsonNode attachmentUnit) {
        JsonNode value = attachmentUnit;
        for (String field : SYSTEM_ID_FIELD_PATH.split("\\.")) {
            value = value.get(field);
        }
        return value.asText();
    }

    private void resetQuery(JsonNode result, JsonNode queryDsl) {
        if (result != null && result.has(RequestResponseOK.TAG_CONTEXT)) {
            ((ObjectNode) result).set(RequestResponseOK.TAG_CONTEXT, queryDsl);
        }
    }

    public JsonNode selectUnitsWithInheritedRules(String transactionId, JsonNode queryDsl)
        throws InvalidParseOperationException {
        JsonNode result;
        LOGGER.debug("DEBUG: start selectUnitsWithInheritedRules {}", queryDsl);

        result = metadataRepository.selectUnitsWithInheritedRules(queryDsl, transactionId);

        resetQuery(result, queryDsl);
        LOGGER.debug("DEBUG: end selectUnitsWithInheritedRules {}");
        return result;
    }
}

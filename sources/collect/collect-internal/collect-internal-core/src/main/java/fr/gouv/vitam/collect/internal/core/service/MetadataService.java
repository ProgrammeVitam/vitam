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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Iterators;
import fr.gouv.culture.archivesdefrance.seda.v2.LevelType;
import fr.gouv.culture.archivesdefrance.seda.v2.UpdateOperationType;
import fr.gouv.vitam.collect.common.dto.MetadataUnitUp;
import fr.gouv.vitam.collect.common.exception.CollectInternalException;
import fr.gouv.vitam.collect.internal.core.common.ProjectModel;
import fr.gouv.vitam.collect.internal.core.common.TransactionModel;
import fr.gouv.vitam.collect.internal.core.helpers.CsvHelper;
import fr.gouv.vitam.collect.internal.core.helpers.JsonHelper;
import fr.gouv.vitam.collect.internal.core.helpers.MetadataHelper;
import fr.gouv.vitam.collect.internal.core.repository.MetadataRepository;
import fr.gouv.vitam.collect.internal.core.repository.ProjectRepository;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.query.action.SetAction;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.database.utils.ScrollSpliterator;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.mapping.mapper.VitamObjectMapper;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.UnitType;
import fr.gouv.vitam.common.model.VitamConstants;
import fr.gouv.vitam.common.model.unit.ArchiveUnitModel;
import fr.gouv.vitam.common.model.unit.ManagementModel;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.worker.core.distribution.JsonLineGenericIterator;
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static fr.gouv.vitam.collect.internal.core.helpers.MetadataHelper.DYNAMIC_ATTACHEMENT;
import static fr.gouv.vitam.collect.internal.core.helpers.MetadataHelper.STATIC_ATTACHMENT;
import static fr.gouv.vitam.common.mapping.mapper.VitamObjectMapper.buildSerializationObjectMapper;

public class MetadataService {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(MetadataService.class);

    private static final String TITLE = "Title";

    private final MetadataRepository metadataRepository;

    private final ProjectRepository projectRepository;

    public MetadataService(MetadataRepository metadataRepository, ProjectRepository projectRepository) {
        this.metadataRepository = metadataRepository;
        this.projectRepository = projectRepository;
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
            VitamObjectMapper.buildDeserializationObjectMapper().convertValue(unit, ArchiveUnitModel.class);
        String unitId = GUIDFactory.newUnitGUID(VitamThreadUtils.getVitamSession().getTenantId()).getId();
        unitModel.setId(unitId);
        unitModel.setOpi(transactionModel.getId());
        unitModel.setUnitType(UnitType.INGEST.name());

        Optional<ProjectModel> project = projectRepository.findProjectById(transactionModel.getProjectId());

        if (project.isEmpty()) {
            throw new CollectInternalException("Cannot find project");
        }

        ProjectModel projectModel = project.get();
        HashMap<String, String> attachmentUnits = prepareAttachmentUnits(projectModel, transactionModel.getId());

        if (projectModel.getUnitUp() != null) {
            unitModel.setUnitups(Collections.singletonList(attachmentUnits.get(STATIC_ATTACHMENT)));
        }
        if (projectModel.getUnitUps() != null) {
            Map.Entry<String, String> attachment =
                MetadataHelper.findUnitParent(((ObjectNode) unit).put(VitamFieldsHelper.id(), unitId),
                    projectModel.getUnitUps(), attachmentUnits);
            unitModel.setUnitups(Collections.singletonList(attachment.getValue()));
        }

        JsonNode jsonNode = buildSerializationObjectMapper().convertValue(unitModel, JsonNode.class);
        metadataRepository.saveArchiveUnit((ObjectNode) jsonNode);
        return jsonNode;
    }

    public List<JsonNode> selectUnits(JsonNode queryDsl, String transactionId) throws CollectInternalException {
        return metadataRepository.selectUnits(queryDsl, transactionId).getResults();
    }

    public void updateUnits(TransactionModel transaction, InputStream is) throws CollectInternalException {
        String requestId = VitamThreadUtils.getVitamSession().getRequestId();
        File file = PropertiesUtils.fileFromTmpFolder("metadata_" + requestId + VitamConstants.JSONL_EXTENSION);
        try {
            CsvHelper.convertCsvToMetadataFile(is, file);
            try (InputStream metadataInputStream = new FileInputStream(file)) {
                updateUnitsWithMetadataFile(transaction.getId(), metadataInputStream);
            }
        } catch (IOException e) {
            throw new CollectInternalException(e);
        } finally {
            FileUtils.deleteQuietly(file);
        }
    }


    void updateUnitsWithMetadataFile(String transactionId, InputStream is)
        throws CollectInternalException, IOException {
        Map<String, String> unitsByURI = buildGraphFromExistingUnits(transactionId);
        updateUnitsMetadata(is, unitsByURI);
    }

    private void updateUnitsMetadata(InputStream is, Map<String, String> unitsByURI) throws CollectInternalException {
        JsonLineGenericIterator<JsonLineModel> metadata = new JsonLineGenericIterator<>(is, new TypeReference<>() {
        });
        Iterator<List<JsonLineModel>> iterator = Iterators.partition(metadata, 1000);
        boolean updated = false;
        while (iterator.hasNext()) {
            try {
                updated = true;
                List<JsonLineModel> next = iterator.next();
                Map<String, JsonNode> unitsIdByURI =
                    next.stream().map(e -> new AbstractMap.SimpleEntry<>(e.getId(), e.getParams()))
                        .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
                // update unit with list
                final List<JsonNode> updateMultiQueries = convertToQuery(unitsIdByURI, unitsByURI);
                final RequestResponse<JsonNode> result = metadataRepository.atomicBulkUpdate(updateMultiQueries);

                final boolean thereIsError =
                    ((RequestResponseOK<JsonNode>) result).getResults().stream().map(e -> e.get("$results"))
                        .map(e -> e.get(0)).map(e -> e.get("#status")).map(JsonNode::asText)
                        .anyMatch(e -> !e.equals("OK"));

                if (thereIsError) {
                    throw new CollectInternalException("Error when trying to update units metadata");
                }
            } catch (InvalidCreateOperationException | InvalidParseOperationException e) {
                LOGGER.error("Could not create update query", e);
                throw new CollectInternalException(e);
            }
        }

        if (!updated) {
            throw new CollectInternalException("no update data found !");
        }
    }

    private Map<String, String> buildGraphFromExistingUnits(String transactionId) throws CollectInternalException {
        try {
            SelectMultiQuery select = new SelectMultiQuery();
            select.addQueries(QueryHelper.eq(VitamFieldsHelper.initialOperation(), transactionId));
            final ObjectNode projection = JsonHandler.createObjectNode();
            projection.set("$fields", JsonHandler.createObjectNode().put(VitamFieldsHelper.id(), 1).put(TITLE, 1)
                .put(VitamFieldsHelper.unitups(), 1).put(VitamFieldsHelper.allunitups(), 1));
            select.addProjection(projection);
            final ScrollSpliterator<JsonNode> unitScrollSpliterator =
                metadataRepository.selectUnits(select, transactionId);

            final List<JsonNode> units = new ArrayList<>();
            unitScrollSpliterator.forEachRemaining(units::add);
            units.sort(Comparator.comparingInt(a -> a.get(VitamFieldsHelper.allunitups()).size()));

            BiMap<String, String> hash = HashBiMap.create();
            units.forEach(u -> {
                final ArrayNode parentUnit = (ArrayNode) u.get(VitamFieldsHelper.unitups());
                if (parentUnit.size() > 0) {
                    hash.put(hash.inverse().getOrDefault(parentUnit.get(0).asText(), parentUnit.get(0).asText()) + "/" +
                        u.get(TITLE).asText(), u.get(VitamFieldsHelper.id()).asText());
                } else {
                    hash.put(u.get(TITLE).asText(), u.get(VitamFieldsHelper.id()).asText());
                }
            });
            return hash;
        } catch (Exception e) {
            throw new CollectInternalException(e);
        }
    }

    private List<JsonNode> convertToQuery(Map<String, JsonNode> unitsByURI, Map<String, String> unitsIdByURI)
        throws InvalidCreateOperationException, InvalidParseOperationException, CollectInternalException {
        List<JsonNode> listQueries = new ArrayList<>();
        for (Map.Entry<String, JsonNode> unit : unitsByURI.entrySet()) {
            Optional<String> first = unitsIdByURI.keySet().stream().filter(e -> e.endsWith(unit.getKey())).findFirst();
            if (first.isEmpty()) {
                throw new CollectInternalException("Cannot find unit with path " + unit.getKey());
            }
            String unitId = unitsIdByURI.get(first.get());
            UpdateMultiQuery query = new UpdateMultiQuery();
            query.addRoots(unitId);
            final Map<String, JsonNode> metadataMap = JsonHelper.jsonToMap(unit.getValue());
            query.addActions(new SetAction(metadataMap));
            listQueries.add(query.getFinalUpdate());
        }
        return listQueries;
    }

    private ArchiveUnitModel createAttachmentUnit(String transactionId, String title, String unitUp) {
        ArchiveUnitModel unit = MetadataHelper.createUnit(transactionId, LevelType.SERIES, title, null);
        ManagementModel managementModel = new ManagementModel();
        UpdateOperationType updateOperationType = new UpdateOperationType();
        updateOperationType.setSystemId(unitUp);
        managementModel.setUpdateOperationType(updateOperationType);
        unit.setManagement(managementModel);
        return unit;
    }

    private JsonNode findUnitByTitle(String transactionId, String title)
        throws InvalidCreateOperationException, CollectInternalException {
        SelectMultiQuery query = new SelectMultiQuery();
        // FIXME : exact search on analyzed field
        // TODO CHANGE IT WITH SYSTEMID
        query.addQueries(QueryHelper.match(MetadataService.TITLE, title));
        RequestResponseOK<JsonNode> units = metadataRepository.selectUnits(query.getFinalSelect(), transactionId);
        return units.getFirstResult();
    }

    HashMap<String, String> prepareAttachmentUnits(ProjectModel projectModel, String transactionId)
        throws CollectInternalException {
        List<ArchiveUnitModel> units = new ArrayList<>();
        try {
            HashMap<String, String> savedGuidUnits = new HashMap<>();
            if (projectModel.getUnitUp() != null) {
                JsonNode unitByTitle = findUnitByTitle(transactionId, STATIC_ATTACHMENT);
                if (unitByTitle == null) {
                    ArchiveUnitModel unit =
                        createAttachmentUnit(transactionId, STATIC_ATTACHMENT, projectModel.getUnitUp());
                    units.add(unit);
                    savedGuidUnits.put(STATIC_ATTACHMENT, unit.getId());
                } else {
                    savedGuidUnits.put(STATIC_ATTACHMENT, unitByTitle.get(VitamFieldsHelper.id()).asText());
                }
            }

            if (projectModel.getUnitUps() != null) {
                for (MetadataUnitUp unitUp : projectModel.getUnitUps()) {
                    final String unitTitle = String.format("%s_%s", DYNAMIC_ATTACHEMENT, unitUp.getUnitUp());
                    if (savedGuidUnits.get(unitTitle) == null) {
                        JsonNode unitByTitle = findUnitByTitle(transactionId, unitTitle);
                        if (unitByTitle == null) {
                            ArchiveUnitModel unit = createAttachmentUnit(transactionId, unitTitle, unitUp.getUnitUp());
                            units.add(unit);
                            savedGuidUnits.put(unitTitle, unit.getId());
                        } else {
                            savedGuidUnits.put(unitTitle, unitByTitle.get(VitamFieldsHelper.id()).asText());
                        }
                    }
                }
            }
            ObjectMapper objectMapper = buildSerializationObjectMapper();

            metadataRepository.saveArchiveUnits(
                units.stream().map(unit -> objectMapper.convertValue(unit, ObjectNode.class))
                    .collect(Collectors.toList()));

            return savedGuidUnits;
        } catch (InvalidCreateOperationException e) {
            throw new CollectInternalException(e);
        }
    }
}

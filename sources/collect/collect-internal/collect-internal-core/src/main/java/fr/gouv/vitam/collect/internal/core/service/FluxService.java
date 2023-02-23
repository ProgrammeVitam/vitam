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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Iterators;
import fr.gouv.culture.archivesdefrance.seda.v2.UpdateOperationType;
import fr.gouv.vitam.collect.common.dto.FileInfoDto;
import fr.gouv.vitam.collect.common.dto.ObjectDto;
import fr.gouv.vitam.collect.common.dto.ProjectDto;
import fr.gouv.vitam.collect.common.dto.TransactionDto;
import fr.gouv.vitam.collect.common.exception.CollectInternalException;
import fr.gouv.vitam.collect.internal.core.common.CollectUnitModel;
import fr.gouv.vitam.collect.internal.core.common.DescriptionLevel;
import fr.gouv.vitam.collect.internal.core.helpers.CsvMetadataMapper;
import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.LocalDateUtil;
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
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.DataObjectVersionType;
import fr.gouv.vitam.common.model.objectgroup.DbObjectGroupModel;
import fr.gouv.vitam.common.model.unit.ManagementModel;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.storage.compress.ArchiveEntryInputStream;
import fr.gouv.vitam.common.storage.compress.VitamArchiveStreamFactory;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.model.RequestResponseOK.TAG_RESULTS;

public class FluxService {

    private static final String OPI = "#opi";
    private static final String ID = "#id";
    private static final String MGT = "#management";
    private static final String TITLE = "Title";
    private static final String DESCRIPTION_LEVEL = "DescriptionLevel";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(FluxService.class);
    private static final String ATTACHEMENT_AU = "AttachementAu";
    private static final String DUMMY_ARCHIVE_UNIT_TITLE = "AU de Rattachement";
    private static final String UNIT_TYPE = "#unitType";
    private static final String INGEST = "INGEST";

    static final String METADATA_CSV_FILE = "metadata.csv";

    private final CollectService collectService;
    private final MetadataService metadataService;

    public FluxService(CollectService collectService, MetadataService metadataService) {
        this.collectService = collectService;
        this.metadataService = metadataService;
    }

    public void processStream(InputStream inputStreamObject, TransactionDto transactionDto, ProjectDto projectDto)
        throws CollectInternalException {
        final InputStream inputStreamClosable = StreamUtils.getRemainingReadOnCloseInputStream(inputStreamObject);
        try (final ArchiveInputStream archiveInputStream = new VitamArchiveStreamFactory().createArchiveInputStream(
            CommonMediaType.ZIP_TYPE, inputStreamClosable)) {
            ArchiveEntry entry;
            boolean isEmpty = true;
            HashMap<String, String> savedGuidUnits = new HashMap<>();
            boolean isAttachmentAuExist = isAttachmentAuExist(projectDto, transactionDto.getId(), savedGuidUnits);
            boolean isExtraMetadataExist = false;
            // create entryInputStream to resolve the stream closed problem
            final ArchiveEntryInputStream entryInputStream = new ArchiveEntryInputStream(archiveInputStream);
            while ((entry = archiveInputStream.getNextEntry()) != null) {
                if (archiveInputStream.canReadEntryData(entry)) {
                    if (entry.getName().equals(File.separator)) {
                        continue;
                    }
                    String fileName = Paths.get(entry.getName()).getFileName().toString();
                    if (!entry.isDirectory() && fileName.equals(METADATA_CSV_FILE)) {
                        // save file in workspace
                        collectService.pushStreamToWorkspace(transactionDto.getId(), entryInputStream,
                            METADATA_CSV_FILE);
                        isExtraMetadataExist = true;
                    } else {
                        convertEntryToAu(transactionDto.getId(), entry, isAttachmentAuExist, entryInputStream,
                            savedGuidUnits,
                            fileName);
                    }
                    isEmpty = false;
                }
                entryInputStream.setClosed(false);
            }
            if (isEmpty) {
                throw new CollectInternalException("File is empty");
            }

            if (isExtraMetadataExist) {
                try (final InputStream is = collectService.getInputStreamFromWorkspace(transactionDto.getId(),
                    METADATA_CSV_FILE)) {
                    updateUnits(transactionDto.getId(), is, isAttachmentAuExist);
                }
            }

        } catch (IOException | ArchiveException e) {
            LOGGER.error("An error occurs when try to upload the ZIP: {}", e);
            throw new CollectInternalException("An error occurs when try to upload the ZIP: {}");
        }
    }

    public void updateUnits(String transactionId, InputStream is, boolean isAttachmentAuExist)
        throws CollectInternalException, IOException {
        Map<String, String> unitsByURI = buildGraphFromExistingUnits(transactionId);
        populateMetadata(is, unitsByURI, isAttachmentAuExist);
    }

    private Map<String, String> buildGraphFromExistingUnits(String transactionId) throws CollectInternalException {
        try {
            SelectMultiQuery select = buildQuery(transactionId);
            final ScrollSpliterator<JsonNode> unitScrollSpliterator =
                metadataService.selectUnits(select, transactionId);
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

    private SelectMultiQuery buildQuery(String transactionId) throws InvalidCreateOperationException {
        SelectMultiQuery select = new SelectMultiQuery();
        select.addQueries(QueryHelper.eq(VitamFieldsHelper.initialOperation(), transactionId));
        final ObjectNode projection = JsonHandler.createObjectNode();
        projection.set("$fields", JsonHandler.createObjectNode().put(VitamFieldsHelper.id(), 1).put(TITLE, 1)
            .put(VitamFieldsHelper.unitups(), 1).put(VitamFieldsHelper.allunitups(), 1));
        select.addProjection(projection);
        return select;
    }

    private void populateMetadata(InputStream is, Map<String, String> unitsByURI, boolean isAttachmentAuExist)
        throws IOException, CollectInternalException {
        try (final InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
            CSVParser parser = new CSVParser(reader,
                CSVFormat.DEFAULT.withHeader().withTrim().withIgnoreEmptyLines(false).withDelimiter(';'))) {
            List<String> headerNames = parser.getHeaderNames();

            boolean updated = false;

            final Iterator<Map<String, JsonNode>> iterator =
                IteratorUtils.transformedIterator(Iterators.partition(parser.iterator(), 1000),
                    list -> list.stream().map(e -> CsvMetadataMapper.map(e, headerNames))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

            while (iterator.hasNext()) {
                try {
                    updated = true;
                    Map<String, JsonNode> unitsIdByURI = iterator.next();
                    // update unit with list
                    final List<JsonNode> updateMultiQueries =
                        convertToQuery(unitsIdByURI, unitsByURI, isAttachmentAuExist);
                    final RequestResponse<JsonNode> result = metadataService.atomicBulkUpdate(updateMultiQueries);

                    final boolean thereIsError =
                        ((RequestResponseOK<JsonNode>) result).getResults().stream().map(e -> e.get("$results"))
                            .map(e -> e.get(0))
                            .map(e -> e.get("#status"))
                            .map(JsonNode::asText).anyMatch(e -> !e.equals(
                                "OK"));

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
    }

    private List<JsonNode> convertToQuery(Map<String, JsonNode> unitsByURI, Map<String, String> unitsIdByURI,
        boolean isAttachmentAuExist)
        throws InvalidCreateOperationException, InvalidParseOperationException, CollectInternalException {
        List<JsonNode> listQueries = new ArrayList<>();
        for (Map.Entry<String, JsonNode> unit : unitsByURI.entrySet()) {
            String path = (isAttachmentAuExist) ? DUMMY_ARCHIVE_UNIT_TITLE + "/" + unit.getKey() : unit.getKey();
            String unitId = unitsIdByURI.get(path);
            if (unitId == null) {
                throw new CollectInternalException("Cannot find unit with path " + path);
            }
            UpdateMultiQuery query = new UpdateMultiQuery();
            query.addRoots(unitId);
            final Map<String, JsonNode> metadataMap = jsonToMap(unit.getValue());
            query.addActions(new SetAction(metadataMap));
            listQueries.add(query.getFinalUpdate());
        }
        return listQueries;
    }

    private Map<String, JsonNode> jsonToMap(JsonNode unit) {
        Map<String, JsonNode> map = new HashMap<>();
        unit.fieldNames().forEachRemaining(key -> map.put(key, unit.get(key)));
        return map;
    }

    private boolean isAttachmentAuExist(ProjectDto projectDto, String transactionId,
        HashMap<String, String> savedGuidUnits)
        throws CollectInternalException {
        boolean isAttachmentAuExist = false;
        if (projectDto.getUnitUp() != null) {
            JsonNode savedUnit = uploadArchiveUnit(transactionId, DescriptionLevel.SERIES.getValue(),
                DUMMY_ARCHIVE_UNIT_TITLE, null, projectDto.getUnitUp());
            savedGuidUnits.put(ATTACHEMENT_AU, savedUnit.get(ID).asText());
            isAttachmentAuExist = true;
        }
        return isAttachmentAuExist;
    }

    private void convertEntryToAu(String transactionId, ArchiveEntry entry, boolean isAttachmentAuExist,
        ArchiveEntryInputStream entryInputStream, HashMap<String, String> savedGuidUnits, String fileName)
        throws CollectInternalException {
        Map.Entry<String, String> unitParentEntry;
        JsonNode unitRecord;
        if (entry.isDirectory()) {
            unitParentEntry =
                getUnitParentByPath(savedGuidUnits, StringUtils.chop(entry.getName()), isAttachmentAuExist);
            unitRecord =
                uploadArchiveUnit(transactionId, DescriptionLevel.RECORD_GRP.getValue(), fileName,
                    unitParentEntry != null ? unitParentEntry.getValue() : null, null);
            savedGuidUnits.put(StringUtils.chop(entry.getName()), unitRecord.get(ID).asText());
        } else {
            unitParentEntry = getUnitParentByPath(savedGuidUnits, entry.getName(), isAttachmentAuExist);
            JsonNode unitItem =
                uploadArchiveUnit(transactionId, DescriptionLevel.ITEM.getValue(), fileName,
                    unitParentEntry != null ? unitParentEntry.getValue() : null, null);
            uploadObjectGroup(unitItem.get(ID).asText(), fileName);
            uploadBinary(unitItem.get(ID).asText(), entryInputStream);
        }
    }

    public ObjectNode uploadArchiveUnit(String transactionId, String descriptionLevel, String title, String unitParent,
        String attachementUnitId) throws CollectInternalException {

        ObjectNode unit = JsonHandler.createObjectNode();
        unit.put(ID, GUIDFactory.newUnitGUID(VitamThreadUtils.getVitamSession().getTenantId()).getId());
        unit.put(OPI, transactionId);
        unit.put(UNIT_TYPE, INGEST);
        if (null != attachementUnitId) {
            ManagementModel managementModel = new ManagementModel();
            UpdateOperationType updateOperationType = new UpdateOperationType();
            updateOperationType.setSystemId(attachementUnitId);
            managementModel.setUpdateOperationType(updateOperationType);
            try {
                unit.replace(MGT, JsonHandler.toJsonNode(managementModel));
            } catch (InvalidParseOperationException e) {
                throw new CollectInternalException("Error while trying to add management to unit");
            }
        } else {
            unit.replace(MGT, JsonHandler.createObjectNode());
        }
        unit.put(TITLE, title);
        unit.put(DESCRIPTION_LEVEL, descriptionLevel);
        if (unitParent != null) {
            unit.set(VitamFieldsHelper.unitups(), JsonHandler.createArrayNode().add(unitParent));
        }
        JsonNode savedUnitJsonNode = metadataService.saveArchiveUnit(unit);
        if (savedUnitJsonNode == null) {
            throw new CollectInternalException("Error while trying to save units");
        }
        return unit;
    }

    public Map.Entry<String, String> getUnitParentByPath(Map<String, String> unitMap, String entryName,
        boolean isAttachmentAuExist) {
        int sepPos = entryName.lastIndexOf(File.separator);
        if (sepPos == -1) {
            if (isAttachmentAuExist) {
                return new AbstractMap.SimpleEntry<>(null, unitMap.get(ATTACHEMENT_AU));
            } else {
                return null;
            }
        }
        return new AbstractMap.SimpleEntry<>(entryName.substring(0, sepPos),
            unitMap.get(entryName.substring(0, sepPos)));
    }

    public void uploadObjectGroup(String unitId, String fileName) throws CollectInternalException {
        try {
            FileInfoDto fileInfoDto =
                new FileInfoDto(fileName, LocalDateUtil.getFormattedDateForMongo(LocalDateTime.now()));
            ObjectDto objectDto =
                new ObjectDto(GUIDFactory.newObjectGUID(ParameterHelper.getTenantParameter()).getId(), fileInfoDto);
            JsonNode unitResponse = metadataService.selectUnitById(unitId);
            CollectUnitModel unitModel =
                JsonHandler.getFromJsonNode(unitResponse.get(TAG_RESULTS).get(0), CollectUnitModel.class);
            collectService.updateOrSaveObjectGroup(unitModel, DataObjectVersionType.BINARY_MASTER, 1, objectDto);

        } catch (InvalidParseOperationException e) {
            throw new CollectInternalException(e);
        }
    }


    private void uploadBinary(String unitId, InputStream uploadedInputStream) throws CollectInternalException {
        try {
            JsonNode unitResponse = metadataService.selectUnitById(unitId);
            CollectUnitModel unitModel =
                JsonHandler.getFromJsonNode(unitResponse.get(TAG_RESULTS).get(0), CollectUnitModel.class);
            if (unitModel.getOg() == null) {
                LOGGER.debug("Cannot found any got attached to unit with id({}))", unitModel.getId());
                throw new IllegalArgumentException(
                    "Cannot found any object attached to unit with id(" + unitModel.getId() + ")");
            }
            final RequestResponseOK<JsonNode> result =
                RequestResponseOK.getFromJsonNode(metadataService.selectObjectGroupById(unitModel.getOg(), true));
            final DbObjectGroupModel dbObjectGroupModel =
                JsonHandler.getFromJsonNode(result.getResults().get(0), DbObjectGroupModel.class);
            collectService.addBinaryInfoToQualifier(dbObjectGroupModel, DataObjectVersionType.BINARY_MASTER, 1,
                uploadedInputStream);
        } catch (InvalidParseOperationException e) {
            throw new CollectInternalException(e);
        }
    }
}

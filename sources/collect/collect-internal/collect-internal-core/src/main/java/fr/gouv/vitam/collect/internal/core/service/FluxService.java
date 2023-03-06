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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Iterators;
import fr.gouv.culture.archivesdefrance.seda.v2.LevelType;
import fr.gouv.vitam.collect.common.exception.CollectInternalException;
import fr.gouv.vitam.collect.internal.core.common.DescriptionLevel;
import fr.gouv.vitam.collect.internal.core.common.ProjectModel;
import fr.gouv.vitam.collect.internal.core.common.TransactionModel;
import fr.gouv.vitam.collect.internal.core.helpers.CsvHelper;
import fr.gouv.vitam.collect.internal.core.helpers.MetadataHelper;
import fr.gouv.vitam.collect.internal.core.repository.MetadataRepository;
import fr.gouv.vitam.collect.internal.core.repository.ProjectRepository;
import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.format.identification.model.FormatIdentifierResponse;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.MetadataType;
import fr.gouv.vitam.common.model.VitamConstants;
import fr.gouv.vitam.common.model.objectgroup.ObjectGroupResponse;
import fr.gouv.vitam.common.model.unit.ArchiveUnitModel;
import fr.gouv.vitam.common.storage.compress.ArchiveEntryInputStream;
import fr.gouv.vitam.common.storage.compress.VitamArchiveStreamFactory;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.worker.core.distribution.JsonLineGenericIterator;
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import fr.gouv.vitam.worker.core.distribution.JsonLineWriter;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.input.CountingInputStream;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.Strings;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static fr.gouv.vitam.collect.internal.core.helpers.MetadataHelper.STATIC_ATTACHMENT;
import static fr.gouv.vitam.collect.internal.core.helpers.MetadataHelper.findUnitParent;
import static fr.gouv.vitam.common.SedaConstants.PREFIX_UP;
import static fr.gouv.vitam.common.mapping.mapper.VitamObjectMapper.buildSerializationObjectMapper;
import static fr.gouv.vitam.common.model.IngestWorkflowConstants.CONTENT_FOLDER;

public class FluxService {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(FluxService.class);

    private static final int BULK_SIZE = 1000;
    private final static String METADATA = "Metadata";
    private static final String TITLE = "Title";
    static final String METADATA_CSV_FILE = "metadata.csv";

    private final CollectService collectService;
    private final MetadataService metadataService;
    private final ProjectRepository projectRepository;
    private final MetadataRepository metadataRepository;

    public FluxService(CollectService collectService, MetadataService metadataService,
        ProjectRepository projectRepository, MetadataRepository metadataRepository) {
        this.collectService = collectService;
        this.metadataService = metadataService;
        this.projectRepository = projectRepository;
        this.metadataRepository = metadataRepository;
    }

    public void processStream(InputStream inputStreamObject, TransactionModel transactionModel)
        throws CollectInternalException {

        Optional<ProjectModel> projectById = projectRepository.findProjectById(transactionModel.getProjectId());
        if (projectById.isEmpty()) {
            throw new CollectInternalException("Project not found");
        }
        ProjectModel projectModel = projectById.get();

        final InputStream inputStreamClosable = StreamUtils.getRemainingReadOnCloseInputStream(inputStreamObject);
        try (final ArchiveInputStream archiveInputStream = new VitamArchiveStreamFactory().createArchiveInputStream(
            CommonMediaType.ZIP_TYPE, inputStreamClosable)) {
            ArchiveEntry entry;
            boolean isEmpty = true;
            HashMap<String, String> attachmentGUID =
                metadataService.prepareAttachmentUnits(projectModel, transactionModel.getId());
            boolean isExtraMetadataExist = false;
            // create entryInputStream to resolve the stream closed problem
            final ArchiveEntryInputStream entryInputStream = new ArchiveEntryInputStream(archiveInputStream);
            int maxLevel = -1;
            Map<String, String> unitIds = new HashMap<>();
            while ((entry = archiveInputStream.getNextEntry()) != null) {
                if (archiveInputStream.canReadEntryData(entry)) {
                    String path = entry.getName().replaceAll("/$", "");
                    if (Strings.isEmpty(path)) {
                        continue;
                    }
                    String fileName = Paths.get(path).getFileName().toString();
                    if (!entry.isDirectory() && fileName.equals(METADATA_CSV_FILE)) {
                        // save file in workspace
                        collectService.pushStreamToWorkspace(transactionModel.getId(), entryInputStream,
                            METADATA_CSV_FILE);
                        isExtraMetadataExist = true;
                    } else {
                        maxLevel =
                            createMetadata(transactionModel, entry, attachmentGUID, entryInputStream, maxLevel, unitIds,
                                path, fileName, projectModel.getUnitUp() != null);
                    }
                    isEmpty = false;
                }
                entryInputStream.setClosed(false);
            }

            if (isEmpty) {
                throw new CollectInternalException("File is empty");
            }

            if (isExtraMetadataExist) {
                File metadataFile = PropertiesUtils.fileFromTmpFolder(
                    METADATA + "_" + transactionModel.getId() + VitamConstants.JSONL_EXTENSION);
                try (InputStream is = collectService.getInputStreamFromWorkspace(transactionModel.getId(),
                    METADATA_CSV_FILE)) {
                    CsvHelper.convertCsvToMetadataFile(is, metadataFile);
                }
            }

            Map<String, String> unitUps =
                (isExtraMetadataExist) ? findUnitUps(projectModel, transactionModel, attachmentGUID) : new HashMap<>();

            bulkWriteUnits(maxLevel, unitUps, transactionModel.getId());

            bulkWriteObjectGroups(transactionModel.getId());

            cleanTemporaryFiles(maxLevel, transactionModel.getId());

            if (isExtraMetadataExist) {
                File metadataFile = PropertiesUtils.fileFromTmpFolder(
                    METADATA + "_" + transactionModel.getId() + VitamConstants.JSONL_EXTENSION);
                try (InputStream is = new FileInputStream(metadataFile)) {
                    metadataService.updateUnitsWithMetadataFile(transactionModel.getId(), is);
                } finally {
                    FileUtils.deleteQuietly(metadataFile);
                }
            }
        } catch (IOException | ArchiveException e) {
            LOGGER.error("An error occurs when try to upload the ZIP: {}", e);
            throw new CollectInternalException("An error occurs when try to upload the ZIP: {}");
        } catch (InvalidParseOperationException e) {
            throw new CollectInternalException(e);
        }
    }

    private void cleanTemporaryFiles(int maxLevel, String transactionId) {
        File ogFile = PropertiesUtils.fileFromTmpFolder(
            MetadataType.OBJECTGROUP.getName() + "_" + transactionId + VitamConstants.JSONL_EXTENSION);
        FileUtils.deleteQuietly(ogFile);
        for (int level = 0; level < maxLevel; level++) {
            File file = PropertiesUtils.fileFromTmpFolder(
                MetadataType.UNIT.getName() + "_" + level + "_" + transactionId + VitamConstants.JSONL_EXTENSION);
            FileUtils.deleteQuietly(file);
        }
    }

    private Map<String, String> findUnitUps(ProjectModel projectModel, TransactionModel transactionModel,
        HashMap<String, String> attachmentGUID) throws FileNotFoundException {
        if (projectModel.getUnitUps() != null) {
            File metadataFile = PropertiesUtils.fileFromTmpFolder(
                METADATA + "_" + transactionModel.getId() + VitamConstants.JSONL_EXTENSION);
            JsonLineGenericIterator<JsonLineModel> iterator =
                new JsonLineGenericIterator<>(new FileInputStream(metadataFile), new TypeReference<>() {
                });
            return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false)
                .filter(e -> StringUtils.countMatches(e.getId().replaceAll("/$", ""), File.separator) == 0).map(e -> {
                    String id = e.getId();
                    ObjectNode unit = (ObjectNode) e.getParams();
                    unit.put(VitamFieldsHelper.id(), id);
                    return unit;
                }).map(e -> findUnitParent(e, projectModel.getUnitUps(), attachmentGUID))
                .filter(e -> Objects.nonNull(e.getValue()))
                .collect(Collectors.toMap(Entry<String, String>::getKey, Entry<String, String>::getValue));
        } else {
            return new HashMap<>();
        }
    }

    private int createMetadata(TransactionModel transactionModel, ArchiveEntry entry,
        HashMap<String, String> attachmentGUID, ArchiveEntryInputStream entryInputStream, int maxLevel,
        Map<String, String> unitIds, String path, String fileName, boolean isAttachmentAuExist)
        throws IOException, CollectInternalException, InvalidParseOperationException {
        DescriptionLevel descriptionLevel = (entry.isDirectory()) ? DescriptionLevel.RECORD_GRP : DescriptionLevel.ITEM;

        Path parent = Paths.get(path).getParent();

        String parentUnit;
        if (path.lastIndexOf(File.separator) == -1) {
            parentUnit = (isAttachmentAuExist) ? attachmentGUID.get(STATIC_ATTACHMENT) : null;
        } else {
            parentUnit = parent != null ? unitIds.get(parent.getFileName().toString()) : null;
        }
        ArchiveUnitModel unit =
            MetadataHelper.createUnit(transactionModel.getId(), LevelType.fromValue(descriptionLevel.getValue()),
                fileName, parentUnit);

        unitIds.put(fileName, unit.getId());
        if (!entry.isDirectory()) {
            String extension = FilenameUtils.getExtension(fileName).toLowerCase();
            String objectId = GUIDFactory.newGUID().getId();
            String newFilename = (Strings.isNullOrEmpty(extension)) ? objectId : objectId + "." + extension;

            Entry<String, Long> binaryInformations =
                writeObjectToWorkspace(transactionModel.getId(), entryInputStream, newFilename);
            FormatIdentifierResponse formatIdentifierResponse =
                collectService.detectFileFormat(transactionModel.getId(), newFilename);

            ObjectGroupResponse objectGroup =
                MetadataHelper.createObjectGroup(transactionModel.getId(), fileName, objectId, newFilename,
                    formatIdentifierResponse, binaryInformations.getKey(), binaryInformations.getValue());
            writeObjectGroupToTemporaryFile(objectGroup, transactionModel.getId());
            unit.setOg(objectGroup.getId());
        }

        maxLevel = writeUnitToTemporaryFile(StringUtils.countMatches(path, File.separator), maxLevel, unit,
            transactionModel.getId());
        return maxLevel;
    }

    private void bulkWriteUnits(int maxLevel, Map<String, String> unitUps, String transactionId)
        throws FileNotFoundException {
        for (int level = 0; level <= maxLevel; level++) {
            File unitFile = PropertiesUtils.fileFromTmpFolder(
                MetadataType.UNIT.getName() + "_" + level + "_" + transactionId + VitamConstants.JSONL_EXTENSION);
            Iterator<ObjectNode> unitIterator =
                new JsonLineGenericIterator<>(new FileInputStream(unitFile), new TypeReference<>() {
                });

            if (level == 0 && !unitUps.isEmpty()) {
                unitIterator = IteratorUtils.transformedIterator(unitIterator, e -> updateParent(e, unitUps));
            }

            Iterators.partition(unitIterator, BULK_SIZE).forEachRemaining(units -> {
                try {
                    metadataRepository.saveArchiveUnits(units);
                } catch (CollectInternalException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private ObjectNode updateParent(ObjectNode unit, Map<String, String> unitUps) {
        String title = unit.get(TITLE).asText();
        String up = unitUps.get(title);
        if (up != null) {
            unit.set(PREFIX_UP, JsonHandler.createArrayNode().add(up));
        }
        return unit;
    }

    private void bulkWriteObjectGroups(String transactionId) throws FileNotFoundException {
        File ogFile = PropertiesUtils.fileFromTmpFolder(
            MetadataType.OBJECTGROUP.getName() + "_" + transactionId + VitamConstants.JSONL_EXTENSION);
        JsonLineGenericIterator<ObjectNode> ogIterator =
            new JsonLineGenericIterator<>(new FileInputStream(ogFile), new TypeReference<>() {
            });
        Iterators.partition(ogIterator, BULK_SIZE).forEachRemaining(objectGroups -> {
            try {
                metadataRepository.saveObjectGroups(objectGroups);
            } catch (CollectInternalException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void writeObjectGroupToTemporaryFile(Object objectGroup, String transactionId) throws IOException {
        File file = PropertiesUtils.fileFromTmpFolder(
            MetadataType.OBJECTGROUP.getName() + "_" + transactionId + VitamConstants.JSONL_EXTENSION);
        try (JsonLineWriter writer = new JsonLineWriter(new FileOutputStream(file, true), file.length() == 0)) {
            JsonNode objectGroupToSave = buildSerializationObjectMapper().convertValue(objectGroup, JsonNode.class);
            writer.addEntry(objectGroupToSave);
        }
    }

    private int writeUnitToTemporaryFile(int level, int maxLevel, Object unit, String transactionId)
        throws IOException {
        File file = PropertiesUtils.fileFromTmpFolder(
            MetadataType.UNIT.getName() + "_" + level + "_" + transactionId + VitamConstants.JSONL_EXTENSION);
        try (JsonLineWriter writer = new JsonLineWriter(new FileOutputStream(file, true), file.length() == 0)) {
            JsonNode unitToSave = buildSerializationObjectMapper().convertValue(unit, JsonNode.class);
            writer.addEntry(unitToSave);
        }
        return Math.max(maxLevel, level);
    }

    private Entry<String, Long> writeObjectToWorkspace(String transactionId, ArchiveEntryInputStream entryInputStream,
        String fileName) throws IOException, CollectInternalException {
        try (CountingInputStream countingInputStream = new CountingInputStream(entryInputStream)) {
            String digest = collectService.pushStreamToWorkspace(transactionId, countingInputStream,
                CONTENT_FOLDER.concat(File.separator).concat(fileName));

            return new SimpleEntry<>(digest, countingInputStream.getByteCount());
        }
    }
}

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
import fr.gouv.vitam.collect.common.exception.CollectInternalException;
import fr.gouv.vitam.collect.common.exception.CollectInternalInvalidRequestException;
import fr.gouv.vitam.collect.common.exception.CollectInternalServerSideException;
import fr.gouv.vitam.collect.internal.core.common.CollectJsonMetadataLine;
import fr.gouv.vitam.collect.internal.core.common.ProjectModel;
import fr.gouv.vitam.collect.internal.core.csv.CsvHelper;
import fr.gouv.vitam.collect.internal.core.csv.SedaSchemaInfoResolver;
import fr.gouv.vitam.collect.internal.core.helpers.JsonlMetadataFileValidator;
import fr.gouv.vitam.collect.internal.core.helpers.MetadataHelper;
import fr.gouv.vitam.collect.internal.core.helpers.TempWorkspace;
import fr.gouv.vitam.collect.internal.core.repository.MetadataRepository;
import fr.gouv.vitam.collect.internal.core.repository.ProjectRepository;
import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.format.identification.model.FormatIdentifierResponse;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.MetadataType;
import fr.gouv.vitam.common.model.VitamConstants;
import fr.gouv.vitam.common.model.objectgroup.ObjectGroupResponse;
import fr.gouv.vitam.common.model.unit.ArchiveUnitModel;
import fr.gouv.vitam.common.model.unit.LevelType;
import fr.gouv.vitam.common.storage.compress.ArchiveEntryInputStream;
import fr.gouv.vitam.common.storage.compress.VitamArchiveStreamFactory;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.worker.core.distribution.JsonLineGenericIterator;
import fr.gouv.vitam.worker.core.distribution.JsonLineWriter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.input.CountingInputStream;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.Strings;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.zip.ZipException;

import static fr.gouv.vitam.collect.internal.core.helpers.MetadataHelper.findUnitParent;
import static fr.gouv.vitam.common.mapping.mapper.VitamObjectMapper.getSerializationObjectMapper;
import static fr.gouv.vitam.common.model.IngestWorkflowConstants.CONTENT_FOLDER;

public class FluxService {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(FluxService.class);

    private static final int BULK_SIZE = 1000;
    private static final String TRANSFORMED_METADATA_JSONL_FILE = "transformed_metadata.jsonl";
    static final String METADATA_CSV_FILE = "metadata.csv";
    static final String METADATA_JSONL_FILE = "metadata.jsonl";

    private final CollectService collectService;
    private final MetadataService metadataService;
    private final ProjectRepository projectRepository;
    private final MetadataRepository metadataRepository;
    private final AdminManagementClientFactory adminManagementClientFactory;

    public FluxService(
        CollectService collectService,
        MetadataService metadataService,
        ProjectRepository projectRepository,
        MetadataRepository metadataRepository,
        AdminManagementClientFactory adminManagementClientFactory
    ) {
        this.adminManagementClientFactory = adminManagementClientFactory;
        this.collectService = collectService;
        this.metadataService = metadataService;
        this.projectRepository = projectRepository;
        this.metadataRepository = metadataRepository;
    }

    public void processStream(
        InputStream inputStreamObject,
        String projectId,
        String transactionId,
        @Nullable String encoding
    ) throws CollectInternalException {
        Optional<ProjectModel> projectById = projectRepository.findProjectById(projectId);
        if (projectById.isEmpty()) {
            throw new CollectInternalException("Project not found");
        }
        ProjectModel projectModel = projectById.get();

        try (
            final TempWorkspace tempWorkspace = new TempWorkspace();
            final InputStream inputStreamClosable = StreamUtils.getRemainingReadOnCloseInputStream(inputStreamObject);
            final ArchiveInputStream archiveInputStream = new VitamArchiveStreamFactory()
                .createArchiveInputStream(CommonMediaType.ZIP_TYPE, inputStreamClosable, encoding)
        ) {
            ArchiveEntry entry;
            boolean isEmpty = true;
            Map<String, String> attachmentUnitsBySystemId = metadataService.prepareAttachmentUnits(
                projectModel,
                transactionId
            );
            Map<String, String> unitIdsByUploadPath = new HashMap<>();
            File metadataFile = null;
            boolean isCsvMetadataFile = false;
            // create entryInputStream to resolve the stream closed problem
            final ArchiveEntryInputStream entryInputStream = new ArchiveEntryInputStream(archiveInputStream);
            while ((entry = archiveInputStream.getNextEntry()) != null) {
                if (archiveInputStream.canReadEntryData(entry)) {
                    checkNonEmptyBinary(entry);

                    String path = FilenameUtils.normalize(entry.getName());
                    if (!FilenameUtils.equals(entry.getName(), path)) {
                        throw new IllegalStateException("path " + path + " is not canonical");
                    }
                    if (Strings.isNullOrEmpty(path)) {
                        continue;
                    }
                    path = FilenameUtils.normalizeNoEndSeparator(path);
                    if (!entry.isDirectory() && (path.equals(METADATA_JSONL_FILE) || path.equals(METADATA_CSV_FILE))) {
                        if (metadataFile != null) {
                            throw new CollectInternalInvalidRequestException(
                                "Cannot process zip upload for " +
                                projectById.get().getId() +
                                "/" +
                                transactionId +
                                ". Multiple metadata update files found."
                            );
                        }

                        metadataFile = tempWorkspace.writeToFile(path, entryInputStream);
                        isCsvMetadataFile = path.equals(METADATA_CSV_FILE);
                    } else {
                        String staticAttachmentUnitId = attachmentUnitsBySystemId.get(projectModel.getUnitUp());
                        createMetadata(
                            tempWorkspace,
                            transactionId,
                            path,
                            entryInputStream,
                            entry.isDirectory(),
                            unitIdsByUploadPath,
                            staticAttachmentUnitId
                        );
                    }
                    isEmpty = false;
                }
                entryInputStream.setClosed(false);
            }

            if (isEmpty) {
                throw new CollectInternalInvalidRequestException("Empty zip file.");
            }

            File validatedJsonlMetadataFile = null;
            if (metadataFile != null) {
                if (isCsvMetadataFile) {
                    validatedJsonlMetadataFile = csvMetadataToTransformedMetadataFile(tempWorkspace, metadataFile);
                } else {
                    validatedJsonlMetadataFile = validateJsonlMetadataFile(metadataFile);
                }
            }

            Map<String, Set<String>> dynamicAttachmentUnitUpsByRootUnitsUploadPath =
                computeDynamicAttachmentUnitUpsForRootUnits(
                    validatedJsonlMetadataFile,
                    projectModel,
                    attachmentUnitsBySystemId
                );

            bulkWriteUnits(tempWorkspace, dynamicAttachmentUnitUpsByRootUnitsUploadPath);

            bulkWriteObjectGroups(tempWorkspace);

            if (validatedJsonlMetadataFile != null) {
                try (InputStream is = new FileInputStream(validatedJsonlMetadataFile)) {
                    metadataService.updateUnitsWithJsonlMetadataFile(transactionId, is);
                }
            }
        } catch (ZipException | ArchiveException e) {
            throw new CollectInternalInvalidRequestException("Invalid ZIP archive: " + e.getMessage(), e);
        } catch (CollectInternalException e) {
            throw e;
        } catch (Exception e) {
            throw new CollectInternalException(
                "An unexpected error occurs when try to upload the ZIP: " + e.getMessage(),
                e
            );
        }
    }

    private File csvMetadataToTransformedMetadataFile(TempWorkspace tempWorkspace, File metadataFile)
        throws CollectInternalException {
        SedaSchemaInfoResolver sedaSchemaInfoResolver = new SedaSchemaInfoResolver(adminManagementClientFactory);

        try {
            File tranformedMetadataFile = tempWorkspace.getFile(TRANSFORMED_METADATA_JSONL_FILE);

            try (InputStream is = new FileInputStream(metadataFile)) {
                CsvHelper.convertCsvToJsonlMetadataFile(sedaSchemaInfoResolver, is, tranformedMetadataFile);
            }
            return tranformedMetadataFile;
        } catch (IOException e) {
            throw new CollectInternalServerSideException(
                "An internal error occurred during csv metadata file processing",
                e
            );
        }
    }

    private File validateJsonlMetadataFile(File jsonlMetadataFile) throws CollectInternalException {
        JsonlMetadataFileValidator jsonlMetadataFileValidator = new JsonlMetadataFileValidator();
        jsonlMetadataFileValidator.validate(jsonlMetadataFile, true);
        return jsonlMetadataFile;
    }

    private void checkNonEmptyBinary(ArchiveEntry entry) throws CollectInternalInvalidRequestException {
        if (!entry.isDirectory() && entry.getSize() == 0L) {
            throw new CollectInternalInvalidRequestException("Cannot upload empty file '" + entry.getName() + "'");
        }
    }

    private Map<String, Set<String>> computeDynamicAttachmentUnitUpsForRootUnits(
        File jsonlMetadataFile,
        ProjectModel projectModel,
        Map<String, String> attachmentUnitsBySystemId
    ) throws IOException {
        if (projectModel.getUnitUps() == null || jsonlMetadataFile == null) {
            return new HashMap<>();
        }
        try (
            JsonLineGenericIterator<CollectJsonMetadataLine> iterator = new JsonLineGenericIterator<>(
                new FileInputStream(jsonlMetadataFile),
                new TypeReference<>() {}
            )
        ) {
            Map<String, Set<String>> unitUpsByUploadPath = new HashMap<>();
            while (iterator.hasNext()) {
                CollectJsonMetadataLine jsonMetadataLine = iterator.next();
                String uploadPath = jsonMetadataLine.getFile();
                if (isNotRootLevelUnit(uploadPath)) {
                    // Only keep root-level units for dynamic attachment
                    continue;
                }

                ObjectNode unitContent = jsonMetadataLine.getUnitContent();
                Set<String> unitUps = findUnitParent(unitContent, projectModel.getUnitUps(), attachmentUnitsBySystemId);

                unitUpsByUploadPath.put(uploadPath, unitUps);
            }
            return unitUpsByUploadPath;
        }
    }

    private static boolean isNotRootLevelUnit(String uploadPath) {
        return StringUtils.contains(uploadPath, File.separatorChar);
    }

    private void createMetadata(
        TempWorkspace tempWorkspace,
        String transactionId,
        String path,
        InputStream entryInputStream,
        boolean isDirectory,
        Map<String, String> unitIdsByUploadPath,
        String staticAttachmentUnitId
    ) throws IOException, CollectInternalException {
        LevelType descriptionLevel = isDirectory ? LevelType.RECORD_GRP : LevelType.ITEM;
        String parentPath = FilenameUtils.getPathNoEndSeparator(path);

        String parentUnit;
        if (Strings.isNullOrEmpty(parentPath)) {
            parentUnit = staticAttachmentUnitId;
        } else {
            parentUnit = unitIdsByUploadPath.get(parentPath);
            if (parentUnit == null) {
                LOGGER.debug("Creating implicit parent folder '{}'", parentPath);
                createMetadata(
                    tempWorkspace,
                    transactionId,
                    parentPath,
                    null,
                    true,
                    unitIdsByUploadPath,
                    staticAttachmentUnitId
                );
            }

            parentUnit = unitIdsByUploadPath.get(parentPath);
        }
        String fileName = FilenameUtils.getName(path);

        ArchiveUnitModel unit = MetadataHelper.createUnit(transactionId, descriptionLevel, path, fileName, parentUnit);

        unitIdsByUploadPath.put(path, unit.getId());
        if (!isDirectory) {
            String extension = FilenameUtils.getExtension(fileName).toLowerCase();
            String objectId = GUIDFactory.newGUID().getId();
            String newFilename = (Strings.isNullOrEmpty(extension)) ? objectId : objectId + "." + extension;

            File binaryFile = tempWorkspace.writeToFile(newFilename, entryInputStream);
            try {
                Optional<FormatIdentifierResponse> formatIdentifierResponseOpt = collectService.detectFileFormat(
                    binaryFile
                );
                Entry<String, Long> binaryInformations = writeObjectToWorkspace(transactionId, binaryFile, newFilename);
                ObjectGroupResponse objectGroup = MetadataHelper.createObjectGroup(
                    transactionId,
                    fileName,
                    objectId,
                    newFilename,
                    formatIdentifierResponseOpt,
                    binaryInformations.getKey(),
                    binaryInformations.getValue()
                );
                writeObjectGroupToTemporaryFile(tempWorkspace, objectGroup);
                unit.setOg(objectGroup.getId());
            } finally {
                Files.deleteIfExists(binaryFile.toPath());
            }
        }

        writeUnitToTemporaryFile(tempWorkspace, StringUtils.countMatches(path, File.separator), unit);
    }

    private void bulkWriteUnits(TempWorkspace tempWorkspace, Map<String, Set<String>> unitUps) throws IOException {
        int level = 0;
        do {
            File unitFile = tempWorkspace.getFile(
                MetadataType.UNIT.getName() + "_" + level + VitamConstants.JSONL_EXTENSION
            );
            if (!unitFile.exists()) {
                // All levels processed
                return;
            }
            Iterator<ObjectNode> unitIterator = new JsonLineGenericIterator<>(
                new FileInputStream(unitFile),
                new TypeReference<>() {}
            );

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
            level++;
        } while ((true));
    }

    private ObjectNode updateParent(ObjectNode unit, Map<String, Set<String>> unitUps) {
        String unitUploadPath = unit.get(VitamFieldsHelper.uploadPath()).asText();
        Set<String> dynamicAttachmentParentUnitIds = unitUps.get(unitUploadPath);
        if (CollectionUtils.isNotEmpty(dynamicAttachmentParentUnitIds)) {
            unit.set(VitamFieldsHelper.unitups(), JsonHandler.createStringArrayNode(dynamicAttachmentParentUnitIds));
        }
        return unit;
    }

    private void bulkWriteObjectGroups(TempWorkspace tempWorkspace) throws IOException {
        File ogFile = tempWorkspace.getFile(MetadataType.OBJECTGROUP.getName() + VitamConstants.JSONL_EXTENSION);

        if (!ogFile.exists()) {
            LOGGER.info("No object group to insert");
            return;
        }

        JsonLineGenericIterator<ObjectNode> ogIterator = new JsonLineGenericIterator<>(
            new FileInputStream(ogFile),
            new TypeReference<>() {}
        );
        Iterators.partition(ogIterator, BULK_SIZE).forEachRemaining(objectGroups -> {
            try {
                metadataRepository.saveObjectGroups(objectGroups);
            } catch (CollectInternalException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void writeObjectGroupToTemporaryFile(TempWorkspace tempWorkspace, Object objectGroup) throws IOException {
        File file = tempWorkspace.getFile(MetadataType.OBJECTGROUP.getName() + VitamConstants.JSONL_EXTENSION);
        try (JsonLineWriter writer = new JsonLineWriter(new FileOutputStream(file, true), file.length() == 0)) {
            JsonNode objectGroupToSave = getSerializationObjectMapper().convertValue(objectGroup, JsonNode.class);
            writer.addEntry(objectGroupToSave);
        }
    }

    private void writeUnitToTemporaryFile(TempWorkspace tempWorkspace, int level, Object unit) throws IOException {
        // TODO : Do not open/write/close for each entry, use an appender to write units per-level
        File file = tempWorkspace.getFile(MetadataType.UNIT.getName() + "_" + level + VitamConstants.JSONL_EXTENSION);
        try (JsonLineWriter writer = new JsonLineWriter(new FileOutputStream(file, true), file.length() == 0)) {
            JsonNode unitToSave = getSerializationObjectMapper().convertValue(unit, JsonNode.class);
            writer.addEntry(unitToSave);
        }
    }

    private Entry<String, Long> writeObjectToWorkspace(String transactionId, File fileToWrite, String fileName)
        throws IOException, CollectInternalException {
        // TODO : Do not open/write/close for each entry, use an appender to write objects groups
        try (CountingInputStream countingInputStream = new CountingInputStream(new FileInputStream(fileToWrite))) {
            String digest = collectService.pushStreamToWorkspace(
                transactionId,
                countingInputStream,
                CONTENT_FOLDER.concat(File.separator).concat(fileName)
            );

            return new SimpleEntry<>(digest, countingInputStream.getByteCount());
        }
    }
}

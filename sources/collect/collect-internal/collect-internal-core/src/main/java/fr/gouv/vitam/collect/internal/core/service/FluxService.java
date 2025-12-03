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
import com.google.common.annotations.Beta;
import com.google.common.base.Strings;
import com.google.common.collect.Iterators;
import fr.gouv.vitam.collect.common.exception.CollectInternalException;
import fr.gouv.vitam.collect.common.exception.CollectInternalInvalidRequestException;
import fr.gouv.vitam.collect.common.exception.CollectInternalServerSideException;
import fr.gouv.vitam.collect.internal.core.common.CollectJsonMetadataLine;
import fr.gouv.vitam.collect.internal.core.common.ProjectModel;
import fr.gouv.vitam.collect.internal.core.configuration.CollectInternalConfiguration;
import fr.gouv.vitam.collect.internal.core.csv.CsvHelper;
import fr.gouv.vitam.collect.internal.core.csv.SedaSchemaInfoResolver;
import fr.gouv.vitam.collect.internal.core.helpers.MetadataHelper;
import fr.gouv.vitam.collect.internal.core.helpers.TempWorkspace;
import fr.gouv.vitam.collect.internal.core.jsonl.JsonlMetadataFileValidator;
import fr.gouv.vitam.collect.internal.core.repository.MetadataRepository;
import fr.gouv.vitam.collect.internal.core.repository.ProjectRepository;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.exception.InvalidJstlTransformerException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.JsltTransformationFailedException;
import fr.gouv.vitam.common.format.identification.model.FormatIdentifierResponse;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsltTransformer;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.objectgroup.ObjectGroupResponse;
import fr.gouv.vitam.common.model.unit.ArchiveUnitModel;
import fr.gouv.vitam.common.model.unit.LevelType;
import fr.gouv.vitam.common.storage.compress.ArchiveEntryInputStream;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.worker.core.distribution.JsonLineGenericIterator;
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import fr.gouv.vitam.worker.core.distribution.JsonLineWriter;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.model.FileParams;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceCollectClientFactory;
import fr.gouv.vitam.workspace.common.BulkMoveEntry;
import fr.gouv.vitam.workspace.common.BulkMoveRequest;
import jakarta.annotation.Nullable;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.zip.ZipException;

import static fr.gouv.vitam.collect.internal.core.helpers.MetadataHelper.findUnitParent;
import static fr.gouv.vitam.collect.internal.core.jsonl.JsonlHelper.getInitialUploadPath;
import static fr.gouv.vitam.common.SedaConstants.UPDATE_OPERATION;
import static fr.gouv.vitam.common.mapping.mapper.VitamObjectMapper.getSerializationObjectMapper;
import static fr.gouv.vitam.common.model.IngestWorkflowConstants.CONTENT_FOLDER;

public class FluxService {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(FluxService.class);

    private static final int BULK_SIZE = 1000;
    static final String METADATA_CSV_FILE = "metadata.csv";
    static final String METADATA_JSONL_FILE = "metadata.jsonl";
    public static final TypeReference<CollectJsonMetadataLine> COLLECT_JSON_METADATA_LINE_TYPE_REFERENCE =
        new TypeReference<>() {};
    public static final TypeReference<JsonLineModel> JSON_LINE_MODEL_TYPE_REFERENCE = new TypeReference<>() {};
    private static final String TITLE_FIELD = "Title";
    private static final String DESCRIPTION_LEVEL_FIELD = "DescriptionLevel";

    private final CollectService collectService;
    private final MetadataService metadataService;
    private final ProjectRepository projectRepository;
    private final MetadataRepository metadataRepository;
    private final AdminManagementClientFactory adminManagementClientFactory;
    private final CollectInternalConfiguration configuration;

    public FluxService(
        CollectService collectService,
        MetadataService metadataService,
        ProjectRepository projectRepository,
        MetadataRepository metadataRepository,
        AdminManagementClientFactory adminManagementClientFactory,
        CollectInternalConfiguration configuration
    ) {
        this.adminManagementClientFactory = adminManagementClientFactory;
        this.collectService = collectService;
        this.metadataService = metadataService;
        this.projectRepository = projectRepository;
        this.metadataRepository = metadataRepository;
        this.configuration = configuration;
    }

    public void processStream(
        InputStream inputStreamObject,
        String projectId,
        String transactionId,
        @Nullable String encoding,
        @Nullable String explicitAttachementId
    ) throws CollectInternalException {
        ProjectModel projectModel = getProjectModel(projectId);

        validateExplicitAttachementId(transactionId, explicitAttachementId);

        Map<String, String> attachmentUnitsBySystemId = createOrGetAttachementUnits(
            transactionId,
            explicitAttachementId,
            projectModel
        );

        try (final TempWorkspace tempWorkspace = new TempWorkspace()) {
            PreprocessingResult preprocessingResult = preprocessZipFile(
                inputStreamObject,
                transactionId,
                encoding,
                tempWorkspace,
                projectModel
            );

            File unitsToWriteFile = preprocessingResult.unitsToWriteFile();
            File objectGroupsToWriteFile = preprocessingResult.objectGroupsToWriteFile();
            File metadataFile = preprocessingResult.metadataFile();

            File jsonlMetadataFile = validateAndConvertMetadataToJsonl(
                metadataFile,
                tempWorkspace,
                explicitAttachementId != null
            );

            // Handle "ObjectFiles" path declaration & update units & object groups accordingly
            BidiMap<String, String> unitIdToObjectGroupIdOverrideMap = parseObjectFilesPathDeclarations(
                jsonlMetadataFile,
                unitsToWriteFile
            );
            unitsToWriteFile = updateUnitObjectGroups(
                tempWorkspace,
                unitsToWriteFile,
                unitIdToObjectGroupIdOverrideMap
            );
            objectGroupsToWriteFile = updateObjectGroupsParentUnits(
                tempWorkspace,
                objectGroupsToWriteFile,
                unitIdToObjectGroupIdOverrideMap
            );

            // Pre-dynamic attachement JSLT transformation
            jsonlMetadataFile = applyPreDynamicAttachementJsltTransformation(
                jsonlMetadataFile,
                unitsToWriteFile,
                projectModel,
                tempWorkspace
            );

            // Update units parents with dynamic attachement
            TopLevelUnitAttachmentResolver topLevelUnitAttachementResolver = computeTopLevelUnitAttachments(
                jsonlMetadataFile,
                projectModel,
                explicitAttachementId,
                attachmentUnitsBySystemId
            );

            // Post-dynamic attachement JSLT transformation
            jsonlMetadataFile = applyPostDynamicAttachementJsltTransformation(
                jsonlMetadataFile,
                unitsToWriteFile,
                projectModel,
                tempWorkspace
            );

            bulkWriteUnits(unitsToWriteFile, tempWorkspace, topLevelUnitAttachementResolver);

            bulkWriteObjectGroups(objectGroupsToWriteFile);

            bulkUpdateUnits(transactionId, jsonlMetadataFile);
        } catch (CollectInternalException e) {
            throw e;
        } catch (Exception e) {
            throw new CollectInternalException(
                "An unexpected error occurs when try to upload the ZIP: " + e.getMessage(),
                e
            );
        }
    }

    private PreprocessingResult preprocessZipFile(
        InputStream inputStreamObject,
        String transactionId,
        String encoding,
        TempWorkspace tempWorkspace,
        ProjectModel projectModel
    ) throws IOException, CollectInternalException {
        File unitsToWriteFile = tempWorkspace.tempFile();
        File objectGroupsToWriteFile = tempWorkspace.tempFile();
        File metadataFile = null;
        boolean isEmpty = true;

        try (
            final InputStream inputStreamClosable = StreamUtils.getRemainingReadOnCloseInputStream(inputStreamObject);
            final ZipArchiveInputStream archiveInputStream = new ZipArchiveInputStream(inputStreamClosable, encoding);
            FileOutputStream unitsToWriteOutputStream = new FileOutputStream(unitsToWriteFile);
            JsonLineWriter unitToWriteWriter = new JsonLineWriter(unitsToWriteOutputStream);
            FileOutputStream objectGroupsToWriteOutputStream = new FileOutputStream(objectGroupsToWriteFile);
            JsonLineWriter objectGroupsToWriteWriter = new JsonLineWriter(objectGroupsToWriteOutputStream)
        ) {
            ArchiveEntry entry;

            Map<String, String> unitIdsByUploadPath = new HashMap<>();
            // create entryInputStream to resolve the stream closed problem
            final ArchiveEntryInputStream entryInputStream = new ArchiveEntryInputStream(archiveInputStream);
            while ((entry = archiveInputStream.getNextEntry()) != null) {
                if (archiveInputStream.canReadEntryData(entry)) {
                    checkNonEmptyBinary(entry);
                    if (Strings.isNullOrEmpty(entry.getName())) {
                        continue;
                    }
                    String path = FilenameUtils.normalize(entry.getName());
                    if (!FilenameUtils.equals(entry.getName(), path)) {
                        throw new IllegalStateException("path " + path + " is not canonical");
                    }
                    path = FilenameUtils.normalizeNoEndSeparator(path);
                    if (!entry.isDirectory() && (path.equals(METADATA_JSONL_FILE) || path.equals(METADATA_CSV_FILE))) {
                        if (metadataFile != null) {
                            throw new CollectInternalInvalidRequestException(
                                "Cannot process zip upload for " +
                                projectModel.getId() +
                                "/" +
                                transactionId +
                                ". Multiple metadata update files found."
                            );
                        }

                        metadataFile = tempWorkspace.writeToFile(path, entryInputStream);
                    } else {
                        createMetadata(
                            tempWorkspace,
                            unitToWriteWriter,
                            objectGroupsToWriteWriter,
                            transactionId,
                            path,
                            entryInputStream,
                            entry.isDirectory(),
                            unitIdsByUploadPath,
                            projectModel
                        );
                    }
                    isEmpty = false;
                }
                entryInputStream.setClosed(false);
            }
        } catch (ZipException e) {
            throw new CollectInternalInvalidRequestException("Invalid ZIP archive: " + e.getMessage(), e);
        }

        if (isEmpty) {
            throw new CollectInternalInvalidRequestException("Empty zip file.");
        }

        return new PreprocessingResult(unitsToWriteFile, objectGroupsToWriteFile, metadataFile);
    }

    private ProjectModel getProjectModel(String projectId) throws CollectInternalException {
        Optional<ProjectModel> projectById = projectRepository.findProjectById(projectId);
        if (projectById.isEmpty()) {
            throw new CollectInternalException("Project not found");
        }
        return projectById.get();
    }

    private void validateExplicitAttachementId(String transactionId, String explicitAttachementId)
        throws CollectInternalException {
        if (explicitAttachementId == null) {
            return;
        }
        try {
            SelectMultiQuery query = new SelectMultiQuery();
            query.addQueries(QueryHelper.eq(VitamFieldsHelper.id(), explicitAttachementId));
            query.addUsedProjection(VitamFieldsHelper.id());
            RequestResponseOK<JsonNode> units = metadataService.selectUnitsByTransactionId(
                query.getFinalSelect(),
                transactionId
            );
            if (units.getResults().isEmpty()) {
                throw new CollectInternalInvalidRequestException(
                    "No such unit with id '" + explicitAttachementId + "' in the transaction"
                );
            }
        } catch (InvalidParseOperationException | InvalidCreateOperationException e) {
            throw new CollectInternalServerSideException(e);
        }
    }

    private Map<String, String> createOrGetAttachementUnits(
        String transactionId,
        String explicitAttachementId,
        ProjectModel projectModel
    ) throws CollectInternalException {
        if (explicitAttachementId != null) {
            // No need for static/attachment units when an explicitAttachementId is specified
            return Collections.emptyMap();
        }

        return metadataService.prepareAttachmentUnits(projectModel, transactionId);
    }

    private File validateAndConvertMetadataToJsonl(
        File metadataFile,
        TempWorkspace tempWorkspace,
        boolean explicitAttachementMode
    ) throws CollectInternalException {
        if (metadataFile == null) {
            return null;
        }
        if (metadataFile.getName().equals(METADATA_CSV_FILE)) {
            return csvMetadataToConvertedJsonlMetadataFile(tempWorkspace, metadataFile, explicitAttachementMode);
        } else {
            return validateJsonlMetadataFile(metadataFile, explicitAttachementMode);
        }
    }

    @Beta
    private File applyPreDynamicAttachementJsltTransformation(
        File jsonlMetadataFile,
        File unitsToWriteFile,
        ProjectModel projectModel,
        TempWorkspace tempWorkspace
    ) throws CollectInternalException, IOException {
        if (
            StringUtils.isBlank(projectModel.getTransformationRules()) ||
            configuration.isApplyJsltPostDynamicAttachement()
        ) {
            return jsonlMetadataFile;
        }

        // Update jsonl with "default" unit metadata (Title & DescriptionLevel)
        File fullJsonlMetadataFile = mergeDefaultMetadataIntoMetadataJsonl(
            unitsToWriteFile,
            jsonlMetadataFile,
            tempWorkspace
        );

        return transformJsonMetadataFile(projectModel, tempWorkspace, fullJsonlMetadataFile);
    }

    @Beta
    private File applyPostDynamicAttachementJsltTransformation(
        File jsonlMetadataFile,
        File unitsToWriteFile,
        ProjectModel projectModel,
        TempWorkspace tempWorkspace
    ) throws CollectInternalException, IOException {
        if (
            StringUtils.isBlank(projectModel.getTransformationRules()) ||
            !configuration.isApplyJsltPostDynamicAttachement()
        ) {
            return jsonlMetadataFile;
        }

        // Update jsonl with "default" unit metadata (Title & DescriptionLevel)
        File fullJsonlMetadataFile = mergeDefaultMetadataIntoMetadataJsonl(
            unitsToWriteFile,
            jsonlMetadataFile,
            tempWorkspace
        );

        return transformJsonMetadataFile(projectModel, tempWorkspace, fullJsonlMetadataFile);
    }

    private File mergeDefaultMetadataIntoMetadataJsonl(
        File unitsToWriteFile,
        File jsonlMetadataFile,
        TempWorkspace tempWorkspace
    ) throws IOException, CollectInternalException {
        Map<String, TitleAndDescriptionLevel> defaultUnitMetadataByUploadPath = loadDefaultMetadataUnits(
            unitsToWriteFile
        );

        File fullMetadataJsonlFile = tempWorkspace.tempFile();
        try (
            OutputStream outputStream = new FileOutputStream(fullMetadataJsonlFile);
            JsonLineWriter writer = new JsonLineWriter(outputStream)
        ) {
            if (jsonlMetadataFile != null) {
                try (
                    JsonLineGenericIterator<CollectJsonMetadataLine> iterator = new JsonLineGenericIterator<>(
                        new FileInputStream(jsonlMetadataFile),
                        COLLECT_JSON_METADATA_LINE_TYPE_REFERENCE
                    )
                ) {
                    while (iterator.hasNext()) {
                        CollectJsonMetadataLine entry = iterator.next();

                        String uploadPath = getInitialUploadPath(entry);

                        TitleAndDescriptionLevel unitMetadata = defaultUnitMetadataByUploadPath.get(uploadPath);

                        if (unitMetadata == null) {
                            throw new CollectInternalInvalidRequestException(
                                "Invalid metadata file. No such File '" + uploadPath + "'"
                            );
                        }

                        if (!entry.getUnitContent().has(TITLE_FIELD)) {
                            entry.getUnitContent().put(TITLE_FIELD, unitMetadata.title());
                        }
                        if (!entry.getUnitContent().has(DESCRIPTION_LEVEL_FIELD)) {
                            entry.getUnitContent().put(DESCRIPTION_LEVEL_FIELD, unitMetadata.descriptionLevel());
                        }
                        writer.addEntry(entry);

                        // Remove unit to free-up memory
                        defaultUnitMetadataByUploadPath.remove(uploadPath);
                    }
                }
            }

            // Append default metadata for units without explicit metadata
            for (String fileUploadPath : defaultUnitMetadataByUploadPath.keySet()) {
                TitleAndDescriptionLevel unitMetadata = defaultUnitMetadataByUploadPath.get(fileUploadPath);
                ObjectNode metadata = JsonHandler.createObjectNode();

                metadata.put(TITLE_FIELD, unitMetadata.title());
                metadata.put(DESCRIPTION_LEVEL_FIELD, unitMetadata.descriptionLevel());

                writer.addEntry(new CollectJsonMetadataLine(fileUploadPath, null, null, null, metadata));
            }
        }
        return fullMetadataJsonlFile;
    }

    private Map<String, TitleAndDescriptionLevel> loadDefaultMetadataUnits(File unitsToWriteFile) throws IOException {
        Map<String, TitleAndDescriptionLevel> unitMetadataByPath = new HashMap<>();
        try (
            InputStream inputStream = new FileInputStream(unitsToWriteFile);
            JsonLineGenericIterator<JsonLineModel> unitsToWriteIterator = new JsonLineGenericIterator<>(
                inputStream,
                JSON_LINE_MODEL_TYPE_REFERENCE
            )
        ) {
            while (unitsToWriteIterator.hasNext()) {
                JsonLineModel entry = unitsToWriteIterator.next();
                unitMetadataByPath.put(
                    entry.getParams().get(VitamFieldsHelper.uploadPath()).asText(),
                    new TitleAndDescriptionLevel(
                        entry.getParams().get(TITLE_FIELD).asText(),
                        entry.getParams().get(DESCRIPTION_LEVEL_FIELD).asText()
                    )
                );
            }
        }
        return unitMetadataByPath;
    }

    @Beta
    private File transformJsonMetadataFile(
        ProjectModel projectModel,
        TempWorkspace tempWorkspace,
        File validatedJsonlMetadataFile
    ) throws CollectInternalException, IOException {
        try {
            JsltTransformer jsltTransformer = new JsltTransformer(projectModel.getTransformationRules());

            File transformedJsonlMetadataFile = tempWorkspace.tempFile();
            try (
                JsonLineGenericIterator<CollectJsonMetadataLine> iterator = new JsonLineGenericIterator<>(
                    new FileInputStream(validatedJsonlMetadataFile),
                    COLLECT_JSON_METADATA_LINE_TYPE_REFERENCE
                );
                JsonLineWriter writer = new JsonLineWriter(new FileOutputStream(transformedJsonlMetadataFile))
            ) {
                while (iterator.hasNext()) {
                    CollectJsonMetadataLine entry = iterator.next();
                    ObjectNode initialUnitContent = entry.getUnitContent();
                    ObjectNode transformedUnitContent = jsltTransformer.transform(initialUnitContent);

                    CollectJsonMetadataLine transformedJsonMetadataLine = new CollectJsonMetadataLine(
                        entry.getFile(),
                        entry.getId(),
                        entry.getObjectFiles(),
                        entry.getSelector(),
                        transformedUnitContent
                    );
                    writer.addEntry(transformedJsonMetadataLine);
                }
            }
            return transformedJsonlMetadataFile;
        } catch (JsltTransformationFailedException e) {
            throw new CollectInternalInvalidRequestException("Invalid JSLT transformation", e);
        } catch (InvalidJstlTransformerException e) {
            throw new CollectInternalServerSideException("JSLT transformation failed: " + e.getMessage(), e);
        }
    }

    private File csvMetadataToConvertedJsonlMetadataFile(
        TempWorkspace tempWorkspace,
        File metadataFile,
        boolean explicitAttachementMode
    ) throws CollectInternalException {
        SedaSchemaInfoResolver sedaSchemaInfoResolver = new SedaSchemaInfoResolver(adminManagementClientFactory);

        try {
            File tranformedMetadataFile = tempWorkspace.tempFile();

            try (InputStream is = new FileInputStream(metadataFile)) {
                CsvHelper.convertCsvToJsonlMetadataFile(
                    sedaSchemaInfoResolver,
                    is,
                    tranformedMetadataFile,
                    true,
                    explicitAttachementMode
                );
            }
            return tranformedMetadataFile;
        } catch (IOException e) {
            throw new CollectInternalServerSideException(
                "An internal error occurred during csv metadata file processing",
                e
            );
        }
    }

    private File validateJsonlMetadataFile(File jsonlMetadataFile, boolean explicitAttachementMode)
        throws CollectInternalException {
        JsonlMetadataFileValidator jsonlMetadataFileValidator = new JsonlMetadataFileValidator();
        jsonlMetadataFileValidator.validate(jsonlMetadataFile, true, explicitAttachementMode);
        return jsonlMetadataFile;
    }

    private BidiMap<String, String> parseObjectFilesPathDeclarations(File jsonlMetadataFile, File unitsToWriteFile)
        throws IOException, CollectInternalInvalidRequestException {
        if (jsonlMetadataFile == null) {
            return new DualHashBidiMap<>();
        }

        // Load graph info (uploadPath -> Unit & ObjectGroup ids)
        Map<String, String> initialUploadPathToUnitId = new HashMap<>();
        Map<String, String> initialUploadPathToObjectGroupId = new HashMap<>();
        try (
            InputStream inputStream = new FileInputStream(unitsToWriteFile);
            JsonLineGenericIterator<JsonLineModel> unitsToWrite = new JsonLineGenericIterator<>(
                inputStream,
                JSON_LINE_MODEL_TYPE_REFERENCE
            )
        ) {
            while (unitsToWrite.hasNext()) {
                JsonLineModel entry = unitsToWrite.next();
                String uploadPath = entry.getParams().get(VitamFieldsHelper.uploadPath()).asText();

                String unitId = entry.getParams().get(VitamFieldsHelper.id()).asText();
                initialUploadPathToUnitId.put(uploadPath, unitId);

                if (entry.getParams().has(VitamFieldsHelper.object())) {
                    String objectGroupId = entry.getParams().get(VitamFieldsHelper.object()).asText();
                    initialUploadPathToObjectGroupId.put(uploadPath, objectGroupId);
                }
            }
        }

        // Parse & validate "File" ("#uploadPath" selector) & "ObjectFiles"
        Set<String> duplicatePaths = new HashSet<>();
        BidiMap<String, String> unitPathToObjectFilePathMap = new DualHashBidiMap<>();
        try (
            JsonLineGenericIterator<CollectJsonMetadataLine> iterator = new JsonLineGenericIterator<>(
                new FileInputStream(jsonlMetadataFile),
                COLLECT_JSON_METADATA_LINE_TYPE_REFERENCE
            )
        ) {
            while (iterator.hasNext()) {
                CollectJsonMetadataLine entry = iterator.next();

                // Validate upload path ("File" or "#uploadPath" selector)
                String uploadPath = getInitialUploadPath(entry);

                if (duplicatePaths.contains(uploadPath)) {
                    throw new CollectInternalInvalidRequestException(
                        "Duplicate File or #uploadPath selector declaration for '" + uploadPath + "'"
                    );
                }
                duplicatePaths.add(uploadPath);

                if (!initialUploadPathToUnitId.containsKey(uploadPath)) {
                    throw new CollectInternalInvalidRequestException(
                        "Invalid File or #uploadPath selector '" + uploadPath + "'. No such file or directory."
                    );
                }

                // Validate "ObjectFiles"
                String objectFilesPath = entry.getObjectFiles();
                if (objectFilesPath == null) {
                    continue;
                }

                if (!initialUploadPathToObjectGroupId.containsKey(objectFilesPath)) {
                    if (initialUploadPathToUnitId.containsKey(objectFilesPath)) {
                        throw new CollectInternalInvalidRequestException(
                            "Invalid ObjectFiles value '" + objectFilesPath + "'. Must be a file."
                        );
                    } else {
                        throw new CollectInternalInvalidRequestException(
                            "Invalid ObjectFiles value '" + objectFilesPath + "'. No such file."
                        );
                    }
                }

                // Skip edge case where ObjectFiles is the same as uploadPath (redundant info)
                if (objectFilesPath.equals(uploadPath)) {
                    continue;
                }

                if (duplicatePaths.contains(objectFilesPath)) {
                    throw new CollectInternalInvalidRequestException(
                        "Duplicate ObjectFiles declaration for '" + objectFilesPath + "'"
                    );
                }
                duplicatePaths.add(objectFilesPath);

                // ObjectFiles can only be set for directory uploadPaths.
                if (initialUploadPathToObjectGroupId.containsKey(uploadPath)) {
                    throw new CollectInternalInvalidRequestException(
                        "ObjectFiles value '" +
                        objectFilesPath +
                        "' can only be set when File or #uploadPath selector '" +
                        uploadPath +
                        "' is a directory."
                    );
                }

                unitPathToObjectFilePathMap.put(uploadPath, objectFilesPath);
            }
        }

        if (unitPathToObjectFilePathMap.isEmpty()) {
            return unitPathToObjectFilePathMap;
        }

        // Map uploadPaths to unitId & ObjectGroupId
        BidiMap<String, String> unitIdToObjectGroupIdRemapping = new DualHashBidiMap<>();
        for (String uploadPath : unitPathToObjectFilePathMap.keySet()) {
            String newObjectPath = unitPathToObjectFilePathMap.get(uploadPath);
            String unitId = initialUploadPathToUnitId.get(uploadPath);
            String objectGroupId = initialUploadPathToObjectGroupId.get(newObjectPath);
            unitIdToObjectGroupIdRemapping.put(unitId, objectGroupId);
        }
        return unitIdToObjectGroupIdRemapping;
    }

    private static File updateUnitObjectGroups(
        TempWorkspace tempWorkspace,
        File unitsToWriteFile,
        BidiMap<String, String> unitIdToObjectGroupIdOverrideMap
    ) throws IOException {
        if (unitIdToObjectGroupIdOverrideMap.isEmpty()) {
            return unitsToWriteFile;
        }

        File updatedUnitsToWriteFile = tempWorkspace.tempFile();
        try (
            InputStream inputStream = new FileInputStream(unitsToWriteFile);
            JsonLineGenericIterator<JsonLineModel> unitsToWrite = new JsonLineGenericIterator<>(
                inputStream,
                JSON_LINE_MODEL_TYPE_REFERENCE
            );
            FileOutputStream unitsToWriteOutputStream = new FileOutputStream(updatedUnitsToWriteFile);
            JsonLineWriter unitToWriteWriter = new JsonLineWriter(unitsToWriteOutputStream)
        ) {
            while (unitsToWrite.hasNext()) {
                JsonLineModel entry = unitsToWrite.next();

                ObjectNode unit = (ObjectNode) entry.getParams();
                String unitId = unit.get(VitamFieldsHelper.id()).asText();
                if (unit.has(VitamFieldsHelper.object())) {
                    String objectGroupId = unit.get(VitamFieldsHelper.object()).asText();
                    if (unitIdToObjectGroupIdOverrideMap.containsValue(objectGroupId)) {
                        // Delete this unit (no more used since its ObjectGroup is being reattached to another unit)
                        continue;
                    }
                }

                if (unitIdToObjectGroupIdOverrideMap.containsKey(unitId)) {
                    String newObjectGroupId = unitIdToObjectGroupIdOverrideMap.get(unitId);
                    unit.put(VitamFieldsHelper.object(), newObjectGroupId);
                    unit.put(DESCRIPTION_LEVEL_FIELD, LevelType.ITEM.value());
                }

                unitToWriteWriter.addEntry(entry);
            }
        }
        return updatedUnitsToWriteFile;
    }

    private File updateObjectGroupsParentUnits(
        TempWorkspace tempWorkspace,
        File objectsToWriteFile,
        BidiMap<String, String> unitIdToObjectGroupIdOverrideMap
    ) throws IOException {
        if (unitIdToObjectGroupIdOverrideMap.isEmpty()) {
            return objectsToWriteFile;
        }

        File updatedObjectGroupsToWriteFile = tempWorkspace.tempFile();
        try (
            JsonLineGenericIterator<ObjectNode> ogIterator = new JsonLineGenericIterator<>(
                new FileInputStream(objectsToWriteFile),
                new TypeReference<>() {}
            );
            FileOutputStream objectGroupsToWriteOutputStream = new FileOutputStream(updatedObjectGroupsToWriteFile);
            JsonLineWriter objectGroupsToWriteWriter = new JsonLineWriter(objectGroupsToWriteOutputStream)
        ) {
            while (ogIterator.hasNext()) {
                ObjectNode objectGroup = ogIterator.next();
                String objectGroupId = objectGroup.get(VitamFieldsHelper.id()).asText();
                if (unitIdToObjectGroupIdOverrideMap.containsValue(objectGroupId)) {
                    String newUnitId = unitIdToObjectGroupIdOverrideMap.getKey(objectGroupId);
                    objectGroup.set(VitamFieldsHelper.unitups(), JsonHandler.createStringArrayNode(newUnitId));
                }
                objectGroupsToWriteWriter.addEntry(objectGroup);
            }
        }
        return updatedObjectGroupsToWriteFile;
    }

    private void checkNonEmptyBinary(ArchiveEntry entry) throws CollectInternalInvalidRequestException {
        if (!entry.isDirectory() && entry.getSize() == 0L) {
            throw new CollectInternalInvalidRequestException("Cannot upload empty file '" + entry.getName() + "'");
        }
    }

    private TopLevelUnitAttachmentResolver computeTopLevelUnitAttachments(
        File jsonlMetadataFile,
        ProjectModel projectModel,
        String explicitAttachementId,
        Map<String, String> attachmentUnitsBySystemId
    ) throws IOException {
        // Top-level unit attachment precedence order :
        // 1. Explicit X-Attachement-Id header is set
        // 2. #management.UpdateOperation.* fields (no parent units apply)
        // 3. Matching dynamic attachment rules (if they exist and none match skip)
        // 4. Static attachment unit Id
        // 5. None (no attachement)

        if (explicitAttachementId != null) {
            return TopLevelUnitAttachmentResolver.singleton(explicitAttachementId);
        }

        if (attachmentUnitsBySystemId.isEmpty()) {
            // No static/dynamic attachement (and UpdateOperation shall not have parents)
            return TopLevelUnitAttachmentResolver.empty();
        }

        String staticAttachmentUnitId = (projectModel.getUnitUp() != null)
            ? attachmentUnitsBySystemId.get(projectModel.getUnitUp())
            : null;

        if (jsonlMetadataFile == null) {
            // No UpdateOperation or dynamic attachment, only static attachment apply
            if (projectModel.getUnitUp() != null) {
                return TopLevelUnitAttachmentResolver.singleton(staticAttachmentUnitId);
            } else {
                return TopLevelUnitAttachmentResolver.empty();
            }
        }

        Map<String, Set<String>> unitUpsByUploadPath = computeUnitUpsFormUnitMetadata(
            jsonlMetadataFile,
            projectModel,
            attachmentUnitsBySystemId,
            staticAttachmentUnitId
        );

        return uploadPath -> {
            // Units with metadata
            if (unitUpsByUploadPath.containsKey(uploadPath)) {
                return unitUpsByUploadPath.get(uploadPath);
            }
            // Units without metadata (not present in jsonlMetadataFile) ==> only static attachement apply
            if (staticAttachmentUnitId != null) {
                return Collections.singleton(staticAttachmentUnitId);
            }
            return Collections.emptySet();
        };
    }

    private Map<String, Set<String>> computeUnitUpsFormUnitMetadata(
        File jsonlMetadataFile,
        ProjectModel projectModel,
        Map<String, String> attachmentUnitsBySystemId,
        String staticAttachmentUnitId
    ) throws IOException {
        Map<String, Set<String>> unitUpsByUploadPath = new HashMap<>();
        try (
            JsonLineGenericIterator<CollectJsonMetadataLine> iterator = new JsonLineGenericIterator<>(
                new FileInputStream(jsonlMetadataFile),
                COLLECT_JSON_METADATA_LINE_TYPE_REFERENCE
            )
        ) {
            while (iterator.hasNext()) {
                CollectJsonMetadataLine jsonMetadataLine = iterator.next();
                String uploadPath = jsonMetadataLine.getFile();
                if (isNotRootLevelUnit(uploadPath)) {
                    // Only keep root-level units for dynamic attachment
                    continue;
                }

                ObjectNode unitContent = jsonMetadataLine.getUnitContent();

                Set<String> unitUps = computeAttachmentUnitUps(
                    unitContent,
                    projectModel,
                    attachmentUnitsBySystemId,
                    staticAttachmentUnitId
                );

                unitUpsByUploadPath.put(uploadPath, unitUps);
            }
        }
        return unitUpsByUploadPath;
    }

    private Set<String> computeAttachmentUnitUps(
        ObjectNode unitContent,
        ProjectModel projectModel,
        Map<String, String> attachmentUnitsBySystemId,
        String staticAttachmentUnitId
    ) {
        if (
            unitContent.has(VitamFieldsHelper.management()) &&
            unitContent.get(VitamFieldsHelper.management()).has(UPDATE_OPERATION)
        ) {
            // No parent units
            return Collections.emptySet();
        }

        if (CollectionUtils.isNotEmpty(projectModel.getUnitUps())) {
            Set<String> dynamicAttachmentUnitUps = findUnitParent(
                unitContent,
                projectModel.getUnitUps(),
                attachmentUnitsBySystemId
            );

            if (!dynamicAttachmentUnitUps.isEmpty()) {
                return dynamicAttachmentUnitUps;
            }
        }

        if (staticAttachmentUnitId != null) {
            return Collections.singleton(staticAttachmentUnitId);
        }

        return Collections.emptySet();
    }

    private static boolean isNotRootLevelUnit(String uploadPath) {
        return StringUtils.contains(uploadPath, File.separatorChar);
    }

    private void createMetadata(
        TempWorkspace tempWorkspace,
        JsonLineWriter unitToWriteWriter,
        JsonLineWriter objectGroupsToWriteWriter,
        String transactionId,
        String path,
        InputStream entryInputStream,
        boolean isDirectory,
        Map<String, String> unitIdsByUploadPath,
        ProjectModel projectModel
    ) throws IOException, CollectInternalException {
        LevelType descriptionLevel = isDirectory ? LevelType.RECORD_GRP : LevelType.ITEM;
        String parentPath = FilenameUtils.getPathNoEndSeparator(path);

        String parentUnit;
        if (Strings.isNullOrEmpty(parentPath)) {
            parentUnit = null;
        } else {
            parentUnit = unitIdsByUploadPath.get(parentPath);
            if (parentUnit == null) {
                LOGGER.debug("Creating implicit parent folder '{}'", parentPath);
                createMetadata(
                    tempWorkspace,
                    unitToWriteWriter,
                    objectGroupsToWriteWriter,
                    transactionId,
                    parentPath,
                    null,
                    true,
                    unitIdsByUploadPath,
                    projectModel
                );
            }

            parentUnit = unitIdsByUploadPath.get(parentPath);
        }
        String fileName = FilenameUtils.getName(path);
        String originationAgency = null;
        if (
            projectModel.getManifestContext() != null &&
            StringUtils.isNotEmpty(projectModel.getManifestContext().getOriginatingAgencyIdentifier())
        ) {
            originationAgency = projectModel.getManifestContext().getOriginatingAgencyIdentifier();
        }
        ArchiveUnitModel unit = MetadataHelper.createUnit(
            transactionId,
            descriptionLevel,
            path,
            fileName,
            parentUnit,
            originationAgency
        );

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
                String batchId = VitamThreadUtils.getVitamSession().getRequestId();
                DigestWithSize binaryInfo = writeObjectToWorkspace(batchId, binaryFile, newFilename);
                String originatingAgency = null;
                if (
                    projectModel.getManifestContext() != null &&
                    StringUtils.isNotEmpty(projectModel.getManifestContext().getOriginatingAgencyIdentifier())
                ) {
                    originatingAgency = projectModel.getManifestContext().getOriginatingAgencyIdentifier();
                }
                ObjectGroupResponse objectGroup = MetadataHelper.createObjectGroup(
                    transactionId,
                    fileName,
                    objectId,
                    newFilename,
                    formatIdentifierResponseOpt,
                    binaryInfo.digest(),
                    binaryInfo.size(),
                    originatingAgency,
                    unit.getId()
                );
                writeObjectGroupToTemporaryFile(objectGroupsToWriteWriter, objectGroup);
                unit.setOg(objectGroup.getId());
            } finally {
                Files.deleteIfExists(binaryFile.toPath());
            }
        }

        writeUnitToTemporaryFile(unitToWriteWriter, StringUtils.countMatches(path, File.separator), unit);
    }

    private void bulkWriteUnits(
        File unitsToWriteFile,
        TempWorkspace tempWorkspace,
        TopLevelUnitAttachmentResolver topLevelUnitAttachementResolver
    ) throws IOException {
        List<File> filePerLevel = splitFilePerLevel(unitsToWriteFile, tempWorkspace);

        for (File file : filePerLevel) {
            try (
                InputStream inputStream = new FileInputStream(file);
                JsonLineGenericIterator<JsonLineModel> unitsToWrite = new JsonLineGenericIterator<>(
                    inputStream,
                    JSON_LINE_MODEL_TYPE_REFERENCE
                )
            ) {
                Iterator<ObjectNode> unitIterator = IteratorUtils.transformedIterator(unitsToWrite, e -> {
                    ObjectNode unitContent = (ObjectNode) e.getParams();
                    // Set attachement unit ids for top-level units
                    if (e.getDistribGroup() == 0) {
                        updateParents(unitContent, topLevelUnitAttachementResolver);
                    }
                    return unitContent;
                });

                Iterators.partition(unitIterator, BULK_SIZE).forEachRemaining(units -> {
                    try {
                        metadataRepository.saveArchiveUnits(units);
                    } catch (CollectInternalException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }
    }

    private List<File> splitFilePerLevel(File unitsToWriteFile, TempWorkspace tempWorkspace) throws IOException {
        List<File> filePerLevel = new ArrayList<>();
        List<JsonLineWriter> writerPerLevel = new ArrayList<>();

        try (
            InputStream inputStream = new FileInputStream(unitsToWriteFile);
            JsonLineGenericIterator<JsonLineModel> unitsToWrite = new JsonLineGenericIterator<>(
                inputStream,
                JSON_LINE_MODEL_TYPE_REFERENCE
            )
        ) {
            while (unitsToWrite.hasNext()) {
                JsonLineModel entry = unitsToWrite.next();
                int level = entry.getDistribGroup();
                JsonLineWriter writer;
                if (writerPerLevel.size() > level) {
                    writer = writerPerLevel.get(level);
                } else if (writerPerLevel.size() == level) {
                    File file = tempWorkspace.tempFile();
                    filePerLevel.add(file);
                    writer = new JsonLineWriter(new FileOutputStream(file));
                    writerPerLevel.add(writer);
                } else {
                    throw new IllegalStateException("Level : " + level + ", nb writers: " + writerPerLevel.size());
                }
                writer.addEntry(entry);
            }
        } finally {
            for (JsonLineWriter writer : writerPerLevel) {
                writer.close();
            }
        }
        return filePerLevel;
    }

    private void updateParents(ObjectNode unit, TopLevelUnitAttachmentResolver topLevelUnitAttachmentResolver) {
        String unitUploadPath = unit.get(VitamFieldsHelper.uploadPath()).asText();
        Set<String> parentUnitIds = topLevelUnitAttachmentResolver.getParentUnits(unitUploadPath);
        if (CollectionUtils.isNotEmpty(parentUnitIds)) {
            unit.set(VitamFieldsHelper.unitups(), JsonHandler.createStringArrayNode(parentUnitIds));
        }
    }

    private void bulkWriteObjectGroups(File ogFile) throws IOException {
        if (!ogFile.exists()) {
            LOGGER.info("No object group to insert");
            return;
        }

        try (
            JsonLineGenericIterator<ObjectNode> ogIterator = new JsonLineGenericIterator<>(
                new FileInputStream(ogFile),
                new TypeReference<>() {}
            )
        ) {
            Iterators.partition(ogIterator, BULK_SIZE).forEachRemaining(objectGroups -> {
                try {
                    metadataRepository.saveObjectGroups(objectGroups);
                } catch (CollectInternalException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private static void writeObjectGroupToTemporaryFile(
        JsonLineWriter objectGroupsToWriteWriter,
        ObjectGroupResponse objectGroup
    ) throws IOException {
        objectGroupsToWriteWriter.addEntry(getSerializationObjectMapper().convertValue(objectGroup, JsonNode.class));
    }

    private static void writeUnitToTemporaryFile(JsonLineWriter unitToWriteWriter, int level, ArchiveUnitModel unit)
        throws IOException {
        JsonNode unitJson = getSerializationObjectMapper().convertValue(unit, JsonNode.class);
        unitToWriteWriter.addEntry(new JsonLineModel(unit.getId(), level, unitJson));
    }

    private DigestWithSize writeObjectToWorkspace(String batchId, File fileToWrite, String fileName)
        throws IOException, CollectInternalException {
        try (
            BoundedInputStream countingInputStream = BoundedInputStream.builder()
                .setInputStream(new FileInputStream(fileToWrite))
                .get()
        ) {
            String digest = collectService.pushStreamToWorkspace(
                batchId,
                countingInputStream,
                CONTENT_FOLDER.concat(File.separator).concat(fileName)
            );

            return new DigestWithSize(digest, countingInputStream.getCount());
        }
    }

    private void bulkUpdateUnits(String transactionId, File jsonlMetadataFile)
        throws IOException, CollectInternalException {
        if (jsonlMetadataFile != null) {
            try (InputStream is = new FileInputStream(jsonlMetadataFile)) {
                metadataService.updateUnitsWithJsonlMetadataFile(transactionId, is);
            }
        }
    }

    public void moveObjectsFromBatchToTransaction(String batchId, String transactionId)
        throws CollectInternalException {
        // Move files from batch folder to transaction folder using bulkMove
        try (WorkspaceClient workspaceClient = WorkspaceCollectClientFactory.getInstance().getClient()) {
            if (
                workspaceClient.isExistingContainer(batchId) &&
                workspaceClient.isExistingFolder(batchId, CONTENT_FOLDER)
            ) {
                // Get list of files in the batch folder
                RequestResponse<Map<String, FileParams>> filesResponse = workspaceClient.getFilesWithParamsFromFolder(
                    batchId,
                    CONTENT_FOLDER
                );
                if (filesResponse.isOk()) {
                    Map<String, FileParams> files =
                        ((RequestResponseOK<Map<String, FileParams>>) filesResponse).getFirstResult();
                    if (!files.isEmpty()) {
                        // Create transaction container and folder if they don't exist
                        if (!workspaceClient.isExistingContainer(transactionId)) {
                            workspaceClient.createContainer(transactionId);
                        }
                        if (!workspaceClient.isExistingFolder(transactionId, CONTENT_FOLDER)) {
                            workspaceClient.createFolder(transactionId, CONTENT_FOLDER);
                        }

                        // Create bulk move entries
                        List<BulkMoveEntry> entries = new ArrayList<>();
                        for (String filePath : files.keySet()) {
                            String fullPath = CONTENT_FOLDER + File.separator + filePath;
                            entries.add(new BulkMoveEntry(fullPath, fullPath));

                            // Log the move operation
                            LOGGER.info(
                                "Moving file from batch {} to transaction {}: {} -> {}",
                                batchId,
                                transactionId,
                                fullPath,
                                fullPath
                            );
                        }

                        // Move files from batch folder to transaction folder

                        BulkMoveRequest bulkMoveRequest = new BulkMoveRequest(entries);
                        workspaceClient.bulkMove(batchId, transactionId, bulkMoveRequest);
                        LOGGER.info("Successfully moved {} files from batch to transaction container", entries.size());
                    }
                }

                // Delete batch container
                workspaceClient.deleteContainer(batchId, true);
            }
        } catch (ContentAddressableStorageException e) {
            throw new CollectInternalException(
                "Error when moving files from batch folder to transaction folder:" + e.getMessage(),
                e
            );
        }
    }

    private record TitleAndDescriptionLevel(String title, String descriptionLevel) {}

    public record DigestWithSize(String digest, long size) {}

    private record PreprocessingResult(File unitsToWriteFile, File objectGroupsToWriteFile, File metadataFile) {}

    @FunctionalInterface
    private interface TopLevelUnitAttachmentResolver {
        Set<String> getParentUnits(String uploadPath);

        static TopLevelUnitAttachmentResolver empty() {
            return uploadPath -> Collections.emptySet();
        }

        static TopLevelUnitAttachmentResolver singleton(String parentId) {
            Set<String> parentUnits = Collections.singleton(parentId);
            return uploadPath -> parentUnits;
        }
    }
}

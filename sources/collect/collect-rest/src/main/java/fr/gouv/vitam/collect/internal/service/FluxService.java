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
package fr.gouv.vitam.collect.internal.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.collect.internal.dto.FileInfoDto;
import fr.gouv.vitam.collect.internal.dto.ObjectGroupDto;
import fr.gouv.vitam.collect.internal.dto.ProjectDto;
import fr.gouv.vitam.collect.internal.exception.CollectException;
import fr.gouv.vitam.collect.internal.helpers.builders.ObjectMapperBuilder;
import fr.gouv.vitam.collect.internal.server.CollectConfiguration;
import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.administration.DataObjectVersionType;
import fr.gouv.vitam.common.model.objectgroup.DbObjectGroupModel;
import fr.gouv.vitam.common.model.unit.ArchiveUnitModel;
import fr.gouv.vitam.common.storage.compress.ArchiveEntryInputStream;
import fr.gouv.vitam.common.storage.compress.VitamArchiveStreamFactory;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.client.WorkspaceType;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

import static fr.gouv.vitam.common.model.RequestResponseOK.TAG_RESULTS;

public class FluxService {

    private static final String LINUX_PATH_SEPARATOR = "/";
    private static final String OPI = "#opi";
    private static final String ID = "#id";
    private static final String MGT = "#management";
    private static final String TITLE = "Title";
    private static final String DESCRIPTION_LEVEL = "DescriptionLevel";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(FluxService.class);
    private static final ObjectMapper objectMapper = ObjectMapperBuilder.buildObjectMapper();
    private final TransactionService transactionService;
    private final ProjectService projectService;
    private final CollectService collectService;

    public FluxService(TransactionService transactionService, ProjectService projectService,
        CollectService collectService, CollectConfiguration collectConfiguration) {
        this.transactionService = transactionService;
        this.projectService = projectService;
        this.collectService = collectService;
        WorkspaceClientFactory.changeMode(collectConfiguration.getWorkspaceUrl(), WorkspaceType.COLLECT);
    }

    @VisibleForTesting
    public FluxService(TransactionService transactionService, ProjectService projectService,
        CollectService collectService, MetaDataClientFactory metaDataClientFactory) {
        this.transactionService = transactionService;
        this.projectService = projectService;
        this.collectService = collectService;
    }

    public void process(int tenantId, InputStream inputStreamObject, ProjectDto projectDto) throws CollectException {
        try (final InputStream inputStreamClosable = StreamUtils.getRemainingReadOnCloseInputStream(inputStreamObject);
            final ArchiveInputStream archiveInputStream = new VitamArchiveStreamFactory()
                .createArchiveInputStream(CommonMediaType.ZIP_TYPE, inputStreamClosable)) {
            ArchiveEntry entry;
            boolean isEmpty = true;
            // create entryInputStream to resolve the stream closed problem
            final ArchiveEntryInputStream entryInputStream = new ArchiveEntryInputStream(archiveInputStream);
            JsonNode unitRecord = null;
            HashMap<String, String> savedGuuidUnits = new HashMap<>();
            while ((entry = archiveInputStream.getNextEntry()) != null) {
                if (archiveInputStream.canReadEntryData(entry)) {
                    String fileName = Paths.get(entry.getName()).getFileName().toString();
                    AbstractMap.SimpleEntry<String, String> unitParentEntry;
                    if (entry.isDirectory()) {
                        unitParentEntry = getUnitParentByPath(savedGuuidUnits, StringUtils
                            .chop(entry.getName()));
                        unitRecord = uploadArchiveUnit(projectDto.getTransactionId(),
                            "RecordGrp", fileName,
                            unitParentEntry != null ? unitParentEntry.getValue() : null);
                        savedGuuidUnits.put(StringUtils
                            .chop(entry.getName()), unitRecord.get(ID).asText());
                    } else {
                        unitParentEntry = getUnitParentByPath(savedGuuidUnits, entry.getName());
                        JsonNode unitItem = uploadArchiveUnit(projectDto.getTransactionId(), "Item", fileName,
                            unitParentEntry.getValue());
                        uploadObjectGroup(unitItem.get(ID).asText(), fileName);
                        uploadBinary(unitItem.get(ID).asText(), unitParentEntry.getKey(), entryInputStream);
                    }
                    isEmpty = false;

                }
                entryInputStream.setClosed(false);
            }
            if (isEmpty) {
                throw new CollectException("File is empty");
            }

        } catch (IOException | ArchiveException e) {
            LOGGER.error("An error occurs when try to upload the ZIP: {}", e);
            throw new CollectException("An error occurs when try to upload the ZIP: {}");
        }
    }

    public ObjectNode uploadArchiveUnit(String transactionId, String descriptionLevel, String title, String unitParent)
        throws CollectException {
        ObjectNode unitObjectNode = JsonHandler.createObjectNode();
        unitObjectNode.put(ID, CollectService.createRequestId());
        unitObjectNode.put(OPI, transactionId);
        unitObjectNode.replace(MGT, JsonHandler.createObjectNode());
        unitObjectNode.put(TITLE, title);
        unitObjectNode.put(DESCRIPTION_LEVEL, descriptionLevel);
        if (unitParent != null) {
            unitObjectNode.put("#unitups", JsonHandler.createArrayNode().add(unitParent));
        }
        JsonNode savedUnitJsonNode = collectService.saveArchiveUnitInMetaData(unitObjectNode);
        if (savedUnitJsonNode == null) {
            throw new CollectException("Error while trying to save units");
        }
        return unitObjectNode;
    }

    public AbstractMap.SimpleEntry<String, String> getUnitParentByPath(Map<String, String> unitMap,
        String entryName) {
        int sepPos = entryName.lastIndexOf(LINUX_PATH_SEPARATOR);
        if (sepPos == -1) {
            return null;
        }
        return new AbstractMap.SimpleEntry<>(entryName.substring(0, sepPos),
            unitMap.get(entryName.substring(0, sepPos)));
    }

    public void uploadObjectGroup(String unitId, String fileName)
        throws CollectException {
        FileInfoDto fileInfoDto =
            new FileInfoDto(fileName, LocalDateUtil.getFormattedDateForMongo(LocalDateTime.now()));
        ObjectGroupDto objectGroupDto = new ObjectGroupDto(CollectService.createRequestId(), fileInfoDto);
        JsonNode unitResponse = collectService.getUnitByIdInMetaData(unitId);
        ArchiveUnitModel archiveUnitModel =
            objectMapper.convertValue(unitResponse.get(TAG_RESULTS).get(0), ArchiveUnitModel.class);
        collectService
            .saveObjectGroupInMetaData(archiveUnitModel, DataObjectVersionType.BINARY_MASTER, 1, objectGroupDto);
    }


    public void uploadBinary(String unitId, String fileUri, InputStream uploadedInputStream)
        throws CollectException {
        JsonNode unitResponse = collectService.getUnitByIdInMetaData(unitId);
        ArchiveUnitModel archiveUnitModel =
            objectMapper.convertValue(unitResponse.get(TAG_RESULTS).get(0), ArchiveUnitModel.class);
        DbObjectGroupModel dbObjectGroupModel = collectService.getDbObjectGroup(archiveUnitModel);
        collectService
            .addBinaryInfoToQualifier(dbObjectGroupModel, DataObjectVersionType.BINARY_MASTER, 1, uploadedInputStream,
                fileUri);
    }


}

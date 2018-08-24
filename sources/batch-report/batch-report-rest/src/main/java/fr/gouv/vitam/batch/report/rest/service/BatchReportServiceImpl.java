/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 *******************************************************************************/
package fr.gouv.vitam.batch.report.rest.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.client.MongoCursor;
import fr.gouv.vitam.batch.report.model.EliminationActionObjectGroupModel;
import fr.gouv.vitam.batch.report.model.EliminationActionUnitModel;
import fr.gouv.vitam.batch.report.rest.repository.EliminationActionObjectGroupRepository;
import fr.gouv.vitam.batch.report.rest.repository.EliminationActionUnitRepository;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import fr.gouv.vitam.worker.core.distribution.JsonLineWriter;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.bson.Document;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MassReportService
 */
public class BatchReportServiceImpl {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(BatchReportServiceImpl.class);
    private static final String JSON_EXTENSION = ".json";
    private EliminationActionUnitRepository eliminationActionUnitRepository;
    private EliminationActionObjectGroupRepository eliminationActionObjectGroupRepository;
    private WorkspaceClientFactory workspaceClientFactory;

    public BatchReportServiceImpl(EliminationActionUnitRepository eliminationActionUnitRepository,
        EliminationActionObjectGroupRepository eliminationActionObjectGroupRepository,
        WorkspaceClientFactory workspaceClientFactory) {
        this.eliminationActionUnitRepository = eliminationActionUnitRepository;
        this.eliminationActionObjectGroupRepository = eliminationActionObjectGroupRepository;
        this.workspaceClientFactory = workspaceClientFactory;
    }

    public void appendEliminationActionUnitReport(String processId, List<JsonNode> entries, int tenantId) {
        List<EliminationActionUnitModel> documents =
            entries.stream()
                .map(entry -> new EliminationActionUnitModel(
                    GUIDFactory.newGUID().toString(), processId, tenantId,
                    LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()),
                    entry))
                .collect(Collectors.toList());
        eliminationActionUnitRepository.bulkAppendReport(documents);
    }

    public void appendEliminationActionObjectGroupReport(String processId, List<JsonNode> entries, int tenantId) {

        List<EliminationActionObjectGroupModel> documents =
            entries.stream()
                .map(entry -> new EliminationActionObjectGroupModel(
                    GUIDFactory.newGUID().toString(), processId,
                    LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()), entry, tenantId))
                .collect(Collectors.toList());
        eliminationActionObjectGroupRepository.bulkAppendReport(documents);
    }

    public void exportEliminationActionUnitReport(String processId, String fileName, int tenantId)
        throws InvalidParseOperationException, IOException, ContentAddressableStorageServerException {
        File tempFile = File.createTempFile(fileName, JSON_EXTENSION, new File(VitamConfiguration.getVitamTmpFolder()));
        try (MongoCursor<Document> iterator =
            eliminationActionUnitRepository.findCollectionByProcessIdTenant(processId, tenantId)) {

            createFileFromMongoCursorWithDocument(tempFile, iterator);
            transferFileToWorkspace(processId, fileName, tempFile);

        } finally {
            deleteQuietly(tempFile);
        }
    }

    public void exportEliminationActionObjectGroupReport(String processId, String fileName, int tenantId)
        throws InvalidParseOperationException, ContentAddressableStorageServerException, IOException {
        File tempFile = File.createTempFile(fileName, JSON_EXTENSION, new File(VitamConfiguration.getVitamTmpFolder()));
        try (MongoCursor<Document> iterator = eliminationActionObjectGroupRepository
            .findCollectionByProcessIdTenant(processId, tenantId)) {

            createFileFromMongoCursorWithDocument(tempFile, iterator);

            transferFileToWorkspace(processId, fileName, tempFile);

        } finally {
            deleteQuietly(tempFile);
        }
    }

    private void createFileFromMongoCursorWithDocument(File tempFile, MongoCursor<Document> iterator)
        throws IOException, InvalidParseOperationException {
        try (JsonLineWriter jsonLineWriter = new JsonLineWriter(new FileOutputStream(tempFile))) {
            while (iterator.hasNext()) {
                Document document = iterator.next();
                JsonLineModel jsonLineModel =
                    JsonHandler.getFromJsonNode(JsonHandler.toJsonNode(document), JsonLineModel.class);
                jsonLineWriter.addEntry(jsonLineModel);
            }
        }
    }

    private void createFileFromMongoCursorWithString(File tempFile, MongoCursor<String> iterator)
        throws IOException {
        try (JsonLineWriter jsonLineWriter = new JsonLineWriter(new FileOutputStream(tempFile))) {
            while (iterator.hasNext()) {
                String objectGroupId = iterator.next();
                JsonLineModel jsonLineModel = getJsonLineModelWithString(objectGroupId);
                jsonLineWriter.addEntry(jsonLineModel);
            }
        }
    }

    private JsonLineModel getJsonLineModelWithString(String next) {
        return new JsonLineModel(next, null, null);
    }

    private void transferFileToWorkspace(String processId, String fileName, File tempFile)
        throws IOException, ContentAddressableStorageServerException {
        try (WorkspaceClient client = workspaceClientFactory.getClient();
            FileInputStream fileInputStream = new FileInputStream(tempFile)) {
            client.putObject(processId, fileName, fileInputStream);
        }
    }

    public void exportDistinctObjectGroupOfDeletedUnits(String processId, String filename, int tenantId)
        throws IOException, ContentAddressableStorageServerException {
        File tempFile = File.createTempFile(filename, JSON_EXTENSION, new File(VitamConfiguration.getVitamTmpFolder()));
        try (MongoCursor<String> iterator = eliminationActionUnitRepository
            .distinctObjectGroupOfDeletedUnits(processId, tenantId)) {

            createFileFromMongoCursorWithString(tempFile, iterator);
            transferFileToWorkspace(processId, filename, tempFile);
        } finally {
            deleteQuietly(tempFile);
        }
    }

    public void deleteEliminationUnitByProcessId(String processId, int tenantId) {
        eliminationActionUnitRepository.deleteReportByIdAndTenant(processId, tenantId);
    }

    public void deleteEliminationObjectGroupByIdAndTenant(String processId, int tenantId) {
        eliminationActionObjectGroupRepository.deleteReportByIdAndTenant(processId, tenantId);
    }

    private void deleteQuietly(File tempFile) {
        if (!tempFile.delete()) {
            LOGGER.warn("Could not delete file " + tempFile.getAbsolutePath());
        }
    }
}

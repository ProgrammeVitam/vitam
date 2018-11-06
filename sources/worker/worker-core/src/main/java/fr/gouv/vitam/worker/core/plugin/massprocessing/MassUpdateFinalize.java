/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.worker.core.plugin.massprocessing;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageAlreadyExistsClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

import javax.ws.rs.core.Response;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.json.JsonHandler.createJsonGenerator;

/**
 * Mass update finalize.
 */
public class MassUpdateFinalize extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ActionHandler.class);

    /**
     * MASS_UPDATE_FINALIZE
     */
    private static final String MASS_UPDATE_FINALIZE = "MASS_UPDATE_FINALIZE";

    /**
     * DISTRIBUTION_LOCAL_REPORTS_RANK
     */
    private static final int DISTRIBUTION_LOCAL_REPORTS_RANK = 0;

    /**
     * Default strategy identifier
     */
    private static final String STRATEGY_ID = "default";
    private static final String RESULTS = "$results";
    private static final String REPORT = "report";
    private static final String FILE_NAME_FORMAT = "%s_%s_%s.json";

    private StorageClientFactory storageClientFactory;
    private WorkspaceClientFactory workspaceClientFactory;

    /**
     * Constructor.
     */
    public MassUpdateFinalize() {
        this(StorageClientFactory.getInstance(), WorkspaceClientFactory.getInstance());
    }

    @VisibleForTesting
    public MassUpdateFinalize(StorageClientFactory storageClientFactory,
        WorkspaceClientFactory workspaceClientFactory) {
        this.storageClientFactory = storageClientFactory;
        this.workspaceClientFactory = workspaceClientFactory;
    }

    /**
     * Execute an action
     * @param param {@link WorkerParameters}
     * @param handler the handlerIo
     * @return CompositeItemStatus:response contains a list of functional message and status code
     * @throws ProcessingException if an error is encountered when executing the action
     * @throws ContentAddressableStorageServerException if a storage exception is encountered when executing the action
     */
    @Override public ItemStatus execute(WorkerParameters param, HandlerIO handler)
        throws ProcessingException, ContentAddressableStorageServerException {

        final ItemStatus itemStatus = new ItemStatus(MASS_UPDATE_FINALIZE);

        final String distribReportConatiner = (String) handler.getInput(DISTRIBUTION_LOCAL_REPORTS_RANK);
        try (final WorkspaceClient workspaceClient = workspaceClientFactory.getClient()) {
            List<URI> uris =
                JsonHandler
                    .getFromStringAsTypeRefence(workspaceClient
                        .getListUriDigitalObjectFromFolder(param.getContainerName(), distribReportConatiner)
                        .toJsonNode().get(RESULTS).get(0).toString(), new TypeReference<List<URI>>() {
                    });
            if (uris.isEmpty()) {
                itemStatus.increment(StatusCode.KO);
                return new ItemStatus(MASS_UPDATE_FINALIZE).setItemsStatus(MASS_UPDATE_FINALIZE, itemStatus);
            }
            Predicate<String> predicate = p -> p.toLowerCase().endsWith("success.json");
            Map<Boolean, List<String>> listing = uris.stream().map(String::valueOf)
                .collect(Collectors.partitioningBy(predicate));

            String containerName = String.format("%s_%s", ParameterHelper.getTenantParameter(),
                DataCategory.DISTRIBUTIONREPORTS.getFolder());

            String successReportName =
                String.format(FILE_NAME_FORMAT, param.getRequestId(), REPORT, "success");
            final File distribReports = handler.getNewLocalFile(successReportName);
            constructReportFile(param, distribReportConatiner, workspaceClient, listing.get(Boolean.TRUE),
                containerName, distribReports, successReportName);

            String errorReportName =
                String.format(FILE_NAME_FORMAT, param.getRequestId(), REPORT, "error");
            final File distribReportsKO = handler.getNewLocalFile(errorReportName);
            constructReportFile(param, distribReportConatiner, workspaceClient, listing.get(Boolean.FALSE),
                containerName, distribReportsKO, errorReportName);

            itemStatus.increment(StatusCode.OK);

        } catch (InvalidParseOperationException | ContentAddressableStorageServerException | ContentAddressableStorageNotFoundException | IOException e) {
            LOGGER.error(e);
            itemStatus.increment(StatusCode.FATAL);
        }
        return new ItemStatus(MASS_UPDATE_FINALIZE).setItemsStatus(MASS_UPDATE_FINALIZE, itemStatus);
    }

    private void constructReportFile(WorkerParameters param, String distribReportConatiner,
        WorkspaceClient workspaceClient, List<String> files, String containerName, File distribReports, String reportName)
        throws IOException, ContentAddressableStorageServerException, ContentAddressableStorageNotFoundException {
        if (files.isEmpty()) {
            return;
        }
        try {
            Response response;
            try (BufferedOutputStream buffOut = new BufferedOutputStream(new FileOutputStream(distribReports));
                JsonGenerator jsonGenerator = createJsonGenerator(buffOut)) {
                jsonGenerator.writeStartArray();
                for (int i = 0; i < files.size(); i++) {
                    response = workspaceClient
                        .getObject(param.getContainerName(), distribReportConatiner + "/" + files.get(i));
                    try (BufferedReader br = new BufferedReader(
                        new InputStreamReader((InputStream) response.getEntity()))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            jsonGenerator.writeRawValue(line);
                        }
                    }
                }
                jsonGenerator.writeEndArray();
            } finally {
                try (InputStream inputStream = new BufferedInputStream(new FileInputStream(distribReports))) {
                    workspaceClient.createContainer(containerName);
                    workspaceClient.putObject(containerName, reportName, inputStream);
                } catch (ContentAddressableStorageAlreadyExistException e) {
                    LOGGER.error(e);
                }
            }
            try (final StorageClient storageClient = storageClientFactory.getClient()) {
                final ObjectDescription description = new ObjectDescription();
                description.setWorkspaceContainerGUID(containerName);
                description.setWorkspaceObjectURI(reportName);
                storageClient
                    .storeFileFromWorkspace(STRATEGY_ID, DataCategory.DISTRIBUTIONREPORTS, reportName,
                        description);
            } catch (StorageAlreadyExistsClientException | StorageNotFoundClientException | StorageServerClientException e) {
                LOGGER.error(e);
            } finally {
                workspaceClient.deleteContainer(containerName, true);
            }
        } catch (ContentAddressableStorageNotFoundException e) {
            LOGGER.error(e);
            throw e;
        }
    }

}

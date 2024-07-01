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
package fr.gouv.vitam.worker.core.plugin;

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.batch.report.client.BatchReportClient;
import fr.gouv.vitam.batch.report.client.BatchReportClientFactory;
import fr.gouv.vitam.batch.report.model.Report;
import fr.gouv.vitam.batch.report.model.ReportBody;
import fr.gouv.vitam.batch.report.model.ReportType;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageAlreadyExistsClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.worker.core.exception.ProcessingStatusException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

import java.util.List;

public abstract class CommonReportService<T> {

    public static final String JSONL_EXTENSION = ".jsonl";
    public static final String WORKSPACE_REPORT_URI = "report.jsonl";

    private final BatchReportClientFactory batchReportClientFactory;
    private final WorkspaceClientFactory workspaceClientFactory;
    private final StorageClientFactory storageClientFactory;
    private final ReportType reportType;

    protected CommonReportService(ReportType reportType) {
        this(
            reportType,
            BatchReportClientFactory.getInstance(),
            WorkspaceClientFactory.getInstance(),
            StorageClientFactory.getInstance()
        );
    }

    @VisibleForTesting
    protected CommonReportService(
        ReportType reportType,
        BatchReportClientFactory batchReportClientFactory,
        WorkspaceClientFactory workspaceClientFactory,
        StorageClientFactory storageClientFactory
    ) {
        this.reportType = reportType;
        this.batchReportClientFactory = batchReportClientFactory;
        this.workspaceClientFactory = workspaceClientFactory;
        this.storageClientFactory = storageClientFactory;
    }

    public void appendEntries(String processId, List<T> entries) throws ProcessingStatusException {
        try (BatchReportClient batchReportClient = batchReportClientFactory.getClient()) {
            ReportBody<T> reportBody = new ReportBody<>(processId, reportType, entries);
            batchReportClient.appendReportEntries(reportBody);
        } catch (VitamClientInternalException e) {
            throw new ProcessingStatusException(StatusCode.FATAL, "Could not append entries into report", e);
        }
    }

    public boolean isReportWrittenInWorkspace(String processId) throws ProcessingStatusException {
        try (WorkspaceClient workspaceClient = workspaceClientFactory.getClient()) {
            return workspaceClient.isExistingObject(processId, WORKSPACE_REPORT_URI);
        } catch (ContentAddressableStorageServerException e) {
            throw new ProcessingStatusException(StatusCode.FATAL, "Could not check report existence in workspace", e);
        }
    }

    public void deleteReportFromWorkspaceIfExists(String processId) throws ProcessingStatusException {
        try (WorkspaceClient workspaceClient = workspaceClientFactory.getClient()) {
            if (workspaceClient.isExistingObject(processId, WORKSPACE_REPORT_URI)) {
                workspaceClient.deleteObject(processId, WORKSPACE_REPORT_URI);
            }
        } catch (ContentAddressableStorageServerException | ContentAddressableStorageNotFoundException e) {
            throw new ProcessingStatusException(StatusCode.FATAL, "Could not purge existing report from workspace", e);
        }
    }

    public void storeReportToWorkspace(Report reportInfo) throws ProcessingStatusException {
        try (BatchReportClient batchReportClient = batchReportClientFactory.getClient()) {
            batchReportClient.storeReportToWorkspace(reportInfo);
        } catch (VitamClientInternalException e) {
            throw new ProcessingStatusException(StatusCode.FATAL, "Could not store report", e);
        }
    }

    public void storeReportToOffers(String containerName) throws ProcessingStatusException {
        try (StorageClient storageClient = storageClientFactory.getClient()) {
            ObjectDescription description = new ObjectDescription();
            description.setWorkspaceContainerGUID(containerName);
            description.setWorkspaceObjectURI(WORKSPACE_REPORT_URI);
            storageClient.storeFileFromWorkspace(
                VitamConfiguration.getDefaultStrategy(),
                DataCategory.REPORT,
                containerName + JSONL_EXTENSION,
                description
            );
        } catch (
            StorageAlreadyExistsClientException | StorageNotFoundClientException | StorageServerClientException e
        ) {
            throw new ProcessingStatusException(StatusCode.FATAL, "Could not store report to offers", e);
        }
    }

    public void cleanupReport(String processId) throws ProcessingStatusException {
        try (BatchReportClient batchReportClient = batchReportClientFactory.getClient()) {
            batchReportClient.cleanupReport(processId, reportType);
        } catch (VitamClientInternalException e) {
            throw new ProcessingStatusException(
                StatusCode.FATAL,
                "Could not cleanup report entries (" + processId + ")",
                e
            );
        }
    }
}

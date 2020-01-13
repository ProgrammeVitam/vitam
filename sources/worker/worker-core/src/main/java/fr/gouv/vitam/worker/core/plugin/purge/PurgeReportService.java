/*
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
 */
package fr.gouv.vitam.worker.core.plugin.purge;

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.batch.report.client.BatchReportClient;
import fr.gouv.vitam.batch.report.client.BatchReportClientFactory;
import fr.gouv.vitam.batch.report.model.Report;
import fr.gouv.vitam.batch.report.model.ReportBody;
import fr.gouv.vitam.batch.report.model.ReportExportRequest;
import fr.gouv.vitam.batch.report.model.ReportType;
import fr.gouv.vitam.batch.report.model.entry.PurgeObjectGroupReportEntry;
import fr.gouv.vitam.batch.report.model.entry.PurgeUnitReportEntry;
import fr.gouv.vitam.common.collection.CloseableIterator;
import fr.gouv.vitam.common.collection.CloseableIteratorUtils;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.stream.VitamAsyncInputStream;
import fr.gouv.vitam.worker.core.distribution.JsonLineIterator;
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import fr.gouv.vitam.worker.core.exception.ProcessingStatusException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

import javax.ws.rs.core.Response;
import java.util.List;

public class PurgeReportService {

    static final String OBJECT_GROUP_REPORT_JSONL = "objectGroupReport.jsonl";
    static final String DISTINCT_REPORT_JSONL = "unitObjectGroups.jsonl";
    static final String ACCESSION_REGISTER_REPORT_JSONL = "accession_register.jsonl";

    private final BatchReportClientFactory batchReportClientFactory;
    private final WorkspaceClientFactory workspaceClientFactory;

    public PurgeReportService() {
        this(
            BatchReportClientFactory.getInstance(),
            WorkspaceClientFactory.getInstance());
    }

    @VisibleForTesting
    PurgeReportService(
        BatchReportClientFactory batchReportClientFactory,
        WorkspaceClientFactory workspaceClientFactory) {
        this.batchReportClientFactory = batchReportClientFactory;
        this.workspaceClientFactory = workspaceClientFactory;
    }

    public void appendUnitEntries(String processId, List<PurgeUnitReportEntry> entries)
        throws ProcessingStatusException {

        appendEntries(processId, entries, ReportType.PURGE_UNIT);
    }

    public void appendObjectGroupEntries(String processId, List<PurgeObjectGroupReportEntry> entries)
        throws ProcessingStatusException {

        appendEntries(processId, entries, ReportType.PURGE_OBJECTGROUP);
    }

    private <T> void appendEntries(String processId, List<T> entries, ReportType reportType)
        throws ProcessingStatusException {

        try (BatchReportClient batchReportClient = batchReportClientFactory.getClient()) {
            ReportBody<T> reportBody = new ReportBody<>();
            reportBody.setProcessId(processId);
            reportBody.setReportType(reportType);
            reportBody.setEntries(entries);
            batchReportClient.appendReportEntries(reportBody);
        } catch (VitamClientInternalException e) {
            throw new ProcessingStatusException(StatusCode.FATAL, "Could not append entries into report", e);
        }
    }

    public CloseableIterator<String> exportDistinctObjectGroups(String processId) throws ProcessingStatusException {

        try (BatchReportClient batchReportClient = batchReportClientFactory.getClient()) {

            batchReportClient.generatePurgeDistinctObjectGroupInUnitReport(processId,
                new ReportExportRequest(DISTINCT_REPORT_JSONL));

        } catch (VitamClientInternalException e) {
            throw new ProcessingStatusException(StatusCode.FATAL,
                "Could not generate distinct object group report for deleted units to workspace", e);
        }

        try (WorkspaceClient workspaceClient = workspaceClientFactory.getClient()) {

            Response reportResponse = workspaceClient.getObject(processId, DISTINCT_REPORT_JSONL);
            JsonLineIterator jsonLineIterator = new JsonLineIterator(new VitamAsyncInputStream(reportResponse));

            return CloseableIteratorUtils.map(jsonLineIterator, JsonLineModel::getId);

        } catch (ContentAddressableStorageServerException | ContentAddressableStorageNotFoundException e) {
            throw new ProcessingStatusException(StatusCode.FATAL, "Could not load report from workspace", e);
        }
    }

    public void exportAccessionRegisters(String processId) throws ProcessingStatusException {

        try (BatchReportClient batchReportClient = batchReportClientFactory.getClient()) {

            batchReportClient.generatePurgeAccessionRegisterReport(
                processId,
                new ReportExportRequest(ACCESSION_REGISTER_REPORT_JSONL));

        } catch (VitamClientInternalException e) {
            throw new ProcessingStatusException(StatusCode.FATAL,
                "Could not generate purge accession register reports (" + processId + ")", e);
        }
    }

    public void cleanupReport(String processId) throws ProcessingStatusException {
        try (BatchReportClient batchReportClient = batchReportClientFactory.getClient()) {
            batchReportClient.cleanupReport(processId, ReportType.PURGE_UNIT);
            batchReportClient.cleanupReport(processId, ReportType.PURGE_OBJECTGROUP);
        } catch (VitamClientInternalException e) {
            throw new ProcessingStatusException(StatusCode.FATAL,
                "Could not cleanup purge reports (" + processId + ")", e);
        }
    }
}

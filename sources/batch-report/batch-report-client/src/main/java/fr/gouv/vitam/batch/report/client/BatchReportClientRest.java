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
package fr.gouv.vitam.batch.report.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.batch.report.exception.BatchReportException;
import fr.gouv.vitam.batch.report.model.Report;
import fr.gouv.vitam.batch.report.model.ReportBody;
import fr.gouv.vitam.batch.report.model.ReportExportRequest;
import fr.gouv.vitam.batch.report.model.ReportType;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client.DefaultClient;
import fr.gouv.vitam.common.client.VitamClientFactoryInterface;
import fr.gouv.vitam.common.client.VitamRequestBuilder;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ExtractedMetadata;
import fr.gouv.vitam.common.thread.VitamThreadUtils;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

import static fr.gouv.vitam.common.client.VitamRequestBuilder.delete;
import static fr.gouv.vitam.common.client.VitamRequestBuilder.get;
import static fr.gouv.vitam.common.client.VitamRequestBuilder.post;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static javax.ws.rs.core.Response.Status.fromStatusCode;

public class BatchReportClientRest extends DefaultClient implements BatchReportClient {

    private static final String APPEND = "append";
    private static final String STORE_TO_WORKSPACE = "storeToWorkspace";
    private static final String CLEANUP = "cleanup";
    private static final String EXPORT_PURGE_UNIT_DISTINCT_OBJECTGROUPS = "purge_unit/objectgroup_export/";
    private static final String EXPORT_PURGE_ACCESSION_REGISTER = "purge/accession_register_export/";
    private static final String UNITS_AND_PROGENY_INVALIDATION = "/computedInheritedRulesInvalidation/";
    private static final String STORE_EXTRACTED_METADATA_FOR_AU = "/storeExtractedMetadataForAu/";
    private static final String CREATE_DISTRIBUTION_FILE_FOR_AU = "/createExtractedMetadataDistributionFileForAu/";

    @VisibleForTesting
    BatchReportClientRest(VitamClientFactoryInterface<?> factory) {
        super(factory);
    }

    @Override
    public void generatePurgeDistinctObjectGroupInUnitReport(String processId, ReportExportRequest reportExportRequest)
        throws VitamClientInternalException {
        ParametersChecker.checkParameter("processId should be filled", processId);

        VitamRequestBuilder request = post()
            .withPath(EXPORT_PURGE_UNIT_DISTINCT_OBJECTGROUPS + processId)
            .withBody(reportExportRequest)
            .withHeader(GlobalDataRest.X_TENANT_ID, VitamThreadUtils.getVitamSession().getTenantId())
            .withJson();
        try (Response response = make(request)) {
            check(response);
        }
    }

    @Override
    public void appendReportEntries(ReportBody reportBody) throws VitamClientInternalException {
        VitamRequestBuilder request = post()
            .withPath(APPEND)
            .withBody(reportBody)
            .withHeader(GlobalDataRest.X_TENANT_ID, VitamThreadUtils.getVitamSession().getTenantId())
            .withJson();
        try (Response response = make(request)) {
            check(response);
        }
    }

    @Override
    public void storeReportToWorkspace(Report reportInfo) throws VitamClientInternalException {
        VitamRequestBuilder request = post()
            .withPath(STORE_TO_WORKSPACE)
            .withBody(reportInfo)
            .withHeader(GlobalDataRest.X_TENANT_ID, VitamThreadUtils.getVitamSession().getTenantId())
            .withJson();
        try (Response response = make(request)) {
            check(response);
        }
    }

    @Override
    public void exportUnitsToInvalidate(String processId, ReportExportRequest reportExportRequest)
        throws VitamClientInternalException {
        ParametersChecker.checkParameter("processId parameter should be filled", processId);

        VitamRequestBuilder request = post()
            .withPath(UNITS_AND_PROGENY_INVALIDATION + processId)
            .withBody(reportExportRequest)
            .withHeader(GlobalDataRest.X_TENANT_ID, VitamThreadUtils.getVitamSession().getTenantId())
            .withJson();
        try (Response response = make(request)) {
            check(response);
        }
    }

    @Override
    public void generatePurgeAccessionRegisterReport(String processId, ReportExportRequest reportExportRequest)
        throws VitamClientInternalException {
        ParametersChecker.checkParameter("processId should be filled", processId);

        VitamRequestBuilder request = post()
            .withPath(EXPORT_PURGE_ACCESSION_REGISTER + processId)
            .withBody(reportExportRequest)
            .withHeader(GlobalDataRest.X_TENANT_ID, VitamThreadUtils.getVitamSession().getTenantId())
            .withJson();
        try (Response response = make(request)) {
            check(response);
        }
    }

    @Override
    public void cleanupReport(String processId, ReportType reportType) throws VitamClientInternalException {
        ParametersChecker.checkParameter("processId and reportType should be filled", processId, reportType);

        VitamRequestBuilder request = delete()
            .withPath(CLEANUP + "/" + reportType + "/" + processId)
            .withHeader(GlobalDataRest.X_TENANT_ID, VitamThreadUtils.getVitamSession().getTenantId())
            .withAccept(MediaType.APPLICATION_JSON_TYPE);
        try (Response response = make(request)) {
            check(response);
        }
    }

    @Override
    public void storeExtractedMetadataForAu(List<ExtractedMetadata> extractedMetadata)
        throws VitamClientInternalException {
        VitamRequestBuilder request = post()
            .withPath(STORE_EXTRACTED_METADATA_FOR_AU)
            .withBody(extractedMetadata)
            .withHeader(GlobalDataRest.X_TENANT_ID, VitamThreadUtils.getVitamSession().getTenantId())
            .withJson();
        try (Response response = make(request)) {
            check(response);
        }
    }

    @Override
    public void createExtractedMetadataDistributionFileForAu(String processId) throws Exception {
        VitamRequestBuilder request = get()
            .withPath(CREATE_DISTRIBUTION_FILE_FOR_AU + processId)
            .withHeader(GlobalDataRest.X_TENANT_ID, VitamThreadUtils.getVitamSession().getTenantId())
            .withJson();
        try (Response response = make(request)) {
            check(response);
        }
    }

    @Override
    public JsonNode readComputedDetailsFromReport(ReportType reportType, String processId) {
        VitamRequestBuilder request = get()
            .withPath("/readComputedDetailsFromReport")
            .withHeader(GlobalDataRest.X_TENANT_ID, VitamThreadUtils.getVitamSession().getTenantId())
            .withBody(new ReportBody(processId, reportType, null))
            .withJson();
        try (Response response = make(request)) {
            check(response);
            return JsonHandler.getFromString(response.readEntity(String.class));
        } catch (final VitamClientInternalException | InvalidParseOperationException e) {
            throw new BatchReportException(e);
        }
    }

    private void check(Response response) throws VitamClientInternalException {
        Response.Status status = response.getStatusInfo().toEnum();
        if (SUCCESSFUL.equals(status.getFamily())) {
            return;
        }

        throw new VitamClientInternalException(
            String.format(
                "Error with the response, get status: '%d' and reason '%s'.",
                response.getStatus(),
                fromStatusCode(response.getStatus()).getReasonPhrase()
            )
        );
    }
}

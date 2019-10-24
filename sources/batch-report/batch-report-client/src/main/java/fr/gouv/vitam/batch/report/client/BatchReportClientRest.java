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
package fr.gouv.vitam.batch.report.client;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.batch.report.model.Report;
import fr.gouv.vitam.batch.report.model.ReportBody;
import fr.gouv.vitam.batch.report.model.ReportExportRequest;
import fr.gouv.vitam.batch.report.model.ReportType;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client.VitamClientFactoryInterface;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.external.client.DefaultClient;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.thread.VitamThreadUtils;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;

/**
 * BatchReportClientRest
 */
public class BatchReportClientRest extends DefaultClient implements BatchReportClient {

    private static final String APPEND = "append";
    private static final String STORE = "store";
    private static final String CLEANUP = "cleanup";
    private static final String EXPORT_PURGE_UNIT_DISTINCT_OBJECTGROUPS =
        "purge_unit/objectgroup_export/";
    private static final String EXPORT_PURGE_ACCESSION_REGISTER =
        "purge/accession_register_export/";
    private static final String UNITS_AND_PROGENY_INVALIDATION = "/computedInheritedRulesInvalidation/";
    private static final String EMPTY_BODY_ERROR_MESSAGE = "Body should be filled";

    /**
     * Constructor using given scheme (http)
     *
     * @param factory The client factory
     */
    public BatchReportClientRest(VitamClientFactoryInterface<?> factory) {
        super(factory);
    }

    @Override
    public void generatePurgeDistinctObjectGroupInUnitReport(String processId,
        ReportExportRequest reportExportRequest) throws VitamClientInternalException {

        ParametersChecker.checkParameter("processId and reportExportRequest should be filled", processId,
            reportExportRequest);

        httpPost(EXPORT_PURGE_UNIT_DISTINCT_OBJECTGROUPS + processId, reportExportRequest);
    }

    @Override
    public void appendReportEntries(ReportBody reportBody)
        throws VitamClientInternalException {
        ParametersChecker.checkParameter(EMPTY_BODY_ERROR_MESSAGE, reportBody);

        JsonNode body;
        try {
            body = JsonHandler.toJsonNode(reportBody);
        } catch (InvalidParseOperationException e) {
            throw new VitamClientInternalException(e);
        }

        httpPost(APPEND, body);
    }

    @Override
    public void storeReport(Report reportEntry) throws VitamClientInternalException {
        ParametersChecker.checkParameter(EMPTY_BODY_ERROR_MESSAGE, reportEntry);

        httpPost(STORE, reportEntry);
    }

    @Override
    public void exportUnitsToInvalidate(String processId, ReportExportRequest reportExportRequest) throws VitamClientInternalException {
        ParametersChecker.checkParameter("processId parameter should be filled", processId);
        httpPost(UNITS_AND_PROGENY_INVALIDATION + processId, reportExportRequest);
    }

    @Override
    public void generatePurgeAccessionRegisterReport(String processId,
        ReportExportRequest reportExportRequest) throws VitamClientInternalException {

        ParametersChecker.checkParameter("processId and reportExportRequest should be filled", processId,
            reportExportRequest);

        httpPost(EXPORT_PURGE_ACCESSION_REGISTER + processId, reportExportRequest);
    }

    @Override
    public void cleanupReport(String processId, ReportType reportType)
        throws VitamClientInternalException {

        ParametersChecker.checkParameter("processId and reportType should be filled", processId, reportType);

        httpDelete(CLEANUP + "/" + reportType + "/" + processId);
    }

    private void httpPost(String path, Object body)
        throws VitamClientInternalException {
        httpRequest(HttpMethod.POST, path, body);
    }

    private void httpDelete(String path)
        throws VitamClientInternalException {
        httpRequest(HttpMethod.DELETE, path, null);
    }

    private void httpRequest(String httpMethod, String path, Object body) throws VitamClientInternalException {

        Response response = null;
        try {
            response = performRequest(httpMethod, path, body);
            if(response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
                throw new VitamClientInternalException("Batch report server returned failure status " + response.getStatusInfo().getStatusCode());
            }
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    private Response performRequest(String httpMethod, String path, Object body) throws VitamClientInternalException {
        int tenantId = VitamThreadUtils.getVitamSession().getTenantId();

        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(GlobalDataRest.X_TENANT_ID, tenantId);
        if (body == null) {
            return performRequest(httpMethod, path, headers, MediaType.APPLICATION_JSON_TYPE);
        }

        return performRequest(httpMethod, path, headers, body, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);

    }
}

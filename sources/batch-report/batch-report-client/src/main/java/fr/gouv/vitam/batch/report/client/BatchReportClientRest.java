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
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.thread.VitamThreadUtils;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * BatchReportClientRest
 */
public class BatchReportClientRest extends DefaultClient implements BatchReportClient {

    private static final String APPEND = "append";
    private static final String STORE = "store";
    private static final String CLEANUP = "cleanup";
    private static final String EXPORT_ELIMINATION_ACTION_UNIT_DISTINCT_OBJECTGROUPS =
        "elimination_action_unit/objectgroup_export/";
    private static final String EXPORT_ELIMINATION_ACTION_ACCESSION_REGISTER =
        "elimination_action/accession_register_export/";
    private static final String UNITS_AND_PROGENY_INVALIDATION = "/computeInheritedRulesInvalidation/";
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
    public RequestResponse<JsonNode> generateEliminationActionDistinctObjectGroupInUnitReport(String processId,
        ReportExportRequest reportExportRequest) throws VitamClientInternalException {

        ParametersChecker.checkParameter("processId and reportExportRequest should be filled", processId,
            reportExportRequest);

        return httpPost(EXPORT_ELIMINATION_ACTION_UNIT_DISTINCT_OBJECTGROUPS + processId, reportExportRequest);
    }

    @Override
    public RequestResponse<JsonNode> appendReportEntries(ReportBody reportBody)
        throws VitamClientInternalException {
        ParametersChecker.checkParameter(EMPTY_BODY_ERROR_MESSAGE, reportBody);

        JsonNode body;
        try {
            body = JsonHandler.toJsonNode(reportBody);
        } catch (InvalidParseOperationException e) {
            throw new VitamClientInternalException(e);
        }

        return httpPost(APPEND, body);
    }

    @Override
    public RequestResponse<JsonNode> storeReport(Report reportEntry) throws VitamClientInternalException {
        ParametersChecker.checkParameter(EMPTY_BODY_ERROR_MESSAGE, reportEntry);

        return httpPost(STORE, reportEntry);
    }

    @Override
    public RequestResponse<JsonNode> saveUnitsAndProgeny(String processId, List<String> unitsId) throws VitamClientInternalException {
        ParametersChecker.checkParameter(EMPTY_BODY_ERROR_MESSAGE, unitsId);

        return httpPost(UNITS_AND_PROGENY_INVALIDATION + processId, unitsId);
    }

    @Override
    public RequestResponse<JsonNode> deleteUnitsAndProgeny(String processId) throws VitamClientInternalException {
        ParametersChecker.checkParameter(EMPTY_BODY_ERROR_MESSAGE, processId);

        return httpRequest(HttpMethod.DELETE, UNITS_AND_PROGENY_INVALIDATION + processId, null);
    }

    @Override
    public JsonNode getUnitsToInvalidate(String processId) throws VitamClientInternalException {
        ParametersChecker.checkParameter("processId parameter should be filled", processId);

        Response response = performRequest(HttpMethod.GET, UNITS_AND_PROGENY_INVALIDATION + processId, null);
        return response.readEntity(JsonNode.class);
    }

    @Override
    public RequestResponse<JsonNode> generateEliminationActionAccessionRegisterReport(String processId,
        ReportExportRequest reportExportRequest) throws VitamClientInternalException {

        ParametersChecker.checkParameter("processId and reportExportRequest should be filled", processId,
            reportExportRequest);

        return httpPost(EXPORT_ELIMINATION_ACTION_ACCESSION_REGISTER + processId, reportExportRequest);
    }

    @Override
    public RequestResponse<JsonNode> cleanupReport(String processId, ReportType reportType)
        throws VitamClientInternalException {

        ParametersChecker.checkParameter("processId and reportType should be filled", processId, reportType);

        return httpDelete(CLEANUP + "/" + reportType + "/" + processId);
    }

    private RequestResponse<JsonNode> httpPost(String path, Object body)
        throws VitamClientInternalException {
        return httpRequest(HttpMethod.POST, path, body);
    }

    private RequestResponse<JsonNode> httpDelete(String path)
        throws VitamClientInternalException {
        return httpRequest(HttpMethod.DELETE, path, null);
    }

    private RequestResponse<JsonNode> httpRequest(String httpMethod, String path, Object body) throws VitamClientInternalException {

        Response response = null;
        try {
            response = performRequest(httpMethod, path, body);
            return RequestResponse.parseFromResponse(response);
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

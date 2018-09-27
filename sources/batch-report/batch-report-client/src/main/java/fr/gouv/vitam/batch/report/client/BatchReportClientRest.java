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
import fr.gouv.vitam.batch.report.model.ReportExportRequest;
import fr.gouv.vitam.batch.report.model.ReportBody;
import fr.gouv.vitam.batch.report.model.ReportType;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client.VitamClientFactoryInterface;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.external.client.DefaultClient;
import fr.gouv.vitam.common.model.RequestResponse;
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
    private static final String CLEANUP = "cleanup";
    public static final String EXPORT_UNIT = "elimination_action_unit/unit_export/";
    public static final String EXPORT_OBJECTGROUP = "elimination_action_objectgroup/objectgroup_export/";
    public static final String EXPORT_DISTINCT_OBJECTGROUP = "elimination_action_unit/objectgroup_export/";



    /**
     * Constructor using given scheme (http)
     *
     * @param factory The client factory
     */
    public BatchReportClientRest(VitamClientFactoryInterface<?> factory) {
        super(factory);
    }

    @Override
    public RequestResponse<JsonNode> generateEliminationActionUnitReport(String processId,
        ReportExportRequest reportExportRequest) throws VitamClientInternalException {
        Response response = null;
        try {
            ParametersChecker.checkParameter("processId and reportExportRequest should be filled",
                processId, reportExportRequest);
            Integer tenantId = VitamThreadUtils.getVitamSession().getTenantId();

            final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
            headers.add(GlobalDataRest.X_TENANT_ID, tenantId);
            response =
                performRequest(HttpMethod.POST, EXPORT_UNIT + processId, headers,
                    reportExportRequest,
                    MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);
            return RequestResponse.parseFromResponse(response);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<JsonNode> generateEliminationActionObjectGroupReport(String processId,
        ReportExportRequest reportExportRequest)
        throws VitamClientInternalException {
        Response response = null;
        try {
            ParametersChecker.checkParameter("processId and should be filled", processId,
                reportExportRequest);
            Integer tenantId = VitamThreadUtils.getVitamSession().getTenantId();

            final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
            headers.add(GlobalDataRest.X_TENANT_ID, tenantId);
            response =
                performRequest(HttpMethod.POST, EXPORT_OBJECTGROUP + processId, headers,
                    reportExportRequest,
                    MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);
            return RequestResponse.parseFromResponse(response);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<JsonNode> generateEliminationActionDistinctObjectGroupInUnitReport(String processId,
        String status,
        ReportExportRequest reportExportRequest) throws VitamClientInternalException {
        Response response = null;
        try {
            ParametersChecker.checkParameter("status should be filled", status);
            Integer tenantId = VitamThreadUtils.getVitamSession().getTenantId();

            final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
            headers.add(GlobalDataRest.X_TENANT_ID, tenantId);
            response =
                performRequest(HttpMethod.POST, EXPORT_DISTINCT_OBJECTGROUP + processId, headers,
                    reportExportRequest,
                    MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);
            return RequestResponse.parseFromResponse(response);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<JsonNode> appendReportEntries(ReportBody reportBody)
        throws VitamClientInternalException {
        Response response = null;
        try {
            ParametersChecker.checkParameter("Body should be filled", reportBody);
            int tenantId = VitamThreadUtils.getVitamSession().getTenantId();

            final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
            headers.add(GlobalDataRest.X_TENANT_ID, tenantId);
            response =
                performRequest(HttpMethod.POST, APPEND, headers, reportBody,
                    MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);
            return RequestResponse.parseFromResponse(response);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<JsonNode> cleanupReport(String processId, ReportType reportType)
        throws VitamClientInternalException {
        Response response = null;
        try {
            ParametersChecker.checkParameter("ReportType should be filled", reportType);
            int tenantId = VitamThreadUtils.getVitamSession().getTenantId();

            final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
            headers.add(GlobalDataRest.X_TENANT_ID, tenantId);
            response = performRequest(HttpMethod.DELETE, CLEANUP + "/" + reportType + "/" + processId, headers,
                MediaType.APPLICATION_JSON_TYPE);
            return RequestResponse.parseFromResponse(response);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }
}

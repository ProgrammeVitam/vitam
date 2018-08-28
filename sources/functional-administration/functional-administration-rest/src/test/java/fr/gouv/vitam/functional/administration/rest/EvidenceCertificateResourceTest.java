package fr.gouv.vitam.functional.administration.rest;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.single.RequestSingle;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.CertificationRequest;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

/**
 * EvidenceCertificateResource Test
 */
public class EvidenceCertificateResourceTest {


    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());
    @Mock ProcessingManagementClientFactory processingManagementClientFactory;
    @Mock ProcessingManagementClient processingManagementClient;
    @Mock WorkspaceClientFactory workspaceClientFactory;
    @Mock WorkspaceClient workspaceClient;
    @Mock LogbookOperationsClientFactory logbookOperationsClientFactory;
    @Mock LogbookOperationsClient logbookOperationsClient;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private static final int TENANT_ID = 0;


    private EvidenceCertificateResource evidenceCertificateResource;

    @Before
    public void setUp() {
        when(processingManagementClientFactory.getClient()).thenReturn(processingManagementClient);
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);
        when(logbookOperationsClientFactory.getClient()).thenReturn(logbookOperationsClient);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        GUID guid = GUIDFactory.newEventGUID(TENANT_ID);
        VitamThreadUtils.getVitamSession().setRequestId(guid);
        evidenceCertificateResource =
            new EvidenceCertificateResource(processingManagementClientFactory, logbookOperationsClientFactory,
                workspaceClientFactory);
    }



    @Test
    @RunWithCustomExecutor
    public void given_empty_query_when_export_then_return_forbidden_request() throws Exception {

        Response certificate = evidenceCertificateResource
            .exportEvidenceCertificate(new CertificationRequest(new Select().getFinalSelect(), "BinaryMaster"));
        assertThat(certificate.getStatus()).isEqualTo(Response.Status.FORBIDDEN.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void given_empty_usage_or_invalid_when_export_then_return_forbidden_request() throws Exception {

        Select select = new Select();
        select.setQuery(QueryHelper.eq("name", "dd"));

        Response certificate = evidenceCertificateResource
            .exportEvidenceCertificate(new CertificationRequest(select.getFinalSelect(), ""));
        assertThat(certificate.getStatus()).isEqualTo(Response.Status.FORBIDDEN.getStatusCode());

        certificate = evidenceCertificateResource
            .exportEvidenceCertificate(new CertificationRequest(select.getFinalSelect(), "sss"));
        assertThat(certificate.getStatus()).isEqualTo(Response.Status.FORBIDDEN.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void given_good_request_then_return_ok_request() throws Exception {

        Select select = new Select();
        select.setQuery(QueryHelper.eq("name", "dd"));
        CertificationRequest certificationRequest = new CertificationRequest(select.getFinalSelect(), "BinaryMaster");

        Response certificate = evidenceCertificateResource
            .exportEvidenceCertificate(new CertificationRequest(new Select().getFinalSelect(), "BinaryMaster"));
        assertThat(certificate.getStatus()).isEqualTo(Response.Status.FORBIDDEN.getStatusCode());

        SelectMultiQuery selectMultiQuery = new SelectMultiQuery();

        selectMultiQuery.setQuery(QueryHelper.eq("title", "test"));

        when(processingManagementClient
            .executeOperationProcess(anyString(), eq("EXPORT_EVIDENCE_CERTIFICATE"), anyString(), anyString()))
            .thenReturn(new RequestResponseOK<JsonNode>(new Select().getFinalSelect()).setHttpCode(200));
        certificate = evidenceCertificateResource.exportEvidenceCertificate(certificationRequest);
        assertThat(certificate.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void fail_when_a_related_server_is_unavailable() throws Exception {

        Select select = new Select();
        select.setQuery(QueryHelper.eq("name", "dd"));
        CertificationRequest certificationRequest = new CertificationRequest(select.getFinalSelect(), "BinaryMaster");
        willThrow(VitamClientException.class).given(workspaceClient).putObject(anyString(), any(), any());
        Response certificate = evidenceCertificateResource.exportEvidenceCertificate(certificationRequest);
        assertThat(certificate.getStatus()).isEqualTo(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());


        willThrow(LogbookClientAlreadyExistsException.class).given(logbookOperationsClient).create(any());
        certificate = evidenceCertificateResource.exportEvidenceCertificate(certificationRequest);
        assertThat(certificate.getStatus()).isEqualTo(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

}

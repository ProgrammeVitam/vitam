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

package fr.gouv.vitam.collect.external.external.service;

import fr.gouv.vitam.collect.common.enums.TransactionStatus;
import fr.gouv.vitam.collect.external.external.exception.CollectExternalInvalidRequestException;
import fr.gouv.vitam.collect.external.external.exception.CollectExternalNotFoundException;
import fr.gouv.vitam.collect.external.external.exception.CollectExternalServerSideException;
import fr.gouv.vitam.collect.internal.client.CollectInternalClient;
import fr.gouv.vitam.collect.internal.client.exceptions.CollectInternalClientException;
import fr.gouv.vitam.collect.internal.client.exceptions.CollectInternalClientInvalidRequestException;
import fr.gouv.vitam.collect.internal.client.exceptions.CollectInternalClientNotFoundException;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.ingest.external.client.IngestExternalClient;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static fr.gouv.vitam.common.model.ProcessAction.RESUME;
import static fr.gouv.vitam.logbook.common.parameters.Contexts.DEFAULT_WORKFLOW;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class CollectExternalIngestServiceTest {

    private static final int TENANT_ID = 1;
    public static final String TRANSACTION_ID = "transactionId";
    public static final String INGEST_OPERATION_ID = "ingestOperationId";

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private CollectInternalClient collectInternalClient;

    @Mock
    private IngestExternalClient ingestExternalClient;

    @Before
    public void setUp() {
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(TENANT_ID));
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
    }

    @Test
    @RunWithCustomExecutor
    public void testIngestSip_OK() throws Exception {
        // Given
        doReturn(
            new RequestResponseOK<Void>().addHeader(GlobalDataRest.X_REQUEST_ID, INGEST_OPERATION_ID).setHttpCode(200)
        )
            .when(ingestExternalClient)
            .ingest(any(), any(), eq(DEFAULT_WORKFLOW.name()), eq(RESUME.name()));

        // When
        CollectExternalIngestService service = new CollectExternalIngestService();
        service.ingestSip(collectInternalClient, ingestExternalClient, TRANSACTION_ID);

        // Then
        verify(collectInternalClient).awaitTransactionValidation(TRANSACTION_ID);
        verify(collectInternalClient).changeTransactionStatus(TRANSACTION_ID, TransactionStatus.SENDING);
        verify(collectInternalClient).downloadSIP(TRANSACTION_ID);
        verify(ingestExternalClient).ingest(any(), any(), eq(DEFAULT_WORKFLOW.name()), eq(RESUME.name()));
        verify(collectInternalClient).attachVitamOperationId(TRANSACTION_ID, INGEST_OPERATION_ID);
        verify(collectInternalClient).changeTransactionStatus(TRANSACTION_ID, TransactionStatus.SENT);
    }

    @Test
    @RunWithCustomExecutor
    public void testIngestSip_NotFound() throws Exception {
        // Given
        doThrow(new CollectInternalClientNotFoundException("not found"))
            .when(collectInternalClient)
            .awaitTransactionValidation(TRANSACTION_ID);

        // When / Then
        CollectExternalIngestService service = new CollectExternalIngestService();
        assertThatThrownBy(
            () -> service.ingestSip(collectInternalClient, ingestExternalClient, TRANSACTION_ID)
        ).isInstanceOf(CollectExternalNotFoundException.class);

        // Then
        verify(collectInternalClient, never()).changeTransactionStatus(anyString(), any());
        verify(collectInternalClient, never()).downloadSIP(TRANSACTION_ID);
        verify(ingestExternalClient, never()).ingest(any(), any(), eq(DEFAULT_WORKFLOW.name()), eq(RESUME.name()));
        verify(collectInternalClient, never()).attachVitamOperationId(TRANSACTION_ID, INGEST_OPERATION_ID);
    }

    @Test
    @RunWithCustomExecutor
    public void testIngestSip_BadRequest() throws Exception {
        // Given
        doThrow(new CollectInternalClientInvalidRequestException("bad request"))
            .when(collectInternalClient)
            .awaitTransactionValidation(TRANSACTION_ID);

        // When / Then
        CollectExternalIngestService service = new CollectExternalIngestService();
        assertThatThrownBy(
            () -> service.ingestSip(collectInternalClient, ingestExternalClient, TRANSACTION_ID)
        ).isInstanceOf(CollectExternalInvalidRequestException.class);

        // Then
        verify(collectInternalClient, never()).changeTransactionStatus(anyString(), any());
        verify(collectInternalClient, never()).downloadSIP(TRANSACTION_ID);
        verify(ingestExternalClient, never()).ingest(any(), any(), eq(DEFAULT_WORKFLOW.name()), eq(RESUME.name()));
        verify(collectInternalClient, never()).attachVitamOperationId(TRANSACTION_ID, INGEST_OPERATION_ID);
    }

    @Test
    @RunWithCustomExecutor
    public void testIngestSip_InternalServerError() throws Exception {
        // Given
        doThrow(new CollectInternalClientException("something went wrong"))
            .when(collectInternalClient)
            .awaitTransactionValidation(TRANSACTION_ID);

        // When / Then
        CollectExternalIngestService service = new CollectExternalIngestService();
        assertThatThrownBy(
            () -> service.ingestSip(collectInternalClient, ingestExternalClient, TRANSACTION_ID)
        ).isInstanceOf(CollectExternalServerSideException.class);

        // Then
        verify(collectInternalClient, never()).changeTransactionStatus(anyString(), any());
        verify(collectInternalClient, never()).downloadSIP(TRANSACTION_ID);
        verify(ingestExternalClient, never()).ingest(any(), any(), eq(DEFAULT_WORKFLOW.name()), eq(RESUME.name()));
        verify(collectInternalClient, never()).attachVitamOperationId(TRANSACTION_ID, INGEST_OPERATION_ID);
    }
}

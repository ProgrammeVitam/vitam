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

package fr.gouv.vitam.collect.internal.resource;

import fr.gouv.vitam.collect.external.dto.ProjectDto;
import fr.gouv.vitam.collect.internal.model.TransactionModel;
import fr.gouv.vitam.collect.internal.model.TransactionStatus;
import fr.gouv.vitam.collect.internal.service.FluxService;
import fr.gouv.vitam.collect.internal.service.MetadataService;
import fr.gouv.vitam.collect.internal.service.ProjectService;
import fr.gouv.vitam.collect.internal.service.SipService;
import fr.gouv.vitam.collect.internal.service.TransactionService;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class TransactionResourceTest {
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private static final String PROJECT_ID = "PROJECT_ID";
    private static final String TRANSACTION_ID = "TRANSACTION_ID";
    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    private TransactionResource transactionResource;

    @Mock
    private TransactionService transactionService;
    @Mock
    private MetadataService metadataService;
    @Mock
    private SipService sipService;
    @Mock
    private FluxService fluxService;
    @Mock
    private ProjectService projectService;

    @Before
    public void setUp() throws Exception {
        transactionResource =
            new TransactionResource(transactionService, sipService, metadataService, fluxService, projectService);

        final ProjectDto projectDto = new ProjectDto();
        projectDto.setId(PROJECT_ID);

        final TransactionModel transactionModel = new TransactionModel();
        transactionModel.setId(TRANSACTION_ID);
        transactionModel.setStatus(TransactionStatus.OPEN);
        transactionModel.setProjectId(PROJECT_ID);
        when(transactionService.findTransaction(eq(TRANSACTION_ID))).thenReturn(Optional.of(transactionModel));
        when(projectService.findProject(eq(PROJECT_ID))).thenReturn(Optional.of(projectDto));
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void getTransactionById() {
    }

    @Test
    public void updateTransaction() {
    }

    @Test
    public void deleteTransactionById() {
    }

    @Test
    public void uploadArchiveUnit() throws Exception {
    }

    @Test
    public void selectUnits() {
    }

    @Test
    public void closeTransaction() {
    }

    @Test
    public void abortTransaction() {
    }

    @Test
    public void reopenTransaction() {
    }

    @Test
    public void generateAndSendSip() {
    }

    @Test
    @RunWithCustomExecutor
    public void should_throw_error_when_update_units_using_empty_stream() {
        // Given
        final InputStream is = new ByteArrayInputStream(new byte[0]);

        when(transactionService.checkStatus(any(), eq(TransactionStatus.OPEN))).thenReturn(true);

        // When
        Response result = transactionResource.updateUnits(TRANSACTION_ID, is);
        // Then
        Assertions.assertThat(result.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void should_upload_transaction_zip_when_transaction_is_open() throws Exception {
        // Given
        final InputStream inputStreamZip =
            PropertiesUtils.getResourceAsStream("streamZip/transaction.zip");

        when(transactionService.checkStatus(any(), eq(TransactionStatus.OPEN))).thenReturn(true);

        // When
        Response result = transactionResource.uploadTransactionZip(TRANSACTION_ID, inputStreamZip);
        // Then
        Assertions.assertThat(result.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    }

    @Test
    public void should_not_upload_transaction_zip_when_transaction_is_not_open() throws Exception {
        // Given
        final InputStream inputStreamZip =
            PropertiesUtils.getResourceAsStream("streamZip/transaction.zip");

        when(transactionService.checkStatus(any(), eq(TransactionStatus.OPEN))).thenReturn(false);

        // When
        Response result = transactionResource.uploadTransactionZip(TRANSACTION_ID, inputStreamZip);
        // Then
        Assertions.assertThat(result.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
    }
}
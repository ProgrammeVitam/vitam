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
package fr.gouv.vitam.collect.internal.service;

import fr.gouv.vitam.collect.external.dto.TransactionDto;
import fr.gouv.vitam.collect.internal.exception.CollectException;
import fr.gouv.vitam.collect.internal.model.TransactionModel;
import fr.gouv.vitam.collect.internal.model.TransactionStatus;
import fr.gouv.vitam.collect.internal.repository.TransactionRepository;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doReturn;

public class TransactionServiceTest {
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @InjectMocks
    private TransactionService transactionService;
    @Mock
    private TransactionRepository transactionRepository;

    @Test
    public void createCollectTest() throws CollectException {
        // Given
        TransactionDto transactionDto =
            new TransactionDto("XXXX00000111111", null, null, null, null, null, null, null, null, null);

        // When
        transactionService.createTransaction(transactionDto);

        // Then
        ArgumentCaptor<TransactionModel> collectModelCaptor = ArgumentCaptor.forClass(TransactionModel.class);
        then(transactionRepository).should().createTransaction(collectModelCaptor.capture());
        TransactionModel transactionModelAdded = collectModelCaptor.getValue();
        Assertions.assertThat(transactionModelAdded.getId()).isEqualTo(
            transactionDto.getId());

    }

    @Test
    public void testFindCollect() throws CollectException {
        final String idCollect = "XXXX000002222222";
        // Given
        TransactionDto transactionDto =
            new TransactionDto("XXXX00000111111", null, null, null, null, null, null, null, null, null);
        doReturn(Optional.of(transactionDto)).when(transactionRepository).findTransaction(any());

        // When
        transactionService.findTransaction(idCollect);

        // Then
        then(transactionRepository).should()
            .findTransaction(idCollect);
    }

    @Test
    public void testCheckStatus_OK() throws CollectException {
        // Given
        final String idCollect = "XXXX000002222222";
        TransactionModel transactionModel =
            new TransactionModel(idCollect, null, TransactionStatus.OPEN, null, null);

        // When
        boolean checkStatus =
            transactionService.checkStatus(transactionModel, TransactionStatus.OPEN, TransactionStatus.ACK_ERROR);

        // Then
        Assertions.assertThat(checkStatus).isTrue();
    }

    @Test
    public void testCheckStatus_KO() throws InvalidParseOperationException {
        // Given
        final String idCollect = "XXXX000002222222";
        TransactionModel transactionModel =
            new TransactionModel(idCollect, null, TransactionStatus.OPEN, null, null);

        // When
        boolean checkStatus =
            transactionService.checkStatus(transactionModel, TransactionStatus.CLOSE, TransactionStatus.ACK_ERROR);

        // Then
        Assertions.assertThat(checkStatus).isFalse();
    }

}
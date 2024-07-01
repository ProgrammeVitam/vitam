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
package fr.gouv.vitam.common.timestamp;

import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.exception.TimeStampException;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.tsp.TSPException;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampResponse;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.math.BigInteger;
import java.security.cert.CertificateEncodingException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class TimestampGeneratorTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @InjectMocks
    private TimestampGenerator timestampGenerator;

    @Mock
    private TimeStampSignature timeStampSignature;

    @Test
    public void should_generate_a_time_stamp_request()
        throws TimeStampException, TSPException, CertificateEncodingException, OperatorCreationException {
        // Given
        byte[] hash = new byte[20];
        final TimeStampResponse timeStampResponse = mock(TimeStampResponse.class);
        final ArgumentCaptor<TimeStampRequest> timeStampRequestArgumentCaptor = ArgumentCaptor.forClass(
            TimeStampRequest.class
        );
        given(timeStampSignature.sign(timeStampRequestArgumentCaptor.capture())).willReturn(timeStampResponse);

        // When
        timestampGenerator.generateToken(hash, DigestType.SHA1, BigInteger.TEN);

        // Then
        final TimeStampRequest capture = timeStampRequestArgumentCaptor.getValue();
        assertThat(capture.getNonce()).isEqualTo(BigInteger.TEN);
        assertThat(capture.getMessageImprintDigest()).isEqualTo(hash);
    }
}

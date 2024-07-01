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

import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.tsp.TSPAlgorithms;
import org.bouncycastle.tsp.TSPException;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampRequestGenerator;
import org.bouncycastle.tsp.TimeStampResponse;
import org.bouncycastle.tsp.TimeStampToken;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TimeStampSignatureWithKeystoreTest {

    private TimeStampSignatureWithKeystore timeStampSignatureWithKeystore;

    @Before
    public void init()
        throws UnrecoverableKeyException, CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, URISyntaxException {
        final URL url = this.getClass().getResource("/tsa.p12");
        timeStampSignatureWithKeystore = new TimeStampSignatureWithKeystore(
            new File(url.toURI()),
            "1234".toCharArray()
        );
    }

    @Test
    public void should_fail_if_keystore_has_many_aliases()
        throws UnrecoverableKeyException, CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        // Given
        final URL url = this.getClass().getResource("/keystore_with_multiple_key.p12");

        // When / Then
        assertThatThrownBy(() -> new TimeStampSignatureWithKeystore(new File(url.toURI()), "secret".toCharArray()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Keystore has many key");
    }

    @Test
    public void should_sign_a_time_stamp_request()
        throws TSPException, CertificateEncodingException, OperatorCreationException, IOException {
        // Given
        final TimeStampRequestGenerator reqGen = new TimeStampRequestGenerator();
        reqGen.setCertReq(true);
        final byte[] hash = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        final BigInteger nonce = BigInteger.TEN;
        final TimeStampRequest request = reqGen.generate(TSPAlgorithms.SHA1, hash, nonce);

        // When
        final TimeStampResponse timeStampResponse = timeStampSignatureWithKeystore.sign(request);
        final TimeStampToken timeStampToken = timeStampResponse.getTimeStampToken();

        // Then
        assertThat(timeStampResponse.getStatus()).isEqualTo(0);
        assertThat(timeStampResponse.getEncoded()).isNotNull();
        assertThat(timeStampToken.getTimeStampInfo().getNonce()).isEqualTo(nonce);
        assertThat(timeStampToken.getTimeStampInfo().getMessageImprintDigest()).isEqualTo(hash);
        assertThat(timeStampToken.getCertificates().getMatches(null)).hasSize(2);
    }
}

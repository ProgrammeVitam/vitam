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
package fr.gouv.vitam.security.internal.rest.service;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import fr.gouv.vitam.common.alert.AlertService;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogLevel;
import fr.gouv.vitam.security.internal.common.model.CertificateBaseModel;
import fr.gouv.vitam.security.internal.common.model.CertificateStatus;
import fr.gouv.vitam.security.internal.common.model.IdentityModel;
import fr.gouv.vitam.security.internal.common.model.PersonalCertificateModel;
import fr.gouv.vitam.security.internal.common.service.CRLService;
import fr.gouv.vitam.security.internal.rest.repository.IdentityRepository;
import fr.gouv.vitam.security.internal.rest.repository.PersonalRepository;
import org.bson.Document;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.stubbing.OngoingStubbing;

import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CRLException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;

import static com.google.common.io.ByteStreams.toByteArray;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CRLServiceTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private IdentityRepository identityRepository;

    @Mock
    private PersonalRepository personalRepository;

    @Mock
    private AlertService alertService;

    private CRLService crlService;

    private static final String EMPTY_CRL_FILE = "/signing-ca-no-revoked-cert-yet.crl";

    private static final String CRL_SIA_REVOKED_FILE = "/signing-ca-with-revoked-cert.crl";

    private static final String IDENTITY_CERT_FILE = "/my-sia.crt";

    private static final String PERSONAL_CERT_FILE = "/my-personal-certificate.crt";

    private static final String IDENTITY_EXPIRED_CERT_FILE = "/my-expired-sia.crt";

    private static final String PERSONAL_EXPIRED_CERT_FILE = "/my-expired-personal-certificate.crt";

    private static final String ISSUER_NAME =
        "CN=ca_intermediate_client-external, OU=authorities, O=vitam, L=paris, ST=idf, C=fr";

    @Before
    public void setUp() {
        crlService = new CRLServiceImpl(identityRepository, personalRepository, alertService);
    }

    @Test
    public void checkCertificatesWhenExpired()
        throws IOException, CertificateException, CRLException, InvalidParseOperationException {
        InputStream emptyCRL = getClass().getResourceAsStream(EMPTY_CRL_FILE);

        doReturn(constructList(constructCertificateDocument("01", IDENTITY_EXPIRED_CERT_FILE)))
            .when(identityRepository)
            .findCertificate(ISSUER_NAME, CertificateStatus.VALID);
        doReturn(IdentityModel.class).when(identityRepository).getEntityModelType();

        doReturn(constructList(constructCertificateDocument("02", PERSONAL_EXPIRED_CERT_FILE)))
            .when(personalRepository)
            .findCertificate(ISSUER_NAME, CertificateStatus.VALID);
        doReturn(PersonalCertificateModel.class).when(personalRepository).getEntityModelType();

        crlService.checkIdentityWithCRL(toByteArray(emptyCRL));

        verify(identityRepository, times(0)).updateCertificateState(anyList(), eq(CertificateStatus.REVOKED));

        verify(identityRepository).findCertificate(anyString(), eq(CertificateStatus.VALID));
        verify(identityRepository).getEntityModelType();

        verify(personalRepository).updateCertificateState(anyList(), eq(CertificateStatus.EXPIRED));

        verify(personalRepository).findCertificate(anyString(), eq(CertificateStatus.VALID));
        verify(personalRepository).getEntityModelType();

        verify(alertService, times(2)).createAlert(eq(VitamLogLevel.WARN), ArgumentMatchers.endsWith("is expired"));
    }

    @Test
    public void checkCertificatesWithEmptyCRLTest()
        throws IOException, CertificateException, CRLException, InvalidParseOperationException {
        InputStream emptyCRL = getClass().getResourceAsStream(EMPTY_CRL_FILE);

        doReturn(constructList(constructCertificateDocument("01", IDENTITY_CERT_FILE)))
            .when(identityRepository)
            .findCertificate(ISSUER_NAME, CertificateStatus.VALID);
        doReturn(IdentityModel.class).when(identityRepository).getEntityModelType();

        doReturn(constructList(constructCertificateDocument("02", PERSONAL_CERT_FILE)))
            .when(personalRepository)
            .findCertificate(ISSUER_NAME, CertificateStatus.VALID);
        doReturn(PersonalCertificateModel.class).when(personalRepository).getEntityModelType();

        crlService.checkIdentityWithCRL(toByteArray(emptyCRL));

        verify(identityRepository, times(0)).updateCertificateState(anyList(), eq(CertificateStatus.REVOKED));

        verify(identityRepository).findCertificate(anyString(), eq(CertificateStatus.VALID));
        verify(identityRepository).getEntityModelType();

        verify(personalRepository).findCertificate(anyString(), eq(CertificateStatus.VALID));
        verify(personalRepository).getEntityModelType();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void checkCertificatesWithNonEmptyCRLTest()
        throws IOException, CertificateException, CRLException, InvalidParseOperationException {
        //check with not empty CRL
        InputStream crlRevokingSIA = getClass().getResourceAsStream(CRL_SIA_REVOKED_FILE);

        doNothing().when(identityRepository).updateCertificateState(any(), eq(CertificateStatus.REVOKED));

        doReturn(
            constructList(
                constructCertificateDocument("01", IDENTITY_EXPIRED_CERT_FILE),
                constructCertificateDocument("02", IDENTITY_CERT_FILE)
            )
        )
            .when(identityRepository)
            .findCertificate(ISSUER_NAME, CertificateStatus.VALID);
        doReturn(IdentityModel.class).when(identityRepository).getEntityModelType();

        doReturn(
            constructList(
                constructCertificateDocument("03", PERSONAL_EXPIRED_CERT_FILE),
                constructCertificateDocument("04", PERSONAL_CERT_FILE)
            )
        )
            .when(personalRepository)
            .findCertificate(ISSUER_NAME, CertificateStatus.VALID);
        doReturn(PersonalCertificateModel.class).when(personalRepository).getEntityModelType();

        ArgumentCaptor<List<String>> identCertificatesToRevokeCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<String>> identCertificatesHasExpiredCaptor = ArgumentCaptor.forClass(List.class);

        crlService.checkIdentityWithCRL(toByteArray(crlRevokingSIA));

        verify(identityRepository).updateCertificateState(
            identCertificatesToRevokeCaptor.capture(),
            eq(CertificateStatus.REVOKED)
        );

        verify(identityRepository).updateCertificateState(
            identCertificatesHasExpiredCaptor.capture(),
            eq(CertificateStatus.EXPIRED)
        );

        verify(identityRepository).findCertificate(anyString(), eq(CertificateStatus.VALID));
        verify(identityRepository, times(2)).getEntityModelType();

        // verify that certicate state is updated to REVOKED
        assertThat(identCertificatesToRevokeCaptor.getValue()).isNotNull().isNotEmpty().contains("02");

        // verify that certicate state is updated to EXPIRED
        assertThat(identCertificatesHasExpiredCaptor.getValue()).isNotNull().isNotEmpty().contains("01");

        verify(personalRepository).findCertificate(anyString(), eq(CertificateStatus.VALID));
        verify(personalRepository, times(2)).getEntityModelType();

        verify(alertService).createAlert(eq(VitamLogLevel.WARN), ArgumentMatchers.endsWith("was revoked by CRL"));
        verify(alertService, times(2)).createAlert(eq(VitamLogLevel.WARN), ArgumentMatchers.endsWith("is expired"));
    }

    private Document constructCertificateDocument(String certId, String certFile) throws CertificateException {
        CertificateBaseModel identityModel = new CertificateBaseModel();
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) certificateFactory.generateCertificate(
            getClass().getResourceAsStream(certFile)
        );

        identityModel.setId(certId);
        identityModel.setIssuerDN(cert.getIssuerDN().getName());
        identityModel.setCertificate(cert.getEncoded());
        identityModel.setSubjectDN(cert.getSubjectDN().getName());
        identityModel.setCertificateStatus(CertificateStatus.VALID);

        return Document.parse(JsonHandler.prettyPrint(identityModel));
    }

    @SuppressWarnings("unchecked")
    private FindIterable<Document> constructList(Document... documents) {
        FindIterable<Document> iterable = mock(FindIterable.class);
        MongoCursor<Document> cursor = mock(MongoCursor.class);

        OngoingStubbing<Boolean> whenHasNext = when(cursor.hasNext());
        for (Document ignored : documents) {
            whenHasNext = whenHasNext.thenReturn(true);
        }
        whenHasNext.thenReturn(false);

        OngoingStubbing<Document> whenNext = when(cursor.next());
        for (Document document : documents) {
            whenNext = whenNext.thenReturn(document);
        }
        whenNext.thenReturn(null);

        when(iterable.iterator()).thenReturn(cursor);
        return iterable;
    }
}

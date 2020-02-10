/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.security.internal.common.model.CertificateBaseModel;
import fr.gouv.vitam.security.internal.common.model.CertificateStatus;
import fr.gouv.vitam.security.internal.common.model.IdentityModel;
import fr.gouv.vitam.security.internal.common.model.PersonalCertificateModel;
import fr.gouv.vitam.security.internal.common.service.CRLService;
import fr.gouv.vitam.security.internal.common.service.X509PKIUtil;
import fr.gouv.vitam.security.internal.rest.repository.IdentityRepository;
import fr.gouv.vitam.security.internal.rest.repository.PersonalRepository;
import org.bson.Document;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CRLException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

import static com.google.common.io.ByteStreams.toByteArray;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class CRLServiceTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private IdentityRepository identityRepository;

    @Mock
    private PersonalRepository personalRepository;

    private CRLService crlService;

    private final static String EMPTY_CRL_FILE = "/ca-intermediate.crl";

    private final static String CRL_SIA_REVOKED_FILE = "/ca-intermediate-sia-revoked.crl";

    private final static String IDENTITY_CERT_FILE = "/sia-client-external.crt";

    private final static String PERSONAL_CERT_FILE = "/personal-client-external.crt";

    private final static String ISSUER_NAME =
        "CN=ca_intermediate_client-external, OU=authorities, O=vitam, L=paris, ST=idf, C=fr";


    @Before
    public void setUp() throws Exception {
        crlService = new CRLServiceImpl(identityRepository, personalRepository);
    }

    @Test
    public void checkCertificatesWithEmptyCRLTest()
        throws IOException, CertificateException, CRLException, InvalidParseOperationException {
        InputStream emptyCRL = getClass().getResourceAsStream(EMPTY_CRL_FILE);

        doReturn(constructCertificateDocumentList("01", CertificateStatus.VALID, IDENTITY_CERT_FILE))
            .when(identityRepository).findCertificate(ISSUER_NAME, CertificateStatus.VALID);
        doReturn(IdentityModel.class).when(identityRepository).getEntityModelType();

        doReturn(constructCertificateDocumentList("02", CertificateStatus.VALID, PERSONAL_CERT_FILE))
            .when(personalRepository).findCertificate(ISSUER_NAME, CertificateStatus.VALID);
        doReturn(PersonalCertificateModel.class).when(personalRepository).getEntityModelType();

        crlService.checkIdentityWithCRL(toByteArray(emptyCRL));
    }


    @Test
    public void checkCertificatesWithNonEmptyCRLTest()
        throws IOException, CertificateException, CRLException, InvalidParseOperationException {

        //check with not empty CRL
        InputStream crlRevokingSIA = getClass().getResourceAsStream(CRL_SIA_REVOKED_FILE);

        doNothing().when(identityRepository)
            .updateCertificateState(any(), eq(CertificateStatus.REVOKED));

        doReturn(constructCertificateDocumentList("01", CertificateStatus.VALID, IDENTITY_CERT_FILE))
            .when(identityRepository).findCertificate(ISSUER_NAME, CertificateStatus.VALID);
        doReturn(IdentityModel.class).when(identityRepository).getEntityModelType();

        doReturn(constructCertificateDocumentList("02", CertificateStatus.VALID, PERSONAL_CERT_FILE))
            .when(personalRepository).findCertificate(ISSUER_NAME, CertificateStatus.VALID);
        doReturn(PersonalCertificateModel.class).when(personalRepository).getEntityModelType();

        ArgumentCaptor<List> identCertificatesToUpdateCaptor = ArgumentCaptor.forClass(List.class);

        crlService.checkIdentityWithCRL(toByteArray(crlRevokingSIA));

        verify(identityRepository)
            .updateCertificateState(identCertificatesToUpdateCaptor.capture(), eq(CertificateStatus.REVOKED));

        verify(identityRepository).findCertificate(anyString(), eq(CertificateStatus.VALID));
        verify(identityRepository).getEntityModelType();

        verify(personalRepository).findCertificate(anyString(), eq(CertificateStatus.VALID));
        verify(personalRepository).getEntityModelType();

        List<String> certificatesToUpdateList = identCertificatesToUpdateCaptor.getValue();

        assertThat(certificatesToUpdateList).isNotNull().isNotEmpty().contains("01");

    }

    private FindIterable<Document> constructCertificateDocumentList(String certId, CertificateStatus isValid,
        String certFile)
        throws IOException, CertificateException {

        CertificateBaseModel identityModel = new CertificateBaseModel();
        X509Certificate cert = X509PKIUtil.parseX509Certificate(toByteArray(getClass().getResourceAsStream(certFile)));
        identityModel.setId(certId);
        identityModel.setIssuerDN(cert.getIssuerDN().getName());
        identityModel.setCertificate(cert.getEncoded());
        identityModel.setSubjectDN(cert.getSubjectDN().getName());
        identityModel.setCertificateStatus(isValid);

        Document identityModelDoc = Document.parse(JsonHandler.prettyPrint(identityModel));


        FindIterable iterableResult = mock(FindIterable.class);
        MongoCursor cursor = mock(MongoCursor.class);

        doReturn(cursor).when(iterableResult).iterator();
        doReturn(true).doReturn(false).when(cursor).hasNext();
        doReturn(identityModelDoc).when(cursor).next();

        return iterableResult;
    }
}

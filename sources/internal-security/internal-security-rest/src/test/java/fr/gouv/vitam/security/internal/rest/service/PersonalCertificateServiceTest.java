package fr.gouv.vitam.security.internal.rest.service;


import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.security.internal.common.model.PersonalCertificateModel;
import fr.gouv.vitam.security.internal.rest.exeption.PersonalCertificateException;
import fr.gouv.vitam.security.internal.rest.repository.PersonalRepository;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Optional;

import static com.google.common.io.ByteStreams.toByteArray;
import static org.mockito.ArgumentCaptor.*;
import static org.mockito.BDDMockito.given;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class PersonalCertificateServiceTest {
    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private PersonalRepository personalRepository;

    @Mock
    private LogbookOperationsClientFactory logbookOperationsClientFactory;

    private PersonalCertificateService personalCertificateService;

    @Before
    public void setUp() throws Exception {
        personalCertificateService = new PersonalCertificateService(logbookOperationsClientFactory, personalRepository);

    }

    @Test
    @RunWithCustomExecutor
    public void should_fail_to_parse_invalid_certificate()
        throws LogbookClientServerException, LogbookClientAlreadyExistsException, LogbookClientBadRequestException,
        InvalidParseOperationException, PersonalCertificateException {
        // Given
        given(logbookOperationsClientFactory.getClient()).willReturn(mock(LogbookOperationsClient.class));
        // When /then
        assertThatThrownBy(() -> personalCertificateService.checkPersonalCertificateExistence(new byte[3]))
            .isInstanceOf(PersonalCertificateException.class);
    }

    @Test
    @RunWithCustomExecutor
    public void should_fail_when_no_certificate_transmited()
        throws LogbookClientServerException, LogbookClientAlreadyExistsException, LogbookClientBadRequestException,
        InvalidParseOperationException, PersonalCertificateException {
        // Given
        given(logbookOperationsClientFactory.getClient()).willReturn(mock(LogbookOperationsClient.class));
        // When /then
        assertThatThrownBy(() -> personalCertificateService.checkPersonalCertificateExistence(null))
            .hasMessageContaining("No certificate transmitted");
    }

    @Test
    @RunWithCustomExecutor
    public void should_fail_when_no_valid_certificate_transmited()
        throws LogbookClientServerException, LogbookClientAlreadyExistsException, LogbookClientBadRequestException,
        InvalidParseOperationException, PersonalCertificateException, CertificateException, IOException {
        // Given
        InputStream stream = getClass().getResourceAsStream("/certificate.pem");
        byte[] certificate = toByteArray(stream);

        PersonalCertificateModel personalCertificateModel = new PersonalCertificateModel();
        given(logbookOperationsClientFactory.getClient()).willReturn(mock(LogbookOperationsClient.class));
        given(personalRepository.findIPersonalCertificateByHash(any()))
            .willReturn(Optional.empty());
        // When /then
        assertThatThrownBy(() -> personalCertificateService.checkPersonalCertificateExistence(certificate))
            .isInstanceOf(PersonalCertificateException.class).hasMessageContaining("Invalid certificate");
    }

    @Test
    @RunWithCustomExecutor
    public void should_check_valid_certificate_transmited()
        throws LogbookClientServerException, LogbookClientAlreadyExistsException, LogbookClientBadRequestException,
        InvalidParseOperationException, PersonalCertificateException, CertificateException, IOException {
        // Given
        InputStream stream = getClass().getResourceAsStream("/certificate.pem");
        byte[] certificate = toByteArray(stream);

        PersonalCertificateModel personalCertificateModel = new PersonalCertificateModel();
        given(logbookOperationsClientFactory.getClient()).willReturn(mock(LogbookOperationsClient.class));
        given(personalRepository.findIPersonalCertificateByHash(any()))
            .willReturn(Optional.of(personalCertificateModel));
        // When

        personalCertificateService.checkPersonalCertificateExistence(certificate);
        ///then
        verify(personalRepository)
            .findIPersonalCertificateByHash("2f1062f8bf84e7eb83a0f64c98d891fbe2c811b17ffac0bce1a6dc9c7c3dcbb7");
    }

    @Test
    public void should_create_certificate_transmited()
        throws LogbookClientServerException, LogbookClientAlreadyExistsException, LogbookClientBadRequestException,
        InvalidParseOperationException, PersonalCertificateException, CertificateException, IOException {
        // Given
        InputStream stream = getClass().getResourceAsStream("/certificate.pem");
        byte[] certificate = toByteArray(stream);
        ArgumentCaptor<PersonalCertificateModel> argumentCaptor = forClass(PersonalCertificateModel.class);

        // When
        personalCertificateService.createIdentity(certificate);
        ///then
        verify(personalRepository).createPersonalCertificate(argumentCaptor.capture());

        assertThat(argumentCaptor.getValue().getCertificateHash())
            .isEqualTo("2f1062f8bf84e7eb83a0f64c98d891fbe2c811b17ffac0bce1a6dc9c7c3dcbb7");
    }


}

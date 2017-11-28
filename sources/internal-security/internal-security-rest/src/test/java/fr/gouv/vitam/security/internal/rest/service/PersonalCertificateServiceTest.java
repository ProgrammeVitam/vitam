package fr.gouv.vitam.security.internal.rest.service;


import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
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

import java.io.InputStream;
import java.util.Optional;

import static com.google.common.io.ByteStreams.toByteArray;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class PersonalCertificateServiceTest {

    public static final String TEST_PERMISSION = "TEST_PERMISSION";
    public static final String CERTIFICATE_HASH = "2f1062f8bf84e7eb83a0f64c98d891fbe2c811b17ffac0bce1a6dc9c7c3dcbb7";
    public static final String CERTIFICATE_FILE = "/certificate.pem";

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
        throws Exception {
        // Given
        given(logbookOperationsClientFactory.getClient()).willReturn(mock(LogbookOperationsClient.class));
        // When /then
        assertThatThrownBy(
            () -> personalCertificateService.checkPersonalCertificateExistence(new byte[3], TEST_PERMISSION))
            .isInstanceOf(PersonalCertificateException.class);
        verifyNoMoreInteractions(personalRepository, logbookOperationsClientFactory);
    }

    @Test
    @RunWithCustomExecutor
    public void should_fail_when_no_certificate_transmitted()
        throws Exception {
        // Given
        LogbookOperationsClient logbookOperationsClient = mock(LogbookOperationsClient.class);
        given(logbookOperationsClientFactory.getClient()).willReturn(logbookOperationsClient);
        // When /then
        assertThatThrownBy(() -> personalCertificateService.checkPersonalCertificateExistence(null, TEST_PERMISSION))
            .hasMessageContaining("No certificate transmitted");
        verify(logbookOperationsClient).create(any(LogbookOperationParameters.class));
        verifyNoMoreInteractions(personalRepository, logbookOperationsClient);
    }

    @Test
    @RunWithCustomExecutor
    public void should_fail_when_no_valid_certificate_transmitted()
        throws Exception {
        // Given
        InputStream stream = getClass().getResourceAsStream(CERTIFICATE_FILE);
        byte[] certificate = toByteArray(stream);

        LogbookOperationsClient logbookOperationsClient = mock(LogbookOperationsClient.class);
        given(logbookOperationsClientFactory.getClient()).willReturn(logbookOperationsClient);
        given(personalRepository.findPersonalCertificateByHash(any()))
            .willReturn(Optional.empty());
        // When /then
        assertThatThrownBy(
            () -> personalCertificateService.checkPersonalCertificateExistence(certificate, TEST_PERMISSION))
            .isInstanceOf(PersonalCertificateException.class).hasMessageContaining("Invalid certificate");
        verify(personalRepository)
            .findPersonalCertificateByHash(CERTIFICATE_HASH);
        verify(logbookOperationsClient).create(any(LogbookOperationParameters.class));
        verifyNoMoreInteractions(personalRepository, logbookOperationsClient);
    }

    @Test
    @RunWithCustomExecutor
    public void should_check_valid_certificate_transmitted()
        throws Exception {
        // Given
        InputStream stream = getClass().getResourceAsStream(CERTIFICATE_FILE);
        byte[] certificate = toByteArray(stream);

        PersonalCertificateModel personalCertificateModel = new PersonalCertificateModel();
        given(logbookOperationsClientFactory.getClient()).willReturn(mock(LogbookOperationsClient.class));
        given(personalRepository.findPersonalCertificateByHash(any()))
            .willReturn(Optional.of(personalCertificateModel));
        // When

        personalCertificateService.checkPersonalCertificateExistence(certificate, TEST_PERMISSION);
        ///then
        verify(personalRepository)
            .findPersonalCertificateByHash(CERTIFICATE_HASH);
        verifyNoMoreInteractions(logbookOperationsClientFactory, personalRepository);
    }

    @Test
    public void should_create_certificate_transmitted_not_already_exists()
        throws Exception {
        // Given
        InputStream stream = getClass().getResourceAsStream(CERTIFICATE_FILE);
        byte[] certificate = toByteArray(stream);
        ArgumentCaptor<PersonalCertificateModel> argumentCaptor = forClass(PersonalCertificateModel.class);
        given(personalRepository.findPersonalCertificateByHash(any()))
            .willReturn(Optional.empty());
        // When
        personalCertificateService.createPersonalCertificateIfNotPresent(certificate);
        ///then
        verify(personalRepository).findPersonalCertificateByHash(
            CERTIFICATE_HASH);
        verify(personalRepository).createPersonalCertificate(argumentCaptor.capture());

        assertThat(argumentCaptor.getValue().getCertificateHash())
            .isEqualTo(CERTIFICATE_HASH);
        verifyNoMoreInteractions(logbookOperationsClientFactory, personalRepository);
    }

    @Test
    public void should_create_certificate_transmitted_already_exists()
        throws Exception {
        // Given
        InputStream stream = getClass().getResourceAsStream(CERTIFICATE_FILE);
        byte[] certificate = toByteArray(stream);
        PersonalCertificateModel personalCertificateModel = new PersonalCertificateModel();
        given(personalRepository.findPersonalCertificateByHash(any()))
            .willReturn(Optional.of(personalCertificateModel));
        // When
        personalCertificateService.createPersonalCertificateIfNotPresent(certificate);
        ///then
        verify(personalRepository).findPersonalCertificateByHash(
            CERTIFICATE_HASH);
        verifyNoMoreInteractions(logbookOperationsClientFactory, personalRepository);
    }

    @Test
    public void should_delete_certificate()
        throws Exception {
        // Given
        InputStream stream = getClass().getResourceAsStream(CERTIFICATE_FILE);
        byte[] certificate = toByteArray(stream);
        // When
        personalCertificateService.deletePersonalCertificateIfPresent(certificate);
        ///then
        verify(personalRepository).deletePersonalCertificate(
            CERTIFICATE_HASH);
        verifyNoMoreInteractions(logbookOperationsClientFactory, personalRepository);
    }
}

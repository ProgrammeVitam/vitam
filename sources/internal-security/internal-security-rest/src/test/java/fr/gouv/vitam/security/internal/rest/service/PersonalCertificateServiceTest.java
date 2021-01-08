package fr.gouv.vitam.security.internal.rest.service;


import static com.google.common.io.ByteStreams.toByteArray;
import static fr.gouv.vitam.logbook.common.parameters.LogbookParameterName.eventDetailData;
import static fr.gouv.vitam.logbook.common.parameters.LogbookParameterName.eventType;
import static fr.gouv.vitam.logbook.common.parameters.LogbookParameterName.eventTypeProcess;
import static fr.gouv.vitam.logbook.common.parameters.LogbookParameterName.outcome;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.InputStream;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.security.internal.common.exception.PersonalCertificateException;
import fr.gouv.vitam.security.internal.common.model.PersonalCertificateModel;
import fr.gouv.vitam.security.internal.rest.repository.PersonalRepository;

public class PersonalCertificateServiceTest {

    public static final String TEST_PERMISSION = "TEST_PERMISSION";
    public static final String CERTIFICATE_HASH = "336d23963cd039a856e2c24188cd15eb98b85cf7075a84c540a362e54fa6ae79";
    public static final String CERTIFICATE_FILE = "/certificate.pem";
    private static final String evedetData = "{\n" +
        "  \"Context\" : {\n" +
        "    \"CertificateSn\" : \"3\",\n" +
        "    \"CertificateSubjectDN\" : \"CN=userAdmin, O=VITAM, L=Paris, C=FR\",\n" +
        "    \"Permission\" : \"TEST_PERMISSION\"\n" +
        "  }\n" +
        "}";

    private static String evedetDataNoCertificate = "{\n" +
        "  \"Context\" : {\n" +
        "    \"Certificate\" : \"No certificate\",\n" +
        "    \"Permission\" : \"TEST_PERMISSION\"\n" +
        "  }\n" +
        "}";

    private static String evedetDataInvalidCertificate = "{\n" +
        "  \"Context\" : {\n" +
        "    \"Certificate\" : \"Invalid certificate\",\n" +
        "    \"Permission\" : \"TEST_PERMISSION\"\n" +
        "  }\n" +
        "}";
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
        ArgumentCaptor<LogbookOperationParameters> captor = ArgumentCaptor.forClass(LogbookOperationParameters.class);
        LogbookOperationsClient logbookOperationsClient = mock(LogbookOperationsClient.class);
        given(logbookOperationsClientFactory.getClient()).willReturn(logbookOperationsClient);

        // When /then
        assertThatThrownBy(
            () -> personalCertificateService.checkPersonalCertificateExistence(new byte[3], TEST_PERMISSION))
            .isInstanceOf(PersonalCertificateException.class);
        verify(logbookOperationsClient).create(captor.capture());

        LogbookOperationParameters parameters = captor.getValue();

        verifyNoMoreInteractions(personalRepository, logbookOperationsClient);
        assertThat(parameters.getParameterValue(eventType)).isEqualTo("STP_PERSONAL_CERTIFICATE_CHECK");
        assertThat(parameters.getParameterValue(eventTypeProcess)).isEqualTo("CHECK");
        assertThat(parameters.getParameterValue(outcome)).isEqualTo("KO");

        assertThat(parameters.getParameterValue(eventDetailData)).isEqualTo(evedetDataInvalidCertificate);
    }

    @Test
    @RunWithCustomExecutor
    public void should_fail_when_no_certificate_transmitted()
        throws Exception {
        // Given
        ArgumentCaptor<LogbookOperationParameters> captor = ArgumentCaptor.forClass(LogbookOperationParameters.class);
        LogbookOperationsClient logbookOperationsClient = mock(LogbookOperationsClient.class);
        given(logbookOperationsClientFactory.getClient()).willReturn(logbookOperationsClient);

        // When /then
        assertThatThrownBy(() -> personalCertificateService.checkPersonalCertificateExistence(null, TEST_PERMISSION))
            .hasMessageContaining("No certificate transmitted");
        verify(logbookOperationsClient).create(captor.capture());
        LogbookOperationParameters parameters = captor.getValue();

        verifyNoMoreInteractions(personalRepository, logbookOperationsClient);

        assertThat(parameters.getParameterValue(eventType)).isEqualTo("STP_PERSONAL_CERTIFICATE_CHECK");
        assertThat(parameters.getParameterValue(eventTypeProcess)).isEqualTo("CHECK");
        assertThat(parameters.getParameterValue(outcome)).isEqualTo("KO");

        assertThat(parameters.getParameterValue(eventDetailData)).isEqualTo(evedetDataNoCertificate);
    }

    @Test
    @RunWithCustomExecutor
    public void should_fail_when_no_valid_certificate_transmitted()
        throws Exception {
        // Given
        InputStream stream = getClass().getResourceAsStream(CERTIFICATE_FILE);
        byte[] certificate = toByteArray(stream);
        ArgumentCaptor<LogbookOperationParameters> captor = ArgumentCaptor.forClass(LogbookOperationParameters.class);

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
        verify(logbookOperationsClient).create(captor.capture());
        LogbookOperationParameters parameters = captor.getValue();

        verifyNoMoreInteractions(personalRepository, logbookOperationsClient);

        assertThat(parameters.getParameterValue(eventType)).isEqualTo("STP_PERSONAL_CERTIFICATE_CHECK");
        assertThat(parameters.getParameterValue(eventTypeProcess)).isEqualTo("CHECK");
        assertThat(parameters.getParameterValue(outcome)).isEqualTo("KO");

        assertThat(parameters.getParameterValue(eventDetailData)).isEqualTo(evedetData);

    }

    @Test
    @RunWithCustomExecutor
    public void should_check_valid_certificate_transmitted()
        throws Exception {
        // Given
        InputStream stream = getClass().getResourceAsStream(CERTIFICATE_FILE);
        byte[] certificate = toByteArray(stream);

        PersonalCertificateModel personalCertificateModel = new PersonalCertificateModel();
        personalCertificateModel.setCertificate(certificate);
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
        personalCertificateModel.setCertificate(certificate);
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

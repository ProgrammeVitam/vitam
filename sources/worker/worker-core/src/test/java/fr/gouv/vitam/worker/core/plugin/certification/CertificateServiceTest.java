package fr.gouv.vitam.worker.core.plugin.certification;


import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.objectgroup.VersionsModel;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.worker.core.plugin.CreateSecureFileActionPlugin;
import fr.gouv.vitam.worker.core.plugin.evidence.exception.EvidenceStatus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Optional;

import static fr.gouv.vitam.common.PropertiesUtils.getResourceFile;
import static fr.gouv.vitam.common.json.JsonHandler.getFromFile;
import static fr.gouv.vitam.common.json.JsonHandler.getFromString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CertificateServiceTest {
    @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
    @Mock private MetaDataClientFactory metaDataClientFactory;
    @Mock private LogbookOperationsClientFactory logbookOperationsClientFactory;
    @Mock private LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory;
    @Mock private StorageClientFactory storageClientFactory;

    @Mock private MetaDataClient metaDataClient;
    @Mock private LogbookOperationsClient logbookOperationsClient;
    @Mock private LogbookLifeCyclesClient logbookLifeCyclesClient;
    @Mock private StorageClient storageClient;



    @Mock private CertificateService certificateService;
    private JsonNode metadata;

    @Before
    public void setUp() throws Exception {

        when(metaDataClientFactory.getClient()).thenReturn(metaDataClient);
        when(logbookOperationsClientFactory.getClient()).thenReturn(logbookOperationsClient);
        when(metaDataClientFactory.getClient()).thenReturn(metaDataClient);
        when(logbookLifeCyclesClientFactory.getClient()).thenReturn(logbookLifeCyclesClient);
        when(storageClientFactory.getClient()).thenReturn(storageClient);

        certificateService = new CertificateService(metaDataClientFactory, logbookOperationsClientFactory,
            logbookLifeCyclesClientFactory, storageClientFactory);
        metadata = JsonHandler.getFromFile(getResourceFile("certification/objectGoup.json"));


    }

    @Test
    public void should_extract_last_qualifier_version_from_archive_unit() throws Exception {
        //given

        //when
        Optional<VersionsModel> model = certificateService.extractQualifierVersion(metadata, "BinaryMaster", "LAST");
        //Then
        assertThat(model.get().getId()).isEqualTo("aeaaaaaaaahad455abryqalenekbcnqaaaca");
        assertThat(model.get().getDataObjectGroupId()).isEqualTo("aebaaaaaaqhad455abryqalenekbcnyaaaaq");
        assertThat(model.get().getDataObjectVersion()).isEqualTo("BinaryMaster_2");
        assertThat(model.get().getFileInfoModel().getFilename())
            .isEqualTo("009734_20130456_0003_20101216_DI_26e_journees_de_Paris_huissiers_justice_FMolins.pdf.pdf");
        assertThat(model.get().getOpi()).isEqualTo("aeeaaaaaashi422cab3gyalenej2kcyaaaaq");
        assertThat(model.get().getMessageDigest()).isEqualTo(
            "a3556239aa1fcc1894229f8907bd43b6b322281114e53bd5a16f2fd94967ee2f28bd0cc1c70cca7daaec46c2d84fa66940dd0e4000cb16e8cc3bca0534f4917b");
        assertThat(model.get().getStorage().getOfferIds())
            .isEqualTo(Lists.newArrayList("offer-fs-1.service.int.consul", "offer-fs-2.service.int.consul"));

        // assertThat(model.get().get)
    }

    @Test
    public void should_extract_qualifier_version_from_archive_unit() throws Exception {
        //given

        //when
        Optional<VersionsModel> model = certificateService.extractQualifierVersion(metadata, "BinaryMaster", "1");
        //Then
        assertThat(model.get().getId()).isEqualTo("aeaaaaaaaahad455abryqalenekbcnqaaaco");
        assertThat(model.get().getDataObjectGroupId()).isEqualTo("aebaaaaaaqhad455abryqalenekbcnyaaaaq");
        assertThat(model.get().getDataObjectVersion()).isEqualTo("BinaryMaster_1");
        assertThat(model.get().getFileInfoModel().getFilename())
            .isEqualTo("009734_20130456_0003_20101216_DI_26e_journees_de_Paris_huissiers_justice_FMolins.pdf.pdf");
        assertThat(model.get().getOpi()).isEqualTo("aeeaaaaaashi422cab3gyalenej2kcyaaaaq");
        assertThat(model.get().getMessageDigest()).isEqualTo(
            "a3556239aa1fcc1894229f8907bd43b6b322281114e53bd5a16f2fd94967ee2f28bd0cc1c70cca7daaec46c2d84fa66940dd0e4000cb16e8cc3bca0534f4917b");
        assertThat(model.get().getStorage().getOfferIds())
            .isEqualTo(Lists.newArrayList("offer-fs-1.service.int.consul", "offer-fs-2.service.int.consul"));
    }

    @Test
    public void should_check_object_digest() throws Exception {
        // given
        JsonNode info = getFromString(
            "{\"offer-fs-1.service.int.consul\":{\"digest\":\"a3556239aa1fcc1894229f8907bd43b6b322281114e53bd5a16f2fd94967ee2f28bd0cc1c70cca7daaec46c2d84fa66940dd0e4000cb16e8cc3bca0534f4917b\"},\"offer-fs-2.service.int.consul\":{\"digest\":\"a3556239aa1fcc1894229f8907bd43b6b322281114e53bd5a16f2fd94967ee2f28bd0cc1c70cca7daaec46c2d84fa66940dd0e4000cb16e8cc3bca0534f4917b\"}}");
        Optional<VersionsModel> model = certificateService.extractQualifierVersion(metadata, "BinaryMaster", "LAST");
        CertificateParameters parameters = new CertificateParameters();
        parameters.setVersionsModel(model.get());
        when(storageClient
            .getInformation("default", DataCategory.OBJECT, "aeaaaaaaaahad455abryqalenekbcnqaaaca",
                Lists.newArrayList("offer-fs-1.service.int.consul", "offer-fs-2.service.int.consul"))).thenReturn(info);
        //when
        certificateService.checkStorageHash(parameters);

        assertThat(parameters.getReports().size()).isEqualTo(1);
        assertThat(parameters.getReports().get(0).getStatus()).isEqualTo(EvidenceStatus.OK);

    }

    @Test
    public void should_fail_when_checking_object_digest_with_differnt_hashes() throws Exception {
        // given
        JsonNode info = getFromString(
            "{\"offer-fs-1.service.int.consul\":{\"digest\":\"a35z56239aa1fcc1894229f8907bd43b6b322281114e53bd5a16f2fd94967ee2f28bd0cc1c70cca7daaec46c2d84fa66940dd0e4000cb16e8cc3bca0534f4917b\"},\"offer-fs-2.service.int.consul\":{\"digest\":\"a3556239aa1fcc1894229f8907bd43b6b322281114e53bd5a16f2fd94967ee2f28bd0cc1c70cca7daaec46c2d84fa66940dd0e4000cb16e8cc3bca0534f4917b\"}}");
        Optional<VersionsModel> model = certificateService.extractQualifierVersion(metadata, "BinaryMaster", "LAST");
        CertificateParameters parameters = new CertificateParameters();
        parameters.setVersionsModel(model.get());

        when(storageClient
            .getInformation("default", DataCategory.OBJECT, "aeaaaaaaaahad455abryqalenekbcnqaaaca",
                Lists.newArrayList("offer-fs-1.service.int.consul", "offer-fs-2.service.int.consul"))).thenReturn(info);
        //when
        certificateService.checkStorageHash(parameters);


        assertThat(parameters.getReports().size()).isEqualTo(1);
        assertThat(parameters.getReports().get(0).getStatus()).isEqualTo(EvidenceStatus.KO);
        assertThat(parameters.getReports().get(0).getDetails()).isEqualTo(
            "Hash : 'a35z56239aa1fcc1894229f8907bd43b6b322281114e53bd5a16f2fd94967ee2f28bd0cc1c70cca7daaec46c2d84fa66940dd0e4000cb16e8cc3bca0534f4917b' for offerId : offer-fs-1.service.int.consul is not equal of to a3556239aa1fcc1894229f8907bd43b6b322281114e53bd5a16f2fd94967ee2f28bd0cc1c70cca7daaec46c2d84fa66940dd0e4000cb16e8cc3bca0534f4917b");


    }

    @Test
    public void should_fail_when_checking_object_digest() throws Exception {
        // given
        JsonNode info = getFromString(
            "{\"offer-fs-1.service.int.consul\":{\"digest\":\"a35z56239aa1fcc1894229f8907bd43b6b322281114e53bd5a16f2fd94967ee2f28bd0cc1c70cca7daaec46c2d84fa66940dd0e4000cb16e8cc3bca0534f4917b\"}}");
        Optional<VersionsModel> model = certificateService.extractQualifierVersion(metadata, "BinaryMaster", "LAST");
        CertificateParameters parameters = new CertificateParameters();
        parameters.setVersionsModel(model.get());

        when(storageClient
            .getInformation("default", DataCategory.OBJECT, "aeaaaaaaaahad455abryqalenekbcnqaaaca",
                Lists.newArrayList("offer-fs-1.service.int.consul", "offer-fs-2.service.int.consul"))).thenReturn(info);
        //when
        certificateService.checkStorageHash(parameters);

        assertThat(parameters.getReports().size()).isEqualTo(1);
        assertThat(parameters.getReports().get(0).getStatus()).isEqualTo(EvidenceStatus.KO);
        assertThat(parameters.getReports().get(0).getDetails()).isEqualTo(
            "Hash : 'a35z56239aa1fcc1894229f8907bd43b6b322281114e53bd5a16f2fd94967ee2f28bd0cc1c70cca7daaec46c2d84fa66940dd0e4000cb16e8cc3bca0534f4917b' for offerId : offer-fs-1.service.int.consul is not equal of to a3556239aa1fcc1894229f8907bd43b6b322281114e53bd5a16f2fd94967ee2f28bd0cc1c70cca7daaec46c2d84fa66940dd0e4000cb16e8cc3bca0534f4917b, Hash : 'null' for offerId : offer-fs-2.service.int.consul is not equal of to a3556239aa1fcc1894229f8907bd43b6b322281114e53bd5a16f2fd94967ee2f28bd0cc1c70cca7daaec46c2d84fa66940dd0e4000cb16e8cc3bca0534f4917b");


    }

    @Test
    public void should_get_storage_events() throws Exception {
        JsonNode lfc = getFromFile(getResourceFile("certification/lfcObjectGroup.json"));
        CertificateParameters parameter = new CertificateParameters();

        VersionsModel versionsModel = new VersionsModel();
        versionsModel.setOpi("aeeaaaaaashi422cab3gyalenej2kcyaaaaq");
        versionsModel.setId("aeaaaaaaaahad455abryqalenekbcnqaaaca");
        certificateService.checkStorageEvent(parameter, lfc, versionsModel);

        assertThat(parameter.getReports().size()).isEqualTo(1);
        assertThat(parameter.getReports().get(0).getStatus()).isEqualTo(EvidenceStatus.OK);
        assertThat(parameter.getLogbookEvent().getLastPersistedDate()).isEqualTo("2018-07-05T06:15:17.106");
    }

    @Test
    public void should_check_logbook_event() throws Exception {
        JsonNode logbook = getFromFile(getResourceFile("certification/logbook.json"));
        CertificateParameters parameter = new CertificateParameters();

        VersionsModel versionsModel = new VersionsModel();
        versionsModel.setOpi("aeeaaaaaashi422cab3gyalenej2kcyaaaaq");
        versionsModel.setId("aeaaaaaaaahad455abryqalenekbcnqaaaca");

        when(logbookOperationsClient.selectOperationById("aeeaaaaaashi422cab3gyalenej2kcyaaaaq")).thenReturn(logbook);

        certificateService.checkStorageLogbookOperationInfo(parameter, versionsModel);

        assertThat(parameter.getReports().size()).isEqualTo(1);
        assertThat(parameter.getReports().get(0).getStatus()).isEqualTo(EvidenceStatus.OK);
        assertThat(parameter.getArchivalAgreement()).isEqualTo("IC-000001");


        assertThat(parameter.getAgIdApp()).isEqualTo("CT-000001");
        assertThat(parameter.getEvIdAppSession()).isEqualTo("MyApplicationId-ChangeIt");


    }

    @Test
    public void should_extract_hash_for_events_before_date() throws Exception {
        JsonNode lfc = getFromFile(getResourceFile("certification/lfcObjectGroup.json"));
        JsonNode lfcWithGoodEvent = getFromFile(getResourceFile("certification/lfcObjectGroup_rightEvents.json"));

        String goodHash = CreateSecureFileActionPlugin
            .generateDigest(lfcWithGoodEvent.get("events"), VitamConfiguration.getDefaultDigestType());
        String hashEventsBeforeDate = certificateService.getHashEventsBeforeDate("2018-07-05T06:15:16.106", lfc);
        assertThat(hashEventsBeforeDate).isEqualTo(goodHash);

    }

    @Test
    public void should_check_logbookInformation() throws Exception {

        //given
        CertificateParameters parameter = new CertificateParameters();

        VersionsModel versionsModel = new VersionsModel();
        String id = "aeeaaaaaashi422cab3gyalenej2kcyaaaaq";
        versionsModel.setOpi(id);
        parameter.setVersionsModel(versionsModel);
        JsonNode logbook = getFromFile(getResourceFile("certification/logbook2.json"));
        JsonNode secureLogbook = getFromFile(getResourceFile("certification/secure_logbook2.json"));
        JsonNode result = getFromFile(getResourceFile("certification/logbook_select_result.json"));

        // when
        when(logbookOperationsClient.selectOperationById(id)).thenReturn(logbook);
        when(logbookOperationsClient.selectOperationById("aecaaaaaacfpcnnvabc4ialfdxp5jviaaaaq"))
            .thenReturn(secureLogbook);
        when(logbookOperationsClient.selectOperation(any())).thenReturn(result);


        certificateService.getLogbookOperationInfo(parameter);

        //then
        assertThat(parameter.getVersionLogbook().get(id)).isEqualTo(secureLogbook.get("$results").get(0));
        assertThat(parameter.getLogBookSecuredOpiFileName()).isEqualTo("0_LogbookOperation_20180809_084353.zip");

    }
}

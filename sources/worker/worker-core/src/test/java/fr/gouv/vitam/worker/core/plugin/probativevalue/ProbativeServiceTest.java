/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 *******************************************************************************/
package fr.gouv.vitam.worker.core.plugin.probativevalue;


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

public class ProbativeServiceTest {
    @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
    @Mock private MetaDataClientFactory metaDataClientFactory;
    @Mock private LogbookOperationsClientFactory logbookOperationsClientFactory;
    @Mock private LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory;
    @Mock private StorageClientFactory storageClientFactory;

    @Mock private MetaDataClient metaDataClient;
    @Mock private LogbookOperationsClient logbookOperationsClient;
    @Mock private LogbookLifeCyclesClient logbookLifeCyclesClient;
    @Mock private StorageClient storageClient;



    @Mock private ProbativeService probativeService;
    private JsonNode metadata;

    @Before
    public void setUp() throws Exception {

        when(metaDataClientFactory.getClient()).thenReturn(metaDataClient);
        when(logbookOperationsClientFactory.getClient()).thenReturn(logbookOperationsClient);
        when(metaDataClientFactory.getClient()).thenReturn(metaDataClient);
        when(logbookLifeCyclesClientFactory.getClient()).thenReturn(logbookLifeCyclesClient);
        when(storageClientFactory.getClient()).thenReturn(storageClient);

        probativeService = new ProbativeService(metaDataClientFactory, logbookOperationsClientFactory,
            logbookLifeCyclesClientFactory, storageClientFactory);
        metadata = JsonHandler.getFromFile(getResourceFile("certification/objectGoup.json"));


    }

    @Test
    public void should_extract_last_qualifier_version_from_archive_unit() throws Exception {
        //given

        //when

        Optional<ProbativeUsageParameter> paramModel =
            probativeService.extractQualifierVersion(metadata, "BinaryMaster", "LAST");
        assertThat(paramModel).isPresent();

        ProbativeUsageParameter model = paramModel.get();  //Then
        assertThat(model.getVersionsModel().getId()).isEqualTo("aeaaaaaaaahad455abryqalenekbcnqaaaco");
        assertThat(model.getVersionsModel().getDataObjectGroupId()).isEqualTo("aebaaaaaaqhad455abryqalenekbcnyaaaaq");
        assertThat(model.getVersionsModel().getDataObjectVersion()).isEqualTo("BinaryMaster_2");
        assertThat(model.getVersionsModel().getFileInfoModel().getFilename())
            .isEqualTo("009734_20130456_0003_20101216_DI_26e_journees_de_Paris_huissiers_justice_FMolins.pdf.pdf");
        assertThat(model.getVersionsModel().getOpi()).isEqualTo("aeeaaaaaashi422cab3gyalenej2kcyaaaaq");
        assertThat(model.getVersionsModel().getMessageDigest()).isEqualTo(
            "a3556239aa1fcc1894229f8907bd43b6b322281114e53bd5a16f2fd94967ee2f28bd0cc1c70cca7daaec46c2d84fa66940dd0e4000cb16e8cc3bca0534f4917b");
        assertThat(model.getVersionsModel().getStorage().getOfferIds())
            .isEqualTo(Lists.newArrayList("offer-fs-1.service.int.consul", "offer-fs-2.service.int.consul"));


    }

    @Test
    public void should_extract_qualifier_version_from_archive_unit() throws Exception {
        //given

        //when
        Optional<ProbativeUsageParameter> paramModel =
            probativeService.extractQualifierVersion(metadata, "BinaryMaster", "1");

        assertThat(paramModel).isPresent();
        ProbativeUsageParameter model = paramModel.get();
        //Then
     //   assertThat(model.getId()).isEqualTo("aeaaaaaaaahad455abryqalenekbcnqaaaco");
        assertThat(model.getVersionsModel().getId()).isEqualTo("aeaaaaaaaahad455abryqalenekbcnqaaaco");
        assertThat(model.getVersionsModel().getDataObjectGroupId()).isEqualTo("aebaaaaaaqhad455abryqalenekbcnyaaaaq");
        assertThat(model.getVersionsModel().getDataObjectVersion()).isEqualTo("BinaryMaster_1");
        assertThat(model.getVersionsModel().getFileInfoModel().getFilename())
            .isEqualTo("009734_20130456_0003_20101216_DI_26e_journees_de_Paris_huissiers_justice_FMolins.pdf.pdf");
        assertThat(model.getVersionsModel().getOpi()).isEqualTo("aeeaaaaaashi422cab3gyalenej2kcyaaaaq");
        assertThat(model.getVersionsModel().getMessageDigest()).isEqualTo(
            "a3556239aa1fcc1894229f8907bd43b6b322281114e53bd5a16f2fd94967ee2f28bd0cc1c70cca7daaec46c2d84fa66940dd0e4000cb16e8cc3bca0534f4917b");
        assertThat(model.getVersionsModel().getStorage().getOfferIds())
            .isEqualTo(Lists.newArrayList("offer-fs-1.service.int.consul", "offer-fs-2.service.int.consul"));
    }

    @Test
    public void should_check_object_digest() throws Exception {
        // given
        JsonNode info = getFromString(
            "{\"offer-fs-1.service.int.consul\":{\"digest\":\"a3556239aa1fcc1894229f8907bd43b6b322281114e53bd5a16f2fd94967ee2f28bd0cc1c70cca7daaec46c2d84fa66940dd0e4000cb16e8cc3bca0534f4917b\"},\"offer-fs-2.service.int.consul\":{\"digest\":\"a3556239aa1fcc1894229f8907bd43b6b322281114e53bd5a16f2fd94967ee2f28bd0cc1c70cca7daaec46c2d84fa66940dd0e4000cb16e8cc3bca0534f4917b\"}}");
        Optional<ProbativeUsageParameter> paramModel =
            probativeService.extractQualifierVersion(metadata, "BinaryMaster", "LAST");

        assertThat(paramModel).isPresent();
        ProbativeUsageParameter parameter = paramModel.get();
        when(storageClient
            .getInformation("default", DataCategory.OBJECT, "aeaaaaaaaahad455abryqalenekbcnqaaaco",
                Lists.newArrayList("offer-fs-1.service.int.consul", "offer-fs-2.service.int.consul"))).thenReturn(info);
        //when
        probativeService.checkStorageHash(parameter);

        assertThat(parameter.getReports().size()).isEqualTo(1);
        assertThat(parameter.getReports().get(0).getStatus()).isEqualTo(EvidenceStatus.OK);

    }

    @Test
    public void should_fail_when_checking_object_digest_with_differnt_hashes() throws Exception {
        // given
        JsonNode info = getFromString(
            "{\"offer-fs-1.service.int.consul\":{\"digest\":\"a35z56239aa1fcc1894229f8907bd43b6b322281114e53bd5a16f2fd94967ee2f28bd0cc1c70cca7daaec46c2d84fa66940dd0e4000cb16e8cc3bca0534f4917b\"},\"offer-fs-2.service.int.consul\":{\"digest\":\"a3556239aa1fcc1894229f8907bd43b6b322281114e53bd5a16f2fd94967ee2f28bd0cc1c70cca7daaec46c2d84fa66940dd0e4000cb16e8cc3bca0534f4917b\"}}");
        Optional<ProbativeUsageParameter>
            model = probativeService.extractQualifierVersion(metadata, "BinaryMaster", "LAST");

        ProbativeUsageParameter parameters = model.get();

        when(storageClient
            .getInformation("default", DataCategory.OBJECT, "aeaaaaaaaahad455abryqalenekbcnqaaaco",
                Lists.newArrayList("offer-fs-1.service.int.consul", "offer-fs-2.service.int.consul"))).thenReturn(info);
        //when
        probativeService.checkStorageHash(parameters);


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
        Optional<ProbativeUsageParameter>
            model = probativeService.extractQualifierVersion(metadata, "BinaryMaster", "LAST");
        ProbativeUsageParameter parameter = model.get();

        when(storageClient
            .getInformation("default", DataCategory.OBJECT, "aeaaaaaaaahad455abryqalenekbcnqaaaco",
                Lists.newArrayList("offer-fs-1.service.int.consul", "offer-fs-2.service.int.consul"))).thenReturn(info);
        //when
        probativeService.checkStorageHash(parameter);

        assertThat(parameter.getReports().size()).isEqualTo(1);
        assertThat(parameter.getReports().get(0).getStatus()).isEqualTo(EvidenceStatus.KO);
        assertThat(parameter.getReports().get(0).getDetails()).isEqualTo(
            "Hash : 'a35z56239aa1fcc1894229f8907bd43b6b322281114e53bd5a16f2fd94967ee2f28bd0cc1c70cca7daaec46c2d84fa66940dd0e4000cb16e8cc3bca0534f4917b' for offerId : offer-fs-1.service.int.consul is not equal of to a3556239aa1fcc1894229f8907bd43b6b322281114e53bd5a16f2fd94967ee2f28bd0cc1c70cca7daaec46c2d84fa66940dd0e4000cb16e8cc3bca0534f4917b, Hash : 'null' for offerId : offer-fs-2.service.int.consul is not equal of to a3556239aa1fcc1894229f8907bd43b6b322281114e53bd5a16f2fd94967ee2f28bd0cc1c70cca7daaec46c2d84fa66940dd0e4000cb16e8cc3bca0534f4917b");


    }

    @Test
    public void should_get_storage_events() throws Exception {
        JsonNode lfc = getFromFile(getResourceFile("certification/lfcObjectGroup.json"));
        ProbativeUsageParameter parameter = new ProbativeUsageParameter();

        VersionsModel versionsModel = new VersionsModel();
        versionsModel.setOpi("aeeaaaaaashi422cab3gyalenej2kcyaaaaq");
        versionsModel.setId("aeaaaaaaaahad455abryqalenekbcnqaaaca");

        parameter.setVersionsModel(versionsModel);

        probativeService.checkStorageEvent(parameter, lfc);

        assertThat(parameter.getReports().size()).isEqualTo(1);
        assertThat(parameter.getReports().get(0).getStatus()).isEqualTo(EvidenceStatus.OK);
        assertThat(parameter.getLogbookEvent().getLastPersistedDate()).isEqualTo("2018-07-05T06:15:17.106");
    }

    @Test
    public void should_check_logbook_event() throws Exception {
        JsonNode logbook = getFromFile(getResourceFile("certification/logbook.json"));
        ProbativeUsageParameter parameter = new ProbativeUsageParameter();

        VersionsModel versionsModel = new VersionsModel();
        versionsModel.setOpi("aeeaaaaaashi422cab3gyalenej2kcyaaaaq");
        versionsModel.setId("aeaaaaaaaahad455abryqalenekbcnqaaaca");
        parameter.setVersionsModel(versionsModel);
        when(logbookOperationsClient.selectOperationById("aeeaaaaaashi422cab3gyalenej2kcyaaaaq")).thenReturn(logbook);

        probativeService.checkStorageLogbookOperationInfo(parameter);

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
        String hashEventsBeforeDate = probativeService.getHashEventsBeforeDate("2018-07-05T06:15:16.106", lfc);
        assertThat(hashEventsBeforeDate).isEqualTo(goodHash);

    }

    @Test
    public void should_check_logbookInformation() throws Exception {

        //given
        ProbativeUsageParameter parameter = new ProbativeUsageParameter();

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


        probativeService.getLogbookOperationInfo(parameter);

        //then
        assertThat(parameter.getVersionLogbook().get(id)).isEqualTo(secureLogbook.get("$results").get(0));
        assertThat(parameter.getLogBookSecuredOpiFileName()).isEqualTo("0_LogbookOperation_20180809_084353.zip");

    }
}

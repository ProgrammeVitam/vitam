package fr.gouv.vitam.worker.common.utils;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.alert.AlertService;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogLevel;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import org.assertj.core.util.Lists;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static fr.gouv.vitam.storage.engine.common.model.DataCategory.UNIT;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class StorageClientUtilTest {
    @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock private StorageClient storageClient;
    @Mock private AlertService alertService;

    @Test public void should_get_storage_info() throws Exception {

        String ogText =
            "{\"_id\":\"aeaqaaaaaafwtl4tabauualfquql45aaaabq\",\"_mgt\":{},\"DescriptionLevel\":\"RecordGrp\",\"Title\":\"dossier2\",\"Description\":\"Donec luctus vehicula leo ac mollis. Vivamus vitae ipsum pharetra, pharetra sem ut, elementum purus. Praesent eu nunc enim. Donec eu ipsum ac risus finibus condimentum\",\"StartDate\":\"2016-06-03T15:28:00\",\"EndDate\":\"2016-06-03T15:28:00\",\"SedaVersion\":\"2.1\",\"_storage\":{\"_nbc\":2,\"offerIds\":[\"offer-fs-1.service.consul\",\"offer-fs-2.service.consul\"],\"strategyId\":\"default\"},\"_sp\":\"FRAN_NP_009913\",\"_ops\":[\"aeeaaaaaacfwtl4taaxawalfquqis2aaaaaq\"],\"_opi\":\"aeeaaaaaacfwtl4taaxawalfquqis2aaaaaq\",\"_unitType\":\"INGEST\",\"_up\":[],\"_v\":0,\"_tenant\":0}";


        String storageResults =
            "{\"offer-fs-1.service.consul\":{\"objectName\":\"aeaqaaaaaafwtl4tabauualfquql45aaaabq.json\",\"type\":\"unit\",\"digest\":\"38794aad825f81e0cff401c1a7e0622da0a3fa802b6fd0bd762d65f410b4b733a1a58376247c7c9d3448dbd1503841958d31887c81bbf9b7436d7fafc98276ba\",\"fileSize\":6482,\"fileOwner\":\"Vitam_0\",\"lastAccessDate\":\"2018-08-29T10:23:32.564105Z\",\"lastModifiedDate\":\"2018-08-29T10:23:32.540104Z\"},\"offer-fs-2.service.consul\":{\"objectName\":\"aeaqaaaaaafwtl4tabauualfquql45aaaabq.json\",\"type\":\"unit\",\"digest\":\"38794aad825f81e0cff401c1a7e0622da0a3fa802b6fd0bd762d65f410b4b733a1a58376247c7c9d3448dbd1503841958d31887c81bbf9b7436d7fafc98276ba\",\"fileSize\":6482,\"fileOwner\":\"Vitam_0\",\"lastAccessDate\":\"2018-08-29T10:23:32.568105Z\",\"lastModifiedDate\":\"2018-08-29T10:23:32.556105Z\"}}";
        JsonNode og = JsonHandler.getFromString(ogText);


        when(storageClient.getInformation("default", UNIT, "aeaqaaaaaafwtl4tabauualfquql45aaaabq",
            Lists.newArrayList("offer-fs-1.service.consul", "offer-fs-2.service.consul")))
            .thenReturn(JsonHandler.getFromString(storageResults));



        String id1 = StorageClientUtil
            .getLFCAndMetadataGlobalHashFromStorage(og, UNIT, "aeaqaaaaaafwtl4tabauualfquql45aaaabq", storageClient,
                alertService);

        assertThat(id1).isEqualTo(
            "38794aad825f81e0cff401c1a7e0622da0a3fa802b6fd0bd762d65f410b4b733a1a58376247c7c9d3448dbd1503841958d31887c81bbf9b7436d7fafc98276ba");

    }

    @Test public void should_create_security_alert_when_one_offer_is_altered() throws Exception {

        String ogText =
            "{\"_id\":\"aeaqaaaaaafwtl4tabauualfquql45aaaabq\",\"_mgt\":{},\"DescriptionLevel\":\"RecordGrp\",\"Title\":\"dossier2\",\"Description\":\"Donec luctus vehicula leo ac mollis. Vivamus vitae ipsum pharetra, pharetra sem ut, elementum purus. Praesent eu nunc enim. Donec eu ipsum ac risus finibus condimentum\",\"StartDate\":\"2016-06-03T15:28:00\",\"EndDate\":\"2016-06-03T15:28:00\",\"SedaVersion\":\"2.1\",\"_storage\":{\"_nbc\":2,\"offerIds\":[\"offer-fs-1.service.consul\",\"offer-fs-2.service.consul\"],\"strategyId\":\"default\"},\"_sp\":\"FRAN_NP_009913\",\"_ops\":[\"aeeaaaaaacfwtl4taaxawalfquqis2aaaaaq\"],\"_opi\":\"aeeaaaaaacfwtl4taaxawalfquqis2aaaaaq\",\"_unitType\":\"INGEST\",\"_up\":[],\"_v\":0,\"_tenant\":0}";


        String storageResults =
            "{\"offer-fs-1.service.consul\":{\"objectName\":\"aeaqaaaaaafwtl4tabauualfquql45aaaabq.json\",\"type\":\"unit\",\"digest\":\"38794aad825f81e0cff401c1a7e0622da0a3fa802b6fd0bd762d65f410b4b733a1a58376247c7c9d3448dbd1503841958d31887c81bbf9b7436d7fafc98276ba\",\"fileSize\":6482,\"fileOwner\":\"Vitam_0\",\"lastAccessDate\":\"2018-08-29T10:23:32.564105Z\",\"lastModifiedDate\":\"2018-08-29T10:23:32.540104Z\"},\"offer-fs-2.service.consul\":{\"objectName\":\"aeaqaaaaaafwtl4tabauualfquql45aaaabq.json\",\"type\":\"unit\",\"digest\":\"fakedigest\",\"fileSize\":6482,\"fileOwner\":\"Vitam_0\",\"lastAccessDate\":\"2018-08-29T10:23:32.568105Z\",\"lastModifiedDate\":\"2018-08-29T10:23:32.556105Z\"}}";
        JsonNode og = JsonHandler.getFromString(ogText);


        when(storageClient.getInformation("default", UNIT, "aeaqaaaaaafwtl4tabauualfquql45aaaabq",
            Lists.newArrayList("offer-fs-1.service.consul", "offer-fs-2.service.consul")))
            .thenReturn(JsonHandler.getFromString(storageResults));



        String id1 = StorageClientUtil
            .getLFCAndMetadataGlobalHashFromStorage(og, UNIT, "aeaqaaaaaafwtl4tabauualfquql45aaaabq", storageClient,
                alertService);

        verify(alertService).createAlert(VitamLogLevel.ERROR,
            "[UNIT] id [aeaqaaaaaafwtl4tabauualfquql45aaaabq] The digest  'fakedigest' for the offer 'offer-fs-2.service.consul' not equal to the first Offer globalDigest expected (38794aad825f81e0cff401c1a7e0622da0a3fa802b6fd0bd762d65f410b4b733a1a58376247c7c9d3448dbd1503841958d31887c81bbf9b7436d7fafc98276ba)");

        assertThat(id1).isEqualTo(
            "0000000000000000000000000000000000000000000000000000000000000000");

    }

    @Test public void should_create_security_alert_when_one_file_in_offer_is_deleted() throws Exception {

        String ogText =
            "{\"_id\":\"aeaqaaaaaafwtl4tabauualfquql45aaaabq\",\"_mgt\":{},\"DescriptionLevel\":\"RecordGrp\",\"Title\":\"dossier2\",\"Description\":\"Donec luctus vehicula leo ac mollis. Vivamus vitae ipsum pharetra, pharetra sem ut, elementum purus. Praesent eu nunc enim. Donec eu ipsum ac risus finibus condimentum\",\"StartDate\":\"2016-06-03T15:28:00\",\"EndDate\":\"2016-06-03T15:28:00\",\"SedaVersion\":\"2.1\",\"_storage\":{\"_nbc\":2,\"offerIds\":[\"offer-fs-1.service.consul\",\"offer-fs-2.service.consul\"],\"strategyId\":\"default\"},\"_sp\":\"FRAN_NP_009913\",\"_ops\":[\"aeeaaaaaacfwtl4taaxawalfquqis2aaaaaq\"],\"_opi\":\"aeeaaaaaacfwtl4taaxawalfquqis2aaaaaq\",\"_unitType\":\"INGEST\",\"_up\":[],\"_v\":0,\"_tenant\":0}";


        String storageResults =
            "{\"offer-fs-1.service.consul\":{\"objectName\":\"aeaqaaaaaafwtl4tabauualfquql45aaaabq.json\",\"type\":\"unit\",\"digest\":\"38794aad825f81e0cff401c1a7e0622da0a3fa802b6fd0bd762d65f410b4b733a1a58376247c7c9d3448dbd1503841958d31887c81bbf9b7436d7fafc98276ba\",\"fileSize\":6482,\"fileOwner\":\"Vitam_0\",\"lastAccessDate\":\"2018-08-29T10:23:32.564105Z\",\"lastModifiedDate\":\"2018-08-29T10:23:32.540104Z\"}}";
        JsonNode og = JsonHandler.getFromString(ogText);


        when(storageClient.getInformation("default", UNIT, "aeaqaaaaaafwtl4tabauualfquql45aaaabq",
            Lists.newArrayList("offer-fs-1.service.consul", "offer-fs-2.service.consul")))
            .thenReturn(JsonHandler.getFromString(storageResults));



        String id1 = StorageClientUtil
            .getLFCAndMetadataGlobalHashFromStorage(og, UNIT, "aeaqaaaaaafwtl4tabauualfquql45aaaabq", storageClient,
                alertService);

        verify(alertService).createAlert(VitamLogLevel.ERROR,
            "[UNIT] id [aeaqaaaaaafwtl4tabauualfquql45aaaabq] The digest  '38794aad825f81e0cff401c1a7e0622da0a3fa802b6fd0bd762d65f410b4b733a1a58376247c7c9d3448dbd1503841958d31887c81bbf9b7436d7fafc98276ba' for the offer 'offer-fs-2.service.consul' is not present ");

        assertThat(id1).isEqualTo(
            "0000000000000000000000000000000000000000000000000000000000000000");

    }
}

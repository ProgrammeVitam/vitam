package fr.gouv.vitam.ihmrecette.appserver;



import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

public class StorageCRUDUtilsTest {

    public @Rule MockitoRule rule = MockitoJUnit.rule();


    @Mock StorageClient storageClient;

    private StorageCRUDUtils storageCRUDUtils;

    @Before
    public void setUp() throws Exception {

    }

    String information =
        "{\"offer-fs-1.service.consul\":{\"objectName\":\"aeeaaaaaacew2hcbaafoialcsdnwzyyaaaaq.json\",\"type\":\"object\",\"digest\":\"9a928aed2b62f3755e8dd09ab8b3c0817823383465fb3249de422113f5b4af282c80db552cddadaf1baac17288bd6a1e41f04c2506bf40e47e19fa800445b273\",\"fileSize\":1495,\"fileOwner\":\"Vitam_0\",\"lastAccessDate\":\"2018-04-06T09:51:34.192599Z\",\"lastModifiedDate\":\"2018-04-06T09:51:34.124597Z\"}}";

    @Test
    public void should_deleteObject() throws Exception {
        ArrayList<String> offers = new ArrayList<>();
        offers.add("offer-fs-1.service.consul");
        given(storageClient
            .getInformation(VitamConfiguration.getDefaultStrategy(), DataCategory.OBJECT, "aeeaaaaaacew2hcbaafoialcsdnwzyyaaaaq.json", offers, true))
            .willReturn(
                JsonHandler.getFromString(information));

        given(storageClient.getOffers(VitamConfiguration.getDefaultStrategy())).willReturn(offers);
        storageCRUDUtils = new StorageCRUDUtils(storageClient);


        boolean result = storageCRUDUtils.deleteFile(DataCategory.OBJECT, VitamConfiguration.getDefaultStrategy(), "aeeaaaaaacew2hcbaafoialcsdnwzyyaaaaq.json","offer-fs-1.service.consul");

        assertThat(result).isFalse();

    }
}

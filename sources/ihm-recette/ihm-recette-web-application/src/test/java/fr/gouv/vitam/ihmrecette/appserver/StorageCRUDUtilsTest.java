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

    @Mock
    StorageClient storageClient;

    private StorageCRUDUtils storageCRUDUtils;

    @Before
    public void setUp() throws Exception {}

    String information =
        "{\"offer-fs-1.service.consul\":{\"objectName\":\"aeeaaaaaacew2hcbaafoialcsdnwzyyaaaaq.json\",\"type\":\"object\",\"digest\":\"9a928aed2b62f3755e8dd09ab8b3c0817823383465fb3249de422113f5b4af282c80db552cddadaf1baac17288bd6a1e41f04c2506bf40e47e19fa800445b273\",\"fileSize\":1495,\"fileOwner\":\"Vitam_0\",\"lastAccessDate\":\"2018-04-06T09:51:34.192599Z\",\"lastModifiedDate\":\"2018-04-06T09:51:34.124597Z\"}}";

    @Test
    public void should_deleteObject() throws Exception {
        ArrayList<String> offers = new ArrayList<>();
        offers.add("offer-fs-1.service.consul");
        given(
            storageClient.getInformation(
                VitamConfiguration.getDefaultStrategy(),
                DataCategory.OBJECT,
                "aeeaaaaaacew2hcbaafoialcsdnwzyyaaaaq.json",
                offers,
                true
            )
        ).willReturn(JsonHandler.getFromString(information));

        given(storageClient.getOffers(VitamConfiguration.getDefaultStrategy())).willReturn(offers);
        storageCRUDUtils = new StorageCRUDUtils(storageClient);

        boolean result = storageCRUDUtils.deleteFile(
            DataCategory.OBJECT,
            VitamConfiguration.getDefaultStrategy(),
            "aeeaaaaaacew2hcbaafoialcsdnwzyyaaaaq.json",
            "offer-fs-1.service.consul"
        );

        assertThat(result).isFalse();
    }
}

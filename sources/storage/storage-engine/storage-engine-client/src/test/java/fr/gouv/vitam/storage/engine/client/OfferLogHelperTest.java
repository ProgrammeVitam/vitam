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
package fr.gouv.vitam.storage.engine.client;

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.OfferLogAction;
import fr.gouv.vitam.storage.engine.common.model.Order;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class OfferLogHelperTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private StorageClientFactory storageClientFactory;

    @Mock
    private StorageClient storageClient;

    private List<String> offerIds = Arrays.asList("offer1", "offer2");
    private static final String DEFAULT_OFFER_ID = "default";

    @Before
    public void init() throws Exception {
        doReturn(storageClient).when(storageClientFactory).getClient();
        doReturn(offerIds).when(storageClient).getOffers(VitamConfiguration.getDefaultStrategy());
    }

    private void givenOfferLogOffsets(List<Integer> filesOffsets) throws StorageServerClientException {
        doAnswer(args -> {
            Long startOffset = args.getArgument(3);
            int limit = args.getArgument(4);

            return new RequestResponseOK<>()
                .addAllResults(
                    filesOffsets
                        .stream()
                        .filter(o -> startOffset == null || o >= startOffset)
                        .limit(limit)
                        .map(o -> new OfferLog(o, LocalDateUtil.now(), "0_unit", "file" + o, OfferLogAction.WRITE))
                        .collect(Collectors.toList())
                );
        })
            .when(storageClient)
            .getOfferLogs(
                eq(VitamConfiguration.getDefaultStrategy()),
                eq(DEFAULT_OFFER_ID),
                eq(DataCategory.UNIT),
                anyLong(),
                anyInt(),
                eq(Order.ASC)
            );
    }

    @Test
    public void testOffsetAndLimitV2OfferLogs() throws Exception {
        // Given
        List<Integer> filesOffsets = Arrays.asList(10, 20, 30, 40, 50, 60, 70, 80, 90, 100);
        givenOfferLogOffsets(filesOffsets);

        Iterator<OfferLog> offerLogIterator = OfferLogHelper.getListing(
            storageClientFactory,
            VitamConfiguration.getDefaultStrategy(),
            DEFAULT_OFFER_ID,
            DataCategory.UNIT,
            20L,
            Order.ASC,
            5,
            7
        );

        // When
        assertThat(offerLogIterator)
            .extracting(OfferLog::getFileName)
            .containsExactly("file20", "file30", "file40", "file50", "file60", "file70", "file80");

        verify(storageClient, times(2)).getOfferLogs(
            eq(VitamConfiguration.getDefaultStrategy()),
            eq(DEFAULT_OFFER_ID),
            eq(DataCategory.UNIT),
            anyLong(),
            anyInt(),
            eq(Order.ASC)
        );
    }
}

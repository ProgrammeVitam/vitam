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
package fr.gouv.vitam.worker.core.plugin.lfc_traceability;

import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class StrategyIdOfferIdLoaderTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private StorageClientFactory storageClientFactory;

    @Mock
    private StorageClient storageClient;

    @Before
    public void before() {
        doReturn(storageClient).when(storageClientFactory).getClient();
    }

    @Test
    public void getOfferIdsFirstInvocation() throws Exception {
        // Given
        doReturn(Arrays.asList("offer1", "offer2")).when(storageClient).getOffers("strategy1");
        StrategyIdOfferIdLoader instance = new StrategyIdOfferIdLoader(storageClientFactory);

        // When
        List<String> strategy1_Offers = instance.getOfferIds("strategy1");

        // Then
        assertThat(strategy1_Offers).containsExactlyInAnyOrder("offer1", "offer2");
        verify(storageClient).getOffers(anyString());
    }

    @Test
    public void getOfferIdsMultipleInvocations() throws Exception {
        // Given
        doReturn(Arrays.asList("offer1", "offer2")).when(storageClient).getOffers("strategy1");
        StrategyIdOfferIdLoader instance = new StrategyIdOfferIdLoader(storageClientFactory);

        // When
        List<String> strategy1_Offers1 = instance.getOfferIds("strategy1");
        List<String> strategy1_Offers2 = instance.getOfferIds("strategy1");
        List<String> strategy1_Offers3 = instance.getOfferIds("strategy1");

        // Then
        assertThat(strategy1_Offers1).containsExactlyInAnyOrder("offer1", "offer2");
        assertThat(strategy1_Offers2).containsExactlyInAnyOrder("offer1", "offer2");
        assertThat(strategy1_Offers3).containsExactlyInAnyOrder("offer1", "offer2");
        verify(storageClient).getOffers(anyString());
    }

    @Test
    public void getOfferIdsMultipleInvocationsMultipleStrategies() throws Exception {
        // Given
        doReturn(Arrays.asList("offer1", "offer2")).when(storageClient).getOffers("strategy1");
        doReturn(Arrays.asList("offer3")).when(storageClient).getOffers("strategy2");
        doReturn(Arrays.asList("offer4")).when(storageClient).getOffers("strategy3");
        StrategyIdOfferIdLoader instance = new StrategyIdOfferIdLoader(storageClientFactory);

        // When
        List<String> strategy1_Offers1 = instance.getOfferIds("strategy1");
        List<String> strategy1_Offers2 = instance.getOfferIds("strategy1");
        List<String> strategy2_Offers = instance.getOfferIds("strategy2");
        List<String> strategy3_Offers = instance.getOfferIds("strategy3");
        List<String> strategy1_Offers3 = instance.getOfferIds("strategy1");

        // Then
        assertThat(strategy1_Offers1).containsExactlyInAnyOrder("offer1", "offer2");
        assertThat(strategy1_Offers2).containsExactlyInAnyOrder("offer1", "offer2");
        assertThat(strategy1_Offers3).containsExactlyInAnyOrder("offer1", "offer2");
        assertThat(strategy2_Offers).containsExactlyInAnyOrder("offer3");
        assertThat(strategy3_Offers).containsExactlyInAnyOrder("offer4");

        verify(storageClient, times(3)).getOffers(anyString());
    }
}

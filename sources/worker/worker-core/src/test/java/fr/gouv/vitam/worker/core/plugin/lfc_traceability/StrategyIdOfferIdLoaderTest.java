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

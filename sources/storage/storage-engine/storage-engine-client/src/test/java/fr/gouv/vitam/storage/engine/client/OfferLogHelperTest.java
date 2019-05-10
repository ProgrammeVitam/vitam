package fr.gouv.vitam.storage.engine.client;

import fr.gouv.vitam.common.LocalDateUtil;
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

    private static final String STRATEGY = "default";

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private StorageClientFactory storageClientFactory;
    @Mock
    private StorageClient storageClient;
    private List<String> offerIds = Arrays.asList("offer1", "offer2");

    @Before
    public void init() throws Exception {
        doReturn(storageClient).when(storageClientFactory).getClient();
        doReturn(offerIds).when(storageClient).getOffers(STRATEGY);
    }

    private void givenOfferLogOffsets(List<Integer> filesOffsets)
        throws StorageServerClientException {
        doAnswer(
            args -> {
                Long startOffset = args.getArgument(2);
                int limit = args.getArgument(3);

                return new RequestResponseOK<>()
                    .addAllResults(filesOffsets.stream()
                        .filter(o -> startOffset == null || o >= startOffset)
                        .limit(limit)
                        .map(o -> new OfferLog(o, LocalDateUtil.now(), "0_unit", "file" + o, OfferLogAction.WRITE))
                        .collect(Collectors.toList()));
            }
        ).when(storageClient).getOfferLogs(eq(STRATEGY), eq(DataCategory.UNIT), anyLong(), anyInt(), eq(Order.ASC));
    }

    @Test
    public void testOffsetAndLimitV2OfferLogs() throws Exception {

        // Given
        List<Integer> filesOffsets = Arrays.asList(10, 20, 30, 40, 50, 60, 70, 80, 90, 100);
        givenOfferLogOffsets(filesOffsets);

        Iterator<OfferLog> offerLogIterator =
            OfferLogHelper.getListing(storageClientFactory, STRATEGY, DataCategory.UNIT, 20L, Order.ASC, 5, 7);

        // When
        assertThat(offerLogIterator)
            .extracting(OfferLog::getFileName)
            .containsExactly("file20", "file30", "file40", "file50", "file60", "file70", "file80");

        verify(storageClient, times(2))
            .getOfferLogs(eq(STRATEGY), eq(DataCategory.UNIT), anyLong(), anyInt(), eq(Order.ASC));
    }
}

package fr.gouv.vitam.collect.external.client;

import fr.gouv.vitam.collect.internal.dto.TransactionDto;
import fr.gouv.vitam.common.client.VitamClientFactoryInterface;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.model.RequestResponseOK;
import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class CollectClientRestTest {

    @Test
    public void rest_client_collect_init_transaction_should_return_an_transaction_id() throws
        InvalidParseOperationException {
        //Given
        CollectClientFactory collectClientFactory = CollectClientFactory.getInstance();
        collectClientFactory.setVitamClientType(VitamClientFactoryInterface.VitamClientType.PRODUCTION);
        CollectClient client = collectClientFactory.getClient();

        //When
        RequestResponseOK<TransactionDto> response = client.initTransaction(new TransactionDto());

        //Then
        //TODO : change when implementation is done
        assertThat(response).isNull();
    }
}

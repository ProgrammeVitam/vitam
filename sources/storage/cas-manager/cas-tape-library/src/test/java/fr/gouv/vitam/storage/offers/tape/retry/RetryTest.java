package fr.gouv.vitam.storage.offers.tape.retry;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.internal.verification.Times;

public class RetryTest {

    @Test
    public void executeOK() throws Exception {
        Retry.Delegate delegate = mock(Retry.Delegate.class);
        when(delegate.call())
            .thenThrow(new Exception(""))
            .thenThrow(new Exception())
            .thenReturn(Long.valueOf(10l));

        Long res = new Retry<Long>(3, 1).execute(delegate);

        Assertions.assertThat(res).isEqualTo(10l);
        verify(delegate, new Times(3)).call();
    }

    @Test(expected = Exception.class)
    public void executeKO() throws Exception {
        Retry.Delegate delegate = mock(Retry.Delegate.class);
        when(delegate.call())
            .thenThrow(new Exception(""))
            .thenThrow(new Exception())
            .thenThrow(new Exception())
            .thenReturn(Long.valueOf(10l));
        new Retry<Long>(3, 1).execute(delegate);
    }
}
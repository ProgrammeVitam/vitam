package fr.gouv.vitam.storage.offers.tape.retry;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import com.mongodb.MongoException;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.internal.verification.Times;

public class RetryTest {

    @Test
    public void execute_any_exception_ok_at_last_retry() throws Exception {
        Retry.Delegate delegate = mock(Retry.Delegate.class);
        when(delegate.call())
            .thenThrow(new Exception(""))
            .thenThrow(new Exception())
            .thenReturn(Long.valueOf(10l));

        Long res = new Retry<Long>(3, 1).execute(delegate);

        Assertions.assertThat(res).isEqualTo(10l);
        verify(delegate, new Times(3)).call();
    }


    @Test
    public void execute_any_exception_ok_before_last_retry() throws Exception {
        Retry.Delegate delegate = mock(Retry.Delegate.class);
        when(delegate.call())
            .thenThrow(new Exception(""))
            .thenReturn(Long.valueOf(10l))
            .thenThrow(new Exception());

        Long res = new Retry<Long>(3, 1).execute(delegate);

        Assertions.assertThat(res).isEqualTo(10l);
        verify(delegate, new Times(2)).call();
    }

    @Test
    public void execute_check_exception_ok_at_last_retry() throws Exception {
        Retry.Delegate delegate = mock(Retry.Delegate.class);
        when(delegate.call())
            .thenThrow(new MongoException(""))
            .thenThrow(new IOException())
            .thenReturn(Long.valueOf(10l));

        Long res = new Retry<Long>(3, 1).execute(delegate, MongoException.class, IOException.class);

        Assertions.assertThat(res).isEqualTo(10l);
        verify(delegate, new Times(3)).call();
    }


    @Test
    public void execute_check_exception_ok_before_last_retry() throws Exception {
        Retry.Delegate delegate = mock(Retry.Delegate.class);
        when(delegate.call())
            .thenThrow(new MongoException(""))
            .thenReturn(Long.valueOf(10l))
            .thenThrow(new IOException());

        Long res = new Retry<Long>(3, 1).execute(delegate, MongoException.class, IOException.class);

        Assertions.assertThat(res).isEqualTo(10l);
        verify(delegate, new Times(2)).call();
    }

    @Test(expected = IOException.class)
    public void execute_any_exception_ko() throws Exception {
        Retry.Delegate delegate = mock(Retry.Delegate.class);
        when(delegate.call())
            .thenThrow(new IllegalStateException())
            .thenThrow(new MongoException(""))
            .thenThrow(new IOException())
            .thenReturn(Long.valueOf(10l));
        new Retry<Long>(3, 1).execute(delegate);
    }

    @Test(expected = IllegalStateException.class)
    public void execute_check_exception_ko() throws Exception {
        Retry.Delegate delegate = mock(Retry.Delegate.class);
        when(delegate.call())
            .thenThrow(new RuntimeException(""))
            .thenThrow(new MongoException(""))
            .thenThrow(new IllegalStateException(""))
            .thenReturn(Long.valueOf(10l));
        new Retry<Long>(10, 1).execute(delegate, MongoException.class, RuntimeException.class);
    }
}
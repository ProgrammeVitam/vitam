package fr.gouv.vitam.storage.engine.server.distribution.impl;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TimeoutStopwatchTest {

    @Test
    public void TestDecreasingTimeout() throws Exception {


        TimeoutStopwatch timeoutStopwatch = new TimeoutStopwatch(1000);

        long remaining1 = timeoutStopwatch.getRemainingDelayInMilliseconds();

        Thread.sleep(100);

        long remaining2 = timeoutStopwatch.getRemainingDelayInMilliseconds();

        assertThat(remaining1).isBetween(0L, 5000L);
        assertThat(remaining2).isBetween(0L, 5000L);
        assertThat(remaining2).isLessThanOrEqualTo(remaining1 - 100L);
    }
}

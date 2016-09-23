package fr.gouv.vitam.common.stream;

import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.Test;

public class StreamUtilsTest {

    @Test
    public void testClose() {
        InputStream inputStream = null;
        try {
            StreamUtils.closeSilently(inputStream);
        } catch (Exception e) {
            fail("Should not raized an exception");
        }
        inputStream = new ByteArrayInputStream(new byte[10]);
        try {
            StreamUtils.closeSilently(inputStream);
        } catch (Exception e) {
            fail("Should not raized an exception");
        }
    }

}

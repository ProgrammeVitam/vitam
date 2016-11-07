package fr.gouv.vitam.common.junit;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class FakeInputStreamTest {

    @Test
    public void testRead() {
        int len = 100;
        try (FakeInputStream fakeInputStream = new FakeInputStream(len, true)) {
            assertEquals(len, JunitHelper.consumeInputStreamPerByte(fakeInputStream));
        }
        try (FakeInputStream fakeInputStream = new FakeInputStream(len, false)) {
            assertEquals(len, JunitHelper.consumeInputStreamPerByte(fakeInputStream));
        }
        len = 1000000;
        try (FakeInputStream fakeInputStream = new FakeInputStream(len, true)) {
            assertEquals(len, JunitHelper.consumeInputStreamPerByte(fakeInputStream));
        }
        try (FakeInputStream fakeInputStream = new FakeInputStream(len, false)) {
            assertEquals(len, JunitHelper.consumeInputStreamPerByte(fakeInputStream));
        }
    }

    @Test
    public void testReadByteArray() {
        int len = 100;
        try (FakeInputStream fakeInputStream = new FakeInputStream(len, true)) {
            assertEquals(len, JunitHelper.consumeInputStream(fakeInputStream));
        }
        try (FakeInputStream fakeInputStream = new FakeInputStream(len, false)) {
            assertEquals(len, JunitHelper.consumeInputStream(fakeInputStream));
        }
        len = 1000000;
        try (FakeInputStream fakeInputStream = new FakeInputStream(len, true)) {
            assertEquals(len, JunitHelper.consumeInputStream(fakeInputStream));
        }
        try (FakeInputStream fakeInputStream = new FakeInputStream(len, false)) {
            assertEquals(len, JunitHelper.consumeInputStream(fakeInputStream));
        }
    }

    @Test
    public void testAvailable() {
        final int len = 100;
        try (FakeInputStream fakeInputStream = new FakeInputStream(len, true)) {
            assertEquals(len, fakeInputStream.available());
            fakeInputStream.read();
            assertEquals(len - 1, fakeInputStream.available());
            assertEquals(1, fakeInputStream.readCount());
        }
        try (FakeInputStream fakeInputStream = new FakeInputStream(len, false)) {
            assertEquals(len, fakeInputStream.available());
            fakeInputStream.read();
            assertEquals(len - 1, fakeInputStream.available());
            assertEquals(1, fakeInputStream.readCount());
        }

    }

}

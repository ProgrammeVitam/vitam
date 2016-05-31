package fr.gouv.vitam.common.collection;

import static org.junit.Assert.*;

import org.junit.Test;

public class EmptyCloseableIteratorTest {

    @Test
    public final void test() {
        EmptyCloseableIterator<Object> emptyCloseableIterator0 = new EmptyCloseableIterator<Object>();
        Object object0 = emptyCloseableIterator0.next();
        assertNull(object0);
        emptyCloseableIterator0 = new EmptyCloseableIterator<Object>();
        emptyCloseableIterator0.close();
        assertFalse(emptyCloseableIterator0.hasNext());
        emptyCloseableIterator0 = new EmptyCloseableIterator<Object>();
        assertFalse(emptyCloseableIterator0.hasNext());
    }

}

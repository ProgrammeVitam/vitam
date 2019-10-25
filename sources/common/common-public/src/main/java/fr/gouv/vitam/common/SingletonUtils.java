/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
package fr.gouv.vitam.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Singleton utility class
 */
public final class SingletonUtils {
    private static final byte[] SINGLETON_BYTE_ARRAY = new byte[0];
    private static final InputStream SINGLETON_INPUTSTREAM = new NullInputStream();
    private static final OutputStream SINGLETON_OUTPUTSTREAM = new VoidOutputStream();

    private SingletonUtils() {
        // empty
    }


    /**
     * Immutable empty byte array
     *
     * @return a Byte Array Singleton
     */
    public static final byte[] getSingletonByteArray() {
        return SINGLETON_BYTE_ARRAY;
    }

    /**
     * Immutable empty List
     *
     * @return an immutable empty List
     */
    public static final <E> List<E> singletonList() {
        return Collections.emptyList();
    }

    /**
     * Immutable empty Set
     *
     * @return an immutable empty Set
     */
    public static final <E> Set<E> singletonSet() {
        return Collections.emptySet();
    }

    /**
     * Immutable empty Map
     *
     * @return an immutable empty Map
     */
    public static final <E, V> Map<E, V> singletonMap() {
        return Collections.emptyMap();
    }

    /**
     * Immutable empty Iterator
     *
     * @return an immutable empty Iterator
     */
    public static final <E> Iterator<E> singletonIterator() {
        return Collections.emptyIterator();
    }

    /**
     * Empty InputStream
     */
    private static class NullInputStream extends InputStream {
        @Override
        public int read() {
            return -1;
        }

        @Override
        public int available() {
            return 0;
        }

        @Override
        public void close() {
            // Empty
        }

        @Override
        public void mark(int arg0) {
            // Empty
        }

        @Override
        public boolean markSupported() {
            return true;
        }

        @Override
        public int read(byte[] arg0, int arg1, int arg2) {
            return -1;
        }

        @Override
        public int read(byte[] arg0) {
            return -1;
        }

        @Override
        public void reset() {
            // Empty
        }

        @Override
        public long skip(long arg0) {
            return 0;
        }
    }

    /**
     * Immutable empty InputStream
     *
     * @return an immutable empty InputStream
     */
    public static final InputStream singletonInputStream() {
        return SINGLETON_INPUTSTREAM;
    }

    /**
     * OutputStream discarding all writed elements
     */
    private static class VoidOutputStream extends OutputStream {
        @Override
        public void close() throws IOException {
            // Empty
        }

        @Override
        public void flush() throws IOException {
            // Empty
        }

        @Override
        public void write(byte[] arg0, int arg1, int arg2) throws IOException {
            // Empty
        }

        @Override
        public void write(byte[] arg0) throws IOException {
            // Empty
        }

        @Override
        public void write(int arg0) throws IOException {
            // Empty
        }
    }

    /**
     * Immutable void OutputStream. Any write elements are discarder (equivalent to /dev/null).
     *
     * @return an immutable empty OutputStream
     */
    public static final OutputStream singletonOutputStream() {
        return SINGLETON_OUTPUTSTREAM;
    }

}

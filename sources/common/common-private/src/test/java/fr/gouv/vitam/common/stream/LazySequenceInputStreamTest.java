/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "https://cecill.info".
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
package fr.gouv.vitam.common.stream;

import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.io.input.NullInputStream;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

public class LazySequenceInputStreamTest {

    @Test
    public void testEmptyIterator() {
        // Given
        Iterator<InputStream> inputStreamIterator = IteratorUtils.emptyIterator();

        // When
        LazySequenceInputStream lazySequenceInputStream = new LazySequenceInputStream(inputStreamIterator);

        // Then
        assertThat(lazySequenceInputStream).hasSameContentAs(InputStream.nullInputStream());
    }

    @Test
    public void testSingleEmptyStreamIterator() {
        // Given
        Iterator<InputStream> inputStreamIterator = List.of(InputStream.nullInputStream()).iterator();

        // When
        LazySequenceInputStream lazySequenceInputStream = new LazySequenceInputStream(inputStreamIterator);

        // Then
        assertThat(lazySequenceInputStream).hasSameContentAs(InputStream.nullInputStream());
    }

    @Test
    public void testSingleStreamIterator() {
        // Given
        Iterator<InputStream> inputStreamIterator = List.of(
            (InputStream) new ByteArrayInputStream("data".getBytes())
        ).iterator();

        // When
        LazySequenceInputStream lazySequenceInputStream = new LazySequenceInputStream(inputStreamIterator);

        // Then
        assertThat(lazySequenceInputStream).hasSameContentAs(new ByteArrayInputStream("data".getBytes()));
    }

    @Test
    public void testMultipleStreamIterators() {
        // Given
        Iterator<InputStream> inputStreamIterator = List.of(
            new ByteArrayInputStream("my".getBytes()),
            InputStream.nullInputStream(),
            new ByteArrayInputStream("tailor".getBytes()),
            new ByteArrayInputStream("is".getBytes()),
            new ByteArrayInputStream("rich".getBytes())
        ).iterator();

        // When
        LazySequenceInputStream lazySequenceInputStream = new LazySequenceInputStream(inputStreamIterator);

        // Then
        assertThat(lazySequenceInputStream).hasSameContentAs(new ByteArrayInputStream("mytailorisrich".getBytes()));
    }

    @Test
    public void testMultipleLargeStreamIterators() throws IOException {
        // Given
        Iterator<InputStream> inputStreamIterator = List.<InputStream>of(
            new NullInputStream(10_000_000L),
            new NullInputStream(20_000_000L)
        ).iterator();

        // When
        LazySequenceInputStream lazySequenceInputStream = new LazySequenceInputStream(inputStreamIterator);

        // Then
        assertThat(lazySequenceInputStream).hasSameContentAs(
            new ExactSizeInputStream(new NullInputStream(30_000_000L), 30_000_000L)
        );
    }

    @Test
    public void testMultipleStreamIteratorLaziness() throws IOException {
        // Given
        AtomicInteger nbLoadedInputStreams = new AtomicInteger(0);
        Iterator<InputStream> inputStreamIterator = IntStream.range(0, 7)
            .<InputStream>mapToObj(i -> {
                nbLoadedInputStreams.incrementAndGet();
                return new ByteArrayInputStream(Integer.toString(i).getBytes());
            })
            .iterator();

        // When
        LazySequenceInputStream lazySequenceInputStream = new LazySequenceInputStream(inputStreamIterator);

        // Then

        // No input stream loaded yet
        assertThat(nbLoadedInputStreams.get()).isEqualTo(0);

        // Reading first byte only consumes the first inputStream
        assertThat(lazySequenceInputStream.read()).isEqualTo('0');
        assertThat(nbLoadedInputStreams.get()).isEqualTo(1);

        // Reading next byte only consumes the next inputStream
        byte[] buff = new byte[1];
        assertThat(lazySequenceInputStream.read(buff)).isEqualTo(1);
        assertThat(buff).containsExactly('1');
        assertThat(nbLoadedInputStreams.get()).isEqualTo(2);

        // Reading next byte only consumes the next inputStream
        assertThat(lazySequenceInputStream.read(buff)).isEqualTo(1);
        assertThat(buff).containsExactly('2');
        assertThat(nbLoadedInputStreams.get()).isEqualTo(3);

        // Closing stream does not consume remaining inputStreams
        lazySequenceInputStream.close();
        assertThat(nbLoadedInputStreams.get()).isEqualTo(3);
    }
}

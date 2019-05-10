package fr.gouv.vitam.common.iterables;

import org.apache.commons.collections4.IteratorUtils;
import org.junit.Test;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class BulkIteratorTest {

    @Test
    public void testIteratorWithDifferentSizes() {

        final int BUFFER_SIZE = 10;

        for (int iteratorSize = 0; iteratorSize < 30; iteratorSize++) {

            Iterator<Integer> iterator = Stream
                .iterate(0, i -> i + 1)
                .limit(iteratorSize)
                .iterator();

            BulkIterator<Integer> bulkIterator = new BulkIterator<>(iterator, BUFFER_SIZE);

            List<List<Integer>> partitions = IteratorUtils.toList(bulkIterator);

            int lastValue = 0;
            for (int i = 0; i < partitions.size(); i++) {
                List<Integer> partition = partitions.get(i);

                // Ensure all values are present
                for (Integer integer : partition) {
                    assertThat(integer).isEqualTo(lastValue);
                    lastValue++;
                }

                // Ensure all partitions have BUFFER_SIZE, except for last one
                if (i < partitions.size() - 1) {
                    assertThat(partition).hasSize(BUFFER_SIZE);
                } else {
                    assertThat(partition.size()).isLessThanOrEqualTo(BUFFER_SIZE);
                }
            }
            assertThat(lastValue).isEqualTo(iteratorSize);
        }
    }
}

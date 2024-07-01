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

package fr.gouv.vitam.storage.engine.server.offerdiff.sort;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterators;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.Function;
import java.util.function.Supplier;

public class LargeFileSorter<T> {

    public static final int INITIAL_FILE_CHUNK_SIZE_TO_SORT_IN_MEMORY = 100_000;
    public static final int NB_FILES_TO_MERGE_SORT = 10;

    private final int initialFileChunkSizeToSortInMemory;
    private final int nbFilesToMergeSort;

    private final Function<File, LargeFileReader<T>> fileReaderFactory;
    private final Function<File, LargeFileWriter<T>> fileWriterFactory;
    private final Comparator<T> entryComparator;
    private final Supplier<File> tempFileCreator;

    public LargeFileSorter(
        Function<File, LargeFileReader<T>> fileReaderFactory,
        Function<File, LargeFileWriter<T>> fileWriterFactory,
        Comparator<T> entryComparator,
        Supplier<File> tempFileCreator
    ) {
        this(
            INITIAL_FILE_CHUNK_SIZE_TO_SORT_IN_MEMORY,
            NB_FILES_TO_MERGE_SORT,
            fileReaderFactory,
            fileWriterFactory,
            entryComparator,
            tempFileCreator
        );
    }

    @VisibleForTesting
    LargeFileSorter(
        int initialFileChunkSizeToSortInMemory,
        int nbFilesToMergeSort,
        Function<File, LargeFileReader<T>> fileReaderFactory,
        Function<File, LargeFileWriter<T>> fileWriterFactory,
        Comparator<T> entryComparator,
        Supplier<File> tempFileCreator
    ) {
        this.initialFileChunkSizeToSortInMemory = initialFileChunkSizeToSortInMemory;
        this.nbFilesToMergeSort = nbFilesToMergeSort;
        this.fileReaderFactory = fileReaderFactory;
        this.fileWriterFactory = fileWriterFactory;
        this.entryComparator = entryComparator;
        this.tempFileCreator = tempFileCreator;
    }

    /**
     * Sorts a large file using entryComparator.
     *
     * @param inputFile the input stream to sort
     */
    public File sortLargeFile(File inputFile) throws IOException {
        try (LargeFileReader<T> reader = this.fileReaderFactory.apply(inputFile)) {
            List<File> filesToMerge = splitIntoInitialSortedChunkFiles(reader);
            return mergeSortFiles(filesToMerge);
        }
    }

    private List<File> splitIntoInitialSortedChunkFiles(LargeFileReader<T> reader) throws IOException {
        List<File> filesToMerge = new ArrayList<>();

        Iterator<List<T>> bulkIterator = Iterators.partition(reader, initialFileChunkSizeToSortInMemory);

        while (bulkIterator.hasNext()) {
            List<T> entries = bulkIterator.next();

            Iterator<T> entryIterator = entries.stream().sorted(this.entryComparator).iterator();

            File tempFile = writeToTempFile(entryIterator);
            filesToMerge.add(tempFile);
        }

        return filesToMerge;
    }

    private File mergeSortFiles(List<File> filesToMerge) throws IOException {
        if (filesToMerge.isEmpty()) {
            return writeEmptyFile();
        }

        Queue<File> remainingFilesToMerge = new LinkedList<>(filesToMerge);

        while (remainingFilesToMerge.size() > 1) {
            List<File> currentFilesToMerge = new ArrayList<>();
            while (currentFilesToMerge.size() < nbFilesToMergeSort && !remainingFilesToMerge.isEmpty()) {
                currentFilesToMerge.add(remainingFilesToMerge.poll());
            }

            List<LargeFileReader<T>> fileReadersToMerge = new ArrayList<>();
            try {
                for (File file : currentFilesToMerge) {
                    fileReadersToMerge.add(this.fileReaderFactory.apply(file));
                }

                Iterator<T> sortedLinesIterator = Iterators.mergeSorted(
                    IteratorUtils.asIterable(fileReadersToMerge.iterator()),
                    this.entryComparator
                );

                File tempFile = writeToTempFile(sortedLinesIterator);
                remainingFilesToMerge.add(tempFile);
            } finally {
                for (LargeFileReader<T> reader : fileReadersToMerge) {
                    reader.close();
                }
                for (File file : currentFilesToMerge) {
                    FileUtils.deleteQuietly(file);
                }
            }
        }

        return remainingFilesToMerge.poll();
    }

    private File writeToTempFile(Iterator<T> entryIterator) throws IOException {
        File tempFile = this.tempFileCreator.get();
        try (LargeFileWriter<T> writer = this.fileWriterFactory.apply(tempFile)) {
            while (entryIterator.hasNext()) {
                writer.writeEntry(entryIterator.next());
            }
        }
        return tempFile;
    }

    private File writeEmptyFile() throws IOException {
        File tmpEmptyFile = this.tempFileCreator.get();
        try (LargeFileWriter<T> emptyWriter = this.fileWriterFactory.apply(tmpEmptyFile)) {
            // Write nothing
        }
        return tmpEmptyFile;
    }
}

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

package fr.gouv.vitam.storage.offers.core.diag;

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.collection.CloseableIterator;
import fr.gouv.vitam.common.io.TempWorkspace;
import fr.gouv.vitam.common.largefilesorter.LargeFileSorter;
import fr.gouv.vitam.common.largefilesorter.ObjectEntryLargeFileReader;
import fr.gouv.vitam.common.largefilesorter.ObjectEntryLargeFileWriter;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.storage.ObjectEntry;
import fr.gouv.vitam.common.storage.cas.container.api.ContentAddressableStorage;
import fr.gouv.vitam.common.thread.ExecutorUtils;
import fr.gouv.vitam.common.thread.PeriodicTask;
import fr.gouv.vitam.common.thread.VitamThreadFactory;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.Order;
import fr.gouv.vitam.storage.offers.core.DefaultOfferService;
import fr.gouv.vitam.storage.offers.core.diag.OfferDiagReportEntry.ObjectState;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import org.apache.commons.collections4.iterators.PeekingIterator;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.lang3.time.StopWatch;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

public class OfferDiagProcess {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(OfferDiagProcess.class);
    private static final int OFFER_LOG_BATCH_SIZE = 10_000;
    private static final int FUTURE_TIMEOUT_MINUTES = 60;

    private final ContentAddressableStorage contentAddressableStorage;
    private final DefaultOfferService offerService;
    private final String containerName;
    private final OfferDiagStatus offerDiagStatus;
    private final int offerLogBatchSize;

    public OfferDiagProcess(
        ContentAddressableStorage contentAddressableStorage,
        DefaultOfferService offerService,
        String containerName
    ) {
        this(contentAddressableStorage, offerService, containerName, OFFER_LOG_BATCH_SIZE);
    }

    @VisibleForTesting
    OfferDiagProcess(
        ContentAddressableStorage contentAddressableStorage,
        DefaultOfferService offerService,
        String containerName,
        int offerLogBatchSize
    ) {
        this.contentAddressableStorage = contentAddressableStorage;
        this.offerService = offerService;
        this.containerName = containerName;
        this.offerLogBatchSize = offerLogBatchSize;
        this.offerDiagStatus = new OfferDiagStatus()
            .setRequestId(VitamThreadUtils.getVitamSession().getRequestId())
            .setTenantId(VitamThreadUtils.getVitamSession().getTenantId())
            .setContainer(containerName)
            .setStartDate(LocalDateUtil.nowFormatted())
            .setEndDate(null)
            .setReportFileName(null)
            .setStatusCode(StatusCode.UNKNOWN);
    }

    public void run() throws IOException, ContentAddressableStorageException {
        ExecutorService executor = Executors.newFixedThreadPool(2, VitamThreadFactory.getInstance());

        try (
            TempWorkspace tempWorkspace = new TempWorkspace(
                "offer_diag_" +
                offerDiagStatus.getRequestId() +
                "_" +
                LocalDateUtil.now().format(LocalDateUtil.getDateTimeFormatterForFileNames()) +
                "_"
            )
        ) {
            int tenant = VitamThreadUtils.getVitamSession().getTenantId();
            String requestId = VitamThreadUtils.getVitamSession().getRequestId();

            CompletableFuture<File> sortedObjectListingEntriesFileFuture = CompletableFuture.supplyAsync(
                () -> {
                    String originalThreadName = Thread.currentThread().getName();
                    try {
                        Thread.currentThread().setName("offer-diag-object-listing-" + containerName);
                        VitamThreadUtils.getVitamSession().setTenantId(tenant);
                        VitamThreadUtils.getVitamSession().setRequestId(requestId);
                        File objectListingEntriesFile = listContainerObjects(tempWorkspace);
                        return sortObjectListingEntries(tempWorkspace, objectListingEntriesFile);
                    } catch (IOException | ContentAddressableStorageException e) {
                        throw new RuntimeException(e);
                    } finally {
                        // Reset the thread name to its original value
                        Thread.currentThread().setName(originalThreadName);
                    }
                },
                executor
            );

            CompletableFuture<File> lastOfferLogListingEntriesFileFuture = CompletableFuture.supplyAsync(
                () -> {
                    String originalThreadName = Thread.currentThread().getName();
                    try {
                        Thread.currentThread().setName("offer-diag-offer-log-listing-" + containerName);
                        VitamThreadUtils.getVitamSession().setTenantId(tenant);
                        VitamThreadUtils.getVitamSession().setRequestId(requestId);
                        File offerLogListing = listOfferLogEntries(tempWorkspace);
                        File sortedOfferLogListingEntriesFile = sortOfferLogListingEntries(
                            tempWorkspace,
                            offerLogListing
                        );
                        return filterOfferLogListingKeepingLastEvents(tempWorkspace, sortedOfferLogListingEntriesFile);
                    } catch (IOException | ContentAddressableStorageException e) {
                        throw new RuntimeException(e);
                    } finally {
                        // Reset the thread name to its original value
                        Thread.currentThread().setName(originalThreadName);
                    }
                },
                executor
            );

            File sortedObjectListingEntriesFile;
            File lastOfferLogListingEntriesFile;

            try {
                // Use a timeout to prevent indefinite blocking
                sortedObjectListingEntriesFile = sortedObjectListingEntriesFileFuture.get(
                    FUTURE_TIMEOUT_MINUTES,
                    TimeUnit.MINUTES
                );
                lastOfferLogListingEntriesFile = lastOfferLogListingEntriesFileFuture.get(
                    FUTURE_TIMEOUT_MINUTES,
                    TimeUnit.MINUTES
                );
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Process interrupted", e);
            } catch (ExecutionException e) {
                throw new RuntimeException("Process failed", e);
            } catch (TimeoutException e) {
                throw new RuntimeException("Process timed out after " + FUTURE_TIMEOUT_MINUTES + " minutes", e);
            }

            compareListingsAndGenerateReport(sortedObjectListingEntriesFile, lastOfferLogListingEntriesFile);
        } catch (Exception e) {
            this.offerDiagStatus.setStatusCode(StatusCode.FATAL);
            throw e;
        } finally {
            this.offerDiagStatus.setEndDate(LocalDateUtil.nowFormatted());
            executor.shutdown();
        }
    }

    private void compareListingsAndGenerateReport(
        File sortedObjectListingEntriesFile,
        File lastOfferLogListingEntriesFile
    ) throws IOException {
        File reportFileName = new File(
            VitamConfiguration.getVitamTmpFolder(),
            "OfferDiag_" +
            LocalDateUtil.now().format(LocalDateUtil.getDateTimeFormatterForFileNames()) +
            "_" +
            offerDiagStatus.getRequestId() +
            "_" +
            containerName +
            "_Report.jsonl"
        );
        try (
            CloseableIterator<ObjectEntry> objectEntryIterator = new ObjectEntryLargeFileReader(
                sortedObjectListingEntriesFile
            );
            CloseableIterator<OfferLog> offerLogIterator = new OfferLogEntryLargeFileReader(
                lastOfferLogListingEntriesFile
            );
            OfferDiagReportWriter reportWriter = new OfferDiagReportWriter(reportFileName)
        ) {
            PeekingIterator<ObjectEntry> objectEntryPeekingIterator = new PeekingIterator<>(objectEntryIterator);
            PeekingIterator<OfferLog> offerLogEntryPeekingIterator = new PeekingIterator<>(offerLogIterator);

            while (objectEntryPeekingIterator.hasNext() || offerLogEntryPeekingIterator.hasNext()) {
                boolean pickObjectEntry =
                    objectEntryPeekingIterator.hasNext() &&
                    (!offerLogEntryPeekingIterator.hasNext() ||
                        objectEntryPeekingIterator
                                .peek()
                                .getObjectId()
                                .compareTo(offerLogEntryPeekingIterator.peek().getFileName()) <=
                            0);
                boolean pickOfferLogEntry =
                    offerLogEntryPeekingIterator.hasNext() &&
                    (!objectEntryPeekingIterator.hasNext() ||
                        objectEntryPeekingIterator
                                .peek()
                                .getObjectId()
                                .compareTo(offerLogEntryPeekingIterator.peek().getFileName()) >=
                            0);

                ObjectEntry objectEntry = pickObjectEntry ? objectEntryPeekingIterator.next() : null;
                OfferLog offerLog = pickOfferLogEntry ? offerLogEntryPeekingIterator.next() : null;

                if (objectEntry != null && offerLog != null) {
                    switch (offerLog.getAction()) {
                        case WRITE:
                            reportWriter.reportMatchingObject(offerLog.getFileName());
                            break;
                        case DELETE:
                            reportWriter.reportObjectMismatch(
                                objectEntry.getObjectId(),
                                objectEntry.getSize(),
                                LocalDateUtil.getFormattedDate(offerLog.getTime()),
                                ObjectState.ABSENT,
                                ObjectState.PRESENT
                            );
                            break;
                        default:
                            throw new IllegalStateException("Unexpected value: " + offerLog.getAction());
                    }
                } else if (objectEntry != null) {
                    reportWriter.reportObjectMismatch(
                        objectEntry.getObjectId(),
                        objectEntry.getSize(),
                        null,
                        ObjectState.ABSENT,
                        ObjectState.PRESENT
                    );
                } else if (offerLog != null) {
                    switch (offerLog.getAction()) {
                        case WRITE:
                            reportWriter.reportObjectMismatch(
                                offerLog.getFileName(),
                                null,
                                LocalDateUtil.getFormattedDate(offerLog.getTime()),
                                ObjectState.PRESENT,
                                ObjectState.ABSENT
                            );
                            break;
                        case DELETE:
                            reportWriter.reportMatchingObject(offerLog.getFileName());
                            break;
                        default:
                            throw new IllegalStateException("Unexpected value: " + offerLog.getAction());
                    }
                } else {
                    throw new IllegalStateException("Unexpected state");
                }
            }

            // Write status
            reportWriter.close();
            this.offerDiagStatus.setReportFileName(reportFileName.getAbsoluteFile().toString());
            this.offerDiagStatus.setTotalObjectCount(reportWriter.getTotalObjectCount());
            this.offerDiagStatus.setErrorCount(reportWriter.getErrorCount());
            this.offerDiagStatus.setStatusCode(reportWriter.getErrorCount() == 0 ? StatusCode.OK : StatusCode.KO);
        }

        // Cleanup to free up disk
        Files.delete(sortedObjectListingEntriesFile.toPath());
        Files.delete(lastOfferLogListingEntriesFile.toPath());
    }

    private File listContainerObjects(TempWorkspace tempWorkspace)
        throws IOException, ContentAddressableStorageException {
        LOGGER.info(
            "Listing container objects for " +
            containerName +
            ". This could take a while depending on your infrastructure and storage volume."
        );
        StopWatch stopWatch = new StopWatch();
        AtomicLong objectCount = new AtomicLong();

        File objectListingEntriesFile = tempWorkspace.tempFile();

        try (
            PeriodicTask _periodicLogger = ExecutorUtils.periodicTask(
                5_000,
                "ObjectListingStats",
                () -> LOGGER.info("Found " + objectCount.get() + " objects in " + formatDuration(stopWatch))
            )
        ) {
            try (ObjectEntryLargeFileWriter writer = new ObjectEntryLargeFileWriter(objectListingEntriesFile)) {
                contentAddressableStorage.listContainer(containerName, entry -> {
                    writer.writeEntry(entry);
                    objectCount.incrementAndGet();
                });
            } catch (ContentAddressableStorageNotFoundException e) {
                LOGGER.warn("empty container " + containerName);
            }
        }

        LOGGER.info(
            "Finished listing container objects. Total " + objectCount.get() + " in " + formatDuration(stopWatch)
        );
        return objectListingEntriesFile;
    }

    private File listOfferLogEntries(TempWorkspace tempWorkspace)
        throws ContentAddressableStorageException, IOException {
        LOGGER.info("Listing OfferLog entries for " + containerName + ". This could take a while...");
        StopWatch stopWatch = StopWatch.createStarted();
        AtomicLong objectCount = new AtomicLong();

        File offerLogListingEntriesFile = tempWorkspace.tempFile();

        try (
            PeriodicTask _periodicLogger = ExecutorUtils.periodicTask(
                5_000,
                "OfferLogListingStats",
                () -> LOGGER.info("Found " + objectCount.get() + " OfferLog entries in " + formatDuration(stopWatch))
            )
        ) {
            try (OfferLogEntryLargeFileWriter writer = new OfferLogEntryLargeFileWriter(offerLogListingEntriesFile)) {
                Long offset = null;
                while (true) {
                    List<OfferLog> offerLogs =
                        this.offerService.getOfferLogs(containerName, offset, offerLogBatchSize, Order.ASC);

                    for (OfferLog offerLog : offerLogs) {
                        writer.writeEntry(offerLog);
                        objectCount.incrementAndGet();
                    }

                    if (offerLogs.size() < offerLogBatchSize) {
                        break;
                    }
                    offset = offerLogs.get(offerLogs.size() - 1).getSequence() + 1;
                }
            }
        }
        LOGGER.info(
            "Finished listing offer log objects. Total " + objectCount.get() + " in " + formatDuration(stopWatch)
        );
        return offerLogListingEntriesFile;
    }

    private File sortObjectListingEntries(TempWorkspace tempWorkspace, File objectListingEntriesFile)
        throws IOException {
        StopWatch stopWatch = StopWatch.createStarted();
        LOGGER.info("Sorting objects listing for " + containerName + "...");

        LargeFileSorter<ObjectEntry> objectEntryLargeFileSorter = new LargeFileSorter<>(
            ObjectEntryLargeFileReader::new,
            ObjectEntryLargeFileWriter::new,
            Comparator.comparing(ObjectEntry::getObjectId),
            () -> {
                try {
                    return tempWorkspace.tempFile();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        );
        File sortedFile = objectEntryLargeFileSorter.sortLargeFile(objectListingEntriesFile);
        LOGGER.info("Object listing sorted successfully (took " + formatDuration(stopWatch) + ")");

        // Cleanup to free up disk
        Files.delete(objectListingEntriesFile.toPath());

        return sortedFile;
    }

    private File sortOfferLogListingEntries(TempWorkspace tempWorkspace, File offerLogListingEntriesFile)
        throws IOException {
        StopWatch stopWatch = StopWatch.createStarted();
        LOGGER.info("Sorting OfferLog listing for " + containerName + "...");

        LargeFileSorter<OfferLog> objectEntryLargeFileSorter = new LargeFileSorter<>(
            OfferLogEntryLargeFileReader::new,
            OfferLogEntryLargeFileWriter::new,
            Comparator.comparing(OfferLog::getFileName).thenComparing(OfferLog::getSequence),
            () -> {
                try {
                    return tempWorkspace.tempFile();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        );
        File sortedFile = objectEntryLargeFileSorter.sortLargeFile(offerLogListingEntriesFile);
        LOGGER.info("OfferLog listing sorted successfully (took " + formatDuration(stopWatch) + ")");

        // Cleanup to free up disk
        Files.delete(offerLogListingEntriesFile.toPath());

        return sortedFile;
    }

    private File filterOfferLogListingKeepingLastEvents(
        TempWorkspace tempWorkspace,
        File sortedOfferLogListingEntriesFile
    ) throws IOException {
        StopWatch stopWatch = StopWatch.createStarted();
        LOGGER.info("Filtering old OfferLog entries for " + containerName + "...");

        File filtredOfferLogListingFile = tempWorkspace.tempFile();
        try (
            OfferLogEntryLargeFileReader reader = new OfferLogEntryLargeFileReader(sortedOfferLogListingEntriesFile);
            OfferLogEntryLargeFileWriter writer = new OfferLogEntryLargeFileWriter(filtredOfferLogListingFile)
        ) {
            PeekingIterator<OfferLog> peekingIterator = new PeekingIterator<>(reader);

            while (peekingIterator.hasNext()) {
                OfferLog offerLog = peekingIterator.next();
                while (
                    peekingIterator.hasNext() &&
                    Objects.equals(offerLog.getFileName(), peekingIterator.peek().getFileName())
                ) {
                    offerLog = peekingIterator.next();
                }
                writer.writeEntry(offerLog);
            }
        }

        LOGGER.info("Old OfferLog entries filtered successfully (took " + formatDuration(stopWatch) + ")");

        // Cleanup to free up disk
        Files.delete(sortedOfferLogListingEntriesFile.toPath());

        return filtredOfferLogListingFile;
    }

    private static String formatDuration(StopWatch stopWatch) {
        return DurationFormatUtils.formatDuration(stopWatch.getTime(), "[d'd']HH:mm:ss");
    }

    public OfferDiagStatus getOfferDiagStatus() {
        return offerDiagStatus;
    }
}

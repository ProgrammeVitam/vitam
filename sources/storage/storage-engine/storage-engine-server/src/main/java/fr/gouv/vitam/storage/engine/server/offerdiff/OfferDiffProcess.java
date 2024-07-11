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

package fr.gouv.vitam.storage.engine.server.offerdiff;

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.collection.CloseableIterator;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.storage.ObjectEntry;
import fr.gouv.vitam.common.model.storage.ObjectEntryReader;
import fr.gouv.vitam.common.model.storage.ObjectEntryWriter;
import fr.gouv.vitam.common.thread.VitamThreadFactory;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.referential.model.OfferReference;
import fr.gouv.vitam.storage.engine.server.distribution.StorageDistribution;
import fr.gouv.vitam.storage.engine.server.offerdiff.sort.LargeFileSorter;
import fr.gouv.vitam.storage.engine.server.offerdiff.sort.ObjectEntryLargeFileReader;
import fr.gouv.vitam.storage.engine.server.offerdiff.sort.ObjectEntryLargeFileWriter;
import org.apache.commons.collections4.iterators.PeekingIterator;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class OfferDiffProcess {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(OfferDiffProcess.class);

    private final StorageDistribution distribution;
    private final String offer1;
    private final String offer2;
    private final DataCategory dataCategory;

    private OfferDiffStatus offerDiffStatus;

    public OfferDiffProcess(StorageDistribution distribution, String offer1, String offer2, DataCategory dataCategory) {
        this.distribution = distribution;
        this.offer1 = offer1;
        this.offer2 = offer2;
        this.dataCategory = dataCategory;

        this.offerDiffStatus = new OfferDiffStatus()
            .setRequestId(VitamThreadUtils.getVitamSession().getRequestId())
            .setTenantId(VitamThreadUtils.getVitamSession().getTenantId())
            .setStatusCode(StatusCode.UNKNOWN)
            .setOffer1(offer1)
            .setOffer2(offer2)
            .setContainer(dataCategory.getCollectionName())
            .setStartDate(getCurrentDate())
            .setEndDate(null);
    }

    public void run() {
        File diffOperationTempDir = null;
        try {
            LOGGER.info(
                String.format(
                    "[OfferDiff] Start offer diff process for offers '{%s}' and '{%s}' for category {%s}.",
                    offer1,
                    offer2,
                    dataCategory
                )
            );

            Set<String> offerIds = distribution
                .getStrategies()
                .values()
                .stream()
                .flatMap(s -> s.getOffers().stream())
                .map(OfferReference::getId)
                .collect(Collectors.toSet());

            if (!offerIds.contains(offer1)) {
                throw new IllegalArgumentException("Invalid offer1 : '" + offer1 + "'");
            }

            if (!offerIds.contains(offer2)) {
                throw new IllegalArgumentException("Invalid offer2 : '" + offer2 + "'");
            }

            diffOperationTempDir = createProcessTempStorageDir();

            process(offer1, offer2, dataCategory, diffOperationTempDir);

            LOGGER.info("[OfferDiff] Offer diff completed successfully");
            this.offerDiffStatus.setStatusCode(
                    this.offerDiffStatus.getErrorCount() == 0 ? StatusCode.OK : StatusCode.WARNING
                );
        } catch (Exception e) {
            LOGGER.error("[OfferDiff]: An exception has been thrown during offer diff process.", e);
            this.offerDiffStatus.setStatusCode(StatusCode.KO);
            cleanupTempDirFilesQuietly(diffOperationTempDir);
        } finally {
            this.offerDiffStatus.setEndDate(getCurrentDate());

            LOGGER.info("[OfferDiff] Offer diff result:\n" + JsonHandler.prettyPrint(this.offerDiffStatus));
        }
    }

    private void process(String offer1, String offer2, DataCategory dataCategory, File diffOperationTempDir)
        throws StorageException {
        LOGGER.info("[OfferDiff] Listing offer files...");

        ExecutorService executor = Executors.newFixedThreadPool(2, VitamThreadFactory.getInstance());
        int tenant = VitamThreadUtils.getVitamSession().getTenantId();
        String requestId = VitamThreadUtils.getVitamSession().getRequestId();

        CompletableFuture<File> listOffers1CompletableFuture = CompletableFuture.supplyAsync(
            () -> listOfferObjects(offer1, dataCategory, diffOperationTempDir, tenant, requestId),
            executor
        );

        CompletableFuture<File> listOffers2CompletableFuture = CompletableFuture.supplyAsync(
            () -> listOfferObjects(offer2, dataCategory, diffOperationTempDir, tenant, requestId),
            executor
        );

        Optional<File> offerListing1 = await(listOffers1CompletableFuture, offer1);
        Optional<File> offerListing2 = await(listOffers2CompletableFuture, offer2);

        executor.shutdown();

        if (offerListing1.isEmpty() || offerListing2.isEmpty()) {
            throw new StorageException("One or more offer listing failed. Aborting");
        }

        LOGGER.info("[OfferDiff] Sorting offer files...");

        File sortedOfferListing1 = sort(offer1, offerListing1.get(), diffOperationTempDir);
        File sortedOfferListing2 = sort(offer2, offerListing2.get(), diffOperationTempDir);

        LOGGER.info("[OfferDiff] Comparing offer files...");
        try {
            File reportFile = createTempFile(
                diffOperationTempDir,
                "_" + this.offerDiffStatus.getRequestId() + ".jsonl"
            );

            try (ReportWriter reportWriter = new ReportWriter(reportFile)) {
                compareOfferListings(offer1, offer2, sortedOfferListing1, sortedOfferListing2, reportWriter);

                this.offerDiffStatus.setReportFileName(reportFile.getAbsoluteFile().toString());
                this.offerDiffStatus.setTotalObjectCount(reportWriter.getTotalObjectCount());
                this.offerDiffStatus.setErrorCount(reportWriter.getErrorCount());

                cleanupFiles(sortedOfferListing1, sortedOfferListing2);
            }
        } catch (IOException e) {
            throw new StorageException("Could not compare sorted offer listings", e);
        }
        LOGGER.info("[OfferDiff] Comparing offer files done successfully !");
    }

    private File listOfferObjects(
        String offerId,
        DataCategory dataCategory,
        File diffOperationTempDir,
        Integer tenant,
        String requestId
    ) {
        Thread.currentThread().setName("OfferListing-" + offerId);
        VitamThreadUtils.getVitamSession().setTenantId(tenant);
        VitamThreadUtils.getVitamSession().setRequestId(requestId);

        try (
            CloseableIterator<ObjectEntry> objectEntryIterator =
                this.distribution.listContainerObjectsForOffer(dataCategory, offerId, true)
        ) {
            File tempFile = createTempFile(diffOperationTempDir);

            try (
                FileOutputStream fos = new FileOutputStream(tempFile);
                ObjectEntryWriter objectEntryWriter = new ObjectEntryWriter(fos)
            ) {
                for (int i = 0; objectEntryIterator.hasNext(); i++) {
                    objectEntryWriter.write(objectEntryIterator.next());
                    if (i > 0 && i % 10000 == 0) {
                        LOGGER.info("[OfferDiff] " + i + "/? files listed from " + offerId + "/" + dataCategory);
                    }
                }
                objectEntryWriter.writeEof();
            }

            return tempFile;
        } catch (StorageException | IOException e) {
            throw new RuntimeException(
                "Could not list offer objects. OfferId: '" + offerId + "', dataCategory: " + dataCategory
            );
        }
    }

    private Optional<File> await(CompletableFuture<File> completableFuture, String offerId) {
        try {
            File offerListingFile = completableFuture.get();
            LOGGER.info("Offer listing succeeded for offer " + offerId);
            return Optional.of(offerListingFile);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.error("Interrupted thread while listing offer objects for " + offerId, e);
            return Optional.empty();
        } catch (ExecutionException e) {
            LOGGER.error("An error occurred during offer listing for " + offerId, e);
            return Optional.empty();
        }
    }

    private File sort(String offerId, File offerListing, File diffOperationTempDir) throws StorageException {
        LOGGER.info("[OfferDiff] Sorting offer file " + offerId);

        try {
            LargeFileSorter<ObjectEntry> objectEntryLargeFileSorter = new LargeFileSorter<>(
                ObjectEntryLargeFileReader::new,
                ObjectEntryLargeFileWriter::new,
                Comparator.comparing(ObjectEntry::getObjectId),
                () -> {
                    try {
                        return createTempFile(diffOperationTempDir);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
            );

            File result = objectEntryLargeFileSorter.sortLargeFile(offerListing);

            LOGGER.info("[OfferDiff] Offer listing file " + offerId + " sorted successfully !");

            return result;
        } catch (IOException e) {
            throw new StorageException("Cloud not sort offer listing file for offer " + offerId, e);
        } finally {
            FileUtils.deleteQuietly(offerListing);
        }
    }

    private void compareOfferListings(
        String offer1,
        String offer2,
        File sortedOfferListing1,
        File sortedOfferListing2,
        ReportWriter reportWriter
    ) throws IOException {
        try (
            InputStream inputStream1 = new FileInputStream(sortedOfferListing1);
            ObjectEntryReader objectEntryReader1 = new ObjectEntryReader(inputStream1);
            InputStream inputStream2 = new FileInputStream(sortedOfferListing2);
            ObjectEntryReader objectEntryReader2 = new ObjectEntryReader(inputStream2)
        ) {
            PeekingIterator<ObjectEntry> peekingIterator1 = new PeekingIterator<>(objectEntryReader1);
            PeekingIterator<ObjectEntry> peekingIterator2 = new PeekingIterator<>(objectEntryReader2);

            while (peekingIterator1.hasNext() || peekingIterator2.hasNext()) {
                if (peekingIterator1.hasNext() && peekingIterator2.hasNext()) {
                    String objectId1 = peekingIterator1.peek().getObjectId();
                    String objectId2 = peekingIterator2.peek().getObjectId();
                    if (objectId1.equals(objectId2)) {
                        reportDifference(
                            peekingIterator1.next(),
                            peekingIterator2.next(),
                            offer1,
                            offer2,
                            reportWriter
                        );
                    } else if (objectId1.compareTo(objectId2) < 0) {
                        reportDifference(peekingIterator1.next(), null, offer1, offer2, reportWriter);
                    } else {
                        reportDifference(null, peekingIterator2.next(), offer1, offer2, reportWriter);
                    }
                } else if (peekingIterator1.hasNext()) {
                    reportDifference(peekingIterator1.next(), null, offer1, offer2, reportWriter);
                } else {
                    reportDifference(null, peekingIterator2.next(), offer1, offer2, reportWriter);
                }
            }
        }
    }

    private void reportDifference(
        ObjectEntry objectEntry1,
        ObjectEntry objectEntry2,
        String offer1,
        String offer2,
        ReportWriter reportWriter
    ) throws IOException {
        if (objectEntry1 != null && objectEntry2 != null) {
            if (objectEntry1.getSize() == objectEntry2.getSize()) {
                LOGGER.debug("File " + objectEntry1.getObjectId() + " matches. Size: " + objectEntry1.getSize());
                reportWriter.reportMatchingObject(objectEntry1.getObjectId());
            } else {
                LOGGER.warn(
                    "File " +
                    objectEntry1.getObjectId() +
                    " found on both offers, but size mismatches. " +
                    offer1 +
                    ": " +
                    objectEntry1.getSize() +
                    "bytes , " +
                    offer2 +
                    ": " +
                    objectEntry2.getSize() +
                    " bytes"
                );
                reportWriter.reportObjectMismatch(
                    objectEntry1.getObjectId(),
                    objectEntry1.getSize(),
                    objectEntry2.getSize()
                );
            }
        } else if (objectEntry1 != null) {
            LOGGER.warn(
                "File " +
                objectEntry1.getObjectId() +
                " found on offer " +
                offer1 +
                " (" +
                objectEntry1.getSize() +
                "bytes), but is missing from offer " +
                offer2
            );
            reportWriter.reportObjectMismatch(objectEntry1.getObjectId(), objectEntry1.getSize(), null);
        } else {
            LOGGER.warn(
                "File " +
                objectEntry2.getObjectId() +
                " found on offer " +
                offer2 +
                " (" +
                objectEntry2.getSize() +
                "bytes), but is missing from offer " +
                offer1
            );
            reportWriter.reportObjectMismatch(objectEntry2.getObjectId(), null, objectEntry2.getSize());
        }
    }

    private void cleanupFiles(File... files) {
        for (File file : files) {
            FileUtils.deleteQuietly(file);
        }
    }

    public boolean isRunning() {
        return this.offerDiffStatus.getEndDate() == null;
    }

    public OfferDiffStatus getOfferDiffStatus() {
        return offerDiffStatus;
    }

    private String getCurrentDate() {
        return LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now());
    }

    private File createTempFile(File diffOperationTempDir) throws IOException {
        return createTempFile(diffOperationTempDir, null);
    }

    private File createTempFile(File diffOperationTempDir, String suffix) throws IOException {
        return File.createTempFile("offer_diff_", suffix, diffOperationTempDir);
    }

    private void cleanupTempDirFilesQuietly(File diffOperationTempDir) {
        try {
            if (diffOperationTempDir == null) {
                return;
            }
            FileUtils.forceDelete(diffOperationTempDir);
        } catch (IOException ex) {
            LOGGER.error("Could not clean temp folder");
        }
    }

    private File createProcessTempStorageDir() throws IOException {
        File tempDir = new File(VitamConfiguration.getVitamTmpFolder());
        File diffOperationTempDir = new File(
            tempDir,
            "offer_diff_" + VitamThreadUtils.getVitamSession().getRequestId()
        );
        FileUtils.forceMkdir(diffOperationTempDir);
        return diffOperationTempDir;
    }
}

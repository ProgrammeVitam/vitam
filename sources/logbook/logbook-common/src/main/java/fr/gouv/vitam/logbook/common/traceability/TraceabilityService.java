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
package fr.gouv.vitam.logbook.common.traceability;

import fr.gouv.vitam.common.BaseXx;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.TimeStampException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.security.merkletree.MerkleTree;
import fr.gouv.vitam.common.security.merkletree.MerkleTreeAlgo;
import fr.gouv.vitam.common.timestamp.TimestampGenerator;
import fr.gouv.vitam.logbook.common.exception.TraceabilityException;
import fr.gouv.vitam.logbook.common.model.TraceabilityEvent;
import fr.gouv.vitam.logbook.common.model.TraceabilityFile;
import fr.gouv.vitam.logbook.common.model.TraceabilityStatistics;
import org.apache.commons.compress.archivers.ArchiveException;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service used to make the generic traceability algo.
 */
public class TraceabilityService {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(TraceabilityService.class);

    private final TimestampGenerator timestampGenerator;
    private final Integer tenantId;
    private final LogbookTraceabilityHelper helper;
    private final File tmpFolder;
    private final DateTimeFormatter formatter;
    private static final String SECURISATION_VERSION = "V1";

    private File zipFile = null;

    /**
     * @param timestampGenerator Service used to generate timestamp for the traceability
     * @param traceabilityHelper Implementation to handle specific method or values depending on the traceability type
     * @param tenantId used tenantId for the traceability
     * @param tmpFolder Folder witch one we store the ZipFile
     */
    public TraceabilityService(
        TimestampGenerator timestampGenerator,
        LogbookTraceabilityHelper traceabilityHelper,
        Integer tenantId,
        File tmpFolder
    ) {
        this.timestampGenerator = timestampGenerator;
        this.tenantId = tenantId;
        this.helper = traceabilityHelper;

        this.tmpFolder = tmpFolder;
        formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        tmpFolder.mkdir();
    }

    /**
     * Initialize and do the traceability process.<br>
     * The generated GUID for the operation is gettable after the operation finish with getOperationID.
     *
     * @throws TraceabilityException if any error or problem occurs
     */
    public void secureData(String strategyId) throws TraceabilityException {
        helper.startTraceability();

        TraceabilityEvent event;

        // Call createZipFile
        String fileName = createZipFile(
            tenantId,
            LocalDateUtil.parseMongoFormattedDate(helper.getTraceabilityEndDate())
        );

        try (TraceabilityFile traceabilityFile = new TraceabilityFile(zipFile)) {
            // Create new merkleTreeAlgo
            final MerkleTreeAlgo merkleAlgo = new MerkleTreeAlgo(VitamConfiguration.getDefaultDigestType());

            // Call storeExtractedData
            helper.saveDataInZip(merkleAlgo, traceabilityFile);

            // Get MerkleTree with given MerkleTreeAlgo
            final MerkleTree merkleTree = merkleAlgo.generateMerkle();

            if (merkleTree != null) {
                traceabilityFile.storeMerkleTree(merkleTree);
                byte[] merkleRootHash = merkleTree.getRoot();
                String rootHash = BaseXx.getBase64(merkleRootHash);

                // Compute and store token
                byte[] timestampToken = computeAndStoreTimestampToken(traceabilityFile, merkleRootHash);

                final long numberOfLine = helper.getDataSize();
                final String startDate = helper.getTraceabilityStartDate();
                final String endDate = helper.getTraceabilityEndDate();

                traceabilityFile.storeAdditionalInformation(numberOfLine, startDate, endDate);

                // fill traceability event
                String previousDate = helper.getPreviousStartDate();
                String previousMonthDate = helper.getPreviousMonthStartDate();
                String previousYearDate = helper.getPreviousYearStartDate();
                long size = zipFile.length();
                boolean maxEntriesReached = helper.getMaxEntriesReached();
                TraceabilityStatistics traceabilityStatistics = helper.getTraceabilityStatistics();

                event = new TraceabilityEvent(
                    helper.getTraceabilityType(),
                    startDate,
                    endDate,
                    rootHash,
                    timestampToken,
                    previousDate,
                    previousMonthDate,
                    previousYearDate,
                    numberOfLine,
                    fileName,
                    size,
                    VitamConfiguration.getDefaultDigestType(),
                    maxEntriesReached,
                    SECURISATION_VERSION,
                    traceabilityStatistics
                );
            } else {
                // do nothing, nothing to be handled
                LOGGER.warn("No entries to be processed");
                helper.saveEmpty(tenantId);
                if (!zipFile.delete()) {
                    LOGGER.error("Unable to delete zipFile");
                }
                return;
            }
        } catch (IOException | ArchiveException | InvalidParseOperationException e) {
            helper.createLogbookOperationEvent(tenantId, helper.getStepName(), StatusCode.FATAL, null);

            if (!zipFile.delete()) {
                LOGGER.error("Can't delete zipFile");
            }
            throw new TraceabilityException(e);
        }

        helper.storeAndDeleteZip(tenantId, strategyId, zipFile, fileName, event);
        helper.createLogbookOperationEvent(tenantId, helper.getStepName(), StatusCode.OK, event);
    }

    private String createZipFile(Integer tenantId, LocalDateTime date) {
        final String fileName = String.format("%d_%s_%s.zip", tenantId, helper.getZipName(), date.format(formatter));

        zipFile = new File(tmpFolder, fileName);
        return fileName;
    }

    private byte[] computeAndStoreTimestampToken(TraceabilityFile file, byte[] merkleRootHash)
        throws IOException, TraceabilityException, InvalidParseOperationException {
        final String rootHash = BaseXx.getBase64(merkleRootHash);

        final byte[] timestampToken1 = helper.getPreviousTimestampToken();
        final byte[] timestampToken2 = helper.getPreviousMonthTimestampToken();
        final byte[] timestampToken3 = helper.getPreviousYearTimestampToken();

        final String timestampToken1Base64 = (timestampToken1 == null) ? null : BaseXx.getBase64(timestampToken1);
        final String timestampToken2Base64 = (timestampToken2 == null) ? null : BaseXx.getBase64(timestampToken2);
        final String timestampToken3Base64 = (timestampToken3 == null) ? null : BaseXx.getBase64(timestampToken3);

        final byte[] timestampToken = generateTimeStampToken(
            merkleRootHash,
            timestampToken1,
            timestampToken2,
            timestampToken3
        );

        file.storeTimeStampToken(timestampToken);
        file.storeComputedInformation(rootHash, timestampToken1Base64, timestampToken2Base64, timestampToken3Base64);

        return timestampToken;
    }

    private byte[] generateTimeStampToken(byte[] rootHash, byte[] hash1, byte[] hash2, byte[] hash3)
        throws TraceabilityException {
        try {
            TimeStampService timeStampService = new TimeStampService();
            byte[] hashDigest = timeStampService.getDigestFrom(rootHash, hash1, hash2, hash3);
            DigestType digestType = timeStampService.getDigestType();

            // TODO maybe nonce could be different than null ? If so, think about changing VerifyTimeStampActionHandler
            final byte[] timeStampToken = timestampGenerator.generateToken(hashDigest, digestType, null);

            helper.createLogbookOperationEvent(tenantId, helper.getTimestampStepName(), StatusCode.OK, null);
            return timeStampToken;
        } catch (final TimeStampException e) {
            LOGGER.error("unable to generate timestamp", e);
            helper.createLogbookOperationEvent(tenantId, helper.getTimestampStepName(), StatusCode.FATAL, null);
            throw new TraceabilityException(e);
        }
    }
}

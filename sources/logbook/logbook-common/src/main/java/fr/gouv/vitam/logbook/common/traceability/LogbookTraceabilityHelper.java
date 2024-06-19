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

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.security.merkletree.MerkleTreeAlgo;
import fr.gouv.vitam.logbook.common.exception.TraceabilityException;
import fr.gouv.vitam.logbook.common.model.TraceabilityEvent;
import fr.gouv.vitam.logbook.common.model.TraceabilityFile;
import fr.gouv.vitam.logbook.common.model.TraceabilityStatistics;
import fr.gouv.vitam.logbook.common.model.TraceabilityType;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Interface used to handle specific steps of the traceability process
 */
public interface LogbookTraceabilityHelper {
    LocalDateTime INITIAL_START_DATE = LocalDateUtil.EPOCH;

    /**
     * Search for entries to secure with the traceability process and store data in the given Traceability zip file.<br/>
     * Also, Merkle Algorithm should be updated by adding each wanted entry as a leaf of the tree.
     *
     * @param algo algorithm used to generate MerkleTree with data.
     * @param file output zip to store data (in some TYPE_COLLECTION.json)
     * @throws IOException if any error occurs while writing in traceabilityFile
     * @throws TraceabilityException for any other errors specifically in the search / data transformation
     */
    void saveDataInZip(MerkleTreeAlgo algo, TraceabilityFile file) throws IOException, TraceabilityException;

    /**
     * Prepare and start the traceability operation
     *
     * @throws TraceabilityException if any error occurs
     */
    void startTraceability() throws TraceabilityException;

    /**
     * Update or finalize the traceability entry in the logbook collection
     *
     * @param tenantId tenant used for the operation
     * @param eventType code of the eventType for the entry
     * @param status status of the entry
     * @param event data of the entry
     * @throws TraceabilityException if any error occurs
     */
    void createLogbookOperationEvent(Integer tenantId, String eventType, StatusCode status, TraceabilityEvent event)
        throws TraceabilityException;

    /**
     * Store the temporary zip.
     *
     * @param tenant tenant used for the operation
     * @param strategyId strategy used for the storage
     * @param zipFile file containing multiple information about the traceability operation (data, merkle, computingData, ...)
     * @param fileName name of the file on the destination
     * @param event traceability event that should be updated by this operation
     * @throws TraceabilityException if any error occurs
     */
    void storeAndDeleteZip(Integer tenant, String strategyId, File zipFile, String fileName, TraceabilityEvent event)
        throws TraceabilityException;

    /**
     * @return the traceabilityType of the implementation class
     */
    TraceabilityType getTraceabilityType();

    /**
     * @return the main eventType code for the specific implementation class
     */
    String getStepName();

    /**
     * @return the timestamp eventType code for the specific implementation class
     */
    String getTimestampStepName();

    /**
     * @return the name of the tmp zip file
     */
    String getZipName();

    /**
     * @return the start date of the traceability (computed from the end date of the last traceability)
     * @throws TraceabilityException on error
     */
    String getTraceabilityStartDate() throws TraceabilityException;

    /**
     * @return the end date of the traceability (now)
     * @throws TraceabilityException on error
     */
    String getTraceabilityEndDate() throws TraceabilityException;

    /**
     * Warning: This method MUST be used after "saveDataInZip" witch initialize traceabilityIterator
     *
     * @return the number of items secured in traceabilityIterator
     * @throws TraceabilityException if the traceabilityIterator isn't yet initialized
     */
    long getDataSize() throws TraceabilityException;

    /**
     * Get the last traceability (in logbook) event and return the timestampToken of the entry
     *
     * @return the timestamp token of the last traceability entry or null if no previous traceability OK for that type
     * @throws InvalidParseOperationException if any errors occurs while deserializing entry's data.
     */
    byte[] getPreviousTimestampToken() throws InvalidParseOperationException;

    /**
     * Get the first traceability (in logbook) one month before the given currentDate and return the timestampToken of the entry
     *
     * @return the timestamp token of the matching entry or null if no matching entry
     * @throws InvalidParseOperationException if any errors occurs while deserializing entry's data.
     * @throws TraceabilityException if any other error occurs
     */
    byte[] getPreviousMonthTimestampToken() throws InvalidParseOperationException, TraceabilityException;

    /**
     * Get the first traceability (in logbook) one year before the given currentDate and return the timestampToken of the entry
     *
     * @return the timestamp token of the matching entry or null if no matching entry
     * @throws InvalidParseOperationException if any errors occurs while deserializing entry's data.
     * @throws TraceabilityException if any other error occurs
     */
    byte[] getPreviousYearTimestampToken() throws InvalidParseOperationException, TraceabilityException;

    /**
     * Get the last traceability (in logbook) event and return the startDate of the entry
     *
     * @return the start date of the last traceability entry
     * @throws InvalidParseOperationException if any errors occurs while deserializing entry's data.
     */
    String getPreviousStartDate() throws InvalidParseOperationException;

    /**
     * Get the first traceability (in logbook) one month before the given currentDate and return the startDate of the entry
     *
     * @return the start date of the matching entry or null if no matching entry
     * @throws InvalidParseOperationException if any errors occurs while deserializing entry's data.
     * @throws TraceabilityException if any other error occurs
     */
    String getPreviousMonthStartDate() throws InvalidParseOperationException, TraceabilityException;

    /**
     * Get the first traceability (in logbook) one year before the given currentDate and return the startDate of the entry
     *
     * @return the start date of the matching entry or null if no matching entry
     * @throws InvalidParseOperationException if any errors occurs while deserializing entry's data.
     * @throws TraceabilityException if any other error occurs
     */
    String getPreviousYearStartDate() throws InvalidParseOperationException, TraceabilityException;

    /**
     * Save the close master event if needed
     *
     * @param tenantId the tenant used for log
     * @throws TraceabilityException
     */
    void saveEmpty(Integer tenantId) throws TraceabilityException;

    /**
     * @return true if max entries reached (unit &amp; object group lifecycle traceability operation are limited in size)
     */
    boolean getMaxEntriesReached();

    TraceabilityStatistics getTraceabilityStatistics();
}

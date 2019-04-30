/*******************************************************************************
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
 *******************************************************************************/
package fr.gouv.vitam.logbook.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.common.digest.DigestType;

import java.util.List;

/**
 * Class used to save traceability event to be store in logbook collection
 */
public class TraceabilityEvent {

    /**
     * traceability logbook type
     */
    private TraceabilityType logType;

    /**
     * traceability start date
     */
    private String startDate;

    /**
     * traceability end date
     */
    private String endDate;

    /**
     * time stamp token (base64 encoded)
     */
    private String hash;

    /**
     * time stamp token (base64 encoded)
     */
    private byte[] timeStampToken;

    /**
     * Start date of the previous traceability
     */
    private String previousLogbookTraceabilityDate;

    /**
     * Start date of the next traceability 1 month before - logbook.mounth(-1).next()
     */
    private String minusOneMonthLogbookTraceabilityDate;

    /**
     * Start date of the next traceability 1 year before - logbook.year(-1).next()
     */
    private String minusOneYearLogbookTraceabilityDate;

    /**
     * Number of securised elements
     */
    private long numberOfElements;

    /**
     * name of the secure archive in the storage
     */
    private String fileName;

    /**
     * Total size of the ZIP entry
     */
    private long size;
    /**
     * securisationVersion
     */
    private String securisationVersion;

    /**
     * Digest Algorithm
     */
    private DigestType digestAlgorithm;

    /**
     * Max entries reached (unit & object group lifecycle traceability operation are limited in size)
     */
    private boolean maxEntriesReached;

    /**
     * Traceability statistics
     */
    @JsonProperty("Warnings")
    private List<String> warnings;

    /**
     * Empty constructor for Jackson
     */
    public TraceabilityEvent() {
        // Empty
    }

    /**
     * Constructor
     * 
     * @param logType logType
     * @param startDate startDate
     * @param endDate endDate
     * @param hash  hash
     * @param timeStampToken timeStampToken
     * @param previousLogbookTraceabilityDate previousLogbookTraceabilityDate
     * @param minusOneMonthLogbookTraceabilityDate minusOneMonthLogbookTraceabilityDate
     * @param minusOneYearLogbookTraceabilityDate minusOneYearLogbookTraceabilityDate
     * @param numberOfElements numberOfElements
     * @param fileName fileName
     * @param size size
     * @param digestAlgorithm digestAlgorithm
     * @param warnings
     */
    public TraceabilityEvent(TraceabilityType logType, String startDate, String endDate, String hash,
        byte[] timeStampToken, String previousLogbookTraceabilityDate, String minusOneMonthLogbookTraceabilityDate,
        String minusOneYearLogbookTraceabilityDate, long numberOfElements, String fileName, long size,
        DigestType digestAlgorithm, boolean maxEntriesReached, String securisationVersion,
        List<String> warnings) {
        this.logType = logType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.previousLogbookTraceabilityDate = previousLogbookTraceabilityDate;
        this.hash = hash;
        this.minusOneMonthLogbookTraceabilityDate = minusOneMonthLogbookTraceabilityDate;
        this.minusOneYearLogbookTraceabilityDate = minusOneYearLogbookTraceabilityDate;
        this.timeStampToken = timeStampToken;
        this.numberOfElements = numberOfElements;
        this.fileName = fileName;
        this.size = size;
        this.digestAlgorithm = digestAlgorithm;
        this.maxEntriesReached = maxEntriesReached;
        this.securisationVersion = securisationVersion;

        this.warnings = warnings;
    }

    /**
     * @return startDate
     */
    public String getStartDate() {
        return startDate;
    }

    /**
     * @return endDate
     */
    public String getEndDate() {
        return endDate;
    }

    /**
     * @return hash
     */
    public String getHash() {
        return hash;
    }

    /**
     *  getter of numberOfElements
     * @return numberOfElements
     */
    public long getNumberOfElements() {
        return numberOfElements;
    }

    /**
     * @return file name
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * @return timestamp token
     */
    public byte[] getTimeStampToken() {
        return timeStampToken;
    }

    /**
     * @return log type
     */
    public TraceabilityType getLogType() {
        return logType;
    }

    /**
     * @return Date of the one month previous traceability logbook
     */
    public String getMinusOneMonthLogbookTraceabilityDate() {
        return minusOneMonthLogbookTraceabilityDate;
    }

    /**
     * @return Date of the one year previous traceability logbook
     */
    public String getMinusOneYearLogbookTraceabilityDate() {
        return minusOneYearLogbookTraceabilityDate;
    }

    /**
     * @return Date of the previous traceability logbook
     */
    public String getPreviousLogbookTraceabilityDate() {
        return previousLogbookTraceabilityDate;
    }

    /**
     * @return Size of the entry
     */
    public long getSize() {
        return size;
    }

    /**
     * @return Size of the entry
     */
    public DigestType getDigestAlgorithm() {
        return digestAlgorithm;
    }

    /**
     * @return true if max entries has been reached (unit & object group lifecycle traceability operation are limited in size)
     */
    public boolean getMaxEntriesReached() {
        return maxEntriesReached;
    }

    /**
     * getter for securisationVersion
     **/
    public String getSecurisationVersion() {
        return securisationVersion;
    }

    /**
     * setter for securisationVersion
     **/
    public void setSecurisationVersion(String securisationVersion) {
        this.securisationVersion = securisationVersion;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public TraceabilityEvent setWarnings(List<String> warnings) {
        this.warnings = warnings;
        return this;
    }
}

/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.logbook.common.model;

/**
 * information on the traceability event
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
    
    private long numberOfElement;

    /**
     * name of the secure archive in the storage
     */
    private String fileName;

    /**
     * Total size of the ZIP entry
     */
    private long size;
    
    /**
     * Empty constructor for Jackson
     */
    public TraceabilityEvent() {
        // Empty
    }

    /**
     * @param logType         the traceability type
     * @param startDate       date of the first document
     * @param endDate         date of the last document
     * @param hash            hash of the mekle tree
     * @param previousLogbookTraceabilityDate  the previous Logbook Traceability Date
     * @param minusOneMonthLogbookTraceabilityDate  the minus One Month Logbook Traceability Date
     * @param minusOneYearLogbookTraceabilityDate   the minus One Year Logbook Traceability Date
     * @param timeStampToken  timestamp token
     * @param timeStampToken  timestamp token
     * @param numberOfElement number of document to secure
     * @param fileName        path on the archive in workspace
     * @param size            the size
     */
    public TraceabilityEvent(TraceabilityType logType, String startDate, String endDate, String hash, 
    	byte[] timeStampToken, String previousLogbookTraceabilityDate, String minusOneMonthLogbookTraceabilityDate,
    	String minusOneYearLogbookTraceabilityDate, long numberOfElement, String fileName, long size) {
    	this.logType = logType;
    	this.startDate = startDate;
        this.endDate = endDate;
        this.hash = hash;
        this.minusOneMonthLogbookTraceabilityDate = minusOneMonthLogbookTraceabilityDate;
        this.minusOneYearLogbookTraceabilityDate = minusOneYearLogbookTraceabilityDate;
        this.timeStampToken = timeStampToken;
        this.numberOfElement = numberOfElement;
        this.fileName = fileName;
        this.size = size;
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
     * @return numberOfElement
     */
    public long getNumberOfElement() {
        return numberOfElement;
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
}

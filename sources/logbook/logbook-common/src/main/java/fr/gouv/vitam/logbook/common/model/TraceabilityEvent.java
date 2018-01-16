package fr.gouv.vitam.logbook.common.model;

import fr.gouv.vitam.common.digest.DigestType;

/* FIXME: Need specific traceabilityEvent depending on logbook type ? */
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
     * Digest Algorithm
     */
    private DigestType digestAlgorithm;

    /**
     * Empty constructor for Jackson
     */
    public TraceabilityEvent() {
        // Empty
    }
	
	public TraceabilityEvent(TraceabilityType logType, String startDate, String endDate, String hash, 
			byte[] timeStampToken, String previousLogbookTraceabilityDate, String minusOneMonthLogbookTraceabilityDate,
			String minusOneYearLogbookTraceabilityDate, long numberOfElement, String fileName, long size,
			DigestType digestAlgorithm) {
		this.logType = logType;
		this.startDate = startDate;
		this.endDate = endDate;
		this.previousLogbookTraceabilityDate = previousLogbookTraceabilityDate;
		this.hash = hash;
		this.minusOneMonthLogbookTraceabilityDate = minusOneMonthLogbookTraceabilityDate;
		this.minusOneYearLogbookTraceabilityDate = minusOneYearLogbookTraceabilityDate;
		this.timeStampToken = timeStampToken;
		this.numberOfElement = numberOfElement;
		this.fileName = fileName;
		this.size = size;
		this.digestAlgorithm = digestAlgorithm;
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
    /* TODO: TraceabilityType: Move LogbookEnum to common ? */
    // private TraceabilityType type;
    public Object getLogType() {
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
}

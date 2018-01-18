package fr.gouv.vitam.logbook.common.traceability;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;

import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.security.merkletree.MerkleTreeAlgo;
import fr.gouv.vitam.logbook.common.exception.TraceabilityException;
import fr.gouv.vitam.logbook.common.model.TraceabilityEvent;
import fr.gouv.vitam.logbook.common.model.TraceabilityFile;
import fr.gouv.vitam.logbook.common.model.TraceabilityType;

/**
 * Interface used to handle specific steps of the traceability process
 *
 */
public interface LogbookTraceabilityHelper {

    static final LocalDateTime INITIAL_START_DATE = LocalDateTime.of(1970, 1, 1, 0, 0);

	/**
	 * Search for the last traceability entry for the given traceability type and return the StartDate of that item.
	 * 
	 * @param operationID id of the traceability process
	 * @return the startDate of the first element if found, INITIAL_START_DATE else.
	 * @throws TraceabilityException for any error on the search
	 */
	LocalDateTime getLastEvent() throws TraceabilityException;
    
	/**
	 * Search for entries to secure with the traceability process and store data in the given Traceability zip file.<br/>
	 * Also, Merkle Algorithm should be updated by adding each wanted entry as a leaf of the tree.
	 * 
	 * @param algo algorithm used to generate MerkleTree with data.
	 * @param startDate date of the first element that must be secured by the traceability process
	 * @param file output zip to store data (in some TYPE_COLLECTION.json)
	 * @return updated startDate with the first event (only updated if no previous traceability event is found)
	 * @throws IOException if any error occurs while writing in traceabilityFile
	 * @throws TraceabilityException for any other errors specifically in the search / data transformation
	 */
	LocalDateTime saveDataInZip(MerkleTreeAlgo algo, LocalDateTime startDate, TraceabilityFile file)
	    throws IOException, TraceabilityException;
	
	/**
	 * initialize the traceability entry in the logbook collection
	 * 
	 * @param id id of the traceability operation (= id of the created entry in logbook)
	 * @throws TraceabilityException if any error occurs
	 */
	void createLogbookOperationStructure() throws TraceabilityException;

	/**
	 * Update or finalize the traceability entry in the logbook collection
	 * 
	 * @param operationID id of the traceability operation (= id of the created entry in logbook)
	 * @param tenantId tenant used for the operation
	 * @param eventType code of the eventType for the entry
	 * @param status status of the entry
	 * @param event data of the entry
	 * @throws TraceabilityException if any error occurs
	 */
	void createLogbookOperationEvent(Integer tenantId, String eventType, StatusCode status, TraceabilityEvent event)
	    throws TraceabilityException;

	/**
	 * Store the temporary zip in the final destination (on a specific offer for example) and delete tmp file.
	 * 
	 * @param id id of the traceability operation
	 * @param tenant tenant used for the operation
	 * @param zipFile file containing multiple information about the traceability operation (data, merkle, computingData, ...)
	 * @param fileName name of the file on the destination
	 * @param uri uri of the tmp file
	 * @param event traceability event that should be updated by this operation
	 * @throws TraceabilityException if any error occurs
	 */
	void storeAndDeleteZip(Integer tenant, File zipFile, String fileName, String uri, TraceabilityEvent event)
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
	
	// FIXME usefull info ?
	/**
	 * @return the name of the tmp zip file
	 */
	String getZipName();
	
	/**
	 * @return the uri of the tmp file
	 */
	Object getUriName();
	
	/**
	 * Warning: This method MUST be used after "saveDataInZip" witch initialize traceabilityIterator
	 * 
	 * @return the date of the last secured event in traceabilityIterator
	 * @throws TraceabilityException if the traceabilityIterator isn't yet initialized
	 */
	String getEndDate() throws TraceabilityException;
	
	/**
	 * Warning: This method MUST be used after "saveDataInZip" witch initialize traceabilityIterator
	 * 
	 * @return the number of items secured in traceabilityIterator
	 * @throws TraceabilityException if the traceabilityIterator isn't yet initialized
	 */
	Long getDataSize() throws TraceabilityException;

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
	 * @param currentDate date used to compute the "Date -1 month" search
	 * @return the timestamp token of the matching entry or null if no matching entry
	 * @throws InvalidParseOperationException if any errors occurs while deserializing entry's data.
	 * @throws TraceabilityException if any other error occurs
	 */
	byte[] getPreviousMonthTimestampToken(LocalDateTime currentDate) throws InvalidParseOperationException, TraceabilityException;

	/**
	 * Get the first traceability (in logbook) one year before the given currentDate and return the timestampToken of the entry
	 * 
	 * @param currentDate date used to compute the "Date -1 year" search
	 * @return the timestamp token of the matching entry or null if no matching entry
	 * @throws InvalidParseOperationException if any errors occurs while deserializing entry's data.
	 * @throws TraceabilityException if any other error occurs
	 */
	byte[] getPreviousYearTimestampToken(LocalDateTime currentDate) throws InvalidParseOperationException, TraceabilityException;

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
	 * @param currentDate date used to compute the "Date -1 month" search
	 * @return the start date of the matching entry or null if no matching entry
	 * @throws InvalidParseOperationException if any errors occurs while deserializing entry's data.
	 * @throws TraceabilityException if any other error occurs
	 */
	String getPreviousMonthStartDate(LocalDateTime currentDate) throws InvalidParseOperationException, TraceabilityException;

	/**
	 * Get the first traceability (in logbook) one year before the given currentDate and return the startDate of the entry
	 * 
	 * @param currentDate date used to compute the "Date -1 year" search
	 * @return the start date of the matching entry or null if no matching entry
	 * @throws InvalidParseOperationException if any errors occurs while deserializing entry's data.
	 * @throws TraceabilityException if any other error occurs
	 */
	String getPreviousYearStartDate(LocalDateTime currentDate) throws InvalidParseOperationException, TraceabilityException;


}

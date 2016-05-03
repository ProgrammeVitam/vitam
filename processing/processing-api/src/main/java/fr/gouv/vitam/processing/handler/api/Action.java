package fr.gouv.vitam.processing.handler.api;

import fr.gouv.vitam.processing.api.Response;
import fr.gouv.vitam.processing.beans.WorkParams;

/**
 * 
 * 
 *         Action interface: is a contract for different action event
 * 
 *         action class must be implement this interface
 *
 */
public interface Action {

	/**
	 * 
	 * @return boolean flag to indicate status of action (executed ; true or
	 *         false)
	 */
	boolean isExecuted();

	/**
	 * 
	 * @param process
	 * @param params
	 * @return response contains a list of functional message and status code
	 */
	Response execute(fr.gouv.vitam.processing.beans.Process process, WorkParams params);

}

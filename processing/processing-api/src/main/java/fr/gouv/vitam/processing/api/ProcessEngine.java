package fr.gouv.vitam.processing.api;

import fr.gouv.vitam.processing.beans.WorkParams;
import fr.gouv.vitam.processing.engine.ExecutionContextInterface;

public interface ProcessEngine {

	/***
	 * 
	 * instance Process engine
	 * 
	 * 
	 * @return the instance of Process engine (initialed)
	 */

	public ProcessEngine init();

	/***
	 * 
	 * Adds context object representing the handler pool objects
	 * 
	 * @param context
	 * 
	 * 
	 * 
	 * @return the Response of process: message and status code
	 */

	public Response start();

	/***
	 * 
	 * Adds Procces object representing the workflow
	 * 
	 * @param Process
	 *            (workflow)
	 * 
	 * 
	 * @return the Process engine object
	 */

	public ProcessEngine process(fr.gouv.vitam.processing.beans.Process process);

	/***
	 * 
	 * Adds context object representing the handler pool objects
	 * 
	 * @param context
	 * 
	 * 
	 * 
	 * @return the Process engine object
	 */

	public ProcessEngine context(ExecutionContextInterface context);

	/***
	 * 
	 * Adds workParams object representing the References Data objects
	 * 
	 * @param workParams
	 *            contains the SEDA and Numeric object's URL
	 * 
	 * 
	 * 
	 * 
	 * @return the Process engine object
	 */
	public ProcessEngine Workparams(WorkParams workParams);

}

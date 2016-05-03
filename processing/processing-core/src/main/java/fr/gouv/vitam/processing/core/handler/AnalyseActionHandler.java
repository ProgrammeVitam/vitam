package fr.gouv.vitam.processing.core.handler;

import fr.gouv.vitam.processing.api.Response;
import fr.gouv.vitam.processing.beans.ProcessResponse;
import fr.gouv.vitam.processing.beans.WorkParams;

/**
 * 
 *
 * 
 */
public class AnalyseActionHandler extends ActionHandler {

	public static final String HANDLER_ID = "analyseAction";

	@Override
	public boolean isExecuted() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Response execute(fr.gouv.vitam.processing.beans.Process process, WorkParams params) {

		LOGGER.info("AnalyseActionHandler running ...");
		Response response = new ProcessResponse();
		// TODO Auto-generated method stub
		return response;
	}

}

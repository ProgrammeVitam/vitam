package fr.gouv.vitam.processing.core.handler;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.gouv.vitam.processing.api.Response;
import fr.gouv.vitam.processing.beans.ProcessResponse;
import fr.gouv.vitam.processing.constants.StatusCode;
import fr.gouv.vitam.processing.handler.api.Action;

/**
 * 
 *
 */
public abstract class ActionHandler implements Action {

	protected static final Logger LOGGER = LoggerFactory.getLogger(ActionHandler.class);

	/**
	 * functional error status
	 * 
	 * @param message
	 * @return response with KO status Code and functional messages
	 */
	protected Response messageKo(String message) {
		Response response = new ProcessResponse();
		List<String> messages = new ArrayList<>();
		response.setStatus(StatusCode.KO);
		response.setMessages(messages);
		return response;
	}

	/**
	 * technical error status
	 * 
	 * @param message
	 * @return response with Error status Code and technical messages
	 */
	protected Response messageError(String message) {
		Response response = new ProcessResponse();
		List<String> messages = new ArrayList<>();
		response.setStatus(StatusCode.ERROR);
		response.setMessages(messages);
		return response;
	}

}

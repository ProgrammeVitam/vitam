package fr.gouv.vitam.processing.beans;

import java.util.List;

import fr.gouv.vitam.processing.api.Response;
import fr.gouv.vitam.processing.constants.StatusCode;

/**
 * 
 *
 */
public class ProcessResponse implements Response {
	/**
	 * Enum status code
	 */
	private StatusCode status;

	/**
	 * List of functional messages
	 */
	private List<String> messages;

	@Override
	public StatusCode getStatus() {
		return status;
	}

	@Override
	public void setStatus(StatusCode status) {
		this.status = status;
	}

	@Override
	public List<String> getMessages() {
		return messages;
	}

	@Override
	public void setMessages(List<String> messages) {
		this.messages = messages;
	}

}

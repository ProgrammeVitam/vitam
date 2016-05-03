package fr.gouv.vitam.processing.api;

import java.util.List;

import fr.gouv.vitam.processing.constants.StatusCode;

public interface Response {

	/**
	 * 
	 * @return Enum StatusCode {OK,KO,ERROR}
	 */
	public StatusCode getStatus();

	/**
	 * 
	 * @param status
	 *            ENUM statusCode
	 */
	public void setStatus(StatusCode status);

	/**
	 * 
	 * @return list of functional error message
	 */
	public List<String> getMessages();

	/**
	 * 
	 * @param messages
	 *            list of functional error message
	 */
	public void setMessages(List<String> messages);
}

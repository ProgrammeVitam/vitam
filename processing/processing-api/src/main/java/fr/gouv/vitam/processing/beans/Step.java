package fr.gouv.vitam.processing.beans;

import javax.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 
 * 
 *         step class is a collection of actions event
 *
 */
@JsonIgnoreProperties(ignoreUnknown = false)
@XmlType
public class Step {

	@JsonProperty("action")
	protected String action;

	/**
	 * 
	 * @return ({String}) ID of action object or bean
	 */
	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

}

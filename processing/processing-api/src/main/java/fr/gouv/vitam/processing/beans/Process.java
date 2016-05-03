package fr.gouv.vitam.processing.beans;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 
 * 
 *         Process class used for deserialize json file (root element) represent
 *         a totality of workflow objects
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Process {

	protected String id;
	protected String comment;

	/**
	 * workflow propertie, must be defined in JSON file(required)
	 */
	@XmlElement(required = true)
	@JsonProperty("workflow")
	protected List<Workflow> workflow;

	/**
	 * 
	 * @return deserialized workflow list
	 */
	public List<Workflow> getWorkflow() {
		return workflow;
	}

	/**
	 * 
	 * @param workflow
	 */
	public void setWorkflow(List<Workflow> workflow) {
		this.workflow = workflow;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	/**
	 * 
	 * @return comments
	 */
	public String getComment() {
		return comment;
	}

	/**
	 * 
	 * @param comments
	 */
	public void setComment(String comments) {
		this.comment = comments;
	}

	@Override
	public String toString() {

		StringBuilder sb = new StringBuilder();

		sb.append("ID=" + getId() + "\n");
		sb.append("comments=" + getComment() + "\n");
		return sb.toString();
	}

}

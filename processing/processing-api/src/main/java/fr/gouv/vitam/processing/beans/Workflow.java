package fr.gouv.vitam.processing.beans;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 
 *
 */

@JsonIgnoreProperties( ignoreUnknown = true )
@XmlType(name="workflow")
public class Workflow {

	
	protected String workerGroupId;
	protected String stepName;
	@JsonProperty( "steps" )
	@XmlElement( required = true )
	protected List<Step> steps;

	public String getWorkerGroupId() {
		return workerGroupId;
	}

	public void setWorkerGroupId(String workerGroupId) {
		this.workerGroupId = workerGroupId;
	}

	public String getStepName() {
		return stepName;
	}

	public void setStepName(String stepName) {
		this.stepName = stepName;
	}

	public List<Step> getSteps() {
		return steps;
	}

	public void setSteps(List<Step> steps) {
		this.steps = steps;
	}

}

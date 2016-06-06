/*******************************************************************************
 * This file is part of Vitam Project.
 * 
 * Copyright Vitam (2012, 2015)
 *
 * This software is governed by the CeCILL 2.1 license under French law and
 * abiding by the rules of distribution of free software. You can use, modify
 * and/ or redistribute the software under the terms of the CeCILL license as
 * circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify
 * and redistribute granted by the license, users are provided only with a
 * limited warranty and the software's author, the holder of the economic
 * rights, and the successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with
 * loading, using, modifying and/or developing or reproducing the software by
 * the user in light of its specific status of free software, that may mean that
 * it is complicated to manipulate, and that also therefore means that it is
 * reserved for developers and experienced professionals having in-depth
 * computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling
 * the security of their systems and/or data to be ensured and, more generally,
 * to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.processing.common.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Step Object in process workflow
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class Step {

	private String workerGroupId;
	private String stepName;
	private Distribution distribution;
	@JsonProperty("actions")
	private List<Action> actions;

	public List<Action> getActions() {
	    if (actions == null) {
	        return new ArrayList<>();
	    }
		return actions;
	}

	/**
	 * 
	 * @param actions
	 */
	public Step setActions(List<Action> actions) {
		this.actions = actions;
		return this;
	}

	/**
	 * 
	 * @return workerGroupId
	 */
	public String getWorkerGroupId() {
	    if (workerGroupId == null) {
	        return "";
	    }
		return workerGroupId;
	}

	/**
	 * 
	 * @param workerGroupId
	 */
	public Step setWorkerGroupId(String workerGroupId) {
		this.workerGroupId = workerGroupId;
		return this;
	}

	/**
	 * @return the stepName
	 */
	public String getStepName() {
	    if (stepName == null) {
            return "";
        }
		return stepName;
	}

	/**
	 * @param stepName
	 *            the stepName to set
	 */
	public Step setStepName(String stepName) {
		this.stepName = stepName;
		return this;
	}

    public Distribution getDistribution() {
        if (distribution == null) {
            return new Distribution();
        }
        return distribution;
    }

    public Step setDistribution(Distribution distribution) {
        this.distribution = distribution;
        return this;
    }
	
}

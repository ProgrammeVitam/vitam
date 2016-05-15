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
package fr.gouv.vitam.processing.api.model;

import fr.gouv.vitam.processing.api.config.ServerConfiguration;

/**
 * WorkParams class
 *
 * Contains all useful parameters for the process engine and handler
 * 
 */
// TODO REVIEW improve comment form
public class WorkParams {
        // FIXME REVIEW add the notion of tenantID
	private String containerName;
	private String objectName;
	// object 's id {digital, metatdata,goupeobject, SIP etc...}

	private ServerConfiguration serverConfiguration;
	private String objectId;
	// TODO REVIEW what is the difference between objectId and guuid ? Probably the guuid is the id of the workflow instance (not the class, the instance of the class)
	private String guuid;
	private String metaDataRequest;

	// FIXME REVIEW do not return null
	public String getContainerName() {
		return containerName;
	}

	public WorkParams setContainerName(String containerName) {
		this.containerName = containerName;
		return this;

	}

	/**
	 * @return the objectId
	 */
	// FIXME REVIEW do not return null
	public String getObjectId() {
		return objectId;
	}

	/**
	 * @param objectId
	 *            the objectId to set
	 * 
	 * @return WorkParams
	 */
	public WorkParams setObjectId(String objectId) {
		this.objectId = objectId;
		return this;
	}

	/**
	 * @return the objectName
	 */
	// FIXME REVIEW do not return null
	public String getObjectName() {
		return objectName;
	}

	/**
	 * @param objectName
	 *            the objectName to set
	 * @return WorkParams
	 */
	public WorkParams setObjectName(String objectName) {
		this.objectName = objectName;
		return this;
	}

	/**
	 * @return the metaDataRequest
	 */
	// FIXME REVIEW do not return null
	public String getMetaDataRequest() {
		return metaDataRequest;
	}

	/**
	 * @param metaData
	 *            the metaDataRequest to set
	 * @return WorkParams
	 */
	public WorkParams setMetaDataRequest(String metaData) {
		this.metaDataRequest = metaData;
		return this;
	}

	/**
	 * @return the serverConfiguration
	 */
	// FIXME REVIEW do not return null
	public ServerConfiguration getServerConfiguration() {
		return serverConfiguration;
	}

	/**
	 * @param serverConfiguration
	 *            the serverConfiguration to set
	 */
	public WorkParams setServerConfiguration(ServerConfiguration serverConfiguration) {
		this.serverConfiguration = serverConfiguration;
		return this;
	}

	/**
	 * @return the guuid
	 */
	// FIXME REVIEW do not return null
	public String getGuuid() {
		return guuid;
	}

	/**
	 * @param guuid
	 *            the guuid to set
	 */
	public WorkParams setGuuid(String guuid) {
		this.guuid = guuid;
		return this;
	}

}

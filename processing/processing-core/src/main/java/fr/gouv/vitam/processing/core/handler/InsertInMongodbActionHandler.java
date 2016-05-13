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
package fr.gouv.vitam.processing.core.handler;

import java.io.InputStream;

import fr.gouv.vitam.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.client.MetaDataClient;
import fr.gouv.vitam.processing.api.model.ProcessResponse;
import fr.gouv.vitam.processing.api.model.Response;
import fr.gouv.vitam.processing.api.model.StatusCode;
import fr.gouv.vitam.processing.api.model.WorkParams;
import fr.gouv.vitam.processing.core.utils.FileVitamUtils;
import fr.gouv.vitam.workspace.client.WorkspaceClient;

/**
 * Save/insert unit 's metaData in mongodb
 * 
 *
 */
public class InsertInMongodbActionHandler extends ActionHandler {

	public static final String HANDLER_ID = "saveInDataBaseAction";

	private MetaDataClient metaDataClient;

	private WorkspaceClient workspaceClient;

	public InsertInMongodbActionHandler() {

	}

	@Override
	public Response execute(WorkParams params) {
		LOGGER.info("InsertInMongodbActionHandler running ...");
		/**
		 * 
		 */
		if (params != null && params.getServerConfiguration() != null) {
			this.metaDataClient = new MetaDataClient(params.getServerConfiguration().getUrlMetada());
			workspaceClient = new WorkspaceClient(params.getServerConfiguration().getUrlWorkspace());
		}
		/**
		 * 
		 */
		Response response = new ProcessResponse();

		if (params == null) {
			LOGGER.info("parameters or metatDataRequest is null");
			return messageKo("parameters or metatDataRequest is null");
		}

		try {

			/**
			 * Retrieves an inputStream representing the data at location
			 * (GUUID) containerName and objectName
			 **/
			InputStream inputStream = workspaceClient.getObject(params.getGuuid(), "json.json");
			// convert InputStream To String
			// populate Request Insert
			String insertRequest = getInsertRequest(FileVitamUtils.convertInputStreamToString(inputStream));
			// insert Metadata
			metaDataClient.insert(insertRequest);

		} catch (MetaDataExecutionException e) {
			LOGGER.error(e.getMessage(), e);
			return messageFatal(e.getMessage());

		} catch (Exception e) {
			LOGGER.error("Exception thrown when excuting InsertInMongodbActionHandler", e);
			return messageFatal("Exception thrown when excuting InsertInMongodbActionHandler");
		}
		response.setStatus(StatusCode.OK);

		return response;
	}

	private String getInsertRequest(String data) {

		return "{\"$roots\": []," + "\"$query\": []," + "\"$filter\": []," + "\"$data\": " + data + "}";
	}
}

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

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import fr.gouv.vitam.processing.api.model.ProcessResponse;
import fr.gouv.vitam.processing.api.model.Response;
import fr.gouv.vitam.processing.api.model.StatusCode;
import fr.gouv.vitam.processing.api.model.WorkParams;
import fr.gouv.vitam.processing.core.utils.FileVitamUtils;
import fr.gouv.vitam.workspace.client.WorkspaceClient;

/**
 * 
 * ExtractContentActionHandler handler class used to extract metaData .Create
 * and put a new file (matadata extracted) json.json into container GUID
 *
 */
public class ExtractContentActionHandler extends ActionHandler {
	// FIXME REVIEW prefer a static method getId()

	public static final String HANDLER_ID = "extractContentAction";

	private WorkspaceClient workspaceClient;

	@Override
	public Response execute(WorkParams params) {
		// FIXME REVIEW you should not depend on LOGGER from another class!
		LOGGER.info("ExtractContentActionHandler running ...");
		Response response = new ProcessResponse();
		/**
		 * 
		 */
                // FIXME REVIEW Throw an exception or send a messageFatal is the params is incorrect
		if (params != null && params.getServerConfiguration() != null) {
			LOGGER.info("instantiate WorkspaceClient and metaDataClient ...");
			this.workspaceClient = new WorkspaceClient(params.getServerConfiguration().getUrlWorkspace());

		}
		try {
			/**
			 * Retrieves an object representing the data at location (GUUID)
			 * containerName/objectName
			 **/
			// FIXME REVIEW name should be a global parameter (as "manifest.xml") or even a parameter from WorkParams
			InputStream inputStream = workspaceClient.getObject(params.getGuuid(), "seda.xml");
			/**
			 * // TODO extract metatData
			 */
                        // FIXME REVIEW do not put file in memory: keep a stream a stream or ensure you have small parts to get it into memory
			// convert xml to JSON file
			String jsonData = FileVitamUtils.convertInputStreamXMLToString(inputStream);

			// convert String into InputStream
			InputStream json = new ByteArrayInputStream(jsonData.getBytes());
			// put json file to workspace
			workspaceClient.putObject(params.getGuuid(), "json.json", json);
			/**
			 * 
			 */
		} catch (Exception e) {
			LOGGER.info("An exception thrown when adding file to workspace");
			messageFatal(e.getMessage());
		}

		response.setStatus(StatusCode.OK);

		return response;
	}

}

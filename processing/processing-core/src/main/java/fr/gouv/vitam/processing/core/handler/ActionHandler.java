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

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.gouv.vitam.processing.api.model.ProcessResponse;
import fr.gouv.vitam.processing.api.model.Response;
import fr.gouv.vitam.processing.api.model.StatusCode;
import fr.gouv.vitam.processing.api.worker.Action;


// TODO REVIEW The name of the class could be clearer / Documentation of the class is missing

/**
 * 
 * 
 *
 */
// FIXME REVIEW missing package-info
public abstract class ActionHandler implements Action {
	// FIXME REVIEW should be private

	protected static final Logger LOGGER = LoggerFactory.getLogger(ActionHandler.class);

	/**
	 * functional error status
	 * 
	 * @param message
	 * @return response with KO status Code and functional messages
	 */
	protected Response messageKo(String message) {
		// FIXME REVIEW message is ignored
		Response response = new ProcessResponse();
		List<String> messages = new ArrayList<>();
		response.setStatus(StatusCode.KO);
		response.setMessages(messages);
		return response;
	}

	/**
	 * fatal error status : Indicates a critical error such as technical ,
	 * runtime Exception
	 * 
	 * @param message
	 * @return response with FATAL status Code and technical error message
	 */
	protected Response messageFatal(String message) {
		// FIXME REVIEW message is ignored
		Response response = new ProcessResponse();
		List<String> messages = new ArrayList<>();
		response.setStatus(StatusCode.FATAL);
		response.setMessages(messages);
		return response;
	}

}

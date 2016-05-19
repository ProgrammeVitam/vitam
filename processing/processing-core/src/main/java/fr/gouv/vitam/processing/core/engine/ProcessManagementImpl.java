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
package fr.gouv.vitam.processing.core.engine;

import java.io.File;
import java.io.FileReader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import fr.gouv.vitam.processing.api.config.ServerConfiguration;
import fr.gouv.vitam.processing.api.engine.ProcessEngine;
import fr.gouv.vitam.processing.api.engine.ProcessManagement;
import fr.gouv.vitam.processing.api.model.Response;
import fr.gouv.vitam.processing.api.model.WorkParams;

// TODO REVIEW Documentation is not sufficient

/**
 * ProcessManagementImpl
 * 
 * 
 * 
 */
// FIXME REVIEW separate package for Management and Engine
public class ProcessManagementImpl implements ProcessManagement {

	private ProcessEngine processEngine;

	// FIXME REVIEW make it private and build a public static getInstance() / createInstance()
	public ProcessManagementImpl() {
		/**
		 * inject process engine
		 */
		processEngine = new ProcessEngineImpl();
	}

	@Override
	public Response executeVitamProcess(WorkParams workParams, String workflowId) {
		FileReader yamlFile;
		try {
                        // FIXME REVIEW Normally, this type of call is more in the test classes than in "business" code
			yamlFile = new FileReader(new File(
					Thread.currentThread().getContextClassLoader().getSystemResource("processing.conf").getFile()));
			ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
			ServerConfiguration configuration = new ServerConfiguration();
			configuration = mapper.readValue(yamlFile, ServerConfiguration.class);
			workParams.setServerConfiguration(configuration);
                // FIXME REVIEW Manage Exception
		} catch (Exception e) {

		}
		return processEngine.startProcessByWorkFlowId(workParams, workflowId);
	}

}

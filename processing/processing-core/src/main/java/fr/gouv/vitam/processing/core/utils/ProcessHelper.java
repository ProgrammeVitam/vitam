package fr.gouv.vitam.processing.core.utils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
// FIXME REVIEW Don't use UUID implementation but fr.gouv.vitam.common.UUIDFactory
import fr.gouv.vitam.common.UUID22;
import fr.gouv.vitam.processing.api.config.ServerConfiguration;
import fr.gouv.vitam.processing.api.engine.ProcessManagement;
import fr.gouv.vitam.processing.api.model.WorkParams;
import fr.gouv.vitam.processing.core.engine.ProcessManagementImpl;
import fr.gouv.vitam.workspace.client.WorkspaceClient;

// TODO REVIEW missing licence header
// TODO REVIEW missing javadoc comment

public class ProcessHelper {

	private WorkspaceClient workspaceClient;

	/**
	 * 
	 */
	public ProcessHelper() {
		workspaceClient = new WorkspaceClient(getConfiguration().getUrlWorkspace());
	}
	// FIXME REVIEW this is a test code, not a business code

	public static void main(String... args) {
		ProcessHelper helper = new ProcessHelper();
		try {
			String guiid = helper.saveInWorkaspace("sip1");
			ProcessManagement processManagement = new ProcessManagementImpl();

			processManagement.executeVitamProcess(new WorkParams().setGuuid(guiid), "workflowJSONv1");

		} catch (IOException e) {
		}

	}

	public String saveInWorkaspace(String fileName) throws IOException {
		// FIXME REVIEW Use Factory
		// FIXME REVIEW check null

		String guiid = new UUID22().toString();

		if (!workspaceClient.containerExists(guiid)) {
			workspaceClient.createContainer(guiid);
		}

                // FIXME REVIEW Normally, this type of call is more in the test classes than in "business" code
		workspaceClient.putObject(guiid, "seda.xml", getInputStream(fileName + ".xml"));
                // FIXME REVIEW Normally, this type of call is more in the test classes than in "business" code
		workspaceClient.putObject(guiid, "sip.pdf", getInputStream(fileName + ".pdf"));

		return guiid;
	}

	public ServerConfiguration getConfiguration() {

		ServerConfiguration serverConfiguration = null;

		FileReader yamlFile;
		try {
                    // FIXME REVIEW Normally, this type of call is more in the test classes than in "business" code
			yamlFile = new FileReader(new File(
					Thread.currentThread().getContextClassLoader().getSystemResource("processing.conf").getFile()));
			ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
			serverConfiguration = mapper.readValue(yamlFile, ServerConfiguration.class);
		} catch (Exception e) {

		}
		return serverConfiguration;
	}

        // FIXME REVIEW Normally, this method is more in the test classes
	private InputStream getInputStream(String file) throws IOException {
		return Thread.currentThread().getContextClassLoader().getResourceAsStream(file);
	}

}

package fr.gouv.vitam.processing.core.utils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import fr.gouv.vitam.common.UUID22;
import fr.gouv.vitam.processing.api.config.ServerConfiguration;
import fr.gouv.vitam.processing.api.engine.ProcessManagement;
import fr.gouv.vitam.processing.api.model.WorkParams;
import fr.gouv.vitam.processing.core.engine.ProcessManagementImpl;
import fr.gouv.vitam.workspace.client.WorkspaceClient;

public class ProcessHelper {

	private WorkspaceClient workspaceClient;

	/**
	 * 
	 */
	public ProcessHelper() {
		workspaceClient = new WorkspaceClient(getConfiguration().getUrlWorkspace());
	}

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

		String guiid = new UUID22().toString();

		if (!workspaceClient.containerExists(guiid)) {
			workspaceClient.createContainer(guiid);
		}

		workspaceClient.putObject(guiid, "seda.xml", getInputStream(fileName + ".xml"));

		workspaceClient.putObject(guiid, "sip.pdf", getInputStream(fileName + ".pdf"));

		return guiid;
	}

	public ServerConfiguration getConfiguration() {

		ServerConfiguration serverConfiguration = null;

		FileReader yamlFile;
		try {
			yamlFile = new FileReader(new File(
					Thread.currentThread().getContextClassLoader().getSystemResource("processing.conf").getFile()));
			ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
			serverConfiguration = mapper.readValue(yamlFile, ServerConfiguration.class);
		} catch (Exception e) {

		}
		return serverConfiguration;
	}

	private InputStream getInputStream(String file) throws IOException {
		return Thread.currentThread().getContextClassLoader().getResourceAsStream(file);
	}

}

package fr.gouv.vitam.processing.core.handler;

import java.io.File;
import java.io.IOException;

import com.google.common.io.Files;

import fr.gouv.vitam.processing.api.Response;
import fr.gouv.vitam.processing.beans.ProcessResponse;
import fr.gouv.vitam.processing.beans.WorkParams;
import fr.gouv.vitam.processing.constants.StatusCode;
import fr.gouv.vitam.utils.ContainerUtils;
import fr.gouv.vitam.workspace.api.ContentAddressableStorage;
import fr.gouv.vitam.workspace.api.config.StorageConfiguration;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.core.FileSystemMock;

/**
 * 
 *
 */
public class StoreActionHandler extends ActionHandler {

	public static final String HANDLER_ID = "storeAction";

	{
		init();
	}

	private ContentAddressableStorage storageService;

	@Override
	public boolean isExecuted() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Response execute(fr.gouv.vitam.processing.beans.Process process, WorkParams params) {

		LOGGER.info("StoreActionHandler running ...");
		Response response = new ProcessResponse();

		try {
			// String containerName = generateContainerName();
			// create container
			storageService.createContainer(params.getContainerName());
			// put object in the container
			/**
			 * // TODO getTestBytes("file1.pdf") JUST FOR TESTING / WE WILL ADD
			 * A REAL BYTES FILES OR REAL NAME OBJECT
			 */

			if (storageService.containerExists(params.getContainerName())) {
				LOGGER.info("adding num object in container name :" + params.getContainerName());
				storageService.putObject(params.getContainerName(), params.getObjectName(),
						ContainerUtils.getTestBytes(params.getObjectName()));
				if (storageService.objectExists(params.getContainerName(), params.getObjectName())) {
					response.setStatus(StatusCode.OK);
				} else {
					response.setStatus(StatusCode.KO);
				}

			}

		} catch (ContentAddressableStorageException e) {
			LOGGER.error(
					"ContentAddressableStorageException thrown by contentAddressableStorage when puting object/objectName :"
							+ params.getObjectName() + "",
					e);
			response.setStatus(StatusCode.KO);

		} catch (Exception e) {

			LOGGER.error("runtime exceptions thrown by the StoreActionHandler during the execution :", e);
			response.setStatus(StatusCode.ERROR);
		}

		return response;
	}

	private void init() {

		LOGGER.info("initializing StoreActionHandler ...");
		StorageConfiguration configuration = new StorageConfiguration();

		File tempDir = Files.createTempDir();
		try {
			configuration.storagePath = tempDir.getCanonicalPath();
		} catch (IOException e) {
			LOGGER.error("IOException ..", e);
		}
		storageService = new FileSystemMock(configuration);

	}

}

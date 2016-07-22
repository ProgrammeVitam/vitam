package fr.gouv.vitam.processing.management.client;

/**
 * ProcessingManagement factory for creating ProcessingManagement client
 */
public class ProcessingManagementClientFactory {
	
	 /**
     * Create ProcessingManagement client with server URL
     *
     * @param serviceUrl ProcessingManagemen server Url
     * @return ProcessingManagementClient
     */
	
    public static ProcessingManagementClient create(String serviceUrl) {
        return new ProcessingManagementClient(serviceUrl);
    }

}

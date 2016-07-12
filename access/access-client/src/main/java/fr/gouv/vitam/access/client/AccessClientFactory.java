/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 * 
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.access.client;

import java.io.IOException;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.VitamServerFactory;
import fr.gouv.vitam.common.server.application.configuration.ClientConfiguration;
import fr.gouv.vitam.common.server.application.configuration.ClientConfigurationImpl;

/**
 * Access client factory<br>
 * 
 * Used to create access client : if configuration file does not exist
 * {@value 'access-client.conf'}},<br>
 * mock access client will be returned
 * 
 */
public class AccessClientFactory {
	/**
	 * Default client operation type
	 */
	private static AccessClientType defaultOperationsClientType;
	private static final String CONFIGURATION_FILENAME = "access-client.conf";
	private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AccessClientFactory.class);
	private static final AccessClientFactory ACCESS_CLIENT_FACTORY = new AccessClientFactory();

	private String server = "localhost";
	private int port = VitamServerFactory.getDefaultPort();

	private AccessClientFactory() {
		changeConfigurationFile(CONFIGURATION_FILENAME);
	}

	/**
	 * Set the AccessClientFactory configuration
	 *
	 * @param type
	 * @param server
	 *            hostname
	 * @param port
	 *            port to use
	 * @throws IllegalArgumentException
	 *             if type null or if type is OPERATIONS and server is null or
	 *             empty or port <= 0
	 */
	static final void setConfiguration(AccessClientType type, String server, int port) {

		changeDefaultClientType(type);
		if (type == AccessClientType.PRODUCTION) {
			ParametersChecker.checkParameter("Server cannot be null or empty with OPERATIONS", server);
			ParametersChecker.checkValue("port", port, 1);
		}
		ACCESS_CLIENT_FACTORY.server = server;
		ACCESS_CLIENT_FACTORY.port = port;
	}

	/**
	 * Get the AccessClientFactory instance
	 *
	 * @return the instance
	 */
	public static final AccessClientFactory getInstance() {
		return ACCESS_CLIENT_FACTORY;
	}

	/**
	 * Get the default type access client
	 *
	 * @return the default access client
	 */
	public AccessClient getAccessOperationClient() {
		AccessClient client;
		switch (defaultOperationsClientType) {
		case MOCK:
			client = new AccessClientMock();
			break;
		case PRODUCTION:
			client = new AccessClientRest(server, port);
			break;
		default:
			throw new IllegalArgumentException("Log type unknown");
		}
		return client;
	}

	/**
	 * Modify the default access client type
	 *
	 * @param type
	 *            the client type to set
	 * @throws IllegalArgumentException
	 *             if type null
	 */
	static void changeDefaultClientType(AccessClientType type) {
		if (type == null) {
			throw new IllegalArgumentException();
		}
		defaultOperationsClientType = type;
	}

	/**
	 * Get the default access client type
	 *
	 * @return the default access client type
	 */
	public static AccessClientType getDefaultAccessClientType() {
		return defaultOperationsClientType;
	}

	/**
	 * Change client configuration from a Yaml files
	 *
	 * @param configurationPath
	 *            the path to the configuration file
	 */
	public final void changeConfigurationFile(String configurationPath) {
		changeDefaultClientType(AccessClientType.MOCK);
		ClientConfiguration configuration = null;
		try {
			configuration = PropertiesUtils.readYaml(PropertiesUtils.findFile(configurationPath),
					ClientConfigurationImpl.class);
		} catch (final IOException fnf) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(String.format("Error when retrieving configuration file %s, using mock",
						CONFIGURATION_FILENAME), fnf);
			}
		}
		if (configuration == null) {
			LOGGER.debug(
					String.format("Error when retrieving configuration file %s, using mock", CONFIGURATION_FILENAME));
		} else {
			server = configuration.getServerHost();
			port = configuration.getServerPort();
			changeDefaultClientType(AccessClientType.PRODUCTION);
		}
	}

	/**
	 * enum to define client type
	 */
	public enum AccessClientType {
		/**
		 * To use only in MOCK ACCESS
		 */
		MOCK,
		/**
		 * Use real service (need server to be set)
		 */
		PRODUCTION
	}
}

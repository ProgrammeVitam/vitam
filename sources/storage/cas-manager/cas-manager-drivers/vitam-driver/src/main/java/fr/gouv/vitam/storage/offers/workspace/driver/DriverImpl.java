/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "https://cecill.info".
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
package fr.gouv.vitam.storage.offers.workspace.driver;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.client.VitamClientFactoryInterface;
import fr.gouv.vitam.common.client.configuration.ClientConfiguration;
import fr.gouv.vitam.common.client.configuration.ClientConfigurationImpl;
import fr.gouv.vitam.common.client.configuration.SSLConfiguration;
import fr.gouv.vitam.common.client.configuration.SSLKey;
import fr.gouv.vitam.common.client.configuration.SecureClientConfigurationImpl;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.storage.driver.AbstractConnection;
import fr.gouv.vitam.storage.driver.AbstractDriver;
import fr.gouv.vitam.storage.driver.Connection;
import fr.gouv.vitam.storage.driver.exception.StorageDriverException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageOffer;

import javax.annotation.Nonnull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Workspace Driver Implementation
 */
public class DriverImpl extends AbstractDriver {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DriverImpl.class);

    private static final String DRIVER_NAME = "DefaultOfferDriver";
    private static final String RESOURCE_PATH = "/offer/v1";

    private static final DriverImpl DRIVER_IMPL = new DriverImpl();

    static class DriverClientFactory extends VitamClientFactory<ConnectionImpl> {

        final Properties parameters;

        DriverClientFactory(ClientConfiguration configuration, String resourcePath, Properties parameters) {
            super(configuration, resourcePath);
            enableUseAuthorizationFilter();
            this.parameters = parameters;
        }

        @Override
        public ConnectionImpl getClient() {
            return new ConnectionImpl(DRIVER_NAME, this);
        }
    }

    /**
     * Constructor
     */
    public DriverImpl() {
        // Empty
    }

    /**
     * Get the ProcessingManagementClientFactory instance
     *
     * @return the instance
     */
    public static DriverImpl getInstance() {
        return DRIVER_IMPL;
    }

    @Override
    protected VitamClientFactoryInterface<? extends AbstractConnection> addInternalOfferAsFactory(
        StorageOffer offer,
        Properties parameters
    ) {
        return new DriverClientFactory(changeConfigurationFile(offer), RESOURCE_PATH, parameters);
    }

    @Override
    @Nonnull
    public Connection connect(String offerId) throws StorageDriverException {
        ParametersChecker.checkParameter("The parameter offer is required", offerId);

        VitamClientFactoryInterface<? extends AbstractConnection> factory = connectionFactories.get(offerId);
        if (factory == null) {
            LOGGER.error("Driver {} has no Offer named {}", getName(), offerId);
            StorageNotFoundException exception = new StorageNotFoundException(
                "Driver " + getName() + " has no Offer named " + offerId
            );
            throw new StorageDriverException(
                "Driver " + getName() + " with Offer " + offerId,
                exception.getMessage(),
                false,
                exception
            );
        }

        return factory.getClient();
    }

    private static ClientConfiguration changeConfigurationFile(StorageOffer offer) {
        ParametersChecker.checkParameter("StorageOffer cannot be null", offer);
        try {
            final URI url = new URI(offer.getBaseUrl());
            Map<String, String> param = offer.getParameters();
            if ("https".equalsIgnoreCase(url.getScheme()) || (param != null && param.get("keyStore-keyPath") != null)) {
                List<SSLKey> keystoreList = new ArrayList<>();
                List<SSLKey> truststoreList = new ArrayList<>();
                keystoreList.add(new SSLKey(param.get("keyStore-keyPath"), param.get("keyStore-keyPassword")));
                truststoreList.add(new SSLKey(param.get("trustStore-keyPath"), param.get("trustStore-keyPassword")));
                return new SecureClientConfigurationImpl(
                    url.getHost(),
                    url.getPort(),
                    true,
                    new SSLConfiguration(keystoreList, truststoreList)
                );
            } else {
                return new ClientConfigurationImpl(url.getHost(), url.getPort());
            }
        } catch (final URISyntaxException e) {
            throw new IllegalStateException("Cannot parse the URI: ", e);
        }
    }

    @Override
    public String getName() {
        return DRIVER_NAME;
    }

    @Override
    public int getMajorVersion() {
        return 0;
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }
}

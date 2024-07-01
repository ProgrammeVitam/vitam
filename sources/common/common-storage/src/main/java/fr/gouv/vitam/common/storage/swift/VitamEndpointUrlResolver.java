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
package fr.gouv.vitam.common.storage.swift;

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.storage.StorageConfiguration;
import org.openstack4j.api.identity.EndpointURLResolver;
import org.openstack4j.api.types.ServiceType;
import org.openstack4j.model.identity.URLResolverParams;
import org.openstack4j.openstack.identity.internal.DefaultEndpointURLResolver;

import java.util.function.Function;

public class VitamEndpointUrlResolver implements EndpointURLResolver {

    private DefaultEndpointURLResolver resolver;
    private StorageConfiguration configuration;
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(SwiftKeystoneFactoryV3.class);

    /**
     * Constructor
     *
     * @param configuration the given storageConfiguration
     */
    public VitamEndpointUrlResolver(StorageConfiguration configuration) {
        this(new DefaultEndpointURLResolver(), configuration);
    }

    /**
     * Useful for inject mock in test class
     *
     * @param resolver can be mock or instance of DefaultEndpointURLResolver
     * @param configuration can be a mock or new instance of StorageConfiguration
     */
    @VisibleForTesting
    VitamEndpointUrlResolver(DefaultEndpointURLResolver resolver, StorageConfiguration configuration) {
        this.resolver = resolver;
        this.configuration = configuration;
    }

    @Override
    public String findURLV2(URLResolverParams params) {
        return findSwiftUrl(params, resolver::findURLV2);
    }

    @Override
    public String findURLV3(URLResolverParams params) {
        return findSwiftUrl(params, resolver::findURLV3);
    }

    private String findSwiftUrl(URLResolverParams params, Function<URLResolverParams, String> findURL) {
        String result = findURL.apply(params);

        if (params.type == ServiceType.OBJECT_STORAGE) {
            if (configuration.getSwiftUrl() == null || "".equals(configuration.getSwiftUrl().trim())) {
                LOGGER.debug("No swift url found in configuration, using openstack4j EndpointUrlResolver result");
                return result;
            }
            if (result != null) {
                return configuration.getSwiftUrl();
            }
        }
        return result;
    }
}

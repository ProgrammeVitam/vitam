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
package fr.gouv.vitam.cas.container.swift;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.MetadatasObject;
import fr.gouv.vitam.common.storage.ContainerInformation;
import fr.gouv.vitam.common.storage.StorageConfiguration;
import fr.gouv.vitam.common.storage.cas.container.api.ContentAddressableStorageJcloudsAbstract;
import fr.gouv.vitam.common.storage.cas.container.api.MetadatasStorageObject;
import fr.gouv.vitam.common.storage.constants.ErrorMessage;
import fr.gouv.vitam.common.storage.constants.StorageProvider;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.openstack.keystone.config.KeystoneProperties;
import org.jclouds.openstack.swift.v1.SwiftApi;
import org.jclouds.openstack.swift.v1.domain.Account;
import org.jclouds.openstack.swift.v1.domain.SwiftObject;
import org.jclouds.openstack.swift.v1.features.AccountApi;
import org.jclouds.openstack.swift.v1.features.ContainerApi;

import java.util.Properties;

import static org.jclouds.Constants.PROPERTY_CONNECTION_TIMEOUT;
import static org.jclouds.Constants.PROPERTY_MAX_CONNECTIONS_PER_CONTEXT;
import static org.jclouds.Constants.PROPERTY_MAX_CONNECTIONS_PER_HOST;
import static org.jclouds.Constants.PROPERTY_SO_TIMEOUT;
import static org.jclouds.Constants.PROPERTY_USER_THREADS;

/**
 * Creates {@link BlobStoreContext} configured on {@link StorageConfiguration}
 * storage.conf file . This can be used to make an information about container.
 *
 * @see SwiftApi
 * @see BlobStoreContext
 * @see <a href="https://github.com/jclouds/jclouds/pull/1046">
 * https://github.com/jclouds/jclouds/pull/1046
 * </a>
 * <p>
 * <br/>
 * Managing the header name in the TempAuth (Identity Protocol v1)
 */
public class OpenstackSwift extends ContentAddressableStorageJcloudsAbstract {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(OpenstackSwift.class);
    private static final String CONTAINER_API_IS_NULL = "container api is null";
    private static final String ACCOUNT_API_IS_NULL = "Account api is null";
    private static final String SWIFT_IS_NULL = "swift is null";
    // maximum list size of the blob store. In S3, Azure, and Swift, this is
    // 1000, 5000, and 10000
    // respectively
    private int maxResults = 10000;

    private SwiftApi swiftApi;
    private ContainerApi containerApi;
    private AccountApi accountApi;

    /**
     * @param configuration
     */
    public OpenstackSwift(StorageConfiguration configuration) {
        super(configuration);
    }

    public OpenstackSwift(
        StorageConfiguration configuration,
        SwiftApi swiftApi,
        ContainerApi containerApi,
        AccountApi accountApi
    ) {
        super(configuration);
        this.swiftApi = swiftApi;
        this.containerApi = containerApi;
        this.accountApi = accountApi;
    }

    @Override
    public ContainerInformation getContainerInformation(String containerName)
        throws ContentAddressableStorageNotFoundException {
        ParametersChecker.checkParameter("Container name may not be null", containerName);
        final ContainerInformation containerInformation = new ContainerInformation();
        Account account = getAccountApi().get();
        if (account != null) {
            containerInformation.setUsableSpace(-1);
        }
        return containerInformation;
    }

    @Override
    public BlobStoreContext getContext(StorageConfiguration configuration) {
        ContextBuilder contextBuilder = getContextBuilder(configuration);
        return (BlobStoreContext) contextBuilder.buildApi(BlobStoreContext.class);
    }

    @Override
    public void closeContext() {
        // TODO Maybe keeping the client opened
        close();
    }

    @Override
    public void close() {
        context.close();
    }

    private ContextBuilder getContextBuilder(StorageConfiguration configuration) {
        final String swiftUserName;
        if (StringUtils.isBlank(configuration.getSwiftUser())) {
            swiftUserName = configuration.getSwiftDomain();
        } else {
            swiftUserName = configuration.getSwiftDomain() + ":" + configuration.getSwiftUser();
        }
        ContextBuilder contextBuilder = ContextBuilder.newBuilder(configuration.getProvider())
            .endpoint(configuration.getSwiftKeystoneAuthUrl())
            .credentials(swiftUserName, configuration.getSwiftPassword());
        // Set mandatory headers for keystone v1
        if (StorageProvider.SWIFT_AUTH_V1.getValue().equalsIgnoreCase(configuration.getProvider())) {
            Properties overrides = new Properties();
            overrides.setProperty(KeystoneProperties.CREDENTIAL_TYPE, "tempAuthCredentials");
            overrides.setProperty("jclouds.swift.tempAuth.headerUser", "X-Auth-User");
            overrides.setProperty("jclouds.swift.tempAuth.headerPass", "X-Auth-Key");
            overrides.setProperty(
                PROPERTY_MAX_CONNECTIONS_PER_CONTEXT,
                String.valueOf(configuration.getSwiftMaxConnectionsPerRoute())
            );
            overrides.setProperty(
                PROPERTY_MAX_CONNECTIONS_PER_HOST,
                String.valueOf(configuration.getSwiftMaxConnections())
            );
            overrides.setProperty(
                PROPERTY_CONNECTION_TIMEOUT,
                String.valueOf(configuration.getSwiftConnectionTimeout())
            );
            overrides.setProperty(PROPERTY_SO_TIMEOUT, String.valueOf(configuration.getSwiftReadTimeout()));
            overrides.setProperty(PROPERTY_USER_THREADS, String.valueOf(configuration.getSwiftMaxConnections()));

            contextBuilder.overrides(overrides);
        }

        return contextBuilder;
    }

    @Override
    public void createContainer(String containerName) {
        LOGGER.info("- create container CEPH : " + containerName);
        ParametersChecker.checkParameter(
            ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(),
            containerName
        );
        try {
            // create container in region default region
            getContainerApi().create(containerName);
        } finally {
            closeContext();
        }
    }

    /**
     * Provides ContainerApi <br>
     * with default region id
     *
     * @return ContainerApi
     */
    private ContainerApi getContainerApi() {
        // TODO multi-regions will be supported
        this.containerApi = getSwiftAPi().getContainerApi(swiftApi.getConfiguredRegions().iterator().next());

        if (containerApi == null) {
            LOGGER.error(CONTAINER_API_IS_NULL);
            throw new IllegalArgumentException(CONTAINER_API_IS_NULL);
        }
        return containerApi;
    }

    /**
     * Provides ContainerApi <br>
     * with default region id
     *
     * @return ContainerApi
     */
    private AccountApi getAccountApi() {
        if (accountApi == null) {
            accountApi = getSwiftAPi().getAccountApi(swiftApi.getConfiguredRegions().iterator().next());
        }
        if (accountApi == null) {
            LOGGER.error(ACCOUNT_API_IS_NULL);
            throw new IllegalArgumentException(ACCOUNT_API_IS_NULL);
        }
        return accountApi;
    }

    /**
     * @return swiftApi
     */
    private SwiftApi getSwiftAPi() {
        try {
            StorageConfiguration configuration = getConfiguration();
            if (configuration != null) {
                if (this.swiftApi == null) {
                    ContextBuilder contextBuilder = getContextBuilder(configuration);
                    this.swiftApi = contextBuilder.buildApi(SwiftApi.class);
                }
            }
        } catch (Exception e) {
            LOGGER.error(e);
        }
        if (this.swiftApi == null) {
            LOGGER.error(SWIFT_IS_NULL);
            throw new IllegalArgumentException(SWIFT_IS_NULL);
        }
        return this.swiftApi;
    }

    /**
     * @return the maxResults
     */
    public int getMaxResults() {
        return maxResults;
    }

    @Override
    public MetadatasObject getObjectMetadata(String containerName, String objectId, boolean noCache)
        throws ContentAddressableStorageException {
        ParametersChecker.checkParameter(
            ErrorMessage.CONTAINER_OBJECT_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName,
            objectId
        );
        MetadatasStorageObject result = new MetadatasStorageObject();
        result.setType(containerName.split("_")[1]);
        result.setLastAccessDate(null);
        SwiftObject swiftobject = getSwiftAPi()
            .getObjectApi(swiftApi.getConfiguredRegions().iterator().next(), containerName)
            .get(objectId);
        if (swiftobject == null) {
            throw new ContentAddressableStorageNotFoundException(
                "The Object" + objectId + " can not be found for the container " + containerName
            );
        }
        result.setObjectName(objectId);
        // TODO To be reviewed with the X-DIGEST-ALGORITHM parameter
        result.setDigest(getObjectDigest(containerName, objectId, VitamConfiguration.getDefaultDigestType(), noCache));
        result.setFileSize(swiftobject.getPayload().getContentMetadata().getContentLength());
        result.setLastModifiedDate(swiftobject.getLastModified().toString());

        return result;
    }
}

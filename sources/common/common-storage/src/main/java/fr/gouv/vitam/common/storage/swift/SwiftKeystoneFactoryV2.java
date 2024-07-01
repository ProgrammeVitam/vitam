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

import com.google.common.base.Stopwatch;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.performance.PerformanceLogger;
import fr.gouv.vitam.common.storage.StorageConfiguration;
import org.openstack4j.api.OSClient;
import org.openstack4j.core.transport.Config;
import org.openstack4j.model.identity.v2.Access;
import org.openstack4j.openstack.OSFactory;
import org.openstack4j.openstack.internal.OSClientSession;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * SwiftKeystoneFactoryV2
 */
public class SwiftKeystoneFactoryV2 implements Supplier<OSClient> {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(SwiftKeystoneFactoryV2.class);

    private final StorageConfiguration configuration;
    private final Config configOS4J;
    private final AtomicReference<Access> atomicAccess = new AtomicReference<>(null);
    private final AtomicBoolean oneThread = new AtomicBoolean(true);
    private final Object monitor = new Object();

    public SwiftKeystoneFactoryV2(StorageConfiguration configuration) {
        configOS4J = Config.newConfig()
            .withEndpointURLResolver(new VitamEndpointUrlResolver(configuration))
            .withConnectionTimeout(configuration.getSwiftConnectionTimeout())
            .withReadTimeout(configuration.getSwiftReadTimeout())
            .withMaxConnections(configuration.getSwiftMaxConnections())
            .withMaxConnectionsPerRoute(configuration.getSwiftMaxConnectionsPerRoute());
        this.configuration = configuration;
    }

    public OSClient.OSClientV2 get() {
        Access currentAccess = atomicAccess.get();
        // First call to we have to authenticate
        Date nearTime = LocalDateUtil.getDate(
            LocalDateUtil.now().plusSeconds(configuration.getSwiftHardRenewTokenDelayBeforeExpireTime())
        );
        if (currentAccess == null || currentAccess.getToken().getExpires().before(nearTime)) {
            synchronized (monitor) {
                currentAccess = atomicAccess.get();
                // Double-checked locking through AtomicReference
                if (currentAccess == null) {
                    OSClient.OSClientV2 osClientV2 = renewAccess();
                    currentAccess = osClientV2.getAccess();
                    atomicAccess.set(currentAccess);
                    return osClientV2;
                }
            }
        }

        // Renew Access before expiration only one thread should re-authenticate
        Date farTime = LocalDateUtil.getDate(
            LocalDateUtil.now().plusSeconds(configuration.getSwiftSoftRenewTokenDelayBeforeExpireTime())
        );
        if (currentAccess.getToken().getExpires().before(farTime)) {
            // Only one thread should re-authentication
            if (oneThread.compareAndSet(true, false)) {
                try {
                    OSClient.OSClientV2 osClientV2 = renewAccess();
                    currentAccess = osClientV2.getAccess();
                    atomicAccess.set(currentAccess);
                    return osClientV2;
                } finally {
                    oneThread.set(true);
                }
            }
        }

        // If current client already exists
        OSClientSession.OSClientSessionV2 currentClient =
            (OSClientSession.OSClientSessionV2) OSClientSession.OSClientSessionV2.getCurrent();
        if (null != currentClient && currentAccess.getToken().equals(currentClient.getAccess().getToken())) {
            return currentClient;
        }

        // In all other cases, create a new client from token
        return OSFactory.clientFromAccess(currentAccess, configOS4J);
    }

    private OSClient.OSClientV2 renewAccess() {
        Stopwatch times = Stopwatch.createStarted();
        LOGGER.info("No access or access expired, let's get authenticate again");
        try {
            return OSFactory.builderV2()
                .endpoint(configuration.getSwiftKeystoneAuthUrl())
                .tenantName(configuration.getSwiftDomain())
                .credentials(configuration.getSwiftUser(), configuration.getSwiftPassword())
                .withConfig(configOS4J)
                .authenticate();
        } finally {
            PerformanceLogger.getInstance()
                .log("STP_AUTHENTICATION", "AUTHENTICATE", "RENEW_ACCESS", times.elapsed(TimeUnit.MILLISECONDS));
        }
    }
}

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
import org.apache.http.ssl.SSLContexts;
import org.openstack4j.api.OSClient;
import org.openstack4j.core.transport.Config;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.model.identity.v3.Token;
import org.openstack4j.openstack.OSFactory;
import org.openstack4j.openstack.internal.OSClientSession;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * SwiftKeystoneFactory V3
 */
public class SwiftKeystoneFactoryV3 implements Supplier<OSClient> {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(SwiftKeystoneFactoryV3.class);

    private final StorageConfiguration configuration;
    private final Config configOS4J;
    private final Identifier domainIdentifier;
    private final Identifier projectIdentifier;

    private final AtomicReference<Token> atomicToken = new AtomicReference<>(null);
    private final AtomicBoolean oneThread = new AtomicBoolean(true);

    public SwiftKeystoneFactoryV3(StorageConfiguration configuration)
        throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, KeyManagementException {
        domainIdentifier = Identifier.byName(configuration.getSwiftDomain());
        projectIdentifier = Identifier.byName(configuration.getSwiftProjectName());
        configOS4J = Config.newConfig()
            .withEndpointURLResolver(new VitamEndpointUrlResolver(configuration))
            .withConnectionTimeout(configuration.getSwiftConnectionTimeout())
            .withReadTimeout(configuration.getSwiftReadTimeout())
            .withMaxConnections(configuration.getSwiftMaxConnections())
            .withMaxConnectionsPerRoute(configuration.getSwiftMaxConnectionsPerRoute());

        if (configuration.getSwiftKeystoneAuthUrl().startsWith("https")) {
            File file = new File(configuration.getSwiftTrustStore());
            SSLContext sslContext = SSLContexts.custom()
                .loadTrustMaterial(file, configuration.getSwiftTrustStorePassword().toCharArray())
                .build();

            configOS4J.withSSLContext(sslContext);
        }
        this.configuration = configuration;
    }

    private final Object monitor = new Object();

    public OSClient.OSClientV3 get() {
        Token currentToken = atomicToken.get();
        // First call to we have to authenticate
        Date nearTime = LocalDateUtil.getDate(
            LocalDateUtil.now().plusSeconds(configuration.getSwiftHardRenewTokenDelayBeforeExpireTime())
        );
        if (currentToken == null || currentToken.getExpires().before(nearTime)) {
            synchronized (monitor) {
                currentToken = atomicToken.get();
                // Double-checked locking through AtomicReference
                if (currentToken == null) {
                    OSClient.OSClientV3 osClientV3 = renewToken();
                    currentToken = osClientV3.getToken();
                    atomicToken.set(currentToken);
                    return osClientV3;
                }
            }
        }

        // Renew Token before expiration only one thread should re-authenticate
        Date farTime = LocalDateUtil.getDate(
            LocalDateUtil.now().plusSeconds(configuration.getSwiftSoftRenewTokenDelayBeforeExpireTime())
        );
        if (currentToken.getExpires().before(farTime)) {
            // Only one thread should re-authentication
            if (oneThread.compareAndSet(true, false)) {
                try {
                    OSClient.OSClientV3 osClientV3 = renewToken();
                    currentToken = osClientV3.getToken();
                    atomicToken.set(currentToken);
                    return osClientV3;
                } finally {
                    oneThread.set(true);
                }
            }
        }

        // If current client already exists
        OSClientSession.OSClientSessionV3 currentClient =
            (OSClientSession.OSClientSessionV3) OSClientSession.OSClientSessionV3.getCurrent();
        if (null != currentClient && currentToken.equals(currentClient.getToken())) {
            return currentClient;
        }

        // In all other cases, create a new client from token
        return OSFactory.clientFromToken(currentToken, configOS4J);
    }

    private OSClient.OSClientV3 renewToken() {
        Stopwatch times = Stopwatch.createStarted();
        LOGGER.info("No token or token is expired, let's get authenticate again");
        try {
            return OSFactory.builderV3()
                .endpoint(configuration.getSwiftKeystoneAuthUrl())
                .credentials(configuration.getSwiftUser(), configuration.getSwiftPassword(), domainIdentifier)
                .scopeToProject(projectIdentifier, domainIdentifier)
                .withConfig(configOS4J)
                .authenticate();
        } finally {
            PerformanceLogger.getInstance()
                .log("STP_AUTHENTICATION", "AUTHENTICATE", "RENEW_TOKEN", times.elapsed(TimeUnit.MILLISECONDS));
        }
    }
}

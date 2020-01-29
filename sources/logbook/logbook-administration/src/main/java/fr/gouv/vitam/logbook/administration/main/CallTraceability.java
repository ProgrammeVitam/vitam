/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.logbook.administration.main;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.VitamConfigurationParameters;
import fr.gouv.vitam.common.configuration.SecureConfiguration;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.thread.VitamThreadFactory;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;

/**
 * Utility to launch the Traceability through command line and external scheduler
 */
public class CallTraceability {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CallTraceability.class);
    private static final String VITAM_CONF_FILE_NAME = "vitam.conf";
    private static final String VITAM_SECURISATION_NAME = "securisationDaemon.conf";

    /**
     * @param args ignored
     * @throws InvalidParseOperationException if json data not well-formed
     * @throws LogbookClientServerException if logbook server is unreachable
     */
    public static void main(String[] args) throws InvalidParseOperationException, LogbookClientServerException {
        platformSecretConfiguration();
        try {
            File confFile = PropertiesUtils.findFile(VITAM_SECURISATION_NAME);
            final SecureConfiguration conf = PropertiesUtils.readYaml(confFile, SecureConfiguration.class);
            VitamThreadFactory instance = VitamThreadFactory.getInstance();
            Thread thread = instance.newThread(() -> {
                conf.getTenants().forEach((v) -> {
                    Integer i = Integer.parseInt(v);
                    secureByTenantId(i);
                });
            });
            thread.start();
            thread.join();
        } catch (final IOException e) {
            LOGGER.error(e);
            throw new IllegalStateException("Cannot start the Application Server", e);
        } catch (InterruptedException e) {
            LOGGER.error(e);
            throw new IllegalStateException("Cannot start the Application Server", e);
        }
    }

    private static void secureByTenantId(int tenantId) {
        try {
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);

            final LogbookOperationsClientFactory logbookOperationsClientFactory =
                LogbookOperationsClientFactory.getInstance();

            try (LogbookOperationsClient client = logbookOperationsClientFactory.getClient()) {

                client.traceability();
            }
        } catch (InvalidParseOperationException | LogbookClientServerException e) {

            throw new IllegalStateException(" Error when securing Tenant  :  " + tenantId, e);
        }
        finally {
            VitamThreadUtils.getVitamSession().setTenantId(null);

        }

    }


    private static void platformSecretConfiguration() {
        // Load Platform secret from vitam.conf file
        try (final InputStream yamlIS = PropertiesUtils.getConfigAsStream(VITAM_CONF_FILE_NAME)) {
            final VitamConfigurationParameters vitamConfigurationParameters =
                PropertiesUtils.readYaml(yamlIS, VitamConfigurationParameters.class);

            VitamConfiguration.setSecret(vitamConfigurationParameters.getSecret());
            VitamConfiguration.setFilterActivation(vitamConfigurationParameters.isFilterActivation());

        } catch (final IOException e) {
            LOGGER.error(e);
            throw new IllegalStateException("Cannot start the Application Server", e);
        }
    }

}
